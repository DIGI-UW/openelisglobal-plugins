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

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.openelisglobal.analyzerimport.analyzerreaders.AnalyzerLineInserter;
import org.openelisglobal.common.services.PluginAnalyzerService;
import org.openelisglobal.plugin.AnalyzerImporterPlugin;
import org.openelisglobal.analyzerimport.analyzerreaders.AnalyzerResponder;
import org.openelisglobal.common.log.LogEvent;

public class GeneXpertAnalyzer implements AnalyzerImporterPlugin {
	
	public static final String ANALYZER_NAME = "GeneXpertAnalyzer";

    @Override
	public boolean connect() { 
		List<PluginAnalyzerService.TestMapping> nameMapping = new ArrayList<>();
		nameMapping.add(
				new PluginAnalyzerService.TestMapping(GeneXpertAnalyzerImplementation.ANALYZER_TEST_WBC, "White Blood Cells Count (WBC)",
						GeneXpertAnalyzerImplementation.LOINC_WBC));
		nameMapping.add(
				new PluginAnalyzerService.TestMapping(GeneXpertAnalyzerImplementation.ANALYZER_TEST_RBC, "Red Blood Cells Count (RBC)",
						GeneXpertAnalyzerImplementation.LOINC_RBC));
		nameMapping.add(
				new PluginAnalyzerService.TestMapping(GeneXpertAnalyzerImplementation.ANALYZER_TEST_HGB, "Hemoglobin",
				GeneXpertAnalyzerImplementation.LOINC_HGB));
		nameMapping.add(
				new PluginAnalyzerService.TestMapping(GeneXpertAnalyzerImplementation.ANALYZER_TEST_HCT, "Hematocrit",
						GeneXpertAnalyzerImplementation.LOINC_HCT));
		nameMapping.add(
				new PluginAnalyzerService.TestMapping(GeneXpertAnalyzerImplementation.ANALYZER_TEST_MCV, "Medium corpuscular volum",
						GeneXpertAnalyzerImplementation.LOINC_MCV));
		nameMapping.add(
				new PluginAnalyzerService.TestMapping(GeneXpertAnalyzerImplementation.ANALYZER_TEST_MCH, "",
						GeneXpertAnalyzerImplementation.LOINC_MCH));
		nameMapping
				.add(new PluginAnalyzerService.TestMapping(GeneXpertAnalyzerImplementation.ANALYZER_TEST_MCHC, "",
						GeneXpertAnalyzerImplementation.LOINC_MCHC));
		nameMapping
				.add(new PluginAnalyzerService.TestMapping(GeneXpertAnalyzerImplementation.ANALYZER_TEST_RDWSD, "",
						GeneXpertAnalyzerImplementation.LOINC_RDWSD));
		nameMapping.add(
				new PluginAnalyzerService.TestMapping(GeneXpertAnalyzerImplementation.ANALYZER_TEST_RDWCV, "",
						GeneXpertAnalyzerImplementation.LOINC_RDWCV));
		nameMapping
				.add(new PluginAnalyzerService.TestMapping(GeneXpertAnalyzerImplementation.ANALYZER_TEST_PLT, "Platelets",
						GeneXpertAnalyzerImplementation.LOINC_PLT));
		nameMapping.add(
				new PluginAnalyzerService.TestMapping(GeneXpertAnalyzerImplementation.ANALYZER_TEST_MPV, "",
						GeneXpertAnalyzerImplementation.LOINC_MPV));
		nameMapping.add(
				new PluginAnalyzerService.TestMapping(GeneXpertAnalyzerImplementation.ANALYZER_TEST_NEUT_COUNT, "Neutrophiles",
				GeneXpertAnalyzerImplementation.LOINC_NEUT_COUNT));
		nameMapping.add(
				new PluginAnalyzerService.TestMapping(GeneXpertAnalyzerImplementation.ANALYZER_TEST_NEUT_PERCENT, "Neutrophiles (%)",
						GeneXpertAnalyzerImplementation.LOINC_NEUT_PERCENT));
		nameMapping.add(
				new PluginAnalyzerService.TestMapping(GeneXpertAnalyzerImplementation.ANALYZER_TEST_LYMPH_COUNT, "Lymphocytes (Abs)",
						GeneXpertAnalyzerImplementation.LOINC_LYMPH_COUNT));
		nameMapping.add(
				new PluginAnalyzerService.TestMapping(GeneXpertAnalyzerImplementation.ANALYZER_TEST_LYMPH_PERCENT, "Lymphocytes (%)",
						GeneXpertAnalyzerImplementation.LOINC_LYMPH_PERCENT));
		nameMapping.add(
				new PluginAnalyzerService.TestMapping(GeneXpertAnalyzerImplementation.ANALYZER_TEST_MONO_COUNT, "Monocytes (Abs)",
						GeneXpertAnalyzerImplementation.LOINC_MONO_COUNT));
		nameMapping.add(
				new PluginAnalyzerService.TestMapping(GeneXpertAnalyzerImplementation.ANALYZER_TEST_MONO_PERCENT, "Monocytes (%)",
						GeneXpertAnalyzerImplementation.LOINC_MONO_PERCENT));
		nameMapping.add(
				new PluginAnalyzerService.TestMapping(GeneXpertAnalyzerImplementation.ANALYZER_TEST_EO_COUNT, "Eosinophiles",
						GeneXpertAnalyzerImplementation.LOINC_EO_COUNT));
		nameMapping.add(
				new PluginAnalyzerService.TestMapping(GeneXpertAnalyzerImplementation.ANALYZER_TEST_EO_PERCENT, "Eosinophiles (%)",
						GeneXpertAnalyzerImplementation.LOINC_EO_PERCENT));
		nameMapping.add(
				new PluginAnalyzerService.TestMapping(GeneXpertAnalyzerImplementation.ANALYZER_TEST_BASO_COUNT, "Basophiles",
						GeneXpertAnalyzerImplementation.LOINC_BASO_COUNT));
		nameMapping.add(
				new PluginAnalyzerService.TestMapping(GeneXpertAnalyzerImplementation.ANALYZER_TEST_BASO_PERCENT, "Basophiles (%)",
						GeneXpertAnalyzerImplementation.LOINC_BASO_PERCENT));
		nameMapping.add(
				new PluginAnalyzerService.TestMapping(GeneXpertAnalyzerImplementation.ANALYZER_TEST_IG_COUNT, "",
						GeneXpertAnalyzerImplementation.LOINC_IG_COUNT));
		nameMapping.add(
				new PluginAnalyzerService.TestMapping(GeneXpertAnalyzerImplementation.ANALYZER_TEST_IG_PERCENT, "",
						GeneXpertAnalyzerImplementation.LOINC_IG_PERCENT));
		nameMapping.add(
				new PluginAnalyzerService.TestMapping(GeneXpertAnalyzerImplementation.ANALYZER_TEST_RET_COUNT, "",
						GeneXpertAnalyzerImplementation.LOINC_RET_COUNT));
		nameMapping.add(
				new PluginAnalyzerService.TestMapping(GeneXpertAnalyzerImplementation.ANALYZER_TEST_RET_PERCENT, "",
						GeneXpertAnalyzerImplementation.LOINC_RET_PERCENT));
		nameMapping.add(
				new PluginAnalyzerService.TestMapping(GeneXpertAnalyzerImplementation.ANALYZER_TEST_IRF, "",
						GeneXpertAnalyzerImplementation.LOINC_IRF));
		nameMapping.add(
				new PluginAnalyzerService.TestMapping(GeneXpertAnalyzerImplementation.ANALYZER_TEST_RETHE, "",
						GeneXpertAnalyzerImplementation.LOINC_RETHE));
		nameMapping.add(
				new PluginAnalyzerService.TestMapping(GeneXpertAnalyzerImplementation.ANALYZER_TEST_WBCBF, "",
						GeneXpertAnalyzerImplementation.LOINC_WBCBF));
		nameMapping.add(
				new PluginAnalyzerService.TestMapping(GeneXpertAnalyzerImplementation.ANALYZER_TEST_RBCBF, "",
						GeneXpertAnalyzerImplementation.LOINC_RBCBF));
		nameMapping.add(
				new PluginAnalyzerService.TestMapping(GeneXpertAnalyzerImplementation.ANALYZER_TEST_MN_COUNT, "",
						GeneXpertAnalyzerImplementation.LOINC_MN_COUNT));
		nameMapping.add(
				new PluginAnalyzerService.TestMapping(GeneXpertAnalyzerImplementation.ANALYZER_TEST_MN_PERCENT, "",
						GeneXpertAnalyzerImplementation.LOINC_MN_PERCENT));
		nameMapping.add(
				new PluginAnalyzerService.TestMapping(GeneXpertAnalyzerImplementation.ANALYZER_TEST_PMN_COUNT, "",
						GeneXpertAnalyzerImplementation.LOINC_PMN_COUNT));
		nameMapping.add(
				new PluginAnalyzerService.TestMapping(GeneXpertAnalyzerImplementation.ANALYZER_TEST_PMN_PERCENT, "",
						GeneXpertAnalyzerImplementation.LOINC_PMN_PERCENT));
		nameMapping.add(
				new PluginAnalyzerService.TestMapping(GeneXpertAnalyzerImplementation.ANALYZER_TEST_TCBF_COUNT, "",
						GeneXpertAnalyzerImplementation.LOINC_TCBF_COUNT));
		try{
			loadNamingMappingsFromCSV(nameMapping);	
		}catch(Exception e){
		}						
		getInstance().addAnalyzerDatabaseParts("GeneXpertAnalyzer", "GeneXpertAnalyzer", nameMapping, true);
		getInstance().registerAnalyzer(this);
		return true;
	}

