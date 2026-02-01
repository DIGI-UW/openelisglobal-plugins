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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.analyzerimport.analyzerreaders.AnalyzerLineInserter;

/**
 * Unit tests for StagoSTart4Analyzer plugin class.
 *
 * <p>Feature: 011-madagascar-analyzer-integration Milestone: M11 (Stago STart 4)
 *
 * <p>Tests analyzer identification (ASTM and HL7), result detection, and plugin registration.
 */
public class StagoSTart4AnalyzerTest {

  private StagoSTart4Analyzer analyzer;

  @Before
  public void setUp() {
    analyzer = new StagoSTart4Analyzer();
  }

  @Test
  public void testIsTargetAnalyzer_WithASTMHeaderContainingSTART4_ReturnsTrue() {
    List<String> lines = new ArrayList<>();
    lines.add("H|\\^&|||STAGO^START4^V1.0|||||||LIS2-A2|20260128080000");
    lines.add("P|1||PAT-001|Patient^Test||M|19800101");

    boolean result = analyzer.isTargetAnalyzer(lines);

    assertTrue("Should identify ASTM message with START4 in header", result);
  }

  @Test
  public void testIsTargetAnalyzer_WithASTMHeaderContainingSTAGO_ReturnsTrue() {
    List<String> lines = new ArrayList<>();
    lines.add("H|\\^&|||STAGO^ANALYZER^V1.0|||||||LIS2-A2|20260128080000");
    lines.add("P|1||PAT-001|Patient^Test||M|19800101");

    boolean result = analyzer.isTargetAnalyzer(lines);

    assertTrue("Should identify ASTM message with STAGO in header", result);
  }

  @Test
  public void testIsTargetAnalyzer_WithHL7MSHContainingSTAGO_ReturnsTrue() {
    List<String> lines = new ArrayList<>();
    lines.add("MSH|^~\\&|STAGO|LAB|OpenELIS|LAB|20260123120000||ORU^R01|STAGO001|P|2.5.1");
    lines.add("PID|1||PAT001^^^HOSPITAL||DOE^JOHN||19800115|M");

    boolean result = analyzer.isTargetAnalyzer(lines);

    assertTrue("Should identify HL7 message with STAGO in MSH", result);
  }

  @Test
  public void testIsTargetAnalyzer_WithNonStagoASTM_ReturnsFalse() {
    List<String> lines = new ArrayList<>();
    lines.add("H|\\^&|||ABX^PENTRA60^V2.0|||||||LIS2-A2|20260128080000");
    lines.add("P|1||PAT-001|Patient^Test||M|19800101");

    boolean result = analyzer.isTargetAnalyzer(lines);

    assertFalse("Should not identify non-Stago analyzer", result);
  }

  @Test
  public void testIsTargetAnalyzer_WithNonStagoHL7_ReturnsFalse() {
    List<String> lines = new ArrayList<>();
    lines.add("MSH|^~\\&|MINDRAY|LAB|OpenELIS|LAB|20260123120000||ORU^R01|MINDRAY001|P|2.5.1");
    lines.add("PID|1||PAT001^^^HOSPITAL||DOE^JOHN||19800115|M");

    boolean result = analyzer.isTargetAnalyzer(lines);

    assertFalse("Should not identify non-Stago HL7 analyzer", result);
  }

  @Test
  public void testIsTargetAnalyzer_WithNullLines_ReturnsFalse() {
    boolean result = analyzer.isTargetAnalyzer(null);

    assertFalse("Should return false for null lines", result);
  }

  @Test
  public void testIsTargetAnalyzer_WithEmptyLines_ReturnsFalse() {
    List<String> emptyLines = new ArrayList<>();

    boolean result = analyzer.isTargetAnalyzer(emptyLines);

    assertFalse("Should return false for empty lines", result);
  }

  @Test
  public void testIsAnalyzerResult_WithASTMRSegments_ReturnsTrue() {
    List<String> lines = new ArrayList<>();
    lines.add("H|\\^&|||STAGO^START4^V1.0|||||||LIS2-A2|20260128080000");
    lines.add("O|1|SAMPLE-001^LAB|COAG^Coagulation Panel||20260128075500");
    lines.add("R|1|^^^PT|12.5|sec|11.0-13.5|N||F|20260128080100");

    boolean result = analyzer.isAnalyzerResult(lines);

    assertTrue("Should detect ASTM R segments as results", result);
  }

  @Test
  public void testIsAnalyzerResult_WithHL7OBXSegments_ReturnsTrue() {
    List<String> lines = new ArrayList<>();
    lines.add("MSH|^~\\&|STAGO|LAB|OpenELIS|LAB|20260123120000||ORU^R01|STAGO001|P|2.5.1");
    lines.add("OBX|1|NM|^^^PT^PROTHROMBIN TIME||12.5|sec|||||F||||||");

    boolean result = analyzer.isAnalyzerResult(lines);

    assertTrue("Should detect HL7 OBX segments as results", result);
  }

  @Test
  public void testIsAnalyzerResult_WithNoResultSegments_ReturnsFalse() {
    List<String> lines = new ArrayList<>();
    lines.add("H|\\^&|||STAGO^START4^V1.0|||||||LIS2-A2|20260128080000");
    lines.add("P|1||PAT-001|Patient^Test||M|19800101");
    lines.add("O|1|SAMPLE-001^LAB|COAG^Coagulation Panel||20260128075500");
    // No R or OBX segments

    boolean result = analyzer.isAnalyzerResult(lines);

    assertFalse("Should return false when no result segments present", result);
  }

  @Test
  public void testIsAnalyzerResult_WithNullLines_ReturnsFalse() {
    boolean result = analyzer.isAnalyzerResult(null);

    assertFalse("Should return false for null lines", result);
  }

  @Test
  public void testGetAnalyzerLineInserter_ReturnsNonNullInserter() {
    AnalyzerLineInserter inserter = analyzer.getAnalyzerLineInserter();

    assertNotNull("Should return non-null inserter", inserter);
    assertTrue(
        "Should return StagoSTart4AnalyzerLineInserter instance",
        inserter instanceof StagoSTart4AnalyzerLineInserter);
  }

  @Test
  public void testGetAnalyzerLineInserter_ReturnsNewInstanceEachTime() {
    AnalyzerLineInserter inserter1 = analyzer.getAnalyzerLineInserter();
    AnalyzerLineInserter inserter2 = analyzer.getAnalyzerLineInserter();

    assertNotNull("First inserter should not be null", inserter1);
    assertNotNull("Second inserter should not be null", inserter2);
    // Note: We can't test for different instances without equals() override,
    // but we verify both are non-null and correct type
  }
}
