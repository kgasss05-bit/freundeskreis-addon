package de.ulgu.fkreis.api;

import java.util.function.BooleanSupplier;

/**
 * Gemeinsamer Zustand zwischen dem Tagebuch (core) und dem Mixin, das beim Beenden des Spiels nachfragt.
 * Der core traegt ein, OB gefragt werden soll und WIE die Frage geoeffnet wird; das Mixin haelt nur kurz an.
 */
public final class DiaryState {

  /** true = vor dem Beenden soll gefragt werden (Einstellung an, heute noch kein Eintrag). */
  public static volatile BooleanSupplier shouldAsk;
  /** Oeffnet die Frage "Moechtest du vorher noch einen Tagebucheintrag machen?". */
  public static volatile Runnable openAsk;

  /** Die Frage oder das Buch ist gerade offen, das Beenden wartet. */
  public static volatile boolean busy;
  /** Beenden ist freigegeben (Antwort gegeben oder Frage ausgeschaltet). */
  public static volatile boolean allow;
  /** Der core moechte das Spiel jetzt wirklich beenden. */
  public static volatile boolean quitNow;
  public static volatile long askedAt;
  /** true, sobald der Spiel-Eingriff (nur auf dafuer gebauten Versionen) laeuft. Sonst fragt das Addon beim Server-Verlassen. */
  public static volatile boolean hookActive;
  /** Anzahl offener Tagebuch-Bildschirme, die zum Beenden gehoeren. */
  public static volatile int screens;

  private DiaryState() {
  }
}
