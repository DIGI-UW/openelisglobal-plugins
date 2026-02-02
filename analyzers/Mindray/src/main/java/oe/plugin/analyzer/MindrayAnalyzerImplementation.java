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
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import org.openelisglobal.analyzer.service.AnalyzerService;
import org.openelisglobal.analyzer.valueholder.Analyzer;
import org.openelisglobal.analyzerimport.analyzerreaders.AnalyzerLineInserter;
import org.openelisglobal.analyzerimport.analyzerreaders.AnalyzerReaderUtil;
import org.openelisglobal.analyzerresults.valueholder.AnalyzerResults;
import org.openelisglobal.spring.util.SpringContext;
import org.openelisglobal.test.service.TestService;
import org.openelisglobal.test.valueholder.Test;

public class MindrayAnalyzerImplementation extends AnalyzerLineInserter {

  private TestService testService = SpringContext.getBean(TestService.class);
  private AnalyzerService analyzerService = SpringContext.getBean(AnalyzerService.class);

  static final String TBil = "T-Bil";
  static final String DBil = "D-Bil";
  static final String IBIL = "IBIL";
  static final String ALT = "ALT";
  static final String AST = "AST";
  static final String ALP = "ALP";
  static final String YGT = "Y-GT";
  static final String LDH = "LDH";
  static final String TG = "TG";
  static final String HDLC = "HDL-C";
  static final String TC = "TC";
  static final String LDL = "LDL";
  static final String VLDL = "VLDL";
  static final String CholHDL = "Chol/HDL";
  static final String CREA = "CREA";
  static final String AZOTE_UREA = "AZOTE UREA";
  static final String Ca = "Ca";
  static final String P = "P";
  static final String TP = "TP";
  static final String ALB = "ALB";
  static final String Globulines = "Globulines";
  static final String Alb_Glob = "Alb/Glob";
  static final String Mg = "Mg";

  static final String TBil_LOINC = "1975-2";
  static final String DBil_LOINC = "1968-7";
  static final String IBIL_LOINC = "1971-1";
  static final String ALT_LOINC = "1742-6";
  static final String AST_LOINC = "1920-8";
  static final String ALP_LOINC = "6768-6";
  static final String YGT_LOINC = "2324-2";
  static final String LDH_LOINC = "14805-6";
  static final String TG_LOINC = "2571-8";
  static final String HDLC_LOINC = "2085-9";
  static final String TC_LOINC = "2093-3";
  static final String LDL_LOINC = "13457-7";
  static final String VLDL_LOINC = "13458-5";
  static final String CholHDL_LOINC = "9830-1";
  static final String CREA_LOINC = "2160-0";
  static final String AZOTE_UREA_LOINC = "3094-0";
  static final String Ca_LOINC = "17861-6";
  static final String P_LOINC = "2777-1";
  static final String TP_LOINC = "2885-2";
  static final String ALB_LOINC = "1751-7";
  static final String Globulines_LOINC = "2336-6";
  static final String Alb_Glob_LOINC = "10834-0";
  static final String Mg_LOINC = "19123-9";

  private String ANALYZER_ID;
  private HashMap<String, List<Test>> testLoincMap = new HashMap<>();

  private AnalyzerReaderUtil readerUtil = new AnalyzerReaderUtil();

  public MindrayAnalyzerImplementation() {
    testLoincMap.put(TBil_LOINC, testService.getTestsByLoincCode(TBil_LOINC));
    testLoincMap.put(DBil_LOINC, testService.getTestsByLoincCode(DBil_LOINC));
    testLoincMap.put(IBIL_LOINC, testService.getTestsByLoincCode(IBIL_LOINC));
    testLoincMap.put(ALT_LOINC, testService.getTestsByLoincCode(ALT_LOINC));
    testLoincMap.put(AST_LOINC, testService.getTestsByLoincCode(AST_LOINC));
    testLoincMap.put(ALP_LOINC, testService.getTestsByLoincCode(ALP_LOINC));
    testLoincMap.put(YGT_LOINC, testService.getTestsByLoincCode(YGT_LOINC));
    testLoincMap.put(LDH_LOINC, testService.getTestsByLoincCode(LDH_LOINC));
    testLoincMap.put(TG_LOINC, testService.getTestsByLoincCode(TG_LOINC));
    testLoincMap.put(HDLC_LOINC, testService.getTestsByLoincCode(HDLC_LOINC));
    testLoincMap.put(TC_LOINC, testService.getTestsByLoincCode(TC_LOINC));
    testLoincMap.put(LDL_LOINC, testService.getTestsByLoincCode(LDL_LOINC));
    testLoincMap.put(VLDL_LOINC, testService.getTestsByLoincCode(VLDL_LOINC));
    testLoincMap.put(CholHDL_LOINC, testService.getTestsByLoincCode(CholHDL_LOINC));
    testLoincMap.put(CREA_LOINC, testService.getTestsByLoincCode(CREA_LOINC));
    testLoincMap.put(AZOTE_UREA_LOINC, testService.getTestsByLoincCode(AZOTE_UREA_LOINC));
    testLoincMap.put(Ca_LOINC, testService.getTestsByLoincCode(Ca_LOINC));
    testLoincMap.put(P_LOINC, testService.getTestsByLoincCode(P_LOINC));
    testLoincMap.put(TP_LOINC, testService.getTestsByLoincCode(TP_LOINC));
    testLoincMap.put(ALB_LOINC, testService.getTestsByLoincCode(ALB_LOINC));
    testLoincMap.put(Globulines_LOINC, testService.getTestsByLoincCode(Globulines_LOINC));
    testLoincMap.put(Alb_Glob_LOINC, testService.getTestsByLoincCode(Alb_Glob_LOINC));
    testLoincMap.put(Mg_LOINC, testService.getTestsByLoincCode(Mg_LOINC));

    Analyzer analyzer = analyzerService.getAnalyzerByName("Mindray");
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
    return false;
  }

  @Override
  public String getError() {
    return "Mindray analyzer unable to write to database";
  }

  public void addResult(
      List<AnalyzerResults> resultList,
      List<AnalyzerResults> notMatchedResults,
      String resultType,
      String resultValue,
      String accessionNumber,
      boolean isControl,
      String resultUnits,
      String analyzerTestId) {
    AnalyzerResults analyzerResults =
        createAnalyzerResult(
            resultType, resultValue, resultUnits, accessionNumber, isControl, analyzerTestId);
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
      String accessionNumber,
      boolean isControl,
      String analyzerTestId) {
    AnalyzerResults analyzerResults = new AnalyzerResults();

    analyzerResults.setAnalyzerId(ANALYZER_ID);
    analyzerResults.setResult(resultValue);
    analyzerResults.setUnits(resultUnits);
    analyzerResults.setCompleteDate(new Timestamp(new Date().getTime()));
    analyzerResults.setAccessionNumber(accessionNumber);

    // Add null safety checks to prevent NullPointerException
    List<Test> tests = testLoincMap.get(analyzerTestId);
    if (tests != null && !tests.isEmpty()) {
      analyzerResults.setTestId(tests.get(0).getId());
      analyzerResults.setTestName(tests.get(0).getLocalizedTestName().getLocalizedValue());
    } else {
      analyzerResults.setTestId("");
      analyzerResults.setTestName("");
    }

    analyzerResults.setIsControl(isControl);
    return analyzerResults;
  }

  public void persistImport(List<AnalyzerResults> resultList) {
    this.persistImport("1", resultList);
  }
}
