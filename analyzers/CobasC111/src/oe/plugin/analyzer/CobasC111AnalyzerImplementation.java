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
import java.util.HashMap;
import java.util.List;
import org.openelisglobal.analyzer.service.AnalyzerService;
import org.openelisglobal.analyzer.valueholder.Analyzer;
import org.openelisglobal.analyzerimport.analyzerreaders.AnalyzerLineInserter;
import org.openelisglobal.analyzerresults.valueholder.AnalyzerResults;
import org.openelisglobal.common.util.DateUtil;
import org.openelisglobal.spring.util.SpringContext;
import org.openelisglobal.test.service.TestService;
import org.openelisglobal.test.valueholder.Test;

public class CobasC111AnalyzerImplementation extends AnalyzerLineInserter {

  private static final String ANALYZER_NAME = "CobasC111Analyzer";
  private static final String DATE_PATTERN = "yyyyMMdd";
  private static final String CONTROL_ACCESSION_PREFIX = "PCC";
  private static final String DELIMITER = ";";

  // Lazy-initialized services
  private TestService testService;
  private AnalyzerService analyzerService;

  // Lazy-initialized data
  private String analyzerId;
  private HashMap<String, Test> testNameMap;
  private HashMap<String, String> testUnitMap;

  double result = Double.NaN;
  String line;
  String[] data;

  // Lazy getter for TestService
  protected TestService getTestService() {
    if (testService == null) {
      testService = SpringContext.getBean(TestService.class);
    }
    return testService;
  }

  // Lazy getter for AnalyzerService
  protected AnalyzerService getAnalyzerService() {
    if (analyzerService == null) {
      analyzerService = SpringContext.getBean(AnalyzerService.class);
    }
    return analyzerService;
  }

  // Lazy getter for analyzer ID
  protected String getAnalyzerId() {
    if (analyzerId == null) {
      Analyzer analyzer = getAnalyzerService().getAnalyzerByName(ANALYZER_NAME);
      if (analyzer != null) {
        analyzerId = analyzer.getId();
      }
    }
    return analyzerId;
  }

  // Lazy getter for test name map
  protected HashMap<String, Test> getTestNameMap() {
    if (testNameMap == null) {
      testNameMap = new HashMap<>();
      testNameMap.put("GLU2", getTestService().getTestByName("Glucose"));
      testNameMap.put("CREJ2", getTestService().getTestByName("Créatinine"));
      testNameMap.put("ALTL", getTestService().getTestByName("Transaminases GPT (37°C)"));
    }
    return testNameMap;
  }

  // Lazy getter for test unit map
  protected HashMap<String, String> getTestUnitMap() {
    if (testUnitMap == null) {
      testUnitMap = new HashMap<>();
      testUnitMap.put("GLU2", "/1|10^3/uL");
      testUnitMap.put("CREJ2", "/1|10^6/uL");
      testUnitMap.put("ALTL", "/1|g/dL");
    }
    return testUnitMap;
  }

  @Override
  public boolean insert(List<String> lines, String currentUserId) {
    List<AnalyzerResults> results = new ArrayList<>();

    for (Integer j = 1; j < lines.size(); j++) {
      System.out.println("processing line #: " + j);
      line = lines.get(j);
      data = line.split(";");

      if (line.length() == 0 || data.length == 0) {
        continue;
      }

      String currentAccessionNumber = data[10].replace("\"", "").trim();
      if (currentAccessionNumber.contains("CHRSP")
          || currentAccessionNumber.startsWith(CONTROL_ACCESSION_PREFIX)) {

        if (data[3].contains("690") || data[3].contains("685") || data[3].contains("767")) {
          String testKey = data[8].replace("\"", "").trim();
          String date = data[4].replace("\"", "");
          AnalyzerResults aResult = new AnalyzerResults();

          Test test = getTestNameMap().get(testKey);
          if (test != null) {
            aResult.setTestId(test.getId());
            aResult.setTestName(test.getName());
          }
          aResult.setResult(data[12].replace("\"", "").trim());
          aResult.setAnalyzerId(getAnalyzerId());
          aResult.setUnits(data[13].replace("\"", ""));
          aResult.setAccessionNumber(data[10].replace("\"", "").trim());
          aResult.setIsControl(CheckControl(currentAccessionNumber));
          aResult.setCompleteDate(getTimestampFromDate(date));

          System.out.println(
              "***"
                  + aResult.getAccessionNumber()
                  + " "
                  + aResult.getCompleteDate()
                  + " "
                  + aResult.getResult());

          results.add(aResult);
        }
      }
    }
    return persistImport(currentUserId, results);
  }

  private boolean CheckControl(String AccessionPrefix) {
    boolean IsControl = false;
    if (AccessionPrefix.startsWith(CONTROL_ACCESSION_PREFIX)) {
      IsControl = true;
    }
    return IsControl;
  }

  private Timestamp getTimestampFromDate(String dateTime) {
    return DateUtil.convertStringDateToTimestampWithPattern(dateTime, DATE_PATTERN);
  }

  @Override
  public String getError() {
    return "Cobas C111 analyzer unable to write to database";
  }
}
