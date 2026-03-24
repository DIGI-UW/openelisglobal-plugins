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
package org.openelisglobal.plugins.analyzer.genericastm;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.openelisglobal.analysis.service.AnalysisService;
import org.openelisglobal.analysis.valueholder.Analysis;
import org.openelisglobal.analyzerimport.analyzerreaders.AnalyzerResponder;
import org.openelisglobal.analyzerimport.service.AnalyzerTestMappingService;
import org.openelisglobal.analyzerimport.valueholder.AnalyzerTestMapping;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.patient.valueholder.Patient;
import org.openelisglobal.person.valueholder.Person;
import org.openelisglobal.sample.service.SampleService;
import org.openelisglobal.sample.valueholder.Sample;
import org.openelisglobal.samplehuman.service.SampleHumanService;
import org.openelisglobal.spring.util.SpringContext;
import org.openelisglobal.test.valueholder.Test;

/** Builds ASTM query responses for GenericASTM analyzers. */
public class GenericASTMResponder implements AnalyzerResponder {

  private static final String COMPONENT_DELIMITER = "^";
  private static final String QUERY_SEGMENT_PREFIX = "Q|";

  private static final String HEADER_TEMPLATE = "H|\\^&|||OpenELIS^OrderResponse^1.0|||||||LIS2-A2\r\n";
  private static final String TERMINATOR_SEGMENT = "L|1|N\r\n";
  private static final String NO_ORDER_SEGMENT_SUFFIX = "||||||||||||||||||||||||Y\r\n";

  private static final class ResolvedAccession {
    private final String accessionNumber;
    private final Sample sample;

    private ResolvedAccession(String accessionNumber, Sample sample) {
      this.accessionNumber = accessionNumber;
      this.sample = sample;
    }
  }

  private static final String RESPONSE_TIMEZONE_PROPERTY = "org.openelisglobal.plugins.genericastm.response-timezone";
  private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");
  private static final DateTimeFormatter DATE_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

  private final String analyzerTypeId;
  private final String analyzerName;
  private final SampleService sampleService;
  private final SampleHumanService sampleHumanService;
  private final AnalysisService analysisService;
  private final AnalyzerTestMappingService analyzerTestMappingService;
  private final ZoneId responseZoneId;

  public GenericASTMResponder(String analyzerTypeId, String analyzerName) {
    this(
        analyzerTypeId,
        analyzerName,
        SpringContext.getBean(SampleService.class),
        SpringContext.getBean(SampleHumanService.class),
        SpringContext.getBean(AnalysisService.class),
        SpringContext.getBean(AnalyzerTestMappingService.class));
  }

  GenericASTMResponder(
      String analyzerTypeId,
      String analyzerName,
      SampleService sampleService,
      SampleHumanService sampleHumanService,
      AnalysisService analysisService,
      AnalyzerTestMappingService analyzerTestMappingService) {
    this.analyzerTypeId = analyzerTypeId;
    this.analyzerName = analyzerName;
    this.sampleService = sampleService;
    this.sampleHumanService = sampleHumanService;
    this.analysisService = analysisService;
    this.analyzerTestMappingService = analyzerTestMappingService;
    this.responseZoneId = resolveResponseZoneId();
  }

  private static ZoneId resolveResponseZoneId() {
    String tz = System.getProperty(RESPONSE_TIMEZONE_PROPERTY);
    if (tz != null && !tz.trim().isEmpty()) {
      try {
        return ZoneId.of(tz.trim());
      } catch (Exception e) {
        LogEvent.logWarn(
            GenericASTMResponder.class.getSimpleName(),
            "resolveResponseZoneId",
            "Invalid " + RESPONSE_TIMEZONE_PROPERTY + "='" + tz + "', using UTC");
      }
    }
    return ZoneId.of("UTC");
  }

