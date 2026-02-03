/**
 * The contents of this file are subject to the Mozilla Public License Version 1.1 (the "License");
 * you may not use this file except in compliance with the License. You may obtain a copy of the
 * License at http://www.mozilla.org/MPL/
 *
 * <p>Software distributed under the License is distributed on an "AS IS" basis, WITHOUT WARRANTY OF
 * ANY KIND, either express or implied. See the License for the specific language governing rights
 * and limitations under the License.
 *
 * <p>The Original Code is OpenELIS code.
 *
 * <p>Copyright (C) CIRG, University of Washington, Seattle WA. All Rights Reserved.
 */
package org.openelisglobal.plugins.analyzer.generichl7;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.Optional;
import org.apache.commons.lang3.StringUtils;
import org.openelisglobal.analyzer.service.AnalyzerConfigurationService;
import org.openelisglobal.analyzer.valueholder.AnalyzerConfiguration;
import org.openelisglobal.analyzerimport.analyzerreaders.AnalyzerLineInserter;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.services.PluginAnalyzerService;
import org.openelisglobal.plugin.AnalyzerImporterPlugin;
import org.openelisglobal.spring.util.SpringContext;

/**
 * Generic HL7 plugin for dashboard-configured analyzers.
 *
 * <p>Feature: 011-madagascar-analyzer-integration (M19)
 *
 * <p>Database-driven HL7 v2.x plugin that uses MSH-3 (sending application) pattern matching
 * to identify analyzers configured via analyzer_configuration.identifier_pattern.
 *
 * <p>Unlike legacy HL7 plugins that hardcode analyzer identification,
 * this generic plugin:
 *
 * <ul>
 *   <li>Extracts MSH-3 from HL7 messages
 *   <li>Matches against analyzer_configuration.identifier_pattern (regex)
 *   <li>Loads test mappings from analyzer_test_mapping table
 * </ul>
 *
 * <p>This enables new HL7 analyzers to be added entirely through the UI without writing Java code.
 *
 * <p>Flow:
 *
 * <ol>
 *   <li>HL7 message arrives at /analyzer/hl7 endpoint
 *   <li>HL7AnalyzerReader iterates all registered plugins (single list; first match wins)
 *   <li>GenericHL7Analyzer.isTargetAnalyzer() queries DB for MSH-3 pattern match
 *   <li>If match found, getAnalyzerLineInserter() returns inserter with matched analyzer ID
 *   <li>GenericHL7LineInserter loads mappings from DB and processes OBX results
 * </ol>
 *
 * <p>Task Reference: T203 (M19) - Implement GenericHL7Analyzer with MSH-3 pattern matching
 */
public class GenericHL7Analyzer implements AnalyzerImporterPlugin {

  /** Plugin name for logging and identification */
  private static final String PLUGIN_NAME = "GenericHL7";

  /**
   * Thread-local storage for matched analyzer configuration.
   *
   * <p>Since isTargetAnalyzer() and getAnalyzerLineInserter() are called separately by
   * HL7AnalyzerReader, we need to preserve the matched configuration between calls. Thread-local
   * ensures thread safety for concurrent requests.
   */
  private final ThreadLocal<AnalyzerConfiguration> matchedConfiguration = new ThreadLocal<>();

  /**
   * Register the generic HL7 plugin with PluginAnalyzerService.
   *
   * <p>Unlike legacy plugins, this does NOT call addAnalyzerDatabaseParts() because:
   *
   * <ul>
   *   <li>Analyzers are created via Dashboard UI, not plugin registration
   *   <li>Test mappings are configured via UI, not hardcoded
   *   <li>This plugin serves MANY analyzers (one config per analyzer_configuration row)
   * </ul>
   *
   * @return true (always succeeds - actual analyzer lookup happens in isTargetAnalyzer)
   */
  public boolean isGenericPlugin() {
    return true;
  }

  @Override
  public boolean connect() {
    SpringContext.getBean(PluginAnalyzerService.class).registerAnalyzer(this);
    LogEvent.logInfo(
        this.getClass().getName(),
        "connect",
        PLUGIN_NAME + " plugin registered - analyzers configured via Dashboard");
    return true;
  }

