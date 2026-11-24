package de.ulgu.fkreis.core.vanish;

import net.labymod.api.util.I18n;

/** Kurzform fuer Uebersetzungen (folgt automatisch der Minecraft-Sprache: Deutsch oder Englisch). */
public final class L {

  private L() {
  }

  public static String t(String key, Object... args) {
    return I18n.translate("fkreis.ui.vanish." + key, args);
  }
}
