package ca.uhn.hl7v2.custom.model.v25.message;

import ca.uhn.hl7v2.HL7Exception;
import ca.uhn.hl7v2.custom.model.v25.group.RSP_Z02_PATIENT;
import ca.uhn.hl7v2.model.AbstractMessage;
import ca.uhn.hl7v2.model.v25.message.OML_O21;
import ca.uhn.hl7v2.model.v25.segment.MSA;
import ca.uhn.hl7v2.model.v25.segment.MSH;
import ca.uhn.hl7v2.model.v25.segment.QAK;
import ca.uhn.hl7v2.model.v25.segment.QPD;
import ca.uhn.hl7v2.parser.DefaultModelClassFactory;
import ca.uhn.hl7v2.parser.ModelClassFactory;

public class RSP_Z02 extends AbstractMessage {

    /**
     * Creates a new RSP_Z02 message with DefaultModelClassFactory.
     */
    public RSP_Z02() {
        this(new DefaultModelClassFactory());
    }

    /**
     * Creates a new RSP_Z02 message with custom ModelClassFactory.
     */
    public RSP_Z02(ModelClassFactory factory) {
        super(factory);
        init(factory);
    }

    private void init(ModelClassFactory factory) {
        try {
            this.add(MSH.class, true, false);
            this.add(MSA.class, true, false);
            this.add(QAK.class, true, false);
            this.add(QPD.class, true, false);
            this.add(RSP_Z02_PATIENT.class, false, true);
            OML_O21 m;
        } catch (HL7Exception e) {
            log.error("Unexpected error creating RSP_Z02 - this is probably a bug in the source code generator.", e);
        }
    }

    /**
     * Returns "2.5"
     */
    @Override
    public String getVersion() {
        return "2.5";
    }

    public MSH getMSH() {
        return getTyped("MSH", MSH.class);
    }

    public MSA getMSA() {
        return getTyped("MSA", MSA.class);
    }

    public QAK getQAK() {
        return getTyped("QAK", QAK.class);
    }

    public QPD getQPD() {
        return getTyped("QPD", QPD.class);
    }

    public RSP_Z02_PATIENT getPATIENT() {
        return getTyped("PATIENT", RSP_Z02_PATIENT.class);
    }

    public RSP_Z02_PATIENT getPATIENT(int rep) {
        return getTyped("PATIENT", rep, RSP_Z02_PATIENT.class);
    }

    public int getPATIENTReps() {
        return getReps("PATIENT");
    }

    public java.util.List<RSP_Z02_PATIENT> getPATIENTAll() throws HL7Exception {
        return getAllAsList("PATIENT", RSP_Z02_PATIENT.class);
    }

    public void insertPATIENT(RSP_Z02_PATIENT structure, int rep) throws HL7Exception {
        super.insertRepetition("PATIENT", structure, rep);
    }

    public RSP_Z02_PATIENT insertPATIENT(int rep) throws HL7Exception {
        return (RSP_Z02_PATIENT) super.insertRepetition("PATIENT", rep);
    }

    public RSP_Z02_PATIENT removePATIENT(int rep) throws HL7Exception {
        return (RSP_Z02_PATIENT) super.removeRepetition("PATIENT", rep);
    }

}
