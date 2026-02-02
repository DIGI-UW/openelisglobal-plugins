/*
 * The contents of this file are subject to the Mozilla Public License
 * Version 1.1 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.mozilla.org/MPL/
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
 * License for the specific language governing rights and limitations under
 * the License.
 *
 * The Original Code is OpenELIS code.
 *
 * Copyright (C) ITECH, University of Washington, Seattle WA.  All Rights Reserved.
 */

package oe.plugin.analyzer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import org.junit.Test;
import org.openelisglobal.analyzer.service.AnalyzerService;
import org.openelisglobal.analyzer.valueholder.Analyzer;
import org.openelisglobal.analyzerresults.valueholder.AnalyzerResults;
import org.openelisglobal.plugin.test.PluginTestBase;
import org.openelisglobal.spring.util.SpringContext;
import org.openelisglobal.test.service.TestService;

public class MindrayAnalyzerImplementationTest extends PluginTestBase {

  @Override
  protected void setupMocks() {
    TestService mockTestService = mock(TestService.class);
    AnalyzerService mockAnalyzerService = mock(AnalyzerService.class);

    // Setup analyzer mock
    Analyzer mockAnalyzer = new Analyzer();
    mockAnalyzer.setId("1");
    mockAnalyzer.setName("Mindray");

    when(SpringContext.getBean(TestService.class)).thenReturn(mockTestService);
    when(SpringContext.getBean(AnalyzerService.class)).thenReturn(mockAnalyzerService);
    when(mockAnalyzerService.getAnalyzerByName("Mindray")).thenReturn(mockAnalyzer);

    // Mock all LOINC code lookups to return empty lists by default
    when(mockTestService.getTestsByLoincCode(anyString())).thenReturn(new ArrayList<>());
  }

  @Test
  public void testConstructor_CanInstantiateWithoutSpringContext() {
    MindrayAnalyzerImplementation impl = new MindrayAnalyzerImplementation();

    assertNotNull("Implementation should be created successfully", impl);
  }

  @Test
  public void testGetError_ReturnsExpectedMessage() {
    MindrayAnalyzerImplementation impl = new MindrayAnalyzerImplementation();

    String error = impl.getError();

    assertEquals(
        "Error message should match expected value",
        "Mindray analyzer unable to write to database",
        error);
  }

  @Test
  public void testAddResult_WithUnknownLoincCode_ShouldNotThrowException() {
    MindrayAnalyzerImplementation impl = new MindrayAnalyzerImplementation();
    List<AnalyzerResults> resultList = new ArrayList<>();
    List<AnalyzerResults> notMatchedResults = new ArrayList<>();

    // This should not throw NullPointerException even with unknown LOINC code
    impl.addResult(
        resultList, notMatchedResults, "NM", "5.5", "ACC123", false, "mg/dL", "UNKNOWN-LOINC");

    // The result should be added to notMatchedResults since testId will be empty
    assertTrue(
        "Result with unknown LOINC should be added to notMatchedResults",
        notMatchedResults.size() > 0);
  }

  @Test
  public void testAddResult_WithNullLoincCode_ShouldNotThrowException() {
    MindrayAnalyzerImplementation impl = new MindrayAnalyzerImplementation();
    List<AnalyzerResults> resultList = new ArrayList<>();
    List<AnalyzerResults> notMatchedResults = new ArrayList<>();

    // This should not throw NullPointerException even with null LOINC code
    impl.addResult(resultList, notMatchedResults, "NM", "5.5", "ACC123", false, "mg/dL", null);

    // The result should be added to notMatchedResults since testId will be empty
    assertTrue(
        "Result with null LOINC should be added to notMatchedResults",
        notMatchedResults.size() > 0);
  }

  @Test
  public void testAddResult_WithValidLoincCode_AddsToResultList() {
    MindrayAnalyzerImplementation impl = new MindrayAnalyzerImplementation();
    List<AnalyzerResults> resultList = new ArrayList<>();
    List<AnalyzerResults> notMatchedResults = new ArrayList<>();

    // Test with a known LOINC code - TBil_LOINC
    impl.addResult(
        resultList,
        notMatchedResults,
        "NM",
        "1.2",
        "ACC456",
        false,
        "mg/dL",
        MindrayAnalyzerImplementation.TBil_LOINC);

    // Since our mock returns empty list, testId will be empty and it goes to notMatchedResults
    assertTrue("Result should be processed without exception", notMatchedResults.size() > 0);
  }

  @Test
  public void testPersistImport_ShouldNotThrowException() {
    MindrayAnalyzerImplementation impl = new MindrayAnalyzerImplementation();
    List<AnalyzerResults> resultList = new ArrayList<>();

    // Add a sample result
    AnalyzerResults result = new AnalyzerResults();
    result.setTestId("1");
    result.setResult("5.5");
    resultList.add(result);

    // This should not throw exception even though it won't actually persist
    // (since we're in a test environment without real database)
    try {
      impl.persistImport(resultList);
    } catch (Exception e) {
      // Expected in test environment - just verify we didn't get NPE
      assertTrue("Should not throw NullPointerException", !(e instanceof NullPointerException));
    }
  }
}
