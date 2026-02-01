package oe.plugin.analyzer;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.openelisglobal.analysis.dao.AnalysisDAO;
import org.openelisglobal.analysis.valueholder.Analysis;
import org.openelisglobal.analyzer.service.AnalyzerService;
import org.openelisglobal.analyzer.valueholder.Analyzer;
import org.openelisglobal.analyzerimport.analyzerreaders.AnalyzerLineInserter;
import org.openelisglobal.analyzerimport.analyzerreaders.AnalyzerReaderUtil;
import org.openelisglobal.analyzerresults.valueholder.AnalyzerResults;
import org.openelisglobal.common.services.StatusService;
import org.openelisglobal.common.util.DateUtil;
import org.openelisglobal.internationalization.MessageUtil;
import org.openelisglobal.sample.service.SampleService;
import org.openelisglobal.spring.util.SpringContext;
import org.openelisglobal.test.service.TestService;
import org.openelisglobal.test.valueholder.Test;

public class SysmexXN1000AnalyzerImplementation extends AnalyzerLineInserter {

  private static final String ANALYZER_NAME = "SysmexXN1000Analyzer";
  private static final String DELIMITER = ",";
  private static final String DATE_PATTERN = "dd/MM/yyyy HH:mm:ss";
  private static final String CONTROL_ACCESSION_PREFIX = "QC-";

  private int ORDER_NUMBER_INDEX = 0;
  private int ORDER_DAY_INDEX = 0;
  private int ORDER_HOUR_INDEX = 0;

  // Lazy-initialized services
  private TestService testService;
  private AnalyzerService analyzerService;
  private SampleService sampleService;
  private AnalysisDAO analysisDao;

  // Lazy-initialized data
  private String analyzerId;
  private String projectCode;
  private String validStatusId;
  private HashMap<String, Test> testHeaderNameMap;
  private HashMap<String, String> scaleIndexMap;

  HashMap<String, String> indexTestMap = new HashMap<>();
  private AnalyzerReaderUtil readerUtil = new AnalyzerReaderUtil();

  // Lazy getter for TestService
  protected TestService getTestService() {
    if (testService == null) {
      testService = SpringContext.getBean(TestService.class);
    }
    return testService;
  }

  // Lazy getter for AnalyzerService
  protected AnalyzerService getAnalyzerService() {
    if (analyzerService == null) {
      analyzerService = SpringContext.getBean(AnalyzerService.class);
    }
    return analyzerService;
  }

  // Lazy getter for SampleService
  protected SampleService getSampleService() {
    if (sampleService == null) {
      sampleService = SpringContext.getBean(SampleService.class);
    }
    return sampleService;
  }

  // Lazy getter for AnalysisDAO
  protected AnalysisDAO getAnalysisDao() {
    if (analysisDao == null) {
      analysisDao = SpringContext.getBean(AnalysisDAO.class);
    }
    return analysisDao;
  }

  // Lazy getter for analyzer ID
  protected String getAnalyzerId() {
    if (analyzerId == null) {
      Analyzer analyzer = getAnalyzerService().getAnalyzerByName(ANALYZER_NAME);
      if (analyzer != null) {
        analyzerId = analyzer.getId();
      }
    }
    return analyzerId;
  }

  // Lazy getter for project code
  protected String getProjectCode() {
    if (projectCode == null) {
      projectCode = MessageUtil.getMessage("sample.entry.project.LART");
    }
    return projectCode;
  }

  // Lazy getter for valid status ID
  protected String getValidStatusId() {
    if (validStatusId == null) {
      validStatusId =
          StatusService.getInstance().getStatusID(StatusService.AnalysisStatus.Finalized);
    }
    return validStatusId;
  }

  // Lazy getter for test header name map
  protected HashMap<String, Test> getTestHeaderNameMap() {
    if (testHeaderNameMap == null) {
      testHeaderNameMap = new HashMap<>();
      TestService ts = getTestService();
      testHeaderNameMap.put("WBC(10^3/uL)", ts.getTestByName("GB"));
      testHeaderNameMap.put("RBC(10^6/uL)", ts.getTestByName("GR"));
      testHeaderNameMap.put("HGB(g/dL)", ts.getTestByName("Hb"));
      testHeaderNameMap.put("HCT(%)", ts.getTestByName("HCT"));
      testHeaderNameMap.put("MCV(fL)", ts.getTestByName("VGM"));
      testHeaderNameMap.put("MCH(pg)", ts.getTestByName("TCMH"));
      testHeaderNameMap.put("MCHC(g/dL)", ts.getTestByName("CCMH"));
      testHeaderNameMap.put("PLT(10^3/uL)", ts.getTestByName("PLQ"));
      testHeaderNameMap.put("NEUT%(%)", ts.getTestByName("Neut %"));
      testHeaderNameMap.put("LYMPH%(%)", ts.getTestByName("Lymph %"));
      testHeaderNameMap.put("MONO%(%)", ts.getTestByName("Mono %"));
      testHeaderNameMap.put("EO%(%)", ts.getTestByName("Eo %"));
      testHeaderNameMap.put("BASO%(%)", ts.getTestByName("Baso %"));
    }
    return testHeaderNameMap;
  }

