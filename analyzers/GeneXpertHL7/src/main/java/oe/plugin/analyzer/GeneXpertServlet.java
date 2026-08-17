package oe.plugin.analyzer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;

import org.openelisglobal.analysis.service.AnalysisService;
import org.openelisglobal.analysis.valueholder.Analysis;
import org.openelisglobal.analyzerresults.valueholder.AnalyzerResults;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.patient.valueholder.Patient;
import org.openelisglobal.patient.service.PatientService;
import org.openelisglobal.plugin.ServletPlugin;
import org.openelisglobal.sample.service.SampleService;
import org.openelisglobal.sample.valueholder.Sample;
import org.openelisglobal.spring.util.SpringContext;

import ca.uhn.hl7v2.AcknowledgmentCode;
import ca.uhn.hl7v2.DefaultHapiContext;
import ca.uhn.hl7v2.HL7Exception;
import ca.uhn.hl7v2.HapiContext;
import ca.uhn.hl7v2.custom.factory.PluginModelClassFactory;
import ca.uhn.hl7v2.custom.model.v25.group.RSP_Z02_ORDER;
import ca.uhn.hl7v2.custom.model.v25.group.RSP_Z02_PATIENT;
import ca.uhn.hl7v2.custom.model.v25.message.QBP_Z03;
import ca.uhn.hl7v2.custom.model.v25.message.RSP_Z02;
import ca.uhn.hl7v2.hoh.hapi.server.HohServlet;
import ca.uhn.hl7v2.model.Message;
import ca.uhn.hl7v2.model.Type;
import ca.uhn.hl7v2.model.Varies;
import ca.uhn.hl7v2.model.v25.group.ORU_R01_OBSERVATION;
import ca.uhn.hl7v2.model.v25.group.ORU_R01_ORDER_OBSERVATION;
import ca.uhn.hl7v2.model.v25.group.ORU_R01_PATIENT_RESULT;
import ca.uhn.hl7v2.model.v25.message.ACK;
import ca.uhn.hl7v2.model.v25.message.ORU_R01;
import ca.uhn.hl7v2.model.v25.message.QCN_J01;
import ca.uhn.hl7v2.model.v25.segment.MSH;
import ca.uhn.hl7v2.model.v25.segment.NTE;
import ca.uhn.hl7v2.model.v25.segment.OBR;
import ca.uhn.hl7v2.model.v25.segment.OBX;
import ca.uhn.hl7v2.model.v25.segment.ORC;
import ca.uhn.hl7v2.model.v25.segment.PID;
import ca.uhn.hl7v2.model.v25.segment.QAK;
import ca.uhn.hl7v2.model.v25.segment.QPD;
import ca.uhn.hl7v2.model.v25.segment.SPM;
import ca.uhn.hl7v2.model.v25.segment.TQ1;
import ca.uhn.hl7v2.parser.ModelClassFactory;
import ca.uhn.hl7v2.protocol.ReceivingApplication;
import ca.uhn.hl7v2.protocol.ReceivingApplicationException;

@SuppressWarnings("serial")
public class GeneXpertServlet extends HohServlet implements ServletPlugin {

	private static final String[] LOINC_CODES = { "29615-2", "11011-4", "10351-5", "94500-6" };
	SampleService sampleService = SpringContext.getBean(SampleService.class);
	AnalysisService analysisService = SpringContext.getBean(AnalysisService.class);
	PatientService patientService = SpringContext.getBean(PatientService.class);
	private GeneXpertAnalyzerImplementation inserter ;

	@Override
	public void init(ServletConfig theConfig) throws ServletException {
		ReceivingApplication<Message> application = new GeneXpertApplication();
		setApplication(application);
		inserter = new GeneXpertAnalyzerImplementation();
	}

	/**
	 * The application does the actual processing
	 */
	private class GeneXpertApplication implements ReceivingApplication<Message> {

		private Map<String, Message> activeRequests = new HashMap<>();

