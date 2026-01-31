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

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import org.openelisglobal.analyzerimport.analyzerreaders.AnalyzerLineInserter;
import org.openelisglobal.analyzerimport.analyzerreaders.AnalyzerReaderUtil;
import org.openelisglobal.analyzerimport.util.AnalyzerTestNameCache;
import org.openelisglobal.analyzerimport.util.MappedTestName;
import org.openelisglobal.analyzerresults.valueholder.AnalyzerResults;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.util.DateUtil;

/**
 * Dual-protocol result parser for Stago STart 4 coagulation analyzer.
 *
 * <p>Supports both ASTM LIS2-A2 and HL7 v2.5 message formats. Automatically detects the message
 * format and parses accordingly.
 *
 * <p>ASTM format: Parses H (Header), P (Patient), O (Order), R (Result), and L (Terminator)
 * segments. Extracts test results from R segments.
 *
 * <p>HL7 format: Parses MSH, PID, ORC, OBR, and OBX segments. Extracts test results from OBX
 * segments.
 *
 * <p>ASTM R-segment format: {@code R|seq|^^^TEST_CODE|value|units|ref_range|flag||status|timestamp}
 *
 * <p>HL7 OBX format: {@code OBX|seq|valueType|testCode|testName||value|units|...}
 *
 * <p>Feature: 011-madagascar-analyzer-integration Milestone: M11 (Stago STart 4)
 *
 * <p>Reference: specs/011-madagascar-analyzer-integration/research.md
 */
public class StagoSTart4AnalyzerLineInserter extends AnalyzerLineInserter {

  private static final String ANALYZER_NAME = StagoSTart4Analyzer.ANALYZER_NAME;

  // ASTM segment type identifiers
  private static final String ORDER_SEGMENT = "O";
  private static final String RESULT_SEGMENT = "R";
  private static final String TERMINATOR_SEGMENT = "L";

  // ASTM delimiters
  private static final String FIELD_DELIMITER = "|";
  private static final String COMPONENT_DELIMITER = "^";

  // ASTM R-segment field indices (0-based after split by FIELD_DELIMITER)
  private static final int R_TEST_ID_FIELD = 2;
  private static final int R_VALUE_FIELD = 3;
  private static final int R_UNITS_FIELD = 4;
  private static final int R_TIMESTAMP_FIELD = 9;

  // ASTM O-segment field indices
  private static final int O_SPECIMEN_ID_FIELD = 2;

  // HL7 segment identifiers
  private static final String HL7_MSH_SEGMENT = "MSH";
  private static final String HL7_PID_SEGMENT = "PID";
  private static final String HL7_OBR_SEGMENT = "OBR";
  private static final String HL7_OBX_SEGMENT = "OBX";

  // HL7 OBX field indices (0-based after split by |)
  private static final int OBX_VALUE_TYPE_FIELD = 2;
  private static final int OBX_TEST_CODE_FIELD = 3;
  private static final int OBX_VALUE_FIELD = 5;
  private static final int OBX_UNITS_FIELD = 6;

  // Timestamp format used by Stago ASTM messages
  private static final String ASTM_TIMESTAMP_PATTERN = "yyyyMMddHHmmss";

  private final AnalyzerReaderUtil readerUtil = new AnalyzerReaderUtil();

  /**
   * Parse message lines (ASTM or HL7 format) and persist coagulation results.
   *
   * <p>Automatically detects message format by checking the first line:
   * - Starts with "H|" → ASTM format
   * - Starts with "MSH|" → HL7 format
   *
   * @param lines Message lines (ASTM or HL7 format)
   * @param currentUserId the user performing the import
   * @return true if results were persisted successfully
   */
  @Override
  public boolean insert(List<String> lines, String currentUserId) {
    if (lines == null || lines.isEmpty()) {
      return false;
    }

    // Detect message format
    boolean isHL7 = detectHL7Format(lines);
    if (isHL7) {
      return insertHL7(lines, currentUserId);
    } else {
      return insertASTM(lines, currentUserId);
    }
  }

  @Override
  public String getError() {
    return "Stago STart 4 analyzer unable to write to database";
  }

