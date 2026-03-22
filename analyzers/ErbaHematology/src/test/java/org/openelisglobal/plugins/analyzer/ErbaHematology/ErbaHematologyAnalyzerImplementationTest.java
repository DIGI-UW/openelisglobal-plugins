package org.openelisglobal.plugins.analyzer.ErbaHematology;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import org.junit.Test;
import org.openelisglobal.analyzerresults.valueholder.AnalyzerResults;
import org.openelisglobal.plugin.test.PluginTestBase;
import org.openelisglobal.test.service.TestService;

/**
 * Tests for {@link ErbaHematologyAnalyzerLineInserter}.
 *
 * <p>Because the implementation uses lazy initialization, no Spring context is required at
 * construction time. Tests inject a mock {@link TestService} via the protected {@link
 * ErbaHematologyAnalyzerLineInserter#getTestService()} override, avoiding the need to bootstrap the
 * full application context.
 */
public class ErbaHematologyAnalyzerImplementationTest extends PluginTestBase {

  /**
   * Creates an implementation instance with a mock TestService injected. The mock returns an empty
   * list for every LOINC lookup by default, simulating "test not configured in this OpenELIS
   * instance".
   */
  private ErbaHematologyAnalyzerLineInserter instanceWithMock() {
    TestService mockTestService = mock(TestService.class);
    when(mockTestService.getTestsByLoincCode(anyString())).thenReturn(new ArrayList<>());

    return new ErbaHematologyAnalyzerLineInserter() {
      @Override
      protected TestService getTestService() {
        return mockTestService;
      }
    };
  }

  // Construction

  @Test
  public void testConstructor_DoesNotRequireSpringContext() {
    ErbaHematologyAnalyzerLineInserter impl = instanceWithMock();
    assertNotNull(impl);
  }

  // Contract methods

  @Test
  public void testGetError_ReturnsExpectedMessage() {
    ErbaHematologyAnalyzerLineInserter impl = instanceWithMock();
    assertEquals("ErbaHematology analyzer unable to write to database", impl.getError());
  }

  @Test
  public void testInsert_AlwaysReturnsFalse() {
    ErbaHematologyAnalyzerLineInserter impl = instanceWithMock();
    // Flat-file path is disabled; results arrive via the HL7 servlet only.
    assertEquals(false, impl.insert(new ArrayList<>(), "user1"));
  }

  // addResult routing

  @Test
  public void testAddResult_UnknownLoincCode_RoutesToNotMatched() {
    ErbaHematologyAnalyzerLineInserter impl = instanceWithMock();
    List<AnalyzerResults> matched = new ArrayList<>();
    List<AnalyzerResults> unmatched = new ArrayList<>();

    impl.addResult(matched, unmatched, "NM", "5.5", "ACC001", false, "10*3/uL", "UNKNOWN");

    assertEquals(0, matched.size());
    assertEquals(1, unmatched.size());
  }

  @Test
  public void testAddResult_NullLoincCode_RoutesToNotMatched() {
    ErbaHematologyAnalyzerLineInserter impl = instanceWithMock();
    List<AnalyzerResults> matched = new ArrayList<>();
    List<AnalyzerResults> unmatched = new ArrayList<>();

    impl.addResult(matched, unmatched, "NM", "5.5", "ACC002", false, "10*3/uL", null);

    assertEquals(0, matched.size());
    assertEquals(1, unmatched.size());
  }

  @Test
  public void testAddResult_KnownLoincCode_MockReturnsEmpty_RoutesToNotMatched() {
    // Even a known LOINC code routes to notMatched when the DB has no test
    // configured for it (mock returns empty list).
    ErbaHematologyAnalyzerLineInserter impl = instanceWithMock();
    List<AnalyzerResults> matched = new ArrayList<>();
    List<AnalyzerResults> unmatched = new ArrayList<>();

    impl.addResult(
        matched,
        unmatched,
        "NM",
        "8.67",
        "ACC003",
        false,
        "10*3/uL",
        ErbaHematologyAnalyzerLineInserter.WBC_LOINC);

    assertEquals(0, matched.size());
    assertEquals(1, unmatched.size());
  }

  // AnalyzerResults field values

  @Test
  public void testAddResult_ResultValueStoredCorrectly() {
    ErbaHematologyAnalyzerLineInserter impl = instanceWithMock();
    List<AnalyzerResults> matched = new ArrayList<>();
    List<AnalyzerResults> unmatched = new ArrayList<>();

    impl.addResult(
        matched,
        unmatched,
        "NM",
        "12.0",
        "ACC004",
        false,
        "g/dL",
        ErbaHematologyAnalyzerLineInserter.HGB_LOINC);

    AnalyzerResults result = unmatched.get(0);
    assertEquals("12.0", result.getResult());
    assertEquals("g/dL", result.getUnits());
    assertEquals("ACC004", result.getAccessionNumber());
    assertNull(result.getTestId());
  }

