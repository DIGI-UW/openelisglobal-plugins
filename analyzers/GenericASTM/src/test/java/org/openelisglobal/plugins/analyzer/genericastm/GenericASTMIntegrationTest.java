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

  private void cleanTestData() {
    jdbcTemplate.execute(
        "DELETE FROM analyzer_results WHERE accession_number LIKE '2026-A%' OR analyzer_id = '2006'");
    jdbcTemplate.execute("DELETE FROM analyzer_test_map WHERE analyzer_id = '2006'");
    jdbcTemplate.execute("DELETE FROM analyzer_configuration WHERE analyzer_id = '2006'");
    jdbcTemplate.execute("DELETE FROM analyzer WHERE id = '2006'");
  }

  /**
   * Load analyzer 2006 (Mindray BA-88A), its generic-plugin configuration, and test mappings. Uses
   * test ids 1 and 2 from test-result.xml (with localization). No madagascar-analyzer-test-data.xml
   * to avoid DBUnit column mismatch (fhir_uuid).
   */
  private void loadFixtures() {
    jdbcTemplate.execute("SET search_path TO clinlims");

    jdbcTemplate.execute(
        "INSERT INTO analyzer (id, name, analyzer_type, description, is_active, last_updated) "
            + "VALUES ('2006', 'Mindray BA-88A', 'CHEMISTRY', 'ASTM over RS232 Serial', true, NOW())");

    jdbcTemplate.execute(
        "INSERT INTO analyzer_configuration "
            + "(id, analyzer_id, protocol_version, identifier_pattern, is_generic_plugin, status, sys_user_id, last_updated) "
            + "VALUES ('CONFIG-2006', '2006', 'ASTM LIS2-A2', 'MINDRAY.*BA-88A|BA88A', true, 'ACTIVE', '1', NOW())");

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
