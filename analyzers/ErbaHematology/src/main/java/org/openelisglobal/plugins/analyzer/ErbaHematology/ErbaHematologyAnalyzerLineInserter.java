package org.openelisglobal.plugins.analyzer.ErbaHematology;

import java.sql.Timestamp;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import org.openelisglobal.analyzerimport.analyzerreaders.AnalyzerLineInserter;
import org.openelisglobal.analyzerimport.analyzerreaders.AnalyzerReaderUtil;
import org.openelisglobal.analyzerresults.valueholder.AnalyzerResults;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.spring.util.SpringContext;
import org.openelisglobal.test.service.TestService;
import org.openelisglobal.test.valueholder.Test;

/**
 * Result persistence implementation for ERBA MANNHEIM Hematology Analyzers (H360, H560, ELITE 580).
 *
 * <p>Constants are derived from two sources:
 *
 * <ol>
 *   <li>The instrument's LIS Communication Protocol, Appendix II Table 9.
 *   <li>Real HL7 messages captured from H360, H560, and ELITE 580 instruments, which revealed
 *       additional parameters (GRAN, MID, PDW-CV, Mentzer, RDWI, LIC) not present in the protocol
 *       document's example messages.
 * </ol>
 *
 * <p>The analyzerTestId passed to {@link #addResult} is the ID component of OBX-3 (format {@code
 * ID^Name^EncodeSys}), which is the LOINC code for LN parameters or the vendor code for 99MRC
 * parameters. Both are used as keys into the testLoincMap uniformly.
 *
 * <p>Model-specific notes:
 *
 * <ul>
 *   <li><b>H360</b>: sends CBC+3DIFF uses GRAN%/#, MID%/# instead of the full 5-part
 *       NEU/MON/EOS/BAS split.
 *   <li><b>H560</b>: sends CBC only no differential parameters.
 *   <li><b>ELITE 580</b>: sends CBC+DIFF (5-part) full NEU/LYM/MON/EOS/BAS plus ALY and LIC
 *       extended parameters.
 *   <li>All three models send PDW as two separate parameters: PDW-SD and PDW-CV. There is no plain
 *       "PDW" OBX segment in any real message.
 * </ul>
 */
public class ErbaHematologyAnalyzerLineInserter extends AnalyzerLineInserter {

  // =========================================================================
  // OBX-3 ID component strings  (what the instrument sends in OBX-3)
  // =========================================================================

  //  WBC total
  static final String WBC = "WBC";

  //  5-part differential % (ELITE 580, CBC+DIFF mode)
  static final String NEU_PCT = "NEU%";
  static final String LYM_PCT = "LYM%";
  static final String MON_PCT = "MON%";
  static final String EOS_PCT = "EOS%";
  static final String BAS_PCT = "BAS%";

  //  5-part differential # (ELITE 580, CBC+DIFF mode)
  static final String NEU_ABS = "NEU#";
  static final String LYM_ABS = "LYM#";
  static final String MON_ABS = "MON#";
  static final String EOS_ABS = "EOS#";
  static final String BAS_ABS = "BAS#";

  //  3-part differential (H360, CBC+3DIFF mode)
  static final String GRAN_PCT = "GRAN%";
  static final String MID_PCT = "MID%";
  static final String GRAN_ABS = "GRAN#";
  static final String MID_ABS = "MID#";

  //  RBC series (all models)
  static final String RBC = "RBC";
  static final String HGB = "HGB";
  static final String HCT = "HCT";
  static final String MCV = "MCV";
  static final String MCH = "MCH";
  static final String MCHC = "MCHC";
  static final String RDW_CV = "RDW-CV";
  static final String RDW_SD = "RDW-SD";

  //  RBC derived indices (H360, ELITE 580)
  static final String MENTZER = "*Mentzr";
  static final String RDWI = "*RDWI";

  //  Platelet series (all models)
  static final String PLT = "PLT";
  static final String MPV = "MPV";
  static final String PDW = "PDW";
  static final String PDW_SD = "PDW-SD";
  static final String PDW_CV = "PDW-CV";
  static final String PCT = "PCT";
  static final String PLCR = "P-LCR";
  static final String PLCC = "P-LCC";

  //  Atypical / large immature cells (ELITE 580 only)
  static final String ALY_ABS = "*ALY#";
  static final String ALY_PCT = "*ALY%";
  static final String LIC_ABS = "*LIC#";
  static final String LIC_PCT = "*LIC%";

  // LN = standard LOINC  |  99MRC = vendor-defined

