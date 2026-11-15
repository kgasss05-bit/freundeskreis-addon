package de.ulgu.fkreis.core.ui;

import de.ulgu.fkreis.core.FkreisAddon;
import de.ulgu.fkreis.core.L;
import de.ulgu.fkreis.core.data.WoodType;
import de.ulgu.fkreis.core.gfx.G;
import net.labymod.api.client.gui.screen.activity.AutoActivity;
import net.labymod.api.client.render.matrix.Stack;

/** Holzart-Auswahl: 12 Mini-Regale (4 x 3). Gewaehltes bekommt goldenen Rahmen und "✔". */
@AutoActivity
public class WoodPickerActivity extends FkActivity {

  private final boolean fullscreen;
  private final float[] hover = new float[WoodType.values().length];
  private final float[] pop = new float[WoodType.values().length];

  /** fullscreen = true, wenn aus dem Regal heraus geoeffnet (eigener dunkler Hintergrund). */
  public WoodPickerActivity(boolean fullscreen) {
    this(fullscreen, false);
  }

  /** profile = true: waehlt den Holzrahmen des Steckbriefs statt des Regals. */
  private final boolean profile;

  public WoodPickerActivity(boolean fullscreen, boolean profile) {
    this.fullscreen = fullscreen;
    this.profile = profile;
  }

  private net.labymod.api.configuration.loader.property.ConfigProperty<String> target() {
    var c = FkreisAddon.get().configuration();
    return this.profile ? c.profile().wood() : c.notebook().wood();
  }

  private float tileW() { return (this.w - 44 - 3 * 14) / 4F; }
  private float tileH() { return Math.min(this.tileW() * 0.66F, (this.h - 80) / 3F - 26); }

  private float tileX(int i) { return this.x + 22 + (i % 4) * (this.tileW() + 14); }
  private float tileY(int i) { return this.y + 52 + (i / 4) * (this.tileH() + 30); }

  @Override
  protected void draw(Stack s, float mx, float my) {
    if (this.fullscreen) {
      G.rect(s, this.x, this.y, this.w, this.h, 0xF0151926);
      this.header(s, L.raw("fkreis.settings.notebook.woodPicker.name"), mx, my);
    }
    // In den Einstellungen zeichnet LabyMod die Kopfzeile ("X Holzart") selbst.

    WoodType selected = WoodType.parse(this.target().get());
    float tw = this.tileW(), th = this.tileH();
    WoodType[] all = WoodType.values();
    for (int i = 0; i < all.length; i++) {
      WoodType wt = all[i];
      float tx = this.tileX(i), ty = this.tileY(i);
      boolean over = G.in(mx, my, tx - 4, ty - 4, tw + 8, th + 26);
      this.hover[i] = G.lerp(this.hover[i], over ? 1F : 0F, 0.3F);
      this.pop[i] = G.lerp(this.pop[i], 0F, 0.12F);
      boolean sel = wt == selected;

      ty += -2F * this.hover[i];

      if (sel) {
        G.rect(s, tx - 4, ty - 4, tw + 8, th + 26, 0x26E8B830);
        G.outline(s, tx - 4, ty - 4, tw + 8, th + 26, 2, 0xFFE8B830);
      }
      if (this.profile) {
        miniCard(s, tx, ty, tw, th, wt, i);
      } else {
        miniShelf(s, tx, ty, tw, th, wt, i);
      }
      if (this.hover[i] > 0.01F) {
        G.rect(s, tx, ty, tw, th, G.alpha(0x14FFFFFF, this.hover[i]));
      }
      String label = (sel ? "✔ " : "") + wt.display();
      G.centered(s, label, tx + tw / 2F, ty + th + 6, sel ? 0xFFE8B830 : 0xFFFFFFFF, 1.15F, false);
    }
  }

  /** Mini-Steckbrief: Holzrahmen + helle Karte mit Kopf und Zeilen. */
  public static void miniCard(Stack s, float x, float y, float w, float h, WoodType wt, int seed) {
    G.planks(s, x, y, w, h, wt.plank, seed);
    G.outline(s, x, y, w, h, 1, G.shade(wt.plank, 0.45F));
    float b = Math.max(4, Math.min(w, h) * 0.1F);
    float ix = x + b, iy = y + b, iw = w - 2 * b, ih = h - 2 * b;
    G.rect(s, ix - 1, iy - 1, iw + 2, ih + 2, G.shade(wt.plank, 0.45F));
    G.rect(s, ix, iy, iw, ih, 0xFFF3EAD6);
    float hs = Math.min(iw, ih) * 0.32F;
    G.rect(s, ix + iw / 2F - hs / 2F, iy + 3, hs, hs, 0xFFA8743E);
    float ry = iy + hs + 7;
    while (ry + 4 < iy + ih - 2) {
      G.rect(s, ix + 4, ry, iw - 8, 4, 0xFFE6D8B8);
      ry += 6;
    }
  }

  /** Mini-Regal: Holzrahmen + dunkle Rueckwand + 3 Buecher mit Etikett. */
  public static void miniShelf(Stack s, float x, float y, float w, float h, WoodType wt, int seed) {
    G.planks(s, x, y, w, h, wt.plank, seed);
    G.rect(s, x, y + h - 2, w, 2, G.shade(wt.plank, 0.6F));
    float fx = x + w * 0.09F, fy = y + h * 0.12F, fw = w * 0.82F, fh = h * 0.78F;
    G.rect(s, fx, fy, fw, fh, wt.back);
    G.rect(s, fx, fy, fw, 2, G.shade(wt.back, 0.6F));
    float bh = (fh - 8) / 3F;
    int[] cols = G.BOOK_COLORS;
    for (int b = 0; b < 3; b++) {
      int c = cols[Math.floorMod(seed * 3 + b * 5, cols.length)];
      float off = ((seed + b) % 3) * 2F;
      G.spine(s, fx + 4 + off, fy + 4 + b * (bh + 0.5F), fw - 10 - off, bh - 1, c, true);
    }
  }

  @Override
  protected boolean onClick(float mx, float my, boolean left) {
    if (this.fullscreen && this.headerHit(mx, my)) {
      G.click();
      this.displayPreviousScreen();
      return true;
    }
    WoodType[] all = WoodType.values();
    for (int i = 0; i < all.length; i++) {
      if (G.in(mx, my, this.tileX(i) - 4, this.tileY(i) - 4, this.tileW() + 8, this.tileH() + 26)) {
        this.target().set(all[i].name());
        this.pop[i] = 1F;
        G.sound("block.wood.place", 1.1F);
        return true;
      }
    }
    return false;
  }
}
