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

public class SysmexXTAnalyzer implements AnalyzerImporterPlugin {
  private static final String DELIMITER = ",";
  private static final CharSequence SYSMEX_XT_4000i_INDICATOR = "XT-4000i";
  int InstrumentIndex = -1;

  public boolean connect() {
    List<PluginAnalyzerService.TestMapping> nameMappinng =
        new ArrayList<PluginAnalyzerService.TestMapping>();
    nameMappinng.add(new PluginAnalyzerService.TestMapping("GB(10/uL)", "GB"));
    nameMappinng.add(new PluginAnalyzerService.TestMapping("GR(10^4/uL)", "GR"));
    nameMappinng.add(new PluginAnalyzerService.TestMapping("HBG(g/L)", "Hb"));
    nameMappinng.add(new PluginAnalyzerService.TestMapping("HCT(10^(-1)%)", "HCT"));
    nameMappinng.add(new PluginAnalyzerService.TestMapping("VGM(10^(-1)fL)", "VGM"));
    nameMappinng.add(new PluginAnalyzerService.TestMapping("TCMH(10^(-1)pg)", "TCMH"));
    nameMappinng.add(new PluginAnalyzerService.TestMapping("CCMH(g/L)", "CCMH"));
    nameMappinng.add(new PluginAnalyzerService.TestMapping("PLQ(10^3/uL)", "PLQ"));
    nameMappinng.add(new PluginAnalyzerService.TestMapping("NEUT%(10^(-1)%)", "Neut %"));
    nameMappinng.add(new PluginAnalyzerService.TestMapping("LYMPH%(10^(-1)%)", "Lymph %"));
    nameMappinng.add(new PluginAnalyzerService.TestMapping("MONO%(10^(-1)%)", "Mono %"));
    nameMappinng.add(new PluginAnalyzerService.TestMapping("EO%(10^(-1)%)", "Eo %"));
    nameMappinng.add(new PluginAnalyzerService.TestMapping("BASO%(10^(-1)%)", "Baso %"));
    getInstance()
        .addAnalyzerDatabaseParts(
            "Sysmex4000iAnalyzer", "Plugin for Sysmex XT 4000 i analyzer", nameMappinng);
    getInstance().registerAnalyzer(this);
    return true;
  }

  public boolean isTargetAnalyzer(List<String> lines) {
    int columnsLineIndex = getColumnsLine(lines);
    if (columnsLineIndex < 0) return false;
    String[] fields = lines.get(columnsLineIndex).split(DELIMITER);
    for (int j = 0; j < fields.length; j++) {
      if (fields[j].contains("ID Instrument")) {
        InstrumentIndex = j;
        break;
      }
    }

    if (lines.size() > columnsLineIndex + 1) {
      fields = lines.get(columnsLineIndex + 1).split(DELIMITER);
      if (fields[InstrumentIndex].contains(SYSMEX_XT_4000i_INDICATOR)) {
        return true;
      }
    }
    return false;
  }

  public AnalyzerLineInserter getAnalyzerLineInserter() {
    return new SysmexXTAnalyzerImplementation();
  }

  public int getColumnsLine(List<String> lines) {
    for (int k = 0; k < lines.size(); k++) {
      if (lines.get(k).contains("ID Instrument")
          && lines.get(k).contains("N' Echantillon")
          && lines.get(k).contains("Ana. Jour")
          && lines.get(k).contains("Ana. Heure")
          && lines.get(k).contains("N' Rack")
          && lines.get(k).contains("Pos. Tube")) return k;
    }

    return -1;
  }
}
