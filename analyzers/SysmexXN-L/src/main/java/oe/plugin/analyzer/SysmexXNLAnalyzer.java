/*
 * The contents of this file are subject to the Mozilla Public License
 * Version 1.1 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.mozilla.org/MPL/
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either eXNLress or implied. See the
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

public class SysmexXNLAnalyzer implements AnalyzerImporterPlugin {

  public static final String ANALYZER_NAME = "SysmexXNLAnalyzer";

  @Override
  public boolean connect() {
    List<PluginAnalyzerService.TestMapping> nameMapping = new ArrayList<>();
    nameMapping.add(
        new PluginAnalyzerService.TestMapping(
            SysmexXNLAnalyzerImplementation.ANALYZER_TEST_WBC,
            "White Blood Cells Count (WBC)",
            SysmexXNLAnalyzerImplementation.LOINC_WBC));
    nameMapping.add(
        new PluginAnalyzerService.TestMapping(
            SysmexXNLAnalyzerImplementation.ANALYZER_TEST_RBC,
            "Red Blood Cells Count (RBC)",
            SysmexXNLAnalyzerImplementation.LOINC_RBC));
    nameMapping.add(
        new PluginAnalyzerService.TestMapping(
            SysmexXNLAnalyzerImplementation.ANALYZER_TEST_HGB,
            "Hemoglobin",
            SysmexXNLAnalyzerImplementation.LOINC_HGB));
    nameMapping.add(
        new PluginAnalyzerService.TestMapping(
            SysmexXNLAnalyzerImplementation.ANALYZER_TEST_HCT,
            "Hematocrit",
            SysmexXNLAnalyzerImplementation.LOINC_HCT));
    nameMapping.add(
        new PluginAnalyzerService.TestMapping(
            SysmexXNLAnalyzerImplementation.ANALYZER_TEST_MCV,
            "Medium corpuscular volum",
            SysmexXNLAnalyzerImplementation.LOINC_MCV));
    nameMapping.add(
        new PluginAnalyzerService.TestMapping(
            SysmexXNLAnalyzerImplementation.ANALYZER_TEST_MCH,
            "",
            SysmexXNLAnalyzerImplementation.LOINC_MCH));
    nameMapping.add(
        new PluginAnalyzerService.TestMapping(
            SysmexXNLAnalyzerImplementation.ANALYZER_TEST_MCHC,
            "",
            SysmexXNLAnalyzerImplementation.LOINC_MCHC));
    nameMapping.add(
        new PluginAnalyzerService.TestMapping(
            SysmexXNLAnalyzerImplementation.ANALYZER_TEST_RDWSD,
            "",
            SysmexXNLAnalyzerImplementation.LOINC_RDWSD));
    nameMapping.add(
        new PluginAnalyzerService.TestMapping(
            SysmexXNLAnalyzerImplementation.ANALYZER_TEST_RDWCV,
            "",
            SysmexXNLAnalyzerImplementation.LOINC_RDWCV));
    nameMapping.add(
        new PluginAnalyzerService.TestMapping(
            SysmexXNLAnalyzerImplementation.ANALYZER_TEST_PLT,
            "Platelets",
            SysmexXNLAnalyzerImplementation.LOINC_PLT));
    nameMapping.add(
        new PluginAnalyzerService.TestMapping(
            SysmexXNLAnalyzerImplementation.ANALYZER_TEST_MPV,
            "",
            SysmexXNLAnalyzerImplementation.LOINC_MPV));
    nameMapping.add(
        new PluginAnalyzerService.TestMapping(
            SysmexXNLAnalyzerImplementation.ANALYZER_TEST_NEUT_COUNT,
            "Neutrophiles",
            SysmexXNLAnalyzerImplementation.LOINC_NEUT_COUNT));
    nameMapping.add(
        new PluginAnalyzerService.TestMapping(
            SysmexXNLAnalyzerImplementation.ANALYZER_TEST_NEUT_PERCENT,
            "Neutrophiles (%)",
            SysmexXNLAnalyzerImplementation.LOINC_NEUT_PERCENT));
    nameMapping.add(
        new PluginAnalyzerService.TestMapping(
            SysmexXNLAnalyzerImplementation.ANALYZER_TEST_LYMPH_COUNT,
            "Lymphocytes (Abs)",
            SysmexXNLAnalyzerImplementation.LOINC_LYMPH_COUNT));
    nameMapping.add(
        new PluginAnalyzerService.TestMapping(
            SysmexXNLAnalyzerImplementation.ANALYZER_TEST_LYMPH_PERCENT,
            "Lymphocytes (%)",
            SysmexXNLAnalyzerImplementation.LOINC_LYMPH_PERCENT));
    nameMapping.add(
        new PluginAnalyzerService.TestMapping(
            SysmexXNLAnalyzerImplementation.ANALYZER_TEST_MONO_COUNT,
            "Monocytes (Abs)",
            SysmexXNLAnalyzerImplementation.LOINC_MONO_COUNT));
    nameMapping.add(
        new PluginAnalyzerService.TestMapping(
            SysmexXNLAnalyzerImplementation.ANALYZER_TEST_MONO_PERCENT,
            "Monocytes (%)",
            SysmexXNLAnalyzerImplementation.LOINC_MONO_PERCENT));
    nameMapping.add(
        new PluginAnalyzerService.TestMapping(
            SysmexXNLAnalyzerImplementation.ANALYZER_TEST_EO_COUNT,
            "Eosinophiles",
            SysmexXNLAnalyzerImplementation.LOINC_EO_COUNT));
    nameMapping.add(
        new PluginAnalyzerService.TestMapping(
            SysmexXNLAnalyzerImplementation.ANALYZER_TEST_EO_PERCENT,
            "Eosinophiles (%)",
            SysmexXNLAnalyzerImplementation.LOINC_EO_PERCENT));
    nameMapping.add(
        new PluginAnalyzerService.TestMapping(
            SysmexXNLAnalyzerImplementation.ANALYZER_TEST_BASO_COUNT,
            "Basophiles",
            SysmexXNLAnalyzerImplementation.LOINC_BASO_COUNT));
    nameMapping.add(
        new PluginAnalyzerService.TestMapping(
            SysmexXNLAnalyzerImplementation.ANALYZER_TEST_BASO_PERCENT,
            "Basophiles (%)",
            SysmexXNLAnalyzerImplementation.LOINC_BASO_PERCENT));
    nameMapping.add(
        new PluginAnalyzerService.TestMapping(
            SysmexXNLAnalyzerImplementation.ANALYZER_TEST_IG_COUNT,
            "",
            SysmexXNLAnalyzerImplementation.LOINC_IG_COUNT));
    nameMapping.add(
        new PluginAnalyzerService.TestMapping(
            SysmexXNLAnalyzerImplementation.ANALYZER_TEST_IG_PERCENT,
            "",
            SysmexXNLAnalyzerImplementation.LOINC_IG_PERCENT));
    nameMapping.add(
        new PluginAnalyzerService.TestMapping(
            SysmexXNLAnalyzerImplementation.ANALYZER_TEST_RET_COUNT,
            "",
            SysmexXNLAnalyzerImplementation.LOINC_RET_COUNT));
    nameMapping.add(
        new PluginAnalyzerService.TestMapping(
            SysmexXNLAnalyzerImplementation.ANALYZER_TEST_RET_PERCENT,
            "",
            SysmexXNLAnalyzerImplementation.LOINC_RET_PERCENT));
    nameMapping.add(
        new PluginAnalyzerService.TestMapping(
            SysmexXNLAnalyzerImplementation.ANALYZER_TEST_IRF,
            "",
            SysmexXNLAnalyzerImplementation.LOINC_IRF));
    nameMapping.add(
        new PluginAnalyzerService.TestMapping(
            SysmexXNLAnalyzerImplementation.ANALYZER_TEST_RETHE,
            "",
            SysmexXNLAnalyzerImplementation.LOINC_RETHE));
    nameMapping.add(
        new PluginAnalyzerService.TestMapping(
            SysmexXNLAnalyzerImplementation.ANALYZER_TEST_WBCBF,
            "",
            SysmexXNLAnalyzerImplementation.LOINC_WBCBF));
    nameMapping.add(
        new PluginAnalyzerService.TestMapping(
            SysmexXNLAnalyzerImplementation.ANALYZER_TEST_RBCBF,
            "",
            SysmexXNLAnalyzerImplementation.LOINC_RBCBF));
    nameMapping.add(
        new PluginAnalyzerService.TestMapping(
            SysmexXNLAnalyzerImplementation.ANALYZER_TEST_MN_COUNT,
            "",
            SysmexXNLAnalyzerImplementation.LOINC_MN_COUNT));
    nameMapping.add(
        new PluginAnalyzerService.TestMapping(
            SysmexXNLAnalyzerImplementation.ANALYZER_TEST_MN_PERCENT,
            "",
            SysmexXNLAnalyzerImplementation.LOINC_MN_PERCENT));
    nameMapping.add(
        new PluginAnalyzerService.TestMapping(
            SysmexXNLAnalyzerImplementation.ANALYZER_TEST_PMN_COUNT,
            "",
            SysmexXNLAnalyzerImplementation.LOINC_PMN_COUNT));
    nameMapping.add(
        new PluginAnalyzerService.TestMapping(
            SysmexXNLAnalyzerImplementation.ANALYZER_TEST_PMN_PERCENT,
            "",
            SysmexXNLAnalyzerImplementation.LOINC_PMN_PERCENT));
    nameMapping.add(
        new PluginAnalyzerService.TestMapping(
            SysmexXNLAnalyzerImplementation.ANALYZER_TEST_TCBF_COUNT,
            "",
            SysmexXNLAnalyzerImplementation.LOINC_TCBF_COUNT));
    getInstance().addAnalyzerDatabaseParts(ANALYZER_NAME, ANALYZER_NAME, nameMapping, true);
    getInstance().registerAnalyzer(this);
    return true;
  }

  @Override
  public boolean isTargetAnalyzer(List<String> lines) {
    for (String line : lines) {
      if (line.startsWith(SysmexXNLAnalyzerImplementation.HEADER_RECORD_IDENTIFIER)) {
        String[] headerRecord = line.split(Pattern.quote(SysmexXNLAnalyzerImplementation.FD));
        if (headerRecord.length < 5) {
          LogEvent.logTrace(
              this.getClass().getSimpleName(),
              "isTargetAnalyzer",
              "incoming message is not SysmexXN: header record not long enough");
          return false;
        }
        String[] senderNameFields =
            headerRecord[4].split(Pattern.quote(SysmexXNLAnalyzerImplementation.CD));
        if (senderNameFields.length < 1) {
          LogEvent.logTrace(
              this.getClass().getSimpleName(),
              "isTargetAnalyzer",
              "incoming message is not SysmexXN: sender name field not long enough");
          return false;
        }
        String senderName = senderNameFields[0].trim();

        LogEvent.logTrace(
            this.getClass().getSimpleName(),
            "isTargetAnalyzer",
            "incoming message analyzer name is " + senderName);
        if (senderName.equalsIgnoreCase("XN-550")) {
          LogEvent.logTrace(
              this.getClass().getSimpleName(),
              "isTargetAnalyzer",
              "incoming message is SysmexXN (XN-350)");
          return true;
        } else if (senderName.equalsIgnoreCase("XN-530")) {
          LogEvent.logTrace(
              this.getClass().getSimpleName(),
              "isTargetAnalyzer",
              "incoming message is SysmexXN (XN-350)");
          return true;
        } else if (senderName.equalsIgnoreCase("XN-450")) {
          LogEvent.logTrace(
              this.getClass().getSimpleName(),
              "isTargetAnalyzer",
              "incoming message is SysmexXN (XN-350)");
          return true;
        } else if (senderName.equalsIgnoreCase("XN-430")) {
          LogEvent.logTrace(
              this.getClass().getSimpleName(),
              "isTargetAnalyzer",
              "incoming message is SysmexXN (XN-350)");
          return true;
        } else if (senderName.equalsIgnoreCase("XN-350")) {
          LogEvent.logTrace(
              this.getClass().getSimpleName(),
              "isTargetAnalyzer",
              "incoming message is SysmexXN (XN-350)");
          return true;
        } else if (senderName.equalsIgnoreCase("XN-330")) {
          LogEvent.logTrace(
              this.getClass().getSimpleName(),
              "isTargetAnalyzer",
              "incoming message is SysmexXN (XN-350)");
          return true;
        } else if (senderName.equalsIgnoreCase("XN-150")) {
          LogEvent.logTrace(
              this.getClass().getSimpleName(),
              "isTargetAnalyzer",
              "incoming message is SysmexXN (XN-350)");
          return true;
        } else if (senderName.equalsIgnoreCase("XN-110")) {
          LogEvent.logTrace(
              this.getClass().getSimpleName(),
              "isTargetAnalyzer",
              "incoming message is SysmexXN (XN-350)");
          return true;
        }
        LogEvent.logTrace(
            this.getClass().getSimpleName(),
            "isTargetAnalyzer",
            "incoming message is not SysmexXN: sender name doesn't match");
        return false;
      }
    }
    LogEvent.logTrace(
        this.getClass().getSimpleName(),
        "isTargetAnalyzer",
        "incoming message is not SysmexXN: no header line");
    return false;
  }

  @Override
  public boolean isAnalyzerResult(List<String> lines) {
    for (String line : lines) {
      if (line.startsWith(SysmexXNLAnalyzerImplementation.RESULT_RECORD_IDENTIFIER)) {
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
    return new SysmexXNLAnalyzerImplementation();
  }

  @Override
  public AnalyzerResponder getAnalyzerResponder() {
    return new SysmexXNLAnalyzerImplementation();
  }
}
