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
import java.util.Map;
import org.openelisglobal.analyzerimport.analyzerreaders.AnalyzerLineInserter;
import org.openelisglobal.analyzerresults.valueholder.AnalyzerResults;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.util.DateUtil;
import org.openelisglobal.dictionary.service.DictionaryService;
import org.openelisglobal.dictionary.valueholder.Dictionary;
import org.openelisglobal.spring.util.SpringContext;
import org.openelisglobal.test.service.TestService;
import org.openelisglobal.test.valueholder.Test;
import org.openelisglobal.testresult.service.TestResultService;
import org.openelisglobal.testresult.valueholder.TestResult;

public class TaqMan48DBSAnalyzerImplementation extends AnalyzerLineInserter {

  private static final String DELIMITER = "\\t";
  private static final String DATE_PATTERN = "yyyy/MM/dd HH:mm:ss";
  private static final String ANALYZER_NAME = "TaqMan48DBSAnalyzer";

  // Lazy-initialized services
  private TestService testService;
  private DictionaryService dictionaryService;
  private TestResultService testResultService;

  // Lazy-initialized data
  private Map<String, Test> testHeaderNameMap;
  private String negativeId;
  private String positiveId;
  private String validId;
  private String invalidId;
  private boolean dictionaryIdsInitialized = false;

  private HashMap<String, String> indexTestMap = new HashMap<>();

  // Lazy getters for services
  protected TestService getTestService() {
    if (testService == null) {
      testService = SpringContext.getBean(TestService.class);
    }
    return testService;
  }
    return analyzerService;
  }

  protected DictionaryService getDictionaryService() {
    if (dictionaryService == null) {
      dictionaryService = SpringContext.getBean(DictionaryService.class);
    }
    return dictionaryService;
  }

  protected TestResultService getTestResultService() {
    if (testResultService == null) {
      testResultService = SpringContext.getBean(TestResultService.class);
    }
    return testResultService;
  }

  protected Map<String, Test> getTestHeaderNameMap() {
    if (testHeaderNameMap == null) {
      testHeaderNameMap = new HashMap<>();
      Test test = getTestService().getTestByGUID("27e4527f-1e31-4ddf-b2ba-39b1a11da0d5");
      if (test != null) {
        testHeaderNameMap.put("Result", test);
      } else {
        LogEvent.logWarn(
            this.getClass().getSimpleName(),
            "getTestHeaderNameMap",
            "Test not found by GUID: 27e4527f-1e31-4ddf-b2ba-39b1a11da0d5");
      }
    }
    return testHeaderNameMap;
  }

  protected void initializeDictionaryIds() {
    if (!dictionaryIdsInitialized) {
      List<Test> tests = getTestService().getActiveTestByName("DNA PCR");
      if (tests != null && !tests.isEmpty()) {
        Test test = tests.get(0);
        List<TestResult> testResults =
            getTestResultService().getActiveTestResultsByTest(test.getId());
        DictionaryService ds = getDictionaryService();

        for (TestResult testResult : testResults) {
          Dictionary dictionary = ds.getDataForId(testResult.getValue());
          if (dictionary != null) {
            String dictEntry = dictionary.getDictEntry();
            if ("Positive".equals(dictEntry)) {
              positiveId = dictionary.getId();
            } else if ("Negative".equals(dictEntry)) {
              negativeId = dictionary.getId();
            } else if ("Valid".equals(dictEntry)) {
              validId = dictionary.getId();
            } else if ("Invalid".equals(dictEntry)) {
              invalidId = dictionary.getId();
            }
          }
        }
      }
      dictionaryIdsInitialized = true;
    }
  }

  protected String getNegativeId() {
    initializeDictionaryIds();
    return negativeId;
  }

  protected String getPositiveId() {
    initializeDictionaryIds();
    return positiveId;
  }

  protected String getValidId() {
    initializeDictionaryIds();
    return validId;
  }

  protected String getInvalidId() {
    initializeDictionaryIds();
    return invalidId;
  }

  @Override
  public boolean insert(List<String> lines, String currentUserId) {
    List<AnalyzerResults> results = new ArrayList<>();
    int accessionNumberIndex = -1;
    int dayIndex = -1;
    int sampleTypeIndex = -1;

    String[] headers = lines.get(0).split(DELIMITER);

    Map<String, Test> testMap = getTestHeaderNameMap();
    for (int i = 0; i < headers.length; i++) {
      if (testMap.containsKey(headers[i].replace("\"", "").trim())) {
        indexTestMap.put(String.valueOf(i), headers[i].replace("\"", "").trim());
      } else if ("Order Number".equals(headers[i].replace("\"", "").trim())) {
        accessionNumberIndex = i;
      } else if ("Detection Start Date/Time".equals(headers[i].replace("\"", "").trim())) {
        dayIndex = i;
      } else if ("Sample Type".equals(headers[i].replace("\"", "").trim())) {
        sampleTypeIndex = i;
      }
    }

    for (int j = 1; j < lines.size(); j++) {
      String line = lines.get(j);
      String[] data = line.split(DELIMITER);
      if (line.length() == 0 || data.length == 0) {
        continue;
      }

      for (int k = 0; k < data.length; k++) {

        if (indexTestMap.containsKey(String.valueOf(k))) {
          String testKey = indexTestMap.get(String.valueOf(k));
          Test test = testMap.get(testKey);
          if (test == null) {
            continue;
          }
          AnalyzerResults aResult = new AnalyzerResults();
          aResult.setTestId(test.getId());
          aResult.setTestName(test.getName());
          aResult.setResult(getAppropriateResults(data[k]));
          aResult.setAccessionNumber(data[accessionNumberIndex].replace("\"", "").trim());
          aResult.setCompleteDate(getTimestampFromDate(data[dayIndex].replace("\"", "").trim()));
          aResult.setIsControl(!data[sampleTypeIndex].replace("\"", "").trim().equals("S"));
          aResult.setResultType("D");
          results.add(aResult);
          if (data[k].length() == 0) {
            break;
          }
        }
      }
    }
    return persistImport(currentUserId, results);
  }

  private Timestamp getTimestampFromDate(String dateTime) {
    return DateUtil.convertStringDateToTimestampWithPattern(dateTime, DATE_PATTERN);
  }

  @Override
  public String getError() {
    return "Cobas TaqMan48 DBS analyzer unable to write to database";
  }

  private String getAppropriateResults(String result) {
    result = result.replace("\"", "").trim();
    if (result.toLowerCase().contains("not detected dbs")
        || result.toLowerCase().contains("target not detected")) {
      result = getNegativeId();
    } else if (result.toLowerCase().contains("detected dbs")) {
      result = getPositiveId();
    }

    return result;
  }
}
