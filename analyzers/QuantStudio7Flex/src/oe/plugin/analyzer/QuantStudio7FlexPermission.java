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
 * Copyright (C) ITECH, University of Washington, Seattle WA. All Rights Reserved.
 */

package oe.plugin.analyzer;

import org.openelisglobal.common.services.IPluginPermissionService;
import org.openelisglobal.plugin.PermissionPlugin;
import org.openelisglobal.role.valueholder.Role;
import org.openelisglobal.spring.util.SpringContext;
import org.openelisglobal.systemmodule.valueholder.SystemModule;
import org.openelisglobal.systemmodule.valueholder.SystemModuleUrl;

/**
 * QuantStudio7FlexPermission - Permission plugin for QuantStudio 7 Flex analyzer.
 *
 * M8 Milestone: Madagascar Analyzer Integration (Feature 011)
 */
public class QuantStudio7FlexPermission extends PermissionPlugin {

    @Override
    protected boolean insertPermission() {
        IPluginPermissionService service = SpringContext.getBean(IPluginPermissionService.class);
        SystemModule module = service.getOrCreateSystemModule(
                "AnalyzerResults",
                "QuantStudio7FlexAnalyzer",
                "Results->Analyzer->QuantStudio7FlexAnalyzer");
        Role role = service.getSystemRole("Results");
        SystemModuleUrl moduleUrl = service.getOrCreateSystemModuleUrl(module, "/importAnalyzer");
        return service.bindRoleToModule(role, module, moduleUrl);
    }
}