		/**
		 * processMessage is fired each time a new message arrives.
		 *
		 * @param message     The message which was received
		 * @param theMetadata A map containing additional information about the message,
		 *                    where it came from, etc.
		 */
		@Override
		public Message processMessage(Message message, @SuppressWarnings("rawtypes") Map theMetadata)
				throws ReceivingApplicationException, HL7Exception {
			LogEvent.logTrace(this.getClass().getSimpleName(), "processMessage", "received an HL7 message for GeneXpert analyzer");
			MSH messageHeader = (MSH) message.get("MSH");

			Message response = null;
			try {
				switch (messageHeader.getMessageType().getMessageStructure().getValueOrEmpty()) {
				case "QBP_Z03":
					HapiContext context = new DefaultHapiContext();
					ModelClassFactory cmf = new PluginModelClassFactory("ca.uhn.hl7v2.custom.model");
					context.setModelClassFactory(cmf);
					response = instrumentSystemSendsHostQueryStart(
							(QBP_Z03) context.getPipeParser().parse(message.encode()), theMetadata);
					break;
				case "QCN_J01":
					response = instrumentSystemCancelsHostQuery((QCN_J01) message, theMetadata);
					break;
				case "ORU_R01":
					response = instrumentSystemUploadsTestResults((ORU_R01) message, theMetadata);
					break;
//				case "QRY_R02":
//					response = hostRequestsTestResults((QRY_R02) message, theMetadata);
//					break;
				default:
					response = message.generateACK(AcknowledgmentCode.AR,
							new HL7Exception("not configured to deal with request"));
				}
			} catch (IOException e) {
				throw new ReceivingApplicationException(e);
			}
			return response;
		}

