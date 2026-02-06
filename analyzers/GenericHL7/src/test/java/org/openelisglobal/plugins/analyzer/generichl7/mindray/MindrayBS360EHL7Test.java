/**
 * Integration test for Mindray BS-360E HL7 (chemistry) via GenericHL7 plugin.
 *
 * <p>Task Reference: T125 [M5] Mindray BS-360E HL7 integration. Feature:
 * 011-madagascar-analyzer-integration
 */
package org.openelisglobal.plugins.analyzer.generichl7.mindray;

import org.junit.Test;

public class MindrayBS360EHL7Test extends AbstractMindrayHL7Test {

  @Override
  protected String getAnalyzerName() {
    return "Mindray BS-360E";
  }

  @Override
  protected String getIdentifierPattern() {
    return "MINDRAY.*BS.?360E|BS360E";
  }

  @Override
  protected String getFixturePath() {
    return "testdata/hl7/mindray/bs360e-chemistry-result.hl7";
  }

  @Test
  public void mindrayBs360eHl7Message_resultsStored() throws Exception {
    verifyHl7MessageResultsStored();
  }
}
