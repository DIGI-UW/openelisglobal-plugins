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

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.openelisglobal.analyzerimport.analyzerreaders.AnalyzerLineInserter;
import org.openelisglobal.analyzerimport.analyzerreaders.AnalyzerReaderUtil;
import org.openelisglobal.analyzerimport.util.AnalyzerTestNameCache;
import org.openelisglobal.analyzerimport.util.MappedTestName;
import org.openelisglobal.analyzerresults.valueholder.AnalyzerResults;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.util.DateUtil;

/**
 * HL7 v2.x ORU^R01 result parser for generic dashboard-configured analyzers.
 *
 * <p>Feature: 011-madagascar-analyzer-integration (M19)
 *
 * <p>Database-driven HL7 result inserter that:
 *
 * <ul>
 *   <li>Parses OBX (Observation Result) segments from HL7 messages
 *   <li>Looks up test mappings from analyzer_test_mapping table via {@link AnalyzerTestNameCache}
 *   <li>Creates {@link AnalyzerResults} for each observation
 * </ul>
 *
 * <p>This enables new HL7 analyzers to be configured entirely through the Dashboard UI
 * without writing Java code.
 *
 * <p>HL7 segment parsing:
 *
 * <ul>
 *   <li>PID: Patient identification
 *   <li>OBR: Observation request (order info)
 *   <li>OBX: Observation result (test code, value, units) ← Primary focus
 * </ul>
 *
 * <p>Task Reference: T204 (M19) - Implement GenericHL7LineInserter with OBX parsing
 */
public class GenericHL7LineInserter extends AnalyzerLineInserter {

  /** The analyzer name for looking up test mappings */
  private final String analyzerName;

  // HL7 segment type identifiers
  private static final String OBX_SEGMENT = "OBX|";
  private static final String OBR_SEGMENT = "OBR|";
  private static final String PID_SEGMENT = "PID|";

  // HL7 delimiters
  private static final String FIELD_DELIMITER = "|";
  private static final String COMPONENT_DELIMITER = "^";

  // OBX segment field indices (1-based per HL7 spec, but 0-based after split)
  // OBX|1|NM|WBC||7.5|10^3/uL|4.0-11.0|N|||F
  // Field 0: "OBX"
  // Field 1: Set ID
  // Field 2: Value Type (NM, ST, CE, etc.)
  // Field 3: Observation Identifier (test code)
  // Field 4: Observation Sub-ID (usually empty)
  // Field 5: Observation Value
  // Field 6: Units
  private static final int OBX_SET_ID_FIELD = 1;
  private static final int OBX_VALUE_TYPE_FIELD = 2;
  private static final int OBX_TEST_CODE_FIELD = 3;
  private static final int OBX_VALUE_FIELD = 5;
  private static final int OBX_UNITS_FIELD = 6;

  private final AnalyzerReaderUtil readerUtil = new AnalyzerReaderUtil();
  private String errorMessage;

  /**
   * Create a new GenericHL7LineInserter for the specified analyzer.
   *
   * @param analyzerId The analyzer ID (kept for consistency with GenericASTM)
   * @param analyzerName The analyzer name for looking up test mappings in AnalyzerTestNameCache
   */
  public GenericHL7LineInserter(String analyzerId, String analyzerName) {
    this.analyzerName = analyzerName;
  }

  /**
   * Parse HL7 message lines and persist results.
   *
   * <p>Flow:
   *
   * <ol>
   *   <li>Parse OBX segments to extract test codes and values
   *   <li>Look up test mappings from AnalyzerTestNameCache (populated from DB)
   *   <li>Create AnalyzerResults with mapped test IDs
   *   <li>Persist using AnalyzerReaderUtil
   * </ol>
   *
   * @param lines HL7 message segment lines (MSH|..., PID|..., OBX|..., etc.)
   * @param systemUserId System user ID for audit trail
   * @return true if all results processed successfully
   */
  @Override
  public boolean insert(List<String> lines, String systemUserId) {
    errorMessage = null;

    if (lines == null || lines.isEmpty()) {
      errorMessage = "No lines to process";
      return false;
    }

    List<AnalyzerResults> results = new ArrayList<>();
    String accessionNumber = null;
    Timestamp testDate = new Timestamp(System.currentTimeMillis());

    // Parse HL7 segments
    for (String line : lines) {
      if (line == null) {
        continue;
      }

      // Extract accession number from OBR segment (if present)
      if (line.startsWith(OBR_SEGMENT)) {
        accessionNumber = parseAccessionFromOBR(line);
      }

      // Parse OBX result segments
      if (line.startsWith(OBX_SEGMENT)) {
        AnalyzerResults result = parseObxSegment(line, accessionNumber, testDate);
        if (result != null) {
          results.add(result);
        }
      }
    }

    // Validate we found results
    if (results.isEmpty()) {
      errorMessage = "No OBX segments found in HL7 message";
      LogEvent.logWarn(
          this.getClass().getSimpleName(),
          "insert",
          errorMessage);
      return false;
    }

    // Log results found
    LogEvent.logDebug(
        this.getClass().getSimpleName(),
        "insert",
        "Found " + results.size() + " results for analyzer: " + analyzerName);

    // Persist results using base class method
    return persistImport(systemUserId, results);
  }

