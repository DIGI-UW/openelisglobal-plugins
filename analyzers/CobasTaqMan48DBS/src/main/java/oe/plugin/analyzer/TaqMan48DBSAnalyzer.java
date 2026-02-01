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

public class TaqMan48DBSAnalyzer implements AnalyzerImporterPlugin {

  private static final String DELIMITER = "\\t";

  public boolean connect() {
    List<PluginAnalyzerService.TestMapping> nameMappinng =
        new ArrayList<PluginAnalyzerService.TestMapping>();
    getInstance()
        .addAnalyzerDatabaseParts(
            "TaqMan48DBSAnalyzer", "Plugin for Cobas TaqMan48 DBS analyzer", nameMappinng);
    getInstance().registerAnalyzer(this);
    return true;
  }

  @Override
  public boolean isTargetAnalyzer(List<String> lines) {

    int idInstrumentIndex = -1;
    int colunmsLine = -1;

    for (Integer i = 0; i < lines.size(); i++) {
      String[] data = lines.get(i).split(DELIMITER);
      System.out.println("LIGNE:" + i + "--" + lines.get(i));
      for (int j = 0; j < data.length; j++) {
        if (data[j].contains("Test")) {
          idInstrumentIndex = j;
          colunmsLine = i;
        }
      }
      if (colunmsLine > -1) break;
    }

    if (lines.size() > colunmsLine) {

      if (idInstrumentIndex == -1) {
        return false;
      }

      String[] data = lines.get(colunmsLine + 1).split(DELIMITER);
      if (data[idInstrumentIndex].contains("HI2QLD48")) {
        return true;
      }
    }
    return false;
  }

  @Override
  public AnalyzerLineInserter getAnalyzerLineInserter() {
    return new TaqMan48DBSAnalyzerImplementation();
  }
}
