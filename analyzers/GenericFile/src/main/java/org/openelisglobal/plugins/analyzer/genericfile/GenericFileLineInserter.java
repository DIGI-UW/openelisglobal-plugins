package org.openelisglobal.plugins.analyzer.genericfile;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.openelisglobal.analyzer.service.AnalyzerPluginConfigService;
import org.openelisglobal.analyzerimport.analyzerreaders.AnalyzerLineInserter;
import org.openelisglobal.analyzerresults.valueholder.AnalyzerResults;
import org.openelisglobal.spring.util.SpringContext;

public class GenericFileLineInserter extends AnalyzerLineInserter {

  private static final List<String> DEFAULT_LINE_FIELD_ORDER =
      List.of("sampleId", "testCode", "result", "interpretation", "position", "testDate", "testTime", "units");

  private final String configuredAnalyzerId;
  private final Map<String, Object> explicitProfileConfig;
  private String errorMessage;

  public GenericFileLineInserter() {
    this(null, null);
  }

  public GenericFileLineInserter(String configuredAnalyzerId, Map<String, Object> explicitProfileConfig) {
    this.configuredAnalyzerId = configuredAnalyzerId;
    this.explicitProfileConfig = explicitProfileConfig;
  }

  @Override
  public boolean insert(List<String> lines, String currentUserId) {
    errorMessage = null;
    if (lines == null || lines.isEmpty()) {
      return true;
    }

    Map<String, Object> profileConfig = resolveProfileConfig();
    boolean hasHeader = resolveHasHeader(profileConfig);
    List<String> lineFieldOrder = resolveLineFieldOrder(profileConfig);
    Map<String, String> defaultTestMappings = resolveDefaultTestMappings(profileConfig);

    List<String> dataLines = hasHeader && lines.size() > 1 ? lines.subList(1, lines.size()) : lines;

    List<AnalyzerResults> results = new ArrayList<>();
    for (String line : dataLines) {
      if (line == null || line.isBlank()) {
        continue;
      }

      AnalyzerResults result = mapLineToResult(line, lineFieldOrder, defaultTestMappings);
      if (result != null) {
        results.add(result);
      }
    }

    if (results.isEmpty()) {
      return true;
    }
    return persistImport(currentUserId, results);
  }

  @Override
  public String getError() {
    return errorMessage == null ? "GenericFile analyzer unable to write to database" : errorMessage;
  }

  private AnalyzerResults mapLineToResult(
      String line, List<String> lineFieldOrder, Map<String, String> defaultTestMappings) {
    String[] tokens = line.split("\t", -1);
    String sampleId = getValue(tokens, lineFieldOrder, "sampleId");
    String rawTestCode = getValue(tokens, lineFieldOrder, "testCode");
    String resultValue = getValue(tokens, lineFieldOrder, "result");
    String units = getValue(tokens, lineFieldOrder, "units");
    String interpretation = getValue(tokens, lineFieldOrder, "interpretation");

    if (sampleId == null || sampleId.isBlank()) {
      return null;
    }

    // Allow empty result if interpretation is present (e.g. FluoroCycler
    // negative results have empty CP but Interpretation="Negative")
    boolean hasResult = resultValue != null && !resultValue.isBlank();
    boolean hasInterpretation = interpretation != null && !interpretation.isBlank();
    if (!hasResult && !hasInterpretation) {
      return null;
    }

    if (rawTestCode == null || rawTestCode.isBlank()) {
      errorMessage = "GenericFile analyzer missing required 'testCode' for sampleId '" + sampleId + "'";
      return null;
    }

    String mappedTestName = defaultTestMappings.getOrDefault(rawTestCode, rawTestCode);

    // Use interpretation as result for qualitative assays with no numeric value
    String effectiveResult = hasResult ? resultValue : interpretation;

    AnalyzerResults analyzerResult = new AnalyzerResults();
    analyzerResult.setAccessionNumber(sampleId);
    analyzerResult.setTestName(mappedTestName);
    analyzerResult.setResult(effectiveResult);
    analyzerResult.setUnits(units);
    analyzerResult.setTestId("-1");
    analyzerResult.setAnalyzerId(resolveAnalyzerId());

    Timestamp completeDate = parseTimestamp(getValue(tokens, lineFieldOrder, "testDate"),
        getValue(tokens, lineFieldOrder, "testTime"));
    analyzerResult.setCompleteDate(completeDate);
    return analyzerResult;
  }

