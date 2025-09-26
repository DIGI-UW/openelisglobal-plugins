package oe.plugin.analyzer;

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
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletException;

import org.openelisglobal.analysis.service.AnalysisService;
import org.openelisglobal.analyzerresults.valueholder.AnalyzerResults;
import org.openelisglobal.plugin.ServletPlugin;
import org.openelisglobal.sample.service.SampleService;
import org.openelisglobal.spring.util.SpringContext;

@SuppressWarnings("serial")
public class MindrayServlet extends HohServlet implements ServletPlugin {

  SampleService sampleService = SpringContext.getBean(SampleService.class);
  AnalysisService analysisService = SpringContext.getBean(AnalysisService.class);
  private MindrayAnalyzerImplementation inserter = new MindrayAnalyzerImplementation();

  @Override
  public void init(ServletConfig theConfig) throws ServletException {
    ReceivingApplication<Message> application = new MindrayApplication();
    setApplication(application);
  }

  /** The application does the actual processing */
  private class MindrayApplication implements ReceivingApplication<Message> {

    private Map<String, Message> activeRequests = new HashMap<>();

    /**
     * processMessage is fired each time a new message arrives.
     *
     * @param message The message which was received
     * @param theMetadata A map containing additional information about the message, where it came
     *     from, etc.
     */
    @Override
    public Message processMessage(Message message, @SuppressWarnings("rawtypes") Map theMetadata)
        throws ReceivingApplicationException, HL7Exception {
      MSH messageHeader = (MSH) message.get("MSH");

      Message response = null;
      try {
        switch (messageHeader.getMessageType().getMessageStructure().getValueOrEmpty()) {
          case "ORU^R01":
            response = instrumentSystemUploadsTestResults((ORU_R01) message, theMetadata);
            break;
            //				case "QRY_R02":
            //					response = hostRequestsTestResults((QRY_R02) message, theMetadata);
            //					break;
          default:
            response =
                message.generateACK(
                    AcknowledgmentCode.AR, new HL7Exception("not configured to deal with request"));
        }
      } catch (IOException e) {
        throw new ReceivingApplicationException(e);
      }
      return response;
    }

    private Message instrumentSystemUploadsTestResults(
        ORU_R01 message, @SuppressWarnings("rawtypes") Map theMetadata)
        throws HL7Exception, IOException {
      List<AnalyzerResults> resultList = new ArrayList<>();
      List<AnalyzerResults> notMatchedResults = new ArrayList<>();
      for (ORU_R01_PIDPD1NK1NTEPV1PV2ORCOBRNTEOBXNTECTI patientResult :
          message.getPIDPD1NK1NTEPV1PV2ORCOBRNTEOBXNTECTIAll()) {
        PID pid = patientResult.getPIDPD1NK1NTEPV1PV2().getPID();
        for (ORU_R01_ORCOBRNTEOBXNTECTI orderObservation :
            patientResult.getORCOBRNTEOBXNTECTIAll()) {
          ORC orc = orderObservation.getORC();
          OBR obr = orderObservation.getOBR();
          Optional<NTE> orderNTE =
              orderObservation.getNTEReps() <= 0
                  ? Optional.empty()
                  : Optional.of(orderObservation.getNTE());

          for (ORU_R01_OBXNTE observation : orderObservation.getOBXNTEAll()) {
            OBX obx = observation.getOBX();
            Optional<NTE> resultNTE =
                observation.getNTEReps() <= 0
                    ? Optional.empty()
                    : Optional.of(observation.getNTE());

            String patientId = pid.getPatientID().getCx1_ID().getValue();
            String accessionNumber = obr.getPlacerOrderNumber().getEntityIdentifier().getValue();
            String resultUnits;
            String observationType = null;
            String observationValue;
            String analyzerTestId = null;
            boolean isControl = !obr.getRelevantClinicalInfo().isEmpty();
            if (isControl) {
              resultUnits = obr.getFillerField2().getValue();
              observationValue = obr.getFillerField1().getValue();
            } else {
              observationType = obx.getValueType().getValue();
              resultUnits = obx.getUnits().getText().getValue();
              observationValue = obx.getObservationValue(0).getData().encode();
              analyzerTestId = obx.getObservationIdentifier().getText().getValue();
            }

            inserter.addResult(
                resultList,
                notMatchedResults,
                observationType,
                observationValue,
                accessionNumber,
                isControl,
                resultUnits,
                analyzerTestId);
          }
        }
      }
      inserter.persistImport(resultList);

      return instrumentSystemUploadsTestResultsSuccessACK(message);
    }

    private ACK instrumentSystemUploadsTestResultsSuccessACK(ORU_R01 message)
        throws HL7Exception, IOException {
      ACK response = (ACK) message.generateACK(AcknowledgmentCode.AA, null);
      MSA responseMSA = response.getMSA();
      responseMSA.getTextMessage().setValue("Message accepted");
      responseMSA.getErrorCondition().getIdentifier().setValue("0");

      return response;
    }

    //		private Message hostRequestsTestResults(QRY_R02 message, Map theMetadata) {
    //			// TODO Auto-generated method stub
    //			return null;
    //		}

    /** {@inheritDoc} */
    @Override
    public boolean canProcess(Message theMessage) {
      return true;
    }
  }
}
