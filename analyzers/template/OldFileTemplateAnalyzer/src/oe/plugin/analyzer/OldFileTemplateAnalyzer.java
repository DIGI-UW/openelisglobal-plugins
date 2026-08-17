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


public class OldFileTemplateAnalyzer implements AnalyzerImporterPlugin {


    public boolean connect(){
        nameMapping.add(new PluginAnalyzerService.TestMapping(ANALYZER_TEST_NAME, DB_TEST_NAME, DB_TEST_LOINC));
        getInstance().addAnalyzerDatabaseParts("OldFileTemplateAnalyzer", "Plugin for OldFileTemplateAnalyzer",nameMapping);
        getInstance().registerAnalyzer(this);
        return true;
    }

    @Override
    public boolean isTargetAnalyzer(List<String> lines) {
    
    	if(getColumnsLine(lines)<0) return false;
    	 
    	return true;
    	
    }

    @Override
    public AnalyzerLineInserter getAnalyzerLineInserter() {
        return new OldFileTemplateAnalyzerImplementation();
    }

	public int getColumnsLine(List<String> lines) {
		for(int k=0;k<lines.size();k++){
		if(lines.get(k).contains("Instr"))
		return k;
			
		}
		
		return -1;
	}
}
