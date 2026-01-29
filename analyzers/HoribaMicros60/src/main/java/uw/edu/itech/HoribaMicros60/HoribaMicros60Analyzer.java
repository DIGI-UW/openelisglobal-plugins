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
package uw.edu.itech.HoribaMicros60;

import java.util.ArrayList;
import java.util.List;
import org.openelisglobal.analyzerimport.analyzerreaders.AnalyzerLineInserter;
import org.openelisglobal.common.services.PluginAnalyzerService;
import org.openelisglobal.plugin.AnalyzerImporterPlugin;

/**
 * External plugin JAR for Horiba ABX Micros 60 hematology analyzer.
 *
 * <p>Feature: 011-madagascar-analyzer-integration Milestone: M10 (Micros 60)
 *
 * <p>The Micros 60 is a 3-Part Differential hematology analyzer that communicates via ASTM LIS2-A2
 * protocol over RS232 serial connection. It produces 18 CBC parameters including the 3-part
 * differential (LYM, MXD, NEU).
 *
 * <p>Identification: ASTM Header contains "ABX^MICROS60" or "MICROS"
 *
 * <p>Reference: specs/011-madagascar-analyzer-integration/research.md Section 12
 */
public class HoribaMicros60Analyzer implements AnalyzerImporterPlugin {

  /** Analyzer name used for database registration and identification */
  public static final String ANALYZER_NAME = "Horiba ABX Micros 60";

  /** Description for database registration */
  public static final String ANALYZER_DESCRIPTION =
      "Horiba ABX Micros 60 3-Part Differential Hematology Analyzer (ASTM/RS232)";

  /** Primary ASTM header identifier */
  private static final String HEADER_ID_PRIMARY = "MICROS60";

  /** Fallback ASTM header identifier */
  private static final String HEADER_ID_FALLBACK = "MICROS";

  /** Manufacturer identifier in ASTM header */
  private static final String MANUFACTURER_ID = "ABX";

  /**
   * Register analyzer with PluginAnalyzerService. Called by PluginLoader after Spring context is
   * ready.
   *
   * @return true if registration successful
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
   * Create test mappings for Micros 60 CBC parameters.
   *
   * <p>Maps ASTM test codes to OpenELIS test names via LOINC codes. The Micros 60 produces 18
   * parameters: 12 CBC + 6 three-part differential. MXD (Mixed cells) parameters use the
   * two-argument constructor since there is no standard LOINC code for the combined
   * monocyte+eosinophil+basophil population.
   *
   * <p>Reference: specs/011-madagascar-analyzer-integration/research.md Section 12
   *
   * @return List of test mappings
   */
  private List<PluginAnalyzerService.TestMapping> createTestMappings() {
    List<PluginAnalyzerService.TestMapping> mappings = new ArrayList<>();

    // CBC parameters (shared with Pentra 60)
    mappings.add(new PluginAnalyzerService.TestMapping("WBC", "White Blood Cells", "6690-2"));
    mappings.add(new PluginAnalyzerService.TestMapping("RBC", "Red Blood Cells", "789-8"));
    mappings.add(new PluginAnalyzerService.TestMapping("HGB", "Hemoglobin", "718-7"));
    mappings.add(new PluginAnalyzerService.TestMapping("HCT", "Hematocrit", "4544-3"));
    mappings.add(new PluginAnalyzerService.TestMapping("MCV", "MCV", "787-2"));
    mappings.add(new PluginAnalyzerService.TestMapping("MCH", "MCH", "785-6"));
    mappings.add(new PluginAnalyzerService.TestMapping("MCHC", "MCHC", "786-4"));
    mappings.add(new PluginAnalyzerService.TestMapping("PLT", "Platelet Count", "777-3"));
    mappings.add(new PluginAnalyzerService.TestMapping("RDW", "RDW", "788-0"));
    mappings.add(new PluginAnalyzerService.TestMapping("MPV", "MPV", "32623-1"));

    // 3-Part Differential (LYM, MXD, NEU)
    mappings.add(new PluginAnalyzerService.TestMapping("LYM%", "Lymphocyte %", "736-9"));
    mappings.add(new PluginAnalyzerService.TestMapping("LYM#", "Lymphocyte Count", "731-0"));
    // MXD (Mixed cells = monocytes + eosinophils + basophils) — no standard LOINC
    mappings.add(new PluginAnalyzerService.TestMapping("MXD%", "Mixed Cell %"));
    mappings.add(new PluginAnalyzerService.TestMapping("MXD#", "Mixed Cell Count"));
    mappings.add(new PluginAnalyzerService.TestMapping("NEU%", "Neutrophil %", "770-8"));
    mappings.add(new PluginAnalyzerService.TestMapping("NEU#", "Neutrophil Count", "751-8"));

    return mappings;
  }

  /**
   * Check if the ASTM message is from a Horiba Micros 60 analyzer.
   *
   * <p>Identification strategy: 1. Look for "MICROS60" in ASTM H-segment (primary) 2. Look for
   * "ABX" + "MICROS" in H-segment, excluding "PENTRA" (fallback)
   *
   * @param lines ASTM message lines
   * @return true if message is from Micros 60
   */
  @Override
  public boolean isTargetAnalyzer(List<String> lines) {
    if (lines == null || lines.isEmpty()) {
      return false;
    }

    for (String line : lines) {
      if (line != null && line.startsWith("H|")) {
        String upperLine = line.toUpperCase();
        // Primary check: contains "MICROS60"
        if (upperLine.contains(HEADER_ID_PRIMARY)) {
          return true;
        }
        // Fallback: contains "ABX" and "MICROS" but not "PENTRA"
        if (upperLine.contains(MANUFACTURER_ID)
            && upperLine.contains(HEADER_ID_FALLBACK)
            && !upperLine.contains("PENTRA")) {
          return true;
        }
      }
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

    return lines.stream().anyMatch(line -> line != null && line.startsWith("R|"));
  }

  /**
   * Get the line inserter for processing Micros 60 results.
   *
   * @return HoribaMicros60AnalyzerLineInserter instance
   */
  @Override
  public AnalyzerLineInserter getAnalyzerLineInserter() {
    return new HoribaMicros60AnalyzerLineInserter();
  }
}
