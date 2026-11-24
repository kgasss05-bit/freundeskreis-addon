package de.ulgu.fkreis.api;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Gemeinsamer Zustand zwischen dem Addon (core) und den Mixins (game-runner).
 * Der core schreibt hier rein, die Mixins lesen nur.
 */
public final class VanisherState {

  /** Ausgeblendete Spieler (UUID). */
  public static final Set<UUID> HIDDEN = ConcurrentHashMap.newKeySet();

  /** Addon an/aus. */
  public static volatile boolean enabled = true;

  private VanisherState() {
  }

  /** true = dieser Spieler soll gerade NICHT gezeichnet werden. */
  public static boolean isHidden(UUID uuid) {
    return enabled && uuid != null && HIDDEN.contains(uuid);
  }
}