		private RSP_Z02 instrumentSystemSendsHostQueryStart(QBP_Z03 message,
				@SuppressWarnings("rawtypes") Map theMetadata) throws HL7Exception, IOException {
			LogEvent.logTrace(this.getClass().getSimpleName(), "instrumentSystemSendsHostQueryStart", "received an HL7 message for GeneXpert analyzer: Host query");
			activeRequests.put(message.getMSH().getMessageControlID().getValue(), message);
			Optional<Type> patientId = message.getQPD().getField(3).length > 0
					? Optional.of(((Varies) message.getQPD().getField(3, 0)).getData())
					: Optional.empty();
			Type specimenId = ((Varies) message.getQPD().getField(4)[0]).getData();
			Optional<Type> practicePatientId = message.getQPD().getField(5).length > 0
					? Optional.of(((Varies) message.getQPD().getField(5)[0]).getData())
					: Optional.empty();

			List<Patient> patients = new ArrayList<>();
			if (patientId.isPresent()) {
				LogEvent.logTrace(this.getClass().getSimpleName(), "instrumentSystemSendsHostQueryStart", "looking for patient with id: "+ patientId.get());
			}

			patientId.ifPresent(p -> patients.add(patientService.get(patientId.toString())));

			RSP_Z02 response = instrumentSystemSendsHostQueryStartSuccess(message);
			response.getQAK().getQueryTag().setValue(message.getQPD().getQueryTag().getValue());

			Sample sample = sampleService.getSampleByAccessionNumber(specimenId.toString());
			if (sample != null) {
				LogEvent.logTrace(this.getClass().getSimpleName(), "instrumentSystemSendsHostQueryStart", "found sample for: " + specimenId.toString());

				int pidSeq = 0;
				int orcSeq = 0;
				int obrSeq = 0;
				int spmSeq = 0;
				Patient patient = sampleService.getPatient(sample);
				List<Analysis> analysises = analysisService.getAnalysesBySampleId(sample.getId());
				if (analysises.size() > 0) {
					RSP_Z02_PATIENT z02PatientOrder = response.insertPATIENT(response.getPATIENTReps());
					PID pid = z02PatientOrder.getPID();
					pid.getSetIDPID().setValue(Integer.toString(++pidSeq));
					if (patientId.isPresent()) {
						pid.getPatientID().getIDNumber().setValue(patientId.get().toString());
					}
					for (Analysis analysis : analysises) {
						if (Arrays.asList(LOINC_CODES).contains(analysis.getTest().getLoinc())) {
							RSP_Z02_ORDER z02Order = z02PatientOrder.insertORDER(z02PatientOrder.getORDERReps());
							z02Order.getORC().getOrderControl().setValue("NW");
							z02Order.getORC().getPlacerOrderNumber().getEntityIdentifier()
									.setValue(Integer.toString(++orcSeq));
							z02Order.getORC().getDateTimeOfTransaction().getTime().setValue(new Date());
							z02Order.getOBR().getSetIDOBR().setValue(Integer.toString(++obrSeq));
							z02Order.getOBR().getUniversalServiceIdentifier().getIdentifier()
									.setValue(analysis.getTest().getLoinc());
							z02Order.getOBR().getSpecimenActionCode().setValue("A");
							z02Order.getTQ1().getPriority(0).getIdentifier().setValue("R");
							z02Order.getSPM().getSetIDSPM().setValue(Integer.toString(++spmSeq));
							z02Order.getSPM().getSpecimenID().getPlacerAssignedIdentifier().getEntityIdentifier()
									.setValue(sample.getAccessionNumber());
							z02Order.getSPM().getSpecimenID().getFillerAssignedIdentifier().getEntityIdentifier()
									.setValue(sample.getAccessionNumber());
							z02Order.getSPM().getSpecimenType().getIdentifier().setValue("ORH");
							z02Order.getSPM().getSpecimenRole(0).getIdentifier().setValue("P");
						}
					}
				}
			}
			QAK qak = response.getQAK();
			qak.getQueryTag().setValue(message.getQPD().getQueryTag().getValue());
			qak.getQueryResponseStatus().setValue("OK");
			qak.getMessageQueryName().getIdentifier().setValue("Z03");
			qak.getMessageQueryName().getText().setValue("HOST QUERY");

			QPD qpd = response.getQPD();
			qpd.getMessageQueryName().getIdentifier().setValue("Z03");
			qpd.getMessageQueryName().getText().setValue("HOST QUERY");
			qpd.getUserParametersInsuccessivefields().setData(patientId.get());
			((Varies) qpd.getField(3, 1)).setData(specimenId);
			if (practicePatientId.isPresent()) {
				qpd.getUserParametersInsuccessivefields().getExtraComponents().getComponent(1)
						.setData(practicePatientId.get());
			}
			qpd.getQueryTag().setValue(message.getQPD().getQueryTag().getValue());

			return response;
		}

		private RSP_Z02 instrumentSystemSendsHostQueryStartSuccess(QBP_Z03 message) throws HL7Exception, IOException {
			RSP_Z02 response = new RSP_Z02();
			response = (RSP_Z02) message.fillRSP_Z02ResponseHeader(response, AcknowledgmentCode.AA);

			return response;
		}

		private ACK instrumentSystemCancelsHostQuery(QCN_J01 message, @SuppressWarnings("rawtypes") Map theMetadata)
				throws HL7Exception, IOException {
			activeRequests.remove(message.getQID().getQueryTag().getValue());
			ACK response = instrumentSystemCancelsHostQuerySuccessACK(message);
			return response;
		}

		private ACK instrumentSystemCancelsHostQuerySuccessACK(QCN_J01 message) throws HL7Exception, IOException {
			ACK response = (ACK) message.generateACK(AcknowledgmentCode.CA, null);
			MSH responseMSH = response.getMSH();
			responseMSH.getReceivingApplication().getNamespaceID()
					.setValue(message.getMSH().getSendingApplication().getNamespaceID().getValue());
			responseMSH.getReceivingApplication().getUniversalID()
					.setValue(message.getMSH().getSendingApplication().getUniversalID().getValue());
			responseMSH.getReceivingApplication().getUniversalIDType()
					.setValue(message.getMSH().getSendingApplication().getUniversalIDType().getValue());
			responseMSH.getAcceptAcknowledgmentType().setValue("NE");
			responseMSH.getApplicationAcknowledgmentType().setValue("NE");

			return response;
		}

