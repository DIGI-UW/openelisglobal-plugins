package org.openelisglobal.plugins.analyzer.genericastm;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.Timestamp;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openelisglobal.analysis.service.AnalysisService;
import org.openelisglobal.analysis.valueholder.Analysis;
import org.openelisglobal.analyzerimport.service.AnalyzerTestMappingService;
import org.openelisglobal.analyzerimport.valueholder.AnalyzerTestMapping;
import org.openelisglobal.patient.valueholder.Patient;
import org.openelisglobal.person.valueholder.Person;
import org.openelisglobal.sample.service.SampleService;
import org.openelisglobal.sample.valueholder.Sample;
import org.openelisglobal.samplehuman.service.SampleHumanService;

@RunWith(MockitoJUnitRunner.class)
public class GenericASTMResponderTest {

  private static final String ANALYZER_TYPE_ID = "2006";
  private static final String ANALYZER_NAME = "GenericASTM";

  @Mock private SampleService sampleService;
  @Mock private SampleHumanService sampleHumanService;
  @Mock private AnalysisService analysisService;
  @Mock private AnalyzerTestMappingService analyzerTestMappingService;

  private GenericASTMResponder responder;

  @Before
  public void setUp() {
    responder =
        new GenericASTMResponder(
            ANALYZER_TYPE_ID,
            ANALYZER_NAME,
            sampleService,
            sampleHumanService,
            analysisService,
            analyzerTestMappingService);
  }

  @Test
  public void buildResponse_WithQRecordAndMappedAnalysis_ReturnsProtocolOrderSegments() {
    String accessionNumber = "GX-2026-0001";
    Sample sample = org.mockito.Mockito.mock(Sample.class);
    when(sample.getId()).thenReturn("sample-1");
    when(sample.getEnteredDate()).thenReturn(new java.sql.Date(1741262400000L));

    Person person = org.mockito.Mockito.mock(Person.class);
    when(person.getFirstName()).thenReturn("Jean");
    when(person.getLastName()).thenReturn("Rakoto");

    Patient patient = org.mockito.Mockito.mock(Patient.class);
    when(patient.getNationalId()).thenReturn("PAT-001");
    when(patient.getGender()).thenReturn("M");
    when(patient.getBirthDate()).thenReturn(new Timestamp(479001600000L));
    when(patient.getPerson()).thenReturn(person);

    org.openelisglobal.test.valueholder.Test test =
        org.mockito.Mockito.mock(org.openelisglobal.test.valueholder.Test.class);
    when(test.getId()).thenReturn("101");
    Analysis analysis = org.mockito.Mockito.mock(Analysis.class);
    when(analysis.getTest()).thenReturn(test);

    AnalyzerTestMapping mapping = new AnalyzerTestMapping();
    mapping.setAnalyzerTypeId(ANALYZER_TYPE_ID);
    mapping.setAnalyzerTestName("MTB-RIF");
    mapping.setTestId("101");

    when(sampleService.getSampleByAccessionNumber(accessionNumber)).thenReturn(sample);
    when(sampleHumanService.getPatientForSample(sample)).thenReturn(patient);
    when(analysisService.getAnalysesBySampleId("sample-1")).thenReturn(Collections.singletonList(analysis));
    when(analyzerTestMappingService.getAll()).thenReturn(Collections.singletonList(mapping));

    List<String> lines =
        Arrays.asList(
            "H|\\^&|||GENEXPERT^GeneXpert^4.6.0|||||||LIS2-A2",
            "Q|1|" + accessionNumber + "||ALL",
            "L|1|N");

    String response = responder.buildResponse(lines);

    assertTrue("Response should start with ASTM header", response.startsWith("H|\\^&"));
    assertTrue("Response should contain patient segment", response.contains("P|1|PAT-001||Rakoto^Jean"));
    assertTrue(
        "Response should include mapped analyzer test code in O record",
        response.contains("O|1|GX-2026-0001||^^^MTB-RIF|R|"));
    assertTrue("Response should end with ASTM terminator", response.endsWith("L|1|N\r\n"));
  }

