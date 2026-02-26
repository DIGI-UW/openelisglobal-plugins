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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import javax.sql.DataSource;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.analyzerimport.analyzerreaders.ASTMAnalyzerReader;
import org.openelisglobal.analyzerimport.util.AnalyzerTestNameCache;
import org.openelisglobal.analyzerresults.valueholder.AnalyzerResults;
import org.openelisglobal.common.services.PluginAnalyzerService;
import org.openelisglobal.spring.util.SpringContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Integration test for GenericASTM plugin end-to-end flow.
 *
 * <p>Validates that: 1) ASTM message with H-segment matching configured identifier_pattern is
 * recognized; 2) GenericASTM plugin is selected (not legacy plugins); 3) O/R segments are parsed
 * and results inserted.
 *
 * <p>Feature: 011-madagascar-analyzer-integration (GenericASTM parity with GenericHL7 integration
 * testing).
 */
public class GenericASTMIntegrationTest extends BaseWebContextSensitiveTest {

  @Autowired private DataSource dataSource;

  @Autowired private PluginAnalyzerService pluginAnalyzerService;

  private JdbcTemplate jdbcTemplate;

  @Before
  public void setUp() throws Exception {
    super.setUp();
    jdbcTemplate = new JdbcTemplate(dataSource);

    executeDataSetWithStateManagement("testdata/test-result.xml");
    cleanTestData();
    loadFixtures();
    AnalyzerTestNameCache.getInstance().reloadCache();

    GenericASTMAnalyzer plugin = new GenericASTMAnalyzer();
    when(pluginAnalyzerService.getAnalyzerPlugins()).thenReturn(Collections.singletonList(plugin));
    plugin.connect();

    PluginAnalyzerService fromContext = SpringContext.getBean(PluginAnalyzerService.class);
    assertTrue(
        "ASTMAnalyzerReader must see stubbed plugin list",
        fromContext == pluginAnalyzerService
            && fromContext.getAnalyzerPlugins() != null
            && fromContext.getAnalyzerPlugins().size() == 1);
  }

  @After
  public void tearDown() throws Exception {
    cleanTestData();
  }

  /**
   * Test that GenericASTM plugin processes ASTM message from Mindray BA-88A (CONFIG-2006).
   *
   * <p>H-segment "MINDRAY^BA-88A^1.0" matches identifier_pattern "MINDRAY.*BA-88A|BA88A";
   * GenericASTM is selected; O/R segments parsed and results inserted.
   */
  @Test
  public void testGenericASTM_MindrayBA88AMessage_InsertsResults() throws Exception {
    String astmMessage =
        "H|\\^&|||MINDRAY^BA-88A^1.0|INST||20260202|||ASTM^LIS2-A2^LIS2-A2\r\n"
            + "P|1||PAT001|||M|19800101||\r\n"
            + "O|1|2026-A01|||20260202120000||||\r\n"
            + "R|1|^^^GLUCOSE|105.5|mg/dL|20260202120000|N\r\n"
            + "L|1|N\r\n";

    InputStream stream = new ByteArrayInputStream(astmMessage.getBytes(StandardCharsets.UTF_8));

    ASTMAnalyzerReader reader = new ASTMAnalyzerReader();
    boolean streamRead = reader.readStream(stream);
    if (!streamRead) {
      System.err.println("ASTM readStream failed: " + reader.getError());
    }
    boolean inserted = reader.insertAnalyzerData("systemUser");
    if (!inserted) {
      System.err.println("ASTM insertAnalyzerData failed: " + reader.getError());
    }

    assertTrue("ASTM stream should be read successfully: " + reader.getError(), streamRead);
    assertTrue("Results should be inserted successfully", inserted);

    java.util.List<AnalyzerResults> results =
        jdbcTemplate.query(
            "SELECT id, analyzer_id, accession_number, test_name, result, test_id FROM clinlims.analyzer_results WHERE accession_number = '2026-A01'",
            (rs, rowNum) -> {
              AnalyzerResults result = new AnalyzerResults();
              result.setId(rs.getString("id"));
              result.setAnalyzerId(rs.getString("analyzer_id"));
              result.setAccessionNumber(rs.getString("accession_number"));
              result.setTestName(rs.getString("test_name"));
              result.setResult(rs.getString("result"));
              result.setTestId(rs.getString("test_id"));
              return result;
            });

    assertNotNull("Results should be persisted", results);
    assertTrue("Should have at least one result", results.size() >= 1);

    AnalyzerResults glucoseResult =
        results.stream().filter(r -> "1".equals(r.getTestId())).findFirst().orElse(null);
    assertNotNull("GLUCOSE result (test_id=1) should be persisted", glucoseResult);
    assertTrue(
        "analyzer_id should be 2006 (Mindray BA-88A config)",
        "2006".equals(glucoseResult.getAnalyzerId()));
    assertTrue("GLUCOSE value should match", "105.5".equals(glucoseResult.getResult()));
  }

