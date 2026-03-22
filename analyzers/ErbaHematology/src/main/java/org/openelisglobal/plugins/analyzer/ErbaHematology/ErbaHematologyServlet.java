package org.openelisglobal.plugins.analyzer.ErbaHematology;

import ca.uhn.hl7v2.AcknowledgmentCode;
import ca.uhn.hl7v2.HL7Exception;
import ca.uhn.hl7v2.hoh.hapi.server.HohServlet;
import ca.uhn.hl7v2.model.Message;
import ca.uhn.hl7v2.model.v231.group.ORU_R01_OBXNTE;
import ca.uhn.hl7v2.model.v231.group.ORU_R01_ORCOBRNTEOBXNTECTI;
import ca.uhn.hl7v2.model.v231.group.ORU_R01_PIDPD1NK1NTEPV1PV2ORCOBRNTEOBXNTECTI;
import ca.uhn.hl7v2.model.v231.message.ACK;
import ca.uhn.hl7v2.model.v231.message.ORU_R01;
import ca.uhn.hl7v2.model.v231.segment.MSA;
import ca.uhn.hl7v2.model.v231.segment.MSH;
import ca.uhn.hl7v2.model.v231.segment.NTE;
import ca.uhn.hl7v2.model.v231.segment.OBR;
import ca.uhn.hl7v2.model.v231.segment.OBX;
import ca.uhn.hl7v2.model.v231.segment.ORC;
import ca.uhn.hl7v2.model.v231.segment.PID;
import ca.uhn.hl7v2.protocol.ReceivingApplication;
import ca.uhn.hl7v2.protocol.ReceivingApplicationException;
import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.openelisglobal.analysis.service.AnalysisService;
import org.openelisglobal.analyzerresults.valueholder.AnalyzerResults;
import org.openelisglobal.plugin.ServletPlugin;
import org.openelisglobal.sample.service.SampleService;
import org.openelisglobal.spring.util.SpringContext;

/**
 * HL7 v2.3.1 servlet for ERBA MANNHEIM Hematology Analyzers (H360, H560, ELITE 580).
 *
 * <p>The instrument initiates a persistent TCP connection to the LIS server and transmits ORU^R01
 * messages framed with MLLP ({@code <0x0B>…<0x1C><0x0D>}). This servlet is registered as the HAPI
 * HoH receiving application and processes each inbound message synchronously, returning an ACK^R01
 * acknowledgement.
 *
 * <p>Message structure per protocol section 3.1.2:
 *
 * <pre>
 *   MSH message header (UTF-8, HL7 v2.3.1)
 *   PID patient identification
 *   PV1 patient visit / medical information
 *   {
 *      OBR observation request (sample / QC info)
 *      OBX observation results (one per test parameter)
 *   }
 * </pre>
 *
 * <p>QC messages are distinguished by a non-empty OBR-13 (Relevant Clinical Info) field; when that
 * field is populated the result is flagged as a control.
 *
 * <p>Binary histogram / scattergram OBX segments (value type ED, Base64-encoded BMP/PNG) are
 * silently skipped. Only numeric (NM) and coded (IS/ST) results are forwarded to the inserter.
 */
@SuppressWarnings("serial")
public class ErbaHematologyServlet extends HohServlet implements ServletPlugin {

  private final SampleService sampleService = SpringContext.getBean(SampleService.class);
  private final AnalysisService analysisService = SpringContext.getBean(AnalysisService.class);
  private final ErbaHematologyAnalyzerLineInserter inserter =
      new ErbaHematologyAnalyzerLineInserter();

  // Servlet lifecycle

  @Override
  public void init(ServletConfig theConfig) throws ServletException {
    setApplication(new ErbaHematologyApplication());
  }

  // Inner ReceivingApplication

  /**
   * Only ORU^R01 (test / QC result upload) is currently implemented; all other message types
   * receive an AR (Application Reject) ACK.
   */
  private class ErbaHematologyApplication implements ReceivingApplication<Message> {

    @SuppressWarnings("unused")
    private final Map<String, Message> activeRequests = new HashMap<>();

