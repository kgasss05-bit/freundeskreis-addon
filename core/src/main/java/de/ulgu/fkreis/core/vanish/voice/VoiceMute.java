package de.ulgu.fkreis.core.vanish.voice;

import de.ulgu.fkreis.api.VanisherState;
import de.ulgu.fkreis.core.vanish.Vanish;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Stummschalten im LabyMod-Voicechat.
 *
 * Der Voicechat ist ein eigenes LabyMod-Addon ohne oeffentliche Client-Schnittstelle. Die Anbindung
 * (welche Klasse/Methode im Voicechat-Addon die Lautstaerke pro Spieler setzt) wird hier eingetragen,
 * sobald die Voicechat-Jar auf dem PC untersucht ist. Bis dahin merkt sich die Klasse, wer stumm sein soll,
 * und meldet im Log, dass die Anbindung fehlt – es wird nichts vorgetaeuscht.
 */
public class VoiceMute {

  private final Set<UUID> muted = new HashSet<>();
  private boolean warned;

  /** Gleicht ab: alle Ausgeblendeten stumm, alle anderen (die wir stumm gemacht hatten) wieder laut. */
  public void refresh() {
    Vanish addon = Vanish.get();
    boolean active = addon != null && addon.on();
    Set<UUID> want = active ? new HashSet<>(VanisherState.HIDDEN) : new HashSet<>();
    for (UUID uuid : new HashSet<>(this.muted)) {
      if (!want.contains(uuid)) {
        this.unmute(uuid);
      }
    }
    for (UUID uuid : want) {
      if (!this.muted.contains(uuid)) {
        this.mute(uuid);
      }
    }
  }

  public void mute(UUID uuid) {
    if (this.setVolume(uuid, 0F)) {
      this.muted.add(uuid);
    }
  }

  public void unmute(UUID uuid) {
    if (this.muted.remove(uuid)) {
      this.setVolume(uuid, 1F);
    }
  }

  /** Hier kommt die Voicechat-Anbindung rein. Gibt true zurueck, wenn es geklappt hat. */
  private boolean setVolume(UUID uuid, float volume) {
    if (!this.warned) {
      this.warned = true;
      Vanish.log().info("Vanisher: Voicechat-Anbindung noch nicht eingebaut – Stummschalten folgt.");
    }
    return false;
  }
}
