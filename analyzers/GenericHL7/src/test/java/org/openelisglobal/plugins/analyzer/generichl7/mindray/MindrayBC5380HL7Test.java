/**
 * Integration test for Mindray BC-5380 HL7 (hematology/CBC) via GenericHL7 plugin.
 *
 * <p>Task Reference: T124 [M5] Mindray BC-5380 HL7 integration. Feature:
 * 011-madagascar-analyzer-integration
 */
package org.openelisglobal.plugins.analyzer.generichl7.mindray;

import org.junit.Test;

public class MindrayBC5380HL7Test extends AbstractMindrayHL7Test {

  @Override
  protected String getAnalyzerName() {
    return "Mindray BC-5380";
  }

  @Override
  protected String getIdentifierPattern() {
    return "MINDRAY.*BC.?5380|BC5380";
  }

  @Override
  protected String getFixturePath() {
    return "testdata/hl7/mindray/bc5380-cbc-result.hl7";
  }

  @Test
  public void mindrayBc5380Hl7Message_resultsStored() throws Exception {
    verifyHl7MessageResultsStored();
  }
}
