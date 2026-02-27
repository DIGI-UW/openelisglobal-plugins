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
import org.openelisglobal.analyzerimport.analyzerreaders.AnalyzerLineInserter;
import org.openelisglobal.analyzerimport.analyzerreaders.AnalyzerReaderUtil;
import org.openelisglobal.analyzerresults.valueholder.AnalyzerResults;
import org.openelisglobal.common.util.DateUtil;
import org.openelisglobal.spring.util.SpringContext;
import org.openelisglobal.test.service.TestService;
import org.openelisglobal.test.valueholder.Test;

public class FullyAnalyzerImplementation extends AnalyzerLineInserter {

  private static final String DELIMITER = "\\t";
  private static final String CONTROL_ACCESSION_PREFIX = "CONTROL";
  static String DATE_PATTERN = "dd/MM/yyyy HH:mm:ss";
  static HashMap<String, Test> testHeaderNameMap = new HashMap<>();
  HashMap<String, String> indexTestMap = new HashMap<>();
  static HashMap<String, String> scaleIndexMap = new HashMap<>();
  static HashMap<String, String> testUnitMap = new HashMap<>();
  private AnalyzerReaderUtil readerUtil = new AnalyzerReaderUtil();

  static {
    testHeaderNameMap.put(
        "Glucose",
        SpringContext.getBean(TestService.class)
            .getTestByName("Glucose")); // .getTestByGUID("0e240569-c095-41c7-bfd2-049527452f16"));
    testHeaderNameMap.put(
        "Creatinine",
        SpringContext.getBean(TestService.class)
            .getTestByName(
                "Créatinine")); // .getTestByGUID("fe6405c8-f96b-491b-95c9-b1f635339d6a"));
    testHeaderNameMap.put(
        "GPT/ALT",
        SpringContext.getBean(TestService.class)
            .getTestByName(
                "Transaminases GPT (37°C)")); // .getTestByGUID("cecea358-1fa0-44b2-8185-d8c010315f78"));
    // testHeaderNameMap.put("Cholesterol",
    // SpringContext.getBean(TestService.class).getTestByName("Cholestérol
    // total"));//.getTestByGUID("fe6405c8-f96b-491b-95c9-b1f635339d6a"));
    // testHeaderNameMap.put("Triglycerides",
    // SpringContext.getBean(TestService.class).getTestByName("Triglycérides"));//.getTestByGUID("cecea358-1fa0-44b2-8185-d8c010315f78"));
    // testHeaderNameMap.put("GOT/AST",
    // SpringContext.getBean(TestService.class).getTestByName("Transaminases"));//.getTestByGUID("fe6405c8-f96b-491b-95c9-b1f635339d6a"));
    // testHeaderNameMap.put("GOT/AST",
    // SpringContext.getBean(TestService.class).getTestByName("Transaminases GOT
    // (37°C)"));//.getTestByGUID("fe6405c8-f96b-491b-95c9-b1f635339d6a"));
    System.out.println(testHeaderNameMap);
  }

  static {
    testUnitMap.put("Glucose", "/1|g/l");
    testUnitMap.put("Creatinine", "/1|g/l");
    testUnitMap.put("GPT/ALT", "/1|UI/L");
    testUnitMap.put("Cholesterol", "/1|g/l");
    testUnitMap.put("Triglycerides", "/1|g/l");
    testUnitMap.put("GOT/AST", "/1|UI/L");

    System.out.println(testUnitMap);
  }

  /* (non-Javadoc)
   * @see org.openelisglobal.analyzerimport.analyzerreaders.AnalyzerLineInserter#insert(java.util.List, java.lang.String)
   */
  @Override
  public boolean insert(List<String> lines, String currentUserId) {

    List<AnalyzerResults> results = new ArrayList<>();

    boolean columnsFound = manageColumnsIndex(lines);

    if (!columnsFound) {
      System.out.println("Fully analyzer: Unable to find correct columns in file");
      return false;
    }

    createAnalyzerResultFromLine(lines, results);

    return persistImport(currentUserId, results);
  }

  private Timestamp getTimestampFromDate(String dateTime) {
    return DateUtil.convertStringDateToTimestampWithPattern(dateTime, DATE_PATTERN);
  }

