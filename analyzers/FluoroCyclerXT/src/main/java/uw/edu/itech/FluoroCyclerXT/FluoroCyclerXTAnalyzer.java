/**
 * The contents of this file are subject to the Mozilla Public License Version 1.1 (the "License");
 * you may not use this file except in compliance with the License. You may obtain a copy of the
 * License at http://www.mozilla.org/MPL/
 *
 * <p>Software distributed under the License is distributed on an "AS IS" basis, WITHOUT WARRANTY OF
 * ANY KIND, either express or implied. See the License for the specific language governing rights
 * and limitations under the License.
 *
 * <p>The Original Code is OpenELIS code.
 *
 * <p>Copyright (C) CIRG, University of Washington, Seattle WA. All Rights Reserved.
 */
package uw.edu.itech.FluoroCyclerXT;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.openelisglobal.analyzerimport.analyzerreaders.AnalyzerLineInserter;
import org.openelisglobal.common.services.PluginAnalyzerService;
import org.openelisglobal.plugin.AnalyzerImporterPlugin;

/**
 * External plugin JAR for Hain FluoroCycler XT (file-based CSV import).
 *
 * <p>Feature: 011-madagascar-analyzer-integration Milestone: M13 (FluoroCycler XT)
 *
 * <p>Format reference: specs/011-madagascar-analyzer-integration/research.md
 */
public class FluoroCyclerXTAnalyzer implements AnalyzerImporterPlugin {

  /** Analyzer name used for database registration and identification. */
  public static final String ANALYZER_NAME = "Hain FluoroCycler XT";

  /** Description for database registration. */
  public static final String ANALYZER_DESCRIPTION =
      "Hain FluoroCycler XT PCR Analyzer (file-based CSV import)";

  public static final String RESULT_TEST_NAME = "RESULT";
  public static final String INTERPRETATION_TEST_NAME = "INTERPRETATION";

  @Override
  public boolean connect() {
    List<PluginAnalyzerService.TestMapping> testMappings = new ArrayList<>();
    testMappings.add(
        new PluginAnalyzerService.TestMapping(RESULT_TEST_NAME, "FluoroCycler XT Result"));
    testMappings.add(
        new PluginAnalyzerService.TestMapping(
            INTERPRETATION_TEST_NAME, "FluoroCycler XT Interpretation"));

    String analyzerId =
        PluginAnalyzerService.getInstance()
            .addAnalyzerDatabaseParts(ANALYZER_NAME, ANALYZER_DESCRIPTION, testMappings, true);

    PluginAnalyzerService.getInstance().registerAnalyzer(this, Optional.ofNullable(analyzerId));

    return true;
  }

  @Override
  public boolean isTargetAnalyzer(List<String> lines) {
    if (lines == null || lines.isEmpty()) {
      return false;
    }

    for (String line : lines) {
      if (line == null || line.isEmpty()) {
        continue;
      }
      // Check for explicit FluoroCycler identifier
      if (line.toLowerCase().contains("fluorocycler")) {
        return true;
      }
      // Check for expected header row with column names
      String lowerLine = line.toLowerCase();
      if (lowerLine.contains("sample") && lowerLine.contains("result")) {
        if (lowerLine.contains("interpretation") || lowerLine.contains("position")) {
          return true;
        }
      }
    }
    return false;
  }

  @Override
  public AnalyzerLineInserter getAnalyzerLineInserter() {
    return new FluoroCyclerXTAnalyzerLineInserter();
  }
}
