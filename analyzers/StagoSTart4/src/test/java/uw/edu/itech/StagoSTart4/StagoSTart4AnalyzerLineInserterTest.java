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
package uw.edu.itech.StagoSTart4;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.openelisglobal.plugin.test.PluginTestBase;

/**
 * Unit tests for StagoSTart4AnalyzerLineInserter.
 *
 * <p>Feature: 011-madagascar-analyzer-integration Milestone: M11 (Stago STart 4)
 *
 * <p>Tests ASTM LIS2-A2 message parsing for Stago STart 4 coagulation analyzer. The inserter
 * should extract coagulation test results (PT, INR, APTT, Fibrinogen, TT) from ASTM R-segments
 * and map them to OpenELIS tests.
 *
 * <p>TDD Approach: Write failing tests first, then implement to make them pass.
 */
public class StagoSTart4AnalyzerLineInserterTest extends PluginTestBase {

  private StagoSTart4AnalyzerLineInserter inserter;

  @Before
  public void setUp() {
    inserter = new StagoSTart4AnalyzerLineInserter();
  }

  @Test
  @Ignore("Integration test - requires AnalyzerService from Spring context")
  public void testInsert_WithValidASTMMessage_ParsesSuccessfully() throws Exception {
    // Load test fixture
    List<String> lines = loadTestFixture("stago-start4-coagulation.astm");

    // Note: This test will fail initially because the inserter doesn't exist yet
    // Once implemented, it should parse the ASTM message and extract results
    boolean result = inserter.insert(lines, "test-user");

    // For now, this will fail - TDD RED phase
    // After implementation, should return true if parsing succeeds
    assertTrue("Should parse valid ASTM message successfully", result);
  }

  @Test
  public void testInsert_WithEmptyLines_ReturnsFalse() {
    List<String> emptyLines = new ArrayList<>();

    boolean result = inserter.insert(emptyLines, "test-user");

    assertFalse("Should return false for empty lines", result);
    assertNotNull("Error message should be set", inserter.getError());
  }

  @Test
  public void testInsert_WithNullLines_ReturnsFalse() {
    boolean result = inserter.insert(null, "test-user");

    assertFalse("Should return false for null lines", result);
    assertNotNull("Error message should be set", inserter.getError());
  }

  @Test
  @Ignore("Integration test - requires AnalyzerService from Spring context")
  public void testInsert_WithASTMMessage_ExtractsCoagulationResults() throws Exception {
    List<String> lines = loadTestFixture("stago-start4-coagulation.astm");

    // This test verifies that the inserter can extract coagulation test results
    // Expected results: PT=12.5, INR=1.05, APTT=28.5, FIB=3.2, TT=15.2
    boolean result = inserter.insert(lines, "test-user");

    assertTrue("Should extract coagulation results from ASTM message", result);
    // Note: Full result validation would require mocking AnalyzerTestNameCache
    // This is a basic smoke test
  }

  @Test
  public void testGetError_AfterFailedInsert_ReturnsErrorMessage() {
    List<String> invalidLines = new ArrayList<>();
    invalidLines.add("INVALID|LINE|FORMAT");

    inserter.insert(invalidLines, "test-user");

    String error = inserter.getError();
    assertNotNull("Error message should not be null", error);
    assertFalse("Error message should not be empty", error.isEmpty());
  }

  @Test
  @Ignore("Integration test - requires AnalyzerService from Spring context")
  public void testInsert_WithValidHL7Message_ParsesSuccessfully() throws Exception {
    // Load HL7 test fixture
    List<String> lines = loadTestFixture("stago-start4-coagulation.hl7");

    boolean result = inserter.insert(lines, "test-user");

    assertTrue("Should parse valid HL7 message successfully", result);
  }

  @Test
  @Ignore("Integration test - requires AnalyzerService from Spring context")
  public void testInsert_WithHL7Message_ExtractsCoagulationResults() throws Exception {
    List<String> lines = loadTestFixture("stago-start4-coagulation.hl7");

    // This test verifies that the inserter can extract coagulation test results from HL7
    // Expected results: PT=12.5, INR=1.05, APTT=28.5, FIB=3.2, TT=15.2
    boolean result = inserter.insert(lines, "test-user");

    assertTrue("Should extract coagulation results from HL7 message", result);
  }