  // Lazy getter for scale index map
  protected HashMap<String, String> getScaleIndexMap() {
    if (scaleIndexMap == null) {
      scaleIndexMap = new HashMap<>();
      scaleIndexMap.put("WBC(10^3/uL)", "1,10^3uL");
      scaleIndexMap.put("RBC(10^6/uL)", "1,10^6uL");
      scaleIndexMap.put("HGB(g/dL)", "1,g/dL");
      scaleIndexMap.put("HCT(%)", "1,%");
      scaleIndexMap.put("MCV(fL)", "1,fL");
      scaleIndexMap.put("MCH(pg)", "1,pg");
      scaleIndexMap.put("MCHC(g/dL)", "1,g/dL");
      scaleIndexMap.put("PLT(10^3/uL)", "1,10^3/uL");
      scaleIndexMap.put("NEUT%(%)", "1,%");
      scaleIndexMap.put("LYMPH%(%)", "1,%");
      scaleIndexMap.put("MONO%(%)", "1,%");
      scaleIndexMap.put("EO%(%)", "1,%");
      scaleIndexMap.put("BASO%(%)", "1,%");
    }
    return scaleIndexMap;
  }

  @SuppressWarnings({"rawtypes", "unchecked"})
  public boolean insert(List lines, String currentUserId) {
    List results = new ArrayList();
    boolean columnsFound = manageColumnsIndex(lines);
    if (!columnsFound) {
      Logger.getLogger(this.getClass().getName())
          .log(Level.SEVERE, "Sysmex XN1000 analyzer: Unable to find correct columns in file");
      return false;
    }
    for (int i = getColumnsLine(lines) + 1; i < lines.size(); i++)
      createAnalyzerResultFromLine((String) lines.get(i), results);

    return persistImport(currentUserId, results);
  }

  private Timestamp getTimestampFromDate(String dateTime) {
    return DateUtil.convertStringDateToTimestampWithPattern(dateTime, DATE_PATTERN);
  }

  public String getError() {
    return "SysmexXT analyzer unable to write to database";
  }

  private String[] getAppropriateResults(String result, String testKey) {
    result = result.trim();
    String scale = getScaleIndexMap().get(testKey);
    if (scale == null) {
      return new String[] {result, ""};
    }
    String[] results = scale.split(",");
    int dem = Integer.parseInt(results[0]);
    double d = Double.NaN;
    try {
      d = Double.parseDouble(result) / (double) dem;
    } catch (NumberFormatException numberformatexception) {
    }
    if (dem == 1) results[0] = result;
    else results[0] = String.valueOf(d);
    return results;
  }

  @SuppressWarnings("rawtypes")
  private boolean manageColumnsIndex(List lines) {
    if (getColumnsLine(lines) < 0) return false;
    String[] headers = ((String) lines.get(getColumnsLine(lines))).split(",");
    for (Integer i = (0); i < headers.length; i = (i + 1)) {
      String header = headers[i].trim();
      if (getTestHeaderNameMap().containsKey(header)) indexTestMap.put(i.toString(), header);
      else if ("Sample No.".equals(header)) ORDER_NUMBER_INDEX = i;
      else if ("Date".equals(header)) ORDER_DAY_INDEX = i;
      else if ("Time".equals(header)) ORDER_HOUR_INDEX = i;
    }

    return ORDER_NUMBER_INDEX != 0 && ORDER_DAY_INDEX != 0 && ORDER_HOUR_INDEX != 0;
  }

  @SuppressWarnings("rawtypes")
  public int getColumnsLine(List lines) {
    for (int k = 0; k < lines.size(); k++) {
      String line = (String) lines.get(k);
      if (line.contains("Nickname")
          && line.contains("Analyzer ID")
          && line.contains("Date")
          && line.contains("Time")
          && line.contains("Rack")
          && line.contains("Sample No.")) return k;
    }
    return -1;
  }

  @SuppressWarnings({"rawtypes", "unchecked"})
  private void createAnalyzerResultFromLine(String line, List resultList) {
    String fields[] = line.split(",");
    for (Integer k = (0); k < fields.length; k = (k + 1))
      if (indexTestMap.containsKey(k.toString())) {
        String testKey = indexTestMap.get(k.toString());
        AnalyzerResults aResult = new AnalyzerResults();
        Test test = getTestHeaderNameMap().get(testKey);
        if (test != null) {
          aResult.setTestId(test.getId());
          aResult.setTestName(test.getName());
        }
        String result[] = getAppropriateResults(fields[k], testKey);
        aResult.setResult(result[0]);
        aResult.setUnits(result[1]);
        aResult.setAnalyzerId(getAnalyzerId());
        aResult.setAccessionNumber(fields[ORDER_NUMBER_INDEX].trim());
        aResult.setResultType("N");
        String dateTime = fields[ORDER_DAY_INDEX].trim();
        dateTime =
            (new StringBuilder(String.valueOf(dateTime)))
                .append(" ")
                .append(fields[ORDER_HOUR_INDEX].trim())
                .toString();
        aResult.setCompleteDate(getTimestampFromDate(dateTime));
        if (aResult.getAccessionNumber() != null)
          aResult.setIsControl(aResult.getAccessionNumber().startsWith(CONTROL_ACCESSION_PREFIX));
        else aResult.setIsControl(false);
        addValueToResults(resultList, aResult);
      }
  }

  @SuppressWarnings({"rawtypes", "unchecked"})
  private void addValueToResults(List resultList, AnalyzerResults result) {
    if (result.getIsControl()) {
      resultList.add(result);
      return;
    }
    if (!result.getAccessionNumber().startsWith(getProjectCode())
        || getSampleService().getSampleByAccessionNumber(result.getAccessionNumber()) == null)
      return;
    List analyses =
        getAnalysisDao()
            .getAnalysisByAccessionAndTestId(result.getAccessionNumber(), result.getTestId());
    for (Iterator iterator = analyses.iterator(); iterator.hasNext(); ) {
      Analysis analysis = (Analysis) iterator.next();
      if (analysis.getStatusId().equals(getValidStatusId())) return;
    }
    resultList.add(result);
    AnalyzerResults resultFromDB = readerUtil.createAnalyzerResultFromDB(result);
    if (resultFromDB != null) resultList.add(resultFromDB);
  }
}