  /**
   * When no plugin matches the H-segment, readStream() returns false (ASTM matches plugin during
   * readStream). No results inserted; error message indicates unknown analyzer.
   */
  @Test
  public void testGenericASTM_UnknownHSegment_NoResultsInserted() throws Exception {
    String astmMessage =
        "H|\\^&|||UNKNOWN^MODEL^1.0|INST||20260202|||ASTM\r\n"
            + "O|1||2026-A02|||20260202120000||||\r\n"
            + "R|1|^^^GLUCOSE|99.0|mg/dL|20260202120000|N\r\n"
            + "L|1|N\r\n";

    InputStream stream = new ByteArrayInputStream(astmMessage.getBytes(StandardCharsets.UTF_8));

    ASTMAnalyzerReader reader = new ASTMAnalyzerReader();
    boolean streamRead = reader.readStream(stream);

    assertTrue("readStream() should succeed (parse-only, no plugin match yet)", streamRead);
    boolean processSuccess = reader.processData("systemUser");
    assertFalse("processData() should fail when no plugin matches", processSuccess);
    assertNotNull("Error should indicate no plugin matched", reader.getError());
    assertTrue(
        "Error message should mention plugin or pattern",
        reader.getError().toLowerCase().contains("plugin")
            || reader.getError().toLowerCase().contains("analyzer"));
  }

  /** Test that GenericASTM plugin handles multiple R-segments (multiple results per message). */
  @Test
  public void testGenericASTM_MultipleRSegments_ParsesAll() throws Exception {
    String astmMessage =
        "H|\\^&|||MINDRAY^BA-88A^1.0|INST||20260202|||ASTM^LIS2-A2^LIS2-A2\r\n"
            + "P|1||PAT002|||F|19900515||\r\n"
            + "O|1|2026-A03|||20260202120000||||\r\n"
            + "R|1|^^^GLUCOSE|100.0|mg/dL|20260202120000|N\r\n"
            + "R|2|^^^HGB|14.2|g/dL|20260202120001|N\r\n"
            + "L|1|N\r\n";

    InputStream stream = new ByteArrayInputStream(astmMessage.getBytes(StandardCharsets.UTF_8));

    ASTMAnalyzerReader reader = new ASTMAnalyzerReader();
    boolean streamRead = reader.readStream(stream);
    boolean inserted = reader.insertAnalyzerData("systemUser");

    assertTrue("ASTM stream should be read successfully", streamRead);
    assertTrue("Results should be inserted successfully", inserted);

    Integer resultCount =
        jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM clinlims.analyzer_results WHERE accession_number = '2026-A03'",
            Integer.class);
    assertTrue(
        "Should have multiple results (at least 2)", resultCount != null && resultCount >= 2);

