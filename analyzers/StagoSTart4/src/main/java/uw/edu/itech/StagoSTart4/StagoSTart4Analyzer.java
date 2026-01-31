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
package uw.edu.itech.StagoSTart4;

import java.util.ArrayList;
import java.util.List;
import org.openelisglobal.analyzerimport.analyzerreaders.AnalyzerLineInserter;
import org.openelisglobal.common.services.PluginAnalyzerService;
import org.openelisglobal.plugin.AnalyzerImporterPlugin;

/**
 * External plugin JAR for Stago STart 4 coagulation analyzer.
 *
 * <p>Feature: 011-madagascar-analyzer-integration Milestone: M11 (Stago STart 4)
 *
 * <p>The STart 4 is a coagulation/hemostasis analyzer that supports dual-protocol communication:
 * - ASTM LIS2-A2 over RS232 serial connection
 * - HL7 v2.5 over Network (TCP/IP)
 *
 * <p>It produces coagulation test results including PT (Prothrombin Time), INR (International
 * Normalized Ratio), APTT (Activated Partial Thromboplastin Time), Fibrinogen, and TT (Thrombin
 * Time).
 *
 * <p>Identification:
 * - ASTM: Header contains "STAGO^START4" or "START4"
 * - HL7: MSH segment sending application contains "STAGO"
 *
 * <p>Reference: specs/011-madagascar-analyzer-integration/research.md
 */
public class StagoSTart4Analyzer implements AnalyzerImporterPlugin {

  /** Analyzer name used for database registration and identification */
  public static final String ANALYZER_NAME = "Stago STart 4";

  /** Description for database registration */
  public static final String ANALYZER_DESCRIPTION =
      "Stago STart 4 Coagulation Analyzer (ASTM/HL7 over RS232/Network)";

  /** Primary ASTM header identifier */
  private static final String HEADER_ID_PRIMARY = "START4";

  /** Fallback ASTM header identifier */
  private static final String HEADER_ID_FALLBACK = "STAGO";

  /** HL7 sending application identifier */
  private static final String HL7_SENDING_APP = "STAGO";

  /**
   * Register analyzer with PluginAnalyzerService. Called by PluginLoader after Spring context is
   * ready.
   *
   * @return true if connection successful
   */
  @Override
  public boolean connect() {
    List<PluginAnalyzerService.TestMapping> testMappings = createTestMappings();

    PluginAnalyzerService.getInstance()
        .addAnalyzerDatabaseParts(ANALYZER_NAME, ANALYZER_DESCRIPTION, testMappings, true);

    PluginAnalyzerService.getInstance().registerAnalyzer(this);

    return true;
  }

  /**
   * Create test mappings for Stago STart 4 coagulation parameters.
   *
   * <p>Maps analyzer test codes to OpenELIS test names via LOINC codes. The STart 4 produces
   * coagulation tests including PT, INR, APTT, Fibrinogen, and TT.
   *
   * <p>Reference: specs/011-madagascar-analyzer-integration/research.md
   *
   * @return List of test mappings
   */
  private List<PluginAnalyzerService.TestMapping> createTestMappings() {
    List<PluginAnalyzerService.TestMapping> mappings = new ArrayList<>();

    // Coagulation parameters
    mappings.add(new PluginAnalyzerService.TestMapping("PT", "Prothrombin Time", "5902-2"));
    mappings.add(new PluginAnalyzerService.TestMapping("INR", "International Normalized Ratio", "6301-6"));
    mappings.add(new PluginAnalyzerService.TestMapping("APTT", "Activated Partial Thromboplastin Time", "3173-2"));
    mappings.add(new PluginAnalyzerService.TestMapping("FIB", "Fibrinogen", "3255-7"));
    mappings.add(new PluginAnalyzerService.TestMapping("TT", "Thrombin Time", "3174-0"));

    return mappings;
  }

  /**
   * Check if the message is from a Stago STart 4 analyzer.
   *
   * <p>Supports dual-protocol identification:
   * 1. ASTM: Look for "START4" or "STAGO" in H-segment (primary)
   * 2. HL7: Look for "STAGO" in MSH segment sending application field (fallback)
   *
   * @param lines Message lines (ASTM or HL7 format)
   * @return true if message is from STart 4
   */
  @Override
  public boolean isTargetAnalyzer(List<String> lines) {
    if (lines == null || lines.isEmpty()) {
      return false;
    }

    // Check for ASTM format (H-segment)
    for (String line : lines) {
      if (line != null && line.startsWith("H|")) {
        String upperLine = line.toUpperCase();
        // Primary check: contains "START4"
        if (upperLine.contains(HEADER_ID_PRIMARY)) {
          return true;
        }
        // Fallback: contains "STAGO"
        if (upperLine.contains(HEADER_ID_FALLBACK)) {
          return true;
        }
      }
    }

    // Check for HL7 format (MSH segment)
    for (String line : lines) {
      if (line != null && line.startsWith("MSH|")) {
        String[] fields = line.split("\\|");
        // MSH field 3 is sending application
        if (fields.length > 3 && fields[3].toUpperCase().contains(HL7_SENDING_APP)) {
          return true;
        }
      }
    }

    return false;
  }

  /**
   * Check if the message contains analyzer results.
   *
   * <p>Supports both ASTM (R segments) and HL7 (OBX segments) formats.
   *
   * @param lines Message lines
   * @return true if message contains result records
   */
  @Override
  public boolean isAnalyzerResult(List<String> lines) {
    if (lines == null || lines.isEmpty()) {
      return false;
    }

    // Check for ASTM R (Result) segments or HL7 OBX (Observation) segments
    return lines.stream()
        .anyMatch(
            line ->
                line != null
                    && (line.startsWith("R|") || line.startsWith("OBX|")));
  }

  /**
   * Get the line inserter for processing STart 4 results.
   *
   * <p>The inserter supports both ASTM and HL7 message formats.
   *
   * @return StagoSTart4AnalyzerLineInserter instance
   */
  @Override
  public AnalyzerLineInserter getAnalyzerLineInserter() {
    return new StagoSTart4AnalyzerLineInserter();
  }
}
