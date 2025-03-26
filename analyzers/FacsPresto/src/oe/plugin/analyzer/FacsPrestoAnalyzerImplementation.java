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
import java.util.HashMap;
import java.util.List;
import org.openelisglobal.analyzer.service.AnalyzerService;
import org.openelisglobal.analyzer.valueholder.Analyzer;
import org.openelisglobal.analyzerimport.analyzerreaders.AnalyzerLineInserter;
import org.openelisglobal.analyzerimport.analyzerreaders.AnalyzerReaderUtil;
import org.openelisglobal.analyzerresults.valueholder.AnalyzerResults;
import org.openelisglobal.common.util.DateUtil;
import org.openelisglobal.spring.util.SpringContext;
import org.openelisglobal.test.service.TestService;
import org.openelisglobal.test.valueholder.Test;

public class FacsPrestoAnalyzerImplementation extends AnalyzerLineInserter {

  private int ORDER_NUMBER = 0;
  private int ORDER_DATE = 0;
  private boolean isControl = false;
  private static final String DELIMITER = ",";
  private static final String DATE_PATTERN = "yyyy-MM-dd HH:mm";
  static String ANALYZER_ID;

  static HashMap<String, Test> testHeaderNameMap = new HashMap<>();
  HashMap<String, String> indexTestMap = new HashMap<>();
  static HashMap<String, String> unitsIndexMap = new HashMap<>();

  private AnalyzerReaderUtil readerUtil = new AnalyzerReaderUtil();
  private String error;
  Test test =
      SpringContext.getBean(TestService.class)
          .getActiveTestByName("Dénombrement des lymphocytes  CD4 (%)")
          .get(0);

  static {
    testHeaderNameMap.put(
        "CD4",
        SpringContext.getBean(TestService.class)
            .getTestByName(
                "Dénombrement des lymphocytes CD4 (mm3)")); // .getTestByGUID("0e240569-c095-41c7-bfd2-049527452f16"));
    testHeaderNameMap.put(
        "%CD4",
        SpringContext.getBean(TestService.class)
            .getTestByName(
                "Dénombrement des lymphocytes  CD4 (%)")); // .getTestByGUID("fe6405c8-f96b-491b-95c9-b1f635339d6a"));
    //	testHeaderNameMap.put("Hb",
    // SpringContext.getBean(TestService.class).getTestByName("Hb"));//.getTestByGUID("cecea358-1fa0-44b2-8185-d8c010315f78"));

    //	System.out.println(testHeaderNameMap);

    unitsIndexMap.put("CD4", "mm3");
    unitsIndexMap.put("%CD4", "%");

    AnalyzerService analyzerService = SpringContext.getBean(AnalyzerService.class);
    Analyzer analyzer = analyzerService.getAnalyzerByName("FacsPrestoAnalyzer");
    ANALYZER_ID = analyzer.getId();
  }

  @Override
  public boolean insert(List<String> lines, String currentUserId) {
    this.error = null;
    List<Integer> columnsList = getColumnsLines(lines);

    if (columnsList == null) {
      return false;
    }

    List<AnalyzerResults> results = new ArrayList<>();

    for (int j : columnsList) {
      getResultsForSampleType(lines, j, results);
    }

    return persistImport(currentUserId, results);
  }

  private boolean manageColumnsIndex(int columsLine, List<String> lines) {
    indexTestMap = new HashMap<>();
    String[] headers = lines.get(columsLine).split(DELIMITER);

    for (Integer i = 0; i < headers.length; i++) {
      String header = headers[i];

      if (testHeaderNameMap.containsKey(headers[i])) {
        indexTestMap.put(i.toString(), headers[i]);
      } else if ("Patient ID".equals(header) || "Process Lot ID".equals(header)) {
        ORDER_NUMBER = i;
      } else if ("Run Date/Time".equals(header)) {
        ORDER_DATE = i;
      } else if ("Level".equals(header)) {
        isControl = true;
      } else if ("Reagent QC Passed?".equals(header)) {
        isControl = false;
      }
    }

    return (ORDER_DATE != 0) && (ORDER_NUMBER != 0) && (indexTestMap.size() > 0);
  }

  @Override
  public String getError() {
    return this.error;
  }

  private void addValueToResults(List<AnalyzerResults> resultList, AnalyzerResults result) {
    resultList.add(result);

    AnalyzerResults resultFromDB = this.readerUtil.createAnalyzerResultFromDB(result);
    if (resultFromDB != null) {
      resultList.add(resultFromDB);
    }
  }

  private void createAnalyzerResultFromLine(String line, List<AnalyzerResults> resultList) {

    String[] fields = line.split(DELIMITER);

    for (Integer k = 0; k < fields.length; k++) {

      if (indexTestMap.containsKey(k.toString())) {

        if (fields[ORDER_NUMBER].trim().contains("CHRSP")) {
          String testKey = indexTestMap.get(k.toString());
          AnalyzerResults aResult = new AnalyzerResults();
          aResult.setTestId(testHeaderNameMap.get(testKey).getId());
          aResult.setTestName(testHeaderNameMap.get(testKey).getName());

          aResult.setResult(fields[k].trim());
          aResult.setAnalyzerId(ANALYZER_ID);

          aResult.setAccessionNumber(fields[ORDER_NUMBER].trim());

          aResult.setUnits(unitsIndexMap.get(testKey));
          aResult.setIsControl(isControl);
          aResult.setResultType("N");

          String dateTime = fields[ORDER_DATE].trim();
          // FORMAT DE LA DATE: yyyy-MM-dd HH:mm
          dateTime = dateTime.replaceAll("A", "");
          dateTime = dateTime.replaceAll("P", "");
          dateTime = dateTime.replaceAll("M", "");

          aResult.setCompleteDate(getTimestampFromDate(dateTime.trim()));

          addValueToResults(resultList, aResult);
        }
      }
    }
  }

  public List<Integer> getColumnsLines(List<String> lines) {
    List<Integer> linesList = new ArrayList<>();
    for (int i = 0; i < lines.size(); i++) {
      System.out.print("******* line:" + i);
      System.out.println(":" + lines.get(i));
      if (lines.get(i).toLowerCase().contains("cd4 process control results")
          || lines.get(i).toLowerCase().contains("patient sample results")) {
        System.out.print("============== line:" + i);
        System.out.println(":" + lines.get(i));
        linesList.add(i + 1);
      }

      // i=i+1;
    }

    return linesList.size() == 0 ? null : linesList;
  }

  private Timestamp getTimestampFromDate(String dateTime) {
    return DateUtil.convertStringDateToTimestampWithPattern(dateTime, DATE_PATTERN);
  }

  public void getResultsForSampleType(
      List<String> lines, int columsLine, List<AnalyzerResults> results) {

    boolean columnsFound = manageColumnsIndex(columsLine, lines);

    if (!columnsFound) {
      System.out.println("BD FACSPresto analyzer: Unable to find correct columns in file");
    }

    for (int i = columsLine + 1; i < lines.size(); ++i) {
      if (lines.get(i).startsWith(",,,,,,")) {
        break;
      }
      createAnalyzerResultFromLine(lines.get(i), results);
    }
  }
}