  @Test
  public void testAddResult_ControlFlag_PropagatedCorrectly() {
    ErbaHematologyAnalyzerLineInserter impl = instanceWithMock();
    List<AnalyzerResults> matched = new ArrayList<>();
    List<AnalyzerResults> unmatched = new ArrayList<>();

    // isControl = true simulates a QC message (OBR-13 non-empty per protocol)
    impl.addResult(
        matched,
        unmatched,
        "NM",
        "5.51",
        "QC-BATCH-01",
        true,
        "10*3/uL",
        ErbaHematologyAnalyzerLineInserter.WBC_LOINC);

    assertTrue(unmatched.get(0).getIsControl());
  }

  @Test
  public void testAddResult_CompleteDate_IsSet() {
    ErbaHematologyAnalyzerLineInserter impl = instanceWithMock();
    List<AnalyzerResults> matched = new ArrayList<>();
    List<AnalyzerResults> unmatched = new ArrayList<>();

    impl.addResult(
        matched,
        unmatched,
        "NM",
        "181",
        "ACC005",
        false,
        "10*3/uL",
        ErbaHematologyAnalyzerLineInserter.PLT_LOINC);

    assertNotNull(unmatched.get(0).getCompleteDate());
  }

  // Model-specific parameters

  @Test
  public void testAddResult_H360_GranPct_Handled() {
    ErbaHematologyAnalyzerLineInserter impl = instanceWithMock();
    List<AnalyzerResults> matched = new ArrayList<>();
    List<AnalyzerResults> unmatched = new ArrayList<>();

    impl.addResult(
        matched,
        unmatched,
        "NM",
        "74.8",
        "ACC006",
        false,
        "%",
        ErbaHematologyAnalyzerLineInserter.GRAN_PCT_LOINC);

    assertEquals(1, unmatched.size());
    assertEquals("74.8", unmatched.get(0).getResult());
  }

  @Test
  public void testAddResult_H360_MidAbs_Handled() {
    ErbaHematologyAnalyzerLineInserter impl = instanceWithMock();
    List<AnalyzerResults> matched = new ArrayList<>();
    List<AnalyzerResults> unmatched = new ArrayList<>();

    impl.addResult(
        matched,
        unmatched,
        "NM",
        "0.37",
        "ACC007",
        false,
        "10*3/uL",
        ErbaHematologyAnalyzerLineInserter.MID_ABS_LOINC);

    assertEquals(1, unmatched.size());
  }

  @Test
  public void testAddResult_Elite580_LicAbs_Handled() {
    ErbaHematologyAnalyzerLineInserter impl = instanceWithMock();
    List<AnalyzerResults> matched = new ArrayList<>();
    List<AnalyzerResults> unmatched = new ArrayList<>();

    impl.addResult(
        matched,
        unmatched,
        "NM",
        "0.00",
        "ACC008",
        false,
        "10*3/uL",
        ErbaHematologyAnalyzerLineInserter.LIC_ABS_LOINC);

    assertEquals(1, unmatched.size());
  }

  @Test
  public void testAddResult_PdwSd_And_PdwCv_AreIndependent() {
    // PDW is always sent as two separate OBX segments; both must be handled.
    ErbaHematologyAnalyzerLineInserter impl = instanceWithMock();
    List<AnalyzerResults> matched = new ArrayList<>();
    List<AnalyzerResults> unmatched = new ArrayList<>();

    impl.addResult(
        matched,
        unmatched,
        "NM",
        "19.8",
        "ACC009",
        false,
        "fL",
        ErbaHematologyAnalyzerLineInserter.PDW_SD_LOINC);
    impl.addResult(
        matched,
        unmatched,
        "NM",
        "18.5",
        "ACC009",
        false,
        "%",
        ErbaHematologyAnalyzerLineInserter.PDW_CV_LOINC);

    assertEquals(2, unmatched.size());
  }

  @Test
  public void testAddResult_MentzerIndex_Handled() {
    ErbaHematologyAnalyzerLineInserter impl = instanceWithMock();
    List<AnalyzerResults> matched = new ArrayList<>();
    List<AnalyzerResults> unmatched = new ArrayList<>();

    impl.addResult(
        matched,
        unmatched,
        "NM",
        "13.78",
        "ACC010",
        false,
        "",
        ErbaHematologyAnalyzerLineInserter.MENTZER_LOINC);

    assertEquals(1, unmatched.size());
    assertEquals("13.78", unmatched.get(0).getResult());
  }

  // IS-type metadata segments

  @Test
  public void testAddResult_MetadataSegment_TakeMode_RoutesToNotMatched() {
    // OBX segments like Take Mode (02001), Test Mode (02003) etc. should
    // silently land in notMatchedResults — they are never clinical results.
    ErbaHematologyAnalyzerLineInserter impl = instanceWithMock();
    List<AnalyzerResults> matched = new ArrayList<>();
    List<AnalyzerResults> unmatched = new ArrayList<>();

    impl.addResult(matched, unmatched, "IS", "O", "ACC011", false, "", "02001");

    assertEquals(0, matched.size());
    assertEquals(1, unmatched.size());
  }
}
