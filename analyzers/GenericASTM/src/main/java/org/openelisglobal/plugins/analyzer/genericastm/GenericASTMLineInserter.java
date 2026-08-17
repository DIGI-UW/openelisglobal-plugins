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
 * ASTM LIS2-A2 result parser for generic dashboard-configured analyzers.
 *
 * <p>Feature: 004-analyzer-management + 011-madagascar-analyzer-integration
 *
 * <p>This inserter uses the same infrastructure as legacy plugin inserters:
 *
 * <ul>
 *   <li>{@link AnalyzerTestNameCache} for looking up test mappings
 *   <li>{@link MappedTestName} for mapping details
 *   <li>{@link AnalyzerReaderUtil} for duplicate detection
 * </ul>
 *
 * <p>The key difference is that mappings for generic analyzers are configured via the 004 Dashboard
 * UI instead of being hardcoded in the plugin's connect() method.
 *
 * <p>ASTM segment parsing:
 *
 * <ul>
 *   <li>O-segment: provides accession number
 *   <li>R-segment: provides test code and result value
 *   <li>L-segment: terminates the message
 * </ul>
 */
public class GenericASTMLineInserter extends AnalyzerLineInserter {

  /** The analyzer name for looking up test mappings */
  private final String analyzerName;

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
  private static final int O_ACTION_CODE_FIELD = 11; // O.12 (1-based) = index 11 (0-based)

  // Common ASTM timestamp formats
  private static final String ASTM_TIMESTAMP_PATTERN = "yyyyMMddHHmmss";
  private static final String ASTM_TIMESTAMP_SHORT = "yyyyMMdd";

  private final AnalyzerReaderUtil readerUtil = new AnalyzerReaderUtil();
  private String errorMessage;

  /**
   * Create a new GenericASTMLineInserter for the specified analyzer.
   *
   * @param analyzerId The analyzer ID (not used directly - kept for consistency)
   * @param analyzerName The analyzer name for looking up test mappings in AnalyzerTestNameCache
   */
  public GenericASTMLineInserter(String analyzerId, String analyzerName) {
    this.analyzerName = analyzerName;
  }

  /**
   * Parse ASTM message lines and persist results.
   *
   * <p>Uses the same flow as legacy plugin inserters:
   *
   * <ol>
   *   <li>Parse ASTM segments (O for order/accession, R for results)
   *   <li>Look up test mappings from AnalyzerTestNameCache (populated from DB)
   *   <li>Create AnalyzerResults with mapped test IDs
   *   <li>Persist results to analyzer_results table
   * </ol>
   *
   * @param lines ASTM message lines (H, P, O, R, L segments)
   * @param currentUserId the user performing the import
   * @return true if results were persisted successfully
   */
  @Override
  public boolean insert(List<String> lines, String currentUserId) {
    errorMessage = null;
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

    if (results.isEmpty()) {
      LogEvent.logWarn(
          this.getClass().getSimpleName(), "insert", "No results parsed from ASTM message");
      return true; // Not an error - message might be a query, not results
    }

    LogEvent.logInfo(
        this.getClass().getSimpleName(),
        "insert",
        "Parsed " + results.size() + " results from " + analyzerName);

    return persistImport(currentUserId, results);
  }

  @Override
  public String getError() {
    if (errorMessage != null) {
      return errorMessage;
    }
    return "GenericASTM analyzer (" + analyzerName + ") unable to write to database";
  }

