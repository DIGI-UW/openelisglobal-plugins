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
import java.util.regex.Pattern;
import org.openelisglobal.analyzerimport.analyzerreaders.AnalyzerLineInserter;
import org.openelisglobal.analyzerimport.analyzerreaders.AnalyzerResponder;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.services.PluginAnalyzerService;
import org.openelisglobal.plugin.AnalyzerImporterPlugin;

public class PocH100iAnalyzer implements AnalyzerImporterPlugin {

  public static final String ANALYZER_NAME = "PocH100iAnalyzer";

  @Override
  public boolean connect() {
    List<PluginAnalyzerService.TestMapping> nameMapping = new ArrayList<>();
    nameMapping.add(
        new PluginAnalyzerService.TestMapping(
            PocH100iAnalyzerImplementation.ANALYZER_TEST_WBC,
            "White Blood Cells Count (WBC)",
            PocH100iAnalyzerImplementation.LOINC_WBC));
    nameMapping.add(
        new PluginAnalyzerService.TestMapping(
            PocH100iAnalyzerImplementation.ANALYZER_TEST_RBC,
            "Red Blood Cells Count (RBC)",
            PocH100iAnalyzerImplementation.LOINC_RBC));
    nameMapping.add(
        new PluginAnalyzerService.TestMapping(
            PocH100iAnalyzerImplementation.ANALYZER_TEST_HGB,
            "Hemoglobin",
            PocH100iAnalyzerImplementation.LOINC_HGB));
    nameMapping.add(
        new PluginAnalyzerService.TestMapping(
            PocH100iAnalyzerImplementation.ANALYZER_TEST_HCT,
            "Hematocrit",
            PocH100iAnalyzerImplementation.LOINC_HCT));
    nameMapping.add(
        new PluginAnalyzerService.TestMapping(
            PocH100iAnalyzerImplementation.ANALYZER_TEST_MCV,
            "Medium corpuscular volum",
            PocH100iAnalyzerImplementation.LOINC_MCV));
    nameMapping.add(
        new PluginAnalyzerService.TestMapping(
            PocH100iAnalyzerImplementation.ANALYZER_TEST_MCH,
            "",
            PocH100iAnalyzerImplementation.LOINC_MCH));
    nameMapping.add(
        new PluginAnalyzerService.TestMapping(
            PocH100iAnalyzerImplementation.ANALYZER_TEST_MCHC,
            "",
            PocH100iAnalyzerImplementation.LOINC_MCHC));
    nameMapping.add(
        new PluginAnalyzerService.TestMapping(
            PocH100iAnalyzerImplementation.ANALYZER_TEST_RDWSD,
            "",
            PocH100iAnalyzerImplementation.LOINC_RDWSD));
    nameMapping.add(
        new PluginAnalyzerService.TestMapping(
            PocH100iAnalyzerImplementation.ANALYZER_TEST_RDWCV,
            "",
            PocH100iAnalyzerImplementation.LOINC_RDWCV));
    nameMapping.add(
        new PluginAnalyzerService.TestMapping(
            PocH100iAnalyzerImplementation.ANALYZER_TEST_PLT,
            "Platelets",
            PocH100iAnalyzerImplementation.LOINC_PLT));
    nameMapping.add(
        new PluginAnalyzerService.TestMapping(
            PocH100iAnalyzerImplementation.ANALYZER_TEST_MPV,
            "",
            PocH100iAnalyzerImplementation.LOINC_MPV));
    nameMapping.add(
        new PluginAnalyzerService.TestMapping(
            PocH100iAnalyzerImplementation.ANALYZER_TEST_NEUT_COUNT,
            "Neutrophiles",
            PocH100iAnalyzerImplementation.LOINC_NEUT_COUNT));
    nameMapping.add(
        new PluginAnalyzerService.TestMapping(
            PocH100iAnalyzerImplementation.ANALYZER_TEST_NEUT_PERCENT,
            "Neutrophiles (%)",
            PocH100iAnalyzerImplementation.LOINC_NEUT_PERCENT));
    nameMapping.add(
        new PluginAnalyzerService.TestMapping(
            PocH100iAnalyzerImplementation.ANALYZER_TEST_LYMPH_COUNT,
            "Lymphocytes (Abs)",
            PocH100iAnalyzerImplementation.LOINC_LYMPH_COUNT));
    nameMapping.add(
        new PluginAnalyzerService.TestMapping(
            PocH100iAnalyzerImplementation.ANALYZER_TEST_LYMPH_PERCENT,
            "Lymphocytes (%)",
            PocH100iAnalyzerImplementation.LOINC_LYMPH_PERCENT));
    nameMapping.add(
        new PluginAnalyzerService.TestMapping(
            PocH100iAnalyzerImplementation.ANALYZER_TEST_MONO_COUNT,
            "Monocytes (Abs)",
            PocH100iAnalyzerImplementation.LOINC_MONO_COUNT));
    nameMapping.add(
        new PluginAnalyzerService.TestMapping(
            PocH100iAnalyzerImplementation.ANALYZER_TEST_MONO_PERCENT,
            "Monocytes (%)",
            PocH100iAnalyzerImplementation.LOINC_MONO_PERCENT));
    nameMapping.add(
        new PluginAnalyzerService.TestMapping(
            PocH100iAnalyzerImplementation.ANALYZER_TEST_EO_COUNT,
            "Eosinophiles",
            PocH100iAnalyzerImplementation.LOINC_EO_COUNT));
    nameMapping.add(
        new PluginAnalyzerService.TestMapping(
            PocH100iAnalyzerImplementation.ANALYZER_TEST_EO_PERCENT,
            "Eosinophiles (%)",
            PocH100iAnalyzerImplementation.LOINC_EO_PERCENT));
    nameMapping.add(
        new PluginAnalyzerService.TestMapping(
            PocH100iAnalyzerImplementation.ANALYZER_TEST_BASO_COUNT,
            "Basophiles",
            PocH100iAnalyzerImplementation.LOINC_BASO_COUNT));
    nameMapping.add(
        new PluginAnalyzerService.TestMapping(
            PocH100iAnalyzerImplementation.ANALYZER_TEST_BASO_PERCENT,
            "Basophiles (%)",
            PocH100iAnalyzerImplementation.LOINC_BASO_PERCENT));
    nameMapping.add(
        new PluginAnalyzerService.TestMapping(
            PocH100iAnalyzerImplementation.ANALYZER_TEST_IG_COUNT,
            "",
            PocH100iAnalyzerImplementation.LOINC_IG_COUNT));
    nameMapping.add(
        new PluginAnalyzerService.TestMapping(
            PocH100iAnalyzerImplementation.ANALYZER_TEST_IG_PERCENT,
            "",
            PocH100iAnalyzerImplementation.LOINC_IG_PERCENT));
    nameMapping.add(
        new PluginAnalyzerService.TestMapping(
            PocH100iAnalyzerImplementation.ANALYZER_TEST_MXD_COUNT,
            "",
            PocH100iAnalyzerImplementation.LOINC_MXD_COUNT));
    nameMapping.add(
        new PluginAnalyzerService.TestMapping(
            PocH100iAnalyzerImplementation.ANALYZER_TEST_MXD_PERCENT,
            "",
            PocH100iAnalyzerImplementation.LOINC_MXD_PERCENT));
    getInstance().addAnalyzerDatabaseParts(ANALYZER_NAME, ANALYZER_NAME, nameMapping, true);
    getInstance().registerAnalyzer(this);
    return true;
  }

