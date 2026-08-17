package ca.uhn.hl7v2.custom.model.v25.message;

import java.io.IOException;
import java.util.Date;
import java.util.GregorianCalendar;

import ca.uhn.hl7v2.AcknowledgmentCode;
import ca.uhn.hl7v2.HL7Exception;
import ca.uhn.hl7v2.Version;
import ca.uhn.hl7v2.model.Message;
import ca.uhn.hl7v2.model.Segment;
import ca.uhn.hl7v2.model.primitive.CommonTS;
import ca.uhn.hl7v2.model.v25.message.QBP_Z73;
import ca.uhn.hl7v2.parser.DefaultModelClassFactory;
import ca.uhn.hl7v2.parser.ModelClassFactory;
import ca.uhn.hl7v2.util.StringUtil;
import ca.uhn.hl7v2.util.Terser;

public class QBP_Z03 extends QBP_Z73 {

    /**
     * Creates a new QCN_J01 message with DefaultModelClassFactory.
     */
    public QBP_Z03() {
        this(new DefaultModelClassFactory());
    }

    /**
     * Creates a new QCN_J01 message with custom ModelClassFactory.
     */
    public QBP_Z03(ModelClassFactory factory) {
        super(factory);
        init(factory);
    }

    private void init(ModelClassFactory factory) {
//      try {
//          this.add(MSH.class, true, false);
//          this.add(QPD.class, true, false);
//          this.add(RCP.class, true, false);
//      } catch (HL7Exception e) {
//          log.error("Unexpected error creating QBP_Z01 - this is probably a bug in the source code generator.", e);
//      }
    }

    /**
     * Returns "2.5"
     */
    @Override
    public String getVersion() {
        return "2.5";
    }

//  public MSH getMSH() throws HL7Exception {
//      return getTyped("MSH", MSH.class);
//  }
//
//  public QPD getQPD() throws HL7Exception {
//      return getTyped("QPD", QPD.class);
//  }
//
//  public RCP getRCP() throws HL7Exception {
//      return getTyped("RCP", RCP.class);
//  }

    public Message fillRSP_Z02ResponseHeader(Message out, AcknowledgmentCode code) throws HL7Exception, IOException {
        Segment mshIn = (Segment) get("MSH");
        Segment mshOut = (Segment) out.get("MSH");

        // get MSH data from incoming message ...
        String fieldSep = Terser.get(mshIn, 1, 0, 1, 1);
        String encChars = Terser.get(mshIn, 2, 0, 1, 1);
        String procID = Terser.get(mshIn, 11, 0, 1, 1);

        // populate outbound MSH using data from inbound message ...
        Terser.set(mshOut, 1, 0, 1, 1, fieldSep);
        Terser.set(mshOut, 2, 0, 1, 1, encChars);
        GregorianCalendar now = new GregorianCalendar();
        now.setTime(new Date());
        Terser.set(mshOut, 7, 0, 1, 1, CommonTS.toHl7TSFormat(now));
        Terser.set(mshOut, 9, 0, 1, 1, "RSP");
        Terser.set(mshOut, 9, 0, 2, 1, Terser.get(mshIn, 9, 0, 2, 1));
        String v = mshOut.getMessage().getVersion();
        if (v != null) {
            Version version = Version.versionOf(v);
            if (version != null && !Version.V25.isGreaterThan(version)) {
                Terser.set(mshOut, 9, 0, 3, 1, "ACK");
            }
        }
        Terser.set(mshOut, 10, 0, 1, 1,
                mshIn.getMessage().getParser().getParserConfiguration().getIdGenerator().getID());
        Terser.set(mshOut, 11, 0, 1, 1, procID);

        String versionId = Terser.get(mshIn, 12, 0, 1, 1);
        if (StringUtil.isBlank(versionId)) {
            versionId = Version.highestAvailableVersionOrDefault().getVersion();
        }
        Terser.set(mshOut, 12, 0, 1, 1, versionId);

        // revert sender and receiver
        Terser.set(mshOut, 3, 0, 1, 1, Terser.get(mshIn, 5, 0, 1, 1));
        Terser.set(mshOut, 4, 0, 1, 1, Terser.get(mshIn, 6, 0, 1, 1));
        Terser.set(mshOut, 5, 0, 1, 1, Terser.get(mshIn, 3, 0, 1, 1));
        Terser.set(mshOut, 6, 0, 1, 1, Terser.get(mshIn, 4, 0, 1, 1));

        // fill MSA for the happy case
        Segment msaOut = (Segment) out.get("MSA");
        Terser.set(msaOut, 1, 0, 1, 1, code.name());
        Terser.set(msaOut, 2, 0, 1, 1, Terser.get(mshIn, 10, 0, 1, 1));
        return out;

    }

}
