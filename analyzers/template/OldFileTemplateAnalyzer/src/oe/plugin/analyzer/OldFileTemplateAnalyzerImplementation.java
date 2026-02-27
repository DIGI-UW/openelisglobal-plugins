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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.openelisglobal.analyzerimport.analyzerreaders.AnalyzerLineInserter;
import org.openelisglobal.analyzerresults.valueholder.AnalyzerResults;
import org.openelisglobal.common.util.DateUtil;
import org.openelisglobal.spring.util.SpringContext;
import org.openelisglobal.test.service.TestService;
import org.openelisglobal.test.valueholder.Test;

public class OldFileTemplateAnalyzerImplementation extends AnalyzerLineInserter {

	static String DATE_PATTERN = "yyyyMMdd";

	private static final String CONTROL_ACCESSION_PREFIX = "";
	private static final String DELIMITER = ";";
	static HashMap<String, Test> testNameMap = new HashMap<>();
	static HashMap<String, String> testUnitMap = new HashMap<>();

	static {
		Test test = SpringContext.getBean(TestService.class).getTestsByLoincCode(DB_TEST_LOINC).get(0);
		testNameMap.put(ANALYZER_TEST_NAME, test);
		testUnitMap.put(ANALYZER_TEST_NAME, test.getUnitOfMeasure());
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
		List<AnalyzerResults> results = new ArrayList<>();

		for (Integer j = 1; j < lines.size(); j++) {
			String line = lines.get(j);
			String[] data = line.split(DELIMITER);

			if (line.length() == 0 || data.length == 0) {
				continue;
			}
			String result = data[0];
			String testKey = data[0];
			String accessionNumber = data[0];
			AnalyzerResults aResult = new AnalyzerResults();

			aResult.setTestId(testNameMap.get(testKey).getId());
			aResult.setTestName(testNameMap.get(testKey).getName());
			aResult.setResult(result.trim());
			aResult.setUnits(testUnitMap.get(testKey));
			aResult.setAccessionNumber(accessionNumber.trim());
			// aResult.setReadOnly(CheckReadOnly (testKey));
			aResult.setIsControl(isControl(accessionNumber)));
			aResult.setCompleteDate(getTimestampFromDate(date));

			results.add(aResult);

		}
		return persistImport(currentUserId, results);
	}

	private boolean isControl(String accessionNumber) {
		boolean isControl = false;
		if (accessionNumber.startsWith(CONTROL_ACCESSION_PREFIX)) {
			isControl = true;
		}
		return isControl;

	}

	private Timestamp getTimestampFromDate(String dateTime) {
		return DateUtil.convertStringDateToTimestampWithPattern(dateTime, DATE_PATTERN);
	}

	@Override
	public String getError() {
		return "OldFileTemplateAnalyzer unable to write to database";
	}
}
