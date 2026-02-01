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
package oe.plugin.analyzer;

import java.util.ArrayList;
import java.util.List;
import org.openelisglobal.analysis.service.AnalysisService;
import org.openelisglobal.analysis.valueholder.Analysis;
import org.openelisglobal.analyzer.service.AnalyzerService;
import org.openelisglobal.analyzer.valueholder.Analyzer;
import org.openelisglobal.analyzerimport.analyzerreaders.AnalyzerLineInserter;
import org.openelisglobal.analyzerimport.analyzerreaders.AnalyzerReaderUtil;
import org.openelisglobal.analyzerresults.valueholder.AnalyzerResults;
import org.openelisglobal.common.services.StatusService;
import org.openelisglobal.common.util.DateUtil;
import org.openelisglobal.dictionary.service.DictionaryService;
import org.openelisglobal.dictionary.valueholder.Dictionary;
import org.openelisglobal.internationalization.MessageUtil;
import org.openelisglobal.sample.service.SampleService;
import org.openelisglobal.spring.util.SpringContext;
import org.openelisglobal.test.service.TestService;
import org.openelisglobal.test.valueholder.Test;
import org.openelisglobal.testresult.service.TestResultService;
import org.openelisglobal.testresult.valueholder.TestResult;

public class TaqMan96DBSAnalyzerImplementation extends AnalyzerLineInserter {
  private int ORDER_NUMBER = 0;
  private int ORDER_DATE = 0;
  private int RESULT = 0;
  private int SAMPLE_TYPE = 0;

  // private static final String DELIMITER = "\\t";
  private static String DELIMITER = "\\t";
  private static final String DATE_PATTERN = "yyyy/MM/dd HH:mm:ss";
  private static String NEGATIVE_ID;
  private static String POSITIVE_ID;
  private static String INDETERMINATE_ID;
  private static String INVALID_ID;
  private static String VALID_ID;
  static String ANALYZER_ID;
  private AnalyzerReaderUtil readerUtil = new AnalyzerReaderUtil();
  private String error;
  private final String projectCode = MessageUtil.getMessage("sample.entry.project.LDBS");

  static Test test = SpringContext.getBean(TestService.class).getActiveTestByName("DNA PCR").get(0);
  String validStatusId =
      StatusService.getInstance().getStatusID(StatusService.AnalysisStatus.Finalized);
  AnalysisService analysisService = SpringContext.getBean(AnalysisService.class);

  static {
    AnalyzerService analyzerService = SpringContext.getBean(AnalyzerService.class);
    Analyzer analyzer = analyzerService.getAnalyzerByName("TaqMan96DBSAnalyzer");
    ANALYZER_ID = analyzer.getId();

    DictionaryService dictionaryService = SpringContext.getBean(DictionaryService.class);
    List<TestResult> testResults =
        SpringContext.getBean(TestResultService.class).getActiveTestResultsByTest(test.getId());

    for (TestResult testResult : testResults) {
      Dictionary dictionary = dictionaryService.getDataForId(testResult.getValue());
      if ("Positive".equals(dictionary.getDictEntry())) POSITIVE_ID = dictionary.getId();
      else if ("Negative".equals(dictionary.getDictEntry())) NEGATIVE_ID = dictionary.getId();
      else if ("Invalid".equals(dictionary.getDictEntry())) INVALID_ID = dictionary.getId();
      else if ("Valid".equals(dictionary.getDictEntry())) VALID_ID = dictionary.getId();
      else if ("Indeterminate".equals(dictionary.getDictEntry()))
        INDETERMINATE_ID = dictionary.getId();
    }
  }

  public boolean insert(List<String> lines, String currentUserId) {
    this.error = null;

    List<AnalyzerResults> results = new ArrayList<AnalyzerResults>();

    boolean columnsFound = manageColumnsIndex(lines);

    if (!columnsFound) {
      this.error = "Cobas Taqman DBS analyzer: Unable to find correct columns in file";
      return false;
    }

    for (int i = getColumnsLine(lines) + 1; i < lines.size(); i++) {
      createAnalyzerResultFromLine(lines.get(i), results);
    }

    return persistImport(currentUserId, results);
  }

