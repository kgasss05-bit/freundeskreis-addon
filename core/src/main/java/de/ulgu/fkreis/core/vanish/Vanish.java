package de.ulgu.fkreis.core.vanish;

import de.ulgu.fkreis.core.FkreisAddon;
import de.ulgu.fkreis.core.vanish.data.HiddenStore;
import de.ulgu.fkreis.core.vanish.voice.VoiceMute;
import java.util.UUID;
import net.labymod.api.util.logging.Logging;

/**
 * Vanish – jetzt ein Teil von Freundeskreis.
 * Spieler nur fuer dich ausblenden, auch wenn alle Spieler angezeigt werden.
 */
public final class Vanish {

  private static Vanish instance;
  private static final Logging LOG = Logging.create("Freundeskreis-Vanish");

  private final FkreisAddon addon;
  private final HiddenStore store;
  private final VoiceMute voice;

  private Vanish(FkreisAddon addon) {
    this.addon = addon;
    this.store = HiddenStore.load();
    this.voice = new VoiceMute();
  }

  public static Vanish init(FkreisAddon addon) {
    instance = new Vanish(addon);
    return instance;
  }

  public static Logging log() {
    return LOG;
  }

  public static Vanish get() {
    return instance;
  }

  public HiddenStore store() {
    return this.store;
  }

  public VoiceMute voice() {
    return this.voice;
  }

  /** Vanish wirkt nur, wenn Freundeskreis UND der Bereich "Vanish" eingeschaltet sind. */
  public boolean on() {
    return this.addon.on() && this.addon.configuration().vanish().enabled().get();
  }

  public boolean isHidden(UUID uuid) {
    return this.store.isHidden(uuid);
  }

  /** Spieler ausblenden oder wieder einblenden (Freundeskreis-Rad, Schriftrolle). */
  public void toggle(UUID uuid, String name) {
    if (this.store.isHidden(uuid)) {
      this.show(uuid);
    } else {
      this.store.hide(uuid, name);
      this.voice.refresh();
    }
  }

  public void show(UUID uuid) {
    this.store.show(uuid);
    this.voice.unmute(uuid);
  }

  public void showAll() {
    for (String key : this.store.hidden.keySet()) {
      try {
        this.voice.unmute(UUID.fromString(key));
      } catch (IllegalArgumentException ignored) {
      }
    }
    this.store.showAll();
  }
}
