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
package org.openelisglobal.plugins.analyzer.generichl7.mindray;

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
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.analyzerimport.analyzerreaders.HL7AnalyzerReader;
import org.openelisglobal.analyzerimport.util.AnalyzerTestNameCache;
import org.openelisglobal.common.services.PluginAnalyzerService;
import org.openelisglobal.plugins.analyzer.generichl7.GenericHL7Analyzer;
import org.openelisglobal.spring.util.SpringContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Base class for Mindray HL7 integration tests via GenericHL7 plugin.
 *
 * <p>Tests the end-to-end flow: HL7 message → GenericHL7 plugin (MSH-3 pattern matching) →
 * GenericHL7LineInserter → analyzer_results table.
 *
 * <p>Feature: 011-madagascar-analyzer-integration
 */
public abstract class AbstractMindrayHL7Test extends BaseWebContextSensitiveTest {

  @Autowired private DataSource dataSource;
  @Autowired private PluginAnalyzerService pluginAnalyzerService;

  protected JdbcTemplate jdbcTemplate;

  protected abstract String getAnalyzerId();

  protected abstract String getAnalyzerName();

  protected abstract String getIdentifierPattern();

  protected abstract String getFixturePath();

  /** Override in subclasses that need different OBX-to-test mappings (e.g. chemistry). */
  protected String[][] getTestMappings() {
    return new String[][] {{"WBC", "1"}, {"RBC", "2"}, {"HGB", "1"}, {"HCT", "2"}, {"PLT", "1"}};
  }

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

  protected void verifyHl7MessageResultsStored() throws Exception {
    String raw = loadFixture(getFixturePath());
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
            Integer.parseInt(getAnalyzerId()));
    assertNotNull("Result count should not be null", count);
    assertTrue("Expected at least one analyzer result, got " + count, count >= 1);
  }

  private void cleanTestData() {
    jdbcTemplate.execute(
        "DELETE FROM analyzer_results WHERE analyzer_id = '" + getAnalyzerId() + "'");
    jdbcTemplate.execute(
        "DELETE FROM analyzer_test_map WHERE analyzer_id = '" + getAnalyzerId() + "'");
    jdbcTemplate.execute(
        "DELETE FROM analyzer_configuration WHERE analyzer_id = '" + getAnalyzerId() + "'");
    jdbcTemplate.execute("DELETE FROM analyzer WHERE id = '" + getAnalyzerId() + "'");
  }

  private void loadFixtures() {
    jdbcTemplate.execute("SET search_path TO clinlims");

    jdbcTemplate.execute(
        "INSERT INTO analyzer (id, name, analyzer_type, description, is_active, last_updated) "
            + "VALUES ('"
            + getAnalyzerId()
            + "', '"
            + getAnalyzerName()
            + "', 'HEMATOLOGY', 'GenericHL7 test', true, NOW())");

    jdbcTemplate.execute(
        "INSERT INTO analyzer_configuration "
            + "(id, analyzer_id, protocol_version, identifier_pattern, is_generic_plugin, status, sys_user_id, last_updated) "
            + "VALUES ('CONFIG-"
            + getAnalyzerId()
            + "-TEST', '"
            + getAnalyzerId()
            + "', 'HL7 v2.5.1', '"
            + getIdentifierPattern()
            + "', true, 'ACTIVE', '1', NOW())");

    String[][] testMappings = getTestMappings();

    for (String[] mapping : testMappings) {
      jdbcTemplate.execute(
          "INSERT INTO analyzer_test_map "
              + "(analyzer_id, analyzer_test_name, test_id, last_updated) "
              + "VALUES ("
              + getAnalyzerId()
              + ", '"
              + mapping[0]
              + "', "
              + mapping[1]
              + ", NOW())");
    }
  }

  protected static String loadFixture(String path) throws Exception {
    try (InputStream in = new ClassPathResource(path).getInputStream()) {
      return IOUtils.toString(in, StandardCharsets.UTF_8);
    }
  }
}
