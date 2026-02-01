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
 * Copyright (C) ITECH, University of Washington, Seattle WA.  All Rights Reserved.
 */

package oe.plugin.analyzer;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.junit.Test;
import org.openelisglobal.plugin.test.PluginTestBase;

public class TaqMan96VLAnalyzerImplementationTest extends PluginTestBase {

  @Test
  public void testConstructor_CanInstantiateWithoutSpringContext() {
    TaqMan96VLAnalyzerImplementation impl = new TaqMan96VLAnalyzerImplementation();

    assertNotNull("Implementation should be created successfully", impl);
  }

  @Test
  public void testGetError_InitiallyNull() {
    TaqMan96VLAnalyzerImplementation impl = new TaqMan96VLAnalyzerImplementation();

    String error = impl.getError();

    assertNull("Error should be null initially", error);
  }
}
