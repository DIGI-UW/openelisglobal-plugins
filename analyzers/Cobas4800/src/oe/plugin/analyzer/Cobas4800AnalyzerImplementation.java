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
import java.util.Map.Entry;
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

public class Cobas4800AnalyzerImplementation extends AnalyzerLineInserter {
  private static final String UNDER_THREASHOLD = "< LL";
  private static final double THREASHOLD = 20.0;

  private static String RESULT_FLAG = "Result Name";
  private static String RESULT_VALUE_FLAG = "Value";
  private static String VL_FLAG = "HIV-1";
  private static String EID_FLAG = "HIV-1-qual-DBS";
  private static String TEST_FLAG = "TestType";
  private static String ACCESSION_FLAG = "SpecimenId";
  private static String ACCEPTED_DATE_FLAG = "AcceptedDateTime";
  private static String TEST_TYPE_FLAG = "TestType";
  private static String CONTROL_FLAG = "SpecimenType";

  private static String NEGATIVE_ID;
  private static String POSITIVE_ID;
  private static String INDETERMINATE_ID;
  private static String INVALID_ID;
  private static String VALID_ID;

  private static final String DATE_PATTERN = "yyyy-MM-dd HH:mm:ss";
  static String VL_ANALYZER_ID;
  static String EID_ANALYZER_ID;
  private final String projectCode =
      MessageUtil.getMessage("sample.entry.project.LART")
          + ":"
          + MessageUtil.getMessage("sample.entry.project.LDBS");

  static HashMap<String, Test> testHeaderNameMap = new HashMap<String, Test>();
  static HashMap<String, String> indexAnalyzerMap = new HashMap<String, String>();
  static HashMap<String, String> resultsTypeMap = new HashMap<String, String>();

  private AnalyzerReaderUtil readerUtil = new AnalyzerReaderUtil();
  private String error;
  //  Test test = (Test)SpringContext.getBean(TestService.class).getActiveTestByName("Viral
  // Load").get(0);
  String validStatusId =
      StatusService.getInstance().getStatusID(StatusService.AnalysisStatus.Finalized);
  AnalysisService analysisService = SpringContext.getBean(AnalysisService.class);

  static {
    testHeaderNameMap.put(
        VL_FLAG,
        SpringContext.getBean(TestService.class)
            .getActiveTestByName("Viral Load")
            .get(0)); // .getTestByGUID("0e240569-c095-41c7-bfd2-049527452f16"));
    testHeaderNameMap.put(
        EID_FLAG,
        SpringContext.getBean(TestService.class)
            .getActiveTestByName("DNA PCR")
            .get(0)); // .getTestByGUID("fe6405c8-f96b-491b-95c9-b1f635339d6a"));

    resultsTypeMap.put(VL_FLAG, "A");
    resultsTypeMap.put(EID_FLAG, "D");

    AnalyzerService analyzerService = SpringContext.getBean(AnalyzerService.class);
    Analyzer analyzer = analyzerService.getAnalyzerByName("Cobas4800VLAnalyzer");
    VL_ANALYZER_ID = analyzer.getId();

    analyzer = analyzerService.getAnalyzerByName("Cobas4800EIDAnalyzer");
    EID_ANALYZER_ID = analyzer.getId();

    indexAnalyzerMap.put(VL_FLAG, VL_ANALYZER_ID);
    indexAnalyzerMap.put(EID_FLAG, EID_ANALYZER_ID);

    DictionaryService dictionaryService = SpringContext.getBean(DictionaryService.class);
    Test test = SpringContext.getBean(TestService.class).getActiveTestByName("DNA PCR").get(0);
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

  public String getError() {
    return this.error;
  }

  private void addValueToResults(List<AnalyzerResults> resultList, AnalyzerResults result) {
    if (result.getIsControl()) {
      resultList.add(result);
      return;
    }
    SampleService sampleServ = SpringContext.getBean(SampleService.class);
    String labPrefix = result.getAccessionNumber().substring(0, 4);

    if (!projectCode.contains(labPrefix)
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
    //  ordersExport(results);
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

    if (!projectCode.contains(labPrefix) && accessionNumber.length() >= 9)
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
    analyzerResults.setTestId(testHeaderNameMap.get(testKey).getId());
    analyzerResults.setTestName(testHeaderNameMap.get(testKey).getName());

    // ANALYZER_ID processing
    analyzerResults.setAnalyzerId(indexAnalyzerMap.get(testKey));

    // RESULT_TYPE processing
    analyzerResults.setResultType(resultsTypeMap.get(testKey));

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
          result =
              String.valueOf(
                  (int) (Math.round(resultAsDouble))); // + result.substring(result.indexOf("("));
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
          result =
              String.valueOf(
                  (int) (Math.round(resultAsDouble))); // + result.substring(result.indexOf("("));
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

    if (result.toLowerCase().equals("not detected")) result = NEGATIVE_ID;
    else if (result.toLowerCase().equals("detected")) result = POSITIVE_ID;
    else if (result.toLowerCase().equals("invalid")) result = INVALID_ID;
    else if (result.toLowerCase().equals("valid")) result = VALID_ID;
    else result = INDETERMINATE_ID;

    return result;
  }
}
