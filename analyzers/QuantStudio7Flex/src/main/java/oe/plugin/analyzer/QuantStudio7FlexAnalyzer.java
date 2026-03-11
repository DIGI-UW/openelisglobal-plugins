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
 * Copyright (C) ITECH, University of Washington, Seattle WA. All Rights Reserved.
 */

package oe.plugin.analyzer;

import static org.openelisglobal.common.services.PluginAnalyzerService.getInstance;

import java.util.ArrayList;
import java.util.List;
import org.openelisglobal.analyzerimport.analyzerreaders.AnalyzerLineInserter;
import org.openelisglobal.common.services.PluginAnalyzerService;
import org.openelisglobal.plugin.AnalyzerImporterPlugin;

/**
 * QuantStudio7FlexAnalyzer - Plugin for Thermo Fisher QuantStudio 7 Flex Real-Time PCR System.
 *
 * <p>This plugin adapts the QuantStudio3 plugin to support QuantStudio 7 Flex CSV export format.
 * The QS7 Flex has additional columns compared to QS3 but the core parsing logic is similar.
 *
 * <p>Expected CSV format (tab-delimited): Well Well Position Sample Name Target Task Reporter
 * Quencher Amp Status CT Ct Mean Ct SD ...
 *
 * <p>M8 Milestone: Madagascar Analyzer Integration (Feature 011)
 */
public class QuantStudio7FlexAnalyzer implements AnalyzerImporterPlugin {

  @Override
  public boolean connect() {
    List<PluginAnalyzerService.TestMapping> nameMapping = new ArrayList<>();

    // SARS-CoV-2 test mapping (same as QuantStudio3)
    nameMapping.add(
        new PluginAnalyzerService.TestMapping(
            "SARS-CoV-2 (COVID-19) RNA",
            "SARS-CoV-2 (COVID-19) RNA [Presence] in Respiratory specimen by qRT-PCR",
            QuantStudio7FlexAnalyzerImplementation.SARSCOV2_LOINC));

    getInstance()
        .addAnalyzerDatabaseParts(
            "QuantStudio7FlexAnalyzer",
            "QuantStudio 7 Flex Real-Time PCR System - 384 Wells",
            nameMapping,
            true);
    getInstance().registerAnalyzer(this);

    return true;
  }

  @Override
  public boolean isTargetAnalyzer(List<String> lines) {
    for (String line : lines) {
      // Check for QuantStudio 7 Flex specific patterns
      if (line.contains("Instrument Type")
          && (line.matches("^.*QuantStudio.?\\s+7\\s+Flex.*")
              || line.matches("^.*QuantStudio.?\\s+7.*"))) {
        return true;
      }
      // Also check for QS7 Flex specific column headers (Well Position + Target/Target Name)
      if (line.contains("Well Position")
          && (line.contains("Target") || line.contains("Target Name"))
          && (line.contains("Amp Status") || line.contains("Sample Name"))) {
        return true;
      }
    }
    return false;
  }

  @Override
  public AnalyzerLineInserter getAnalyzerLineInserter() {
    return new QuantStudio7FlexAnalyzerImplementation();
  }

  public int getColumnsLine(List<String> lines) {
    for (int k = 0; k < lines.size(); k++) {
      // Looking for header columns that are used for QS7 Flex
      if (lines.get(k).contains("Sample Name")
          && lines.get(k).contains("Target")
          && lines.get(k).contains("CT")
          && lines.get(k).contains("Amp Status")) {
        return k;
      }
      // Fallback to QS3 pattern
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
