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
package uw.edu.itech.AbbottArchitect;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.Assert;
import org.junit.Test;

public class AbbottArchitectAnalyzerLineInserterTest {

  @Test
  public void parseResults_extractsObxValues() throws Exception {
    String raw = Files.readString(
        Path.of("../../../src/test/resources/testdata/hl7/abbott-architect-result.hl7"),
        StandardCharsets.UTF_8);

    AbbottArchitectAnalyzerLineInserter inserter = new AbbottArchitectAnalyzerLineInserter();
    List<AbbottArchitectAnalyzerLineInserter.ParsedResult> results = inserter.parseResults(raw);

    Assert.assertEquals("Expected two OBX results", 2, results.size());

    AbbottArchitectAnalyzerLineInserter.ParsedResult hiv = results.get(0);
    Assert.assertEquals("HIV", hiv.getTestCode());
    Assert.assertEquals("NEGATIVE", hiv.getValue());
    Assert.assertEquals("", hiv.getUnits());

    AbbottArchitectAnalyzerLineInserter.ParsedResult hbsag = results.get(1);
    Assert.assertEquals("HBSAG", hbsag.getTestCode());
    Assert.assertEquals("POSITIVE", hbsag.getValue());
    Assert.assertEquals("", hbsag.getUnits());
  }
}
