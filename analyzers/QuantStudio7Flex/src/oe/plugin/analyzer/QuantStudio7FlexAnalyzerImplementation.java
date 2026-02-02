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
 * Copyright (C) ITECH, University of Washington, Seattle WA. All Rights Reserved.
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

/**
 * QuantStudio7FlexAnalyzerImplementation - Line inserter for QuantStudio 7 Flex.
 *
 * <p>Extends the QuantStudio3 parsing logic to support additional columns present in QS7 Flex
 * exports.
 *
 * <p>Expected CSV columns (tab-delimited): Well, Well Position, Sample Name, Target, Task,
 * Reporter, Quencher, Amp Status, CT, Ct Mean, Ct SD, ...
 *
 * <p>Key differences from QS3: - "Well Position" column (e.g., "A1", "A2") - "Target" column for
 * test identification (e.g., "SARS-CoV-2", "IC") - "Task" column (UNKNOWN, Control) - "Reporter"
 * and "Quencher" columns for probe info - "Amp Status" column (Positive, Negative)
 *
 * <p>M8 Milestone: Madagascar Analyzer Integration (Feature 011)
 */
public class QuantStudio7FlexAnalyzerImplementation extends AnalyzerLineInserter {

  private static final String[] CONTROL_ACCESSION_PREFIX = {"CNEG", "CPOS", "NTC", "PTC"};

  // QS7 Flex headers (superset of QS3 headers)
  public static final String[] HEADERS_USED = {
    "Sample Name", "Target", "CT", "Ct Mean", "Ct SD", "Well Position", "Amp Status"
  };

  private static final String CSV_DELIMITER = "\t";

  // Test LOINC codes - same as QS3 for COVID testing
  public static final String SARSCOV2_LOINC = "94500-6";
  public static final String IC_LOINC = "94745-7";

  private List<String> columnHeaders;
  private HashMap<String, List<Test>> testLoincMap = new HashMap<>();

  private TestService testService = SpringContext.getBean(TestService.class);
  private SampleService sampleService = SpringContext.getBean(SampleService.class);
  private AnalyzerService analyzerService = SpringContext.getBean(AnalyzerService.class);
  private AnalysisService analysisService = SpringContext.getBean(AnalysisService.class);
  private NoteService noteService = SpringContext.getBean(NoteService.class);

  private final String ANALYZER_NOTE = "Analyzer Note";

  private String ANALYZER_ID;

  public QuantStudio7FlexAnalyzerImplementation() {
    // Map SARS-CoV-2 tests
    List<Test> sarscov2Tests = testService.getTestsByLoincCode(SARSCOV2_LOINC);
    if (sarscov2Tests != null) {
      testLoincMap.put(SARSCOV2_LOINC, sarscov2Tests);
    }

    // Map Internal Control tests
    List<Test> icTests = testService.getTestsByLoincCode(IC_LOINC);
    if (icTests != null) {
      testLoincMap.put(IC_LOINC, icTests);
    }

    Analyzer analyzer = analyzerService.getAnalyzerByName("QuantStudio7FlexAnalyzer");
    if (analyzer != null) {
      ANALYZER_ID = analyzer.getId();
    }
  }

