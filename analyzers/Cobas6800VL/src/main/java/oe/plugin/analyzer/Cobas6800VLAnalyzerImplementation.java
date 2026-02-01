package oe.plugin.analyzer;

import java.io.FileWriter;
import java.sql.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import org.openelisglobal.analysis.service.AnalysisService;
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

public class Cobas6800VLAnalyzerImplementation extends AnalyzerLineInserter {

  private static final String COBAS_6800_DATASET_SEPARATOR = "L|1|N";
  private static final String COBAS_6800_ACCESSION_NUMBER_FLAG = "OBR|1|";
  private static final String COBAS_6800_RESULT_FLAG = "OBX|1|";

  public Cobas6800VLAnalyzerImplementation() {
    indexTestMap = new HashMap();
    readerUtil = new AnalyzerReaderUtil();
    test = SpringContext.getBean(TestService.class).getActiveTestByName("Viral Load").get(0);
    validStatusId = StatusService.getInstance().getStatusID(StatusService.AnalysisStatus.Finalized);
    analysisService = SpringContext.getBean(AnalysisService.class);
  }

  public String getError() {
    return error;
  }

  private void addValueToResults(List<AnalyzerResults> resultList, AnalyzerResults result) {
    if (result.getIsControl()) {
      resultList.add(result);
      return;
    }
    SampleService sampleServ = SpringContext.getBean(SampleService.class);
    if (!result.getAccessionNumber().startsWith(projectCode)
        || sampleServ.getSampleByAccessionNumber(result.getAccessionNumber()) == null) return;
    List<Analysis> analyses =
        analysisService.getAnalysisByAccessionAndTestId(
            result.getAccessionNumber(), result.getTestId());
    for (Analysis analysis : analyses) {
      if (analysis.getStatusId().equals(validStatusId)) return;
    }

    resultList.add(result);
    AnalyzerResults resultFromDB = readerUtil.createAnalyzerResultFromDB(result);
    if (resultFromDB != null) resultList.add(resultFromDB);
  }

  public void ordersExport(List results) {
    Connection c = null;
    Statement stmt = null;
    try {
      Class.forName("org.postgresql.Driver");
      c =
          DriverManager.getConnection(
              "jdbc:postgresql://localhost:5432/clinlims", "clinlims", "clinlims");
      c.setAutoCommit(false);

      String sql =
          (new StringBuilder(
                  "SELECT s.accession_number, a.test_id,pat.national_id,pat.external_id,pat.gender,pat.birth_date FROM clinlims.sample s,clinlims.sample_item si,clinlims.analysis a,clinlims.sample_human sh,clinlims.patient pat WHERE  a.status_id=13  AND a.test_id IN ("))
              .append(test.getId())
              .append(") AND ")
              .append("a.sampitem_id=si.id AND ")
              .append("si.samp_id=s.id AND ")
              .append("sh.samp_id=s.id AND ")
              .append("sh.patient_id=pat.id ")
              .append("ORDER BY 1")
              .toString();
      stmt = c.createStatement();
      ResultSet rs = stmt.executeQuery(sql);
      FileWriter writer =
          new FileWriter("/home/oeserver/Desktop/Prescriptions/ARVPRESC.AST", false);
      writer.write("H|^~\\&|||GLIMS||ORM|||MPL|||A2.2|200712120754|");
      writer.write("\r\n");
      int inc = 0;
      for (; rs.next(); writer.write("\r\n")) {
        inc++;
        String labno = rs.getString("accession_number");
        String sujetno = rs.getString("national_id");
        String external_id = rs.getString("external_id");
        String sexe = rs.getString("gender");
        String birth_date = rs.getString("birth_date");
        String patID = sujetno != null ? sujetno : external_id;
        writer.write(
            (new StringBuilder("P|"))
                .append(inc)
                .append("|")
                .append(labno)
                .append("|||Patient-XXXXX||")
                .append(birth_date.substring(0, 10).replace("-", ""))
                .append("|")
                .append(sexe)
                .append("||||||||||||||||||")
                .toString());
        writer.write("\r\n");
        writer.write(
            (new StringBuilder("OBR|1|"))
                .append(labno)
                .append("||Viral Load|R|||||||||||||")
                .append(patID)
                .append("||||||||||")
                .toString());
      }

      writer.write("L|1|");
      rs.close();
      stmt.close();
      c.close();
      writer.close();
    } catch (Exception e) {
      System.err.println(
          (new StringBuilder(String.valueOf(e.getClass().getName())))
              .append(": ")
              .append(e.getMessage())
              .toString());
    }
  }

