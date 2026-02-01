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
package uw.edu.itech.AbbottArchitect;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.openelisglobal.analyzer.service.HL7MessageService;
import org.openelisglobal.analyzer.service.HL7MessageServiceImpl;
import org.openelisglobal.analyzerimport.analyzerreaders.AnalyzerLineInserter;
import org.openelisglobal.analyzerimport.analyzerreaders.AnalyzerReaderUtil;
import org.openelisglobal.analyzerimport.util.AnalyzerTestNameCache;
import org.openelisglobal.analyzerimport.util.MappedTestName;
import org.openelisglobal.analyzerresults.valueholder.AnalyzerResults;
import org.openelisglobal.common.log.LogEvent;

/**
 * HL7 ORU^R01 result parser for Abbott Architect analyzers.
 *
 * <p>Extracts OBX results via {@link HL7MessageService} and maps analyzer test codes to OpenELIS
 * tests using {@link AnalyzerTestNameCache}. Results are persisted using the standard {@link
 * AnalyzerLineInserter} workflow.
 */
public class AbbottArchitectAnalyzerLineInserter extends AnalyzerLineInserter {

  private static final String ANALYZER_NAME = AbbottArchitectAnalyzer.ANALYZER_NAME;
  private static final String DEFAULT_ACCESSION = "HL7-UNKNOWN";

  private final HL7MessageService hl7MessageService;
  private AnalyzerReaderUtil readerUtil;
  private String error;

  public AbbottArchitectAnalyzerLineInserter() {
    this(new HL7MessageServiceImpl());
  }

  AbbottArchitectAnalyzerLineInserter(HL7MessageService hl7MessageService) {
    this.hl7MessageService = hl7MessageService;
  }

  @Override
  public boolean insert(List<String> lines, String currentUserId) {
    error = null;
    if (lines == null || lines.isEmpty()) {
      error = "HL7 message lines are empty";
      return false;
    }

    try {
      String raw = String.join("\r", lines);
      HL7MessageService.OruR01ParseResult parsed = hl7MessageService.parseOruR01(raw);
      List<ParsedResult> parsedResults = parseResults(parsed);
      if (parsedResults.isEmpty()) {
        error = "HL7 ORU^R01 has no OBX results";
        return false;
      }

      String accession = resolveAccession(parsed);
      List<AnalyzerResults> results = new ArrayList<>();

      for (ParsedResult parsedResult : parsedResults) {
        MappedTestName mappedTest =
            AnalyzerTestNameCache.getInstance()
                .getMappedTest(ANALYZER_NAME, parsedResult.getTestCode());

        if (mappedTest == null) {
          mappedTest =
              AnalyzerTestNameCache.getInstance()
                  .getEmptyMappedTestName(ANALYZER_NAME, parsedResult.getTestCode());
        }

        AnalyzerResults analyzerResult = new AnalyzerResults();
        analyzerResult.setAnalyzerId(mappedTest.getAnalyzerId());
        analyzerResult.setTestId(mappedTest.getTestId());
        analyzerResult.setTestName(mappedTest.getOpenElisTestName());
        analyzerResult.setResult(parsedResult.getValue());
        analyzerResult.setUnits(parsedResult.getUnits());
        analyzerResult.setAccessionNumber(accession);
        analyzerResult.setIsControl(false);
        analyzerResult.setCompleteDate(new Timestamp(System.currentTimeMillis()));

        results.add(analyzerResult);

        AnalyzerResults resultFromDB = getReaderUtil().createAnalyzerResultFromDB(analyzerResult);
        if (resultFromDB != null) {
          results.add(resultFromDB);
        }
      }

      return persistImport(currentUserId, results);
    } catch (HL7MessageService.HL7ParseException e) {
      error = "HL7 parse error: " + e.getMessage();
      LogEvent.logError(e);
      return false;
    } catch (Exception e) {
      error = "HL7 insert error: " + e.getMessage();
      LogEvent.logError(e);
      return false;
    }
  }

  @Override
  public String getError() {
    return error;
  }

  List<ParsedResult> parseResults(String rawMessage) throws HL7MessageService.HL7ParseException {
    HL7MessageService.OruR01ParseResult parsed = hl7MessageService.parseOruR01(rawMessage);
    return parseResults(parsed);
  }

  private List<ParsedResult> parseResults(HL7MessageService.OruR01ParseResult parsed) {
    List<ParsedResult> results = new ArrayList<>();
    if (parsed == null || parsed.getResults() == null) {
      return results;
    }

    for (HL7MessageService.ObxResult obx : parsed.getResults()) {
      if (obx == null) {
        continue;
      }
      String testCode =
          StringUtils.isNotBlank(obx.getTestCode()) ? obx.getTestCode() : obx.getTestName();
      if (StringUtils.isBlank(testCode)) {
        LogEvent.logWarn(
            getClass().getSimpleName(),
            "parseResults",
            "Skipping OBX with empty test code and name");
        continue;
      }
      results.add(
          new ParsedResult(
              testCode,
              obx.getTestName(),
              StringUtils.defaultString(obx.getValue()),
              StringUtils.defaultString(obx.getUnits())));
    }

    return results;
  }

  private String resolveAccession(HL7MessageService.OruR01ParseResult parsed) {
    if (parsed == null) {
      return DEFAULT_ACCESSION;
    }
    if (StringUtils.isNotBlank(parsed.getFillerOrderNumber())) {
      return parsed.getFillerOrderNumber();
    }
    if (StringUtils.isNotBlank(parsed.getPlacerOrderNumber())) {
      return parsed.getPlacerOrderNumber();
    }
    if (StringUtils.isNotBlank(parsed.getPatientId())) {
      return parsed.getPatientId();
    }
    return DEFAULT_ACCESSION;
  }

  static final class ParsedResult {
    private final String testCode;
    private final String testName;
    private final String value;
    private final String units;

    ParsedResult(String testCode, String testName, String value, String units) {
      this.testCode = testCode;
      this.testName = testName;
      this.value = value;
      this.units = units;
    }

    String getTestCode() {
      return testCode;
    }

    String getTestName() {
      return testName;
    }

    String getValue() {
      return value;
    }

    String getUnits() {
      return units;
    }
  }

  private AnalyzerReaderUtil getReaderUtil() {
    if (readerUtil == null) {
      readerUtil = new AnalyzerReaderUtil();
    }
    return readerUtil;
  }
}