  @Test
  public void buildResponse_WithQRecordAndUnknownSample_ReturnsNoOrderMarker() {
    when(sampleService.getSampleByAccessionNumber("UNKNOWN-01")).thenReturn(null);

    List<String> lines =
        Arrays.asList(
            "H|\\^&|||GENEXPERT^GeneXpert^4.6.0|||||||LIS2-A2", "Q|1|UNKNOWN-01||ALL", "L|1|N");

    String response = responder.buildResponse(lines);

    assertTrue("No-order response should contain O-record no-order marker", response.contains("|Y\r\n"));
    assertTrue("No-order response should include requested accession", response.contains("O|1|UNKNOWN-01"));
  }

  @Test
  public void buildResponse_WithoutQRecord_ReturnsEmptyString() {
    List<String> lines =
        Arrays.asList("H|\\^&|||GENEXPERT^GeneXpert^4.6.0|||||||LIS2-A2", "L|1|N");

    assertEquals("", responder.buildResponse(lines));
  }

  @Test
  public void buildResponse_WithCaretDelimitedAccession_UsesMatchingComponentWithoutDuplicateLookup() {
    String accessionNumber = "GX-2026-0002";
    Sample sample = org.mockito.Mockito.mock(Sample.class);
    when(sample.getId()).thenReturn("sample-2");
    when(sample.getEnteredDate()).thenReturn(new java.sql.Date(1741262400000L));

    org.openelisglobal.test.valueholder.Test test =
        org.mockito.Mockito.mock(org.openelisglobal.test.valueholder.Test.class);
    when(test.getId()).thenReturn("101");
    Analysis analysis = org.mockito.Mockito.mock(Analysis.class);
    when(analysis.getTest()).thenReturn(test);

    AnalyzerTestMapping mapping = new AnalyzerTestMapping();
    mapping.setAnalyzerTypeId(ANALYZER_TYPE_ID);
    mapping.setAnalyzerTestName("MTB-RIF");
    mapping.setTestId("101");

    when(sampleService.getSampleByAccessionNumber("ALT-ID")).thenReturn(null);
    when(sampleService.getSampleByAccessionNumber(accessionNumber)).thenReturn(sample);
    when(analysisService.getAnalysesBySampleId("sample-2"))
        .thenReturn(Collections.singletonList(analysis));
    when(analyzerTestMappingService.getAll()).thenReturn(Collections.singletonList(mapping));

    List<String> lines =
        Arrays.asList(
            "H|\\^&|||GENEXPERT^GeneXpert^4.6.0|||||||LIS2-A2",
            "Q|1|ALT-ID^" + accessionNumber + "^IGNORED||ALL",
            "L|1|N");

    String response = responder.buildResponse(lines);

    assertTrue(response.contains("O|1|" + accessionNumber + "||^^^MTB-RIF|R|"));
    verify(sampleService).getSampleByAccessionNumber("ALT-ID");
    verify(sampleService, times(1)).getSampleByAccessionNumber(accessionNumber);
  }