	@Override
	public boolean isTargetAnalyzer(List<String> lines) {
		for (String line : lines) {
			if (line.startsWith(GeneXpertAnalyzerImplementation.HEADER_RECORD_IDENTIFIER)) {
				String[] headerRecord = line.split(Pattern.quote(GeneXpertAnalyzerImplementation.FD));
				if (headerRecord.length < 5) {
					LogEvent.logTrace(this.getClass().getSimpleName(), "isTargetAnalyzer", "incoming message is not GeneXpert: header record not long enough");
					return false;
				}
				String[] senderNameFields = headerRecord[4].split(Pattern.quote(GeneXpertAnalyzerImplementation.CD));
				if (senderNameFields.length < 2) {
					LogEvent.logTrace(this.getClass().getSimpleName(), "isTargetAnalyzer", "incoming message is not GeneXpert: sender name field not long enough");
					return false;
				}
				String systemName = senderNameFields[1].trim();

				LogEvent.logTrace(this.getClass().getSimpleName(), "isTargetAnalyzer", "incoming message analyzer name is " + systemName);
				if (systemName.equalsIgnoreCase("GeneXpert")) {
					LogEvent.logTrace(this.getClass().getSimpleName(), "isTargetAnalyzer", "incoming message is GeneXpert ");
					return true;
				} 
				LogEvent.logTrace(this.getClass().getSimpleName(), "isTargetAnalyzer", "incoming message is not GeneXpert: sender name doesn't match");
				return false;
			}
		}
		LogEvent.logTrace(this.getClass().getSimpleName(), "isTargetAnalyzer", "incoming message is not GeneXpert: no header line");
		return false;
	}