  public boolean filterOrdersExport(List<AnalyzerResults> results, String labno) {
    for (AnalyzerResults ar : results) {
      if (ar.getAccessionNumber().equalsIgnoreCase(labno)) return true;
    }

    return false;
  }

  public int getColumnsLine(List<String> lines) {
    for (int k = 0; k < lines.size(); k++)
      if (lines.get(k).contains("Patient Name")
          && lines.get(k).contains("Patient ID")
          && lines.get(k).contains("Order Number")
          && lines.get(k).contains("Sample ID")
          && lines.get(k).contains("Test")
          && lines.get(k).contains("Result")) return k;

    return -1;
  }

  public static List<List<String>> splitLinestBySeparator(List<String> lines, String separator) {
    List<List<String>> result = new ArrayList<>();
    List<String> currentList = new ArrayList<>();

    for (String item : lines) {
      if (item.equals(separator)) {
        if (!currentList.isEmpty()) {
          result.add(new ArrayList<>(currentList));
          currentList.clear();
        }
      } else {
        currentList.add(item);
      }
    }
    if (!currentList.isEmpty()) {
      result.add(currentList);
    }

    return result;
  }

  public boolean insert(List<String> lines, String currentUserId) {

    List<AnalyzerResults> results = new ArrayList<AnalyzerResults>();
    List<List<String>> resultsSet = splitLinestBySeparator(lines, COBAS_6800_DATASET_SEPARATOR);
    for (List<String> set : resultsSet) {
      createVLResultFromEntry(set, results);
    }

    //		java.util.Map.Entry entry;
    //		for (Iterator iterator = getResultsLines(lines, RESULT_FLAG, VL_FLAG).entrySet().iterator();
    // iterator
    //				.hasNext(); createVLResultFromEntry(lines, entry, results))
    //			entry = (java.util.Map.Entry) iterator.next();

    Collections.sort(
        results,
        new Comparator<AnalyzerResults>() {

          public int compare(AnalyzerResults o1, AnalyzerResults o2) {
            return o1.getAccessionNumber().compareTo(o2.getAccessionNumber());
          }
        });
    return persistImport(currentUserId, results);
  }

  public HashMap getResultsLines(List lines, String RESULT_FLAG, String TEST_FLAG) {
    HashMap IdValuePair = new HashMap();
    for (int i = 0; i < lines.size(); i++)
      if (((String) lines.get(i)).startsWith("P|")) {
        int j;
        for (j = i;
            !((String) lines.get(j)).contains(RESULT_FLAG)
                || !((String) lines.get(j)).contains(TEST_FLAG);
            j++)
          ;
        IdValuePair.put((i), (j));
      }

    return IdValuePair.size() != 0 ? IdValuePair : null;
  }