  private String resolveAnalyzerId() {
    if (getContextAnalyzerId() != null) {
      return getContextAnalyzerId();
    }
    return configuredAnalyzerId;
  }

  @SuppressWarnings("unchecked")
  private Map<String, Object> resolveProfileConfig() {
    if (explicitProfileConfig != null) {
      return explicitProfileConfig;
    }

    String analyzerId = resolveAnalyzerId();
    if (analyzerId == null || analyzerId.isBlank()) {
      return Map.of();
    }
    AnalyzerPluginConfigService configService = SpringContext.getBean(AnalyzerPluginConfigService.class);
    if (configService == null) {
      return Map.of();
    }
    Map<String, Object> config = configService.getConfigAsMap(analyzerId);
    return config == null ? Map.of() : config;
  }

  @SuppressWarnings("unchecked")
  private boolean resolveHasHeader(Map<String, Object> profileConfig) {
    Object defaults = profileConfig.get("configDefaults");
    if (defaults instanceof Map<?, ?> defaultsMap) {
      Object hasHeader = defaultsMap.get("hasHeader");
      if (hasHeader instanceof Boolean b) {
        return b;
      }
    }
    return false;
  }

  @SuppressWarnings("unchecked")
  private Map<String, String> resolveDefaultTestMappings(Map<String, Object> profileConfig) {
    Object mappingsObj = profileConfig.get("default_test_mappings");
    if (!(mappingsObj instanceof Map<?, ?> mappings)) {
      return Map.of();
    }
    return (Map<String, String>) mappings;
  }

  @SuppressWarnings("unchecked")
  private List<String> resolveLineFieldOrder(Map<String, Object> profileConfig) {
    Object lineOrderObj = profileConfig.get("line_field_order");
    if (!(lineOrderObj instanceof List<?> lineOrderList) || lineOrderList.isEmpty()) {
      return DEFAULT_LINE_FIELD_ORDER;
    }
    return (List<String>) lineOrderList;
  }

  private String getValue(String[] tokens, List<String> lineFieldOrder, String fieldName) {
    int index = lineFieldOrder.indexOf(fieldName);
    if (index < 0 || index >= tokens.length) {
      return null;
    }
    String value = tokens[index];
    return value == null ? null : value.trim();
  }

  private Timestamp parseTimestamp(String testDate, String testTime) {
    if (testDate == null || testDate.isBlank()) {
      return null;
    }
    String normalizedTime = (testTime == null || testTime.isBlank()) ? "00:00:00" : testTime;
    List<DateTimeFormatter> dateTimeFormats = List.of(
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"),
        DateTimeFormatter.ofPattern("MM/dd/yyyy HH:mm:ss"),
        DateTimeFormatter.ofPattern("MM/dd/yyyy HH:mm"),
        DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss"),
        DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm"));
    for (DateTimeFormatter formatter : dateTimeFormats) {
      try {
        return Timestamp.valueOf(LocalDateTime.parse(testDate + " " + normalizedTime, formatter));
      } catch (DateTimeParseException e) {
        // try next formatter
      }
    }

    try {
      LocalDate date = LocalDate.parse(testDate, DateTimeFormatter.ISO_DATE);
      LocalTime time = LocalTime.parse(normalizedTime, DateTimeFormatter.ofPattern("HH:mm:ss"));
      return Timestamp.valueOf(LocalDateTime.of(date, time));
    } catch (DateTimeParseException e) {
      return null;
    }
  }
}
