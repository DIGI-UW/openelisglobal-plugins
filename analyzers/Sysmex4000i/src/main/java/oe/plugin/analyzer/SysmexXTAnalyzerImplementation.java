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
import org.openelisglobal.analysis.service.AnalysisService;
import org.openelisglobal.analysis.valueholder.Analysis;
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

public class SysmexXTAnalyzerImplementation extends AnalyzerLineInserter {
  private static final String ANALYZER_NAME = "Sysmex4000iAnalyzer";
  private static final String DELIMITER = ",";
  private static final String DATE_PATTERN = "dd/MM/yyyy HH:mm:ss";
  private static final String CONTROL_ACCESSION_PREFIX = "QC-";

  private int ORDER_NUMBER_INDEX = 0;
  private int ORDER_DAY_INDEX = 0;
  private int ORDER_HOUR_INDEX = 0;

  // Lazy-initialized services
  private TestService testService;
  private AnalysisService analysisService;
  private SampleService sampleService;

  // Lazy-initialized data
  private String projectCode;
  private String validStatusId;
  private HashMap<String, Test> testHeaderNameMap;
  private HashMap<String, String> scaleIndexMap;

  HashMap<String, String> indexTestMap = new HashMap<String, String>();
  private AnalyzerReaderUtil readerUtil = new AnalyzerReaderUtil();

  // Lazy getter for TestService
  protected TestService getTestService() {
    if (testService == null) {
      testService = SpringContext.getBean(TestService.class);
    }
    return testService;
  }

  // Lazy getter for AnalysisService
  protected AnalysisService getAnalysisService() {
    if (analysisService == null) {
      analysisService = SpringContext.getBean(AnalysisService.class);
    }
    return analysisService;
  }

  // Lazy getter for SampleService
  protected SampleService getSampleService() {
    if (sampleService == null) {
      sampleService = SpringContext.getBean(SampleService.class);
    }
    return sampleService;
  }

  // Lazy getter for project code
  protected String getProjectCode() {
    if (projectCode == null) {
      projectCode = MessageUtil.getMessage("sample.entry.project.LART");
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
      testHeaderNameMap.put("GB(10/uL)", ts.getTestByName("GB"));
      testHeaderNameMap.put("GR(10^4/uL)", ts.getTestByName("GR"));
      testHeaderNameMap.put("HBG(g/L)", ts.getTestByName("Hb"));
      testHeaderNameMap.put("HCT(10^(-1)%)", ts.getTestByName("HCT"));
      testHeaderNameMap.put("VGM(10^(-1)fL)", ts.getTestByName("VGM"));
      testHeaderNameMap.put("TCMH(10^(-1)pg)", ts.getTestByName("TCMH"));
      testHeaderNameMap.put("CCMH(g/L)", ts.getTestByName("CCMH"));
      testHeaderNameMap.put("PLQ(10^3/uL)", ts.getTestByName("PLQ"));
      testHeaderNameMap.put("NEUT%(10^(-1)%)", ts.getTestByName("Neut %"));
      testHeaderNameMap.put("LYMPH%(10^(-1)%)", ts.getTestByName("Lymph %"));
      testHeaderNameMap.put("MONO%(10^(-1)%)", ts.getTestByName("Mono %"));
      testHeaderNameMap.put("EO%(10^(-1)%)", ts.getTestByName("Eo %"));
      testHeaderNameMap.put("BASO%(10^(-1)%)", ts.getTestByName("Baso %"));
    }
    return testHeaderNameMap;
  }