  private void createAnalyzerResultFromLine(String line, List<AnalyzerResults> resultList) {
    String[] fields = line.split(DELIMITER);

    AnalyzerResults analyzerResults = new AnalyzerResults();

    String result = getAppropriateResults(fields[this.RESULT]);
    String accessionNumber = fields[this.ORDER_NUMBER].replace("\"", "").trim();
    accessionNumber = accessionNumber.replace(" ", "");
    if (accessionNumber.startsWith(projectCode) && accessionNumber.length() >= 9)
      accessionNumber = accessionNumber.substring(0, 9);

    analyzerResults.setAnalyzerId(ANALYZER_ID);
    analyzerResults.setResult(result);
    analyzerResults.setCompleteDate(
        DateUtil.convertStringDateToTimestampWithPattern(
            fields[this.ORDER_DATE].replace("\"", "").trim(), DATE_PATTERN));
    analyzerResults.setTestId(test.getId());
    analyzerResults.setIsControl(
        fields[this.RESULT].replace("\"", "").trim().toUpperCase().equals("VALID"));
    analyzerResults.setTestName(test.getName());
    analyzerResults.setResultType("D");

    if (analyzerResults.getIsControl()) {
      accessionNumber = accessionNumber + ":" + fields[this.SAMPLE_TYPE].replace("\"", "").trim();
    }
    analyzerResults.setAccessionNumber(accessionNumber);

    addValueToResults(resultList, analyzerResults);
  }

  private String getAppropriateResults(String result) {
    result = result.replace("\"", "").trim();

    if (result.toLowerCase().equals("not detected dbs")) result = NEGATIVE_ID;
    else if (result.toLowerCase().equals("detected dbs")) result = POSITIVE_ID;
    else if (result.toLowerCase().equals("invalid")) result = INVALID_ID;
    else if (result.toLowerCase().equals("valid")) result = VALID_ID;
    else result = INDETERMINATE_ID;

    return result;
  }

  public String getError() {
    return this.error;
  }

  public int getColumnsLine(List<String> lines) {
    for (int k = 0; k < lines.size(); k++) {
      if (lines.get(k).contains("Patient Name")
          && lines.get(k).contains("Patient ID")
          && lines.get(k).contains("Order Number")
          && lines.get(k).contains("Sample ID")
          && lines.get(k).contains("Test")
          && lines.get(k).contains("Result")) return k;
    }

    return -1;
  }

  private boolean manageColumnsIndex(List<String> lines) {
    if (getColumnsLine(lines) < 0) return false;
    DELIMITER = lines.get(getColumnsLine(lines)).substring(14, 15);
    String[] fields = lines.get(getColumnsLine(lines)).split(DELIMITER);

    for (int i = 0; i < fields.length; i++) {
      String header = fields[i].replace("\"", "");

      if ("Order Number".equals(header)) ORDER_NUMBER = i;
      else if ("Order Date/Time".equals(header)) ORDER_DATE = i;
      else if ("Result".equals(header)) RESULT = i;
      else if ("Sample Type".equals(header)) {
        SAMPLE_TYPE = i;
      }
    }

    return (ORDER_DATE != 0) && (ORDER_NUMBER != 0) && (RESULT != 0) && (SAMPLE_TYPE != 0);
  }

  private void addValueToResults(List<AnalyzerResults> resultList, AnalyzerResults result) {

    if (result.getIsControl()) {
      resultList.add(result);
      return;
    }
    SampleService sampleServ = SpringContext.getBean(SampleService.class);
    if (!result.getAccessionNumber().startsWith(projectCode)
        || sampleServ.getSampleByAccessionNumber(result.getAccessionNumber()) == null) return;

    List<Analysis> analyses =
        analysisService.getAnalysisByAccessionAndTestId(
            result.getAccessionNumber(), result.getTestId());
    for (Analysis analysis : analyses) {
      if (analysis.getStatusId().equals(validStatusId)) return;
    }
    resultList.add(result);

    AnalyzerResults resultFromDB = this.readerUtil.createAnalyzerResultFromDB(result);
    if (resultFromDB != null) resultList.add(resultFromDB);
  }
}
