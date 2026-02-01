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

public class Cobas4800Analyzer implements AnalyzerImporterPlugin {
  // private static final String DELIMITER = "\\t";
  // private static String DELIMITER = "\\t";
  private static final CharSequence COBAS_4800_INDICATOR = "cobas 4800";

  int InstrumentIndex = -1;

  public boolean connect() {
    //  List nameMappinng = new ArrayList();
    List<PluginAnalyzerService.TestMapping> nameMappinng =
        new ArrayList<PluginAnalyzerService.TestMapping>();
    nameMappinng.add(new PluginAnalyzerService.TestMapping("Result", "Viral Load"));
    PluginAnalyzerService.getInstance()
        .addAnalyzerDatabaseParts(
            "Cobas4800VLAnalyzer", "Plugin for Cobas4800 VL analyzer", nameMappinng);
    PluginAnalyzerService.getInstance()
        .addAnalyzerDatabaseParts(
            "Cobas4800EIDAnalyzer", "Plugin for Cobas4800 EID analyzer", nameMappinng);
    PluginAnalyzerService.getInstance().registerAnalyzer(this);

    return true;
  }

  public AnalyzerLineInserter getAnalyzerLineInserter() {
    return new Cobas4800AnalyzerImplementation();
  }

  public boolean isTargetAnalyzer(List<String> lines) {

    for (int j = 0; j < lines.size(); j++) {
      if (lines.get(j).contains(COBAS_4800_INDICATOR)) return true;
    }

    return false;
  }
}