  static final String WBC_LOINC = "6690-2"; // LN
  static final String NEU_PCT_LOINC = "770-8"; // LN
  static final String LYM_PCT_LOINC = "736-9"; // LN
  static final String MON_PCT_LOINC = "5905-5"; // LN
  static final String EOS_PCT_LOINC = "713-8"; // LN
  static final String BAS_PCT_LOINC = "706-2"; // LN
  static final String NEU_ABS_LOINC = "751-8"; // LN
  static final String LYM_ABS_LOINC = "731-0"; // LN
  static final String MON_ABS_LOINC = "742-7"; // LN
  static final String EOS_ABS_LOINC = "711-2"; // LN
  static final String BAS_ABS_LOINC = "704-7"; // LN
  static final String GRAN_PCT_LOINC = "20482-6"; // LN
  static final String MID_PCT_LOINC = "32155-4"; // LN
  static final String GRAN_ABS_LOINC = "19023-1"; // LN
  static final String MID_ABS_LOINC = "32154-7"; // LN
  static final String RBC_LOINC = "789-8"; // LN
  static final String HGB_LOINC = "718-7"; // LN
  static final String HCT_LOINC = "4544-3"; // LN
  static final String MCV_LOINC = "787-2"; // LN
  static final String MCH_LOINC = "785-6"; // LN
  static final String MCHC_LOINC = "786-4"; // LN
  static final String RDW_CV_LOINC = "788-0"; // LN
  static final String RDW_SD_LOINC = "21000-5"; // LN
  static final String MENTZER_LOINC = "11092"; // 99MRC
  static final String RDWI_LOINC = "11093"; // 99MRC
  static final String PLT_LOINC = "777-3"; // LN
  static final String MPV_LOINC = "32623-1"; // LN
  static final String PDW_LOINC = "32207-3"; // LN
  static final String PDW_SD_LOINC = "32207-3"; // LN
  static final String PDW_CV_LOINC = "11090"; // 99MRC
  static final String PCT_LOINC = "11003"; // 99MRC
  static final String PLCR_LOINC = "48386-7"; // LN
  static final String PLCC_LOINC = "34167-7"; // LN
  static final String ALY_ABS_LOINC = "26477-0"; // LN
  static final String ALY_PCT_LOINC = "13046-8"; // LN
  static final String LIC_ABS_LOINC = "11001"; // 99MRC
  static final String LIC_PCT_LOINC = "11002"; // 99MRC

  private TestService testService;
  private HashMap<String, List<Test>> testLoincMap;
  private final AnalyzerReaderUtil readerUtil = new AnalyzerReaderUtil();

  protected TestService getTestService() {
    if (testService == null) {
      testService = SpringContext.getBean(TestService.class);
    }
    return testService;
  }

  protected HashMap<String, List<Test>> getTestLoincMap() {
    if (testLoincMap == null) {
      testLoincMap = new HashMap<>();
      TestService ts = getTestService();

      testLoincMap.put(WBC_LOINC, ts.getTestsByLoincCode(WBC_LOINC));
      testLoincMap.put(NEU_PCT_LOINC, ts.getTestsByLoincCode(NEU_PCT_LOINC));
      testLoincMap.put(LYM_PCT_LOINC, ts.getTestsByLoincCode(LYM_PCT_LOINC));
      testLoincMap.put(MON_PCT_LOINC, ts.getTestsByLoincCode(MON_PCT_LOINC));
      testLoincMap.put(EOS_PCT_LOINC, ts.getTestsByLoincCode(EOS_PCT_LOINC));
      testLoincMap.put(BAS_PCT_LOINC, ts.getTestsByLoincCode(BAS_PCT_LOINC));
      testLoincMap.put(NEU_ABS_LOINC, ts.getTestsByLoincCode(NEU_ABS_LOINC));
      testLoincMap.put(LYM_ABS_LOINC, ts.getTestsByLoincCode(LYM_ABS_LOINC));
      testLoincMap.put(MON_ABS_LOINC, ts.getTestsByLoincCode(MON_ABS_LOINC));
      testLoincMap.put(EOS_ABS_LOINC, ts.getTestsByLoincCode(EOS_ABS_LOINC));
      testLoincMap.put(BAS_ABS_LOINC, ts.getTestsByLoincCode(BAS_ABS_LOINC));
      testLoincMap.put(GRAN_PCT_LOINC, ts.getTestsByLoincCode(GRAN_PCT_LOINC));
      testLoincMap.put(MID_PCT_LOINC, ts.getTestsByLoincCode(MID_PCT_LOINC));
      testLoincMap.put(GRAN_ABS_LOINC, ts.getTestsByLoincCode(GRAN_ABS_LOINC));
      testLoincMap.put(MID_ABS_LOINC, ts.getTestsByLoincCode(MID_ABS_LOINC));
      testLoincMap.put(RBC_LOINC, ts.getTestsByLoincCode(RBC_LOINC));
      testLoincMap.put(HGB_LOINC, ts.getTestsByLoincCode(HGB_LOINC));
      testLoincMap.put(HCT_LOINC, ts.getTestsByLoincCode(HCT_LOINC));
      testLoincMap.put(MCV_LOINC, ts.getTestsByLoincCode(MCV_LOINC));
      testLoincMap.put(MCH_LOINC, ts.getTestsByLoincCode(MCH_LOINC));
      testLoincMap.put(MCHC_LOINC, ts.getTestsByLoincCode(MCHC_LOINC));
      testLoincMap.put(RDW_CV_LOINC, ts.getTestsByLoincCode(RDW_CV_LOINC));
      testLoincMap.put(RDW_SD_LOINC, ts.getTestsByLoincCode(RDW_SD_LOINC));
      testLoincMap.put(MENTZER_LOINC, ts.getTestsByLoincCode(MENTZER_LOINC));
      testLoincMap.put(RDWI_LOINC, ts.getTestsByLoincCode(RDWI_LOINC));
      testLoincMap.put(PLT_LOINC, ts.getTestsByLoincCode(PLT_LOINC));
      testLoincMap.put(MPV_LOINC, ts.getTestsByLoincCode(MPV_LOINC));
      testLoincMap.put(PDW_LOINC, ts.getTestsByLoincCode(PDW_LOINC));
      testLoincMap.put(PDW_SD_LOINC, ts.getTestsByLoincCode(PDW_SD_LOINC));
      testLoincMap.put(PDW_CV_LOINC, ts.getTestsByLoincCode(PDW_CV_LOINC));
      testLoincMap.put(PCT_LOINC, ts.getTestsByLoincCode(PCT_LOINC));
      testLoincMap.put(PLCR_LOINC, ts.getTestsByLoincCode(PLCR_LOINC));
      testLoincMap.put(PLCC_LOINC, ts.getTestsByLoincCode(PLCC_LOINC));
      testLoincMap.put(ALY_ABS_LOINC, ts.getTestsByLoincCode(ALY_ABS_LOINC));
      testLoincMap.put(ALY_PCT_LOINC, ts.getTestsByLoincCode(ALY_PCT_LOINC));
      testLoincMap.put(LIC_ABS_LOINC, ts.getTestsByLoincCode(LIC_ABS_LOINC));
      testLoincMap.put(LIC_PCT_LOINC, ts.getTestsByLoincCode(LIC_PCT_LOINC));

      LogEvent.logDebug(
          this.getClass().getSimpleName(),
          "getTestLoincMap",
          "ErbaHematology LOINC map initialised with " + testLoincMap.size() + " entries");
    }
    return testLoincMap;
  }

