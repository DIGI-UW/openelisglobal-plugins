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

public class FacsCantoII implements AnalyzerImporterPlugin {

  public boolean connect() {
    List<PluginAnalyzerService.TestMapping> nameMappinng =
        new ArrayList<PluginAnalyzerService.TestMapping>();

    /*
    -- Make it active if we need select CD3 values

    nameMappinng.add(new PluginAnalyzerService.TestMapping("CD3_ABS", "CD3 en Valeur Absolu"));
    nameMappinng.add(new PluginAnalyzerService.TestMapping("CD3_PER", "CD3 en %"));

    */

    nameMappinng.add(new PluginAnalyzerService.TestMapping("CD3_PER", "CD3 percentage count"));
    nameMappinng.add(new PluginAnalyzerService.TestMapping("CD4_PER", "CD4 percentage count"));
    // nameMappinng.add(new PluginAnalyzerService.TestMapping("CD4_PER", "Dénombrement des
    // lymphocytes  CD4 (%)"));
    getInstance().addAnalyzerDatabaseParts("FacsCantoII", "Plugin for FacsCantoII", nameMappinng);
    getInstance().registerAnalyzer(this);
    return true;
  }

  @Override
  public boolean isTargetAnalyzer(List<String> lines) {
    return lines.get(1) != null && lines.get(1).contains("TRITEST");
  }

  @Override
  public AnalyzerLineInserter getAnalyzerLineInserter() {
    return new FacsCantoIIImplementation();
  }
}
