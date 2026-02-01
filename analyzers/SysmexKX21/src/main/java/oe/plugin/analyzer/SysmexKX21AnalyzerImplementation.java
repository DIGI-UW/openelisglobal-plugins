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
import org.openelisglobal.analysis.dao.AnalysisDAO;
import org.openelisglobal.analysis.daoimpl.AnalysisDAOImpl;
import org.openelisglobal.analysis.valueholder.Analysis;
import org.openelisglobal.analyzer.service.AnalyzerService;
import org.openelisglobal.analyzer.valueholder.Analyzer;
import org.openelisglobal.analyzerimport.analyzerreaders.AnalyzerLineInserter;
import org.openelisglobal.analyzerimport.analyzerreaders.AnalyzerReaderUtil;
import org.openelisglobal.analyzerresults.valueholder.AnalyzerResults;
import org.openelisglobal.common.services.StatusService;
import org.openelisglobal.common.util.ConfigurationProperties;
import org.openelisglobal.common.util.ConfigurationProperties.Property;
import org.openelisglobal.common.util.DateUtil;
import org.openelisglobal.sample.service.SampleService;
import org.openelisglobal.spring.util.SpringContext;
import org.openelisglobal.test.service.TestService;
import org.openelisglobal.test.valueholder.Test;

public class SysmexKX21AnalyzerImplementation extends AnalyzerLineInserter {
  private static final String ANALYZER_NAME = "SysmexKX21Analyzer";
  private static final String DELIMITER = ";";
  private static final String DATE_PATTERN = "dd/MM/yyyy hh:mm";
  private static final String CONTROL_ACCESSION_PREFIX = "QC-";

  private int ORDER_NUMBER_INDEX = -1;
  private int ORDER_DAY_INDEX = -1;
  private int ORDER_HOUR_INDEX = -1;

  // Lazy-initialized services
  private TestService testService;
  private AnalyzerService analyzerService;
  private SampleService sampleService;

  // Lazy-initialized data
  private String analyzerId;
  private String projectCode;
  private String validStatusId;
  private HashMap<String, Test> testHeaderNameMap;
  private HashMap<String, String> scaleIndexMap;