  /**
   * Detect if the message is in HL7 format.
   *
   * @param lines Message lines
   * @return true if message is HL7 format
   */
  private boolean detectHL7Format(List<String> lines) {
    return lines.stream().anyMatch(line -> line != null && line.startsWith("MSH|"));
  }

  /**
   * Parse ASTM LIS2-A2 message lines and persist coagulation results.
   *
   * <p>Iterates through ASTM segments, tracking the current O (Order) segment for accession number
   * context, and creating {@link AnalyzerResults} from each R (Result) segment.
   *
   * @param lines ASTM message lines (H, P, O, R, L segments)
   * @param currentUserId the user performing the import
   * @return true if results were persisted successfully
   */
  private boolean insertASTM(List<String> lines, String currentUserId) {
    List<AnalyzerResults> results = new ArrayList<>();
    String orderRecord = null;

    for (String line : lines) {
      if (line == null || line.isEmpty()) {
        continue;
      }

      String segment = getSegmentType(line);

      switch (segment) {
        case ORDER_SEGMENT:
          orderRecord = line;
          break;
        case RESULT_SEGMENT:
          if (orderRecord != null) {
            addResultFromASTMLine(orderRecord, line, results);
          } else {
            LogEvent.logWarn(
                this.getClass().getSimpleName(),
                "insertASTM",
                "R segment without preceding O segment, skipping");
          }
          break;
        case TERMINATOR_SEGMENT:
          break;
        default:
          // H, P, and comment lines are not needed for result import
          break;
      }
    }

    return persistImport(currentUserId, results);
  }

  /**
   * Parse HL7 v2.5 message lines and persist coagulation results.
   *
   * <p>Extracts patient ID from PID segment, accession number from OBR segment, and test results
   * from OBX segments.
   *
   * @param lines HL7 message lines (MSH, PID, ORC, OBR, OBX segments)
   * @param currentUserId the user performing the import
   * @return true if results were persisted successfully
   */
  private boolean insertHL7(List<String> lines, String currentUserId) {
    List<AnalyzerResults> results = new ArrayList<>();
    String accessionNumber = null;
    String patientId = null;

    for (String line : lines) {
      if (line == null || line.isEmpty()) {
        continue;
      }

      if (line.startsWith(HL7_PID_SEGMENT + "|")) {
        // Extract patient ID from PID segment (field 3)
        String[] fields = line.split("\\|");
        if (fields.length > 3 && fields[3] != null && !fields[3].isEmpty()) {
          // PID field 3 format: ID^^^ASSIGNING_AUTHORITY
          String[] components = fields[3].split(Pattern.quote("^"));
          patientId = components[0].trim();
        }
      } else if (line.startsWith(HL7_OBR_SEGMENT + "|")) {
        // Extract accession number from OBR segment (field 3 - Filler Order Number)
        String[] fields = line.split("\\|");
        if (fields.length > 3 && fields[3] != null && !fields[3].isEmpty()) {
          accessionNumber = fields[3].trim();
        }
      } else if (line.startsWith(HL7_OBX_SEGMENT + "|")) {
        // Extract test result from OBX segment
        if (accessionNumber == null && patientId != null) {
          accessionNumber = patientId;
        }
        if (accessionNumber == null) {
          accessionNumber = "HL7-UNKNOWN";
        }
        addResultFromHL7Line(accessionNumber, line, results);
      }
    }

    return persistImport(currentUserId, results);
  }

  /**
   * Extract the ASTM segment type identifier from a line.
   *
   * @param line ASTM record line (e.g., "R|1|^^^PT|12.5|...")
   * @return segment type string (e.g., "R") or first character if no delimiter
   */
  private String getSegmentType(String line) {
    int delimiterPos = line.indexOf(FIELD_DELIMITER);
    if (delimiterPos > 0) {
      return line.substring(0, delimiterPos);
    }
    return line.length() > 0 ? line.substring(0, 1) : "";
  }

