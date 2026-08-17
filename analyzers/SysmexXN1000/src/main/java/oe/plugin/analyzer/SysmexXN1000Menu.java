package oe.plugin.analyzer;

import java.util.Locale;
import org.openelisglobal.common.services.PluginMenuService;
import org.openelisglobal.menu.valueholder.Menu;
import org.openelisglobal.plugin.MenuPlugin;

public class SysmexXN1000Menu extends MenuPlugin {

  public SysmexXN1000Menu() {}

  protected void insertMenu() {
    PluginMenuService service = PluginMenuService.getInstance();
    Menu menu = new Menu();
    menu.setParent(
        PluginMenuService.getInstance()
            .getKnownMenu(
                org.openelisglobal.common.services.PluginMenuService.KnownMenu.ANALYZER,
                "menu_results"));
    menu.setPresentationOrder(50);
    menu.setElementId("sysmex_xt_analyzer_plugin");
    menu.setActionURL("/AnalyzerResults?type=SysmexXN1000Analyzer");
    menu.setDisplayKey("banner.menu.results.sysmexxn1000analyzer");
    menu.setOpenInNewWindow(false);
    service.addMenu(menu);
    service.insertLanguageKeyValue(
        "banner.menu.results.sysmexxn1000analyzer",
        "Hematology: Sysmex XN1000 analyzer importer",
        Locale.ENGLISH.toLanguageTag());
    service.insertLanguageKeyValue(
        "banner.menu.results.sysmexxn1000analyzer",
        "H\351matologie: Sysmex XN1000",
        Locale.FRENCH.toLanguageTag());
  }
}
