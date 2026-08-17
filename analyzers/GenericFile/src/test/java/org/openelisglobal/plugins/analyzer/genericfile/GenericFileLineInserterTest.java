package org.openelisglobal.plugins.analyzer.genericfile;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Test;
import org.openelisglobal.analyzerresults.valueholder.AnalyzerResults;

public class GenericFileLineInserterTest {

  @Test
  public void testInsert_Qs5LineOrder_MapsExpectedFields() {
    Map<String, Object> profile = new HashMap<>();
    profile.put("line_field_order", List.of("sampleId", "testCode", "result", "units"));
    profile.put("default_test_mappings", Map.of("VL", "HIV-1 Viral Load"));

    CapturingGenericFileLineInserter inserter =
        new CapturingGenericFileLineInserter("301", profile);
    inserter.setContextAnalyzerId("301");

    boolean success = inserter.insert(List.of("SAMPLE-QS5\tVL\t35.7\tcopies/mL"), "1");

    assertTrue(success);
    assertNotNull(inserter.captured);
    assertEquals(1, inserter.captured.size());
    AnalyzerResults result = inserter.captured.get(0);
    assertEquals("SAMPLE-QS5", result.getAccessionNumber());
    assertEquals("HIV-1 Viral Load", result.getTestName());
    assertEquals("35.7", result.getResult());
    assertEquals("copies/mL", result.getUnits());
    assertEquals("301", result.getAnalyzerId());
  }

  @Test
  public void testInsert_Qs7LineOrder_MapsExpectedFields() {
    Map<String, Object> profile = new HashMap<>();
    profile.put("line_field_order", List.of("testCode", "sampleId", "result", "units"));
    profile.put("default_test_mappings", Map.of("CT", "Cycle Threshold"));

    CapturingGenericFileLineInserter inserter =
        new CapturingGenericFileLineInserter("302", profile);
    inserter.setContextAnalyzerId("302");

    boolean success = inserter.insert(List.of("CT\tSAMPLE-QS7\t27.12\tCt"), "1");

    assertTrue(success);
    assertNotNull(inserter.captured);
    assertEquals(1, inserter.captured.size());
    AnalyzerResults result = inserter.captured.get(0);
    assertEquals("SAMPLE-QS7", result.getAccessionNumber());
    assertEquals("Cycle Threshold", result.getTestName());
    assertEquals("27.12", result.getResult());
    assertEquals("Ct", result.getUnits());
    assertEquals("302", result.getAnalyzerId());
  }

  @Test
  public void testInsert_MissingTestCode_SkipsLine() {
    Map<String, Object> profile = new HashMap<>();
    profile.put("line_field_order", List.of("sampleId", "testCode", "result"));

    CapturingGenericFileLineInserter inserter =
        new CapturingGenericFileLineInserter("303", profile);
    inserter.setContextAnalyzerId("303");

    // testCode is blank
    boolean success = inserter.insert(List.of("SAMPLE-1\t\t35.7"), "1");

    assertTrue(success);
    // No results captured because testCode was blank
    assertTrue(inserter.captured == null || inserter.captured.isEmpty());
  }

  @Test
  public void testInsert_WithDateTimeFields_ParsesTimestamp() {
    Map<String, Object> profile = new HashMap<>();
    profile.put("line_field_order",
        List.of("sampleId", "testCode", "result", "units", "testDate", "testTime"));
    profile.put("default_test_mappings", Map.of());

    CapturingGenericFileLineInserter inserter =
        new CapturingGenericFileLineInserter("304", profile);
    inserter.setContextAnalyzerId("304");

    // ISO date format
    boolean success = inserter.insert(
        List.of("S1\tVL\t100\tcopies/mL\t2026-03-11\t14:30:00"), "1");
    assertTrue(success);
    assertNotNull(inserter.captured);
    assertEquals(1, inserter.captured.size());
    assertNotNull("ISO date should parse", inserter.captured.get(0).getCompleteDate());
  }

  @Test
  public void testInsert_WithUSDateFormat_ParsesTimestamp() {
    Map<String, Object> profile = new HashMap<>();
    profile.put("line_field_order",
        List.of("sampleId", "testCode", "result", "units", "testDate", "testTime"));
    profile.put("default_test_mappings", Map.of());

    CapturingGenericFileLineInserter inserter =
        new CapturingGenericFileLineInserter("305", profile);
    inserter.setContextAnalyzerId("305");

    // US date format MM/dd/yyyy with HH:mm (no seconds)
    boolean success = inserter.insert(
        List.of("S2\tVL\t200\tcopies/mL\t03/11/2026\t14:30"), "1");
    assertTrue(success);
    assertNotNull(inserter.captured);
    assertEquals(1, inserter.captured.size());
    assertNotNull("US date format should parse", inserter.captured.get(0).getCompleteDate());
  }

  @Test
  public void testInsert_WithHeaderLine_SkipsHeader() {
    Map<String, Object> profile = new HashMap<>();
    profile.put("line_field_order", List.of("sampleId", "testCode", "result"));
    profile.put("default_test_mappings", Map.of());
    profile.put("configDefaults", Map.of("hasHeader", true));

    CapturingGenericFileLineInserter inserter =
        new CapturingGenericFileLineInserter("306", profile);
    inserter.setContextAnalyzerId("306");

    // First line is a header (Excel column names), second is data
    boolean success = inserter.insert(
        List.of("Sample Name\tTarget Name\tQuantity Mean", "S1\tVL\t35.7"), "1");

    assertTrue(success);
    assertNotNull(inserter.captured);
    assertEquals("Header should be skipped, only data row processed", 1, inserter.captured.size());
    AnalyzerResults result = inserter.captured.get(0);
    assertEquals("S1", result.getAccessionNumber());
    assertEquals("VL", result.getTestName());
    assertEquals("35.7", result.getResult());
  }