  @Override
  public boolean insert(List<String> lines, String currentUserId) {
    return false;
  }

  @Override
  public String getError() {
    return "ErbaHematology analyzer unable to write to database";
  }

  /**
   * Creates an {@link AnalyzerResults} from a single OBX segment and routes it to {@code
   * resultList} (matched) or {@code notMatchedResults} (unmatched).
   *
   * <p>IS-type metadata segments (Take Mode, Blood Mode, Test Mode, Ref Group, Remark codes 02001,
   * 02002, 02003, 03001, 09001) and alarm flag segments (codes 13xxx) have no entry in the LOINC
   * map and go quietly to {@code notMatchedResults}.
   *
   * @param resultList accumulates results with a matched OpenELIS test
   * @param notMatchedResults accumulates results with no matching test
   * @param resultType OBX-2 value type (NM, IS, ST …)
   * @param resultValue OBX-5 observation value
   * @param accessionNumber OBR-3 filler order number (sample ID)
   * @param isControl true when OBR-13 is non-empty (QC sample)
   * @param resultUnits OBX-6 unit string
   * @param analyzerTestId OBX-3 ID component used as testLoincMap key
   */
  public void addResult(
      List<AnalyzerResults> resultList,
      List<AnalyzerResults> notMatchedResults,
      String resultType,
      String resultValue,
      String accessionNumber,
      boolean isControl,
      String resultUnits,
      String analyzerTestId) {

    AnalyzerResults analyzerResults =
        createAnalyzerResult(
            resultType, resultValue, resultUnits, accessionNumber, isControl, analyzerTestId);

    if (analyzerResults.getTestId() != null) {
      addValueToResults(resultList, analyzerResults);
    } else {
      LogEvent.logDebug(
          this.getClass().getSimpleName(),
          "addResult",
          "No test match for analyzerTestId: " + analyzerTestId);
      notMatchedResults.add(analyzerResults);
    }
  }

  private void addValueToResults(List<AnalyzerResults> resultList, AnalyzerResults result) {
    resultList.add(result);
    AnalyzerResults resultFromDB = readerUtil.createAnalyzerResultFromDB(result);
    if (resultFromDB != null) {
      resultList.add(resultFromDB);
    }
  }

  private AnalyzerResults createAnalyzerResult(
      String resultType,
      String resultValue,
      String resultUnits,
      String accessionNumber,
      boolean isControl,
      String analyzerTestId) {

    AnalyzerResults analyzerResults = new AnalyzerResults();
    analyzerResults.setResult(resultValue);
    analyzerResults.setUnits(resultUnits);
    analyzerResults.setCompleteDate(new Timestamp(new Date().getTime()));
    analyzerResults.setAccessionNumber(accessionNumber);
    analyzerResults.setIsControl(isControl);

    List<Test> tests = getTestLoincMap().get(analyzerTestId);
    if (tests != null && !tests.isEmpty()) {
      analyzerResults.setTestId(tests.get(0).getId());
      analyzerResults.setTestName(tests.get(0).getLocalizedTestName().getLocalizedValue());
    } else {
      analyzerResults.setTestId(null);
      analyzerResults.setTestName("");
    }

    return analyzerResults;
  }

  public void persistImport(List<AnalyzerResults> resultList) {
    this.persistImport("1", resultList);
  }
}
