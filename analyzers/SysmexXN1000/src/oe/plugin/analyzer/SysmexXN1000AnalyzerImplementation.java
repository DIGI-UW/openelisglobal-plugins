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

  public SysmexXN1000AnalyzerImplementation() {
    ORDER_NUMBER_INDEX = 0;
    ORDER_DAY_INDEX = 0;
    ORDER_HOUR_INDEX = 0;
    indexTestMap = new HashMap();
    readerUtil = new AnalyzerReaderUtil();
    validStatusId =
        StatusService.getInstance()
            .getStatusID(org.openelisglobal.common.services.StatusService.AnalysisStatus.Finalized);
    analysisDao = SpringContext.getBean(AnalysisDAO.class);
  }

  public boolean insert(List lines, String currentUserId) {
    List results = new ArrayList();
    boolean columnsFound = manageColumnsIndex(lines);
    if (!columnsFound) {
      Logger.getLogger(this.getClass().getName())
          .log(Level.SEVERE, "Sysmex XN1000 analyzer: Unable to find correct columns in file");
      return false;
    }
    for (int i = getColumnsLine(lines) + 1; i < lines.size(); i++)
      createAnalyzerResultFromLine(lines.get(i), results);

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
    String scale = (String) scaleIndexMap.get(testKey);
    String[] results = scale.split(",");
    int dem = Integer.parseInt(results[0]);
    double d = (0.0D / 0.0D);
    try {
      d = Double.parseDouble(result) / (double) dem;
    } catch (NumberFormatException numberformatexception) {
    }
    if (dem == 1) results[0] = result;
    else results[0] = String.valueOf(d);
    return results;
  }

  private boolean manageColumnsIndex(List lines) {
    if (getColumnsLine(lines) < 0) return false;
    String[] headers = lines.get(getColumnsLine(lines)).split(",");
    for (Integer i = (0); i < headers.length; i = (i + 1)) {
      String header = headers[i].trim();
      if (testHeaderNameMap.containsKey(header)) indexTestMap.put(i.toString(), header);
      else if ("Sample No.".equals(header)) ORDER_NUMBER_INDEX = i;
      else if ("Date".equals(header)) ORDER_DAY_INDEX = i;
      else if ("Time".equals(header)) ORDER_HOUR_INDEX = i;
    }

    return ORDER_NUMBER_INDEX != 0 && ORDER_DAY_INDEX != 0 && ORDER_HOUR_INDEX != 0;
  }

  public int getColumnsLine(List lines) {
    for (int k = 0; k < lines.size(); k++)
      if (lines.get(k).contains("Nickname")
          && lines.get(k).contains("Analyzer ID")
          && lines.get(k).contains("Date")
          && lines.get(k).contains("Time")
          && lines.get(k).contains("Rack")
          && lines.get(k).contains("Sample No.")) return k;

    return -1;
  }

  private void createAnalyzerResultFromLine(String line, List resultList) {
    String fields[] = line.split(",");
    for (Integer k = (0); k < fields.length; k = (k + 1))
      if (indexTestMap.containsKey(k.toString())) {
        String testKey = (String) indexTestMap.get(k.toString());
        AnalyzerResults aResult = new AnalyzerResults();
        aResult.setTestId(((Test) testHeaderNameMap.get(testKey)).getId());
        aResult.setTestName(((Test) testHeaderNameMap.get(testKey)).getName());
        String result[] = getAppropriateResults(fields[k], testKey);
        aResult.setResult(result[0]);
        aResult.setUnits(result[1]);
        aResult.setAnalyzerId(ANALYZER_ID);
        aResult.setAccessionNumber(fields[ORDER_NUMBER_INDEX].trim());
        aResult.setResultType("N");
        String dateTime = fields[ORDER_DAY_INDEX].trim();
        dateTime =
            (new StringBuilder(String.valueOf(dateTime)))
                .append(" ")
                .append(fields[ORDER_HOUR_INDEX].trim())
                .toString();
        aResult.setCompleteDate(getTimestampFromDate(dateTime));
        // System.out.println((new
        if (aResult.getAccessionNumber() != null)
          aResult.setIsControl(aResult.getAccessionNumber().startsWith("QC-"));
        else aResult.setIsControl(false);
        addValueToResults(resultList, aResult);
      }
  }

  private void addValueToResults(List resultList, AnalyzerResults result) {
    if (result.getIsControl()) {
      resultList.add(result);
      return;
    }
    SampleService sampleServ = SpringContext.getBean(SampleService.class);
    if (!result.getAccessionNumber().startsWith(projectCode)
        || sampleServ.getSampleByAccessionNumber(result.getAccessionNumber()) == null) return;
    List analyses =
        analysisDao.getAnalysisByAccessionAndTestId(
            result.getAccessionNumber(), result.getTestId());
    for (Iterator iterator = analyses.iterator(); iterator.hasNext(); ) {
      Analysis analysis = (Analysis) iterator.next();
      if (analysis.getStatusId().equals(validStatusId)) return;
    }
    resultList.add(result);
    AnalyzerResults resultFromDB = readerUtil.createAnalyzerResultFromDB(result);
    if (resultFromDB != null) resultList.add(resultFromDB);
  }

  private int ORDER_NUMBER_INDEX;
  private int ORDER_DAY_INDEX;
  private int ORDER_HOUR_INDEX;
  private static final String DELIMITER = ",";
  static String ANALYZER_ID;
  private static final String CONTROL_ACCESSION_PREFIX = "QC-";
  static String DATE_PATTERN = "dd/MM/yyyy HH:mm:ss";
  static HashMap testHeaderNameMap;
  HashMap indexTestMap;
  static HashMap scaleIndexMap;
  private AnalyzerReaderUtil readerUtil;
  private final String projectCode = MessageUtil.getMessage("sample.entry.project.LART");
  String validStatusId;
  AnalysisDAO analysisDao;

  static {
    testHeaderNameMap = new HashMap();
    scaleIndexMap = new HashMap();
    testHeaderNameMap.put(
        "WBC(10^3/uL)", (SpringContext.getBean(TestService.class)).getTestByName("GB"));
    testHeaderNameMap.put(
        "RBC(10^6/uL)", (SpringContext.getBean(TestService.class)).getTestByName("GR"));
    testHeaderNameMap.put(
        "HGB(g/dL)", (SpringContext.getBean(TestService.class)).getTestByName("Hb"));
    testHeaderNameMap.put(
        "HCT(%)", (SpringContext.getBean(TestService.class)).getTestByName("HCT"));
    testHeaderNameMap.put(
        "MCV(fL)", (SpringContext.getBean(TestService.class)).getTestByName("VGM"));
    testHeaderNameMap.put(
        "MCH(pg)", (SpringContext.getBean(TestService.class)).getTestByName("TCMH"));
    testHeaderNameMap.put(
        "MCHC(g/dL)", (SpringContext.getBean(TestService.class)).getTestByName("CCMH"));
    testHeaderNameMap.put(
        "PLT(10^3/uL)", (SpringContext.getBean(TestService.class)).getTestByName("PLQ"));
    testHeaderNameMap.put(
        "NEUT%(%)", (SpringContext.getBean(TestService.class)).getTestByName("Neut %"));
    testHeaderNameMap.put(
        "LYMPH%(%)", (SpringContext.getBean(TestService.class)).getTestByName("Lymph %"));
    testHeaderNameMap.put(
        "MONO%(%)", (SpringContext.getBean(TestService.class)).getTestByName("Mono %"));
    testHeaderNameMap.put(
        "EO%(%)", (SpringContext.getBean(TestService.class)).getTestByName("Eo %"));
    testHeaderNameMap.put(
        "BASO%(%)", (SpringContext.getBean(TestService.class)).getTestByName("Baso %"));
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
    AnalyzerService analyzerService = SpringContext.getBean(AnalyzerService.class);
    Analyzer analyzer = analyzerService.getAnalyzerByName("SysmexXN1000Analyzer");
    ANALYZER_ID = analyzer.getId();
  }
}
