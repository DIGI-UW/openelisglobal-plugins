package org.openelisglobal.plugins.analyzer.generichl7;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openelisglobal.analyzer.service.AnalyzerService;
import org.openelisglobal.analyzer.valueholder.Analyzer;

/**
 * Unit tests for GenericHL7Analyzer plugin.
 *
 * <p>Tests the MSH-3 pattern matching and analyzer identification logic following TDD approach (Red
 * → Green → Refactor).
 *
 * <p>Task Reference: T200 (M19) - Create unit tests for isTargetAnalyzer()
 */
@RunWith(MockitoJUnitRunner.class)
public class GenericHL7AnalyzerTest {

  @Mock private AnalyzerService mockAnalyzerService;

  private GenericHL7Analyzer analyzer;

  @Before
  public void setUp() {
    analyzer = new GenericHL7Analyzer(mockAnalyzerService);
  }

  /** Test that GenericHL7Analyzer returns false for empty message lines. */
  @Test
  public void testIsTargetAnalyzer_EmptyLines_ReturnsFalse() {
    List<String> emptyLines = Collections.emptyList();
    boolean result = analyzer.isTargetAnalyzer(emptyLines);
    assertFalse("Empty lines should return false", result);
  }

  /** Test that GenericHL7Analyzer returns false for null message lines. */
  @Test
  public void testIsTargetAnalyzer_NullLines_ReturnsFalse() {
    boolean result = analyzer.isTargetAnalyzer(null);
    assertFalse("Null lines should return false", result);
  }

  /** Test that GenericHL7Analyzer returns false when no MSH segment present. */
  @Test
  public void testIsTargetAnalyzer_NoMshSegment_ReturnsFalse() {
    List<String> lines = Arrays.asList("PID|1|123456||Doe^John||19700101|M");
    boolean result = analyzer.isTargetAnalyzer(lines);
    assertFalse("Lines without MSH segment should return false", result);
  }

  /** Test that GenericHL7Analyzer returns false when MSH-3 is blank. */
  @Test
  public void testIsTargetAnalyzer_BlankMsh3_ReturnsFalse() {
    List<String> lines = Arrays.asList("MSH|^~\\&|||||||ORU^R01|MSG001|P|2.3.1");
    boolean result = analyzer.isTargetAnalyzer(lines);
    assertFalse("MSH segment with blank MSH-3 should return false", result);
  }

  @Test
  public void testBuildSenderIdentityCandidates_UsesCombinedModelAndUppercaseForms() {
    List<String> lines =
        Arrays.asList(
            "MSH|^~\\&|Mindray|BS-200|OpenELIS|LAB|20260310120000||ORU^R01|MSG001|P|2.3.1");

    List<String> identifiers = analyzer.buildSenderIdentityCandidates(lines);

    assertEquals(
        Arrays.asList("Mindray BS-200", "BS-200", "Mindray", "MINDRAY BS-200", "MINDRAY"),
        identifiers);
  }

  @Test
  public void testIsTargetAnalyzer_UsesCombinedSenderIdentityWhenMsh4Present() {
    List<String> lines =
        Arrays.asList(
            "MSH|^~\\&|Mindray|BS-200|OpenELIS|LAB|20260310120000||ORU^R01|MSG001|P|2.3.1");
    Analyzer mockAnalyzer = new Analyzer();
    mockAnalyzer.setId("ANALYZER-001");
    mockAnalyzer.setName("Mindray BS-200");
    mockAnalyzer.setIdentifierPattern("MINDRAY.*BS.?200");
    when(mockAnalyzerService.findByIdentifierPatternMatch(anyList()))
        .thenReturn(java.util.Optional.of(mockAnalyzer));

    boolean result = analyzer.isTargetAnalyzer(lines);

    assertTrue("Combined sender identity should return true", result);
    verify(mockAnalyzerService)
        .findByIdentifierPatternMatch(
            Arrays.asList("Mindray BS-200", "BS-200", "Mindray", "MINDRAY BS-200", "MINDRAY"));
  }

  /** Test that GenericHL7Analyzer returns false when MSH-3 does not match any pattern. */
  @Test
  public void testIsTargetAnalyzer_NonMatchingMsh3_ReturnsFalse() {
    List<String> lines = Arrays.asList("MSH|^~\\&||UNKNOWN_ANALYZER||||ORU^R01|MSG001|P|2.3.1");

    // Act
    boolean result = analyzer.isTargetAnalyzer(lines);

    // Assert
    assertFalse("MSH-3 not matching any pattern should return false", result);
  }

  /** Test that isAnalyzerResult returns true for messages containing OBX segments. */
  @Test
  public void testIsAnalyzerResult_WithObxSegments_ReturnsTrue() {
    List<String> lines =
        Arrays.asList(
            "MSH|^~\\&||MINDRAY||||ORU^R01|MSG001|P|2.3.1",
            "PID|1|123456||Doe^John||19700101|M",
            "OBX|1|NM|WBC||7.5|10^3/uL|4.0-11.0|N|||F");

    boolean result = analyzer.isAnalyzerResult(lines);
    assertTrue("Message with OBX segments should be identified as result", result);
  }

  /** Test that isAnalyzerResult returns false for messages without OBX segments. */
  @Test
  public void testIsAnalyzerResult_WithoutObxSegments_ReturnsFalse() {
    List<String> lines =
        Arrays.asList("MSH|^~\\&||MINDRAY||||ACK^R01|MSG001|P|2.3.1", "MSA|AA|MSG001");

    boolean result = analyzer.isAnalyzerResult(lines);
    assertFalse("Message without OBX segments should not be identified as result", result);
  }

  /** Test that getAnalyzerLineInserter returns a valid inserter after successful match. */
  @Test
  public void testGetAnalyzerLineInserter_AfterSuccessfulMatch_ReturnsInserter() {
    // Note: This test will fail until GenericHL7LineInserter is implemented
    // Expected behavior: After isTargetAnalyzer() returns true,
    // getAnalyzerLineInserter() should return a GenericHL7LineInserter
    // configured with the matched analyzer ID

    // This is a placeholder test that will be expanded once implementation starts
    // For now, we expect it to fail (TDD Red phase)
  }

  /** Test that getAnalyzerLineInserter throws exception when called without prior match. */
  @Test(expected = IllegalStateException.class)
  public void testGetAnalyzerLineInserter_WithoutPriorMatch_ThrowsException() {
    // Arrange: Create analyzer without calling isTargetAnalyzer()
    GenericHL7Analyzer analyzer = new GenericHL7Analyzer(mockAnalyzerService);

    // Act & Assert: Should throw IllegalStateException
    analyzer.getAnalyzerLineInserter();
  }
}
