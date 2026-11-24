package de.ulgu.fkreis.core.gfx;

import de.ulgu.fkreis.core.FkreisAddon;
import net.labymod.api.Laby;
import net.labymod.api.client.component.Component;

/** Zwei kleine Dinge ueber LabyMods Chat-Schnittstelle: einen Link im Browser oeffnen und eine Zeile nur fuer dich in den Chat schreiben. */
public final class Sys {

  private Sys() {
  }

  public static boolean openUrl(String url) {
    try {
      Laby.labyAPI().minecraft().chatExecutor().openUrl(url);
      return true;
    } catch (RuntimeException e) {
      FkreisAddon.log().warn("Freundeskreis: Link konnte nicht geoeffnet werden: " + e);
      return false;
    }
  }

  public static void chat(String message) {
    try {
      Laby.labyAPI().minecraft().chatExecutor().displayClientMessage(Component.text(message));
    } catch (RuntimeException e) {
      FkreisAddon.log().warn("Freundeskreis: " + e);
    }
  }

  public static String clipboard() {
    try {
      String s = Laby.labyAPI().minecraft().getClipboard();
      return s == null ? "" : s;
    } catch (RuntimeException e) {
      return "";
    }
  }
}
