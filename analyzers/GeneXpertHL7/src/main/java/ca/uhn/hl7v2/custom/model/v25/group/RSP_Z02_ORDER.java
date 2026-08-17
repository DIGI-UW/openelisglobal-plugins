package ca.uhn.hl7v2.custom.model.v25.group;

import ca.uhn.hl7v2.HL7Exception;
import ca.uhn.hl7v2.model.AbstractGroup;
import ca.uhn.hl7v2.model.Group;
import ca.uhn.hl7v2.model.v25.segment.OBR;
import ca.uhn.hl7v2.model.v25.segment.ORC;
import ca.uhn.hl7v2.model.v25.segment.SPM;
import ca.uhn.hl7v2.model.v25.segment.TQ1;
import ca.uhn.hl7v2.parser.ModelClassFactory;

public class RSP_Z02_ORDER extends AbstractGroup {

	/**
	 * Creates a new RSP_Z02_ORDER message with custom ModelClassFactory.
	 */
	public RSP_Z02_ORDER(Group parent, ModelClassFactory factory) {
		super(parent, factory);
		init(factory);
	}

	private void init(ModelClassFactory factory) {
		try {
			this.add(ORC.class, true, false);
			this.add(OBR.class, true, false);
			this.add(TQ1.class, true, false);
			this.add(SPM.class, true, false);
		} catch (HL7Exception e) {
			log.error("Unexpected error creating RSP_Z02_ORDER - this is probably a bug in the source code generator.",
					e);
		}
	}

	public ORC getORC() {
		return getTyped("ORC", ORC.class);
	}

	public OBR getOBR() {
		return getTyped("OBR", OBR.class);
	}

	public TQ1 getTQ1() {
		return getTyped("TQ1", TQ1.class);
	}

	public SPM getSPM() {
		return getTyped("SPM", SPM.class);
	}

}
