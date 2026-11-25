package de.ulgu.fkreis.core.mixtape;

import de.ulgu.fkreis.core.L;
import de.ulgu.fkreis.core.gfx.G;
import de.ulgu.fkreis.core.gfx.Sys;
import net.labymod.api.client.render.matrix.Stack;

/**
 * Der Kassettenrekorder: merkt sich, welche Kassette laeuft, startet den Song ueber den YouTube-Link im Browser
 * und zeichnet den Rekorder (im Menue und im HUD-Widget).
 */
public class Player {

  private static Tapes.Tape current;
  private static long since;

  public static long since() {
    return since;
  }

  public static Tapes.Tape current() {
    return current;
  }

  /** Kassette einlegen: Song starten und Bescheid sagen. */
  public static void play(Tapes.Tape tape) {
    current = tape;
    since = System.currentTimeMillis();
    Sys.openUrl(tape.link);
    Sys.chat(L.t("mixtape.nowPlaying", tape.title));
  }

  public static void eject() {
    current = null;
  }

  /** Zeichnet den Rekorder samt eingelegter Kassette; {@code s} = Bildschirmpixel je Grafikpixel. */
  public static void drawRecorder(Stack st, float x, float y, float s, Tapes.Tape tape, long insertedAt, boolean keys) {
    long now = System.currentTimeMillis();
    G.icon(st, TapeArt.recorder(), x, y, 150 * s, 112 * s);
    boolean on = tape != null;
    long t = now - insertedAt;
    G.rect(st, x + (19 + (on ? 20 + (now / 400) % 30 : 34)) * s, y + 33 * s, s, 9 * s, 0xFFD8381C); // Zeiger der Skala
    G.rect(st, x + 126 * s, y + 35 * s, 4 * s, 4 * s, on && (now / 500) % 2 == 0 ? 0xFFFF5A3A : 0xFF6A1A10);
    if (on) {
      G.icon(st, TapeArt.lying(tape, t > 750 ? (int) (now / 110) : 0), x + 76 * s, y + 58 * s, 46 * s, 28 * s);
      float door = Math.min(1F, t / 450F); // Klappe faehrt von oben zu
      G.rect(st, x + 68 * s, y + 53 * s, 62 * s, 38 * s * door, 0x269FD8E8);
      G.rect(st, x + 68 * s, y + 53 * s, 62 * s, s, 0xFFC8CCD4);
      G.rect(st, x + 68 * s, y + (53 + 38 * door - 1) * s, 62 * s, s, 0xFFC8CCD4);
      if (keys) {
        G.rect(st, x + 62 * s, y + 97 * s, 28 * s, 11 * s, 0x40000000); // Abspieltaste ist gedrueckt
      }
    }
  }
}
