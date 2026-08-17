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

public class SysmexAnalyzer implements AnalyzerImporterPlugin {

  public boolean connect() {
    List<PluginAnalyzerService.TestMapping> nameMappinng =
        new ArrayList<PluginAnalyzerService.TestMapping>();
    // ------------------------
    nameMappinng.add(
        new PluginAnalyzerService.TestMapping("GB(10/uL)", "Numération des globules blancs"));
    nameMappinng.add(
        new PluginAnalyzerService.TestMapping("GR(10^4/uL)", "Numération des globules rouges"));
    nameMappinng.add(new PluginAnalyzerService.TestMapping("HBG(g/L)", "Hémoglobine"));
    nameMappinng.add(new PluginAnalyzerService.TestMapping("HCT(10^(-1)%)", "Hématocrite"));
    nameMappinng.add(
        new PluginAnalyzerService.TestMapping("VGM(10^(-1)fL)", "Volume Globulaire Moyen"));
    nameMappinng.add(
        new PluginAnalyzerService.TestMapping(
            "TCMH(10^(-1)pg)", "Teneur Corpusculaire Moyenne en Hémoglobine"));
    nameMappinng.add(
        new PluginAnalyzerService.TestMapping(
            "CCMH(g/L)", "Concentration Corpusculaire Moyenne en Hémoglobine"));
    nameMappinng.add(new PluginAnalyzerService.TestMapping("PLQ(10^3/uL)", "Plaquette"));
    nameMappinng.add(
        new PluginAnalyzerService.TestMapping("NEUT#(10/uL)", "Polynucléaires Neutrophiles (Abs)"));
    nameMappinng.add(new PluginAnalyzerService.TestMapping("LYMPH#(10/uL)", "Lymphocytes (Abs)"));
    nameMappinng.add(new PluginAnalyzerService.TestMapping("MONO#(10/uL)", "Monocytes (Abs)"));
    nameMappinng.add(
        new PluginAnalyzerService.TestMapping("EO#(10/uL)", "Polynucléaires Eosinophiles (Abs)"));
    nameMappinng.add(
        new PluginAnalyzerService.TestMapping("BASO#(10/uL)", "Polynucléaires basophiles (Abs)"));
    nameMappinng.add(
        new PluginAnalyzerService.TestMapping(
            "NEUT%(10^(-1)%)", "Polynucléaires Neutrophiles (%)"));
    nameMappinng.add(new PluginAnalyzerService.TestMapping("LYMPH%(10^(-1)%)", "Lymphocytes (%)"));
    nameMappinng.add(new PluginAnalyzerService.TestMapping("MONO%(10^(-1)%)", "Monocytes (%)"));
    nameMappinng.add(
        new PluginAnalyzerService.TestMapping("EO%(10^(-1)%)", "Polynucléaires Eosinophiles (%)"));
    nameMappinng.add(
        new PluginAnalyzerService.TestMapping("BASO%(10^(-1)%)", "Polynucléaires basophiles (%)"));

    // -----------------------------------
    getInstance()
        .addAnalyzerDatabaseParts(
            "SysmexAnalyzer", "Plugin for Sysmex XT 2000i analyzer", nameMappinng);
    getInstance().registerAnalyzer(this);
    return true;
  }

  @Override
  public boolean isTargetAnalyzer(List<String> lines) {

    int idInstrumentIndex = -1;

    if (lines.size() > 1) {

      // String[] headers = lines.get(1).split("ret,");
      String[] headers = lines.get(0).split("ret,");
      for (int i = 0; i < headers.length; i++) {
        if (headers[i].contains(" ID Instrument")) {
          idInstrumentIndex = i;
        }
      }

      if (idInstrumentIndex == -1) {
        return false;
      }

      String[] data = lines.get(1).split(",");
      if (data[idInstrumentIndex].contains("XT-2000i")) {
        return true;
      }
    }
    return false;
  }

  @Override
  public AnalyzerLineInserter getAnalyzerLineInserter() {
    return new SysmexAnalyzerImplementation();
  }
}
