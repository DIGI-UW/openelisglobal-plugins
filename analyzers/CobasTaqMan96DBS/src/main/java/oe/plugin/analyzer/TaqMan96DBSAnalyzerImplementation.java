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
import org.openelisglobal.analyzerimport.analyzerreaders.AnalyzerLineInserter;
import org.openelisglobal.analyzerimport.analyzerreaders.AnalyzerReaderUtil;
import org.openelisglobal.analyzerresults.valueholder.AnalyzerResults;
import org.openelisglobal.common.log.LogEvent;
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

  private static final String DATE_PATTERN = "yyyy/MM/dd HH:mm:ss";
  private static final String ANALYZER_NAME = "TaqMan96DBSAnalyzer";

  // Lazy-initialized services
  private TestService testService;
  private SampleService sampleService;
  private AnalysisService analysisService;
  private DictionaryService dictionaryService;
  private TestResultService testResultService;

  // Lazy-initialized data
  private Test dnaPcrTest;
  private String negativeId;
  private String positiveId;
  private String indeterminateId;
  private String invalidId;
  private String validId;
  private String projectCode;
  private String validStatusId;
  private String delimiter = "\\t";
  private boolean dictionaryIdsInitialized = false;

  private AnalyzerReaderUtil readerUtil = new AnalyzerReaderUtil();
  private String error;

  // Lazy getters for services
  protected TestService getTestService() {
    if (testService == null) {
      testService = SpringContext.getBean(TestService.class);
    }
    return testService;
  }
    return analyzerService;
  }

  protected SampleService getSampleService() {
    if (sampleService == null) {
      sampleService = SpringContext.getBean(SampleService.class);
    }
    return sampleService;
  }

  protected AnalysisService getAnalysisService() {
    if (analysisService == null) {
      analysisService = SpringContext.getBean(AnalysisService.class);
    }
    return analysisService;
  }

  protected DictionaryService getDictionaryService() {
    if (dictionaryService == null) {
      dictionaryService = SpringContext.getBean(DictionaryService.class);
    }
    return dictionaryService;
  }

  protected TestResultService getTestResultService() {
    if (testResultService == null) {
      testResultService = SpringContext.getBean(TestResultService.class);
    }
    return testResultService;
  }

  // Lazy getters for data
  protected String getProjectCode() {
    if (projectCode == null) {
      projectCode = MessageUtil.getMessage("sample.entry.project.LDBS");
    }
    return projectCode;
  }

  protected String getValidStatusId() {
    if (validStatusId == null) {
      validStatusId =
          StatusService.getInstance().getStatusID(StatusService.AnalysisStatus.Finalized);
    }
    return validStatusId;
  }

  protected Test getDnaPcrTest() {
    if (dnaPcrTest == null) {
      List<Test> tests = getTestService().getActiveTestByName("DNA PCR");
      if (tests != null && !tests.isEmpty()) {
        dnaPcrTest = tests.get(0);
      } else {
        LogEvent.logWarn(
            this.getClass().getSimpleName(), "getDnaPcrTest", "Test not found: DNA PCR");
      }
    }
    return dnaPcrTest;
  }

  protected void initializeDictionaryIds() {
    if (!dictionaryIdsInitialized) {
      Test test = getDnaPcrTest();
      if (test != null) {
        List<TestResult> testResults =
            getTestResultService().getActiveTestResultsByTest(test.getId());
        DictionaryService ds = getDictionaryService();

        for (TestResult testResult : testResults) {
          Dictionary dictionary = ds.getDataForId(testResult.getValue());
          if (dictionary != null) {
            String dictEntry = dictionary.getDictEntry();
            if ("Positive".equals(dictEntry)) {
              positiveId = dictionary.getId();
            } else if ("Negative".equals(dictEntry)) {
              negativeId = dictionary.getId();
            } else if ("Invalid".equals(dictEntry)) {
              invalidId = dictionary.getId();
            } else if ("Valid".equals(dictEntry)) {
              validId = dictionary.getId();
            } else if ("Indeterminate".equals(dictEntry)) {
              indeterminateId = dictionary.getId();
            }
          }
        }
      }
      dictionaryIdsInitialized = true;
    }
  }

  protected String getNegativeId() {
    initializeDictionaryIds();
    return negativeId;
  }

  protected String getPositiveId() {
    initializeDictionaryIds();
    return positiveId;
  }

  protected String getIndeterminateId() {
    initializeDictionaryIds();
    return indeterminateId;
  }

  protected String getInvalidId() {
    initializeDictionaryIds();
    return invalidId;
  }

  protected String getValidId() {
    initializeDictionaryIds();
    return validId;
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
    String[] fields = line.split(delimiter);

    AnalyzerResults analyzerResults = new AnalyzerResults();

    String result = getAppropriateResults(fields[this.RESULT]);
    String accessionNumber = fields[this.ORDER_NUMBER].replace("\"", "").trim();
    accessionNumber = accessionNumber.replace(" ", "");
    if (accessionNumber.startsWith(getProjectCode()) && accessionNumber.length() >= 9)
      accessionNumber = accessionNumber.substring(0, 9);

    Test test = getDnaPcrTest();
    if (test == null) {
      return;
    }
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

    if (result.toLowerCase().equals("not detected dbs")) result = getNegativeId();
    else if (result.toLowerCase().equals("detected dbs")) result = getPositiveId();
    else if (result.toLowerCase().equals("invalid")) result = getInvalidId();
    else if (result.toLowerCase().equals("valid")) result = getValidId();
    else result = getIndeterminateId();

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
    delimiter = lines.get(getColumnsLine(lines)).substring(14, 15);
    String[] fields = lines.get(getColumnsLine(lines)).split(delimiter);

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
    SampleService sampleServ = getSampleService();
    if (!result.getAccessionNumber().startsWith(getProjectCode())
        || sampleServ.getSampleByAccessionNumber(result.getAccessionNumber()) == null) return;

    List<Analysis> analyses =
        getAnalysisService()
            .getAnalysisByAccessionAndTestId(result.getAccessionNumber(), result.getTestId());
    for (Analysis analysis : analyses) {
      if (analysis.getStatusId().equals(getValidStatusId())) return;
    }
    resultList.add(result);

    AnalyzerResults resultFromDB = this.readerUtil.createAnalyzerResultFromDB(result);
    if (resultFromDB != null) resultList.add(resultFromDB);
  }
}
