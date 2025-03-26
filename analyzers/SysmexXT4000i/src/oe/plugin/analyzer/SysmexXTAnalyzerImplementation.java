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
import org.openelisglobal.analyzerimport.analyzerreaders.AnalyzerReaderUtil;
import org.openelisglobal.analyzerresults.valueholder.AnalyzerResults;
import org.openelisglobal.common.util.DateUtil;
import org.openelisglobal.spring.util.SpringContext;
import org.openelisglobal.test.service.TestService;
import org.openelisglobal.test.valueholder.Test;

public class SysmexXTAnalyzerImplementation extends AnalyzerLineInserter {
  private int ORDER_NUMBER_INDEX = 0;
  private int ORDER_DAY_INDEX = 0;
  private int ORDER_HOUR_INDEX = 0;

  private static final String DELIMITER = ",";
  static String ANALYZER_ID;
  private static final String CONTROL_ACCESSION_PREFIX = "QC-";
  static String DATE_PATTERN = "dd/MM/yyyy HH:mm:ss";
  static HashMap<String, Test> testHeaderNameMap = new HashMap<>();
  HashMap<String, String> indexTestMap = new HashMap<>();
  static HashMap<String, String> scaleIndexMap = new HashMap<>();

  private AnalyzerReaderUtil readerUtil = new AnalyzerReaderUtil();

  static {
    testHeaderNameMap.put(
        "GB(10/uL)",
        SpringContext.getBean(TestService.class)
            .getTestByName("GB")); // .getTestByGUID("0e240569-c095-41c7-bfd2-049527452f16"));
    testHeaderNameMap.put(
        "GR(10^4/uL)",
        SpringContext.getBean(TestService.class)
            .getTestByName("GR")); // .getTestByGUID("fe6405c8-f96b-491b-95c9-b1f635339d6a"));
    testHeaderNameMap.put(
        "HBG(g/L)",
        SpringContext.getBean(TestService.class)
            .getTestByName("Hb")); // .getTestByGUID("cecea358-1fa0-44b2-8185-d8c010315f78"));
    testHeaderNameMap.put(
        "HCT(10^(-1)%)",
        SpringContext.getBean(TestService.class)
            .getTestByName("HCT")); // .getTestByGUID("6792a51e-050b-4493-88ca-6f490c20cc5c"));
    testHeaderNameMap.put(
        "VGM(10^(-1)fL)",
        SpringContext.getBean(TestService.class)
            .getTestByName("VGM")); // .getTestByGUID("ddce6c12-e319-455f-9f48-2f6ff363a246"));
    testHeaderNameMap.put(
        "TCMH(10^(-1)pg)",
        SpringContext.getBean(TestService.class)
            .getTestByName("TCMH")); // .getTestByGUID("bf497153-ba88-4fe8-83ee-c144229d7628"));
    testHeaderNameMap.put(
        "CCMH(g/L)",
        SpringContext.getBean(TestService.class)
            .getTestByName("CCMH")); // .getTestByGUID("8ab87a81-6b6b-4d4b-b53b-fac57109e393"));
    testHeaderNameMap.put(
        "PLQ(10^3/uL)",
        SpringContext.getBean(TestService.class)
            .getTestByName("PLQ")); // .getTestByGUID("88b7d8d3-e82b-441f-aff3-1410ba2850a5"));
    testHeaderNameMap.put(
        "NEUT%(10^(-1)%)",
        SpringContext.getBean(TestService.class)
            .getTestByName("Neut %")); // .getTestByGUID("0c25692f-a321-4e9c-9722-ca73f6625cb9"));
    testHeaderNameMap.put(
        "LYMPH%(10^(-1)%)",
        SpringContext.getBean(TestService.class)
            .getTestByName("Lymph %")); // .getTestByGUID("eede92e7-d141-4c76-ab6e-b24ccfc84215"));
    testHeaderNameMap.put(
        "MONO%(10^(-1)%)",
        SpringContext.getBean(TestService.class)
            .getTestByName("Mono %")); // .getTestByGUID("9eece97f-04f3-4381-b378-2a9ac08a535a"));
    testHeaderNameMap.put(
        "EO%(10^(-1)%)",
        SpringContext.getBean(TestService.class)
            .getTestByName("Eo %")); // .getTestByGUID("50b568e8-e9da-428d-9697-8080bca7377b"));
    testHeaderNameMap.put(
        "BASO%(10^(-1)%)",
        SpringContext.getBean(TestService.class)
            .getTestByName("Baso %")); // .getTestByGUID("a41fcfb4-e3ba-4add-ac5d-56fae322cb9e"));

    System.out.println(testHeaderNameMap);

    scaleIndexMap.put("GB(10/uL)", "100,10^3uL");
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

    AnalyzerService analyzerService = SpringContext.getBean(AnalyzerService.class);
    Analyzer analyzer = analyzerService.getAnalyzerByName("SysmexXTAnalyzer");
    ANALYZER_ID = analyzer.getId();
  }