  /**
   * Check if the HL7 message is from an analyzer configured for this generic plugin.
   *
   * <p>Identification strategy:
   *
   * <ol>
   *   <li>Parse HL7 MSH segment for MSH-3 (sending application)
   *   <li>Query analyzer_configuration for generic plugin configs with matching identifier_pattern
   *   <li>If match found, store configuration and return true
   * </ol>
   *
 * <p>Note: All plugins (legacy and generic) are in one list; the first plugin for which
 * isTargetAnalyzer() returns true is used.
   *
   * @param lines HL7 message segment lines (MSH|..., PID|..., OBX|..., etc.)
   * @return true if message matches a generic HL7 plugin configuration
   */
  @Override
  public boolean isTargetAnalyzer(List<String> lines) {
    // Clear any previous match from thread-local
    matchedConfiguration.remove();

    if (lines == null || lines.isEmpty()) {
      return false;
    }

    // Extract MSH-3 (sending application) from HL7 message
    String msh3 = parseMsh3SendingApplication(lines);
    // #region agent log
    try {
      int lineCount = lines != null ? lines.size() : 0;
      Files.write(Paths.get("/home/ubuntu/OpenELIS-Global-2/.cursor/debug.log"),
          String.format("{\"hypothesisId\":\"D\",\"location\":\"GenericHL7Analyzer.isTargetAnalyzer\",\"message\":\"msh3\",\"data\":{\"msh3\":\"%s\",\"linesSize\":%d},\"timestamp\":%d}\n",
                  msh3 != null ? msh3.replace("\"", "'") : "null", lineCount, System.currentTimeMillis()).getBytes(StandardCharsets.UTF_8),
          StandardOpenOption.CREATE, StandardOpenOption.APPEND);
    } catch (Exception ignore) {}
    // #endregion
    if (StringUtils.isBlank(msh3)) {
      LogEvent.logDebug(
          this.getClass().getSimpleName(),
          "isTargetAnalyzer",
          "Could not extract MSH-3 from HL7 message");
      return false;
    }

    // Query database for matching generic plugin configuration
    try {
      AnalyzerConfigurationService configService =
          SpringContext.getBean(AnalyzerConfigurationService.class);

      if (configService == null) {
        LogEvent.logWarn(
            this.getClass().getSimpleName(),
            "isTargetAnalyzer",
            "AnalyzerConfigurationService not available");
        return false;
      }

      Optional<AnalyzerConfiguration> config =
          configService.findByIdentifierPatternMatch(msh3);
      // #region agent log
      try {
        Files.write(Paths.get("/home/ubuntu/OpenELIS-Global-2/.cursor/debug.log"),
            String.format("{\"hypothesisId\":\"E\",\"location\":\"GenericHL7Analyzer.isTargetAnalyzer\",\"message\":\"config\",\"data\":{\"configPresent\":%s},\"timestamp\":%d}\n",
                    config.isPresent(), System.currentTimeMillis()).getBytes(StandardCharsets.UTF_8),
            StandardOpenOption.CREATE, StandardOpenOption.APPEND);
      } catch (Exception ignore) {}
      // #endregion
      if (config.isPresent()) {
        // Store matched configuration for getAnalyzerLineInserter()
        matchedConfiguration.set(config.get());

        LogEvent.logDebug(
            this.getClass().getSimpleName(),
            "isTargetAnalyzer",
            "Matched MSH-3 '"
                + msh3
                + "' to configuration: "
                + (config.get().getAnalyzer() != null
                    ? config.get().getAnalyzer().getName()
                    : config.get().getId()));
        return true;
      }

    } catch (Exception e) {
      LogEvent.logError(
          "Error checking generic HL7 configuration for MSH-3: " + msh3, e);
    }

    return false;
  }

  /**
   * Check if the message contains analyzer results (OBX segments).
   *
   * @param lines HL7 message segment lines
   * @return true if message contains OBX observation result segments
   */
  @Override
  public boolean isAnalyzerResult(List<String> lines) {
    if (lines == null || lines.isEmpty()) {
      return false;
    }

    // Check for OBX (Observation Result) segments
    return lines.stream().anyMatch(line -> line != null && line.startsWith("OBX|"));
  }

  /**
   * Get the line inserter for processing results from the matched analyzer.
   *
   * @return GenericHL7LineInserter configured with the matched analyzer ID
   * @throws IllegalStateException if called without a prior successful isTargetAnalyzer() call
   */
  @Override
  public AnalyzerLineInserter getAnalyzerLineInserter() {
    AnalyzerConfiguration config = matchedConfiguration.get();

    if (config == null || config.getAnalyzer() == null) {
      LogEvent.logError(
          this.getClass().getSimpleName(),
          "getAnalyzerLineInserter",
          "No matched configuration - isTargetAnalyzer() must be called first");
      throw new IllegalStateException("No matched analyzer configuration");
    }

    String analyzerId = config.getAnalyzer().getId();
    String analyzerName = config.getAnalyzer().getName();

    LogEvent.logDebug(
        this.getClass().getSimpleName(),
        "getAnalyzerLineInserter",
        "Creating inserter for analyzer: " + analyzerName + " (ID: " + analyzerId + ")");

    return new GenericHL7LineInserter(analyzerId, analyzerName);
  }

  /**
   * Parse MSH-3 (sending application) from HL7 MSH segment.
   *
   * <p>HL7 MSH segment format: MSH|^~\&|SendingApp|SendingFacility|ReceivingApp|...
   * - Field 0: Segment ID ("MSH")
   * - Field 1: Field separator ("|")
   * - Field 2: Encoding characters ("^~\&")
   * - Field 3: Sending Application (MSH-3) ← We extract this
   * - Field 4: Sending Facility (MSH-4)
   *
   * <p>Returns MSH-3 value to match against analyzer_configuration.identifier_pattern.
   *
   * @param lines HL7 message segment lines
   * @return MSH-3 sending application string, or null if not found
   */
  private String parseMsh3SendingApplication(List<String> lines) {
    for (String line : lines) {
      if (line != null && line.startsWith("MSH|")) {
        String[] fields = line.split("\\|");
        // MSH segment: MSH|encoding|MSH-3 Sending Application|MSH-4 Sending Facility|...
        // fields[0]=MSH, [1]=encoding, [2]=MSH-3, [3]=MSH-4
        if (fields.length > 2 && !StringUtils.isBlank(fields[2])) {
          return fields[2].trim();
        }
      }
    }
    return null;
  }
}
