package org.openelisglobal.plugins.analyzer.genericfile;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Collections;
import java.util.List;
import org.junit.Test;

public class GenericFileAnalyzerTest {

  @Test
  public void testIsGenericPlugin_ReturnsTrue() {
    GenericFileAnalyzer analyzer = new GenericFileAnalyzer();
    assertTrue(analyzer.isGenericPlugin());
  }

  @Test
  public void testIsTargetAnalyzer_WithDelimitedLine_ReturnsTrue() {
    GenericFileAnalyzer analyzer = new GenericFileAnalyzer();
    assertTrue(analyzer.isTargetAnalyzer(List.of("SAMPLE-1\tVL\t35.0")));
  }

  @Test
  public void testIsTargetAnalyzer_WithEmptyLines_ReturnsFalse() {
    GenericFileAnalyzer analyzer = new GenericFileAnalyzer();
    assertFalse(analyzer.isTargetAnalyzer(Collections.emptyList()));
  }

  @Test
  public void testGetAnalyzerLineInserter_ReturnsGenericFileLineInserter() {
    GenericFileAnalyzer analyzer = new GenericFileAnalyzer();
    assertNotNull(analyzer.getAnalyzerLineInserter());
    assertTrue(analyzer.getAnalyzerLineInserter() instanceof GenericFileLineInserter);
  }
}
