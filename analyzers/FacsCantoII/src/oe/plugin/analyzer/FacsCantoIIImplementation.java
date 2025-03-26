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
import org.openelisglobal.common.services.PluginAnalyzerService;
import org.openelisglobal.common.util.DateUtil;
import org.openelisglobal.spring.util.SpringContext;
import org.openelisglobal.test.service.TestService;
import org.openelisglobal.test.valueholder.Test;

public class FacsCantoIIImplementation extends AnalyzerLineInserter {

  static String ANALYZER_ID;
  // static String DATE_PATTERN = "dd/MM/yyyy HH:mm:ss";
  static String DATE_PATTERN = "MMM dd yyyy hh:mm:ss a";
  private static final String CONTROL_ACCESSION_PREFIX = "QA";
  private static final String CONTROL_ACCESSION_PREFIX2 = "IMM";
  private static final String[] units = new String[100];
  private static final int[] scaleIndex = new int[100];
  private static final String DELIMITER = ",";
  double result = Double.NaN;
  String line;
  String[] data;

  static HashMap<String, Test> testHeaderNameMap = new HashMap<>();
  static HashMap<String, String> testUnitMap = new HashMap<>();
  HashMap<String, String> indexTestMap = new HashMap<>();
  List<PluginAnalyzerService.TestMapping> nameMappinng = new ArrayList<>();

  static {
    testHeaderNameMap.put(
        "CD3 %Parent",
        SpringContext.getBean(TestService.class).getTestByName("CD3 percentage count"));
    testHeaderNameMap.put(
        "CD4 %Parent",
        SpringContext.getBean(TestService.class).getTestByName("CD4 percentage count"));
    // testHeaderNameMap.put("Q2 %Parent",
    // SpringContext.getBean(TestService.class).getTestByName("Dénombrement des lymphocytes  CD4
    // (%)"));

    System.out.println(testHeaderNameMap);
    AnalyzerService analyzerService = SpringContext.getBean(AnalyzerService.class);
    Analyzer analyzer = analyzerService.getAnalyzerByName("FacsCantoII");
    ANALYZER_ID = analyzer.getId();
  }

  static {
    testUnitMap.put("CD3 %Parent", "/1|%");
    testUnitMap.put("CD4 %Parent", "/1|%");

    System.out.println(testUnitMap);
  }

  /* (non-Javadoc)
   * @see org.openelisglobal.analyzerimport.analyzerreaders.AnalyzerLineInserter#insert(java.util.List, java.lang.String)
   */
  @Override
  public boolean insert(List<String> lines, String currentUserId) {
    List<AnalyzerResults> results = new ArrayList<>();
    boolean readData = false;
    int accessionNumberIndex = -1;
    int accessionNumberIndex2 = -1;
    int hourIndex = -1;
    int dayIndex = -1;

    String[] headers = lines.get(0).split(DELIMITER);
    for (Integer i = 0; i < headers.length; i++) {
      if (testHeaderNameMap.containsKey(headers[i])) {
        indexTestMap.put(i.toString(), headers[i]);
      } else if ("SAMPLE ID".equals(headers[i])) {
        accessionNumberIndex = i;

      } else if ("Record Date".equals(headers[i])) {
        dayIndex = i;
      }
    }

    for (Integer j = 1; j < lines.size(); j++) {
      System.out.println("processing line #: " + j);
      line = lines.get(j);
      data = line.split(",");

      if (line.length() == 0 || data.length == 0) {
        continue;
      }
      String dateTime = "";
      String dateTime2 = "";

      String currentAccessionNumber = "";

      for (Integer k = 0; k < data.length; k++) {

        if (indexTestMap.containsKey(k.toString())) {

          String testKey = indexTestMap.get(k.toString());
          AnalyzerResults aResult = new AnalyzerResults();
          aResult.setTestId(testHeaderNameMap.get(testKey).getId());
          aResult.setTestName(testHeaderNameMap.get(testKey).getName());
          aResult.setResult(setResultByTest(k + 1, testKey));
          aResult.setAnalyzerId(ANALYZER_ID);
          aResult.setUnits(setUnitByTestKey(testKey));
          aResult.setAccessionNumber(currentAccessionNumber);
          aResult.setReadOnly(CheckReadOnly(testKey));
          aResult.setIsControl(CheckControl(currentAccessionNumber));
          aResult.setCompleteDate(getTimestampFromDate(dateTime));

          System.out.println(
              "***"
                  + aResult.getAccessionNumber()
                  + " "
                  + aResult.getCompleteDate()
                  + " "
                  + aResult.getResult());

          results.add(aResult);

          if (data[k].length() == 0) {
            break;
          }
        } else if (k == accessionNumberIndex) {

          if (!(data[k + 1].contains("SAMPLE"))) {
            currentAccessionNumber = data[k + 1].trim();
          } else {
            break;
          }
          if (data[k].length() == 0) {
            break;
          }
        } else if (k == dayIndex) {

          if (data[k].contains("Record")) {

            break;
          } else {

            dateTime = data[k].trim();
            dateTime2 = data[k + 1].trim();
            dateTime = dateTime + data[k + 1];
            dateTime = dateTime.replace("\"", "");
            dateTime = dateTime.trim();
          }
        }
      }
    }
    return persistImport(currentUserId, results);
  }

  private String setUnitByTestKey(String testKey) {

    String unitKey = testUnitMap.get(testKey);
    int debut = unitKey.indexOf("|");
    int debutChaineUnit = 1 + debut;
    String unit = unitKey.substring(debutChaineUnit);
    return unit;
  }

  private String setResultByTest(int i, String testKey) {

    String unitKey = testUnitMap.get(testKey);
    int debut = unitKey.indexOf("|");
    String scale = unitKey.substring(1, debut);
    String operateur = unitKey.substring(0, 1);
    if (operateur.equals("/")) {

      try {
        result = Double.parseDouble(data[i].trim()) / Integer.parseInt(scale);
      } catch (NumberFormatException nfe) {
        // no-op -- defaults to NAN
      }
    } else if (operateur.equals("*")) {

      try {
        result = Double.parseDouble(data[i].trim()) * Integer.parseInt(scale);
      } catch (NumberFormatException nfe) {
        // no-op -- defaults to NAN
      }
    }

    String TestResult = String.valueOf(result);
    return TestResult;
  }

  private boolean CheckControl(String AccessionPrefix) {

    boolean IsControl = false;

    if ((AccessionPrefix.startsWith(CONTROL_ACCESSION_PREFIX))
        || (AccessionPrefix.startsWith(CONTROL_ACCESSION_PREFIX2))) {
      IsControl = true;
    }
    return IsControl;
  }

  private boolean CheckReadOnly(String testKey) {

    boolean ReadOnly = false;

    if (testKey.equals("Q2 #Events")) {
      ReadOnly = true;
    }
    return ReadOnly;
  }

  private Timestamp getTimestampFromDate(String dateTime) {
    // return DateUtil.convertStringDateToTimestampWithPattern(dateTime, DATE_PATTERN);
    return DateUtil.convertStringDateToTimestampWithPatternNoLocale(dateTime, DATE_PATTERN);
  }

  @Override
  public String getError() {
    return "FacsCantoII analyzer unable to write to database";
  }
}
