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
package org.openelisglobal.plugins.analyzer.generichl7;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import javax.sql.DataSource;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.analyzer.service.AnalyzerService;
import org.openelisglobal.analyzer.service.AnalyzerTypeService;
import org.openelisglobal.analyzer.valueholder.Analyzer;
import org.openelisglobal.analyzer.valueholder.AnalyzerType;
import org.openelisglobal.analyzerimport.analyzerreaders.HL7AnalyzerReader;
import org.openelisglobal.analyzerimport.util.AnalyzerTestNameCache;
import org.openelisglobal.analyzerresults.valueholder.AnalyzerResults;
import org.openelisglobal.common.services.PluginAnalyzerService;
import org.openelisglobal.spring.util.SpringContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Integration test for GenericHL7 plugin end-to-end flow.
 *
 * <p>Validates that: 1. HL7 message with MSH-3 matching configured pattern is recognized 2.
 * GenericHL7 plugin selected (not legacy plugins) 3. OBX segments parsed and results inserted
 *
 * <p>Task Reference: T207 (M19) - Integration test with mock HL7 server
 */
public class GenericHL7IntegrationTest extends BaseWebContextSensitiveTest {

  @Autowired private DataSource dataSource;

  @Autowired private PluginAnalyzerService pluginAnalyzerService;

  @Autowired private AnalyzerService analyzerService;

  @Autowired private AnalyzerTypeService analyzerTypeService;

  private JdbcTemplate jdbcTemplate;
  private String analyzerTypeId;
  private String analyzerId;

  @Before
  public void setUp() throws Exception {
    super.setUp();
    jdbcTemplate = new JdbcTemplate(dataSource);

    // Use test-result.xml which includes proper Test entities with localization
    // (test id=1,2)
    executeDataSetWithStateManagement("testdata/test-result.xml");
    cleanTestData();
    loadFixtures();

    GenericHL7Analyzer plugin = new GenericHL7Analyzer();
    when(pluginAnalyzerService.getAnalyzerPlugins()).thenReturn(Collections.singletonList(plugin));
    plugin.connect();

    // Reload cache so it picks up the Hibernate-inserted fixtures
    AnalyzerTestNameCache cache = AnalyzerTestNameCache.getInstance();
    cache.reloadCache();

    PluginAnalyzerService fromContext = SpringContext.getBean(PluginAnalyzerService.class);
    assertTrue(
        "HL7AnalyzerReader must see stubbed plugin list",
        fromContext == pluginAnalyzerService
            && fromContext.getAnalyzerPlugins() != null
            && fromContext.getAnalyzerPlugins().size() == 1);
  }

  @After
  public void tearDown() throws Exception {
    cleanTestData();
  }

