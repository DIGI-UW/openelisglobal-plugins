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

public class SysmexKX21Analyzer implements AnalyzerImporterPlugin {

  private static final CharSequence KX21_INDICATOR = "KX21-NERG";
  int InstrumentIndex = -1;

  public boolean connect() {
    List<PluginAnalyzerService.TestMapping> nameMappinng =
        new ArrayList<PluginAnalyzerService.TestMapping>();
    nameMappinng.add(new PluginAnalyzerService.TestMapping("GB", "Numération des globules blancs"));
    nameMappinng.add(new PluginAnalyzerService.TestMapping("GR", "Numération des globules rouges"));
    nameMappinng.add(new PluginAnalyzerService.TestMapping("HB", "Hémoglobine"));
    nameMappinng.add(new PluginAnalyzerService.TestMapping("Hct", "Hématocrite"));
    nameMappinng.add(new PluginAnalyzerService.TestMapping("VGM", "Volume Globulaire Moyen"));
    nameMappinng.add(
        new PluginAnalyzerService.TestMapping(
            "TCMH", "Teneur Corpusculaire Moyenne en Hémoglobine"));
    nameMappinng.add(
        new PluginAnalyzerService.TestMapping(
            "CCMH", "Concentration Corpusculaire Moyenne en Hémoglobine"));
    nameMappinng.add(new PluginAnalyzerService.TestMapping("PLT", "Plaquette"));
    nameMappinng.add(
        new PluginAnalyzerService.TestMapping("GRAN%", "Polynucléaires Neutrophiles (%)"));
    nameMappinng.add(new PluginAnalyzerService.TestMapping("LYM%", "Lymphocytes (%)"));
    nameMappinng.add(new PluginAnalyzerService.TestMapping("MONO%", "Monocytes (%)"));
    nameMappinng.add(
        new PluginAnalyzerService.TestMapping("GRAN#", "Polynucléaires Neutrophiles (Abs)"));
    nameMappinng.add(new PluginAnalyzerService.TestMapping("LYM#", "Lymphocytes (Abs)"));
    nameMappinng.add(new PluginAnalyzerService.TestMapping("MONO#", "Monocytes (Abs)"));
    getInstance()
        .addAnalyzerDatabaseParts("SysmexKX21Analyzer", "Plugin for KX21 analyzer", nameMappinng);
    getInstance().registerAnalyzer(this);

    return true;
  }

  @Override
  public boolean isTargetAnalyzer(List<String> lines) {
    for (int j = 0; j < lines.size(); j++) {
      if (lines.get(j).contains(KX21_INDICATOR)) return true;
    }
    return false;
  }

  @Override
  public AnalyzerLineInserter getAnalyzerLineInserter() {
    return new SysmexKX21AnalyzerImplementation();
  }

  public int getColumnsLine(List<String> lines) {
    for (int k = 0; k < lines.size(); k++) {
      if (lines.get(k).contains("KX21")
          && lines.get(k).contains("LYM%")
          && lines.get(k).contains("GRAN%")) return k;
    }

    return -1;
  }
}
