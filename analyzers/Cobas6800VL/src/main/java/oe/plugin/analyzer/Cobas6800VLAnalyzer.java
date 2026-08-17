package oe.plugin.analyzer;

import java.util.ArrayList;
import java.util.List;
import org.openelisglobal.analyzerimport.analyzerreaders.AnalyzerLineInserter;
import org.openelisglobal.common.services.PluginAnalyzerService;
import org.openelisglobal.plugin.AnalyzerImporterPlugin;

//            Cobas6800VLAnalyzerImplementation

public class Cobas6800VLAnalyzer implements AnalyzerImporterPlugin {

  public Cobas6800VLAnalyzer() {
    InstrumentIndex = -1;
  }

  public boolean connect() {
    List<PluginAnalyzerService.TestMapping> nameMappinng =
        new ArrayList<PluginAnalyzerService.TestMapping>();
    nameMappinng.add(new PluginAnalyzerService.TestMapping("Result", "Viral Load"));
    PluginAnalyzerService.getInstance()
        .addAnalyzerDatabaseParts(
            "Cobas6800VLAnalyzer", "Plugin for Cobas6800 VL analyzer", nameMappinng);
    PluginAnalyzerService.getInstance().registerAnalyzer(this);
    return true;
  }

  public AnalyzerLineInserter getAnalyzerLineInserter() {
    return new Cobas6800VLAnalyzerImplementation();
  }

  public boolean isTargetAnalyzer(List lines) {
    for (int j = 0; j < lines.size(); j++)
      if (((String) lines.get(j)).contains("Load Viral"))
        return ((String) lines.get(0)).contains(COBAS_6800_VL_INDICATOR);

    return false;
  }

  private static final CharSequence COBAS_6800_VL_INDICATOR = "^MPL";
  int InstrumentIndex;
}