  // Lazy getter for scale index map
  protected HashMap<String, String> getScaleIndexMap() {
    if (scaleIndexMap == null) {
      scaleIndexMap = new HashMap<String, String>();
      scaleIndexMap.put("GR(10^4/uL)", "100,10^6uL");
      scaleIndexMap.put("HBG(g/L)", "10,g/dL");
      scaleIndexMap.put("HCT(10^(-1)%)", "10,%");
      scaleIndexMap.put("VGM(10^(-1)fL)", "10,fL");
      scaleIndexMap.put("TCMH(10^(-1)pg)", "10,pg");
      scaleIndexMap.put("CCMH(g/L)", "10,g/dL");
      scaleIndexMap.put("PLQ(10^3/uL)", "1,10^3/uL");
      scaleIndexMap.put("NEUT%(10^(-1)%)", "10,%");
      scaleIndexMap.put("LYMPH%(10^(-1)%)", "10,%");
      scaleIndexMap.put("MONO%(10^(-1)%)", "10,%");
      scaleIndexMap.put("EO%(10^(-1)%)", "10,%");
      scaleIndexMap.put("BASO%(10^(-1)%)", "10,%");
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
    return "SysmexXT analyzer unable to write to database";
  }

  private String[] getAppropriateResults(String result, String testKey) {
    result = result.trim();
    String scale = getScaleIndexMap().get(testKey);
    if (scale == null) {
      return new String[] {result, ""};
    }
    String[] results = scale.split(",");

    int dem = Integer.parseInt(results[0]);

    double d = Double.NaN;

    try {
      d = Double.parseDouble(result) / dem;
    } catch (NumberFormatException nfe) {
      // no-op -- defaults to NAN
    }
    if (dem == 1) results[0] = result;
    else results[0] = String.valueOf(d);
    return results;
  }

  private boolean manageColumnsIndex(List<String> lines) {
    if (getColumnsLine(lines) < 0) return false;

    String[] headers = lines.get(getColumnsLine(lines)).split(DELIMITER);

    for (int i = 0; i < headers.length; i++) {
      String header = headers[i].trim();
      if (getTestHeaderNameMap().containsKey(header)) {
        indexTestMap.put(String.valueOf(i), header);
      } else if ("N' Echantillon".equals(header)) {
        ORDER_NUMBER_INDEX = i;
      } else if ("Ana. Jour".equals(header)) {
        ORDER_DAY_INDEX = i;
      } else if ("Ana. Heure".equals(header)) {
        ORDER_HOUR_INDEX = i;
      }
    }

    return ORDER_NUMBER_INDEX != 0 && ORDER_DAY_INDEX != 0 && ORDER_HOUR_INDEX != 0;
  }

  public int getColumnsLine(List<String> lines) {
    for (int k = 0; k < lines.size(); k++) {
      if (lines.get(k).contains("ID Instrument")
          && lines.get(k).contains("N' Echantillon")
          && lines.get(k).contains("Ana. Jour")
          && lines.get(k).contains("Ana. Heure")
          && lines.get(k).contains("N' Rack")
          && lines.get(k).contains("Pos. Tube")) return k;
    }

    return -1;
  }

  private void createAnalyzerResultFromLine(String line, List<AnalyzerResults> resultList) {
    String[] fields = line.split(DELIMITER);

    for (int k = 0; k < fields.length; k++) {

      if (indexTestMap.containsKey(String.valueOf(k))) {
        String testKey = indexTestMap.get(String.valueOf(k));
        Test test = getTestHeaderNameMap().get(testKey);
        if (test == null) {
          continue;
        }

        AnalyzerResults aResult = new AnalyzerResults();
        aResult.setTestId(test.getId());
        aResult.setTestName(test.getName());

        String[] result = getAppropriateResults(fields[k], testKey);
        aResult.setResult(result[0]);
        aResult.setUnits(result[1]);
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
        getAnalysisService()
            .getAnalysisByAccessionAndTestId(result.getAccessionNumber(), result.getTestId());
    for (Analysis analysis : analyses) {
      if (analysis.getStatusId().equals(getValidStatusId())) return;
    }
    resultList.add(result);

    AnalyzerResults resultFromDB = this.readerUtil.createAnalyzerResultFromDB(result);
    if (resultFromDB != null) resultList.add(resultFromDB);
  }
}