	@Override
	public boolean isAnalyzerResult(List<String> lines) {
		for (String line : lines) {
			if (line.startsWith(GeneXpertAnalyzerImplementation.RESULT_RECORD_IDENTIFIER)) {
				return true;
			}
		}
		LogEvent.logDebug(this.getClass().getSimpleName(), "isAnalyzerResult", "no result recoord identifier located");
		return false;
	}

	@Override
	public AnalyzerLineInserter getAnalyzerLineInserter() {
		return new GeneXpertAnalyzerImplementation();
	}

	@Override
	public AnalyzerResponder getAnalyzerResponder() {
		return new GeneXpertAnalyzerImplementation();
	}

	public void loadNamingMappingsFromCSV(List<PluginAnalyzerService.TestMapping> nameMapping) {
        File file = new File(GeneXpertAnalyzerImplementation.TEST_MAPPING_FILE_PATH);
        if (!file.exists()) {
			LogEvent.logDebug(this.getClass().getName(), "loadMappingsFromCSV", "CSV file not found: " + GeneXpertAnalyzerImplementation.TEST_MAPPING_FILE_PATH);
            return; // Exit if file doesn't exist
        }

        try (FileReader reader = new FileReader(file);
             CSVParser csvParser = new CSVParser(reader, CSVFormat.DEFAULT.withFirstRecordAsHeader())) {

            for (CSVRecord record : csvParser) {
                String analyserTestName = record.get(GeneXpertAnalyzerImplementation.CSV_TEST_MAP_COULMN_ANALYSER_NAME).trim();
				String loincCode = record.get(GeneXpertAnalyzerImplementation.CSV_TEST_MAP_COULMN_LOINC).trim();
                String actualTestName = record.get(GeneXpertAnalyzerImplementation.CSV_TEST_MAP_COULMN_ACTUAL_NAME).trim();
                nameMapping.add(new PluginAnalyzerService.TestMapping(analyserTestName, actualTestName, loincCode));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
