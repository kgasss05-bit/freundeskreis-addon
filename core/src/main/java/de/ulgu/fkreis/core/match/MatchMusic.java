package de.ulgu.fkreis.core.match;

import de.ulgu.fkreis.core.FkreisAddon;
import net.labymod.api.Laby;
import net.labymod.api.client.resources.ResourceLocation;
import net.labymod.api.client.resources.sound.PlayingSound;

/**
 * Hintergrundmusik fuer Blockmatch. Das Stueck liegt als assets/fkreis/sounds/blockmatch.ogg im Addon:
 * "Slow S*x (Sexy music Nights)" von arose-xoo, Pixabay Content License,
 * https://pixabay.com/de/music/schlägt-slow-sx-sexy-music-nights-312276/
 */
public final class MatchMusic {

  private static PlayingSound playing;
  private static int playingVolume = -1;
  private static long retryAt;

  private MatchMusic() {
  }

  /** Jeden Frame aus dem Menue aufrufen: startet die Musik, laesst sie in Schleife laufen und folgt dem Lautstaerkeregler. */
  public static void keepPlaying() {
    var settings = FkreisAddon.get().configuration().match();
    int volume = Math.max(0, Math.min(100, Math.round(settings.volume().get())));
    if (!settings.music().get() || volume == 0) {
      stop();
      return;
    }
    if (playing != null && volume != playingVolume) {
      stop(); // Regler bewegt: mit der neuen Lautstaerke neu starten
    }
    long now = System.currentTimeMillis();
    if ((playing != null && playing.isPlaying()) || now < retryAt) {
      return;
    }
    retryAt = now + 1500L;
    try {
      playing = Laby.labyAPI().minecraft().sounds().startSound(ResourceLocation.create("fkreis", "blockmatch"), volume / 100F, 1F);
      playingVolume = volume;
    } catch (RuntimeException e) {
      FkreisAddon.log().warn("Freundeskreis: Blockmatch-Musik konnte nicht gestartet werden: " + e);
    }
  }

  /** Beim Schliessen des Menues: Musik sofort aus. */
  public static void stop() {
    if (playing != null) {
      try {
        playing.stop();
      } catch (RuntimeException e) {
        FkreisAddon.log().warn("Freundeskreis: " + e);
      }
      playing = null;
    }
  }
}
