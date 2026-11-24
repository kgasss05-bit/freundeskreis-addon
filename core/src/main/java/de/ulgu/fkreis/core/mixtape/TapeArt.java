package de.ulgu.fkreis.core.mixtape;

import de.ulgu.fkreis.core.gfx.Px;
import java.util.HashMap;
import java.util.Map;
import net.labymod.api.client.gui.icon.Icon;

/** Pixel-Grafiken fuer die Mixtapes: Kassette (liegend und stehend), Holzkiste und Kassettenrekorder. */
public final class TapeArt {

  private static final int[][] RING = {{0, -2}, {1, -1}, {2, 0}, {1, 1}, {0, 2}, {-1, 1}, {-2, 0}, {-1, -1}};
  private static final Map<String, Icon> CACHE = new HashMap<>();
  private static Icon crate, recorder;

  private TapeArt() {
  }

  /** Liegende Kassette, 46 x 28 Pixel. spin 0..7 dreht die Spulen. */
  public static Icon lying(Tapes.Tape t, int spin) {
    String key = "L" + t.color + "|" + t.title + "|" + (spin & 7);
    Icon ic = CACHE.get(key);
    if (ic == null) {
      if (CACHE.size() > 600) {
        CACHE.clear();
      }
      Px p = new Px(46, 28);
      int c = t.color, d = Px.shade(c, 0.62F), l = Px.shade(c, 1.25F);
      p.r(1, 0, 44, 28, c);
      p.r(0, 1, 46, 26, c);
      p.r(1, 0, 44, 1, l);
      p.r(0, 1, 1, 25, l);
      p.r(1, 26, 44, 2, d);
      p.r(45, 2, 1, 25, d);
      p.r(3, 3, 40, 10, 0xF4EFE2);
      p.r(3, 11, 40, 2, d);
      p.r(3, 3, 40, 1, 0xFFFFFF);
      int tw = Math.min(36, Px.textW(t.title));
      p.text(t.title, 5 + (36 - tw) / 2, 4, 0x2A2018, 36);
      p.r(8, 15, 30, 9, 0x15171C);
      p.r(9, 16, 28, 7, 0x262A33);
      for (int cx : new int[] {15, 30}) {
        p.r(cx - 2, 17, 5, 5, 0xF4EFE2);
        p.r(cx - 3, 18, 7, 3, 0xF4EFE2);
        p.r(cx - 1, 16, 3, 7, 0xF4EFE2);
        int a = spin & 7;
        p.r(cx + RING[a][0], 19 + RING[a][1], 1, 1, 0x15171C);
        p.r(cx + RING[(a + 4) & 7][0], 19 + RING[(a + 4) & 7][1], 1, 1, 0x15171C);
        p.r(cx, 19, 1, 1, 0x8B8F99);
      }
      p.r(19, 18, 8, 3, 0x3A3E48);
      p.r(12, 25, 22, 3, d);
      p.r(15, 26, 2, 1, 0x15171C);
      p.r(29, 26, 2, 1, 0x15171C);
      ic = p.icon(4);
      CACHE.put(key, ic);
    }
    return ic;
  }

  /** Stehende Kassette von der schmalen Seite, 12 x 44 Pixel. */
  public static Icon standing(int color) {
    String key = "S" + color;
    Icon ic = CACHE.get(key);
    if (ic == null) {
      Px p = new Px(12, 44);
      int d = Px.shade(color, 0.6F), l = Px.shade(color, 1.3F);
      p.r(0, 0, 12, 44, color);
      p.r(0, 0, 12, 1, l);
      p.r(0, 0, 1, 44, l);
      p.r(11, 1, 1, 43, d);
      p.r(2, 3, 8, 16, 0xF4EFE2);
      p.r(2, 17, 8, 2, d);
      p.r(4, 5, 1, 10, 0x2A2018);
      p.r(6, 5, 1, 7, 0x2A2018);
      p.r(3, 22, 6, 8, 0x15171C);
      ic = p.icon(4);
      CACHE.put(key, ic);
    }
    return ic;
  }

