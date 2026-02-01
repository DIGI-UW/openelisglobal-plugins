package oe.plugin.analyzer;

import java.util.Locale;
import org.openelisglobal.common.services.PluginMenuService;
import org.openelisglobal.menu.valueholder.Menu;
import org.openelisglobal.plugin.MenuPlugin;

public class Cobas6800VLMenu extends MenuPlugin {

  public Cobas6800VLMenu() {}

  protected void insertMenu() {
    PluginMenuService service = PluginMenuService.getInstance();
    Menu menu = new Menu();
    menu.setParent(
        PluginMenuService.getInstance()
            .getKnownMenu(PluginMenuService.KnownMenu.ANALYZER, "menu_results"));
    menu.setPresentationOrder(134);
    menu.setElementId("cobas6800_vl_analyzer_plugin");
    menu.setActionURL("/AnalyzerResults?type=Cobas6800VLAnalyzer");
    menu.setDisplayKey("banner.menu.results.cobas6800vlanalyzer");
    menu.setOpenInNewWindow(false);
    service.addMenu(menu);
    service.insertLanguageKeyValue(
        "banner.menu.results.cobas6800vlanalyzer",
        "Virology: Cobas6800: Viral Load",
        Locale.ENGLISH.toLanguageTag());
    service.insertLanguageKeyValue(
        "banner.menu.results.cobas6800vlanalyzer",
        "Virologie: Cobas6800: Charge Virale",
        Locale.FRENCH.toLanguageTag());
  }
}
