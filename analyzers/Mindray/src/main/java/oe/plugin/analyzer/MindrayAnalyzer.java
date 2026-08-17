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

import static org.openelisglobal.common.services.PluginAnalyzerService.getInstance;

import java.util.ArrayList;
import java.util.List;
import org.openelisglobal.analyzerimport.analyzerreaders.AnalyzerLineInserter;
import org.openelisglobal.common.services.PluginAnalyzerService;
import org.openelisglobal.plugin.AnalyzerImporterPlugin;

public class MindrayAnalyzer implements AnalyzerImporterPlugin {

  @Override
  public boolean connect() {
    List<PluginAnalyzerService.TestMapping> nameMapping = new ArrayList<>();
    nameMapping.add(
        new PluginAnalyzerService.TestMapping(
            MindrayAnalyzerImplementation.TBil,
            "Total Bilirubin",
            MindrayAnalyzerImplementation.TBil_LOINC));
    nameMapping.add(
        new PluginAnalyzerService.TestMapping(
            MindrayAnalyzerImplementation.DBil,
            "Direct Bilirubin",
            MindrayAnalyzerImplementation.DBil_LOINC));
    nameMapping.add(
        new PluginAnalyzerService.TestMapping(
            MindrayAnalyzerImplementation.IBIL,
            "Indirect Bilirubin",
            MindrayAnalyzerImplementation.IBIL_LOINC));
    nameMapping.add(
        new PluginAnalyzerService.TestMapping(
            MindrayAnalyzerImplementation.ALT,
            "Alanine Aminotransferase",
            MindrayAnalyzerImplementation.ALT_LOINC));
    nameMapping.add(
        new PluginAnalyzerService.TestMapping(
            MindrayAnalyzerImplementation.AST,
            "Aspartate Aminotransferase",
            MindrayAnalyzerImplementation.AST_LOINC));
    nameMapping.add(
        new PluginAnalyzerService.TestMapping(
            MindrayAnalyzerImplementation.ALP,
            "Alkaline Phosphatase",
            MindrayAnalyzerImplementation.ALP_LOINC));
    nameMapping.add(
        new PluginAnalyzerService.TestMapping(
            MindrayAnalyzerImplementation.YGT,
            "Gamma Glutamyltransferase",
            MindrayAnalyzerImplementation.YGT_LOINC));
    nameMapping.add(
        new PluginAnalyzerService.TestMapping(
            MindrayAnalyzerImplementation.LDH,
            "Lactate Dehydrogenase",
            MindrayAnalyzerImplementation.LDH_LOINC));
    nameMapping.add(
        new PluginAnalyzerService.TestMapping(
            MindrayAnalyzerImplementation.TG,
            "Triglycerides",
            MindrayAnalyzerImplementation.TG_LOINC));
    nameMapping.add(
        new PluginAnalyzerService.TestMapping(
            MindrayAnalyzerImplementation.HDLC,
            "HDL Cholesterol",
            MindrayAnalyzerImplementation.HDLC_LOINC));
    nameMapping.add(
        new PluginAnalyzerService.TestMapping(
            MindrayAnalyzerImplementation.TC,
            "Total Cholesterol",
            MindrayAnalyzerImplementation.TC_LOINC));
    nameMapping.add(
        new PluginAnalyzerService.TestMapping(
            MindrayAnalyzerImplementation.LDL,
            "LDL Cholesterol",
            MindrayAnalyzerImplementation.LDL_LOINC));
    nameMapping.add(
        new PluginAnalyzerService.TestMapping(
            MindrayAnalyzerImplementation.VLDL,
            "VLDL Cholesterol",
            MindrayAnalyzerImplementation.VLDL_LOINC));
    nameMapping.add(
        new PluginAnalyzerService.TestMapping(
            MindrayAnalyzerImplementation.CholHDL,
            "Cholesterol/HDL Ratio",
            MindrayAnalyzerImplementation.CholHDL_LOINC));
    nameMapping.add(
        new PluginAnalyzerService.TestMapping(
            MindrayAnalyzerImplementation.CREA,
            "Creatinine",
            MindrayAnalyzerImplementation.CREA_LOINC));
    nameMapping.add(
        new PluginAnalyzerService.TestMapping(
            MindrayAnalyzerImplementation.AZOTE_UREA,
            "Urea Nitrogen",
            MindrayAnalyzerImplementation.AZOTE_UREA_LOINC));
    nameMapping.add(
        new PluginAnalyzerService.TestMapping(
            MindrayAnalyzerImplementation.Ca, "Calcium", MindrayAnalyzerImplementation.Ca_LOINC));
    nameMapping.add(
        new PluginAnalyzerService.TestMapping(
            MindrayAnalyzerImplementation.P, "Phosphorous", MindrayAnalyzerImplementation.P_LOINC));
    nameMapping.add(
        new PluginAnalyzerService.TestMapping(
            MindrayAnalyzerImplementation.TP,
            "Total Protein",
            MindrayAnalyzerImplementation.TP_LOINC));
    nameMapping.add(
        new PluginAnalyzerService.TestMapping(
            MindrayAnalyzerImplementation.ALB, "Albumin", MindrayAnalyzerImplementation.ALB_LOINC));
    nameMapping.add(
        new PluginAnalyzerService.TestMapping(
            MindrayAnalyzerImplementation.Globulines,
            "Globulines",
            MindrayAnalyzerImplementation.Globulines_LOINC));
    nameMapping.add(
        new PluginAnalyzerService.TestMapping(
            MindrayAnalyzerImplementation.Alb_Glob,
            "Albumin/Globulin Ratio",
            MindrayAnalyzerImplementation.Alb_Glob_LOINC));
    nameMapping.add(
        new PluginAnalyzerService.TestMapping(
            MindrayAnalyzerImplementation.Mg, "Magnesium", MindrayAnalyzerImplementation.Mg_LOINC));
    getInstance().addAnalyzerDatabaseParts("Mindray", "Mindray", nameMapping, true);
    getInstance().registerAnalyzer(this);
    return true;
  }

  @Override
  // this plugin does not work for flat files, so we disable it in that workflow
  public boolean isTargetAnalyzer(List<String> lines) {
    return false;
  }

  @Override
  public AnalyzerLineInserter getAnalyzerLineInserter() {
    return new MindrayAnalyzerImplementation();
  }
}