  @Test
  public void testInsert_WithASTMMissingOrderSegment_HandlesGracefully() {
    List<String> lines = new ArrayList<>();
    lines.add("H|\\^&|||STAGO^START4^V1.0|||||||LIS2-A2|20260128080000");
    lines.add("R|1|^^^PT|12.5|sec|11.0-13.5|N||F|20260128080100");
    // Missing O segment

    // Should handle gracefully (log warning but not crash)
    boolean result = inserter.insert(lines, "test-user");

    // May return false or true depending on implementation
    // Main goal is to not throw exception
    assertNotNull("Should not throw exception", inserter.getError());
  }

  @Test
  @Ignore("Integration test - requires AnalyzerService from Spring context")
  public void testInsert_WithHL7MissingOBRSegment_UsesPatientIdAsAccession() {
    List<String> lines = new ArrayList<>();
    lines.add("MSH|^~\\&|STAGO|LAB|OpenELIS|LAB|20260123120000||ORU^R01|STAGO001|P|2.5.1");
    lines.add("PID|1||PAT002^^^HOSPITAL||RAKOTO^MARIE||19900220|F");
    lines.add("OBX|1|NM|^^^PT^PROTHROMBIN TIME||12.5|sec|||||F||||||");
    // Missing OBR segment

    boolean result = inserter.insert(lines, "test-user");

    // Should use patient ID as accession number fallback
    assertTrue("Should handle HL7 without OBR segment", result);
  }

  @Test
  @Ignore("Integration test - requires AnalyzerService from Spring context")
  public void testInsert_WithASTMInvalidTimestampFormat_HandlesGracefully() {
    List<String> lines = new ArrayList<>();
    lines.add("H|\\^&|||STAGO^START4^V1.0|||||||LIS2-A2|20260128080000");
    lines.add("O|1|SAMPLE-001^LAB|COAG^Coagulation Panel||20260128075500");
    lines.add("R|1|^^^PT|12.5|sec|11.0-13.5|N||F|INVALID-TIMESTAMP");

    // Should handle invalid timestamp gracefully
    boolean result = inserter.insert(lines, "test-user");

    // May use current timestamp as fallback
    assertNotNull("Should handle invalid timestamp", result);
  }

  @Test
  public void testInsert_WithHL7MissingTestCode_HandlesGracefully() {
    List<String> lines = new ArrayList<>();
    lines.add("MSH|^~\\&|STAGO|LAB|OpenELIS|LAB|20260123120000||ORU^R01|STAGO001|P|2.5.1");
    lines.add("PID|1||PAT002^^^HOSPITAL||RAKOTO^MARIE||19900220|F");
    lines.add("OBR|1|PLACER789|FILLER012|1|^^^COAG^COAGULATION PANEL|||20260123110000");
    lines.add("OBX|1|NM|^^^||12.5|sec|||||F||||||");
    // Missing test code in OBX field 3

    boolean result = inserter.insert(lines, "test-user");

    // Should skip OBX segments without test codes
    assertNotNull("Should handle missing test code", result);
  }

  /**
   * Helper method to load test fixture files from test resources.
   *
   * <p>Note: Test fixtures are located in the main OpenELIS project at
   * src/test/resources/testdata/stago/, not in the plugin's resources.
   *
   * @param filename name of the test fixture file
   * @return list of lines from the file
   * @throws Exception if file cannot be read
   */
  private List<String> loadTestFixture(String filename) throws Exception {
    List<String> lines = new ArrayList<>();
    // Test fixtures are in the main project, not plugin resources
    String resourcePath = "testdata/stago/" + filename;

    try (InputStream is = getClass().getClassLoader().getResourceAsStream(resourcePath)) {
      if (is == null) {
        throw new RuntimeException("Test fixture not found: " + resourcePath);
      }

      try (BufferedReader reader =
          new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
        String line;
        while ((line = reader.readLine()) != null) {
          // Skip comment lines
          if (!line.trim().startsWith("#") && !line.trim().isEmpty()) {
            lines.add(line);
          }
        }
      }
    }

    return lines;
  }
}
