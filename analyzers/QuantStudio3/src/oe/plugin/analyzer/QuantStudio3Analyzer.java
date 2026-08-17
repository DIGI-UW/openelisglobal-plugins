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

public class QuantStudio3Analyzer implements AnalyzerImporterPlugin {

  @Override
  public boolean connect() {
    List<PluginAnalyzerService.TestMapping> nameMapping = new ArrayList<>();
    nameMapping.add(
        new PluginAnalyzerService.TestMapping(
            "SARS-CoV-2 (COVID-19) RNA",
            "SARS-CoV-2 (COVID-19) RNA [Presence] in Respiratory specimen by qRT-PCR",
            QuantStudio3AnalyzerImplementation.TEST_LOINC));
    getInstance()
        .addAnalyzerDatabaseParts(
            "QuantStudio3Analyzer", "QuantStudio 3 Covid PCR - 96 Wells", nameMapping, true);
    getInstance().registerAnalyzer(this);
    return true;
  }

  @Override
  public boolean isTargetAnalyzer(List<String> lines) {
    for (String line : lines) {
      if (line.contains("Instrument Type") && line.matches("^.*QuantStudio.?\\s+3\\s+System.*")) {
        return true;
      }
    }
    return false;
  }

  @Override
  public AnalyzerLineInserter getAnalyzerLineInserter() {
    return new QuantStudio3AnalyzerImplementation();
  }

  public int getColumnsLine(List<String> lines) {
    for (int k = 0; k < lines.size(); k++) {
      // looking for header columns that are used for this
      if (lines.get(k).contains("Sample Name")
          && lines.get(k).contains("CT")
          && lines.get(k).contains("Ct Mean")
          && lines.get(k).contains("Ct SD")) {
        return k;
      }
    }

    return -1;
  }
}
