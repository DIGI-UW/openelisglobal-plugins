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

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.apache.commons.lang3.StringUtils;
import org.openelisglobal.analyzer.service.AnalyzerService;
import org.openelisglobal.analyzer.valueholder.Analyzer;
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
 * <p>Database-driven HL7 v2.x plugin that derives dynamic sender identity from HL7 MSH fields and
 * matches analyzers configured via analyzer.identifier_pattern.
 *
 * <p>Unlike legacy HL7 plugins that hardcode analyzer identification, this generic plugin:
 *
 * <ul>
 *   <li>Builds sender identity candidates from MSH-3 and MSH-4
 *   <li>Matches against analyzer.identifier_pattern (regex)
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
 * <p>Task Reference: T203 (M19) - Implement GenericHL7Analyzer with dynamic sender matching
 */
public class GenericHL7Analyzer implements AnalyzerImporterPlugin {

  /** Plugin name for logging and identification */
  private static final String PLUGIN_NAME = "GenericHL7";

  private final AnalyzerService analyzerService;

  /**
   * Thread-local storage for matched analyzer.
   *
   * <p>Since isTargetAnalyzer() and getAnalyzerLineInserter() are called separately by
   * HL7AnalyzerReader, we need to preserve the matched analyzer between calls. Thread-local ensures
   * thread safety for concurrent requests.
   */
  private final ThreadLocal<Analyzer> matchedAnalyzer = new ThreadLocal<>();

  public GenericHL7Analyzer() {
    this(null);
  }

  GenericHL7Analyzer(AnalyzerService analyzerService) {
    this.analyzerService = analyzerService;
  }

  /**
   * Register the generic HL7 plugin with PluginAnalyzerService.
   *
   * <p>Unlike legacy plugins, this does NOT call addAnalyzerDatabaseParts() because:
   *
   * <ul>
   *   <li>Analyzers are created via Dashboard UI, not plugin registration
   *   <li>Test mappings are configured via UI, not hardcoded
   *   <li>This plugin serves MANY analyzers (one config per analyzer row)
   * </ul>
   *
   * @return true (always succeeds - actual analyzer lookup happens in isTargetAnalyzer)
   */
  @Override
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
   *   <li>Parse HL7 MSH segment for sender identity candidates
   *   <li>Query analyzer table for generic plugin configs with matching identifier_pattern
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
    matchedAnalyzer.remove();

    if (lines == null || lines.isEmpty()) {
      return false;
    }

    List<String> senderIdentityCandidates = buildSenderIdentityCandidates(lines);
    if (senderIdentityCandidates.isEmpty()) {
      LogEvent.logDebug(
          this.getClass().getSimpleName(),
          "isTargetAnalyzer",
          "Could not extract sender identity from HL7 message");
      return false;
    }

    try {
      AnalyzerService analyzerService = getAnalyzerService();

      if (analyzerService == null) {
        LogEvent.logWarn(
            this.getClass().getSimpleName(), "isTargetAnalyzer", "AnalyzerService not available");
        return false;
      }

      Optional<Analyzer> analyzer = findMatchingAnalyzer(analyzerService, senderIdentityCandidates);
      if (analyzer.isPresent()) {
        // Store matched analyzer for getAnalyzerLineInserter()
        matchedAnalyzer.set(analyzer.get());

        LogEvent.logDebug(
            this.getClass().getSimpleName(),
            "isTargetAnalyzer",
            "Matched sender identity "
                + senderIdentityCandidates
                + " to analyzer: "
                + analyzer.get().getName());
        return true;
      }

    } catch (Exception e) {
      LogEvent.logError(
          "Error checking generic HL7 configuration for sender identities: "
              + senderIdentityCandidates,
          e);
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
    Analyzer analyzer = matchedAnalyzer.get();

    if (analyzer == null) {
      LogEvent.logError(
          this.getClass().getSimpleName(),
          "getAnalyzerLineInserter",
          "No matched analyzer - isTargetAnalyzer() must be called first");
      throw new IllegalStateException("No matched analyzer");
    }

    String analyzerName = analyzer.getAnalyzerType().getName();
    String physicalAnalyzerId = analyzer.getId();

    LogEvent.logDebug(
        this.getClass().getSimpleName(),
        "getAnalyzerLineInserter",
        "Creating inserter for analyzer: "
            + analyzerName
            + " (device ID: "
            + physicalAnalyzerId
            + ")");

    GenericHL7LineInserter inserter = new GenericHL7LineInserter(physicalAnalyzerId, analyzerName);
    inserter.setContextAnalyzerId(physicalAnalyzerId);
    return inserter;
  }

  List<String> buildSenderIdentityCandidates(List<String> lines) {
    String msh3 = parseMshField(lines, 2);
    String msh4 = parseMshField(lines, 3);

    Set<String> identifiers = new LinkedHashSet<>();
    if (StringUtils.isNotBlank(msh3) && StringUtils.isNotBlank(msh4)) {
      identifiers.add(msh3 + " " + msh4);
    }
    if (StringUtils.isNotBlank(msh4)) {
      identifiers.add(msh4);
    }
    if (StringUtils.isNotBlank(msh3)) {
      identifiers.add(msh3);
    }

    List<String> normalizedIdentifiers = new ArrayList<>(identifiers);
    for (String identifier : identifiers) {
      String upperCased = identifier.toUpperCase();
      if (!upperCased.equals(identifier)) {
        normalizedIdentifiers.add(upperCased);
      }
    }

    return normalizedIdentifiers;
  }

  private AnalyzerService getAnalyzerService() {
    return analyzerService != null ? analyzerService : SpringContext.getBean(AnalyzerService.class);
  }

  @SuppressWarnings("unchecked")
  private Optional<Analyzer> findMatchingAnalyzer(
      AnalyzerService analyzerService, List<String> senderIdentityCandidates) {
    try {
      Object match =
          analyzerService
              .getClass()
              .getMethod("findByIdentifierPatternMatch", List.class)
              .invoke(analyzerService, senderIdentityCandidates);
      if (match instanceof Optional) {
        return (Optional<Analyzer>) match;
      }
    } catch (ReflectiveOperationException e) {
      // Fall back to the legacy single-identifier API when the host interface has
      // not yet been updated in the plugin compile classpath.
    }

    for (String identifier : senderIdentityCandidates) {
      Optional<Analyzer> analyzer = analyzerService.findByIdentifierPatternMatch(identifier);
      if (analyzer.isPresent()) {
        return analyzer;
      }
    }

    return Optional.empty();
  }

  private String parseMshField(List<String> lines, int fieldIndex) {
    for (String line : lines) {
      if (line != null && line.startsWith("MSH|")) {
        String[] fields = line.split("\\|");
        if (fields.length > fieldIndex && !StringUtils.isBlank(fields[fieldIndex])) {
          return fields[fieldIndex].trim();
        }
      }
    }
    return null;
  }
}