  /**
   * Parse one ASTM R (Result) segment and add the result to the accumulator list.
   *
   * <p>Extracts: test code from field 2 component 4 ({@code ^^^PT}), result value from field 3,
   * units from field 4, and completion timestamp from field 9.
   *
   * @param orderRecord the current O-segment (provides accession number)
   * @param resultRecord the R-segment to parse
   * @param results accumulator list for parsed results
   */
  private void addResultFromASTMLine(
      String orderRecord, String resultRecord, List<AnalyzerResults> results) {

    String accessionNumber = extractAccessionNumber(orderRecord);
    if (accessionNumber == null || accessionNumber.isEmpty()) {
      LogEvent.logWarn(
          this.getClass().getSimpleName(),
          "addResultFromASTMLine",
          "Could not extract accession number from O segment");
      return;
    }

    String[] resultFields = resultRecord.split(Pattern.quote(FIELD_DELIMITER));

    String testCode = extractTestCode(resultFields);
    if (testCode == null || testCode.isEmpty()) {
      LogEvent.logWarn(
          this.getClass().getSimpleName(),
          "addResultFromASTMLine",
          "Could not extract test code from R segment");
      return;
    }

    String resultValue = resultFields.length > R_VALUE_FIELD ? resultFields[R_VALUE_FIELD] : "";
    String units = resultFields.length > R_UNITS_FIELD ? resultFields[R_UNITS_FIELD] : "";
    String timestampStr =
        resultFields.length > R_TIMESTAMP_FIELD ? resultFields[R_TIMESTAMP_FIELD] : "";

    // Look up the test mapping registered by StagoSTart4Analyzer.connect()
    MappedTestName mappedTest =
        AnalyzerTestNameCache.getInstance().getMappedTest(ANALYZER_NAME, testCode);

    if (mappedTest == null) {
      mappedTest =
          AnalyzerTestNameCache.getInstance().getEmptyMappedTestName(ANALYZER_NAME, testCode);
    }

    AnalyzerResults analyzerResult = new AnalyzerResults();
    analyzerResult.setAnalyzerId(mappedTest.getAnalyzerId());
    analyzerResult.setTestId(mappedTest.getTestId());
    analyzerResult.setTestName(mappedTest.getOpenElisTestName());
    analyzerResult.setResult(resultValue);
    analyzerResult.setUnits(units);
    analyzerResult.setAccessionNumber(accessionNumber);
    analyzerResult.setIsControl(false);

    Timestamp completeDate =
        DateUtil.convertStringDateToTimestampWithPattern(timestampStr, ASTM_TIMESTAMP_PATTERN);
    analyzerResult.setCompleteDate(completeDate);

    results.add(analyzerResult);

    // Check for existing result in database (for duplicate detection)
    AnalyzerResults resultFromDB = readerUtil.createAnalyzerResultFromDB(analyzerResult);
    if (resultFromDB != null) {
      results.add(resultFromDB);
    }

    LogEvent.logDebug(
        this.getClass().getSimpleName(),
        "addResultFromASTMLine",
        "Added result: "
            + testCode
            + " = "
            + resultValue
            + " "
            + units
            + " for accession "
            + accessionNumber);
  }

