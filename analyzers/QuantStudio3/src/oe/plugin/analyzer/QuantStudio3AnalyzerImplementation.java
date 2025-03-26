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
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import org.apache.commons.validator.GenericValidator;
import org.openelisglobal.analysis.service.AnalysisService;
import org.openelisglobal.analysis.valueholder.Analysis;
import org.openelisglobal.analyzer.service.AnalyzerService;
import org.openelisglobal.analyzer.valueholder.Analyzer;
import org.openelisglobal.analyzerimport.analyzerreaders.AnalyzerLineInserter;
import org.openelisglobal.analyzerresults.valueholder.AnalyzerResults;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.note.service.NoteService;
import org.openelisglobal.note.service.NoteServiceImpl.NoteType;
import org.openelisglobal.note.valueholder.Note;
import org.openelisglobal.sample.service.SampleService;
import org.openelisglobal.sample.valueholder.Sample;
import org.openelisglobal.spring.util.SpringContext;
import org.openelisglobal.test.service.TestService;
import org.openelisglobal.test.valueholder.Test;

public class QuantStudio3AnalyzerImplementation extends AnalyzerLineInserter {

  //	private static String DATE_PATTERN = "yyyyMMdd";

  private static final String[] CONTROL_ACCESSION_PREFIX = {"CNEG", "CPOS"};
  public static final String[] HEADERS_USED = {"Sample Name", "CT", "Ct Mean", "Ct SD"};
  private static final String CSV_DELIMETER = "\t";
  public static final String TEST_LOINC = "94500-6";

  private List<String> columnHeaders;
  private HashMap<String, List<Test>> testLoincMap = new HashMap<>();

  private TestService testService = SpringContext.getBean(TestService.class);
  private SampleService sampleService = SpringContext.getBean(SampleService.class);
  private AnalyzerService analyzerService = SpringContext.getBean(AnalyzerService.class);
  private AnalysisService analysisService = SpringContext.getBean(AnalysisService.class);
  private NoteService noteService = SpringContext.getBean(NoteService.class);

  private final String ANALYZER_NOTE = "Analyzer Note";

  private String ANALYZER_ID;

  public QuantStudio3AnalyzerImplementation() {
    List<Test> tests = testService.getTestsByLoincCode(TEST_LOINC);

    if (tests != null) {
      testLoincMap.put(TEST_LOINC, tests);
    }
    Analyzer analyzer = analyzerService.getAnalyzerByName("QuantStudio3Analyzer");
    ANALYZER_ID = analyzer.getId();
  }

  public void addResultLine(
      String resultLine,
      List<AnalyzerResults> results,
      List<AnalyzerResults> unknownTestResults,
      String currentUserId) {
    String[] resultData = resultLine.split(CSV_DELIMETER, -1);
    //		String currentAccessionNumber = resultData[getIndexOfColumn("Sample Name")].replace("\"",
    // "").trim() + "-"
    //				+ resultData[getIndexOfColumn("Target")].replace("\"", "").trim();
    //		String date = resultData[4].replace("\"", "");
    String currentAccessionNumber =
        resultData[getIndexOfColumn("Sample Name")].replace("\"", "").trim();
    Sample sample = sampleService.getSampleByAccessionNumber(currentAccessionNumber);
    Analysis analysis = null;
    Test test = null;
    if (sample != null) {
      for (Analysis curAnalysis : analysisService.getAnalysesBySampleId(sample.getId())) {
        if (testLoincMap.containsKey(curAnalysis.getTest().getLoinc())) {
          test = curAnalysis.getTest();
          analysis = curAnalysis;
          break;
        }
      }
    }
    AnalyzerResults analyzerResult = new AnalyzerResults();

    analyzerResult.setAccessionNumber(currentAccessionNumber);
    analyzerResult.setIsControl(isControl(currentAccessionNumber));
    analyzerResult.setCompleteDate(Timestamp.from(Instant.now()));
    analyzerResult.setAnalyzerId(ANALYZER_ID);
    analyzerResult.setResultType("D"); // dictionary result
    if (test != null) {
      if (test.getDefaultTestResult() != null) {
        analyzerResult.setResult(test.getDefaultTestResult().getValue());
      }
      analyzerResult.setTestId(test.getId());
      analyzerResult.setTestName(test.getName());
    } else {
      unknownTestResults.add(analyzerResult);
    }
    if (analysis != null) {
      noteService.insertAll(createNotesForAnalysis(analysis, resultData, currentUserId));
    }

    //		analyzerResult.setUnits(resultData[getIndexOfColumn("Cq")].replace("\"", ""));
    //	 	aResult.setReadOnly(CheckReadOnly (testKey));

    LogEvent.logDebug(
        this.getClass().getName(),
        "addResultLine",
        "***"
            + analyzerResult.getAccessionNumber()
            + " "
            + analyzerResult.getCompleteDate()
            + " "
            + analyzerResult.getResult());

    results.add(analyzerResult);
  }

