/**
 * Integration test for Mindray BS-200 HL7 (chemistry) via GenericHL7 plugin.
 *
 * <p>OGC-326: BS-series HL7 validation using a synthetic chemistry fixture aligned to the current
 * GenericHL7 sender matcher.
 *
 * <p>Feature: 013-hjra-hl7-stream-alignment (M3)
 */
package org.openelisglobal.plugins.analyzer.generichl7.mindray;

import org.junit.Test;

public class MindrayBS200HL7Test extends AbstractMindrayHL7Test {

  @Override
  protected String getAnalyzerName() {
    return "Mindray BS-200";
  }

  @Override
  protected String getIdentifierPattern() {
    return "MINDRAY.*BS.?200|BS.?200|BS200";
  }

  @Override
  protected String getFixturePath() {
    return "testdata/hl7/mindray/bs200-chemistry-result.hl7";
  }

  @Override
  protected String[][] getTestMappings() {
    return new String[][] {{"CREA", "1"}, {"ALT", "2"}, {"AST", "1"}};
  }

  @Test
  public void mindrayBs200Hl7Message_resultsStored() throws Exception {
    verifyHl7MessageResultsStored();
  }
}
