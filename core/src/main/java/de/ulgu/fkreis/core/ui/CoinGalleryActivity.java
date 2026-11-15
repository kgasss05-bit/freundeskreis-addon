package de.ulgu.fkreis.core.ui;

import de.ulgu.fkreis.core.L;
import de.ulgu.fkreis.core.coins.Coins;
import de.ulgu.fkreis.core.gfx.G;
import net.labymod.api.client.gui.screen.activity.AutoActivity;
import net.labymod.api.client.render.matrix.Stack;

/**
 * Einstellungs-Tab "Muenzen ansehen": alle 20 Freundschafts-Muenzen in einem 5 x 4 Raster,
 * jeweils mit Namen und ab wann man sie bekommt. Hover laesst die Muenze leicht wippen.
 */
@AutoActivity
public class CoinGalleryActivity extends FkActivity {

  private final float[] hover = new float[Coins.COUNT];

  @Override
  protected void draw(Stack s, float mx, float my) {
    // Kopfzeile zeichnet LabyMod selbst ("X Muenzen ansehen")
    float top = this.y + 44;
    // Fester dunkler Hintergrund, damit die Muenzen vor jeder Welt gut lesbar sind
    G.rect(s, this.x, this.y, this.w, this.h, 0xE0101218);
    float px = this.x + 12, py = top - 14, pw = this.w - 24, ph = this.h - (py - this.y) - 8;
    G.rect(s, px, py, pw, ph, 0xF01B1F27);
    G.outline(s, px, py, pw, ph, 1F, 0xFF3A404C);
    G.rect(s, px + 1, py + 1, pw - 2, 1, 0x30FFFFFF);
    G.text(s, L.t("coins.intro"), this.x + 24, top - 4, 0xFFB8BCC8, 1F);
    int cols = 5, rows = 4;
    float gx = this.x + 22, gw = this.w - 44;
    float cellW = gw / cols;
    float cellH = Math.max(52, (this.h - (top - this.y) - 22) / rows);
    float size = Math.min(34, Math.min(cellW, cellH) - 24);
    for (int i = 0; i < Coins.COUNT; i++) {
      int c = i % cols, r = i / cols;
      float cx = gx + c * cellW + cellW / 2F;
      float cy = top + 12 + r * cellH;
      boolean over = G.in(mx, my, cx - cellW / 2F, cy, cellW, cellH);
      this.hover[i] = G.lerp(this.hover[i], over ? 1F : 0F, 0.25F);
      float bob = (float) Math.sin(System.currentTimeMillis() / 180.0 + i) * 1.5F * this.hover[i];
      G.rect(s, cx - cellW / 2F + 3, cy, cellW - 6, cellH - 4, 0x10FFFFFF);
      if (this.hover[i] > 0.02F) {
        G.rect(s, cx - cellW / 2F + 3, cy, cellW - 6, cellH - 4, G.alpha(0x22FFFFFF, this.hover[i]));
      }
      float sz = size * (1F + 0.15F * this.hover[i]);
      G.icon(s, Coins.icon(i), cx - sz / 2F, cy + 4 + bob - (sz - size) / 2F, sz, sz);
      G.centered(s, (i + 1) + ". " + Coins.name(i), cx, cy + size + 8, 0xFFFFFFFF, 0.95F, false);
      G.centered(s, Coins.when(i), cx, cy + size + 19, 0xFFB4BAC6, 0.85F, false);
    }
  }
}
