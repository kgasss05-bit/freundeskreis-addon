package de.ulgu.fkreis.core.ui;

import de.ulgu.fkreis.core.L;
import de.ulgu.fkreis.core.data.Frame;
import de.ulgu.fkreis.core.gfx.G;
import de.ulgu.fkreis.core.gfx.PhotoTex;
import java.nio.file.Path;
import net.labymod.api.client.render.matrix.Stack;

/** Zeichnet ein Foto im Bilderrahmen (oben/unten/seitlich mit Licht- und Schattenkante + Eck-Nieten). */
public final class PhotoDraw {

  private PhotoDraw() {
  }

  /** x/y/w/h = Aussenmass des Rahmens. Das Bild wird eingepasst (Seitenverhaeltnis bleibt). */
  public static void framed(Stack s, Path file, Frame f, float x, float y, float w, float h, int maxTex) {
    float b = Math.max(3, Math.min(w, h) * 0.07F);
    G.rect(s, x + 2, y + 3, w, h, 0x40000000);
    G.rect(s, x, y, w, h, f.base);
    G.rect(s, x, y, w, 2, f.light);
    G.rect(s, x, y, 2, h, f.light);
    G.rect(s, x, y + h - 2, w, 2, f.dark);
    G.rect(s, x + w - 2, y, 2, h, f.dark);
    float n = Math.max(2, b * 0.4F);
    G.rect(s, x + b / 2F - n / 2F, y + b / 2F - n / 2F, n, n, f.light);
    G.rect(s, x + w - b / 2F - n / 2F, y + b / 2F - n / 2F, n, n, f.light);
    G.rect(s, x + b / 2F - n / 2F, y + h - b / 2F - n / 2F, n, n, f.light);
    G.rect(s, x + w - b / 2F - n / 2F, y + h - b / 2F - n / 2F, n, n, f.light);
    float ix = x + b, iy = y + b, iw = w - 2 * b, ih = h - 2 * b;
    G.rect(s, ix - 1, iy - 1, iw + 2, ih + 2, f.dark);
    G.rect(s, ix, iy, iw, ih, 0xFF1A1C22);
    PhotoTex.Tex t = file == null ? null : PhotoTex.get(file, maxTex);
    if (t == null) {
      G.centered(s, L.t("album.loading"), ix + iw / 2F, iy + ih / 2F - 3, 0xFF8A8F9A, 0.8F, false);
      return;
    }
    float pw = iw, ph = iw / t.ratio;
    if (ph > ih) {
      ph = ih;
      pw = ih * t.ratio;
    }
    G.icon(s, t.icon, ix + (iw - pw) / 2F, iy + (ih - ph) / 2F, pw, ph);
  }
}
