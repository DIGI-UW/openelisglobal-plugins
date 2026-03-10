/**
 * Integration test for Mindray BS-300 HL7 (chemistry) via GenericHL7 plugin.
 *
 * <p>OGC-326: BS-series HL7 validation using a synthetic chemistry fixture aligned to the current
 * GenericHL7 sender matcher. BS-300 equivalence remains a validation task, not a settled fact.
 *
 * <p>Feature: 013-hjra-hl7-stream-alignment (M3)
 */
package org.openelisglobal.plugins.analyzer.generichl7.mindray;

import org.junit.Test;

public class MindrayBS300HL7Test extends AbstractMindrayHL7Test {

  @Override
  protected String getAnalyzerName() {
    return "Mindray BS-300";
  }

  @Override
  protected String getIdentifierPattern() {
    return "MINDRAY.*BS.?300|BS.?300|BS300";
  }

  @Override
  protected String getFixturePath() {
    return "testdata/hl7/mindray/bs300-chemistry-result.hl7";
  }

  @Override
  protected String[][] getTestMappings() {
    return new String[][] {{"CREA", "1"}, {"ALT", "2"}, {"AST", "1"}};
  }

  @Test
  public void mindrayBs300Hl7Message_resultsStored() throws Exception {
    verifyHl7MessageResultsStored();
  }
}