  public void createVLResultFromEntry(List<String> subLines, List<AnalyzerResults> resultList) {
    AnalyzerResults analyzerResults = new AnalyzerResults();

    String accessionNumber = "";
    String result = "";
    String completedDate = "";
    for (String line : subLines) {
      if (line.startsWith(COBAS_6800_ACCESSION_NUMBER_FLAG)) {
        String[] splitedLine = line.split("\\|");
        if (splitedLine.length >= 3) accessionNumber = splitedLine[2];
      }
      if (line.startsWith(COBAS_6800_RESULT_FLAG)) {
        String[] splitedLine = line.split("\\|");
        if (splitedLine.length >= 13) {
          result = splitedLine[5];
          completedDate = splitedLine[12];
        }
      }
    }

    accessionNumber = accessionNumber.trim();
    accessionNumber = accessionNumber.replace(" ", "");
    if (accessionNumber.startsWith(projectCode) && accessionNumber.length() >= 9)
      accessionNumber = accessionNumber.substring(0, 9);
    result = getAppropriateResults(result);

    analyzerResults.setAnalyzerId(ANALYZER_ID);
    analyzerResults.setResult(result);
    analyzerResults.setUnits("< LL".equals(result) ? "" : "cp/ml");
    analyzerResults.setCompleteDate(
        DateUtil.convertStringDateToTimestampWithPattern(
            (new StringBuilder(String.valueOf(completedDate.substring(0, 4))))
                .append("/")
                .append(completedDate.substring(4, 6))
                .append("/")
                .append(completedDate.substring(6, 8))
                .append(" 00:00:00")
                .toString(),
            "yyyy/MM/dd HH:mm:ss"));
    analyzerResults.setTestId(test.getId());
    analyzerResults.setIsControl(false);
    analyzerResults.setTestName(test.getName());
    analyzerResults.setResultType("A");
    analyzerResults.setAccessionNumber(accessionNumber);
    addValueToResults(resultList, analyzerResults);
  }

  public void createVLResultFromEntry(
      List<String> lines, java.util.Map.Entry entry, List<AnalyzerResults> resultList) {
    AnalyzerResults analyzerResults = new AnalyzerResults();
    String line = (String) lines.get(((Integer) entry.getKey()));
    for (int i = 1; i <= 2; i++) line = line.substring(1 + line.indexOf("|"));

    String accessionNumber = line.substring(0, line.indexOf("|"));
    accessionNumber = accessionNumber.trim();
    accessionNumber = accessionNumber.replace(" ", "");
    if (accessionNumber.startsWith(projectCode) && accessionNumber.length() >= 9)
      accessionNumber = accessionNumber.substring(0, 9);
    line = (String) lines.get(((Integer) entry.getValue()));
    for (int i = 1; i <= 5; i++) line = line.substring(1 + line.indexOf("|"));

    String result = line.substring(0, line.indexOf("|"));
    result = getAppropriateResults(result);
    for (int i = 1; i <= 7; i++) line = line.substring(1 + line.indexOf("|"));

    String completedDate = line.substring(0, line.indexOf("|"));
    analyzerResults.setAnalyzerId(ANALYZER_ID);
    analyzerResults.setResult(result);
    analyzerResults.setUnits("< LL".equals(result) ? "" : "cp/ml");
    analyzerResults.setCompleteDate(
        DateUtil.convertStringDateToTimestampWithPattern(
            (new StringBuilder(String.valueOf(completedDate.substring(0, 4))))
                .append("/")
                .append(completedDate.substring(4, 6))
                .append("/")
                .append(completedDate.substring(6, 8))
                .append(" 00:00:00")
                .toString(),
            "yyyy/MM/dd HH:mm:ss"));
    analyzerResults.setTestId(test.getId());
    analyzerResults.setIsControl(false);
    analyzerResults.setTestName(test.getName());
    analyzerResults.setResultType("A");
    analyzerResults.setAccessionNumber(accessionNumber);
    addValueToResults(resultList, analyzerResults);
  }

