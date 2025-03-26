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
import org.openelisglobal.dictionary.service.DictionaryService;
import org.openelisglobal.dictionary.valueholder.Dictionary;
import org.openelisglobal.spring.util.SpringContext;
import org.openelisglobal.test.service.TestService;
import org.openelisglobal.test.valueholder.Test;
import org.openelisglobal.testresult.service.TestResultService;
import org.openelisglobal.testresult.valueholder.TestResult;

public class TaqMan48DBSAnalyzerImplementation extends AnalyzerLineInserter {

  static String ANALYZER_ID;
  private static final String DELIMITER = "\\t";
  private static final String DATE_PATTERN = "yyyy/MM/dd HH:mm:ss";
  static HashMap<String, Test> testHeaderNameMap = new HashMap<>();
  HashMap<String, String> indexTestMap = new HashMap<>();
  private static String NEGATIVE_ID;
  private static String POSITIVE_ID;
  private static String VALID_ID;
  private static String INVALID_ID;

  static {
    DictionaryService dictionaryService = SpringContext.getBean(DictionaryService.class);
    Test test = SpringContext.getBean(TestService.class).getActiveTestByName("DNA PCR").get(0);
    List<TestResult> testResults =
        SpringContext.getBean(TestResultService.class).getActiveTestResultsByTest(test.getId());

    for (TestResult testResult : testResults) {
      Dictionary dictionary = dictionaryService.getDataForId(testResult.getValue());
      if ("Positive".equals(dictionary.getDictEntry())) {
        POSITIVE_ID = dictionary.getId();
      } else if ("Negative".equals(dictionary.getDictEntry())) {
        NEGATIVE_ID = dictionary.getId();
      } else if ("Valid".equals(dictionary.getDictEntry())) {
        VALID_ID = dictionary.getId();
      } else if ("Invalid".equals(dictionary.getDictEntry())) {
        INVALID_ID = dictionary.getId();
      }
    }
  }

  static {
    testHeaderNameMap.put(
        "Result",
        SpringContext.getBean(TestService.class)
            .getTestByGUID("27e4527f-1e31-4ddf-b2ba-39b1a11da0d5"));

    System.out.println(testHeaderNameMap);

    AnalyzerService analyzerService = SpringContext.getBean(AnalyzerService.class);
    Analyzer analyzer = analyzerService.getAnalyzerByName("TaqMan48DBSAnalyzer");
    ANALYZER_ID = analyzer.getId();
  }

  /* (non-Javadoc)
   * @see org.openelisglobal.analyzerimport.analyzerreaders.AnalyzerLineInserter#insert(java.util.List, java.lang.String)
   */
  @Override
  public boolean insert(List<String> lines, String currentUserId) {
    List<AnalyzerResults> results = new ArrayList<>();
    int accessionNumberIndex = -1;
    int dayIndex = -1;
    int sampleTypeIndex = -1;

    String[] headers = lines.get(0).split(DELIMITER);

    for (Integer i = 0; i < headers.length; i++) {
      if (testHeaderNameMap.containsKey(headers[i].replace("\"", "").trim())) {
        indexTestMap.put(i.toString(), headers[i].replace("\"", "").trim());
      } else if ("Order Number".equals(headers[i].replace("\"", "").trim())) {
        accessionNumberIndex = i;
      } else if ("Detection Start Date/Time".equals(headers[i].replace("\"", "").trim())) {
        dayIndex = i;
      } else if ("Sample Type".equals(headers[i].replace("\"", "").trim())) {
        sampleTypeIndex = i;
      }
    }

    for (Integer j = 1; j < lines.size(); j++) {
      System.out.println("processing line #: " + j);
      String line = lines.get(j);
      String[] data = line.split(DELIMITER);
      if (line.length() == 0 || data.length == 0) {
        continue;
      }

      for (Integer k = 0; k < data.length; k++) {

        if (indexTestMap.containsKey(k.toString())) {
          String testKey = indexTestMap.get(k.toString());
          AnalyzerResults aResult = new AnalyzerResults();
          aResult.setTestId(testHeaderNameMap.get(testKey).getId());
          aResult.setTestName(testHeaderNameMap.get(testKey).getName());
          aResult.setResult(getAppropriateResults(data[k]));
          aResult.setAnalyzerId(ANALYZER_ID);
          aResult.setAccessionNumber(data[accessionNumberIndex].replace("\"", "").trim());
          aResult.setCompleteDate(getTimestampFromDate(data[dayIndex].replace("\"", "").trim()));
          aResult.setIsControl(!data[sampleTypeIndex].replace("\"", "").trim().equals("S"));
          aResult.setResultType("D");
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
        }
      }
    }
    return persistImport(currentUserId, results);
  }

  /*
    private void addAnalyzerResultFromLine(List<AnalyzerResults> results, String line) {
        String[] fields = line.split(DELIMITER);

        AnalyzerReaderUtil readerUtil = new AnalyzerReaderUtil();
        String analyzerAccessionNumber = fields[sample_id_index];
        boolean isControl = fields[sample_type_index].equals("Control");

        AnalyzerResults analyzerResults = new AnalyzerResults();

        analyzerResults.setAnalyzerId(ANALYZER_ID);

        String result = fields[result_index];

        String resultForDB = "";

        if (result.contains(" Copies / mL")) {
        	resultForDB = result.substring(0, result.indexOf(" Copies / mL"));
        } else {
        	resultForDB = result;
        }


        analyzerResults.setResult(("Not detected".equals(result) ? "Not detected" : "Detected " + resultForDB));
        analyzerResults.setUnits(UNITS);

        Timestamp timestamp = getTimestampFromDate(collectionDate);
        analyzerResults.setCompleteDate(timestamp);
        analyzerResults.setTestId(TEST_ID);
        analyzerResults.setAccessionNumber(analyzerAccessionNumber);
        analyzerResults.setTestName(TEST_NAME);
        analyzerResults.setIsControl(isControl);

        results.add(analyzerResults);

        AnalyzerResults resultFromDB = readerUtil.createAnalyzerResultFromDB(analyzerResults);
        if( resultFromDB != null){
            results.add(resultFromDB);
        }
    }
  */
  private Timestamp getTimestampFromDate(String dateTime) {
    return DateUtil.convertStringDateToTimestampWithPattern(dateTime, DATE_PATTERN);
  }

  @Override
  public String getError() {
    return "Cobas TaqMan48 DBS analyzer unable to write to database";
  }

  private String getAppropriateResults(String result) {
    result = result.replace("\"", "").trim();
    //	if ("Target Not Detected".equals(result)) {
    if (result.toLowerCase().contains("not detected dbs")
        || result.toLowerCase().contains("target not detected")) {
      result = NEGATIVE_ID;
    } else if (result.toLowerCase().contains("detected dbs")) {
      result = POSITIVE_ID;
      // save this until we finish w/ requirements
      //			String workingResult = result.split("\\(")[0].replace("<", "").replace("E", "");
      //			String[] splitResult = workingResult.split("\\+");
      //
      //			if (Double.parseDouble(splitResult[0]) * Math.pow(10, Double.parseDouble(splitResult[1]))
      // < THREASHOLD) {
      //				result = UNDER_THREASHOLD;
      //			}
    }

    return result;
  }
}
