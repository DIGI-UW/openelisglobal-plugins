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

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import org.openelisglobal.analysis.service.AnalysisService;
import org.openelisglobal.analysis.valueholder.Analysis;
import org.openelisglobal.analyzer.service.AnalyzerService;
import org.openelisglobal.analyzer.valueholder.Analyzer;
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

public class Cobas4800AnalyzerImplementation extends AnalyzerLineInserter {
  private static final String UNDER_THREASHOLD = "< LL";
  private static final double THREASHOLD = 20.0;

  private static final String RESULT_FLAG = "Result Name";
  private static final String RESULT_VALUE_FLAG = "Value";
  private static final String VL_FLAG = "HIV-1";
  private static final String EID_FLAG = "HIV-1-qual-DBS";
  private static final String TEST_FLAG = "TestType";
  private static final String ACCESSION_FLAG = "SpecimenId";
  private static final String ACCEPTED_DATE_FLAG = "AcceptedDateTime";
  private static final String TEST_TYPE_FLAG = "TestType";
  private static final String CONTROL_FLAG = "SpecimenType";
  private static final String DATE_PATTERN = "yyyy-MM-dd HH:mm:ss";

  // Lazy-initialized services
  private TestService testService;
  private AnalyzerService analyzerService;
  private SampleService sampleService;
  private AnalysisService analysisService;
  private DictionaryService dictionaryService;
  private TestResultService testResultService;

  // Lazy-initialized data
  private String vlAnalyzerId;
  private String eidAnalyzerId;
  private Map<String, Test> testHeaderNameMap;
  private Map<String, String> indexAnalyzerMap;
  private Map<String, String> resultsTypeMap;
  private String negativeId;
  private String positiveId;
  private String indeterminateId;
  private String invalidId;
  private String validId;
  private String projectCode;
  private String validStatusId;
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