  /* (non-Javadoc)
   * @see org.openelisglobal.analyzerimport.analyzerreaders.AnalyzerLineInserter#insert(java.util.List, java.lang.String)
   */
  @Override
  public boolean insert(List<String> lines, String currentUserId) {

    List<AnalyzerResults> results = new ArrayList<>();

    boolean columnsFound = manageColumnsIndex(lines);

    if (!columnsFound) {
      System.out.println("Sysmex XT 4000i analyzer: Unable to find correct columns in file");
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

  private void addValueToResults(List<AnalyzerResults> resultList, AnalyzerResults result) {
    resultList.add(result);

    AnalyzerResults resultFromDB = readerUtil.createAnalyzerResultFromDB(result);
    if (resultFromDB != null) {
      resultList.add(resultFromDB);
    }
  }

  private String[] getAppropriateResults(String result, String testKey) {
    result = result.trim();
    String scale = scaleIndexMap.get(testKey);
    String[] results = scale.split(",");

    int dem = Integer.parseInt(results[0]);

    double d = Double.NaN;

    try {
      d = Double.parseDouble(result) / dem;
    } catch (NumberFormatException nfe) {
      // no-op -- defaults to NAN
    }
    if (dem == 1) {
      results[0] = result;
    } else {
      results[0] = String.valueOf(d);
    }
    return results;
  }

  private boolean manageColumnsIndex(List<String> lines) {
    if (getColumnsLine(lines) < 0) {
      return false;
    }

    String[] headers = lines.get(getColumnsLine(lines)).split(DELIMITER);

    for (Integer i = 0; i < headers.length; i++) {
      String header = headers[i].trim();
      if (testHeaderNameMap.containsKey(headers[i])) {
        indexTestMap.put(i.toString(), headers[i]);
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
          && lines.get(k).contains("Pos. Tube")) {
        return k;
      }
    }

    return -1;
  }

  private void createAnalyzerResultFromLine(String line, List<AnalyzerResults> resultList) {
    String[] fields = line.split(DELIMITER);

    for (Integer k = 0; k < fields.length; k++) {

      if (indexTestMap.containsKey(k.toString())) {
        String testKey = indexTestMap.get(k.toString());
        AnalyzerResults aResult = new AnalyzerResults();
        aResult.setTestId(testHeaderNameMap.get(testKey).getId());
        aResult.setTestName(testHeaderNameMap.get(testKey).getName());

        String[] result = getAppropriateResults(fields[k], testKey);
        aResult.setResult(result[0]);
        aResult.setUnits(result[1]);
        aResult.setAnalyzerId(ANALYZER_ID);
        aResult.setAccessionNumber(fields[ORDER_NUMBER_INDEX].trim());
        aResult.setResultType("N");

        String dateTime = fields[ORDER_DAY_INDEX].trim();
        dateTime = dateTime + " " + fields[ORDER_HOUR_INDEX].trim();
        aResult.setCompleteDate(getTimestampFromDate(dateTime));

        System.out.println(
            "***"
                + aResult.getAccessionNumber()
                + " "
                + aResult.getCompleteDate()
                + " "
                + aResult.getResult());

        if (aResult.getAccessionNumber() != null) {
          aResult.setIsControl(aResult.getAccessionNumber().startsWith(CONTROL_ACCESSION_PREFIX));
        } else {
          aResult.setIsControl(false);
        }

        addValueToResults(resultList, aResult);
      }
    }
  }
}
