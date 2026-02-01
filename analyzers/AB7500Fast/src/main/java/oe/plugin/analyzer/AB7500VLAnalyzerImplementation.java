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
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import org.openelisglobal.analysis.dao.AnalysisDAO;
import org.openelisglobal.analysis.daoimpl.AnalysisDAOImpl;
import org.openelisglobal.analysis.valueholder.Analysis;
import org.openelisglobal.analyzer.service.AnalyzerService;
import org.openelisglobal.analyzer.valueholder.Analyzer;
import org.openelisglobal.analyzerimport.analyzerreaders.AnalyzerLineInserter;
import org.openelisglobal.analyzerimport.analyzerreaders.AnalyzerReaderUtil;
import org.openelisglobal.analyzerresults.valueholder.AnalyzerResults;
import org.openelisglobal.common.services.StatusService;
import org.openelisglobal.common.util.DateUtil;
import org.openelisglobal.internationalization.MessageUtil;
import org.openelisglobal.sample.service.SampleService;
import org.openelisglobal.spring.util.SpringContext;
import org.openelisglobal.test.service.TestService;
import org.openelisglobal.test.valueholder.Test;

public class AB7500VLAnalyzerImplementation extends AnalyzerLineInserter {

  private static final String DELIMITER = ",";
  private static final String DATE_PATTERN = "yyyy-MM-dd HH:mm";
  private static final String ANALYZER_NAME = "AB7500VLAnalyzer";

  private int ORDER_NUMBER = 0;
  private int TARGET_NUMBER = 0;
  private int VALUE_NUMBER = 0;
  String result = "";
  private boolean isControl = false;

  // Lazy-initialized service references (allows unit testing without Spring context)
  private TestService testService;
  private AnalyzerService analyzerService;
  private SampleService sampleService;

  // Lazy-initialized data
  private String analyzerId;
  private HashMap<String, Test> testHeaderNameMap;
  private HashMap<String, String> unitsIndexMap;
  private String validStatusId;
  private Test viralLoadTest;

  HashMap<String, String> indexTestMap = new HashMap<String, String>();

  private AnalyzerReaderUtil readerUtil = new AnalyzerReaderUtil();
  private String error;
  String dateTime = "";

  // Lazy-initialized to allow unit testing
  private String accessionNumberPrefix;

  AnalysisDAO analysisDao = new AnalysisDAOImpl();

  // Lazy getter for accession number prefix
  protected String getAccessionNumberPrefix() {
    if (accessionNumberPrefix == null) {
      accessionNumberPrefix = MessageUtil.getMessage("sample.entry.project.LART");
    }
    return accessionNumberPrefix;
  }

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

  // Lazy getter for SampleService
  protected SampleService getSampleService() {
    if (sampleService == null) {
      sampleService = SpringContext.getBean(SampleService.class);
    }
    return sampleService;
  }

  // Lazy getter for analyzer ID
  protected String getAnalyzerId() {
    if (analyzerId == null) {
      Analyzer analyzer = getAnalyzerService().getAnalyzerByName(ANALYZER_NAME);
      if (analyzer != null) {
        analyzerId = analyzer.getId();
      } else {
        error = "Analyzer not found: " + ANALYZER_NAME;
      }
    }
    return analyzerId;
  }

  // Lazy getter for test name map
  protected HashMap<String, Test> getTestHeaderNameMap() {
    if (testHeaderNameMap == null) {
      testHeaderNameMap = new HashMap<String, Test>();
      Test viralLoad = getTestService().getTestByName("Viral Load");
      if (viralLoad != null) {
        testHeaderNameMap.put("Quantity", viralLoad);
      } else {
        error = "Test not found: Viral Load";
      }
    }
    return testHeaderNameMap;
  }

  // Lazy getter for units map
  protected HashMap<String, String> getUnitsIndexMap() {
    if (unitsIndexMap == null) {
      unitsIndexMap = new HashMap<String, String>();
      unitsIndexMap.put("Quantity", "cp/ml");
    }
    return unitsIndexMap;
  }

  // Lazy getter for valid status ID
  protected String getValidStatusId() {
    if (validStatusId == null) {
      validStatusId =
          StatusService.getInstance().getStatusID(StatusService.AnalysisStatus.Finalized);
    }
    return validStatusId;
  }

  // Lazy getter for viral load test (used for display)
  protected Test getViralLoadTest() {
    if (viralLoadTest == null) {
      List<Test> tests = getTestService().getActiveTestByName("Viral Load");
      if (tests != null && !tests.isEmpty()) {
        viralLoadTest = tests.get(0);
      }
    }
    return viralLoadTest;
  }

  public boolean insert(List<String> lines, String currentUserId) {
    this.error = null;
    List<Integer> columnsList = getColumnsLines(lines);

    if (columnsList == null) return false;

    List<AnalyzerResults> results = new ArrayList<AnalyzerResults>();

    for (int j : columnsList) getResultsForSampleType(lines, j, results);

    return persistImport(currentUserId, results);
  }