  /**
   * Parse one HL7 OBX (Observation) segment and add the result to the accumulator list.
   *
   * <p>Extracts: test code from field 3 component 4 ({@code ^^^PT^PROTHROMBIN TIME}), result value
   * from field 5, and units from field 6.
   *
   * @param accessionNumber the accession number (from OBR or PID)
   * @param obxRecord the OBX-segment to parse
   * @param results accumulator list for parsed results
   */
  private void addResultFromHL7Line(
      String accessionNumber, String obxRecord, List<AnalyzerResults> results) {

    String[] fields = obxRecord.split("\\|");

    if (fields.length <= OBX_TEST_CODE_FIELD) {
      LogEvent.logWarn(
          this.getClass().getSimpleName(),
          "addResultFromHL7Line",
          "OBX segment has insufficient fields");
      return;
    }

    // Extract test code from OBX field 3 (format: ^^^TEST_CODE^TEST_NAME)
    String testCodeField = fields[OBX_TEST_CODE_FIELD];
    String testCode = extractHL7TestCode(testCodeField);
    if (testCode == null || testCode.isEmpty()) {
      LogEvent.logWarn(
          this.getClass().getSimpleName(),
          "addResultFromHL7Line",
          "Could not extract test code from OBX segment");
      return;
    }

    String resultValue = fields.length > OBX_VALUE_FIELD ? fields[OBX_VALUE_FIELD] : "";
    String units = fields.length > OBX_UNITS_FIELD ? fields[OBX_UNITS_FIELD] : "";

    // Look up the test mapping registered by StagoSTart4Analyzer.connect()
    MappedTestName mappedTest =
        AnalyzerTestNameCache.getInstance().getMappedTest(ANALYZER_NAME, testCode);

    if (mappedTest == null) {
      mappedTest =
          AnalyzerTestNameCache.getInstance().getEmptyMappedTestName(ANALYZER_NAME, testCode);
    }

    AnalyzerResults analyzerResult = new AnalyzerResults();
    analyzerResult.setAnalyzerId(mappedTest.getAnalyzerId());
    analyzerResult.setTestId(mappedTest.getTestId());
    analyzerResult.setTestName(mappedTest.getOpenElisTestName());
    analyzerResult.setResult(resultValue);
    analyzerResult.setUnits(units);
    analyzerResult.setAccessionNumber(accessionNumber);
    analyzerResult.setIsControl(false);
    analyzerResult.setCompleteDate(new Timestamp(System.currentTimeMillis()));

    results.add(analyzerResult);

    // Check for existing result in database (for duplicate detection)
    AnalyzerResults resultFromDB = readerUtil.createAnalyzerResultFromDB(analyzerResult);
    if (resultFromDB != null) {
      results.add(resultFromDB);
    }

    LogEvent.logDebug(
        this.getClass().getSimpleName(),
        "addResultFromHL7Line",
        "Added result: "
            + testCode
            + " = "
            + resultValue
            + " "
            + units
            + " for accession "
            + accessionNumber);
  }

  /**
   * Extract the accession number from an ASTM O (Order) segment.
   *
   * <p>ASTM O-segment format: {@code O|seq|specimen_id^location|...} The accession number is the
   * first component of field 2 (before '^').
   *
   * @param orderRecord the O-segment line
   * @return accession number string, or null if not found
   */
  private String extractAccessionNumber(String orderRecord) {
    String[] fields = orderRecord.split(Pattern.quote(FIELD_DELIMITER));
    if (fields.length > O_SPECIMEN_ID_FIELD) {
      String specimenId = fields[O_SPECIMEN_ID_FIELD];
      String[] components = specimenId.split(Pattern.quote(COMPONENT_DELIMITER));
      return components[0].trim();
    }
    return null;
  }

  /**
   * Extract the test code from ASTM R-segment fields.
   *
   * <p>Universal Test ID format: {@code ^^^TEST_CODE} The test code is the 4th component (index 3)
   * of field 2.
   *
   * @param resultFields the R-segment split by field delimiter
   * @return test code (e.g., "PT", "INR") or null if not found
   */
  private String extractTestCode(String[] resultFields) {
    if (resultFields.length > R_TEST_ID_FIELD) {
      String[] components = resultFields[R_TEST_ID_FIELD].split(Pattern.quote(COMPONENT_DELIMITER));
      if (components.length >= 4) {
        return components[3].trim();
      }
    }
    return null;
  }

  /**
   * Extract the test code from HL7 OBX test code field.
   *
   * <p>OBX field 3 format: {@code ^^^TEST_CODE^TEST_NAME} The test code is the 4th component
   * (index 3) or can be extracted from the test name if code is missing.
   *
   * @param testCodeField the OBX field 3 value
   * @return test code (e.g., "PT", "INR") or null if not found
   */
  private String extractHL7TestCode(String testCodeField) {
    if (testCodeField == null || testCodeField.isEmpty()) {
      return null;
    }

    String[] components = testCodeField.split(Pattern.quote("^"));
    // Try component 4 first (standard HL7 format: ^^^TEST_CODE)
    if (components.length >= 4 && components[3] != null && !components[3].trim().isEmpty()) {
      return components[3].trim();
    }
    // Fallback: try component 1 if it's a simple code
    if (components.length >= 1 && components[0] != null && !components[0].trim().isEmpty()) {
      return components[0].trim();
    }
    return null;
  }
}
