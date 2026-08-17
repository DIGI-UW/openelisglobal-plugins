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

import java.util.ArrayList;
import java.util.List;
import org.openelisglobal.analyzerimport.analyzerreaders.AnalyzerLineInserter;
import org.openelisglobal.common.services.PluginAnalyzerService;
import org.openelisglobal.plugin.AnalyzerImporterPlugin;

public class TaqMan48VLAnalyzer implements AnalyzerImporterPlugin {
  // private static final String DELIMITER = "\\t";
  private static String DELIMITER = "\\t";
  private static final CharSequence COBAS_TAQMAN_VL_INDICATOR = "HI2CAP48";
  private static final CharSequence COBAS_TAQMAN_VL_INDICATOR2 = "IFS48CDC";

  int InstrumentIndex = -1;

  public boolean connect() {
    //  List nameMappinng = new ArrayList();
    List<PluginAnalyzerService.TestMapping> nameMappinng =
        new ArrayList<PluginAnalyzerService.TestMapping>();
    nameMappinng.add(new PluginAnalyzerService.TestMapping("Result", "Viral Load"));
    PluginAnalyzerService.getInstance()
        .addAnalyzerDatabaseParts(
            "TaqMan48VLAnalyzer", "Plugin for Cobas TaqMan48 VL analyzer", nameMappinng);
    PluginAnalyzerService.getInstance().registerAnalyzer(this);

    return true;
  }

  public boolean isTargetAnalyzer(List<String> lines) {
    if (getColumnsLine(lines) < 0) return false;
    DELIMITER = lines.get(getColumnsLine(lines)).substring(14, 15);
    String[] data = lines.get(getColumnsLine(lines)).split(DELIMITER);
    for (int j = 0; j < data.length; j++) {
      if (data[j].contains("Test")) {
        this.InstrumentIndex = j;
        break;
      }
    }

    if (lines.size() > getColumnsLine(lines) + 1) {
      data = lines.get(getColumnsLine(lines) + 1).split(DELIMITER);
      if ((data[this.InstrumentIndex].contains(COBAS_TAQMAN_VL_INDICATOR))
          || (data[this.InstrumentIndex].contains(COBAS_TAQMAN_VL_INDICATOR2))) {
        return true;
      }
    }

    return false;
  }

  public AnalyzerLineInserter getAnalyzerLineInserter() {
    return new TaqMan48VLAnalyzerImplementation();
  }

  public int getColumnsLine(List<String> lines) {
    for (int k = 0; k < lines.size(); k++) {
      if (lines.get(k).contains("Patient Name")
          && lines.get(k).contains("Patient ID")
          && lines.get(k).contains("Order Number")
          && lines.get(k).contains("Sample ID")
          && lines.get(k).contains("Test")
          && lines.get(k).contains("Result")) return k;
    }

    return -1;
  }
}