  private boolean manageColumnsIndex(int columsLine, List<String> lines) {
    indexTestMap = new HashMap<String, String>();
    String[] headers = lines.get(columsLine).split(DELIMITER);

    for (Integer i = 0; i < headers.length; i++) {
      String header = headers[i];

      if (getTestHeaderNameMap().containsKey(headers[i])) {
        indexTestMap.put(i.toString(), headers[i]);
      } else if ("Sample Name".equals(header)) {
        ORDER_NUMBER = i;
      } else if ("Target Name".equals(header)) {
        TARGET_NUMBER = i;
      } else if (("C?".equals(header)) || ("Cт".equals(header)) || ("CÑ,".equals(header))) {
        VALUE_NUMBER = i;
      }
    }

    return (indexTestMap.size() > 0);
  }

  public String getError() {
    return this.error;
  }

  private void addValueToResults(List<AnalyzerResults> resultList, AnalyzerResults result) {
    /*
     * if (result.getIsControl()){ resultList.add(result); return; }
     */
    if (!result.getAccessionNumber().startsWith(getAccessionNumberPrefix())
        || getSampleService().getSampleByAccessionNumber(result.getAccessionNumber()) == null)
      return;

    List<Analysis> analyses =
        analysisDao.getAnalysisByAccessionAndTestId(
            result.getAccessionNumber(), result.getTestId());
    for (Analysis analysis : analyses) {
      if (analysis.getStatusId().equals(getValidStatusId())) return;
    }
    resultList.add(result);

    AnalyzerResults resultFromDB = this.readerUtil.createAnalyzerResultFromDB(result);
    if (resultFromDB != null) resultList.add(resultFromDB);
  }

  private void createAnalyzerResultFromLine(String line, List<AnalyzerResults> resultList) {

    String[] fields = line.split(DELIMITER);

    for (Integer k = 0; k < fields.length; k++) {

      if (indexTestMap.containsKey(k.toString())) {
        String testKey = indexTestMap.get(k.toString());
        AnalyzerResults aResult = new AnalyzerResults();
        Double resultAsDouble;
        String AccessionNumber = "";
        String resultfinal = "";
        aResult.setTestId(getTestHeaderNameMap().get(testKey).getId());
        aResult.setTestName(getTestHeaderNameMap().get(testKey).getName());

        // ----for result
        if (fields[VALUE_NUMBER].contains("Undetermined") && fields[k].isEmpty()) {
          result = fields[VALUE_NUMBER].trim();

        } else if (!fields[k].isEmpty()) {
          result = fields[k].trim();
          resultAsDouble = Math.log10(Double.parseDouble(result));
          DecimalFormat df = new DecimalFormat("#.##");
          resultfinal = result + "(" + df.format(resultAsDouble).toString() + ")";
          result = resultfinal;
        }

        // ----for accession number
        if (!fields[ORDER_NUMBER].isEmpty()) {
          AccessionNumber = fields[ORDER_NUMBER].trim();

        } else {
          AccessionNumber = fields[TARGET_NUMBER].trim();
        }

        if (AccessionNumber.startsWith("BIOCENTRIC")) {

          isControl = true;
        } else {
          isControl = false;
        }

        aResult.setResult(result);
        aResult.setAnalyzerId(getAnalyzerId());
        aResult.setAccessionNumber(AccessionNumber);
        aResult.setUnits(getUnitsIndexMap().get(testKey));
        aResult.setIsControl(isControl);
        aResult.setResultType("A");

        dateTime = dateTime.replaceAll("A", "");
        dateTime = dateTime.replaceAll("P", "");
        dateTime = dateTime.replaceAll("M", "");
        dateTime = dateTime.replaceAll("G", "");
        dateTime = dateTime.replaceAll("T", "");

        aResult.setCompleteDate(getTimestampFromDate(dateTime.trim()));

        // System.out.print(" date: "+aResult.getCompleteDate() + " AccessionNumber:
        // "+aResult.getAccessionNumber() + " Result: "+aResult.getResult());

        addValueToResults(resultList, aResult);
      }
    }
  }

  public List<Integer> getColumnsLines(List<String> lines) {
    List<Integer> linesList = new ArrayList<Integer>();
    for (int i = 0; i < lines.size(); i++) {
      System.out.print("******* line:" + i);
      System.out.println(":" + lines.get(i));

      if (lines.get(i).contains("Sample Name")) {
        System.out.print("============== line:" + i);
        System.out.println(":" + lines.get(i));
        linesList.add(i);
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

    if (!columnsFound)
      for (int i = columsLine + 1; i < lines.size(); ++i) {
        if (lines.get(i).startsWith(",,,,,,")) break;
        createAnalyzerResultFromLine(lines.get(i), results);
      }
  }
}