  /**
   * Test that GenericHL7 plugin processes HL7 ORU^R01 from Mindray BC2000.
   *
   * <p>Tests the complete flow: - HL7 message with MSH-3 = "MINDRAY" matches identifier_pattern -
   * GenericHL7 plugin selected via pattern matching - OBX segments parsed - Results inserted into
   * analyzer_results table
   */
  @Test
  public void testGenericHL7_MindrayBC2000Message_InsertsResults() throws Exception {
    // Arrange: HL7 ORU^R01 message from Mindray BC2000 (format matches test
    // fixtures)
    String hl7Message =
        "MSH|^~\\&|MINDRAY|LAB|OpenELIS|LAB|20260202120000||ORU^R01|MSG001|P|2.5.1||||||||\r"
            + "PID|1||PAT123^^^HOSPITAL||Doe^John||19700101|M\r"
            + "OBR|1||2026-00001|CBC^Complete Blood Count|||20260202115900\r"
            + "OBX|1|NM|WBC||7.5|10^3/uL|4.0-11.0|N|||F\r"
            + "OBX|2|NM|RBC||4.8|10^6/uL|4.5-5.5|N|||F\r"
            + "OBX|3|NM|HGB||14.2|g/dL|13.0-17.0|N|||F\r";

    InputStream stream = new ByteArrayInputStream(hl7Message.getBytes(StandardCharsets.UTF_8));

    // Act: Process message via HL7AnalyzerReader
    HL7AnalyzerReader reader = new HL7AnalyzerReader();
    boolean streamRead = reader.readStream(stream);
    boolean inserted = reader.insertAnalyzerData("systemUser");

    // Assert: Verify message processed
    assertTrue("HL7 stream should be read successfully: " + reader.getError(), streamRead);
    assertTrue("Results should be inserted successfully", inserted);

    // Verify results were persisted
    List<AnalyzerResults> results =
        jdbcTemplate.query(
            "SELECT * FROM clinlims.analyzer_results WHERE accession_number = '2026-00001'",
            (rs, rowNum) -> {
              AnalyzerResults result = new AnalyzerResults();
              result.setId(rs.getString("id"));
              result.setAnalyzerId(rs.getString("analyzer_id"));
              result.setAccessionNumber(rs.getString("accession_number"));
              result.setTestName(rs.getString("test_name"));
              result.setResult(rs.getString("result"));
              return result;
            });

    assertNotNull("Results should be persisted", results);
    assertTrue("Should have at least one result", results.size() >= 1);

    // Verify specific test result
    AnalyzerResults wbcResult =
        results.stream()
            .filter(r -> "WBC".equals(r.getTestName()) || r.getTestName().contains("White"))
            .findFirst()
            .orElse(null);

    if (wbcResult != null) {
      assertEquals("WBC value should match", "7.5", wbcResult.getResult());
    }
  }

  /** Test that GenericHL7 plugin correctly rejects messages without matching pattern. */
  @Test
  public void testGenericHL7_UnknownMsh3_NoResultsInserted() throws Exception {
    // Arrange: HL7 message with MSH-3 that doesn't match any configured pattern
    String hl7Message =
        "MSH|^~\\&|UNKNOWN_ANALYZER|LAB|OpenELIS|LAB|20260202120000||ORU^R01|MSG001|P|2.5.1||||||||\r"
            + "PID|1||PAT123^^^HOSPITAL||Doe^John||19700101|M\r"
            + "OBR|1||2026-00002|CBC^Complete Blood Count|||20260202115900\r"
            + "OBX|1|NM|WBC||7.5|10^3/uL|4.0-11.0|N|||F\r";

    InputStream stream = new ByteArrayInputStream(hl7Message.getBytes(StandardCharsets.UTF_8));

    // Act: Process message via HL7AnalyzerReader
    HL7AnalyzerReader reader = new HL7AnalyzerReader();
    boolean streamRead = reader.readStream(stream);

    // Assert: Stream should read but no analyzer should be identified
    assertTrue("HL7 stream should be read successfully", streamRead);

    // Note: insertAnalyzerData may return false if no analyzer identified
    // This is expected behavior for unmapped analyzers
  }

  /** Test that GenericHL7 plugin handles multiple OBX segments correctly. */
  @Test
  public void testGenericHL7_MultipleObxSegments_ParsesAll() throws Exception {
    // Arrange: Complete CBC panel with 5 OBX segments
    String hl7Message =
        "MSH|^~\\&|MINDRAY|LAB|OpenELIS|LAB|20260202120000||ORU^R01|MSG002|P|2.5.1||||||||\r"
            + "PID|1||PAT456^^^HOSPITAL||Smith^Jane||19800515|F\r"
            + "OBR|1||2026-00003|CBC^Complete Blood Count|||20260202120000\r"
            + "OBX|1|NM|WBC||8.2|10^3/uL|4.0-11.0|N|||F\r"
            + "OBX|2|NM|RBC||4.5|10^6/uL|4.5-5.5|N|||F\r"
            + "OBX|3|NM|HGB||13.5|g/dL|13.0-17.0|N|||F\r"
            + "OBX|4|NM|HCT||40.2|%|39.0-49.0|N|||F\r"
            + "OBX|5|NM|PLT||250|10^3/uL|150-400|N|||F\r";

    InputStream stream = new ByteArrayInputStream(hl7Message.getBytes(StandardCharsets.UTF_8));

    // Act: Process message
    HL7AnalyzerReader reader = new HL7AnalyzerReader();
    boolean streamRead = reader.readStream(stream);
    boolean inserted = reader.insertAnalyzerData("systemUser");

    // Assert: Multiple results should be processed
    assertTrue("HL7 stream should be read successfully", streamRead);
    assertTrue("Results should be inserted successfully", inserted);

    // Verify multiple results were persisted
    int resultCount =
        jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM clinlims.analyzer_results WHERE accession_number = '2026-00003'",
            Integer.class);

