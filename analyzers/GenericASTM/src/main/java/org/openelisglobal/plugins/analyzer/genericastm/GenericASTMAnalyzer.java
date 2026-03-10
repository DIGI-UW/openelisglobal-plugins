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
package org.openelisglobal.plugins.analyzer.genericastm;

import java.util.List;
import java.util.Optional;
import org.openelisglobal.analyzer.service.AnalyzerService;
import org.openelisglobal.analyzer.valueholder.Analyzer;
import org.openelisglobal.analyzerimport.analyzerreaders.AnalyzerLineInserter;
import org.openelisglobal.analyzerimport.analyzerreaders.AnalyzerResponder;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.services.PluginAnalyzerService;
import org.openelisglobal.plugin.AnalyzerImporterPlugin;
import org.openelisglobal.spring.util.SpringContext;

/**
 * Generic ASTM plugin for dashboard-configured analyzers.
 *
 * <p>Feature: 004-analyzer-management + 011-madagascar-analyzer-integration
 *
 * <p>Unlike legacy plugins (HoribaPentra60, Sysmex, etc.) that hardcode analyzer identification and
 * test mappings, this generic plugin:
 *
 * <ul>
 *   <li>Reads identifier patterns from analyzer.identifier_pattern
 *   <li>Matches incoming ASTM messages against those patterns
 *   <li>Loads test mappings from analyzer_test_mapping table (configured via 004 Dashboard)
 * </ul>
 *
 * <p>This enables new analyzers to be added entirely through the UI without writing Java code.
 *
 * <p>Flow:
 *
 * <ol>
 *   <li>ASTM message arrives at /analyzer/astm endpoint
 *   <li>ASTMAnalyzerReader iterates all plugins (legacy first, then generic)
 *   <li>GenericASTMAnalyzer.isTargetAnalyzer() queries DB for pattern match
 *   <li>If match found, getAnalyzerLineInserter() returns inserter with matched analyzer ID
 *   <li>GenericASTMLineInserter loads mappings from DB and processes results
 * </ol>
 */
public class GenericASTMAnalyzer implements AnalyzerImporterPlugin {

  /** Plugin name for logging and identification */
  private static final String PLUGIN_NAME = "GenericASTM";

  /**
   * Thread-local storage for matched analyzer.
   *
   * <p>Since isTargetAnalyzer() and getAnalyzerLineInserter() are called separately by
   * ASTMAnalyzerReader, we need to preserve the matched analyzer between calls. Thread-local
   * ensures thread safety for concurrent requests.
   */
  private final ThreadLocal<Analyzer> matchedAnalyzer = new ThreadLocal<>();