    @Override
    public Message processMessage(Message message, @SuppressWarnings("rawtypes") Map theMetadata)
        throws ReceivingApplicationException, HL7Exception {

      MSH msh = (MSH) message.get("MSH");
      String messageStructure = msh.getMessageType().getMessageStructure().getValueOrEmpty();

      Message response;
      try {
        switch (messageStructure) {
          case "ORU^R01":
            // Instrument uploads test results or QC data
            response = handleResultsUpload((ORU_R01) message, theMetadata);
            break;
          default:
            response =
                message.generateACK(
                    AcknowledgmentCode.AR,
                    new HL7Exception("Message type not configured: " + messageStructure));
        }
      } catch (IOException e) {
        throw new ReceivingApplicationException(e);
      }
      return response;
    }

    // ORU^R01 processing

    /**
     * Iterates through every patient / order / observation group in the ORU message and forwards
     * each numeric/coded OBX result to the inserter.
     *
     * <p>Protocol notes:
     *
     * <ul>
     *   <li>OBR-3 (Filler Order Number) carries the sample ID for patient results; OBR-2 (Placer
     *       Order Number) is used in bi-directional query responses.
     *   <li>OBR-13 (Relevant Clinical Info) is non-empty for QC messages ({@code isControl =
     *       true}).
     *   <li>OBX-3 identifier format: {@code ID^Name^EncodeSys} the ID component is used as the
     *       analyzerTestId key.
     *   <li>Binary histogram/scattergram segments have OBX-2 = ED and are skipped.
     * </ul>
     */
    private Message handleResultsUpload(
        ORU_R01 message, @SuppressWarnings("rawtypes") Map theMetadata)
        throws HL7Exception, IOException {

      List<AnalyzerResults> resultList = new ArrayList<>();
      List<AnalyzerResults> notMatchedResults = new ArrayList<>();

      for (ORU_R01_PIDPD1NK1NTEPV1PV2ORCOBRNTEOBXNTECTI patientResult :
          message.getPIDPD1NK1NTEPV1PV2ORCOBRNTEOBXNTECTIAll()) {

        @SuppressWarnings("unused")
        PID pid = patientResult.getPIDPD1NK1NTEPV1PV2().getPID();

        for (ORU_R01_ORCOBRNTEOBXNTECTI orderObservation :
            patientResult.getORCOBRNTEOBXNTECTIAll()) {

          @SuppressWarnings("unused")
          ORC orc = orderObservation.getORC();
          OBR obr = orderObservation.getOBR();

          // OBR-3 holds the sample/file ID for patient and QC results
          String accessionNumber = obr.getFillerOrderNumber().getEntityIdentifier().getValue();

          // Non-empty OBR-13 flags this as a QC control sample
          boolean isControl = !obr.getRelevantClinicalInfo().isEmpty();

          Optional<NTE> orderNTE =
              orderObservation.getNTEReps() <= 0
                  ? Optional.empty()
                  : Optional.of(orderObservation.getNTE());

          for (ORU_R01_OBXNTE observation : orderObservation.getOBXNTEAll()) {
            OBX obx = observation.getOBX();

            String valueType = obx.getValueType().getValue();

            // Skip binary image segments (histograms, scattergrams)
            if ("ED".equalsIgnoreCase(valueType)) {
              continue;
            }

            // OBX-3: ID^Name^EncodeSys  use the ID component as the lookup key
            String analyzerTestId = obx.getObservationIdentifier().getIdentifier().getValue();

            String observationValue = obx.getObservationValue(0).getData().encode();

            String resultUnits = obx.getUnits().getText().getValue();

            inserter.addResult(
                resultList,
                notMatchedResults,
                valueType,
                observationValue,
                accessionNumber,
                isControl,
                resultUnits,
                analyzerTestId);
          }
        }
      }

      inserter.persistImport(resultList);

      return buildSuccessACK(message);
    }

    // ACK builder

    /**
     * Builds an ACK^R01 acknowledgement with MSA-1 = AA (message accepted) per protocol section
     * 3.3.1.
     *
     * <p>The MSA-2 value is set to the MSH-10 message control ID of the received message so the
     * instrument can correlate the ACK.
     */
    private ACK buildSuccessACK(ORU_R01 message) throws HL7Exception, IOException {
      ACK response = (ACK) message.generateACK(AcknowledgmentCode.AA, null);
      MSA msa = response.getMSA();
      msa.getTextMessage().setValue("Message accepted");
      msa.getErrorCondition().getIdentifier().setValue("0");
      return response;
    }

    @Override
    public boolean canProcess(Message theMessage) {
      return true;
    }
  }
}
