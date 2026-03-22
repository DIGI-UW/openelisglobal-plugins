package org.openelisglobal.plugins.analyzer.ErbaHematology;

import org.openelisglobal.common.services.IPluginPermissionService;
import org.openelisglobal.plugin.PermissionPlugin;
import org.openelisglobal.role.valueholder.Role;
import org.openelisglobal.spring.util.SpringContext;
import org.openelisglobal.systemmodule.valueholder.SystemModule;

/**
 * Binds the "Results" role to the ErbaHematology analyzer module so that users with that role can
 * access the Analyzer Results page for this instrument.
 *
 * <p>The module path string ({@code "Results->Analyzer->ErbaHematology"}) must match the analyzer
 * name registered in {@link ErbaHematologyAnalyzer}.
 */
public class ErbaHematologyPermission extends PermissionPlugin {

  @Override
  protected boolean insertPermission() {
    IPluginPermissionService service = SpringContext.getBean(IPluginPermissionService.class);

    SystemModule module =
        service.getOrCreateSystemModule(
            "AnalyzerResults", "ErbaHematology", "Results->Analyzer->ErbaHematology");

    Role role = service.getSystemRole("Results");
    return service.bindRoleToModule(role, module);
  }
}
