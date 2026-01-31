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

  private int ORDER_NUMBER = 0;
  private int TARGET_NUMBER = 0;
  private int VALUE_NUMBER = 0;
  String result = "";
  private boolean isControl = false;
  private static final String DELIMITER = ",";
  private static final String DATE_PATTERN = "yyyy-MM-dd HH:mm";
  static String ANALYZER_ID;

  static HashMap<String, Test> testHeaderNameMap = new HashMap<String, Test>();
  HashMap<String, String> indexTestMap = new HashMap<String, String>();
  static HashMap<String, String> unitsIndexMap = new HashMap<String, String>();

  private AnalyzerReaderUtil readerUtil = new AnalyzerReaderUtil();
  private String error;
  String dateTime = "";

  private final String accession_number_prefix =
      MessageUtil.getMessage("sample.entry.project.LART");
  // private final String accession_number_prefix =
  // ConfigurationProperties.getInstance().getPropertyValue(Property.ACCESSION_NUMBER_PREFIX);

  String validStatusId =
      StatusService.getInstance().getStatusID(StatusService.AnalysisStatus.Finalized);
  AnalysisDAO analysisDao = new AnalysisDAOImpl();
  Test test =
      (Test) SpringContext.getBean(TestService.class).getActiveTestByName("Viral Load").get(0);

  static {
    testHeaderNameMap.put(
        "Quantity", SpringContext.getBean(TestService.class).getTestByName("Viral Load"));

    unitsIndexMap.put("Quantity", "cp/ml");

    AnalyzerService analyzerService = SpringContext.getBean(AnalyzerService.class);
    Analyzer analyzer = analyzerService.getAnalyzerByName("AB7500VLAnalyzer");
    ANALYZER_ID = analyzer.getId();
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

      if (testHeaderNameMap.containsKey(headers[i])) {
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
    SampleService sampleServ = SpringContext.getBean(SampleService.class);
    if (!result.getAccessionNumber().startsWith(accession_number_prefix)
        || sampleServ.getSampleByAccessionNumber(result.getAccessionNumber()) == null) return;

    List<Analysis> analyses =
        analysisDao.getAnalysisByAccessionAndTestId(
            result.getAccessionNumber(), result.getTestId());
    for (Analysis analysis : analyses) {
      if (analysis.getStatusId().equals(validStatusId)) return;
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
        aResult.setTestId(testHeaderNameMap.get(testKey).getId());
        aResult.setTestName(testHeaderNameMap.get(testKey).getName());

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
        aResult.setAnalyzerId(ANALYZER_ID);
        aResult.setAccessionNumber(AccessionNumber);
        aResult.setUnits(unitsIndexMap.get(testKey));
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
