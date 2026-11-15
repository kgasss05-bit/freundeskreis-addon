package de.ulgu.fkreis.core;

import net.labymod.api.util.I18n;

/** Kurzform fuer Uebersetzungen (folgt automatisch der Minecraft-Sprache: Deutsch oder Englisch). */
public final class L {

  private L() {
  }

  public static String t(String key, Object... args) {
    return I18n.translate("fkreis.ui." + key, args);
  }

  public static String raw(String fullKey, Object... args) {
    return I18n.translate(fullKey, args);
  }

  /** true, wenn das Spiel auf Deutsch steht (fuer Mob-Namen). */
  public static boolean german() {
    return "Notizen".equals(t("notes.title"));
  }
}