  HashMap<String, String> indexTestMap = new HashMap<String, String>();
  private AnalyzerReaderUtil readerUtil = new AnalyzerReaderUtil();
  AnalysisDAO analysisDao = new AnalysisDAOImpl();

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
      }
    }
    return analyzerId;
  }

  // Lazy getter for project code
  protected String getProjectCode() {
    if (projectCode == null) {
      projectCode =
          ConfigurationProperties.getInstance().getPropertyValue(Property.ACCESSION_NUMBER_PREFIX);
    }
    return projectCode;
  }

  // Lazy getter for valid status ID
  protected String getValidStatusId() {
    if (validStatusId == null) {
      validStatusId =
          StatusService.getInstance().getStatusID(StatusService.AnalysisStatus.Finalized);
    }
    return validStatusId;
  }

  // Lazy getter for test header name map
  protected HashMap<String, Test> getTestHeaderNameMap() {
    if (testHeaderNameMap == null) {
      testHeaderNameMap = new HashMap<String, Test>();
      TestService ts = getTestService();
      testHeaderNameMap.put("GB", ts.getTestByName("Numération des globules blancs"));
      testHeaderNameMap.put("GR", ts.getTestByName("Numération des globules rouges"));
      testHeaderNameMap.put("HB", ts.getTestByName("Hémoglobine"));
      testHeaderNameMap.put("Hct", ts.getTestByName("Hématocrite"));
      testHeaderNameMap.put("VGM", ts.getTestByName("Volume Globulaire Moyen"));
      testHeaderNameMap.put("TCMH", ts.getTestByName("Teneur Corpusculaire Moyenne en Hémoglobine"));
      testHeaderNameMap.put(
          "CCMH", ts.getTestByName("Concentration Corpusculaire Moyenne en Hémoglobine"));
      testHeaderNameMap.put("PLT", ts.getTestByName("Plaquette"));
      testHeaderNameMap.put("GRAN%", ts.getTestByName("Polynucléaires Neutrophiles (%)"));
      testHeaderNameMap.put("LYM%", ts.getTestByName("Lymphocytes (%)"));
      testHeaderNameMap.put("MONO%", ts.getTestByName("Monocytes (%)"));
      testHeaderNameMap.put("GRAN#", ts.getTestByName("Polynucléaires Neutrophiles (Abs)"));
      testHeaderNameMap.put("LYM#", ts.getTestByName("Lymphocytes (Abs)"));
      testHeaderNameMap.put("MONO#", ts.getTestByName("Monocytes (Abs)"));
    }
    return testHeaderNameMap;
  }

  // Lazy getter for scale index map
  protected HashMap<String, String> getScaleIndexMap() {
    if (scaleIndexMap == null) {
      scaleIndexMap = new HashMap<String, String>();
      scaleIndexMap.put("GB", "1,10^3uL");
      scaleIndexMap.put("GR", "1,10^6uL");
      scaleIndexMap.put("HB", "1,g/dL");
      scaleIndexMap.put("Hct", "1,%");
      scaleIndexMap.put("VGM", "1,fL");
      scaleIndexMap.put("TCMH", "1,pg");
      scaleIndexMap.put("CCMH", "1,g/dL");
      scaleIndexMap.put("PLT", "1,10^3/uL");
      scaleIndexMap.put("GRAN%", "1,%");
      scaleIndexMap.put("LYM%", "1,%");
      scaleIndexMap.put("MONO%", "1,%");
      scaleIndexMap.put("GRAN#", "1000,/mm3");
      scaleIndexMap.put("LYM#", "1000,/mm3");
      scaleIndexMap.put("MONO#", "1000,/mm3");
    }
    return scaleIndexMap;
  }

  @Override
  public boolean insert(List<String> lines, String currentUserId) {

    List<AnalyzerResults> results = new ArrayList<AnalyzerResults>();

    boolean columnsFound = manageColumnsIndex(lines);

    if (!columnsFound) {
      return false;
    }

    for (int i = getColumnsLine(lines) + 1; i < lines.size(); ++i) {
      createAnalyzerResultFromLine(lines.get(i), results);
    }

    return persistImport(currentUserId, results);
  }

  private Timestamp getTimestampFromDate(String dateTime) {
    return DateUtil.convertStringDateToTimestampWithPattern(dateTime, DATE_PATTERN);
  }

  @Override
  public String getError() {
    return "KX21 analyzer unable to write to database";
  }

  private String[] getAppropriateResults(String result, String testKey) {
    result = result.trim().replace(",", ".");
    String scale = getScaleIndexMap().get(testKey);
    String[] results = scale.split(",");
    double d = Double.NaN;
    if (!result.equals("")) {
      int dem = Integer.parseInt(results[0]);

      try {
        d = Double.parseDouble(result) * Double.valueOf(dem);
      } catch (NumberFormatException nfe) {
        // no-op -- defaults to NAN
      }
      if (dem == 1) results[0] = result;
      else results[0] = String.valueOf(d);
    } else {
      results[0] = result;
    }

    return results;
  }

  private boolean manageColumnsIndex(List<String> lines) {
    if (getColumnsLine(lines) < 0) return false;
    String[] headers = lines.get(getColumnsLine(lines)).split(DELIMITER);

    for (Integer i = 0; i < headers.length; i++) {
      String header = headers[i].trim();
      if (getTestHeaderNameMap().containsKey(header)) {
        indexTestMap.put(i.toString(), header);
      } else if (header.contains("KX21-NERG")) {
        ORDER_NUMBER_INDEX = i;
      } else if (header.contains("DATE")) {
        ORDER_DAY_INDEX = i;
      } else if (header.contains("HEURE")) {
        ORDER_HOUR_INDEX = i;
      }
    }

    return ORDER_NUMBER_INDEX != -1 && ORDER_DAY_INDEX != -1 && ORDER_HOUR_INDEX != -1;
  }

  public int getColumnsLine(List<String> lines) {
    for (int k = 0; k < lines.size(); k++) {
      if (lines.get(k).contains("KX21")
          && lines.get(k).contains("LYM%")
          && lines.get(k).contains("GRAN%")) return k;
    }

    return -1;
  }

  private void createAnalyzerResultFromLine(String line, List<AnalyzerResults> resultList) {
    String[] fields = line.split(DELIMITER);

    for (Integer k = 0; k < fields.length; k++) {

      if (indexTestMap.containsKey(k.toString())) {
        String testKey = indexTestMap.get(k.toString());
        AnalyzerResults aResult = new AnalyzerResults();
        Test test = getTestHeaderNameMap().get(testKey);
        if (test != null) {
          aResult.setTestId(test.getId());
          aResult.setTestName(test.getName());
        }

        String[] result = getAppropriateResults(fields[k], testKey);
        aResult.setResult(result[0]);
        aResult.setUnits(result[1]);
        aResult.setAnalyzerId(getAnalyzerId());
        aResult.setAccessionNumber(fields[ORDER_NUMBER_INDEX].trim());
        aResult.setResultType("N");

        String dateTime = fields[ORDER_DAY_INDEX].trim();
        dateTime = dateTime + " " + fields[ORDER_HOUR_INDEX].trim();
        aResult.setCompleteDate(getTimestampFromDate(dateTime));

        if (aResult.getAccessionNumber() != null) {
          aResult.setIsControl(aResult.getAccessionNumber().startsWith(CONTROL_ACCESSION_PREFIX));
        } else {
          aResult.setIsControl(false);
        }

        addValueToResults(resultList, aResult);
      }
    }
  }

  private void addValueToResults(List<AnalyzerResults> resultList, AnalyzerResults result) {

    if (result.getIsControl()) {
      resultList.add(result);
      return;
    }
    if (!result.getAccessionNumber().startsWith(getProjectCode())
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
}
