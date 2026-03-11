package org.openelisglobal.plugins.analyzer.genericfile;

import java.util.List;
import org.openelisglobal.analyzerimport.analyzerreaders.AnalyzerLineInserter;
import org.openelisglobal.analyzerimport.analyzerreaders.AnalyzerResponder;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.services.PluginAnalyzerService;
import org.openelisglobal.plugin.AnalyzerImporterPlugin;
import org.openelisglobal.spring.util.SpringContext;

public class GenericFileAnalyzer implements AnalyzerImporterPlugin {

  private static final String PLUGIN_NAME = "GenericFile";

  @Override
  public boolean isGenericPlugin() {
    return true;
  }

  @Override
  public boolean connect() {
    SpringContext.getBean(PluginAnalyzerService.class).registerAnalyzer(this);
    LogEvent.logInfo(
        this.getClass().getSimpleName(),
        "connect",
        PLUGIN_NAME + " plugin registered - profile-driven FILE analyzer");
    return true;
  }

  @Override
  public boolean isTargetAnalyzer(List<String> lines) {
    if (lines == null || lines.isEmpty()) {
      return false;
    }
    // Only match tab-delimited lines (the reader always produces tabs).
    // This plugin is generic and should not claim comma-delimited input
    // that might belong to a more specific plugin.
    return lines.stream().anyMatch(line -> line != null && line.contains("\t"));
  }

  @Override
  public boolean isAnalyzerResult(List<String> lines) {
    return lines != null && !lines.isEmpty();
  }

  @Override
  public AnalyzerLineInserter getAnalyzerLineInserter() {
    return new GenericFileLineInserter();
  }

  @Override
  public AnalyzerResponder getAnalyzerResponder() {
    return null;
  }
}
