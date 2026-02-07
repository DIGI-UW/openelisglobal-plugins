/**
 * Integration test for Sysmex XN Series HL7 (hematology/CBC) via GenericHL7 plugin.
 *
 * <p>Task Reference: T217 [M14] Sysmex XN HL7 integration. Feature:
 * 011-madagascar-analyzer-integration
 */
package org.openelisglobal.plugins.analyzer.generichl7.sysmex;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import javax.sql.DataSource;
import org.apache.commons.io.IOUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.analyzerimport.analyzerreaders.HL7AnalyzerReader;
import org.openelisglobal.analyzerimport.util.AnalyzerTestNameCache;
import org.openelisglobal.common.services.PluginAnalyzerService;
import org.openelisglobal.plugins.analyzer.generichl7.GenericHL7Analyzer;
import org.openelisglobal.spring.util.SpringContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;

public class SysmexXNHL7Test extends BaseWebContextSensitiveTest {

  private static final String ANALYZER_ID = "3002";
  private static final String ANALYZER_NAME = "Sysmex XN-L";
  private static final String IDENTIFIER_PATTERN = "SYSMEX.*XN|XN-.*";
  private static final String FIXTURE_PATH = "testdata/hl7/sysmex-xn-result.hl7";

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

    GenericHL7Analyzer plugin = new GenericHL7Analyzer();
    when(pluginAnalyzerService.getAnalyzerPlugins()).thenReturn(Collections.singletonList(plugin));
    plugin.connect();

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

  @Test
  public void sysmexXnHl7Message_resultsStored() throws Exception {
    String raw = loadFixture(FIXTURE_PATH);
    InputStream in = new ByteArrayInputStream(raw.getBytes(StandardCharsets.UTF_8));

    HL7AnalyzerReader reader = new HL7AnalyzerReader();
    boolean readOk = reader.readStream(in);
    assertTrue("readStream should succeed: " + reader.getError(), readOk);

    boolean insertOk = reader.insertAnalyzerData("1");
    assertTrue("insertAnalyzerData should succeed: " + reader.getError(), insertOk);

    Integer count =
        jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM analyzer_results WHERE analyzer_id = ?",
            Integer.class,
            Integer.parseInt(ANALYZER_ID));
    assertNotNull("Result count should not be null", count);
    assertTrue("Expected at least one analyzer result, got " + count, count >= 1);
  }

  private void cleanTestData() {
    jdbcTemplate.execute("DELETE FROM analyzer_results WHERE analyzer_id = '" + ANALYZER_ID + "'");
    jdbcTemplate.execute("DELETE FROM analyzer_test_map WHERE analyzer_id = '" + ANALYZER_ID + "'");
    jdbcTemplate.execute(
        "DELETE FROM analyzer_configuration WHERE analyzer_id = '" + ANALYZER_ID + "'");
    jdbcTemplate.execute("DELETE FROM analyzer WHERE id = '" + ANALYZER_ID + "'");
  }

  private void loadFixtures() {
    jdbcTemplate.execute("SET search_path TO clinlims");

    jdbcTemplate.execute(
        "INSERT INTO analyzer (id, name, analyzer_type, description, is_active, last_updated) "
            + "VALUES ('"
            + ANALYZER_ID
            + "', '"
            + ANALYZER_NAME
            + "', 'HEMATOLOGY', 'GenericHL7 test', true, NOW())");

    jdbcTemplate.execute(
        "INSERT INTO analyzer_configuration "
            + "(id, analyzer_id, protocol_version, identifier_pattern, is_generic_plugin, status, sys_user_id, last_updated) "
            + "VALUES ('CONFIG-"
            + ANALYZER_ID
            + "-TEST', '"
            + ANALYZER_ID
            + "', 'HL7 v2.5.1', '"
            + IDENTIFIER_PATTERN
            + "', true, 'ACTIVE', '1', NOW())");

    // Map test codes — use test ids 1 and 2 from test-result.xml
    String[][] testMappings = {
      {"WBC", "1"},
      {"RBC", "2"},
      {"HGB", "1"},
      {"HCT", "2"},
      {"PLT", "1"},
      {"NEUT%", "2"},
      {"LYMPH%", "1"},
      {"MONO%", "2"},
      {"EO%", "1"},
      {"BASO%", "2"},
      {"NEUT#", "1"},
      {"LYMPH#", "2"},
      {"MONO#", "1"},
      {"MCV", "2"},
      {"MCH", "1"},
      {"MCHC", "2"}
    };

    for (String[] mapping : testMappings) {
      jdbcTemplate.execute(
          "INSERT INTO analyzer_test_map "
              + "(analyzer_id, analyzer_test_name, test_id, last_updated) "
              + "VALUES ("
              + ANALYZER_ID
              + ", '"
              + mapping[0]
              + "', "
              + mapping[1]
              + ", NOW())");
    }
  }

  private static String loadFixture(String path) throws Exception {
    try (InputStream in = new ClassPathResource(path).getInputStream()) {
      return IOUtils.toString(in, StandardCharsets.UTF_8);
    }
  }
}