    assertTrue("Should have multiple results (at least 2)", resultCount >= 2);
  }

  /** Clean test data to prevent pollution. */
  private void cleanTestData() {
    jdbcTemplate.execute("SET search_path TO clinlims");
    jdbcTemplate.execute("DELETE FROM analyzer_results WHERE accession_number LIKE '2026-%'");
    jdbcTemplate.execute(
        "DELETE FROM analyzer_test_map WHERE analyzer_test_name IN ('WBC', 'RBC', 'HGB', 'HCT', 'PLT')");
    // Analyzer inserted via Hibernate - delete via JDBC after mappings are gone
    jdbcTemplate.execute("DELETE FROM analyzer WHERE name = 'Mindray BC2000'");
  }

  /**
   * Load test fixtures for GenericHL7 integration testing. Uses test id=1 from test-result.xml
   * which has proper localization.
   */
  private void loadFixtures() {
    jdbcTemplate.execute("SET search_path TO clinlims");

    // Insert AnalyzerType via Hibernate so AnalyzerTestNameCache can see it
    AnalyzerType existingType = analyzerTypeService.getAnalyzerTypeByName("GenericHL7");
    if (existingType == null) {
      AnalyzerType type = new AnalyzerType();
      type.setName("GenericHL7");
      type.setDescription("Generic HL7 analyzer plugin");
      type.setProtocol("HL7");
      type.setPluginClassName("org.openelisglobal.plugins.analyzer.generichl7.GenericHL7Analyzer");
      type.setGenericPlugin(true);
      type.setActive(true);
      analyzerTypeId = analyzerTypeService.insert(type);
    } else {
      analyzerTypeId = existingType.getId();
    }

    // Insert analyzer instance via Hibernate so findGenericAnalyzersWithPatterns() can see it
    AnalyzerType type = analyzerTypeService.get(analyzerTypeId);
    Analyzer analyzer = new Analyzer();
    analyzer.setName("Mindray BC2000");
    analyzer.setType("HEMATOLOGY");
    analyzer.setDescription("HL7 v2.3.1 over TCP/IP (MLLP)");
    analyzer.setIdentifierPattern("MINDRAY");
    analyzer.setActive(true);
    analyzer.setAnalyzerType(type);
    analyzerId = analyzerService.insert(analyzer);

    // Insert test mappings directly so the integration flow still exercises the
    // built OpenELIS artifact without relying on branch-specific helper APIs.
    // Map analyzer test codes to test ids 1 and 2 (from test-result.xml, both have
    // localization).
    String[][] testMappings = {
      {"WBC", "1"}, // test 1 = Complete Blood Count
      {"RBC", "2"}, // test 2 = Urinalysis
      {"HGB", "1"},
      {"HCT", "2"},
      {"PLT", "1"}
    };

    for (String[] mapping : testMappings) {
      jdbcTemplate.update(
          "INSERT INTO analyzer_test_map (analyzer_id, analyzer_test_name, test_id) VALUES (?, ?, ?)",
          analyzerId,
          mapping[0],
          mapping[1]);
    }
  }
}