		private Message instrumentSystemUploadsTestResults(ORU_R01 message,
				@SuppressWarnings("rawtypes") Map theMetadata) throws HL7Exception, IOException {
			LogEvent.logTrace(this.getClass().getSimpleName(), "instrumentSystemUploadsTestResults", "received HL7 result(s) for GeneXpert analyzer: parsing ORU_R01");

			List<AnalyzerResults> resultList = new ArrayList<>();
			List<AnalyzerResults> notMatchedResults = new ArrayList<>();
			for (ORU_R01_PATIENT_RESULT patientResult : message.getPATIENT_RESULTAll()) {
				LogEvent.logTrace(this.getClass().getSimpleName(), "instrumentSystemUploadsTestResults", "parsing patient result");
				PID pid = patientResult.getPATIENT().getPID();
				for (ORU_R01_ORDER_OBSERVATION orderObservation : patientResult.getORDER_OBSERVATIONAll()) {
					LogEvent.logTrace(this.getClass().getSimpleName(), "instrumentSystemUploadsTestResults", "parsing order observation");

					ORC orc = orderObservation.getORC();
					OBR obr = orderObservation.getOBR();
					Optional<NTE> orderNTE = orderObservation.getNTEReps() <= 0 ? Optional.empty()
							: Optional.of(orderObservation.getNTE());

					TQ1 tq1 = orderObservation.getTIMING_QTY().getTQ1();
					Date completedTime = tq1.getEndDateTime().getTime().getValueAsDate();
					SPM spm = orderObservation.getSPECIMEN().getSPM();
					boolean isControl = "Q".equals(spm.getSpecimenRole(0).getIdentifier().getValue());
					String accessionNumber = spm.getSpecimenID().getPlacerAssignedIdentifier().getEntityIdentifier()
							.getValue();
					for (ORU_R01_OBSERVATION observation : orderObservation.getOBSERVATIONAll()) {
						LogEvent.logTrace(this.getClass().getSimpleName(), "instrumentSystemUploadsTestResults", "parsing observation");

						OBX obx = observation.getOBX();
						Optional<NTE> resultNTE = observation.getNTEReps() <= 0 ? Optional.empty()
								: Optional.of(observation.getNTE());
						String patientId = pid.getPatientID().getIDNumber().getValue();

						obr.getUniversalServiceIdentifier().getIdentifier();
						obr.getResultStatus().getValue();
						String observationType = obx.getValueType().getValue();
						String resultUnits = obx.getUnits().getText().getValue();
						String analyzerTestId = obx.getObservationIdentifier().getText().getValue();
						String observationValue = obx.getObservationValue(0).getData().toString();

						inserter.addResult(resultList, notMatchedResults, observationType, observationValue,
								completedTime, accessionNumber, isControl, resultUnits, analyzerTestId);

					}
				}
			}
			LogEvent.logTrace(this.getClass().getSimpleName(), "instrumentSystemUploadsTestResults", "parsing order observation");

			inserter.persistImport(resultList);
			return instrumentSystemUploadsTestResultsSuccessACK(message);
		}

		private ACK instrumentSystemUploadsTestResultsSuccessACK(ORU_R01 message) throws HL7Exception, IOException {
			LogEvent.logTrace(this.getClass().getSimpleName(), "instrumentSystemUploadsTestResultsSuccessACK", "Sending ACK");

			ACK response = (ACK) message.generateACK(AcknowledgmentCode.CA, null);
			MSH responseMSH = response.getMSH();
			responseMSH.getAcceptAcknowledgmentType().setValue("NE");
			responseMSH.getApplicationAcknowledgmentType().setValue("NE");

			return response;
		}

//		private Message hostRequestsTestResults(QRY_R02 message, Map theMetadata) {
//			// TODO Auto-generated method stub
//			return null;
//		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public boolean canProcess(Message theMessage) {
			return true;
		}

	}

}
