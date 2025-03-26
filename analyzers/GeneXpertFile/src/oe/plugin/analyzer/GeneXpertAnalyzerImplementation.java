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
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.openelisglobal.analysis.service.AnalysisService;
import org.openelisglobal.analysis.valueholder.Analysis;
import org.openelisglobal.analyzer.service.AnalyzerService;
import org.openelisglobal.analyzer.valueholder.Analyzer;
import org.openelisglobal.analyzerimport.analyzerreaders.AnalyzerLineInserter;
import org.openelisglobal.analyzerimport.analyzerreaders.AnalyzerReaderUtil;
import org.openelisglobal.analyzerresults.valueholder.AnalyzerResults;
import org.openelisglobal.dictionary.service.DictionaryService;
import org.openelisglobal.dictionary.valueholder.Dictionary;
import org.openelisglobal.note.service.NoteService;
import org.openelisglobal.note.service.NoteServiceImpl.NoteType;
import org.openelisglobal.note.valueholder.Note;
import org.openelisglobal.sample.service.SampleService;
import org.openelisglobal.sample.valueholder.Sample;
import org.openelisglobal.spring.util.SpringContext;
import org.openelisglobal.test.service.TestService;
import org.openelisglobal.test.valueholder.Test;
import org.openelisglobal.testresult.valueholder.TestResult;
import org.openelisglobal.typeoftestresult.service.TypeOfTestResultServiceImpl.ResultType;

public class GeneXpertAnalyzerImplementation extends AnalyzerLineInserter {

  private TestService testService = SpringContext.getBean(TestService.class);
  private AnalyzerService analyzerService = SpringContext.getBean(AnalyzerService.class);
  private NoteService noteService = SpringContext.getBean(NoteService.class);
  private AnalysisService analysisService = SpringContext.getBean(AnalysisService.class);
  private SampleService sampleService = SpringContext.getBean(SampleService.class);

  // test names in analyzers
  static final String HBV = "Xpert HBV Viral Load";
  static final String HCV = "Xpert_HCV Viral Load";
  static final String HIV_QUAL = "Xpert HIV-1 Qual";
  static final String HIV_VIRAL = "Xpert_HIV-1 Viral Load";
  static final String COV_2 = "Xpert Xpress SARS-CoV-2";

  // test LOINCS
  static final String HBV_LOINC = "29615-2";
  static final String HCV_LOINC = "11011-4";
  //	static final String HIV_QUAL_LOINC = "";
  static final String HIV_VIRAL_LOINC = "10351-5";
  static final String COV_2_LOINC = "94500-6";

  // results mapping
  static final String COV_2_ANALYZER_POS = "SARS-CoV-2 POSITIVE";
  static final String COV_2_DB_POS = "SARS-CoV-2 RNA DETECTED";
  static final String COV_2_ANALYZER_NEG = "SARS-CoV-2 NEGATIVE";
  static final String COV_2_DB_NEG = "SARS-CoV-2 RNA NOT DETECTED";
  static final String COV_2_ANALYZER_INV = "INVALID";
  static final String COV_2_DB_INV = "INVALID";

  static final String DELIMITER = "[,;]";

  private String ANALYZER_ID;
  private Map<String, List<Test>> testLoincMap = new HashMap<>();
  private Map<String, List<Test>> testNameMap = new HashMap<>();

  private Map<String, String> resultMap = new HashMap<>();

  private AnalyzerReaderUtil readerUtil = new AnalyzerReaderUtil();

  private final String ANALYZER_NOTE = "GeneXpert Analyzer Note";
  private static final Pattern EXTRACT_NUMBER_PATTERN = Pattern.compile("([^\\d]*)([\\d.]+)");

  public GeneXpertAnalyzerImplementation() {
    testLoincMap.put(HBV_LOINC, testService.getTestsByLoincCode(HBV_LOINC));
    testLoincMap.put(HCV_LOINC, testService.getTestsByLoincCode(HCV_LOINC));
    //		testLoincMap.put(HIV_QUAL_LOINC, testService.getTestsByLoincCode(HIV_QUAL_LOINC));
    testLoincMap.put(HIV_VIRAL_LOINC, testService.getTestsByLoincCode(HIV_VIRAL_LOINC));
    testLoincMap.put(COV_2_LOINC, testService.getTestsByLoincCode(COV_2_LOINC));

    testNameMap.put(HBV, testService.getTestsByLoincCode(HBV_LOINC));
    testNameMap.put(HCV, testService.getTestsByLoincCode(HCV_LOINC));
    //		testNameMap.put(HIV_QUAL, testService.getTestsByLoincCode(HIV_QUAL_LOINC));
    testNameMap.put(HIV_VIRAL, testService.getTestsByLoincCode(HIV_VIRAL_LOINC));
    testNameMap.put(COV_2, testService.getTestsByLoincCode(COV_2_LOINC));

    resultMap.put(COV_2_ANALYZER_NEG.toLowerCase(), COV_2_DB_NEG.toLowerCase());
    resultMap.put(COV_2_ANALYZER_POS.toLowerCase(), COV_2_DB_POS.toLowerCase());
    resultMap.put(COV_2_ANALYZER_INV.toLowerCase(), COV_2_DB_INV.toLowerCase());

    Analyzer analyzer = analyzerService.getAnalyzerByName(GeneXpertAnalyzer.ANALYZER_NAME);
    ANALYZER_ID = analyzer.getId();
  }

