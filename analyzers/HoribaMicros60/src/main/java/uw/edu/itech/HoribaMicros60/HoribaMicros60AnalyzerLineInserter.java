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
 * ASTM LIS2-A2 result parser for Horiba ABX Micros 60 hematology analyzer.
 *
 * <p>Parses ASTM messages containing H (Header), P (Patient), O (Order), R (Result), and L
 * (Terminator) segments. Extracts test results from R segments and maps them to OpenELIS tests via
 * {@link AnalyzerTestNameCache}.
 *
 * <p>ASTM R-segment format: {@code R|seq|^^^TEST_CODE|value|units|ref_range|flag||status|timestamp}
 *
 * <p>Feature: 011-madagascar-analyzer-integration Milestone: M10 (Micros 60)
 *
 * <p>Reference: specs/011-madagascar-analyzer-integration/research.md Section 12
 */
public class HoribaMicros60AnalyzerLineInserter extends AnalyzerLineInserter {

  private static final String ANALYZER_NAME = HoribaMicros60Analyzer.ANALYZER_NAME;

  // ASTM segment type identifiers
  private static final String ORDER_SEGMENT = "O";
  private static final String RESULT_SEGMENT = "R";
  private static final String TERMINATOR_SEGMENT = "L";

  // ASTM delimiters
  private static final String FIELD_DELIMITER = "|";
  private static final String COMPONENT_DELIMITER = "^";

  // R-segment field indices (0-based after split by FIELD_DELIMITER)
  private static final int R_TEST_ID_FIELD = 2;
  private static final int R_VALUE_FIELD = 3;
  private static final int R_UNITS_FIELD = 4;
  private static final int R_TIMESTAMP_FIELD = 9;

  // O-segment field indices
  private static final int O_SPECIMEN_ID_FIELD = 2;

  // Timestamp format used by Horiba ASTM messages
  private static final String ASTM_TIMESTAMP_PATTERN = "yyyyMMddHHmmss";

  private final AnalyzerReaderUtil readerUtil = new AnalyzerReaderUtil();

  /**
   * Parse ASTM message lines and persist hematology results.
   *
   * <p>Iterates through ASTM segments, tracking the current O (Order) segment for accession number
   * context, and creating {@link AnalyzerResults} from each R (Result) segment.
   *
   * @param lines ASTM message lines (H, P, O, R, L segments)
   * @param currentUserId the user performing the import
   * @return true if results were persisted successfully
   */
  @Override
  public boolean insert(List<String> lines, String currentUserId) {
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
            addResultFromLine(orderRecord, line, results);
          } else {
            LogEvent.logWarn(
                this.getClass().getSimpleName(),
                "insert",
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

  @Override
  public String getError() {
    return "Horiba ABX Micros 60 analyzer unable to write to database";
  }

  /**
   * Extract the ASTM segment type identifier from a line.
   *
   * @param line ASTM record line (e.g., "R|1|^^^WBC|6.2|...")
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
   * Parse one R (Result) segment and add the result to the accumulator list.
   *
   * <p>Extracts: test code from field 2 component 4 ({@code ^^^WBC}), result value from field 3,
   * units from field 4, and completion timestamp from field 9.
   *
   * @param orderRecord the current O-segment (provides accession number)
   * @param resultRecord the R-segment to parse
   * @param results accumulator list for parsed results
   */
  private void addResultFromLine(
      String orderRecord, String resultRecord, List<AnalyzerResults> results) {

    String accessionNumber = extractAccessionNumber(orderRecord);
    if (accessionNumber == null || accessionNumber.isEmpty()) {
      LogEvent.logWarn(
          this.getClass().getSimpleName(),
          "addResultFromLine",
          "Could not extract accession number from O segment");
      return;
    }

    String[] resultFields = resultRecord.split(Pattern.quote(FIELD_DELIMITER));

    String testCode = extractTestCode(resultFields);
    if (testCode == null || testCode.isEmpty()) {
      LogEvent.logWarn(
          this.getClass().getSimpleName(),
          "addResultFromLine",
          "Could not extract test code from R segment");
      return;
    }

    String resultValue = resultFields.length > R_VALUE_FIELD ? resultFields[R_VALUE_FIELD] : "";
    String units = resultFields.length > R_UNITS_FIELD ? resultFields[R_UNITS_FIELD] : "";
    String timestampStr =
        resultFields.length > R_TIMESTAMP_FIELD ? resultFields[R_TIMESTAMP_FIELD] : "";

    // Look up the test mapping registered by HoribaMicros60Analyzer.connect()
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
        "addResultFromLine",
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
   * Extract the accession number from an O (Order) segment.
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
   * Extract the test code from R-segment fields.
   *
   * <p>Universal Test ID format: {@code ^^^TEST_CODE} The test code is the 4th component (index 3)
   * of field 2.
   *
   * @param resultFields the R-segment split by field delimiter
   * @return test code (e.g., "WBC", "MXD%") or null if not found
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
}