  protected AnalyzerService getAnalyzerService() {
    if (analyzerService == null) {
      analyzerService = SpringContext.getBean(AnalyzerService.class);
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
      projectCode =
          MessageUtil.getMessage("sample.entry.project.LART")
              + ":"
              + MessageUtil.getMessage("sample.entry.project.LDBS");
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

  protected String getVlAnalyzerId() {
    if (vlAnalyzerId == null) {
      Analyzer analyzer = getAnalyzerService().getAnalyzerByName("Cobas4800VLAnalyzer");
      if (analyzer != null) {
        vlAnalyzerId = analyzer.getId();
      } else {
        LogEvent.logWarn(
            this.getClass().getSimpleName(),
            "getVlAnalyzerId",
            "Analyzer not found: Cobas4800VLAnalyzer");
      }
    }
    return vlAnalyzerId;
  }

  protected String getEidAnalyzerId() {
    if (eidAnalyzerId == null) {
      Analyzer analyzer = getAnalyzerService().getAnalyzerByName("Cobas4800EIDAnalyzer");
      if (analyzer != null) {
        eidAnalyzerId = analyzer.getId();
      } else {
        LogEvent.logWarn(
            this.getClass().getSimpleName(),
            "getEidAnalyzerId",
            "Analyzer not found: Cobas4800EIDAnalyzer");
      }
    }
    return eidAnalyzerId;
  }

  protected Map<String, Test> getTestHeaderNameMap() {
    if (testHeaderNameMap == null) {
      testHeaderNameMap = new HashMap<>();
      TestService ts = getTestService();

      List<Test> vlTests = ts.getActiveTestByName("Viral Load");
      if (vlTests != null && !vlTests.isEmpty()) {
        testHeaderNameMap.put(VL_FLAG, vlTests.get(0));
      } else {
        LogEvent.logWarn(
            this.getClass().getSimpleName(), "getTestHeaderNameMap", "Test not found: Viral Load");
      }

      List<Test> pcrTests = ts.getActiveTestByName("DNA PCR");
      if (pcrTests != null && !pcrTests.isEmpty()) {
        testHeaderNameMap.put(EID_FLAG, pcrTests.get(0));
      } else {
        LogEvent.logWarn(
            this.getClass().getSimpleName(), "getTestHeaderNameMap", "Test not found: DNA PCR");
      }
    }
    return testHeaderNameMap;
  }

  protected Map<String, String> getIndexAnalyzerMap() {
    if (indexAnalyzerMap == null) {
      indexAnalyzerMap = new HashMap<>();
      indexAnalyzerMap.put(VL_FLAG, getVlAnalyzerId());
      indexAnalyzerMap.put(EID_FLAG, getEidAnalyzerId());
    }
    return indexAnalyzerMap;
  }

  protected Map<String, String> getResultsTypeMap() {
    if (resultsTypeMap == null) {
      resultsTypeMap = new HashMap<>();
      resultsTypeMap.put(VL_FLAG, "A");
      resultsTypeMap.put(EID_FLAG, "D");
    }
    return resultsTypeMap;
  }

  protected void initializeDictionaryIds() {
    if (!dictionaryIdsInitialized) {
      Map<String, Test> testMap = getTestHeaderNameMap();
      Test test = testMap.get(EID_FLAG);
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

  public String getError() {
    return this.error;
  }

  private void addValueToResults(List<AnalyzerResults> resultList, AnalyzerResults result) {
    if (result.getIsControl()) {
      resultList.add(result);
      return;
    }
    SampleService sampleServ = getSampleService();
    String labPrefix = result.getAccessionNumber().substring(0, 4);

    if (!getProjectCode().contains(labPrefix)
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

  public boolean filterOrdersExport(List<AnalyzerResults> results, String labno) {
    for (AnalyzerResults ar : results) {
      if (ar.getAccessionNumber().equalsIgnoreCase(labno)) return true;
    }
    return false;
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

  public boolean insert(List<String> lines, String currentUserId) {
    List<AnalyzerResults> results = new ArrayList<AnalyzerResults>();
    for (Entry<Integer, Integer> entry :
        getResultsLines(lines, ACCESSION_FLAG, TEST_FLAG, RESULT_FLAG).entrySet()) {
      createResultFromEntry(lines, entry, results);
    }
    Collections.sort(
        results,
        new Comparator<AnalyzerResults>() {
          public int compare(AnalyzerResults o1, AnalyzerResults o2) {
            return o1.getAccessionNumber().compareTo(o2.getAccessionNumber());
          }
        });
    return persistImport(currentUserId, results);
  }

  public HashMap<Integer, Integer> getResultsLines(
      List<String> lines, String LABNO_FLAG, String TEST_FLAG, String RESULT_FLAG) {
    HashMap<Integer, Integer> IdValuePair = new HashMap<Integer, Integer>();

    for (int i = 0; i < lines.size(); i++) {

      if (lines.get(i).contains(ACCESSION_FLAG) && lines.get(i).contains(TEST_FLAG)) {
        int j = i;
        while (!(lines.get(j).contains(RESULT_FLAG))) {
          j = j + 1;
        }
        IdValuePair.put(i, j);
      }
    }

    return IdValuePair.size() == 0 ? null : IdValuePair;
  }

  public void createResultFromEntry(
      List<String> lines, Entry<Integer, Integer> entry, List<AnalyzerResults> resultList) {

    AnalyzerResults analyzerResults = new AnalyzerResults();
    // LABNO processing
    String line = lines.get(entry.getKey());

    String accessionNumber = line.split(ACCESSION_FLAG)[1].substring(2, 11);

    accessionNumber = accessionNumber.trim();
    accessionNumber = accessionNumber.replace(" ", "");
    String labPrefix = accessionNumber.substring(0, 4);

    if (!getProjectCode().contains(labPrefix) && accessionNumber.length() >= 9)
      accessionNumber = accessionNumber.substring(0, 9);

    analyzerResults.setAccessionNumber(accessionNumber);

    // COMPLETED_DATE processing
    String completedDate = line.split(ACCEPTED_DATE_FLAG)[1].substring(2, 12) + " 00:00:00";
    analyzerResults.setCompleteDate(getTimestampFromDate(completedDate));

    // CONTROL CHECKING
    String controlStatus = line.split(CONTROL_FLAG)[1];
    analyzerResults.setIsControl(controlStatus.contains("Control"));

    // TEST_TYPE processing
    String testKey = line.split(TEST_TYPE_FLAG)[1];
    testKey = testKey.split("LisOrderId")[0].trim().substring(2);
    testKey = testKey.substring(0, testKey.length() - 1);

    Map<String, Test> testMap = getTestHeaderNameMap();
    Test test = testMap.get(testKey);
    if (test != null) {
      analyzerResults.setTestId(test.getId());
      analyzerResults.setTestName(test.getName());
    }

    // ANALYZER_ID processing
    analyzerResults.setAnalyzerId(getIndexAnalyzerMap().get(testKey));

    // RESULT_TYPE processing
    analyzerResults.setResultType(getResultsTypeMap().get(testKey));

    // RESULT processing
    line = lines.get(entry.getValue());
    String result = line.split(RESULT_VALUE_FLAG)[1].substring(2);
    result = result.split("CodingSystemId")[0].trim();
    result = result.substring(0, result.length() - 1);
    result =
        testKey.equalsIgnoreCase(VL_FLAG)
            ? getVLResults(result)
            : testKey.equalsIgnoreCase(EID_FLAG) ? getEIDResults(result) : "XXXX";
    analyzerResults.setResult(result);

    // RESULT UNITS processing
    if (testKey.equalsIgnoreCase(VL_FLAG))
      analyzerResults.setUnits(UNDER_THREASHOLD.equals(result) ? "" : "cp/ml");

    addValueToResults(resultList, analyzerResults);
  }

  private String getAppropriateResults(String result) {
    result = result.replace("\"", "").trim();
    if (result.contains("Target Not Detected") || result.contains("Titer min")) {
      result = UNDER_THREASHOLD;
    } else {

      String workingResult = result.replace("E", "");
      String[] splitResult = workingResult.split("\\+");

      try {
        Double resultAsDouble =
            Double.parseDouble(splitResult[0]) * Math.pow(10, Double.parseDouble(splitResult[1]));

        if (resultAsDouble <= THREASHOLD) {
          result = UNDER_THREASHOLD;
        } else {
          result = String.valueOf((int) (Math.round(resultAsDouble)));
          result = result + "(" + String.format("%.3g%n", Math.log10(resultAsDouble));
          result = result + ")";
        }
      } catch (NumberFormatException e) {
        return "XXXX";
      }
    }

    return result;
  }

  private Timestamp getTimestampFromDate(String dateTime) {
    return DateUtil.convertStringDateToTimestampWithPattern(dateTime, DATE_PATTERN);
  }

  private String getVLResults(String result) {
    result = result.split("cp/mL")[0].trim();
    if (result.contains("Target Not Detected") || result.contains("Titer min")) {
      result = UNDER_THREASHOLD;
    } else {

      String workingResult = result.replace("E", "");
      String[] splitResult = workingResult.split("\\+");

      try {
        Double resultAsDouble =
            Double.parseDouble(splitResult[0]) * Math.pow(10, Double.parseDouble(splitResult[1]));

        if (resultAsDouble <= THREASHOLD) {
          result = UNDER_THREASHOLD;
        } else {
          result = String.valueOf((int) (Math.round(resultAsDouble)));
          result = result + "(" + String.format("%.3g%n", Math.log10(resultAsDouble));
          result = result + ")";
        }
      } catch (NumberFormatException e) {
        return "XXXX";
      }
    }

    return result;
  }

  private String getEIDResults(String result) {
    result = result.replace("\"", "").trim();

    if (result.toLowerCase().equals("not detected")) result = getNegativeId();
    else if (result.toLowerCase().equals("detected")) result = getPositiveId();
    else if (result.toLowerCase().equals("invalid")) result = getInvalidId();
    else if (result.toLowerCase().equals("valid")) result = getValidId();
    else result = getIndeterminateId();

    return result;
  }
}