  @Test
  public void buildResponse_WithMultipleMappedAnalyzerCodes_JoinsCodesInOrderRecord() {
    String accessionNumber = "GX-2026-0003";
    Sample sample = org.mockito.Mockito.mock(Sample.class);
    when(sample.getId()).thenReturn("sample-3");
    when(sample.getEnteredDate()).thenReturn(new java.sql.Date(1741262400000L));

    org.openelisglobal.test.valueholder.Test firstTest =
        org.mockito.Mockito.mock(org.openelisglobal.test.valueholder.Test.class);
    when(firstTest.getId()).thenReturn("101");
    Analysis firstAnalysis = org.mockito.Mockito.mock(Analysis.class);
    when(firstAnalysis.getTest()).thenReturn(firstTest);

    org.openelisglobal.test.valueholder.Test secondTest =
        org.mockito.Mockito.mock(org.openelisglobal.test.valueholder.Test.class);
    when(secondTest.getId()).thenReturn("102");
    Analysis secondAnalysis = org.mockito.Mockito.mock(Analysis.class);
    when(secondAnalysis.getTest()).thenReturn(secondTest);

    AnalyzerTestMapping firstMapping = new AnalyzerTestMapping();
    firstMapping.setAnalyzerTypeId(ANALYZER_TYPE_ID);
    firstMapping.setAnalyzerTestName("MTB-RIF");
    firstMapping.setTestId("101");

    AnalyzerTestMapping secondMapping = new AnalyzerTestMapping();
    secondMapping.setAnalyzerTypeId(ANALYZER_TYPE_ID);
    secondMapping.setAnalyzerTestName("XDR");
    secondMapping.setTestId("102");

    when(sampleService.getSampleByAccessionNumber(accessionNumber)).thenReturn(sample);
    when(analysisService.getAnalysesBySampleId("sample-3"))
        .thenReturn(Arrays.asList(firstAnalysis, secondAnalysis));
    when(analyzerTestMappingService.getAll()).thenReturn(Arrays.asList(firstMapping, secondMapping));

    List<String> lines =
        Arrays.asList(
            "H|\\^&|||GENEXPERT^GeneXpert^4.6.0|||||||LIS2-A2",
            "Q|1|" + accessionNumber + "||ALL",
            "L|1|N");

    String response = responder.buildResponse(lines);

    assertTrue(response.contains("O|1|" + accessionNumber + "||^^^MTB-RIF\\^^^XDR|R|"));
  }

  @Test
  public void buildResponse_WithDelimiterCharsInPatientData_SanitizesOutboundFields() {
    String accessionNumber = "GX-2026-0004";
    Sample sample = org.mockito.Mockito.mock(Sample.class);
    when(sample.getId()).thenReturn("sample-4");
    when(sample.getEnteredDate()).thenReturn(new java.sql.Date(1741262400000L));

    Person person = org.mockito.Mockito.mock(Person.class);
    when(person.getFirstName()).thenReturn("Jean|Pierre");
    when(person.getLastName()).thenReturn("O'Brien^Smith");

    Patient patient = org.mockito.Mockito.mock(Patient.class);
    when(patient.getNationalId()).thenReturn("PAT|001");
    when(patient.getGender()).thenReturn("M");
    when(patient.getBirthDate()).thenReturn(new Timestamp(479001600000L));
    when(patient.getPerson()).thenReturn(person);

    org.openelisglobal.test.valueholder.Test test =
        org.mockito.Mockito.mock(org.openelisglobal.test.valueholder.Test.class);
    when(test.getId()).thenReturn("101");
    Analysis analysis = org.mockito.Mockito.mock(Analysis.class);
    when(analysis.getTest()).thenReturn(test);

    AnalyzerTestMapping mapping = new AnalyzerTestMapping();
    mapping.setAnalyzerTypeId(ANALYZER_TYPE_ID);
    mapping.setAnalyzerTestName("MTB-RIF");
    mapping.setTestId("101");

    when(sampleService.getSampleByAccessionNumber(accessionNumber)).thenReturn(sample);
    when(sampleHumanService.getPatientForSample(sample)).thenReturn(patient);
    when(analysisService.getAnalysesBySampleId("sample-4")).thenReturn(Collections.singletonList(analysis));
    when(analyzerTestMappingService.getAll()).thenReturn(Collections.singletonList(mapping));

    List<String> lines =
        Arrays.asList(
            "H|\\^&|||GENEXPERT^GeneXpert^4.6.0|||||||LIS2-A2",
            "Q|1|" + accessionNumber + "||ALL",
            "L|1|N");

    String response = responder.buildResponse(lines);

    assertTrue("Response should not contain raw pipe in patient segment", !response.contains("PAT|001"));
    assertTrue("Response should sanitize patient ID", response.contains("PAT 001"));
    assertTrue("Response should sanitize patient name delimiters", response.contains("O'Brien Smith"));
    assertTrue("Response should contain P segment", response.contains("P|1|"));
  }
}
