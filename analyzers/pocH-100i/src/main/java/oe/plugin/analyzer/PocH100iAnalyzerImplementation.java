/*
 * The contents of this file are subject to the Mozilla Public License
 * Version 1.1 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.mozilla.org/MPL/
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
 * License for the specific language governing rights and limitations under
 * the License.
 *
 * The Original Code is OpenELIS code.
 *
 * Copyright (C) ITECH, University of Washington, Seattle WA.  All Rights Reserved.
 */

package oe.plugin.analyzer;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.regex.Pattern;
import org.apache.commons.validator.GenericValidator;
import org.openelisglobal.analysis.service.AnalysisService;
import org.openelisglobal.analysis.valueholder.Analysis;
import org.openelisglobal.analyzerimport.analyzerreaders.AnalyzerLineInserter;
import org.openelisglobal.analyzerimport.analyzerreaders.AnalyzerReaderUtil;
import org.openelisglobal.analyzerimport.analyzerreaders.AnalyzerResponder;
import org.openelisglobal.analyzerresults.valueholder.AnalyzerResults;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.services.PluginAnalyzerService;
import org.openelisglobal.common.util.DateUtil;
import org.openelisglobal.patient.valueholder.Patient;
import org.openelisglobal.person.valueholder.Person;
import org.openelisglobal.sample.service.SampleService;
import org.openelisglobal.sample.valueholder.Sample;
import org.openelisglobal.samplehuman.service.SampleHumanService;
import org.openelisglobal.spring.util.SpringContext;
import org.openelisglobal.test.service.TestService;
import org.openelisglobal.test.valueholder.Test;

