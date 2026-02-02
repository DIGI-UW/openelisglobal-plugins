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

import org.junit.Test;
import org.openelisglobal.plugin.test.PluginTestBase;

/**
 * Unit tests for QuantStudio7FlexAnalyzerImplementation.
 *
 * <p>Tests the line inserter's header detection and error message functionality using lazy
 * initialization pattern (no Spring context required).
 */
public class QuantStudio7FlexAnalyzerImplementationTest extends PluginTestBase {

  @Test
  public void testConstructor_CanInstantiateWithoutSpringContext() {
    QuantStudio7FlexAnalyzerImplementation impl = new QuantStudio7FlexAnalyzerImplementation();
    assertNotNull("Implementation should be created successfully", impl);
  }

  @Test
  public void testGetError_ReturnsExpectedMessage() {
    QuantStudio7FlexAnalyzerImplementation impl = new QuantStudio7FlexAnalyzerImplementation();
    String error = impl.getError();
    assertEquals("QuantStudio 7 Flex analyzer unable to write to database", error);
  }

  @Test
  public void testIsColumnHeaderRow_WithWellPositionHeader_ReturnsTrue() {
    QuantStudio7FlexAnalyzerImplementation impl = new QuantStudio7FlexAnalyzerImplementation();
    assertTrue(impl.isColumnHeaderRow("Well\tWell Position\tSample Name\tTarget\tAmp Status\tCT"));
  }

  @Test
  public void testIsColumnHeaderRow_WithWellAndTarget_ReturnsTrue() {
    QuantStudio7FlexAnalyzerImplementation impl = new QuantStudio7FlexAnalyzerImplementation();
    assertTrue(impl.isColumnHeaderRow("Well\tTarget\tSample Name"));
  }

  @Test
  public void testIsColumnHeaderRow_WithoutQS7Markers_ReturnsFalse() {
    QuantStudio7FlexAnalyzerImplementation impl = new QuantStudio7FlexAnalyzerImplementation();
    // QS3 format - no "Well Position" or "Target"
    assertFalse(impl.isColumnHeaderRow("Well\tSample Name\tCT"));
  }

  @Test
  public void testIsColumnHeaderRow_WithEmptyLine_ReturnsFalse() {
    QuantStudio7FlexAnalyzerImplementation impl = new QuantStudio7FlexAnalyzerImplementation();
    assertFalse(impl.isColumnHeaderRow(""));
  }
}
