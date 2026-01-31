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
package uw.edu.itech.FluoroCyclerXT;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import org.openelisglobal.analyzerimport.analyzerreaders.AnalyzerLineInserter;
import org.openelisglobal.analyzerimport.analyzerreaders.AnalyzerReaderUtil;
import org.openelisglobal.analyzerimport.util.AnalyzerTestNameCache;
import org.openelisglobal.analyzerimport.util.MappedTestName;
import org.openelisglobal.analyzerresults.valueholder.AnalyzerResults;
import org.openelisglobal.common.log.LogEvent;

/**
 * File-based result parser for Hain FluoroCycler XT CSV exports.
 *
 * <p>Expected columns (after FileAnalyzerReader normalization):
 *
 * <ol>
 *   <li>Sample ID
 *   <li>Result
 *   <li>Interpretation
 *   <li>Position
 * </ol>
 */
public class FluoroCyclerXTAnalyzerLineInserter extends AnalyzerLineInserter {

  private static final String ANALYZER_NAME = FluoroCyclerXTAnalyzer.ANALYZER_NAME;

  private static final String TAB_DELIMITER = "\t";
  private static final String SEMI_COLON_DELIMITER = ";";

  private static final int SAMPLE_ID_INDEX = 0;
  private static final int RESULT_INDEX = 1;
  private static final int INTERPRETATION_INDEX = 2;
  private static final int POSITION_INDEX = 3;

  private final AnalyzerReaderUtil readerUtil = new AnalyzerReaderUtil();

  @Override
  public boolean insert(List<String> lines, String currentUserId) {
    List<AnalyzerResults> results = new ArrayList<>();

    for (String line : lines) {
      if (line == null || line.isBlank()) {
        continue;
      }
      if (isHeaderLine(line)) {
        continue;
      }

      FluoroCyclerRecord record = parseLine(line);
      if (record == null) {
        continue;
      }

      addResult(results, record.sampleId, FluoroCyclerXTAnalyzer.RESULT_TEST_NAME, record.result);
      addResult(
          results,
          record.sampleId,
          FluoroCyclerXTAnalyzer.INTERPRETATION_TEST_NAME,
          record.interpretation);
    }

    return persistImport(currentUserId, results);
  }

  @Override
  public String getError() {
    return "Hain FluoroCycler XT analyzer unable to write to database";
  }

  boolean isHeaderLine(String line) {
    return line.toLowerCase().contains("sample id")
        || line.toLowerCase().contains("interpretation")
        || line.toLowerCase().contains("position");
  }

  FluoroCyclerRecord parseLine(String line) {
    String[] columns = splitLine(line);
    if (columns.length < 3) {
      LogEvent.logWarn(
          this.getClass().getSimpleName(), "parseLine", "Skipping line with insufficient columns");
      return null;
    }

    String sampleId = getColumnValue(columns, SAMPLE_ID_INDEX);
    if (sampleId.isEmpty()) {
      LogEvent.logWarn(
          this.getClass().getSimpleName(), "parseLine", "Skipping line without sample ID");
      return null;
    }

    String result = getColumnValue(columns, RESULT_INDEX);
    String interpretation = getColumnValue(columns, INTERPRETATION_INDEX);
    String position = getColumnValue(columns, POSITION_INDEX);

    return new FluoroCyclerRecord(sampleId, result, interpretation, position);
  }

  private String[] splitLine(String line) {
    if (line.contains(TAB_DELIMITER)) {
      return line.split(TAB_DELIMITER, -1);
    }
    if (line.contains(SEMI_COLON_DELIMITER)) {
      return line.split(SEMI_COLON_DELIMITER, -1);
    }
    return line.split(",", -1);
  }

  private String getColumnValue(String[] columns, int index) {
    if (index < columns.length && columns[index] != null) {
      return columns[index].trim();
    }
    return "";
  }

  private void addResult(
      List<AnalyzerResults> results, String accessionNumber, String testCode, String value) {
    if (value == null || value.isBlank()) {
      return;
    }

    MappedTestName mappedTest =
        AnalyzerTestNameCache.getInstance().getMappedTest(ANALYZER_NAME, testCode);

    if (mappedTest == null) {
      mappedTest = AnalyzerTestNameCache.getInstance().getEmptyMappedTestName(ANALYZER_NAME, testCode);
    }

    AnalyzerResults analyzerResult = new AnalyzerResults();
    analyzerResult.setAnalyzerId(mappedTest.getAnalyzerId());
    analyzerResult.setTestId(mappedTest.getTestId());
    analyzerResult.setTestName(mappedTest.getOpenElisTestName());
    analyzerResult.setResult(value);
    analyzerResult.setAccessionNumber(accessionNumber);
    analyzerResult.setIsControl(false);
    analyzerResult.setCompleteDate(new Timestamp(System.currentTimeMillis()));

    results.add(analyzerResult);

    AnalyzerResults resultFromDB = readerUtil.createAnalyzerResultFromDB(analyzerResult);
    if (resultFromDB != null) {
      results.add(resultFromDB);
    }
  }

  static class FluoroCyclerRecord {
    final String sampleId;
    final String result;
    final String interpretation;
    final String position;

    FluoroCyclerRecord(
        String sampleId, String result, String interpretation, String position) {
      this.sampleId = sampleId;
      this.result = result;
      this.interpretation = interpretation;
      this.position = position;
    }
  }
}
