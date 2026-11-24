package de.ulgu.fkreis.core.zeitbrief;

import de.ulgu.fkreis.core.gfx.Px;
import net.labymod.api.client.gui.icon.Icon;

/** Pixel-Grafiken fuer den Zeitbrief: der Umschlag in Einzelteilen. */
public final class LetterArt {

  public static final int EW = 160, EH = 100, APEX = 56;
  private static Icon back, front, flapUp, flapDown;

  private LetterArt() {
  }

  // Der Umschlag wird grob gezeichnet (ein Grafikpixel = 4 Szenenpixel), damit er wie ein Minecraft-Gegenstand wirkt.
  private static final int GW = 40, GH = 25, GA = 14, OUT = 0x4A3A1C;

  public static Icon back() {
    if (back == null) {
      Px p = new Px(GW, GH);
      p.r(0, 0, GW, GH, 0xB89F66);
      p.r(0, 0, GW, 1, OUT);
      p.r(0, 0, 1, GH, OUT);
      p.r(GW - 1, 0, 1, GH, OUT);
      back = p.icon(8);
    }
    return back;
  }

  /** Die Tasche vorn mit V-Ausschnitt; oberhalb des V durchsichtig. */
  public static Icon front() {
    if (front == null) {
      Px p = new Px(GW, GH);
      for (int x = 0; x < GW; x++) {
        float t = 1 - Math.abs(x + 0.5F - GW / 2F) / (GW / 2F);
        int top = Math.round(t * GA);
        p.r(x, top, 1, GH - top, 0xE6D49C);
        p.r(x, top, 1, 1, 0xC2AA6C);
      }
      p.r(0, GH - 2, GW, 1, 0xCDB87E);
      p.r(0, GH - 1, GW, 1, OUT);
      p.r(0, 0, 1, GH, OUT);
      p.r(GW - 1, 0, 1, GH, OUT);
      front = p.icon(8);
    }
    return front;
  }

  private static Icon tri(boolean up, int col) {
    Px p = new Px(GW, GA);
    for (int i = 0; i < GA; i++) {
      int half = Math.round((GW / 2F) * (1 - i / (float) GA));
      int y = up ? GA - 1 - i : i;
      p.r(GW / 2 - half, y, half * 2, 1, col);
      p.r(GW / 2 - half, y, 1, 1, OUT);
      p.r(GW / 2 + half - 1, y, 1, 1, OUT);
    }
    return p.icon(8);
  }

  /** Offene Klappe (Spitze oben) und geschlossene Klappe (Spitze unten). */
  public static Icon flapUp() {
    if (flapUp == null) {
      flapUp = tri(true, 0xD6C088);
    }
    return flapUp;
  }

  public static Icon flapDown() {
    if (flapDown == null) {
      flapDown = tri(false, 0xEEDFAE);
    }
    return flapDown;
  }
}
