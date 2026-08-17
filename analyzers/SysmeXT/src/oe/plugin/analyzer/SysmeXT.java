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

public class SysmeXT implements AnalyzerImporterPlugin {

  public boolean connect() {
    List<PluginAnalyzerService.TestMapping> nameMappinng =
        new ArrayList<PluginAnalyzerService.TestMapping>();

    nameMappinng.add(
        new PluginAnalyzerService.TestMapping("GB_10_uL", "Numération des globules blancs"));
    nameMappinng.add(
        new PluginAnalyzerService.TestMapping("GR_100000_uL", "Numération des globules rouges"));
    nameMappinng.add(new PluginAnalyzerService.TestMapping("HBG_g_L", "Hémoglobine"));
    nameMappinng.add(new PluginAnalyzerService.TestMapping("HCT_10_NEG_1_PER", "Hématocrite"));
    nameMappinng.add(
        new PluginAnalyzerService.TestMapping("VGM_10_NEG_1_fL", "Volume Globulaire Moyen"));
    nameMappinng.add(
        new PluginAnalyzerService.TestMapping(
            "CCMH_g_L", "Concentration Corpusculaire Moyenne en Hémoglobine"));
    nameMappinng.add(
        new PluginAnalyzerService.TestMapping(
            "TCMH_10_NEG_1_pg", "Teneur Corpusculaire Moyenne en Hémoglobine"));
    nameMappinng.add(new PluginAnalyzerService.TestMapping("PLQ_10_3_uL", "Plaquette"));
    nameMappinng.add(
        new PluginAnalyzerService.TestMapping(
            "NEUT_PER_10_NEG_1_PER", "Polynucléaires Neutrophiles (%)"));
    nameMappinng.add(
        new PluginAnalyzerService.TestMapping("LYMPH_PER_10_NEG_1_PER", "Lymphocytes (%)"));
    nameMappinng.add(
        new PluginAnalyzerService.TestMapping("MONO_PER_10_NEG_1_PER", "Monocytes (%)"));
    nameMappinng.add(
        new PluginAnalyzerService.TestMapping(
            "EO_PER_10_NEG_1_PER", "Polynucléaires Eosinophiles (%)"));
    nameMappinng.add(
        new PluginAnalyzerService.TestMapping(
            "BASO_PER_10_NEG_1_PER", "Polynucléaires basophiles (%)"));
    nameMappinng.add(
        new PluginAnalyzerService.TestMapping(
            "NEUT_COUNT_10_uL", "Polynucléaires Neutrophiles (Abs)"));
    nameMappinng.add(
        new PluginAnalyzerService.TestMapping("LYMPH_COUNT_10_uL", "Lymphocytes (Abs)"));
    nameMappinng.add(new PluginAnalyzerService.TestMapping("MONO_COUNT_10_uL", "Monocytes (Abs)"));
    nameMappinng.add(
        new PluginAnalyzerService.TestMapping(
            "EO_COUNT_10_uL", "Polynucléaires Eosinophiles (Abs)"));
    nameMappinng.add(
        new PluginAnalyzerService.TestMapping(
            "BASO_COUNT_10_uL", "Polynucléaires basophiles (Abs)"));

    // SysmeXT
    getInstance().addAnalyzerDatabaseParts("SysmeXT", "Plugin for SysmeXTi", nameMappinng);
    getInstance().registerAnalyzer(this);
    return true;
  }

  @Override
  public boolean isTargetAnalyzer(List<String> lines) {
    return lines.get(1) != null && lines.get(8).contains("60272");
  }

  @Override
  public AnalyzerLineInserter getAnalyzerLineInserter() {
    return new SysmeXTImplementation();
  }
}
