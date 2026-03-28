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
import org.openelisglobal.analyzer.service.AnalyzerService;
import org.openelisglobal.analyzer.service.AnalyzerTypeService;
import org.openelisglobal.analyzer.valueholder.Analyzer;
import org.openelisglobal.analyzer.valueholder.AnalyzerType;
import org.openelisglobal.analyzerimport.analyzerreaders.HL7AnalyzerReader;
import org.openelisglobal.analyzerimport.service.AnalyzerTestMappingService;
import org.openelisglobal.analyzerimport.util.AnalyzerTestNameCache;
import org.openelisglobal.analyzerimport.valueholder.AnalyzerTestMapping;
import org.openelisglobal.analyzerimport.valueholder.AnalyzerTestMappingPK;
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
  @Autowired private AnalyzerService analyzerService;
  @Autowired private AnalyzerTypeService analyzerTypeService;
  @Autowired private AnalyzerTestMappingService analyzerTestMappingService;

  protected JdbcTemplate jdbcTemplate;
  private String analyzerTypeId;
  private String analyzerId;

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

    GenericHL7Analyzer plugin = new GenericHL7Analyzer();
    when(pluginAnalyzerService.getAnalyzerPlugins()).thenReturn(Collections.singletonList(plugin));
    plugin.connect();

    // Reload cache so it picks up the Hibernate-inserted fixtures
    AnalyzerTestNameCache cache = AnalyzerTestNameCache.getInstance();
    cache.reloadCache();
    cache.registerAnalyzerName("GenericHL7");

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
            "SELECT COUNT(*) FROM clinlims.analyzer_results WHERE analyzer_id = ?",
            Integer.class,
            Integer.parseInt(analyzerId));
    assertNotNull("Result count should not be null", count);
    assertTrue("Expected at least one analyzer result, got " + count, count >= 1);
  }

  private void cleanTestData() {
    jdbcTemplate.execute("SET search_path TO clinlims");
    jdbcTemplate.execute(
        "DELETE FROM analyzer_results WHERE analyzer_id = '" + (analyzerId != null ? analyzerId : "0") + "'");
    // Clean test mappings by test name
    String[][] mappings = getTestMappings();
    StringBuilder names = new StringBuilder();
    for (int i = 0; i < mappings.length; i++) {
      if (i > 0) names.append(", ");
      names.append("'").append(mappings[i][0]).append("'");
    }
    jdbcTemplate.execute("DELETE FROM analyzer_test_map WHERE analyzer_test_name IN (" + names + ")");
    jdbcTemplate.execute("DELETE FROM analyzer WHERE name = '" + getAnalyzerName() + "'");
  }

  private void loadFixtures() {
    jdbcTemplate.execute("SET search_path TO clinlims");

    // Insert AnalyzerType via Hibernate so AnalyzerTestNameCache can see it
    AnalyzerType existingType = analyzerTypeService.getAnalyzerTypeByName("GenericHL7");
    if (existingType == null) {
      AnalyzerType type = new AnalyzerType();
      type.setName("GenericHL7");
      type.setDescription("Generic HL7 analyzer plugin");
      type.setProtocol("HL7");
      type.setPluginClassName(
          "org.openelisglobal.plugins.analyzer.generichl7.GenericHL7Analyzer");
      type.setGenericPlugin(true);
      type.setActive(true);
      analyzerTypeId = analyzerTypeService.insert(type);
    } else {
      analyzerTypeId = existingType.getId();
    }

    // Insert analyzer instance via Hibernate so findGenericAnalyzersWithPatterns() can see it
    AnalyzerType type = analyzerTypeService.get(analyzerTypeId);
    Analyzer analyzer = new Analyzer();
    analyzer.setName(getAnalyzerName());
    analyzer.setType("HEMATOLOGY");
    analyzer.setDescription("GenericHL7 test");
    analyzer.setIdentifierPattern(getIdentifierPattern());
    analyzer.setActive(true);
    analyzer.setAnalyzerType(type);
    analyzerId = analyzerService.insert(analyzer);

    // Insert test mappings via Hibernate so the cache can see them
    String[][] testMappings = getTestMappings();
    for (String[] mapping : testMappings) {
      AnalyzerTestMappingPK pk = new AnalyzerTestMappingPK();
      pk.setAnalyzerId(analyzerId);
      pk.setAnalyzerTestName(mapping[0]);
      AnalyzerTestMapping testMapping = new AnalyzerTestMapping();
      testMapping.setCompoundId(pk);
      testMapping.setTestId(mapping[1]);
      analyzerTestMappingService.insert(testMapping);
    }
  }

  protected static String loadFixture(String path) throws Exception {
    try (InputStream in = new ClassPathResource(path).getInputStream()) {
      return IOUtils.toString(in, StandardCharsets.UTF_8);
    }
  }
}
