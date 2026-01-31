package uw.edu.itech.FluoroCyclerXT;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class FluoroCyclerXTAnalyzerLineInserterTest {

  @Test
  public void parseLine_WithTabDelimitedLine_ReturnsRecord() {
    FluoroCyclerXTAnalyzerLineInserter inserter = new FluoroCyclerXTAnalyzerLineInserter();

    FluoroCyclerXTAnalyzerLineInserter.FluoroCyclerRecord record =
        inserter.parseLine("SAMPLE-001\tDetected\tPositive\tA01");

    assertNotNull(record);
    assertEquals("SAMPLE-001", record.sampleId);
    assertEquals("Detected", record.result);
    assertEquals("Positive", record.interpretation);
    assertEquals("A01", record.position);
  }

  @Test
  public void parseLine_WithSemicolonDelimitedLine_ReturnsRecord() {
    FluoroCyclerXTAnalyzerLineInserter inserter = new FluoroCyclerXTAnalyzerLineInserter();

    FluoroCyclerXTAnalyzerLineInserter.FluoroCyclerRecord record =
        inserter.parseLine("SAMPLE-002;Not Detected;Negative;B05");

    assertNotNull(record);
    assertEquals("SAMPLE-002", record.sampleId);
    assertEquals("Not Detected", record.result);
    assertEquals("Negative", record.interpretation);
    assertEquals("B05", record.position);
  }

  @Test
  public void parseLine_WithMissingSampleId_ReturnsNull() {
    FluoroCyclerXTAnalyzerLineInserter inserter = new FluoroCyclerXTAnalyzerLineInserter();

    FluoroCyclerXTAnalyzerLineInserter.FluoroCyclerRecord record =
        inserter.parseLine("\tDetected\tPositive\tA01");

    assertNull(record);
  }

  @Test
  public void isHeaderLine_WithColumnNames_ReturnsTrue() {
    FluoroCyclerXTAnalyzerLineInserter inserter = new FluoroCyclerXTAnalyzerLineInserter();

    assertTrue(inserter.isHeaderLine("Position;Sample ID;Result;Interpretation"));
  }
}
