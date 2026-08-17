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
package uw.edu.itech.AbbottArchitect;

import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.openelisglobal.analyzerimport.analyzerreaders.AnalyzerLineInserter;
import org.openelisglobal.common.services.PluginAnalyzerService;
import org.openelisglobal.plugin.AnalyzerImporterPlugin;

/**
 * External plugin JAR for Abbott Architect immunoassay analyzers (HL7 ORU^R01).
 *
 * <p>Feature: 011-madagascar-analyzer-integration Milestone: M12 (Abbott Architect)
 *
 * <p>Identification: HL7 MSH-3 (sending application) contains "ARCHITECT".
 */
public class AbbottArchitectAnalyzer implements AnalyzerImporterPlugin {

  /** Analyzer name used for database registration and identification */
  public static final String ANALYZER_NAME = "Abbott Architect";

  /** Description for database registration */
  public static final String ANALYZER_DESCRIPTION =
      "Abbott Architect immunoassay analyzer (HL7 ORU^R01)";

  private static final String MSH_SEGMENT_PREFIX = "MSH|";
  private static final int MSH_SENDING_APPLICATION_FIELD = 2;
  private static final int MSH_SENDING_FACILITY_FIELD = 3;
  private static final String IDENTIFIER_PRIMARY = "ARCHITECT";
  private static final String IDENTIFIER_FALLBACK = "ABBOTT";

  @Override
  public boolean connect() {
    List<PluginAnalyzerService.TestMapping> testMappings = createTestMappings();

    PluginAnalyzerService.getInstance()
        .addAnalyzerDatabaseParts(ANALYZER_NAME, ANALYZER_DESCRIPTION, testMappings, true);

    PluginAnalyzerService.getInstance().registerAnalyzer(this);
    return true;
  }

  private List<PluginAnalyzerService.TestMapping> createTestMappings() {
    List<PluginAnalyzerService.TestMapping> mappings = new ArrayList<>();

    // Default mappings (can be overridden via CSV or UI mapping configuration).
    mappings.add(new PluginAnalyzerService.TestMapping("HIV", "HIV"));
    mappings.add(new PluginAnalyzerService.TestMapping("HBSAG", "HBSAG"));
    mappings.add(new PluginAnalyzerService.TestMapping("VDRL", "VDRL"));

    return mappings;
  }

  @Override
  public boolean isTargetAnalyzer(List<String> lines) {
    if (lines == null || lines.isEmpty()) {
      return false;
    }

    for (String line : lines) {
      if (StringUtils.isBlank(line) || !line.startsWith(MSH_SEGMENT_PREFIX)) {
        continue;
      }
      String[] fields = line.split("\\|", -1);
      String sendingApp =
          fields.length > MSH_SENDING_APPLICATION_FIELD
              ? fields[MSH_SENDING_APPLICATION_FIELD]
              : "";
      String sendingFacility =
          fields.length > MSH_SENDING_FACILITY_FIELD ? fields[MSH_SENDING_FACILITY_FIELD] : "";

      String identifier = (sendingApp + " " + sendingFacility).toUpperCase();
      if (identifier.contains(IDENTIFIER_PRIMARY) || identifier.contains(IDENTIFIER_FALLBACK)) {
        return true;
      }
    }

    return false;
  }

  @Override
  public boolean isAnalyzerResult(List<String> lines) {
    if (lines == null || lines.isEmpty()) {
      return false;
    }
    return lines.stream().anyMatch(line -> line != null && line.startsWith("OBX|"));
  }

  @Override
  public AnalyzerLineInserter getAnalyzerLineInserter() {
    return new AbbottArchitectAnalyzerLineInserter();
  }
}