public class PocH100iAnalyzerImplementation extends AnalyzerLineInserter
    implements AnalyzerResponder {

  static final String ANALYZER_TEST_WBC = "WBC";
  static final String ANALYZER_TEST_RBC = "RBC";
  static final String ANALYZER_TEST_HGB = "HGB";
  static final String ANALYZER_TEST_HCT = "HCT";
  static final String ANALYZER_TEST_MCV = "MCV";
  static final String ANALYZER_TEST_MCH = "MCH";
  static final String ANALYZER_TEST_MCHC = "MCHC";
  static final String ANALYZER_TEST_RDWSD = "RDW-SD";
  static final String ANALYZER_TEST_RDWCV = "RDW-CV";
  static final String ANALYZER_TEST_PLT = "PLT";
  static final String ANALYZER_TEST_MPV = "MPV";
  static final String ANALYZER_TEST_NEUT_COUNT = "NEUT#";
  static final String ANALYZER_TEST_NEUT_PERCENT = "NEUT%";
  static final String ANALYZER_TEST_LYMPH_COUNT = "LYMPH#";
  static final String ANALYZER_TEST_LYMPH_PERCENT = "LYMPH%";
  static final String ANALYZER_TEST_MONO_COUNT = "MONO#";
  static final String ANALYZER_TEST_MONO_PERCENT = "MONO%";
  static final String ANALYZER_TEST_EO_COUNT = "EO#";
  static final String ANALYZER_TEST_EO_PERCENT = "EO%";
  static final String ANALYZER_TEST_BASO_COUNT = "BASO#";
  static final String ANALYZER_TEST_BASO_PERCENT = "BASO%";
  static final String ANALYZER_TEST_IG_COUNT = "IG#";
  static final String ANALYZER_TEST_IG_PERCENT = "IG%";
  static final String ANALYZER_TEST_MXD_COUNT = "MXD#";
  static final String ANALYZER_TEST_MXD_PERCENT = "MXD%";

  static final String LOINC_WBC = "6690-2";
  static final String LOINC_RBC = "789-8";
  static final String LOINC_HGB = "718-7";
  static final String LOINC_HCT = "4544-3";
  static final String LOINC_MCV = "787-2";
  static final String LOINC_MCH = "785-6";
  static final String LOINC_MCHC = "786-4";
  static final String LOINC_RDWSD = "21000-5";
  static final String LOINC_RDWCV = "788-0";
  static final String LOINC_PLT = "777-3";
  static final String LOINC_MPV = "32623-1";
  static final String LOINC_NEUT_COUNT = "751-8";
  static final String LOINC_NEUT_PERCENT = "770-8";
  static final String LOINC_LYMPH_COUNT = "731-0";
  ;
  static final String LOINC_LYMPH_PERCENT = "736-9";
  static final String LOINC_MONO_COUNT = "742-7";
  static final String LOINC_MONO_PERCENT = "5905-5";
  static final String LOINC_EO_COUNT = "711-2";
  static final String LOINC_EO_PERCENT = "713-8";
  static final String LOINC_BASO_COUNT = "704-7";
  static final String LOINC_BASO_PERCENT = "706-2";
  static final String LOINC_IG_COUNT = "53115-2";
  static final String LOINC_IG_PERCENT = "71695-1";
  static final String LOINC_MXD_COUNT = "32154-7";
  static final String LOINC_MXD_PERCENT = "32155-4";

  protected static final String HEADER_RECORD_IDENTIFIER = "H";
  protected static final String QUERY_RECORD_IDENTIFIER = "Q";
  protected static final String PATIENT_RECORD_IDENTIFIER = "P";
  protected static final String ORDER_RECORD_IDENTIFIER = "O";
  protected static final String RESULT_RECORD_IDENTIFIER = "R";
  protected static final String END_RECORD_IDENTIFIER = "L";
  protected static final String FD = "|"; // DEFAULT_FIELD_DELIMITER
  protected static final String CD = "^"; // DEFAULT_COMPONENT_DELIMITER
  protected static final String RD = "\\"; // DEFAULT_REPEATER_DELIMITER
  protected static final String ED = "&"; // DEFAULT_ESCAPE_DELIMITER
  protected static final String TEST_COMMUNICATION_IDENTIFIER = "M|1|106";
  private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd");
  private SimpleDateFormat dateTimeFormat = new SimpleDateFormat("yyyyMMddHHmmss");

  private TestService testService = SpringContext.getBean(TestService.class);
  private SampleService sampleService = SpringContext.getBean(SampleService.class);
  private SampleHumanService sampleHumanService = SpringContext.getBean(SampleHumanService.class);
  private AnalysisService analysisService = SpringContext.getBean(AnalysisService.class);
  private PluginAnalyzerService pluginAnalyzerService =
      SpringContext.getBean(PluginAnalyzerService.class);
  private Map<String, String> testToLoincMap = new HashMap<>();
  private Map<String, String> loincToTestCodeMap = new HashMap<>();
  private Map<String, List<Test>> testCodeToTestsMap = new HashMap<>();

  private AnalyzerReaderUtil readerUtil = new AnalyzerReaderUtil();

  public PocH100iAnalyzerImplementation() {

    testToLoincMap.put(ANALYZER_TEST_WBC, LOINC_WBC);
    testToLoincMap.put(ANALYZER_TEST_RBC, LOINC_RBC);
    testToLoincMap.put(ANALYZER_TEST_HGB, LOINC_HGB);
    testToLoincMap.put(ANALYZER_TEST_HCT, LOINC_HCT);
    testToLoincMap.put(ANALYZER_TEST_MCV, LOINC_MCV);
    testToLoincMap.put(ANALYZER_TEST_MCH, LOINC_MCH);
    testToLoincMap.put(ANALYZER_TEST_MCHC, LOINC_MCHC);
    testToLoincMap.put(ANALYZER_TEST_RDWSD, LOINC_RDWSD);
    testToLoincMap.put(ANALYZER_TEST_RDWCV, LOINC_RDWCV);
    testToLoincMap.put(ANALYZER_TEST_PLT, LOINC_PLT);
    testToLoincMap.put(ANALYZER_TEST_MPV, LOINC_MPV);
    testToLoincMap.put(ANALYZER_TEST_NEUT_COUNT, LOINC_NEUT_COUNT);
    testToLoincMap.put(ANALYZER_TEST_NEUT_PERCENT, LOINC_NEUT_PERCENT);
    testToLoincMap.put(ANALYZER_TEST_LYMPH_COUNT, LOINC_LYMPH_COUNT);
    testToLoincMap.put(ANALYZER_TEST_LYMPH_PERCENT, LOINC_LYMPH_PERCENT);
    testToLoincMap.put(ANALYZER_TEST_MONO_COUNT, LOINC_MONO_COUNT);
    testToLoincMap.put(ANALYZER_TEST_MONO_PERCENT, LOINC_MONO_PERCENT);
    testToLoincMap.put(ANALYZER_TEST_EO_COUNT, LOINC_EO_COUNT);
    testToLoincMap.put(ANALYZER_TEST_EO_PERCENT, LOINC_EO_PERCENT);
    testToLoincMap.put(ANALYZER_TEST_BASO_COUNT, LOINC_BASO_COUNT);
    testToLoincMap.put(ANALYZER_TEST_BASO_PERCENT, LOINC_BASO_PERCENT);
    testToLoincMap.put(ANALYZER_TEST_IG_COUNT, LOINC_IG_COUNT);
    testToLoincMap.put(ANALYZER_TEST_IG_PERCENT, LOINC_IG_PERCENT);
    testToLoincMap.put(ANALYZER_TEST_MXD_COUNT, LOINC_MXD_COUNT);
    testToLoincMap.put(ANALYZER_TEST_MXD_PERCENT, LOINC_MXD_PERCENT);
    pluginAnalyzerService.loadLoincMappingsFromCSV(testToLoincMap, PocH100iAnalyzer.ANALYZER_NAME);

    for (Entry<String, String> entry : testToLoincMap.entrySet()) {
      loincToTestCodeMap.put(entry.getValue(), entry.getKey());
      testCodeToTestsMap.put(entry.getKey(), testService.getTestsByLoincCode(entry.getValue()));
    }
  }

  // example message:
  // H|\^&|||pocH-100i^01-29^^^^               ^02318732||||||||E1394-97<CR>
  // P|1<CR>
  // O|1||^^ 12345ABCDE^B|^^^^WBC\^^^^RBC\^^^^HGB\^^^^HCT\
  // ^^^^MCV\^^^^MCH\^^^^MCHC\^^^^PLT\^^^^W-SCR\^^^^W-MCR\^^^^W-LCR\^^^^W-SCC\^^^^W-MCC\^^^^W-LCC\^^^^RDW-SD\^^^^RDW-CV\^^^^PDW\^^^^MPV\^^^^P-LCR|||||||N||||||||||||||F<CR>
  // R|1|^^^^WBC^26|78|10*2/uL||N||||123456789012345||20011221163530<CR>
  // R|2|^^^^RBC^26|350|10*4/uL||L||||123456789012345||20011221163530<CR>
  // R|3|^^^^HGB^26|***.*|g/dL||A||||123456789012345||20011221163530<CR>
  // ……
  // R|19|^^^^P-LCR^26|50.0|%||H||||123456789012345||20011221163530<CR>
  // L|1|N<CR>
  /*
   * (non-Javadoc)
   *
   * @see
   * org.openelisglobal.analyzerimport.analyzerreaders.AnalyzerLineInserter#insert
   * (java.util.List, java.lang.String)
   */
  @Override
  public boolean insert(List<String> lines, String currentUserId) {

    String patientRecord = null;
    String orderRecord = null;

    List<AnalyzerResults> results = new ArrayList<>();
    for (String line : lines) {
      if (line.startsWith(TEST_COMMUNICATION_IDENTIFIER)) {
        LogEvent.logInfo(
            this.getClass().getName(),
            "insert",
            "this is a test communication record for PocH-100i");
      }
      if (line.startsWith(PATIENT_RECORD_IDENTIFIER)) {
        if (patientRecord != null) {
          patientRecord = null;
          orderRecord = null;
        }
        patientRecord = line;
      }
      if (line.startsWith(ORDER_RECORD_IDENTIFIER)) {
        orderRecord = line;
      }
      if (line.startsWith(RESULT_RECORD_IDENTIFIER)) {
        LogEvent.logDebug(this.getClass().getName(), "insert", "adding result");

        addRecordsToResults(patientRecord, orderRecord, line, results, currentUserId);
      }
      if (line.startsWith(END_RECORD_IDENTIFIER)) {
        LogEvent.logDebug(this.getClass().getName(), "insert", "end Aquios of record");
        break;
      }
    }
    return persistImport(currentUserId, results);
  }

  @Override
  public String getError() {
    return PocH100iAnalyzer.ANALYZER_NAME + " analyzer unable to write to database";
  }

  private Test findMatchingTest(Sample sample, String resultTestCode) {
    if (sample != null) {
      for (Analysis curAnalysis : analysisService.getAnalysesBySampleId(sample.getId())) {
        List<Test> possibleTests = testCodeToTestsMap.get(resultTestCode);
        // if ((possibleTests == null || possibleTests.size() == 0) && resultTestCode.contains("+"))
        // {
        // 	possibleTests = testCodeToTestsMap.get(resultTestCode);
        // }
        if (possibleTests != null) {
          for (Test curTest : possibleTests) {
            if (curTest.getLoinc() != null
                && curTest.getLoinc().equals(curAnalysis.getTest().getLoinc())) {
              LogEvent.logDebug(
                  this.getClass().getSimpleName(),
                  "findMatchingTest",
                  "found test in sample for code: " + resultTestCode);
              return curAnalysis.getTest();
            }
          }
        }
      }
    }
    return null;
  }

  // example patient record:
  // P|1<CR>
  // example order record:
  // O|1||^^ 12345ABCDE^B|^^^^WBC\^^^^RBC\^^^^HGB\^^^^HCT\
  // ^^^^MCV\^^^^MCH\^^^^MCHC\^^^^PLT\^^^^W-SCR\^^^^W-MCR\^^^^W-LCR\^^^^W-SCC\^^^^W-MCC\^^^^W-LCC\^^^^RDW-SD\^^^^RDW-CV\^^^^PDW\^^^^MPV\^^^^P-LCR|||||||N||||||||||||||F<CR>
  // example result records:
  // R|1|^^^^WBC^26|78|10*2/uL||N||||123456789012345||20011221163530<CR>
  private void addRecordsToResults(
      String patientRecord,
      String orderRecord,
      String resultRecord,
      List<AnalyzerResults> results,
      String currentUserId) {
    String[] patientRecordFields = patientRecord.split(Pattern.quote(FD));
    String[] orderRecordFields = orderRecord.split(Pattern.quote(FD));
    String[] orderTestIdFields = orderRecordFields[4].split(Pattern.quote(RD));
    String[] orderIdFields = orderRecordFields[3].split(Pattern.quote(CD));
    String[] resultRecordFields = resultRecord.split(Pattern.quote(FD));
    String[] resultTestIdField = resultRecordFields[2].split(Pattern.quote(CD));
    String resultRecordAbnormalFlag = resultRecordFields[6];
    List<String> orderTestIds = new ArrayList<>();
    for (String orderIdField : orderTestIdFields) {
      String[] orderIds = orderIdField.split(Pattern.quote(CD));
      String orderTestId = orderIds.length >= 5 ? orderIds[4] : "";
      if (GenericValidator.isBlankOrNull(orderTestId)) {
        LogEvent.logWarn(
            this.getClass().getSimpleName(),
            "addRecordsToResults",
            "order analysis parameter name is not present");
      }
      orderTestIds.add(orderTestId);
    }
    if (orderTestIds.size() <= 0) {
      LogEvent.logWarn(
          this.getClass().getSimpleName(),
          "addRecordsToResults",
          "order analysis has no tests specified");
    }
    String resultTestId = resultTestIdField.length >= 5 ? resultTestIdField[4] : "";

    String currentAccessionNumber = orderIdFields[2].trim();
    Sample sample = sampleService.getSampleByAccessionNumber(currentAccessionNumber);
    Test test = findMatchingTest(sample, resultTestId);

    if (test == null) {
      LogEvent.logError(
          this.getClass().getName(),
          "addRecordsToResults",
          "can't import a result if order does not have that test ordered");
      LogEvent.logDebug(this.getClass().getSimpleName(), "", "resultTestId: " + resultTestId);
      return;
    }

    switch (resultRecordAbnormalFlag) {
      case "A": // masked data
        LogEvent.logDebug(
            this.getClass().getSimpleName(), "addRecordsToResults", "masked data result");
        break;
      case ">": // out of assured linearity
        LogEvent.logDebug(
            this.getClass().getSimpleName(),
            "addRecordsToResults",
            "out of assured linearity result");
        break;
      case "W": // low reliability
        LogEvent.logDebug(
            this.getClass().getSimpleName(), "addRecordsToResults", "low reliability result");
        break;
      case "H": // higher than reference interval
        LogEvent.logDebug(
            this.getClass().getSimpleName(), "addRecordsToResults", "higher than interval result");
        break;
      case "L": // lower than reference interval
        LogEvent.logDebug(
            this.getClass().getSimpleName(), "addRecordsToResults", "lower than interval result");
        break;
      case "N": // normal
        LogEvent.logDebug(this.getClass().getSimpleName(), "addRecordsToResults", "normal result");
        break;
      default:
        LogEvent.logWarn(
            this.getClass().getSimpleName(), "addRecordsToResults", "abnormal flag not understood");
    }

    AnalyzerResults analyzerResults =
        addResult(
            results,
            null,
            "N",
            resultRecordFields[3],
            DateUtil.convertStringDateToTimestampWithPattern(
                resultRecordFields[12], "yyyyMMddHHmmss"),
            currentAccessionNumber,
            false,
            resultRecordFields[4],
            test);
    LogEvent.logDebug(
        this.getClass().getName(),
        "addResultLine",
        "***"
            + analyzerResults.getAccessionNumber()
            + " "
            + analyzerResults.getCompleteDate()
            + " "
            + analyzerResults.getResult());
  }

  public AnalyzerResults addResult(
      List<AnalyzerResults> resultList,
      List<AnalyzerResults> notMatchedResults,
      String resultType,
      String resultValue,
      Date date,
      String accessionNumber,
      boolean isControl,
      String resultUnits,
      Test test) {
    LogEvent.logDebug(
        this.getClass().getName(), "addResult", "adding result for lab Number: " + accessionNumber);
    AnalyzerResults analyzerResults =
        createAnalyzerResult(
            resultType, resultValue, resultUnits, date, accessionNumber, isControl, test);
    if (analyzerResults.getTestId() != null) {
      addValueToResults(resultList, analyzerResults);
    } else {
      LogEvent.logWarn(
          this.getClass().getName(), "addResult", "no matching result for " + accessionNumber);
      notMatchedResults.add(analyzerResults);
    }
    return analyzerResults;
  }

  private void addValueToResults(List<AnalyzerResults> resultList, AnalyzerResults result) {
    resultList.add(result);
    LogEvent.logDebug(
        this.getClass().getName(),
        "addValueToResults",
        "searching for matching analysis for " + result.getAccessionNumber());
    AnalyzerResults resultFromDB = readerUtil.createAnalyzerResultFromDB(result);
    if (resultFromDB != null) {
      LogEvent.logWarn(
          this.getClass().getName(),
          "addValueToResults",
          "no resultFromDB for " + result.getAccessionNumber());
      resultList.add(resultFromDB);
    }
  }

  private AnalyzerResults createAnalyzerResult(
      String resultType,
      String resultValue,
      String resultUnits,
      Date date,
      String accessionNumber,
      boolean isControl,
      Test test) {
    LogEvent.logDebug(
        this.getClass().getName(),
        "createAnalyzerResult",
        "creating analyzer result for " + accessionNumber);

    AnalyzerResults analyzerResults = new AnalyzerResults();
    analyzerResults.setResult(resultValue);
    analyzerResults.setUnits(resultUnits);
    if (date != null) {
      analyzerResults.setCompleteDate(new Timestamp(date.getTime()));
    }
    analyzerResults.setAccessionNumber(accessionNumber);
    analyzerResults.setTestId(test.getId());
    analyzerResults.setIsControl(isControl);
    analyzerResults.setTestName(test.getLocalizedTestName().getLocalizedValue());
    return analyzerResults;
  }

  public void persistImport(List<AnalyzerResults> resultList) {
    this.persistImport("1", resultList);
  }

  public String buildResponse(List<String> lines) {

    String queryRecord = null;

    for (String line : lines) {
      if (line.startsWith(TEST_COMMUNICATION_IDENTIFIER)) {
        LogEvent.logInfo(
            this.getClass().getName(),
            "buildResponse",
            "this is a test communication record for " + PocH100iAnalyzer.ANALYZER_NAME);
      }
      if (line.startsWith(QUERY_RECORD_IDENTIFIER)) {
        LogEvent.logDebug(
            this.getClass().getName(), "buildResponse", "request contains query record");
        String response = generateQueryResponse(line);
        return response;
      }
    }
    LogEvent.logWarn(
        this.getClass().getName(),
        "buildResponse",
        "no response could be created, no query identifier supplied");
    return "";
  }

  private String generateQueryResponse(String queryRecord) {
    LogEvent.logDebug(
        this.getClass().getSimpleName(), "generateQueryResponse", "generating query response");
    String[] queryRecordFields = queryRecord.split(Pattern.quote(FD));

    String[] startingRangeIdNumber = queryRecordFields[2].split(Pattern.quote(CD));
    String sampleIdNo = startingRangeIdNumber[2].trim();
    String sampleNoAttribute = startingRangeIdNumber[3];

    StringBuilder msgBuilder = new StringBuilder();
    LogEvent.logDebug(
        this.getClass().getSimpleName(),
        "generateQueryResponse",
        "searching for sample with lab Number: " + sampleIdNo);

    Sample sample = sampleService.getSampleByAccessionNumber(sampleIdNo.trim());
    if (sample == null) {
      LogEvent.logWarn(
          this.getClass().getSimpleName(),
          "generateQueryResponse",
          "no sample found with lab Number: " + sampleIdNo);
      // return could not find sample messager
      return msgBuilder
          .append("H|\\^&|\r\n")
          .append("P|1|\r\n")
          .append("O|1||||||||||||||||||||||||Y\r\n") // "Y" is no order exists marker
          .append("L|1|N\r\n")
          .toString();
    }
    Patient patient = sampleHumanService.getPatientForSample(sample);
    Person person = patient.getPerson();
    msgBuilder.append("H|\\^&|||||||||||E1394-97\r\n");
    msgBuilder
        .append("P|1|||")
        .append(patient.getNationalId()) // patient identifier
        .append("|^")
        .append(person.getFirstName())
        .append("^")
        .append(person.getLastName()) // names
        .append("||")
        .append(
            patient.getBirthDate() == null
                ? ""
                : dateFormat.format(patient.getBirthDate())) // birthdate
        .append("|")
        .append(patient.getGender()) // gender M F U
        .append("|||||")
        .append("") // DR id
        .append("||||||||||||\r\n");

    msgBuilder.append("O|1|").append(queryRecordFields[2]).append("||");

    boolean first = true;
    for (Analysis curAnalysis : analysisService.getAnalysesBySampleId(sample.getId())) {
      Optional<String> testCode =
          Optional.ofNullable(loincToTestCodeMap.get(curAnalysis.getTest().getLoinc()));
      if (testCode.isPresent()) {
        LogEvent.logDebug(
            this.getClass().getSimpleName(),
            "generateQueryResponse",
            "found supported test in sample with test code: " + testCode.get());
        if (!first) {
          msgBuilder.append("\\");
        }
        first = false;
        msgBuilder.append("^^^^").append(testCode.get());
      }
    }
    msgBuilder
        .append("||")
        .append(
            sample.getEnteredDate() == null ? "" : dateTimeFormat.format(sample.getEnteredDate()))
        .append("|||||N||||||||||||||Q\r\n");
    msgBuilder.append("L|1|N\r\n");
    return msgBuilder.toString();
  }
}