  /** Vorderseite der Holzkiste mit Griffloch und Schild, 140 x 54 Pixel. */
  public static Icon crate() {
    if (crate == null) {
      Px p = new Px(140, 54);
      int w = 0x8A5630, wd = 0x56301A, wl = 0xA87040;
      p.r(0, 0, 140, 54, w);
      p.r(0, 0, 140, 3, wl);
      p.r(0, 51, 140, 3, wd);
      for (int y = 13; y < 51; y += 13) {
        p.r(0, y, 140, 1, wd);
        p.r(0, y + 1, 140, 1, wl);
      }
      for (int i = 0; i < 40; i++) { // Maserung
        int gx = 9 + (i * 53) % 120, gy = 4 + (i * 29) % 46;
        p.r(gx, gy, 3 + i % 4, 1, i % 2 == 0 ? Px.shade(w, 0.9F) : Px.shade(w, 1.08F));
      }
      p.r(0, 0, 7, 54, wd);
      p.r(133, 0, 7, 54, wd);
      p.r(1, 1, 5, 1, w);
      p.r(134, 1, 5, 1, w);
      for (int[] n : new int[][] {{2, 4}, {2, 46}, {135, 4}, {135, 46}}) {
        p.r(n[0], n[1], 2, 2, 0xC8CCD4);
        p.r(n[0] + 1, n[1] + 1, 1, 1, 0x5A5F6C);
      }
      p.r(50, 8, 38, 12, 0x1A0F07);
      p.r(48, 10, 42, 8, 0x1A0F07);
      p.r(50, 8, 38, 2, 0x000000);
      p.r(50, 18, 38, 2, 0x3A2413);
      p.r(49, 7, 40, 1, wd);
      p.r(49, 20, 40, 1, wl);
      p.r(41, 30, 56, 13, 0xE9D9A8);
      p.r(41, 41, 56, 2, 0xC9B47C);
      p.r(43, 28, 2, 3, 0xC8CCD4);
      p.r(93, 28, 2, 3, 0xC8CCD4);
      p.text("MIXTAPES", 45, 32, 0x56301A, 0);
      crate = p.icon(4);
    }
    return crate;
  }

  /** Kassettenrekorder ohne Kassette, 150 x 112 Pixel. Fach liegt bei (68,53), 62 x 38 gross. */
  public static Icon recorder() {
    if (recorder == null) {
      Px p = new Px(150, 112);
      p.r(34, 14, 82, 4, 0x2A2D36);
      p.r(34, 14, 82, 1, 0x5A5F6C);
      p.r(34, 14, 4, 16, 0x2A2D36);
      p.r(112, 14, 4, 16, 0x2A2D36);
      p.r(128, 4, 1, 26, 0xC8CCD4);
      p.r(127, 3, 3, 2, 0xC8CCD4);
      p.r(6, 28, 138, 84, 0x4A4E58);
      p.r(6, 28, 138, 2, 0x8B8F99);
      p.r(6, 108, 138, 4, 0x262A33);
      for (int x : new int[] {6, 135}) {
        p.r(x, 28, 9, 84, 0x7A4A26);
        p.r(x, 28, 9, 2, 0xA06A3A);
        p.r(x + 2, 30, 1, 78, 0x56301A);
        p.r(x + 6, 30, 1, 78, 0x56301A);
      }
      p.r(15, 46, 120, 1, 0xC8CCD4);
      p.r(15, 47, 120, 1, 0x262A33);
      p.r(19, 32, 70, 11, 0xE9D9A8);
      p.r(19, 42, 70, 1, 0xC9B47C);
      for (int x = 22; x < 88; x += 4) {
        p.r(x, 34, 1, x % 16 == 6 ? 5 : 3, 0x56301A);
      }
      p.r(96, 33, 9, 9, 0x15171C);
      p.r(98, 35, 5, 5, 0xC8CCD4);
      p.r(100, 35, 1, 3, 0x15171C);
      p.r(112, 33, 9, 9, 0x15171C);
      p.r(114, 35, 5, 5, 0xC8CCD4);
      p.r(116, 37, 3, 1, 0x15171C);
      p.r(19, 51, 42, 42, 0x15171C);
      p.r(20, 52, 40, 40, 0x262A33);
      for (int y = 0; y < 9; y++) {
        for (int x = 0; x < 9; x++) {
          if ((x - 4) * (x - 4) + (y - 4) * (y - 4) <= 18) {
            p.r(23 + x * 4, 55 + y * 4, 2, 2, 0x0C0D10);
          }
        }
      }
      p.r(66, 51, 66, 42, 0x15171C);
      p.r(68, 53, 62, 38, 0x0C0D10);
      p.r(66, 51, 66, 2, 0x5A5F6C);
      p.r(66, 91, 66, 2, 0x000000);
      String[] keys = {"rec", "play", "eject"};
      for (int i = 0; i < keys.length; i++) {
        int kx = 26 + i * 36, ky = 97;
        boolean rec = i == 0;
        p.r(kx, ky, 28, 11, rec ? 0xB5241A : 0xD8DBE2);
        p.r(kx, ky, 28, 1, rec ? 0xE0513F : 0xFFFFFF);
        p.r(kx, 106, 28, 2, rec ? 0x6A1A10 : 0x8B8F99);
        int c = rec ? 0xFFFFFF : 0x262A33, mx = kx + 12, my = ky + 3;
        if (i == 0) {
          p.r(mx, my, 4, 4, c);
        } else if (i == 1) {
          p.r(mx, my - 1, 1, 6, c);
          p.r(mx + 1, my, 1, 4, c);
          p.r(mx + 2, my + 1, 1, 2, c);
        } else {
          p.r(mx + 1, my - 1, 2, 1, c);
          p.r(mx, my, 4, 1, c);
          p.r(mx - 1, my + 1, 6, 1, c);
          p.r(mx - 1, my + 3, 6, 1, c);
        }
      }
      recorder = p.icon(4);
    }
    return recorder;
  }
}