  /**
   * Register the generic plugin with PluginAnalyzerService.
   *
   * <p>Unlike legacy plugins, this does NOT call addAnalyzerDatabaseParts() because:
   *
   * <ul>
   *   <li>Analyzers are created via 004 Dashboard, not plugin registration
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
        PLUGIN_NAME + " plugin registered - analyzers configured via 004 Dashboard");
    return true;
  }

  /**
   * Check if the ASTM message is from an analyzer configured for this generic plugin.
   *
   * <p>Identification strategy:
   *
   * <ol>
   *   <li>Parse ASTM H-segment for manufacturer/model identifier
   *   <li>Query analyzer table for generic plugin configs with matching identifier_pattern
   *   <li>If match found, store configuration and return true
   * </ol>
   *
   * <p>Note: This is called AFTER legacy plugins have had a chance to match. Legacy plugins always
   * get priority since they have more specific identification logic.
   *
   * @param lines ASTM message lines
   * @return true if message matches a generic plugin configuration
   */
  @Override
  public boolean isTargetAnalyzer(List<String> lines) {
    // Clear any previous match from thread-local
    matchedAnalyzer.remove();

    if (lines == null || lines.isEmpty()) {
      return false;
    }

    // Extract analyzer identifier from ASTM H-segment
    String analyzerIdentifier = parseAnalyzerIdentifier(lines);
    if (analyzerIdentifier == null || analyzerIdentifier.isEmpty()) {
      LogEvent.logDebug(
          this.getClass().getSimpleName(),
          "isTargetAnalyzer",
          "Could not extract analyzer identifier from ASTM header");
      return false;
    }

    // Query database for matching analyzer with identifier pattern
    try {
      AnalyzerService analyzerService = SpringContext.getBean(AnalyzerService.class);

      if (analyzerService == null) {
        LogEvent.logWarn(
            this.getClass().getSimpleName(), "isTargetAnalyzer", "AnalyzerService not available");
        return false;
      }

      Optional<Analyzer> analyzer =
          analyzerService.findByIdentifierPatternMatch(analyzerIdentifier);

      if (analyzer.isPresent()) {
        // Store matched analyzer for getAnalyzerLineInserter()
        matchedAnalyzer.set(analyzer.get());

        LogEvent.logDebug(
            this.getClass().getSimpleName(),
            "isTargetAnalyzer",
            "Matched analyzer identifier '"
                + analyzerIdentifier
                + "' to analyzer: "
                + analyzer.get().getName());
        return true;
      }

    } catch (Exception e) {
      LogEvent.logError(
          "Error checking generic plugin configuration for identifier: " + analyzerIdentifier, e);
    }

    return false;
  }

  /**
   * Check if the message contains analyzer results (R segments).
   *
   * @param lines ASTM message lines
   * @return true if message contains result records
   */
  @Override
  public boolean isAnalyzerResult(List<String> lines) {
    if (lines == null || lines.isEmpty()) {
      return false;
    }

    // Check for R (Result) segments - same logic as legacy plugins
    return lines.stream().anyMatch(line -> line != null && line.startsWith("R|"));
  }

  /**
   * Get the line inserter for processing results from the matched analyzer.
   *
   * @return GenericASTMLineInserter configured with the matched analyzer ID
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

    String analyzerId = analyzer.getAnalyzerType().getId();
    String analyzerName = analyzer.getAnalyzerType().getName();

    LogEvent.logDebug(
        this.getClass().getSimpleName(),
        "getAnalyzerLineInserter",
        "Creating inserter for analyzer: " + analyzerName + " (ID: " + analyzerId + ")");

    return new GenericASTMLineInserter(analyzerId, analyzerName);
  }

  /**
   * Parse analyzer identifier from ASTM H-segment header.
   *
   * <p>ASTM H-segment format: H|\^&|||MANUFACTURER^MODEL^VERSION|...
   *
   * <p>Returns the full field 4 content (e.g., "HORIBA^YUMIZEN H500^1.0") to allow flexible pattern
   * matching in analyzer.identifier_pattern.
   *
   * @param lines ASTM message lines
   * @return Analyzer identifier string, or null if not found
   */
  private String parseAnalyzerIdentifier(List<String> lines) {
    for (String line : lines) {
      if (line != null && line.startsWith("H|")) {
        String[] fields = line.split("\\|");
        // Field 4 (index 4) contains manufacturer/model info
        if (fields.length > 4 && fields[4] != null && !fields[4].trim().isEmpty()) {
          return fields[4].trim();
        }
      }
    }
    return null;
  }


  @Override
  public AnalyzerResponder getAnalyzerResponder() {
    Analyzer analyzer = matchedAnalyzer.get();
    if (analyzer == null) {
      LogEvent.logError(
          this.getClass().getSimpleName(),
          "getAnalyzerResponder",
          "No matched analyzer - isTargetAnalyzer() must be called first");
      throw new IllegalStateException("No matched analyzer");
    }

    String analyzerTypeId = analyzer.getAnalyzerType().getId();
    String analyzerTypeName = analyzer.getAnalyzerType().getName();

    return new GenericASTMResponder(analyzerTypeId, analyzerTypeName);
  }
}