  @Override
  public String buildResponse(List<String> lines) {
    if (lines == null || lines.isEmpty()) {
      return "";
    }

    String queryRecord = findQueryRecord(lines);
    if (queryRecord == null) {
      LogEvent.logDebug(
          this.getClass().getSimpleName(),
          "buildResponse",
          "No Q-record present; ignoring non-query message for " + analyzerName);
      return "";
    }

    ResolvedAccession resolvedAccession = resolveRequestedAccession(queryRecord);
    if (resolvedAccession == null || isBlank(resolvedAccession.accessionNumber)) {
      LogEvent.logWarn(
          this.getClass().getSimpleName(),
          "buildResponse",
          "Q-record present but accession identifier could not be resolved");
      return "";
    }

    String requestedAccession = resolvedAccession.accessionNumber;
    Sample sample =
        resolvedAccession.sample != null
            ? resolvedAccession.sample
            : sampleService.getSampleByAccessionNumber(requestedAccession);
    if (sample == null) {
      LogEvent.logInfo(
          this.getClass().getSimpleName(),
          "buildResponse",
          "No sample found for accession " + requestedAccession + "; returning no-order marker");
      return buildNoOrderResponse(requestedAccession);
    }

    List<String> analyzerTestCodes = getMappedAnalyzerTestCodes(sample);
    if (analyzerTestCodes.isEmpty()) {
      LogEvent.logInfo(
          this.getClass().getSimpleName(),
          "buildResponse",
          "Sample "
              + requestedAccession
              + " has no mapped tests for analyzer type "
              + analyzerTypeId
              + "; returning no-order marker");
      return buildNoOrderResponse(requestedAccession);
    }

    Patient patient = sampleHumanService.getPatientForSample(sample);
    return buildOrderResponse(requestedAccession, sample, patient, analyzerTestCodes);
  }

  private String findQueryRecord(List<String> lines) {
    for (String line : lines) {
      if (line != null && line.startsWith(QUERY_SEGMENT_PREFIX)) {
        return line;
      }
    }
    return null;
  }

  private ResolvedAccession resolveRequestedAccession(String queryRecord) {
    String[] queryFields = queryRecord.split("\\|", -1);
    if (queryFields.length <= 2 || isBlank(queryFields[2])) {
      return null;
    }

    String queryRangeField = queryFields[2].trim();
    if (!queryRangeField.contains(COMPONENT_DELIMITER)) {
      return new ResolvedAccession(queryRangeField, null);
    }

    String[] components = queryRangeField.split("\\^", -1);
    Set<String> candidateSet = new LinkedHashSet<>();
    for (String component : components) {
      if (!isBlank(component)) {
        candidateSet.add(component.trim());
      }
    }
    List<String> candidates = new ArrayList<>(candidateSet);

    if (candidates.isEmpty()) {
      return null;
    }

    // Devices differ in which Q.3 component carries accession. Prefer the first candidate that
    // exists as a real sample; otherwise fall back to the first non-empty component.
    for (String candidate : candidates) {
      Sample sample = sampleService.getSampleByAccessionNumber(candidate);
      if (sample != null) {
        return new ResolvedAccession(candidate, sample);
      }
    }

    return new ResolvedAccession(candidates.get(0), null);
  }

  private List<String> getMappedAnalyzerTestCodes(Sample sample) {
    List<Analysis> analyses = analysisService.getAnalysesBySampleId(sample.getId());
    if (analyses == null || analyses.isEmpty()) {
      return new ArrayList<>();
    }

    Map<String, List<String>> testIdToAnalyzerCodes = buildTestIdToAnalyzerCodesMap();
    Set<String> orderedCodes = new LinkedHashSet<>();

    for (Analysis analysis : analyses) {
      Test test = analysis.getTest();
      if (test == null || isBlank(test.getId())) {
        continue;
      }

      List<String> analyzerCodes = testIdToAnalyzerCodes.get(test.getId());
      if (analyzerCodes != null) {
        orderedCodes.addAll(analyzerCodes);
      }
    }

    return new ArrayList<>(orderedCodes);
  }

