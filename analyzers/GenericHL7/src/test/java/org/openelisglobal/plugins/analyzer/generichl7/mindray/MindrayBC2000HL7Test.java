/**
 * Integration test for Mindray BC2000 HL7 (hematology/CBC) via GenericHL7 plugin.
 *
 * <p>Task Reference: T211 [M14] Mindray BC2000 HL7 integration. Feature:
 * 011-madagascar-analyzer-integration
 */
package org.openelisglobal.plugins.analyzer.generichl7.mindray;

import org.junit.Test;

public class MindrayBC2000HL7Test extends AbstractMindrayHL7Test {

  @Override
  protected String getAnalyzerName() {
    return "Mindray BC2000";
  }

  @Override
  protected String getIdentifierPattern() {
    return "MINDRAY.*BC.?2000|BC2000";
  }

  @Override
  protected String getFixturePath() {
    return "testdata/hl7/mindray/bc2000-cbc-result.hl7";
  }

  @Test
  public void mindrayBc2000Hl7Message_resultsStored() throws Exception {
    verifyHl7MessageResultsStored();
  }
}
