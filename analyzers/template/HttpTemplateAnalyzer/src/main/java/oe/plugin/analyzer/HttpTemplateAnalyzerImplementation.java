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

import java.sql.Timestamp;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import org.openelisglobal.analyzerimport.analyzerreaders.AnalyzerLineInserter;
import org.openelisglobal.analyzerimport.analyzerreaders.AnalyzerReaderUtil;
import org.openelisglobal.analyzerimport.util.AnalyzerTestNameCache;
import org.openelisglobal.analyzerimport.util.MappedTestName;
import org.openelisglobal.analyzerresults.valueholder.AnalyzerResults;
import org.openelisglobal.common.services.PluginAnalyzerService;
import org.openelisglobal.spring.util.SpringContext;
import org.openelisglobal.test.service.TestService;
import org.openelisglobal.test.valueholder.Test;

public class HttpTemplateAnalyzerImplementation extends AnalyzerLineInserter {

	private TestService testService = SpringContext.getBean(TestService.class);
	static final String HBV = "Xpert HBV Viral Load";
	static final String HCV = "Xpert HCV Viral Load";
	static final String HIV_QUAL = "Xpert HIV-1 Qual";
	static final String HIV_VIRAL = "Xpert HIV-1 viral Load";
	static final String COV_2 = "Xpert Xpress SARS-CoV-2 assay";
	
	static final String HBV_LOINC = "29615-2";
	static final String HCV_LOINC = "11011-4";
	static final String HIV_QUAL_LOINC = "";
	static final String HIV_VIRAL_LOINC = "10351-5";
	static final String COV_2_LOINC = "94500-6";
	private HashMap<String, List<Test>> testLoincMap = new HashMap<>();

	private AnalyzerReaderUtil readerUtil = new AnalyzerReaderUtil();

	public HttpTemplateAnalyzerAnalyzerImplementation() {
		testLoincMap.put(HBV_LOINC, testService.getTestsByLoincCode(HBV_LOINC));
		testLoincMap.put(HCV_LOINC, testService.getTestsByLoincCode(HCV_LOINC));
		testLoincMap.put(HIV_QUAL_LOINC, testService.getTestsByLoincCode(HIV_QUAL_LOINC));
		testLoincMap.put(HIV_VIRAL_LOINC, testService.getTestsByLoincCode(HIV_VIRAL_LOINC));
		testLoincMap.put(COV_2_LOINC, testService.getTestsByLoincCode(COV_2_LOINC));
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see
	 * org.openelisglobal.analyzerimport.analyzerreaders.AnalyzerLineInserter#insert
	 * (java.util.List, java.lang.String)
	 */
	@Override
	public boolean insert(List<String> lines, String currentUserId) {
		return false;
	}

	@Override
	public String getError() {
		return "HttpTemplateAnalyzer analyzer unable to write to database";
	}

	public void addResult(List<AnalyzerResults> resultList, List<AnalyzerResults> notMatchedResults, String resultType,
			String resultValue, Date date, String accessionNumber, boolean isControl, String resultUnits,
			String analyzerTestId) {
		AnalyzerResults analyzerResults = createAnalyzerResult(resultType, resultValue, resultUnits, date,
				accessionNumber, isControl, analyzerTestId);
		if (analyzerResults.getTestId() != null) {
			addValueToResults(resultList, analyzerResults);
		} else {
			notMatchedResults.add(analyzerResults);
		}
	}

	private void addValueToResults(List<AnalyzerResults> resultList, AnalyzerResults result) {
		resultList.add(result);

		AnalyzerResults resultFromDB = readerUtil.createAnalyzerResultFromDB(result);
		if (resultFromDB != null) {
			resultList.add(resultFromDB);
		}

	}

	private AnalyzerResults createAnalyzerResult(String resultType, String resultValue, String resultUnits, Date date,
			String accessionNumber, boolean isControl, String analyzerTestId) {
		AnalyzerResults analyzerResults = new AnalyzerResults();
		analyzerResults.setResult(resultValue);
		analyzerResults.setUnits(resultUnits);
		if (date != null) {
			analyzerResults.setCompleteDate(new Timestamp(date.getTime()));
		}
		analyzerResults.setAccessionNumber(accessionNumber);
		analyzerResults.setTestId(
				testLoincMap.get(analyzerTestId).size() > 0 ? testLoincMap.get(analyzerTestId).get(0).getId() : "");
		analyzerResults.setIsControl(isControl);
		analyzerResults.setTestName(testLoincMap.get(analyzerTestId).size() > 0
				? testLoincMap.get(analyzerTestId).get(0).getLocalizedTestName().getLocalizedValue()
				: "");
		return analyzerResults;
	}

	public void persistImport(List<AnalyzerResults> resultList) {
		this.persistImport("1", resultList);
	}

}