  /**
   * Extract the ASTM segment type identifier from a line.
   *
   * @param line ASTM record line (e.g., "R|1|^^^WBC|5.8|...")
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
   * <p>Uses AnalyzerTestNameCache to look up test mappings (same as legacy plugins).
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

    String resultValue =
        cleanResultValue(resultFields.length > R_VALUE_FIELD ? resultFields[R_VALUE_FIELD] : "");
    String units = resultFields.length > R_UNITS_FIELD ? resultFields[R_UNITS_FIELD] : "";
    String timestampStr =
        resultFields.length > R_TIMESTAMP_FIELD ? resultFields[R_TIMESTAMP_FIELD] : "";

    // Look up test mapping from AnalyzerTestNameCache (same as HoribaPentra60)
    MappedTestName mappedTest =
        AnalyzerTestNameCache.getInstance().getMappedTest(analyzerName, testCode);

    if (mappedTest == null) {
      // No mapping found - create empty mapping for manual configuration
      mappedTest =
          AnalyzerTestNameCache.getInstance().getEmptyMappedTestName(analyzerName, testCode);
      LogEvent.logDebug(
          this.getClass().getSimpleName(),
          "addResultFromLine",
          "No mapping for test code '" + testCode + "' - will require manual mapping");
    }

    AnalyzerResults analyzerResult = new AnalyzerResults();
    analyzerResult.setAnalyzerId(mappedTest.getAnalyzerId());
    analyzerResult.setTestId(mappedTest.getTestId());
    analyzerResult.setTestName(mappedTest.getOpenElisTestName());
    analyzerResult.setResult(resultValue);
    analyzerResult.setUnits(units);
    analyzerResult.setAccessionNumber(accessionNumber);
    analyzerResult.setIsControl(isQcSample(orderRecord));

    // Parse completion timestamp
    Timestamp completeDate = parseTimestamp(timestampStr);
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
   * Check if the O-record indicates a QC sample via Action Code (O.12).
   *
   * <p>Per ASTM E-1394-97 and Cepheid GeneXpert LIS spec, O.12 = "Q" indicates a QC sample. This
   * determines whether the result is routed to the QC queue or the patient results queue.
   *
   * @param orderRecord the O-segment line
   * @return true if Action Code is "Q" (QC sample)
   */
  private boolean isQcSample(String orderRecord) {
    String[] fields = orderRecord.split(Pattern.quote(FIELD_DELIMITER));
    if (fields.length > O_ACTION_CODE_FIELD) {
      return "Q".equalsIgnoreCase(fields[O_ACTION_CODE_FIELD].trim());
    }
    return false;
  }

  /**
   * Clean ASTM result value by stripping component delimiters.
   *
   * <p>Some analyzers (notably GeneXpert per Cepheid LIS spec) use multi-component R.4 values:
   *
   * <ul>
   *   <li>Qualitative: {@code NEGATIVE^} (value in component 1, empty component 2)
   *   <li>Quantitative complementary: {@code ^3.10} (empty component 1, value in component 2)
   * </ul>
   *
   * <p>This method strips leading/trailing component delimiters to extract the clean value.
   *
   * @param value raw R.4 field value
   * @return cleaned value with component delimiters removed
   */
  private String cleanResultValue(String value) {
    if (value == null || value.isEmpty()) {
      return value;
    }
    // Strip trailing component delimiters (e.g., "NEGATIVE^" → "NEGATIVE")
    String cleaned = value.replaceAll("\\^+$", "");
    // Strip leading component delimiter (e.g., "^3.10" → "3.10")
    if (cleaned.startsWith(COMPONENT_DELIMITER)) {
      cleaned = cleaned.substring(1);
    }
    return cleaned;
  }

  /**
   * Extract the accession number from an O (Order) segment.
   *
   * <p>ASTM O-segment format: O|seq|specimen_id^location|...
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
   * <p>Universal Test ID format: ^^^TEST_CODE or ^TEST_CODE
   *
   * @param resultFields the R-segment split by field delimiter
   * @return test code (e.g., "WBC", "LYM%") or null if not found
   */
  private String extractTestCode(String[] resultFields) {
    if (resultFields.length > R_TEST_ID_FIELD) {
      String testIdField = resultFields[R_TEST_ID_FIELD];
      String[] components = testIdField.split(Pattern.quote(COMPONENT_DELIMITER));

      // Standard ASTM: ^^^TEST_CODE (test code is 4th component)
      if (components.length >= 4 && !components[3].trim().isEmpty()) {
        return components[3].trim();
      }
      // Some analyzers use shorter format: ^TEST_CODE (test code is 2nd component)
      if (components.length >= 2 && !components[1].trim().isEmpty()) {
        return components[1].trim();
      }
      // Fallback: use the whole field if no components
      if (components.length == 1 && !testIdField.trim().isEmpty()) {
        return testIdField.trim();
      }
    }
    return null;
  }

  /**
   * Parse timestamp from ASTM format.
   *
   * @param timestampStr timestamp string (yyyyMMddHHmmss or yyyyMMdd)
   * @return Timestamp or null if parsing fails
   */
  private Timestamp parseTimestamp(String timestampStr) {
    if (timestampStr == null || timestampStr.trim().isEmpty()) {
      return null;
    }

    // Try full format first
    Timestamp ts =
        DateUtil.convertStringDateToTimestampWithPattern(timestampStr, ASTM_TIMESTAMP_PATTERN);
    if (ts != null) {
      return ts;
    }

    // Try short format
    return DateUtil.convertStringDateToTimestampWithPattern(timestampStr, ASTM_TIMESTAMP_SHORT);
  }
}
