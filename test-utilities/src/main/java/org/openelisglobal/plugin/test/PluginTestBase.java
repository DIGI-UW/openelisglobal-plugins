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
 * <p>Copyright (C) ITECH, University of Washington, Seattle WA. All Rights Reserved.
 */
package org.openelisglobal.plugin.test;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import org.junit.After;
import org.junit.Before;
import org.mockito.MockedStatic;
import org.openelisglobal.spring.util.SpringContext;

/**
 * Base test class for plugin tests that require SpringContext mocking.
 *
 * <p>This class provides a reusable test infrastructure for plugins that extend
 * AnalyzerLineInserter or otherwise depend on SpringContext.getBean() calls.
 *
 * <p>Usage in plugin tests:
 *
 * <pre>
 * public class MyPluginTest extends PluginTestBase {
 *   &#64;Test
 *   public void testParsing() {
 *     MyPluginLineInserter inserter = new MyPluginLineInserter();
 *     // Test parsing logic without SpringContext errors
 *   }
 * }
 * </pre>
 *
 * <p>The base class automatically:
 *
 * <ul>
 *   <li>Mocks SpringContext.getBean() to return null (safe for parsing tests)
 *   <li>Sets up and tears down mocks before/after each test
 *   <li>Prevents NullPointerException from SpringContext.factory being null
 * </ul>
 *
 * <p>For tests that require specific service beans, override setupMocks() and configure additional
 * mocking behavior.
 */
public abstract class PluginTestBase {

  private MockedStatic<SpringContext> springContextMock;

  /**
   * Set up SpringContext mocking before each test.
   *
   * <p>This prevents AnalyzerLineInserter constructor from failing due to missing Spring context.
   */
  @Before
  public void setUpPluginTestBase() {
    springContextMock = mockStatic(SpringContext.class);

    // Default: SpringContext.getBean() returns null
    // This is safe for parsing tests that don't actually call service methods
    when(SpringContext.getBean(any(Class.class))).thenReturn(null);

    // Allow subclasses to add additional mock configuration
    setupMocks();
  }

  /**
   * Override this method to configure additional mocks for your specific test needs.
   *
   * <p>Example:
   *
   * <pre>
   * &#64;Override
   * protected void setupMocks() {
   *   MyService mockService = mock(MyService.class);
   *   when(SpringContext.getBean(MyService.class)).thenReturn(mockService);
   * }
   * </pre>
   */
  protected void setupMocks() {
    // Default: no additional mocks
    // Subclasses can override to add specific service mocks
  }

  /**
   * Clean up SpringContext mocking after each test.
   *
   * <p>This ensures mocks don't leak between tests.
   */
  @After
  public void tearDownPluginTestBase() {
    if (springContextMock != null) {
      springContextMock.close();
    }
  }
}
