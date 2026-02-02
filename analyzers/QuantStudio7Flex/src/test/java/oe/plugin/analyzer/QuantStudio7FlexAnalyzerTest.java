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
 * Copyright (C) ITECH, University of Washington, Seattle WA. All Rights Reserved.
 */

package oe.plugin.analyzer;

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.List;
import org.junit.Test;
import org.openelisglobal.plugin.test.PluginTestBase;

/**
 * Unit tests for QuantStudio7FlexAnalyzer.
 *
 * <p>Tests the isTargetAnalyzer() detection logic to ensure correct identification of QS7 Flex
 * exports vs other formats (e.g., QS3).
 */
public class QuantStudio7FlexAnalyzerTest extends PluginTestBase {

  @Test
  public void testIsTargetAnalyzer_WithQS7FlexInstrumentType_ReturnsTrue() {
    QuantStudio7FlexAnalyzer analyzer = new QuantStudio7FlexAnalyzer();
    List<String> lines =
        Arrays.asList(
            "* Instrument Type = QuantStudio 7 Flex System", "Well\tWell Position\tSample Name");
    assertTrue(analyzer.isTargetAnalyzer(lines));
  }

  @Test
  public void testIsTargetAnalyzer_WithQS7Headers_ReturnsTrue() {
    QuantStudio7FlexAnalyzer analyzer = new QuantStudio7FlexAnalyzer();
    List<String> lines =
        Arrays.asList("Well\tWell Position\tSample Name\tTarget\tTask\tAmp Status\tCT");
    assertTrue(analyzer.isTargetAnalyzer(lines));
  }

  @Test
  public void testIsTargetAnalyzer_WithQS3Format_ReturnsFalse() {
    QuantStudio7FlexAnalyzer analyzer = new QuantStudio7FlexAnalyzer();
    // QS3 format lacks "Well Position", "Target", and "Amp Status"
    List<String> lines = Arrays.asList("Well\tSample Name\tCT\tCt Mean");
    assertFalse(analyzer.isTargetAnalyzer(lines));
  }

  @Test
  public void testIsTargetAnalyzer_WithEmptyLines_ReturnsFalse() {
    QuantStudio7FlexAnalyzer analyzer = new QuantStudio7FlexAnalyzer();
    List<String> lines = Arrays.asList("");
    assertFalse(analyzer.isTargetAnalyzer(lines));
  }
}
