package oe.plugin.analyzer;

import org.openelisglobal.common.services.PluginPermissionService;
import org.openelisglobal.plugin.PermissionPlugin;
import org.openelisglobal.role.valueholder.Role;
import org.openelisglobal.systemmodule.valueholder.SystemModule;
import org.openelisglobal.systemmodule.valueholder.SystemModuleUrl;

public class Cobas6800VLPermission extends PermissionPlugin {

  public Cobas6800VLPermission() {}

  protected boolean insertPermission() {
    PluginPermissionService service = new PluginPermissionService();
    SystemModule module =
        service.getOrCreateSystemModule(
            "AnalyzerResults", "Cobas6800VLAnalyzer", "Results->Analyzer->Cobas6800VLAnalyzer");
    SystemModuleUrl moduleUrl = service.getOrCreateSystemModuleUrl(module, "/AnalyzerResults");
    Role role = service.getSystemRole("Results");
    return service.bindRoleToModule(role, module, moduleUrl);
  }
}
