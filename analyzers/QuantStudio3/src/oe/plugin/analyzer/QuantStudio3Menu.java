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

import java.util.Locale;
import org.openelisglobal.common.services.PluginMenuService;
import org.openelisglobal.common.services.PluginMenuService.KnownMenu;
import org.openelisglobal.menu.valueholder.Menu;
import org.openelisglobal.plugin.MenuPlugin;

public class QuantStudio3Menu extends MenuPlugin {

  @Override
  protected void insertMenu() {
    PluginMenuService service = PluginMenuService.getInstance();
    Menu menu = new Menu();

    menu.setParent(
        PluginMenuService.getInstance().getKnownMenu(KnownMenu.ANALYZER, "menu_results"));
    // The order this analyzer will show on the menu relative to other analyzers
    menu.setPresentationOrder(10);
    // The id needs to be unique in the system
    menu.setElementId("quantstudio3_analyzer_plugin");
    // This will always be "/AnalyzerResults?type=<The name of the analyzer in
    // the database as specified in then Analyzer class call to
    // addAnalyzerDatabaseParts(....)
    menu.setActionURL("/AnalyzerResults?type=QuantStudio3Analyzer");
    // The key used for the name of the analyzer on the menu. Should not already
    // exist in MessageResource.properties.
    menu.setDisplayKey("banner.menu.results.quantstudio3analyzer");
    menu.setOpenInNewWindow(false);

    service.addMenu(menu);
    // Analyzer name in English
    service.insertLanguageKeyValue(
        "banner.menu.results.quantstudio3analyzer",
        "QuantStudio 3",
        Locale.ENGLISH.toLanguageTag());
    // Analyzer name in French
    service.insertLanguageKeyValue(
        "banner.menu.results.quantstudio3analyzer", "QuantStudio 3", Locale.FRENCH.toLanguageTag());

    menu = new Menu();

    menu.setParent(PluginMenuService.getInstance().getKnownMenu(KnownMenu.WORKPLAN, "0"));
    // The order this analyzer will show on the menu relative to other analyzers
    menu.setPresentationOrder(10);
    // The id needs to be unique in the system
    menu.setElementId("ananlyzer_setup");
    menu.setActionURL("/AnalyzerSetup");
    // The key used for the name of the analyzer on the menu. Should not already
    // exist in MessageResource.properties.
    menu.setDisplayKey("banner.menu.analyzer.setup");
    menu.setOpenInNewWindow(false);
    if (!service.hasMenu(menu)) {
      service.addMenu(menu);
      // Analyzer name in English
      service.insertLanguageKeyValue(
          "banner.menu.analyzer.setup", "Analyzer Run Setup", Locale.ENGLISH.toLanguageTag());
      // Analyzer name in French
      service.insertLanguageKeyValue(
          "banner.menu.analyzer.setup", "Analyzer Run Setup", Locale.FRENCH.toLanguageTag());
    }
  }
}