  private List<Note> createNotesForAnalysis(
      Analysis analysis, String[] resultData, String currentUserId) {
    List<Note> notes = new ArrayList<>();
    notes.add(createNoteForColumn(analysis, resultData, "CT", currentUserId));
    notes.add(createNoteForColumn(analysis, resultData, "Ct Mean", currentUserId));
    if (!GenericValidator.isBlankOrNull(resultData[getIndexOfColumn("Ct SD")])) {
      notes.add(createNoteForColumn(analysis, resultData, "Ct SD", currentUserId));
    }
    return notes;
  }

  private Note createNoteForColumn(
      Analysis analysis, String[] resultData, String columnName, String currentUserId) {
    return noteService.createSavableNote(
        analysis,
        NoteType.INTERNAL,
        columnName + " - " + resultData[getIndexOfColumn(columnName)],
        ANALYZER_NOTE,
        currentUserId);
  }

  public boolean isColumnHeaderRow(String line) {
    return line.contains("Well" + CSV_DELIMETER);
  }

  public void setColumnHeaders(String columnHeaderLine) {
    columnHeaders = Arrays.asList(columnHeaderLine.split(CSV_DELIMETER, -1));
  }

  private int getIndexOfColumn(String columnHeader) {
    for (int i = 0; i < columnHeaders.size(); ++i) {
      if (columnHeaders.get(i).equals(columnHeader)) {
        return i;
      }
    }

    return -1;
  }

  /*
   * (non-Javadoc)
   *
   * @see
   * org.openelisglobal.analyzerimport.analyzerreaders.AnalyzerLineInserter#insert
   * (java.util.List, java.lang.String)
   */
  @Override
  public boolean insert(List<String> lines, String currentUserId) {
    List<AnalyzerResults> results = new ArrayList<>();
    List<AnalyzerResults> unknownTestResults = new ArrayList<>();

    int columnHeaderRowIndex = lines.size();

    for (int i = 0; i < lines.size(); ++i) {
      String line = lines.get(i);
      if (!GenericValidator.isBlankOrNull(line.trim())) {
        if (i > columnHeaderRowIndex) {
          addResultLine(line, results, unknownTestResults, currentUserId);
        } else if (isColumnHeaderRow(line)) {
          setColumnHeaders(line);
          columnHeaderRowIndex = i;
        }
      }
    }
    // this is done as we couldn't set the proper test for the control tests as
    // they don't have real sample numbers to get the test from so we are getting it
    // from real ones here
    Test test = null;
    for (AnalyzerResults unknownTestResult : unknownTestResults) {
      if (test == null) {
        for (AnalyzerResults result : results) {
          if (!GenericValidator.isBlankOrNull(result.getTestId())) {
            test = testService.get(result.getTestId());
            break;
          }
        }
      }
      // if test is still null assume that we are using the first test with the
      // appropriate loinc
      if (test == null) {
        if (testLoincMap.containsKey(TEST_LOINC)) {
          test = testLoincMap.get(TEST_LOINC).get(0);
        }
      }
      unknownTestResult.setTestId(test.getId());
      unknownTestResult.setTestName(test.getName());
      if (test.getDefaultTestResult() != null) {
        unknownTestResult.setResult(test.getDefaultTestResult().getValue());
      }
    }
    return persistImport(currentUserId, results);
  }

  private boolean isControl(String accessionPrefix) {
    return Arrays.asList(CONTROL_ACCESSION_PREFIX).contains(accessionPrefix);
  }

  @Override
  public String getError() {
    return "QuantStudio3 analyzer unable to write to database";
  }
}
