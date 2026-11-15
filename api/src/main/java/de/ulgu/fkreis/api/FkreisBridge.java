package de.ulgu.fkreis.api;

import net.labymod.api.reference.annotation.Referenceable;

/**
 * Kleine versionsunabhaengige Schnittstelle (wie WorldAccess im alten Freundeskreis).
 * (Nicht zwingend noetig, schadet aber nicht.)
 */
@Referenceable
public interface FkreisBridge {

  /** Markenname fuer Anzeigen/Logs. */
  String brand();
}