  public void addResultLine(
      String resultLine,
      List<AnalyzerResults> results,
      List<AnalyzerResults> unknownTestResults,
      String currentUserId) {

    String[] resultData = resultLine.split(CSV_DELIMITER, -1);

    String currentAccessionNumber =
        getColumnValue(resultData, "Sample Name").replace("\"", "").trim();
    String target = getColumnValue(resultData, "Target").replace("\"", "").trim();
    String ctValue = getColumnValue(resultData, "CT").replace("\"", "").trim();
    String ampStatus = getColumnValue(resultData, "Amp Status").replace("\"", "").trim();

    // Skip rows without sample name
    if (GenericValidator.isBlankOrNull(currentAccessionNumber)) {
      return;
    }

    Sample sample = sampleService.getSampleByAccessionNumber(currentAccessionNumber);
    Analysis analysis = null;
    Test test = null;

    // Determine which LOINC code to use based on target
    String targetLoinc = determineTargetLoinc(target);

    if (sample != null) {
      // Fetch analyses once to avoid duplicate DB calls
      List<Analysis> analyses = analysisService.getAnalysesBySampleId(sample.getId());

      // First pass: try to match by target-specific LOINC
      for (Analysis curAnalysis : analyses) {
        if (testLoincMap.containsKey(curAnalysis.getTest().getLoinc())) {
          if (targetLoinc != null && targetLoinc.equals(curAnalysis.getTest().getLoinc())) {
            test = curAnalysis.getTest();
            analysis = curAnalysis;
            break;
          }
        }
      }
      // Fallback: use any matching analysis if target-specific match not found
      if (test == null) {
        for (Analysis curAnalysis : analyses) {
          if (testLoincMap.containsKey(curAnalysis.getTest().getLoinc())) {
            test = curAnalysis.getTest();
            analysis = curAnalysis;
            break;
          }
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

    LogEvent.logDebug(
        this.getClass().getName(),
        "addResultLine",
        "QS7Flex: "
            + analyzerResult.getAccessionNumber()
            + " Target="
            + target
            + " CT="
            + ctValue
            + " AmpStatus="
            + ampStatus);

    results.add(analyzerResult);
  }

  /** Determine the LOINC code based on the Target column value. */
  private String determineTargetLoinc(String target) {
    if (target == null) {
      return SARSCOV2_LOINC; // Default
    }
    String upperTarget = target.toUpperCase();
    if (upperTarget.contains("IC")
        || upperTarget.contains("INTERNAL")
        || upperTarget.contains("CONTROL")) {
      // Check if it's actually a positive/negative control sample
      if (!upperTarget.contains("POS") && !upperTarget.contains("NEG")) {
        return IC_LOINC;
      }
    }
    return SARSCOV2_LOINC; // Default to SARS-CoV-2
  }

  private List<Note> createNotesForAnalysis(
      Analysis analysis, String[] resultData, String currentUserId) {
    List<Note> notes = new ArrayList<>();

    // Add CT value as note
    String ctValue = getColumnValue(resultData, "CT");
    if (!GenericValidator.isBlankOrNull(ctValue)) {
      notes.add(createNoteForValue(analysis, "CT", ctValue, currentUserId));
    }

    // Add Ct Mean as note
    String ctMean = getColumnValue(resultData, "Ct Mean");
    if (!GenericValidator.isBlankOrNull(ctMean)) {
      notes.add(createNoteForValue(analysis, "Ct Mean", ctMean, currentUserId));
    }

    // Add Ct SD as note if present
    String ctSd = getColumnValue(resultData, "Ct SD");
    if (!GenericValidator.isBlankOrNull(ctSd) && !"0.00".equals(ctSd) && !"0".equals(ctSd)) {
      notes.add(createNoteForValue(analysis, "Ct SD", ctSd, currentUserId));
    }

    // Add Target as note (QS7 Flex specific)
    String target = getColumnValue(resultData, "Target");
    if (!GenericValidator.isBlankOrNull(target)) {
      notes.add(createNoteForValue(analysis, "Target", target, currentUserId));
    }

    // Add Amp Status as note (QS7 Flex specific)
    String ampStatus = getColumnValue(resultData, "Amp Status");
    if (!GenericValidator.isBlankOrNull(ampStatus)) {
      notes.add(createNoteForValue(analysis, "Amp Status", ampStatus, currentUserId));
    }

    // Add Well Position as note (QS7 Flex specific)
    String wellPosition = getColumnValue(resultData, "Well Position");
    if (!GenericValidator.isBlankOrNull(wellPosition)) {
      notes.add(createNoteForValue(analysis, "Well Position", wellPosition, currentUserId));
    }

    return notes;
  }

  private Note createNoteForValue(
      Analysis analysis, String columnName, String value, String currentUserId) {
    return noteService.createSavableNote(
        analysis, NoteType.INTERNAL, columnName + " - " + value, ANALYZER_NOTE, currentUserId);
  }

  public boolean isColumnHeaderRow(String line) {
    // QS7 Flex specific: check for "Well Position" or both "Well" and "Target"
    return (line.contains("Well Position" + CSV_DELIMITER))
        || (line.contains("Well" + CSV_DELIMITER) && line.contains("Target"));
  }

  public void setColumnHeaders(String columnHeaderLine) {
    columnHeaders = Arrays.asList(columnHeaderLine.split(CSV_DELIMITER, -1));
  }

  private int getIndexOfColumn(String columnHeader) {
    if (columnHeaders == null) {
      return -1;
    }
    for (int i = 0; i < columnHeaders.size(); ++i) {
      if (columnHeaders.get(i).equals(columnHeader)) {
        return i;
      }
    }
    return -1;
  }

  private String getColumnValue(String[] resultData, String columnHeader) {
    int index = getIndexOfColumn(columnHeader);
    if (index >= 0 && index < resultData.length) {
      return resultData[index];
    }
    return "";
  }

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

    // Resolve unknown test results using known ones (for control samples)
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
      // If test is still null, use first test with appropriate LOINC
      if (test == null) {
        if (testLoincMap.containsKey(SARSCOV2_LOINC)) {
          test = testLoincMap.get(SARSCOV2_LOINC).get(0);
        }
      }
      if (test != null) {
        unknownTestResult.setTestId(test.getId());
        unknownTestResult.setTestName(test.getName());
        if (test.getDefaultTestResult() != null) {
          unknownTestResult.setResult(test.getDefaultTestResult().getValue());
        }
      }
    }

    return persistImport(currentUserId, results);
  }

  private boolean isControl(String accessionPrefix) {
    if (accessionPrefix == null) {
      return false;
    }
    String upper = accessionPrefix.toUpperCase();
    for (String prefix : CONTROL_ACCESSION_PREFIX) {
      if (upper.startsWith(prefix) || upper.equals(prefix)) {
        return true;
      }
    }
    return false;
  }

  @Override
  public String getError() {
    return "QuantStudio 7 Flex analyzer unable to write to database";
  }
}