  @Test
  public void testInsert_WithoutHeaderConfig_ProcessesAllLines() {
    Map<String, Object> profile = new HashMap<>();
    profile.put("line_field_order", List.of("sampleId", "testCode", "result"));
    profile.put("default_test_mappings", Map.of());
    // No configDefaults.hasHeader — backwards compatibility

    CapturingGenericFileLineInserter inserter =
        new CapturingGenericFileLineInserter("307", profile);
    inserter.setContextAnalyzerId("307");

    boolean success = inserter.insert(
        List.of("S1\tVL\t35.7", "S2\tCT\t27.1"), "1");

    assertTrue(success);
    assertNotNull(inserter.captured);
    assertEquals("Without hasHeader, all lines should be processed", 2, inserter.captured.size());
  }

  @Test
  public void testInsert_WithHeaderAndDefaultFieldOrder_MapsCorrectly() {
    // Simulates the real pipeline: ExcelAnalyzerReader produces header + data
    // with PREFERRED_FIELD_ORDER positions, and inserter uses DEFAULT_LINE_FIELD_ORDER
    Map<String, Object> profile = new HashMap<>();
    // No explicit line_field_order — uses DEFAULT
    profile.put("default_test_mappings", Map.of("VIH-1", "HIV-1 VL (LOINC 20447-9)"));
    profile.put("configDefaults", Map.of("hasHeader", true));

    CapturingGenericFileLineInserter inserter =
        new CapturingGenericFileLineInserter("308", profile);
    inserter.setContextAnalyzerId("308");

    // Simulates ExcelAnalyzerReader output with empty tabs for absent fields:
    // sampleId\ttestCode\tresult\tinterpretation\tposition\ttestDate\ttestTime
    String headerLine = "Sample Name\tTarget Name\tQuantity Mean\t\tWell Position\t\t";
    String dataLine = "E2E001\tVIH-1\t35.7\t\tA1\t\t";

    boolean success = inserter.insert(List.of(headerLine, dataLine), "1");

    assertTrue(success);
    assertNotNull(inserter.captured);
    assertEquals(1, inserter.captured.size());
    AnalyzerResults result = inserter.captured.get(0);
    assertEquals("E2E001", result.getAccessionNumber());
    assertEquals("HIV-1 VL (LOINC 20447-9)", result.getTestName());
    assertEquals("35.7", result.getResult());
  }

  @Test
  public void testInsert_EmptyResultWithInterpretation_UsesInterpretationAsResult() {
    Map<String, Object> profile = new HashMap<>();
    profile.put("line_field_order",
        List.of("sampleId", "testCode", "result", "interpretation", "position", "testDate", "testTime"));
    profile.put("default_test_mappings", Map.of());

    CapturingGenericFileLineInserter inserter =
        new CapturingGenericFileLineInserter("309", profile);
    inserter.setContextAnalyzerId("309");

    // Empty result but valid interpretation (FluoroCycler negative case)
    boolean success = inserter.insert(
        List.of("E2E-FC003\tVIH-1\t\tNegative\tC3\t2026-03-11\t"), "1");

    assertTrue(success);
    assertNotNull(inserter.captured);
    assertEquals("Row with interpretation should not be dropped", 1, inserter.captured.size());
    AnalyzerResults result = inserter.captured.get(0);
    assertEquals("E2E-FC003", result.getAccessionNumber());
    assertEquals("VIH-1", result.getTestName());
    assertEquals("Negative", result.getResult()); // interpretation used as result
  }

  @Test
  public void testInsert_EmptyResultAndNoInterpretation_SkipsRow() {
    Map<String, Object> profile = new HashMap<>();
    profile.put("line_field_order",
        List.of("sampleId", "testCode", "result", "interpretation"));
    profile.put("default_test_mappings", Map.of());

    CapturingGenericFileLineInserter inserter =
        new CapturingGenericFileLineInserter("310", profile);
    inserter.setContextAnalyzerId("310");

    // Both result and interpretation empty — should be skipped
    boolean success = inserter.insert(
        List.of("SAMPLE-1\tVL\t\t"), "1");

    assertTrue(success);
    assertTrue("Row with no result and no interpretation should be skipped",
        inserter.captured == null || inserter.captured.isEmpty());
  }

  @Test
  public void testInsert_ResultPresentWithInterpretation_UsesResult() {
    Map<String, Object> profile = new HashMap<>();
    profile.put("line_field_order",
        List.of("sampleId", "testCode", "result", "interpretation"));
    profile.put("default_test_mappings", Map.of());

    CapturingGenericFileLineInserter inserter =
        new CapturingGenericFileLineInserter("311", profile);
    inserter.setContextAnalyzerId("311");

    // Both result and interpretation present — result takes precedence
    boolean success = inserter.insert(
        List.of("E2E-FC001\tVIH-1\t28.5\tPositive"), "1");

    assertTrue(success);
    assertNotNull(inserter.captured);
    assertEquals(1, inserter.captured.size());
    assertEquals("28.5", inserter.captured.get(0).getResult()); // result, not interpretation
  }

  private static class CapturingGenericFileLineInserter extends GenericFileLineInserter {
    private List<AnalyzerResults> captured;

    private CapturingGenericFileLineInserter(
        String configuredAnalyzerId, Map<String, Object> explicitProfileConfig) {
      super(configuredAnalyzerId, explicitProfileConfig);
    }

    @Override
    protected void persistResults(List<AnalyzerResults> results, String systemUserId) {
      this.captured = results;
    }
  }
}
