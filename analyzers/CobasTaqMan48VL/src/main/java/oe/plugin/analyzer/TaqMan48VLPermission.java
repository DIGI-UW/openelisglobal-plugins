/*
 * The contents of this file are subject to the Mozilla Public License
 * Version 1.1 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.mozilla.org/MPL/
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
 * License for the specific language governing rights and limitations under
 * the License.
 *
 * The Original Code is OpenELIS code.
 *
 * Copyright (C) ITECH, University of Washington, Seattle WA.  All Rights Reserved.
 */

package oe.plugin.analyzer;

import org.openelisglobal.common.services.PluginPermissionService;
import org.openelisglobal.plugin.PermissionPlugin;
import org.openelisglobal.role.valueholder.Role;
import org.openelisglobal.systemmodule.valueholder.SystemModule;
import org.openelisglobal.systemmodule.valueholder.SystemModuleUrl;

/** */
public class TaqMan48VLPermission extends PermissionPlugin {
  protected boolean insertPermission() {
    PluginPermissionService service = new PluginPermissionService();
    SystemModule module =
        service.getOrCreateSystemModule(
            "AnalyzerResults", "TaqMan48VLAnalyzer", "Results->Analyzer->TaqMan48VLAnalyzer");
    SystemModuleUrl moduleUrl = service.getOrCreateSystemModuleUrl(module, "/AnalyzerResults");
    Role role = service.getSystemRole("Results");
    return service.bindRoleToModule(role, module, moduleUrl);
  }
}