  private Map<String, List<String>> buildTestIdToAnalyzerCodesMap() {
    List<AnalyzerTestMapping> mappings = analyzerTestMappingService.getAll();
    Map<String, List<String>> testIdToCodes = new LinkedHashMap<>();

    for (AnalyzerTestMapping mapping : mappings) {
      if (!analyzerTypeId.equals(mapping.getAnalyzerTypeId())
          || isBlank(mapping.getTestId())
          || isBlank(mapping.getAnalyzerTestName())) {
        continue;
      }

      testIdToCodes.computeIfAbsent(mapping.getTestId(), key -> new ArrayList<>());
      List<String> analyzerCodes = testIdToCodes.get(mapping.getTestId());
      if (!analyzerCodes.contains(mapping.getAnalyzerTestName())) {
        analyzerCodes.add(mapping.getAnalyzerTestName());
      }
    }

    return testIdToCodes;
  }

  private String buildOrderResponse(
      String accessionNumber, Sample sample, Patient patient, List<String> analyzerTestCodes) {
    String patientId = "";
    String patientName = "";
    String birthDate = "";
    String gender = "";

    if (patient != null) {
      patientId = sanitizeAstmField(safe(patient.getNationalId()));
      gender = sanitizeAstmField(safe(patient.getGender()));
      if (patient.getBirthDate() != null) {
        birthDate =
            DATE_FORMAT.format(
                Instant.ofEpochMilli(patient.getBirthDate().getTime())
                    .atZone(responseZoneId)
                    .toLocalDate());
      }

      Person person = patient.getPerson();
      if (person != null) {
        patientName =
            sanitizeAstmField(safe(person.getLastName())) + "^"
                + sanitizeAstmField(safe(person.getFirstName()));
      }
    }

    ZonedDateTime orderTime =
        sample.getEnteredDate() != null
            ? Instant.ofEpochMilli(sample.getEnteredDate().getTime()).atZone(responseZoneId)
            : ZonedDateTime.now(responseZoneId);
    String orderTimestamp = DATE_TIME_FORMAT.format(orderTime);
    String orderTestField =
        analyzerTestCodes.stream()
            .map(code -> "^^^" + sanitizeAstmField(code))
            .collect(Collectors.joining("\\"));

    StringBuilder response = new StringBuilder();
    response.append(HEADER_TEMPLATE);
    response
        .append("P|1|")
        .append(patientId)
        .append("||")
        .append(patientName)
        .append("||")
        .append(birthDate)
        .append("|")
        .append(gender)
        .append("\r\n");

    response
        .append("O|1|")
        .append(sanitizeAstmField(accessionNumber))
        .append("||")
        .append(orderTestField)
        .append("|R|")
        .append(orderTimestamp)
        .append("||||A\r\n");
    response.append(TERMINATOR_SEGMENT);
    return response.toString();
  }

  private String buildNoOrderResponse(String accessionNumber) {
    StringBuilder response = new StringBuilder();
    response.append(HEADER_TEMPLATE);
    response.append("P|1|\r\n");
    response.append("O|1|").append(sanitizeAstmField(accessionNumber)).append(NO_ORDER_SEGMENT_SUFFIX);
    response.append(TERMINATOR_SEGMENT);
    return response.toString();
  }

  private String safe(String value) {
    return value == null ? "" : value.trim();
  }

  private boolean isBlank(String value) {
    return value == null || value.trim().isEmpty();
  }

  /**
   * Sanitizes a value for use in ASTM outbound fields. ASTM uses {@code |}, {@code ^}, {@code \},
   * {@code &} as delimiters; values containing these can break message structure. Replaces
   * delimiter chars with space to produce valid output.
   */
  private String sanitizeAstmField(String value) {
    if (value == null || value.isEmpty()) {
      return value == null ? "" : value;
    }
    return value.replace('|', ' ')
        .replace('^', ' ')
        .replace('\\', ' ')
        .replace('&', ' ')
        .trim();
  }
}
