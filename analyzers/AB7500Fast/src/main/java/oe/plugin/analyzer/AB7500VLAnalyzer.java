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

public class AB7500VLAnalyzer implements AnalyzerImporterPlugin {
  // private static final String DELIMITER = ",";
  private static final CharSequence AB7500VL_INDICATOR = "sds7500fast";

  int InstrumentIndex = -1;

  public boolean connect() {
    //  List nameMappinng = new ArrayList();
    List<PluginAnalyzerService.TestMapping> nameMappinng =
        new ArrayList<PluginAnalyzerService.TestMapping>();
    nameMappinng.add(new PluginAnalyzerService.TestMapping("Quantity", "Viral Load"));

    PluginAnalyzerService.getInstance()
        .addAnalyzerDatabaseParts("AB7500VLAnalyzer", "Plugin for AB 7500 analyzer", nameMappinng);
    PluginAnalyzerService.getInstance().registerAnalyzer(this);

    return true;
  }

  public boolean isTargetAnalyzer(List<String> lines) {
    for (int j = 0; j < lines.size(); j++) {
      if (lines.get(j).contains(AB7500VL_INDICATOR)) return true;
    }

    return false;
  }

  public AnalyzerLineInserter getAnalyzerLineInserter() {
    return new AB7500VLAnalyzerImplementation();
  }
}
