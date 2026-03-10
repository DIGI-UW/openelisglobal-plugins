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
