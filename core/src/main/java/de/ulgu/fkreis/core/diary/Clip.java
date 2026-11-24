package de.ulgu.fkreis.core.diary;

import net.labymod.api.Laby;

/** Zwischenablage ueber LabyMods offizielle Schnittstelle. */
final class Clip {

  private Clip() {
  }

  /** Text aus der Zwischenablage, "" wenn leer oder nicht lesbar. */
  static String get() {
    try {
      String s = Laby.labyAPI().minecraft().getClipboard();
      return s == null ? "" : s;
    } catch (RuntimeException e) {
      return "";
    }
  }

  static boolean set(String text) {
    try {
      Laby.labyAPI().minecraft().setClipboard(text);
      return true;
    } catch (RuntimeException e) {
      return false;
    }
  }
}