  public void ordersExport2(List results) {
    Connection c = null;
    Statement stmt = null;
    try {
      Class.forName("org.postgresql.Driver");
      c =
          DriverManager.getConnection(
              "jdbc:postgresql://localhost:5432/clinlims", "clinlims", "clinlims");
      c.setAutoCommit(false);

      String sql =
          (new StringBuilder(
                  "SELECT s.accession_number, a.test_id,pat.national_id,pat.external_id,pat.gender,pat.birth_date FROM clinlims.sample s,clinlims.sample_item si,clinlims.analysis a,clinlims.sample_human sh,clinlims.patient pat WHERE  a.status_id=13  AND a.test_id IN ("))
              .append(test.getId())
              .append(") AND ")
              .append("a.sampitem_id=si.id AND ")
              .append("si.samp_id=s.id AND ")
              .append("sh.samp_id=s.id AND ")
              .append("sh.patient_id=pat.id ")
              .append("ORDER BY 1")
              .toString();
      stmt = c.createStatement();
      ResultSet rs = stmt.executeQuery(sql);
      FileWriter writer =
          new FileWriter("/home/oeserver/Desktop/Prescriptions/ARVPRESC.AST", false);
      writer.write("H|^~\\&|||GLIMS||ORM|||MPL|||A2.2|200712120754|");
      writer.write("\r\n");
      int inc = 0;
      for (; rs.next(); writer.write("\r\n")) {
        inc++;
        String labno = rs.getString("accession_number");
        String sujetno = rs.getString("national_id");
        String external_id = rs.getString("external_id");
        String sexe = rs.getString("gender");
        String birth_date = rs.getString("birth_date");
        String patID = sujetno != null ? sujetno : external_id;
        writer.write(
            (new StringBuilder("P|"))
                .append(inc)
                .append("|")
                .append(labno)
                .append("|||Patient-XXXXX||")
                .append(birth_date.substring(0, 10).replace("-", ""))
                .append("|")
                .append(sexe)
                .append("||||||||||||||||||")
                .toString());
        writer.write("\r\n");
        writer.write(
            (new StringBuilder("OBR|1|"))
                .append(labno)
                .append("||Viral Load|R|||||||||||||")
                .append(patID)
                .append("||||||||||")
                .toString());
      }

      writer.write("L|1|");
      rs.close();
      stmt.close();
      c.close();
      writer.close();
    } catch (Exception e) {
      System.err.println(
          (new StringBuilder(String.valueOf(e.getClass().getName())))
              .append(": ")
              .append(e.getMessage())
              .toString());
    }
  }

  private String getAppropriateResults(String result) {
    result = result.replace("\"", "").trim();
    if (result.contains("BT")
        || result.contains("ND")
        || result.contains("<20")
        || result.contains("<LL")
        || result.contains("ValueNotSet")) result = "< LL";
    else
      try {
        Double resultAsDouble = Double.valueOf(Double.parseDouble(result));
        if (resultAsDouble.doubleValue() <= 20D) {
          result = "< LL";
        } else {
          result = String.valueOf((int) Math.round(resultAsDouble.doubleValue()));
          result =
              (new StringBuilder(String.valueOf(result)))
                  .append("(")
                  .append(
                      String.format(
                          "%.3g%n",
                          new Object[] {Double.valueOf(Math.log10(resultAsDouble.doubleValue()))}))
                  .toString();
          result = (new StringBuilder(String.valueOf(result))).append(")").toString();
        }
      } catch (NumberFormatException e) {
        return "XXXX";
      }
    return result;
  }

  private static final String UNDER_THREASHOLD = "< LL";
  private static final double THREASHOLD = 20D;
  private static String RESULT_FLAG = "OBX";
  private static String VL_FLAG = "Load Viral"; // private static String VL_FLAG = "Viral Load";
  private static final String DATE_PATTERN = "yyyy/MM/dd HH:mm:ss";
  static String ANALYZER_ID;
  private final String projectCode = MessageUtil.getMessage("sample.entry.project.LART");
  static HashMap testHeaderNameMap;
  HashMap indexTestMap;
  static HashMap unitsIndexMap;
  private AnalyzerReaderUtil readerUtil;
  private String error;
  Test test;
  String validStatusId;
  AnalysisService analysisService;

  static {
    testHeaderNameMap = new HashMap();
    unitsIndexMap = new HashMap();
    testHeaderNameMap.put(
        "Viral Load",
        SpringContext.getBean(TestService.class).getActiveTestByName("Viral Load").get(0));
    testHeaderNameMap.put(
        "DNA PCR", SpringContext.getBean(TestService.class).getActiveTestByName("DNA PCR").get(0));
    unitsIndexMap.put("CD4", "");
    unitsIndexMap.put("%CD4", "%");
    AnalyzerService analyzerService = SpringContext.getBean(AnalyzerService.class);
    Analyzer analyzer = analyzerService.getAnalyzerByName("Cobas6800VLAnalyzer");
    ANALYZER_ID = analyzer.getId();
  }
}