  /*
   * (non-Javadoc)
   *
   * @see
   * org.openelisglobal.analyzerimport.analyzerreaders.AnalyzerLineInserter#insert
   * (java.util.List, java.lang.String)
   */
  @Override
  public boolean insert(List<String> lines, String currentUserId) {
    List<AnalyzerResults> resultList = new ArrayList<>();
    List<AnalyzerResults> notMatchedResults = new ArrayList<>();

    String testName = "";
    Test test = null;
    List<TestResult> possibleResults = null;
    for (Integer j = 1; j < lines.size(); j++) {

      String date = "";

      String line = lines.get(j);

      if (line.contains("ASSAY INFORMATION")) {
        String assay = lines.get(++j);
        String assayVersion = lines.get(++j);
        String assayType = lines.get(++j);
        String assayDisclaimer = lines.get(++j);

        testName = assay.split(DELIMITER)[1];
        test = testNameMap.get(testName).get(0);
        possibleResults = testService.getPossibleTestResults(test);
      }
      if (line.contains("RESULT TABLE")) {
        String sampleIDLine = lines.get(++j);
        String patientIDLine = lines.get(++j);
        String assayLine = lines.get(++j);
        String assayVersionLine = lines.get(++j);
        String assayTypeLine = lines.get(++j);
        String testTypeLine = lines.get(++j);
        String sampleTypeLine = lines.get(++j);
        String notesLine = lines.get(++j);
        String testResultLine = lines.get(++j);

        String resultField = testResultLine.split(DELIMITER)[1];
        String sampleIdField = sampleIDLine.split(DELIMITER)[1];
        if (sampleIdField.length() > 20) {
          sampleIdField = sampleIdField.substring(0, 20);
        }

        String result = null;
        String resultType = null;

        Sample sample = sampleService.getSampleByAccessionNumber(sampleIdField);
        Analysis analysis = null;
        if (sample != null) {
          for (Analysis curAnalysis : analysisService.getAnalysesBySampleId(sample.getId())) {
            if (test.getLoinc().equals(curAnalysis.getTest().getLoinc())) {
              test = curAnalysis.getTest();
              analysis = curAnalysis;
              break;
            }
          }
        }

        List<Note> notes = new ArrayList<>();
        for (TestResult possibleResult : possibleResults) {
          if (ResultType.DICTIONARY
              .getCharacterValue()
              .equals(possibleResult.getTestResultType())) {
            Dictionary dictionary =
                SpringContext.getBean(DictionaryService.class).get(possibleResult.getValue());
            if (resultMap
                .get(resultField.toLowerCase())
                .equalsIgnoreCase(dictionary.getDictEntry())) {
              result = dictionary.getId();
              resultType = ResultType.DICTIONARY.getCharacterValue();
            }
          } else if (ResultType.NUMERIC
              .getCharacterValue()
              .equals(possibleResult.getTestResultType())) {
            if (analysis != null) {
              noteService.insert(
                  noteService.createSavableNote(
                      analysis, NoteType.INTERNAL, resultField, ANALYZER_NOTE, currentUserId));
            }
            if (!resultField.matches(".*[0-9].*")) {
              result = "0";
            } else {
              Matcher m = EXTRACT_NUMBER_PATTERN.matcher(resultField);
              if (m.find()) {
                result = m.group(2);
              } else {
                result = "";
              }
            }
            resultType = ResultType.NUMERIC.getCharacterValue();
          } else if (ResultType.ALPHA
              .getCharacterValue()
              .equals(possibleResult.getTestResultType())) {
            result = resultField;
            resultType = ResultType.ALPHA.getCharacterValue();
          }
        }
        if (result == null) {
          System.out.println("null result for: " + sampleIdField);
        }
        this.addResult(
            resultList,
            notMatchedResults,
            resultType,
            result,
            null,
            sampleIdField,
            false,
            null,
            test);
      }
    }
    return persistImport(currentUserId, resultList);
  }

  public void addResult(
      List<AnalyzerResults> resultList,
      List<AnalyzerResults> notMatchedResults,
      String resultType,
      String resultValue,
      Date date,
      String accessionNumber,
      boolean isControl,
      String resultUnits,
      Test test) {
    AnalyzerResults analyzerResults =
        createAnalyzerResult(
            resultType, resultValue, resultUnits, date, accessionNumber, isControl, test);
    if (analyzerResults.getTestId() != null) {
      addValueToResults(resultList, analyzerResults);
    } else {
      notMatchedResults.add(analyzerResults);
    }
  }

  private void addValueToResults(List<AnalyzerResults> resultList, AnalyzerResults result) {
    resultList.add(result);

    AnalyzerResults resultFromDB = readerUtil.createAnalyzerResultFromDB(result);
    if (resultFromDB != null) {
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
    AnalyzerResults analyzerResults = new AnalyzerResults();

    analyzerResults.setAnalyzerId(ANALYZER_ID);
    analyzerResults.setResultType(resultType);
    analyzerResults.setResult(resultValue);
    analyzerResults.setUnits(resultUnits);
    if (date != null) {
      analyzerResults.setCompleteDate(new Timestamp(date.getTime()));
    }
    analyzerResults.setAccessionNumber(accessionNumber);
    analyzerResults.setTestId(test.getId());
    analyzerResults.setIsControl(isControl);
    analyzerResults.setTestName(test.getName());
    return analyzerResults;
  }

  @Override
  public String getError() {
    return GeneXpertAnalyzer.ANALYZER_NAME + " unable to write to database";
  }
}
