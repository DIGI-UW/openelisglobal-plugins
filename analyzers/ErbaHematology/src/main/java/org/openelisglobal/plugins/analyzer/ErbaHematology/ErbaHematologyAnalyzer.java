package org.openelisglobal.plugins.analyzer.ErbaHematology;

import static org.openelisglobal.common.services.PluginAnalyzerService.getInstance;

import java.util.ArrayList;
import java.util.List;
import org.openelisglobal.analyzerimport.analyzerreaders.AnalyzerLineInserter;
import org.openelisglobal.common.services.PluginAnalyzerService;
import org.openelisglobal.plugin.AnalyzerImporterPlugin;

/**
 * Plugin registration for ERBA MANNHEIM Hematology Analyzers (H360, H560, ELITE 580).
 *
 * <p>Test parameter codes and LOINC codes are sourced from the instrument's LIS Communication
 * Protocol document, Appendix II (Table 9).
 */
public class ErbaHematologyAnalyzer implements AnalyzerImporterPlugin {

  @Override
  public boolean connect() {
    List<PluginAnalyzerService.TestMapping> nameMapping = new ArrayList<>();

    //  WBC total
    nameMapping.add(
        new PluginAnalyzerService.TestMapping(
            ErbaHematologyAnalyzerLineInserter.WBC,
            "White Blood Cells",
            ErbaHematologyAnalyzerLineInserter.WBC_LOINC));

    //  5-part differential % (ELITE 580 CBC+DIFF mode)
    nameMapping.add(
        new PluginAnalyzerService.TestMapping(
            ErbaHematologyAnalyzerLineInserter.NEU_PCT,
            "Neutrophils %",
            ErbaHematologyAnalyzerLineInserter.NEU_PCT_LOINC));
    nameMapping.add(
        new PluginAnalyzerService.TestMapping(
            ErbaHematologyAnalyzerLineInserter.LYM_PCT,
            "Lymphocytes %",
            ErbaHematologyAnalyzerLineInserter.LYM_PCT_LOINC));
    nameMapping.add(
        new PluginAnalyzerService.TestMapping(
            ErbaHematologyAnalyzerLineInserter.MON_PCT,
            "Monocytes %",
            ErbaHematologyAnalyzerLineInserter.MON_PCT_LOINC));
    nameMapping.add(
        new PluginAnalyzerService.TestMapping(
            ErbaHematologyAnalyzerLineInserter.EOS_PCT,
            "Eosinophils %",
            ErbaHematologyAnalyzerLineInserter.EOS_PCT_LOINC));
    nameMapping.add(
        new PluginAnalyzerService.TestMapping(
            ErbaHematologyAnalyzerLineInserter.BAS_PCT,
            "Basophils %",
            ErbaHematologyAnalyzerLineInserter.BAS_PCT_LOINC));

    //  5-part differential # (ELITE 580 CBC+DIFF mode)
    nameMapping.add(
        new PluginAnalyzerService.TestMapping(
            ErbaHematologyAnalyzerLineInserter.NEU_ABS,
            "Neutrophils Absolute",
            ErbaHematologyAnalyzerLineInserter.NEU_ABS_LOINC));
    nameMapping.add(
        new PluginAnalyzerService.TestMapping(
            ErbaHematologyAnalyzerLineInserter.LYM_ABS,
            "Lymphocytes Absolute",
            ErbaHematologyAnalyzerLineInserter.LYM_ABS_LOINC));
    nameMapping.add(
        new PluginAnalyzerService.TestMapping(
            ErbaHematologyAnalyzerLineInserter.MON_ABS,
            "Monocytes Absolute",
            ErbaHematologyAnalyzerLineInserter.MON_ABS_LOINC));
    nameMapping.add(
        new PluginAnalyzerService.TestMapping(
            ErbaHematologyAnalyzerLineInserter.EOS_ABS,
            "Eosinophils Absolute",
            ErbaHematologyAnalyzerLineInserter.EOS_ABS_LOINC));
    nameMapping.add(
        new PluginAnalyzerService.TestMapping(
            ErbaHematologyAnalyzerLineInserter.BAS_ABS,
            "Basophils Absolute",
            ErbaHematologyAnalyzerLineInserter.BAS_ABS_LOINC));

    //  3-part differential (H360 CBC+3DIFF mode)
    nameMapping.add(
        new PluginAnalyzerService.TestMapping(
            ErbaHematologyAnalyzerLineInserter.GRAN_PCT,
            "Granulocytes %",
            ErbaHematologyAnalyzerLineInserter.GRAN_PCT_LOINC));
    nameMapping.add(
        new PluginAnalyzerService.TestMapping(
            ErbaHematologyAnalyzerLineInserter.MID_PCT,
            "Mid-range Cells %",
            ErbaHematologyAnalyzerLineInserter.MID_PCT_LOINC));
    nameMapping.add(
        new PluginAnalyzerService.TestMapping(
            ErbaHematologyAnalyzerLineInserter.GRAN_ABS,
            "Granulocytes Absolute",
            ErbaHematologyAnalyzerLineInserter.GRAN_ABS_LOINC));
    nameMapping.add(
        new PluginAnalyzerService.TestMapping(
            ErbaHematologyAnalyzerLineInserter.MID_ABS,
            "Mid-range Cells Absolute",
            ErbaHematologyAnalyzerLineInserter.MID_ABS_LOINC));

    //  RBC series
    nameMapping.add(
        new PluginAnalyzerService.TestMapping(
            ErbaHematologyAnalyzerLineInserter.RBC,
            "Red Blood Cells",
            ErbaHematologyAnalyzerLineInserter.RBC_LOINC));
    nameMapping.add(
        new PluginAnalyzerService.TestMapping(
            ErbaHematologyAnalyzerLineInserter.HGB,
            "Hemoglobin",
            ErbaHematologyAnalyzerLineInserter.HGB_LOINC));
    nameMapping.add(
        new PluginAnalyzerService.TestMapping(
            ErbaHematologyAnalyzerLineInserter.HCT,
            "Hematocrit",
            ErbaHematologyAnalyzerLineInserter.HCT_LOINC));
    nameMapping.add(
        new PluginAnalyzerService.TestMapping(
            ErbaHematologyAnalyzerLineInserter.MCV,
            "Mean Corpuscular Volume",
            ErbaHematologyAnalyzerLineInserter.MCV_LOINC));
    nameMapping.add(
        new PluginAnalyzerService.TestMapping(
            ErbaHematologyAnalyzerLineInserter.MCH,
            "Mean Corpuscular Hemoglobin",
            ErbaHematologyAnalyzerLineInserter.MCH_LOINC));
    nameMapping.add(
        new PluginAnalyzerService.TestMapping(
            ErbaHematologyAnalyzerLineInserter.MCHC,
            "Mean Corpuscular Hemoglobin Concentration",
            ErbaHematologyAnalyzerLineInserter.MCHC_LOINC));
    nameMapping.add(
        new PluginAnalyzerService.TestMapping(
            ErbaHematologyAnalyzerLineInserter.RDW_CV,
            "RDW-CV",
            ErbaHematologyAnalyzerLineInserter.RDW_CV_LOINC));
    nameMapping.add(
        new PluginAnalyzerService.TestMapping(
            ErbaHematologyAnalyzerLineInserter.RDW_SD,
            "RDW-SD",
            ErbaHematologyAnalyzerLineInserter.RDW_SD_LOINC));

    //  RBC indices (Mentzer, RDWI)
    nameMapping.add(
        new PluginAnalyzerService.TestMapping(
            ErbaHematologyAnalyzerLineInserter.MENTZER,
            "Mentzer Index",
            ErbaHematologyAnalyzerLineInserter.MENTZER_LOINC));
    nameMapping.add(
        new PluginAnalyzerService.TestMapping(
            ErbaHematologyAnalyzerLineInserter.RDWI,
            "RDW Index",
            ErbaHematologyAnalyzerLineInserter.RDWI_LOINC));

    //  Platelet series
    nameMapping.add(
        new PluginAnalyzerService.TestMapping(
            ErbaHematologyAnalyzerLineInserter.PLT,
            "Platelets",
            ErbaHematologyAnalyzerLineInserter.PLT_LOINC));
    nameMapping.add(
        new PluginAnalyzerService.TestMapping(
            ErbaHematologyAnalyzerLineInserter.MPV,
            "Mean Platelet Volume",
            ErbaHematologyAnalyzerLineInserter.MPV_LOINC));
    nameMapping.add(
        new PluginAnalyzerService.TestMapping(
            ErbaHematologyAnalyzerLineInserter.PDW,
            "Platelet Distribution Width",
            ErbaHematologyAnalyzerLineInserter.PDW_LOINC));
    nameMapping.add(
        new PluginAnalyzerService.TestMapping(
            ErbaHematologyAnalyzerLineInserter.PDW_SD,
            "Platelet Distribution Width SD",
            ErbaHematologyAnalyzerLineInserter.PDW_SD_LOINC));
    nameMapping.add(
        new PluginAnalyzerService.TestMapping(
            ErbaHematologyAnalyzerLineInserter.PDW_CV,
            "Platelet Distribution Width CV",
            ErbaHematologyAnalyzerLineInserter.PDW_CV_LOINC));
    nameMapping.add(
        new PluginAnalyzerService.TestMapping(
            ErbaHematologyAnalyzerLineInserter.PCT,
            "Plateletcrit",
            ErbaHematologyAnalyzerLineInserter.PCT_LOINC));
    nameMapping.add(
        new PluginAnalyzerService.TestMapping(
            ErbaHematologyAnalyzerLineInserter.PLCR,
            "Platelet Large Cell Ratio",
            ErbaHematologyAnalyzerLineInserter.PLCR_LOINC));
    nameMapping.add(
        new PluginAnalyzerService.TestMapping(
            ErbaHematologyAnalyzerLineInserter.PLCC,
            "Platelet Large Cell Count",
            ErbaHematologyAnalyzerLineInserter.PLCC_LOINC));

    //  Atypical / large immature cells (ELITE 580 only)
    nameMapping.add(
        new PluginAnalyzerService.TestMapping(
            ErbaHematologyAnalyzerLineInserter.ALY_ABS,
            "Atypical Lymphocytes Absolute",
            ErbaHematologyAnalyzerLineInserter.ALY_ABS_LOINC));
    nameMapping.add(
        new PluginAnalyzerService.TestMapping(
            ErbaHematologyAnalyzerLineInserter.ALY_PCT,
            "Atypical Lymphocytes %",
            ErbaHematologyAnalyzerLineInserter.ALY_PCT_LOINC));
    nameMapping.add(
        new PluginAnalyzerService.TestMapping(
            ErbaHematologyAnalyzerLineInserter.LIC_ABS,
            "Large Immature Cells Absolute",
            ErbaHematologyAnalyzerLineInserter.LIC_ABS_LOINC));
    nameMapping.add(
        new PluginAnalyzerService.TestMapping(
            ErbaHematologyAnalyzerLineInserter.LIC_PCT,
            "Large Immature Cells %",
            ErbaHematologyAnalyzerLineInserter.LIC_PCT_LOINC));

    getInstance().addAnalyzerDatabaseParts("ErbaHematology", "ErbaHematology", nameMapping, true);
    getInstance().registerAnalyzer(this);
    return true;
  }

  @Override
  public boolean isTargetAnalyzer(List<String> lines) {
    return false;
  }

  @Override
  public AnalyzerLineInserter getAnalyzerLineInserter() {
    return new ErbaHematologyAnalyzerLineInserter();
  }
}