  @Override
  public String getError() {
    return "Fully analyzer unable to write to database";
  }

  private void addValueToResults(List<AnalyzerResults> resultList, AnalyzerResults result) {
    resultList.add(result);

    AnalyzerResults resultFromDB = readerUtil.createAnalyzerResultFromDB(result);
    if (resultFromDB != null) {
      resultList.add(resultFromDB);
    }
  }

  private boolean manageColumnsIndex(List<String> lines) {
    if (getColumnsLine(lines) < 0) {
      return false;
    }

    for (Integer i = 0; i < lines.size(); i++) {
      String line = lines.get(i);
      if (line.startsWith("Test:")) {
        String[] test = line.split(":");
        if (testHeaderNameMap.containsKey(test[1].trim())) {
          indexTestMap.put(i.toString(), test[1].trim());
        }
      }
    }

    return indexTestMap.size() > 0;
  }

  private void createAnalyzerResultFromLine(List<String> lines, List<AnalyzerResults> resultList) {

    for (String key : indexTestMap.keySet()) {

      String dateLine = lines.get(Integer.parseInt(key) + 1);
      String[] dateTextLine = dateLine.split(":");
      String dateTime = dateTextLine[1].trim();
      dateTime = dateTime.substring(0, 8);
      dateTime =
          dateTime.substring(0, 2)
              + "/"
              + dateTime.substring(2, 4)
              + "/"
              + dateTime.substring(4, 8)
              + " "
              + "00:00:00";

      int resultIndex = Integer.parseInt(key) + 5;

      if (resultIndex == lines.size()) {
        continue;
      }
      lines.add("-------------");
      while (!lines.get(resultIndex).startsWith("------")) {
        String line = lines.get(resultIndex);
        String[] resultsLine = line.split(DELIMITER);
        String testKey = indexTestMap.get(key);
        AnalyzerResults aResult = new AnalyzerResults();
        aResult.setTestId(testHeaderNameMap.get(testKey).getId());
        aResult.setTestName(testHeaderNameMap.get(testKey).getName());
        aResult.setUnits(setUnitByTestKey(testKey));
        aResult.setResult(resultsLine[0].trim());
        aResult.setIsControl(CheckControl(resultsLine[resultsLine.length - 1].trim()));
        // aResult.setAccessionNumber(PutSymbolInAccessionNumber(resultsLine[resultsLine.length-1].trim()));
        String currentAccessionNumber = resultsLine[resultsLine.length - 1].trim();
        // String currentAccessionNumber =
        // PutSymbolInAccessionNumber(resultsLine[resultsLine.length-1].trim());

        aResult.setAccessionNumber(currentAccessionNumber);

        aResult.setCompleteDate(getTimestampFromDate(dateTime));
        if (currentAccessionNumber.contains("CHBKE")
            || currentAccessionNumber.contains(CONTROL_ACCESSION_PREFIX)) {
          addValueToResults(resultList, aResult);
        }

        resultIndex++;
      }
    }
  }

  /*
       private String PutSymbolInAccessionNumber(String testKey) {
                String GoodID = testKey.replaceFirst("CHRM", "CHRM-");
                String unit = GoodID.trim();
                return unit;
            }
  */

  private String setUnitByTestKey(String testKey) {

    String unitKey = testUnitMap.get(testKey);
    int debut = unitKey.indexOf("|");
    int debutChaineUnit = 1 + debut;
    String unit = unitKey.substring(debutChaineUnit);
    return unit;
  }

  private boolean CheckControl(String AccessionPrefix) {

    boolean IsControl = false;

    if (AccessionPrefix.startsWith(CONTROL_ACCESSION_PREFIX)) {
      IsControl = true;
    }
    return IsControl;
  }

  public int getColumnsLine(List<String> lines) {
    for (int k = 0; k < lines.size(); k++) {
      if (lines.get(k).contains("RESULT")
          && lines.get(k).contains("O.D.")
          && lines.get(k).contains("Well O.D.")
          && lines.get(k).contains("ID")
          && lines.get(k).contains("Patient")) {
        return k;
      }
    }

    return -1;
  }
}
