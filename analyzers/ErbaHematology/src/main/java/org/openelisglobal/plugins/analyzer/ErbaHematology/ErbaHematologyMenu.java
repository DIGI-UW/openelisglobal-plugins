package org.openelisglobal.plugins.analyzer.ErbaHematology;

import java.util.Locale;
import org.openelisglobal.common.services.PluginMenuService;
import org.openelisglobal.common.services.PluginMenuService.KnownMenu;
import org.openelisglobal.menu.valueholder.Menu;
import org.openelisglobal.plugin.MenuPlugin;

/**
 * Registers the ERBA MANNHEIM Hematology analyzer entry in the OpenELIS Analyzer Results menu.
 *
 * <p>The {@code actionURL} uses the analyzer name registered in {@link
 * ErbaHematologyAnalyzer#connect()} via {@code addAnalyzerDatabaseParts("ErbaHematology", …)}.
 */
public class ErbaHematologyMenu extends MenuPlugin {

  @Override
  protected void insertMenu() {
    PluginMenuService service = PluginMenuService.getInstance();

    Menu menu = new Menu();
    menu.setParent(service.getKnownMenu(KnownMenu.ANALYZER, "menu_results"));

    // menu.setPresentationOrder(11);

    menu.setElementId("erba_hematology_analyzer_plugin");

    // Must match the analyzer name passed to addAnalyzerDatabaseParts()
    menu.setActionURL("/AnalyzerResults?type=ErbaHematology");

    // i18n key - must not already exist in MessageResource.properties
    menu.setDisplayKey("banner.menu.results.erbahematologyanalyzer");
    menu.setOpenInNewWindow(false);
    service.addMenu(menu);

    // English label
    service.insertLanguageKeyValue(
        "banner.menu.results.erbahematologyanalyzer",
        "Erba Hematology",
        Locale.ENGLISH.toLanguageTag());

    // French label
    service.insertLanguageKeyValue(
        "banner.menu.results.erbahematologyanalyzer",
        "Erba Hématologie",
        Locale.FRENCH.toLanguageTag());
  }
}
