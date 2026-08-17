package ca.uhn.hl7v2.custom.model.v25.group;

import ca.uhn.hl7v2.HL7Exception;
import ca.uhn.hl7v2.model.AbstractGroup;
import ca.uhn.hl7v2.model.Group;
import ca.uhn.hl7v2.model.v25.segment.PID;
import ca.uhn.hl7v2.parser.ModelClassFactory;

public class RSP_Z02_PATIENT extends AbstractGroup {

	/**
	 * Creates a new RSP_Z02_PATIENT message with custom ModelClassFactory.
	 */
	public RSP_Z02_PATIENT(Group parent, ModelClassFactory factory) {
		super(parent, factory);
		init(factory);
	}

	private void init(ModelClassFactory factory) {
		try {
			this.add(PID.class, true, false);
			this.add(RSP_Z02_ORDER.class, true, true);
		} catch (HL7Exception e) {
			log.error(
					"Unexpected error creating RSP_Z02_PATIENT - this is probably a bug in the source code generator.",
					e);
		}
	}

	public PID getPID() {
		return getTyped("PID", PID.class);
	}

	public RSP_Z02_ORDER getORDER() {
		return getTyped("ORDER", RSP_Z02_ORDER.class);
	}

	public RSP_Z02_ORDER getORDER(int rep) {
		return getTyped("ORDER", rep, RSP_Z02_ORDER.class);
	}

	public int getORDERReps() {
		return getReps("ORDER");
	}

	public java.util.List<RSP_Z02_ORDER> getORDERAll() throws HL7Exception {
		return getAllAsList("ORDER", RSP_Z02_ORDER.class);
	}

	public void insertORDER(RSP_Z02_ORDER structure, int rep) throws HL7Exception {
		super.insertRepetition("ORDER", structure, rep);
	}

	public RSP_Z02_ORDER insertORDER(int rep) throws HL7Exception {
		return (RSP_Z02_ORDER) super.insertRepetition("ORDER", rep);
	}

	public RSP_Z02_ORDER removeORDER(int rep) throws HL7Exception {
		return (RSP_Z02_ORDER) super.removeRepetition("ORDER", rep);
	}
}