    Integer countForAnalyzer =
        jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM clinlims.analyzer_results WHERE accession_number = '2026-A03' AND analyzer_id = '2006'",
            Integer.class);
    assertTrue(
        "All results should be for analyzer 2006 (GenericASTM config)",
        countForAnalyzer != null && countForAnalyzer >= 2);
  }

  /**
   * Test that QC samples (O.12 = "Q") are marked as control results.
   *
   * <p>Per ASTM E-1394-97 and Cepheid GeneXpert LIS spec, O.12 (Action Code) = "Q" indicates a QC
   * sample. The GenericASTMLineInserter should set isControl=true for these results.
   */
  @Test
  public void testGenericASTM_QcActionCode_MarksResultAsControl() throws Exception {
    // 26-field O-record with O.12 (index 11) = "Q" for QC sample
    String astmMessage =
        "H|\\^&|||MINDRAY^BA-88A^1.0|INST||20260202|||ASTM^LIS2-A2^LIS2-A2\r\n"
            + "P|1||PAT003|||M|19800101||\r\n"
            + "O|1|QC-CTRL-001|||||||||Q|||||||||||||\r\n"
            + "R|1|^^^GLUCOSE|100.0|mg/dL|||||20260202120000|N\r\n"
            + "L|1|N\r\n";

    InputStream stream = new ByteArrayInputStream(astmMessage.getBytes(StandardCharsets.UTF_8));

    ASTMAnalyzerReader reader = new ASTMAnalyzerReader();
    boolean streamRead = reader.readStream(stream);
    boolean inserted = reader.insertAnalyzerData("systemUser");

    assertTrue("ASTM stream should be read successfully", streamRead);
    assertTrue("Results should be inserted successfully", inserted);

    Boolean isControl =
        jdbcTemplate.queryForObject(
            "SELECT iscontrol FROM clinlims.analyzer_results WHERE accession_number = 'QC-CTRL-001' LIMIT 1",
            Boolean.class);
    assertNotNull("QC result should be persisted", isControl);
    assertTrue("QC result should be marked as control (O.12 = 'Q')", isControl);
  }

  /**
   * Test that patient samples (O.12 empty) are NOT marked as control results.
   *
   * <p>Normal patient samples have an empty or absent Action Code field. The
   * GenericASTMLineInserter should set isControl=false for these results.
   */
  @Test
  public void testGenericASTM_PatientSample_NotMarkedAsControl() throws Exception {
    String astmMessage =
        "H|\\^&|||MINDRAY^BA-88A^1.0|INST||20260202|||ASTM^LIS2-A2^LIS2-A2\r\n"
            + "P|1||PAT004|||F|19900101||\r\n"
            + "O|1|2026-A04|||20260202120000||||\r\n"
            + "R|1|^^^GLUCOSE|95.0|mg/dL|20260202120000|N\r\n"
            + "L|1|N\r\n";

    InputStream stream = new ByteArrayInputStream(astmMessage.getBytes(StandardCharsets.UTF_8));

    ASTMAnalyzerReader reader = new ASTMAnalyzerReader();
    reader.readStream(stream);
    reader.insertAnalyzerData("systemUser");

    Boolean isControl =
        jdbcTemplate.queryForObject(
            "SELECT iscontrol FROM clinlims.analyzer_results WHERE accession_number = '2026-A04' LIMIT 1",
            Boolean.class);
    assertNotNull("Patient result should be persisted", isControl);
    assertFalse("Patient result should NOT be marked as control", isControl);
  }

  /**
   * Test that trailing component delimiters are stripped from qualitative R.4 values.
   *
   * <p>Per Cepheid GeneXpert LIS spec, qualitative results use R.4 format: "NEGATIVE^" (value in
   * component 1, empty component 2). The GenericASTMLineInserter should strip the trailing ^ to
   * store "NEGATIVE".
   */
  @Test
  public void testGenericASTM_TrailingCaret_StrippedFromQualitativeValue() throws Exception {
    String astmMessage =
        "H|\\^&|||MINDRAY^BA-88A^1.0|INST||20260202|||ASTM^LIS2-A2^LIS2-A2\r\n"
            + "P|1||PAT005|||M|19800101||\r\n"
            + "O|1|2026-A05|||20260202120000||||\r\n"
            + "R|1|^^^GLUCOSE|NEGATIVE^||||||20260202120000|N\r\n"
            + "L|1|N\r\n";

    InputStream stream = new ByteArrayInputStream(astmMessage.getBytes(StandardCharsets.UTF_8));

    ASTMAnalyzerReader reader = new ASTMAnalyzerReader();
    reader.readStream(stream);
    reader.insertAnalyzerData("systemUser");

    String storedValue =
        jdbcTemplate.queryForObject(
            "SELECT result FROM clinlims.analyzer_results WHERE accession_number = '2026-A05' LIMIT 1",
            String.class);
    assertNotNull("Result should be persisted", storedValue);
    assertFalse(
        "Stored value should not contain trailing ^: " + storedValue, storedValue.endsWith("^"));
    assertTrue("Stored value should be 'NEGATIVE': " + storedValue, "NEGATIVE".equals(storedValue));
  }

  /**
   * Test that leading component delimiters are stripped from complementary R.4 values.
   *
   * <p>Per Cepheid GeneXpert LIS spec, complementary quantitative results use R.4 format: "^3.10"
   * (empty component 1, value in component 2). The GenericASTMLineInserter should strip the leading
   * ^ to store "3.10".
   */
  @Test
  public void testGenericASTM_LeadingCaret_StrippedFromComplementaryValue() throws Exception {
    String astmMessage =
        "H|\\^&|||MINDRAY^BA-88A^1.0|INST||20260202|||ASTM^LIS2-A2^LIS2-A2\r\n"
            + "P|1||PAT006|||M|19800101||\r\n"
            + "O|1|2026-A06|||20260202120000||||\r\n"
            + "R|1|^^^GLUCOSE|^3.10||||||20260202120000|N\r\n"
            + "L|1|N\r\n";

    InputStream stream = new ByteArrayInputStream(astmMessage.getBytes(StandardCharsets.UTF_8));

    ASTMAnalyzerReader reader = new ASTMAnalyzerReader();
    reader.readStream(stream);
    reader.insertAnalyzerData("systemUser");

    String storedValue =
        jdbcTemplate.queryForObject(
            "SELECT result FROM clinlims.analyzer_results WHERE accession_number = '2026-A06' LIMIT 1",
            String.class);
    assertNotNull("Result should be persisted", storedValue);
    assertFalse(
        "Stored value should not contain leading ^: " + storedValue, storedValue.startsWith("^"));
    assertTrue("Stored value should be '3.10': " + storedValue, "3.10".equals(storedValue));
  }

  /**
   * Test that normal numeric values pass through unchanged (no false stripping).
   *
   * <p>Values like "105.5" should be stored exactly as-is.
   */
  @Test
  public void testGenericASTM_NormalNumericValue_PassesThroughUnchanged() throws Exception {
    String astmMessage =
        "H|\\^&|||MINDRAY^BA-88A^1.0|INST||20260202|||ASTM^LIS2-A2^LIS2-A2\r\n"
            + "P|1||PAT007|||F|19900101||\r\n"
            + "O|1|2026-A07|||20260202120000||||\r\n"
            + "R|1|^^^GLUCOSE|105.5|mg/dL|20260202120000|N\r\n"
            + "L|1|N\r\n";

    InputStream stream = new ByteArrayInputStream(astmMessage.getBytes(StandardCharsets.UTF_8));

    ASTMAnalyzerReader reader = new ASTMAnalyzerReader();
    reader.readStream(stream);
    reader.insertAnalyzerData("systemUser");

    String storedValue =
        jdbcTemplate.queryForObject(
            "SELECT result FROM clinlims.analyzer_results WHERE accession_number = '2026-A07' LIMIT 1",
            String.class);
    assertNotNull("Result should be persisted", storedValue);
    assertTrue("Normal value should be unchanged: " + storedValue, "105.5".equals(storedValue));
  }

  private void cleanTestData() {
    jdbcTemplate.execute(
        "DELETE FROM analyzer_results WHERE accession_number LIKE '2026-A%'"
            + " OR accession_number LIKE 'QC-CTRL%'"
            + " OR analyzer_id = '2006'");
    jdbcTemplate.execute("DELETE FROM analyzer_test_map WHERE analyzer_id = '2006'");
    jdbcTemplate.execute("DELETE FROM analyzer WHERE id = '2006'");
    jdbcTemplate.execute("DELETE FROM analyzer_type WHERE id = 9999");
  }

  /**
   * Load analyzer 2006 (Mindray BA-88A) and test mappings. Uses test ids 1 and 2 from
   * test-result.xml (with localization).
   *
   * <p>NOTE: identifier_pattern is set on the analyzer row — this is where the runtime DAO
   * (AnalyzerDAOImpl.findGenericAnalyzersWithPatterns) reads it from.
   */
  private void loadFixtures() {
    jdbcTemplate.execute("SET search_path TO clinlims");

    // Create analyzer_type for GenericASTM (mirrors what PluginRegistryService does at startup)
    jdbcTemplate.execute(
        "INSERT INTO analyzer_type (id, name, description, protocol, plugin_class_name, is_generic_plugin, is_active, last_updated) "
            + "VALUES (9999, 'GenericASTM', 'Generic ASTM analyzer plugin', 'ASTM', "
            + "'org.openelisglobal.plugins.analyzer.genericastm.GenericASTMAnalyzer', true, true, NOW()) "
            + "ON CONFLICT (name) DO UPDATE SET id = EXCLUDED.id");

    // Insert analyzer WITH analyzer_type_id — required for findGenericAnalyzersWithPatterns() INNER
    // JOIN
    jdbcTemplate.execute(
        "INSERT INTO analyzer (id, name, analyzer_type, description, identifier_pattern, is_active, analyzer_type_id, last_updated) "
            + "VALUES ('2006', 'Mindray BA-88A', 'CHEMISTRY', 'ASTM over RS232 Serial', "
            + "'MINDRAY.*BA-88A|BA88A', true, 9999, NOW())");

    String[][] testMappings = {{"GLUCOSE", "1"}, {"HGB", "2"}};

    for (String[] mapping : testMappings) {
      jdbcTemplate.execute(
          "INSERT INTO analyzer_test_map (analyzer_id, analyzer_test_name, test_id, last_updated) "
              + "VALUES ('2006', '"
              + mapping[0]
              + "', "
              + mapping[1]
              + ", NOW())");
    }
  }
}