  @Override
  public boolean isTargetAnalyzer(List<String> lines) {
    for (String line : lines) {
      if (line.startsWith(PocH100iAnalyzerImplementation.HEADER_RECORD_IDENTIFIER)) {
        String[] headerRecord = line.split(Pattern.quote(PocH100iAnalyzerImplementation.FD));
        if (headerRecord.length < 5) {
          LogEvent.logTrace(
              this.getClass().getSimpleName(),
              "isTargetAnalyzer",
              "incoming message is not PocH100i: header record not long enough");
          return false;
        }
        String[] senderNameFields =
            headerRecord[4].split(Pattern.quote(PocH100iAnalyzerImplementation.CD));
        if (senderNameFields.length < 1) {
          LogEvent.logTrace(
              this.getClass().getSimpleName(),
              "isTargetAnalyzer",
              "incoming message is not PocH100i: sender name field not long enough");
          return false;
        }
        String senderName = senderNameFields[0].trim();
        LogEvent.logTrace(
            this.getClass().getSimpleName(),
            "isTargetAnalyzer",
            "incoming message analyzer name is " + senderName);
        if (senderName.equalsIgnoreCase("pocH-100i")) {
          LogEvent.logTrace(
              this.getClass().getSimpleName(), "isTargetAnalyzer", "incoming message is PocH100i");
          return true;
        }
        LogEvent.logTrace(
            this.getClass().getSimpleName(),
            "isTargetAnalyzer",
            "incoming message is not PocH100i: sender name doesn't match");
        return false;
      }
    }
    LogEvent.logTrace(
        this.getClass().getSimpleName(),
        "isTargetAnalyzer",
        "incoming message is not PocH100i: no header line");
    return false;
  }

  @Override
  public boolean isAnalyzerResult(List<String> lines) {
    for (String line : lines) {
      if (line.startsWith(PocH100iAnalyzerImplementation.RESULT_RECORD_IDENTIFIER)) {
        return true;
      }
    }
    LogEvent.logDebug(
        this.getClass().getSimpleName(),
        "isAnalyzerResult",
        "no result recoord identifier located");
    return false;
  }

  @Override
  public AnalyzerLineInserter getAnalyzerLineInserter() {
    return new PocH100iAnalyzerImplementation();
  }

  @Override
  public AnalyzerResponder getAnalyzerResponder() {
    return new PocH100iAnalyzerImplementation();
  }
}