  @Override
  public String getError() {
    return errorMessage;
  }

  /**
   * Parse OBX segment to extract test result.
   *
   * <p>OBX format: OBX|1|NM|WBC||7.5|10^3/uL|4.0-11.0|N|||F
   *
   * @param obxLine OBX segment line
   * @param accessionNumber Accession number from OBR segment (may be null)
   * @param testDate Test completion timestamp
   * @return AnalyzerResults object, or null if parsing fails
   */
  private AnalyzerResults parseObxSegment(String obxLine, String accessionNumber, Timestamp testDate) {
    String[] fields = obxLine.split("\\|", -1); // -1 to preserve trailing empty fields

    if (fields.length < 6) {
      LogEvent.logWarn(
          this.getClass().getSimpleName(),
          "parseObxSegment",
          "OBX segment has insufficient fields: " + obxLine);
      return null;
    }

    // Extract test code from OBX-3
    String testCode = extractTestCode(fields[OBX_TEST_CODE_FIELD]);
    if (StringUtils.isBlank(testCode)) {
      LogEvent.logWarn(
          this.getClass().getSimpleName(),
          "parseObxSegment",
          "OBX segment missing test code: " + obxLine);
      return null;
    }

    // Look up test mapping from database
    MappedTestName mappedTest = AnalyzerTestNameCache.getInstance()
        .getMappedTest(analyzerName, testCode);

    if (mappedTest == null) {
      // No mapping found - create empty mapping for manual configuration
      mappedTest = AnalyzerTestNameCache.getInstance()
          .getEmptyMappedTestName(analyzerName, testCode);
      LogEvent.logDebug(
          this.getClass().getSimpleName(),
          "parseObxSegment",
          "No mapping found for test code '" + testCode + "', creating empty mapping");
    }

    // Extract value from OBX-5
    String value = fields.length > OBX_VALUE_FIELD ? fields[OBX_VALUE_FIELD].trim() : "";
    if (StringUtils.isBlank(value)) {
      LogEvent.logWarn(
          this.getClass().getSimpleName(),
          "parseObxSegment",
          "OBX segment has empty value for test: " + testCode);
      return null;
    }

    // Extract units from OBX-6 (first component if multi-component)
    String units = "";
    if (fields.length > OBX_UNITS_FIELD && !StringUtils.isBlank(fields[OBX_UNITS_FIELD])) {
      String[] unitComponents = fields[OBX_UNITS_FIELD].split("\\^");
      units = unitComponents[0].trim();
    }

    // Create AnalyzerResults
    AnalyzerResults result = new AnalyzerResults();
    result.setTestId(mappedTest.getTestId());
    result.setTestName(mappedTest.getOpenElisTestName());
    result.setResult(value);
    result.setUnits(units);
    result.setCompleteDate(testDate);
    result.setAccessionNumber(accessionNumber);

    LogEvent.logDebug(
        this.getClass().getSimpleName(),
        "parseObxSegment",
        "Parsed result: test=" + testCode + " → " + mappedTest.getOpenElisTestName()
            + ", value=" + value + ", units=" + units);

    return result;
  }

  /**
   * Extract test code from OBX-3 field.
   *
   * <p>OBX-3 may contain multiple components: WBC^White Blood Cells^L
   * We extract the first component (identifier).
   *
   * @param obx3Field OBX-3 field value
   * @return Test code identifier
   */
  private String extractTestCode(String obx3Field) {
    if (StringUtils.isBlank(obx3Field)) {
      return null;
    }

    // Extract first component if multi-component
    String[] components = obx3Field.split("\\^");
    return components[0].trim();
  }

  /**
   * Parse accession number from OBR segment.
   *
   * <p>OBR format: OBR|1||ORDER123|PANEL^CBC Panel|||20260202115900
   * - Field 2: Placer order number (may contain accession)
   * - Field 3: Filler order number (preferred for accession)
   *
   * @param obrLine OBR segment line
   * @return Accession number, or null if not found
   */
  private String parseAccessionFromOBR(String obrLine) {
    String[] fields = obrLine.split("\\|", -1);

    // Try filler order number first (field 3)
    if (fields.length > 3 && !StringUtils.isBlank(fields[3])) {
      return fields[3].trim();
    }

    // Fall back to placer order number (field 2)
    if (fields.length > 2 && !StringUtils.isBlank(fields[2])) {
      return fields[2].trim();
    }

    return null;
  }
}
