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

public class CobasIntegra400 implements AnalyzerImporterPlugin {

  public boolean connect() {
    List<PluginAnalyzerService.TestMapping> nameMappinng =
        new ArrayList<PluginAnalyzerService.TestMapping>();

    nameMappinng.add(new PluginAnalyzerService.TestMapping("ALTL", "Transaminases ALTL"));
    nameMappinng.add(new PluginAnalyzerService.TestMapping("ASTL", "Transaminases ASTL"));
    nameMappinng.add(new PluginAnalyzerService.TestMapping("CREJ2", "Créatininémie"));
    nameMappinng.add(new PluginAnalyzerService.TestMapping("GLU3", "Glycémie"));
    nameMappinng.add(new PluginAnalyzerService.TestMapping("GLU2", "Glycémie"));
    getInstance()
        .addAnalyzerDatabaseParts("CobasIntegra400", "Plugin for Cobas Integra 400", nameMappinng);
    getInstance().registerAnalyzer(this);
    return true;
    /* ----------------
    nameMappinng.add(new PluginAnalyzerService.TestMapping("ALTL", "Transaminases GPT (37°C)"));
    nameMappinng.add(new PluginAnalyzerService.TestMapping("ASTL", "Transaminases"));
    nameMappinng.add(new PluginAnalyzerService.TestMapping("CREJ2", "Créatinine"));
    nameMappinng.add(new PluginAnalyzerService.TestMapping("GLU2", "Glucose"));


    getInstance().addAnalyzerDatabaseParts("CobasIntegra400", "Plugin for Cobas Integra 400",nameMappinng);
    getInstance().registerAnalyzer(this);
    return true;*/

  }

  @Override
  public boolean isTargetAnalyzer(List<String> lines) {
    return lines.get(0) != null && lines.get(0).contains("COBAS INTEGRA400");
  }

  @Override
  public AnalyzerLineInserter getAnalyzerLineInserter() {
    return new CobasIntegra400Implementation();
  }
}
