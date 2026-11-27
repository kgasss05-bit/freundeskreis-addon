package de.ulgu.fkreis.core.gfx;

import net.labymod.api.client.gui.icon.Icon;
import net.labymod.api.client.render.matrix.Stack;
import net.labymod.api.client.resources.ResourceLocation;

/**
 * Die Grotte: erst steht man draussen in einer kleinen Landschaft vor einem Huegel mit Hoehleneingang. Ein Klick auf
 * den Eingang zoomt hinein; drinnen ist es dunkel, an beiden Seiten brennt eine Fackel, von der Decke haengen
 * Leuchtbeeren, Spinnweben und Tropfsteine, von denen Wasser tropft. Alles ist aus den Block-Texturen des Spiels
 * zusammengesetzt; im Addon liegt keine Mojang-Grafik.
 */
public final class Grotto {

  private static final java.util.Map<String, Icon> BLOCKS = new java.util.HashMap<>();
  private static final long ZOOM_MS = 750L, FADE_MS = 450L;
  /** Umriss des Hoehlenlochs, Zeile fuer Zeile von oben: {Einzug, Breite} in Drittel-Bloecken. */
  private static final int[][] HOLE = {{3, 3}, {2, 5}, {1, 7}, {0, 8}, {0, 9}, {0, 9}, {0, 9}};
  private static final String[] FLOWERS = {"poppy", "dandelion", "cornflower", "allium", "azure_bluet", "oxeye_daisy"};

  /** Hoehe des Bodens in der Grotte. */
  public static final float FLOOR = 66F;

  private long enteredAt;

  public static Icon block(String name) {
    Icon icon = BLOCKS.get(name);
    if (icon == null) {
      icon = Icon.texture(ResourceLocation.create("minecraft", "textures/block/" + name + ".png"));
      BLOCKS.put(name, icon);
    }
    return icon;
  }

  private static void tile(Stack s, String name, float x, float y, float size) {
    G.icon(s, block(name), x, y, size + 0.5F, size + 0.5F);
  }

  private static int hash(int a, int b) {
    int h = a * 73856093 ^ b * 19349663;
    return (h ^ (h >>> 13)) & 0x7FFFFFFF;
  }

  /** Derselbe Boden wie in der Grotte, aber nur zwischen from und to: so geht er unter dem Gleis nahtlos bis zum Rand weiter. */
  public static void floorStrip(Stack s, float x, float y, float w, float h, float from, float to) {
    float[] rh = {14F, 21F, 31F};
    float yy = y + h - FLOOR;
    for (int r = 0; r < 3; r++) {
      float ts = rh[r] * 1.7F, left = x + (w - ((int) (w / ts) + 2) * ts) / 2F;
      for (int c = 0; c * ts < w + ts; c++) {
        float tx = left + c * ts;
        if (tx + ts > from && tx < to) {
          G.icon(s, block(hash(c, r + 50) % 3 == 0 ? "cobbled_deepslate" : "deepslate"), tx, yy, ts + 0.5F, rh[r] + 0.5F);
          G.rect(s, tx, yy, ts + 0.5F, rh[r] + 0.5F, G.alpha(0xFF000000, 0.46F - r * 0.12F));
        }
      }
      yy += rh[r];
    }
  }

  /** Ohne Aussenansicht direkt in der Grotte stehen. */
  public void skipOutside() {
    this.enteredAt = System.currentTimeMillis() - ZOOM_MS;
  }

  /** true, sobald man in der Grotte steht (das Hineinzoomen ist vorbei). */
  public boolean inside() {
    return this.enteredAt > 0 && System.currentTimeMillis() - this.enteredAt >= ZOOM_MS;
  }

  // ------------------------------------------------------------------ draussen

  /** Der Hoehleneingang im Huegel: {x, y, w, h}. */
  private static float[] entrance(float x, float y, float w, float h) {
    float t = tileSize(w, h), floor = y + h - t;
    return new float[] {x + w / 2F - 1.5F * t, floor - 7F * t / 3F, 3 * t, 7F * t / 3F};
  }

  /** Grosse Bloecke: man steht dicht vor der Felswand. */
  private static float tileSize(float w, float h) {
    return Math.max(24F, Math.min(56F, Math.min(w / 11F, h / 6.5F)));
  }

  /** Klick draussen: auf den Eingang geklickt? Dann geht es hinein. Liefert true, wenn der Klick verbraucht ist. */
  public boolean click(float mx, float my, float x, float y, float w, float h) {
    if (this.enteredAt > 0) {
      return !this.inside(); // waehrend des Hineinzoomens nichts anderes zulassen
    }
    float[] e = entrance(x, y, w, h);
    if (G.in(mx, my, e[0] - 6, e[1] - 6, e[2] + 12, e[3] + 6)) {
      this.enteredAt = System.currentTimeMillis();
      G.sound("block.stone.step", 0.7F);
      G.sound("ambient.cave", 1F);
    }
    return true;
  }

  /**
   * Zeichnet die Landschaft mit dem Eingang (und das Hineinzoomen). Liefert false, solange man noch draussen ist;
   * danach zeichnet der Bildschirm selbst das Innere ({@link #drawInside}) und am Ende {@link #veil}.
   */
  public boolean drawOutside(Stack s, float x, float y, float w, float h, float mx, float my, String sign) {
    if (this.inside()) {
      return true;
    }
    long now = System.currentTimeMillis();
    float zoom = this.enteredAt == 0 ? 0F : Math.min(1F, (now - this.enteredAt) / (float) ZOOM_MS);
    float ease = zoom * zoom * (3F - 2F * zoom);
    float[] e = entrance(x, y, w, h);
    float px = e[0] + e[2] / 2F, py = e[1] + e[3] * 0.6F, sc = 1F + 3.2F * ease;
    G.push(s);
    G.translate(s, px, py);
    G.scale(s, sc);
    G.translate(s, -px, -py);
    this.landscape(s, x, y, w, h, mx, my, sign, e, now);
    G.pop(s);
    if (ease > 0F) {
      G.rect(s, x, y, w, h, G.alpha(0xFF000000, ease));
    }
    return false;
  }

  /** Nah vor der Felswand: der ganze Bildschirm ist Stein, in der Mitte der grosse leuchtende Eingang. */
  private void landscape(Stack s, float x, float y, float w, float h, float mx, float my, String sign, float[] e, long now) {
    float t = tileSize(w, h), floor = y + h - t;
    int cols = (int) Math.ceil(w / t) + 2;
    float ox = x + w / 2F - cols * t / 2F;
    // Felswand aus den Bloecken des Spiels, von unten nach oben
    for (int r = 0; floor - r * t > y; r++) {
      for (int c = 0; c < cols; c++) {
        int hsh = hash(c + 3, r + 9);
        String name = hsh % 7 == 0 ? "andesite" : hsh % 5 == 0 ? "cobblestone" : "stone";
        tile(s, name, ox + c * t, floor - (r + 1) * t, t);
      }
    }
    // Boden davor: Stein, kein Weg
    for (int c = 0; c < cols; c++) {
      tile(s, hash(c, 77) % 3 == 0 ? "cobblestone" : "stone", ox + c * t, floor, t);
    }
    G.rect(s, x, floor, w, 4, 0x70000000); // Schatten am Fuss der Wand
    // die Wand wird nach aussen und oben dunkler: so wirkt sie tief statt flach
    for (int i = 0; i < 6; i++) {
      float d = i * w * 0.035F;
      G.rect(s, x, y, w * 0.21F - d, h - t, 0x1C000008);
      G.rect(s, x + w * 0.79F + d, y, w * 0.21F - d, h - t, 0x1C000008);
      G.rect(s, x, y, w, (h - t) * (0.30F - i * 0.045F), 0x1A000008);
    }

    // Eingang: ein ganz normales Hoehlenloch, unregelmaessig aus dem Fels gebrochen (kein Rahmen, kein Schild, keine Fackeln).
    boolean over = this.enteredAt == 0 && G.in(mx, my, e[0] - 6, e[1] - 6, e[2] + 12, e[3] + 6);
    float u = t / 3F;
    for (int pass = 0; pass < 2; pass++) { // erst ein weicher Schatten um das Loch, dann das Loch selbst
      for (int r = 0; r < HOLE.length; r++) {
        float hx = e[0] + HOLE[r][0] * u, hy = e[1] + r * u, hw = HOLE[r][1] * u;
        if (pass == 0) {
          G.rect(s, hx - u * 0.5F, hy - u * 0.5F, hw + u, u * 1.5F, 0x38000000);
        } else {
          G.rect(s, hx, hy, hw, u + 0.5F, 0xFF05040A);
        }
      }
    }
    if (over) { // das Loch leuchtet auf, wenn die Maus darueber ist
      float pulse = 0.7F + 0.3F * (float) Math.sin(now / 200.0);
      for (int r = 0; r < HOLE.length; r++) {
        G.rect(s, e[0] + HOLE[r][0] * u, e[1] + r * u, HOLE[r][1] * u, u + 0.5F, G.alpha(0xFFFFD27A, 0.16F * pulse));
      }
    }
    for (int i = 0; i < 10; i++) { // Gluehwuermchen vor der Wand
      float gx = x + ((i * 137 + now / (60F + i % 5 * 20)) % w), gy = floor - 20 - ((i * 53) % 110) + (float) Math.sin(now / 500.0 + i) * 6;
      G.rect(s, gx, gy, 2, 2, G.alpha(0xFFFFF7A0, 0.3F + 0.6F * (float) Math.abs(Math.sin(now / 400.0 + i * 2))));
    }
  }

  // ------------------------------------------------------------------ drinnen

  /** Das Innere der Grotte: dunkle Felswand, zwei Fackeln, Leuchtbeeren, Spinnweben, tropfende Tropfsteine. */
  public static void drawInside(Stack s, float x, float y, float w, float h) {
    long now = System.currentTimeMillis();
    G.rect(s, x, y, w, h, 0xFF07060C);
    float ceil = 36F, wallTop = y + ceil, floorY = y + h - FLOOR, t = 24F;
    int cols = (int) (w / t) + 2, rows = (int) ((floorY - wallTop) / t) + 2;
    float ox = x + (w - cols * t) / 2F, oy = floorY - rows * t;
    for (int r = 0; r < rows; r++) {
      for (int c = 0; c < cols; c++) {
        int hsh = hash(c + 11, r + 3);
        String name = hsh % 41 == 0 ? "deepslate_gold_ore" : hsh % 53 == 0 ? "deepslate_diamond_ore" : hsh % 8 == 0 ? "cobbled_deepslate" : hsh % 11 == 0 ? "tuff" : "deepslate";
        tile(s, name, ox + c * t, oy + r * t, t);
        if (hsh % 29 == 0) {
          tile(s, "glow_lichen", ox + c * t, oy + r * t, t);
        }
      }
    }
    G.rect(s, x, wallTop, w, floorY - wallTop, 0xA8050410); // es ist dunkel in der Grotte
    // zwei Fackeln, die die Wand in warmes Licht tauchen
    for (int k = 0; k < 2; k++) {
      float tx = x + w * (k == 0 ? 0.22F : 0.78F), ty = wallTop + (floorY - wallTop) * 0.52F;
      float flick = 0.78F + 0.22F * (float) Math.sin(now / 120.0 + k * 2.1) * (float) Math.sin(now / 310.0 + k);
      for (int i = 0; i < 7; i++) {
        float gw = 230 - i * 30;
        G.rect(s, tx - gw / 2F, ty - gw / 2F, gw, gw, G.alpha(0xFFFFA84A, 0.040F * flick));
      }
      tile(s, "torch", tx - 15, ty - 15, 30);
      for (int i = 0; i < 3; i++) { // Funken
        float e = ((now / 9F + i * 211 + k * 97) % 600) / 600F;
        G.rect(s, tx - 2 + (float) Math.sin(e * 6 + i) * 4, ty - 10 - e * 26, 2, 2, G.alpha(0xFFFFD27A, 1F - e));
      }
    }
    // Decke
    float[] ch = {22F, 14F};
    float cy = y;
    for (int r = 0; r < 2; r++) {
      float ts = ch[r] * 1.7F, left = x + (w - ((int) (w / ts) + 2) * ts) / 2F;
      for (int c = 0; c * ts < w + ts; c++) {
        G.icon(s, block(hash(c, r + 70) % 4 == 0 ? "cobbled_deepslate" : "deepslate"), left + c * ts, cy, ts + 0.5F, ch[r] + 0.5F);
      }
      G.rect(s, x, cy, w, ch[r], G.alpha(0xFF000000, 0.72F - r * 0.12F));
      cy += ch[r];
    }
    // Spinnweben in den Ecken
    tile(s, "cobweb", x + w * 0.24F, wallTop + 10, 28);
    tile(s, "cobweb", x + w * 0.76F - 28, wallTop + 10, 28);
    tile(s, "cobweb", x + w * 0.60F, wallTop - 2, 18);
    // Leuchtbeeren haengen von der Decke und leuchten
    float[] vines = {0.36F, 0.5F, 0.64F};
    for (int i = 0; i < vines.length; i++) {
      float vx = x + w * vines[i] - 9;
      int len = 2 + i % 2;
      for (int k = 0; k < len; k++) {
        tile(s, k == len - 1 ? "cave_vines_lit" : "cave_vines_plant_lit", vx, wallTop - 4 + k * 18, 18);
      }
      float glow = 0.6F + 0.4F * (float) Math.sin(now / 800.0 + i);
      G.rect(s, vx - 16, wallTop + len * 18 - 34, 50, 44, G.alpha(0xFFFFC24A, 0.07F * glow));
    }
    // Tropfsteine, von denen Wasser tropft
    float[] drips = {0.30F, 0.43F, 0.58F, 0.70F};
    for (int i = 0; i < drips.length; i++) {
      float dx = x + w * drips[i];
      boolean longOne = i % 2 == 0;
      if (longOne) {
        tile(s, "pointed_dripstone_down_frustum", dx - 9, wallTop - 4, 18);
      }
      float tip = wallTop - 4 + (longOne ? 18 : 0);
      tile(s, "pointed_dripstone_down_tip", dx - 9, tip, 18);
      float cycle = ((now + i * 733L) % 2100L) / 2100F;
      float from = tip + 16, dist = floorY - from;
      if (cycle < 0.55F) { // Tropfen faellt immer schneller
        float e = cycle / 0.55F;
        G.rect(s, dx - 1, from + dist * e * e, 2, 4, 0xFF6FB6FF);
      } else if (cycle < 0.75F) { // Spritzer
        float e = (cycle - 0.55F) / 0.2F;
        G.rect(s, dx - 3 - e * 5, floorY - 2 - e * 5 + e * e * 5, 2, 2, G.alpha(0xFF9BD0FF, 1F - e));
        G.rect(s, dx + 1 + e * 5, floorY - 2 - e * 5 + e * e * 5, 2, 2, G.alpha(0xFF9BD0FF, 1F - e));
      } else if (cycle > 0.9F) { // am Stein sammelt sich der naechste Tropfen
        G.rect(s, dx - 1, from - 2, 2, 2 + (cycle - 0.9F) * 20, 0xFF6FB6FF);
      }
      G.rect(s, dx - 8, floorY - 1, 16, 2, 0x705A9BE0); // kleine Pfuetze
    }
    G.rect(s, x, wallTop, w, 6, 0x70000000);
    // Boden
    float[] rh = {14F, 21F, 31F};
    float yy = floorY;
    for (int r = 0; r < 3; r++) {
      float ts = rh[r] * 1.7F, left = x + (w - ((int) (w / ts) + 2) * ts) / 2F;
      for (int c = 0; c * ts < w + ts; c++) {
        int hsh = hash(c, r + 50);
        G.icon(s, block(hsh % 3 == 0 ? "cobbled_deepslate" : "deepslate"), left + c * ts, yy, ts + 0.5F, rh[r] + 0.5F);
      }
      G.rect(s, x, yy, w, rh[r], G.alpha(0xFF000000, 0.46F - r * 0.12F));
      yy += rh[r];
    }
    G.rect(s, x, floorY - 3, w, 3, 0x80000000);
    for (int i = 0; i < 7; i++) { // Tropfsteine wachsen aus dem Boden
      float bx = x + w * (0.22F + 0.56F * ((hash(i, 57) % 1000) / 1000F)), size = 14 + hash(i, 23) % 12;
      tile(s, "pointed_dripstone_up_tip", bx - size / 2F, floorY - size + 2, size);
    }
    for (int i = 0; i < 4; i++) { // duenner Bodennebel
      G.rect(s, x, floorY - 10 + i * 6, w, 8, G.alpha(0xFF8C7CC8, 0.035F));
    }
    for (int i = 0; i < 5; i++) { // ein paar Amethystdrusen am Boden
      float bx = x + ((hash(i, 91) % 1000) / 1000F) * w, size = 16 + hash(i, 9) % 10;
      tile(s, i % 2 == 0 ? "amethyst_cluster" : "large_amethyst_bud", bx - size / 2F, floorY - size + 1, size);
    }
    // die Grotte ist klein: Fels woelbt sich von den Seiten und von oben herein und laesst nur eine Kammer frei
    float mt = 26F, kx = x + w / 2F, ky = y + h * 0.66F, krx = w * 0.40F, kry = h * 0.62F;
    for (float ty2 = y; ty2 < y + h; ty2 += mt) {
      for (float tx2 = x - ((mt - (w % mt)) / 2F); tx2 < x + w; tx2 += mt) {
        float dx = (tx2 + mt / 2F - kx) / krx, dy = (ty2 + mt / 2F - ky) / kry;
        int hsh = hash((int) (tx2 / mt) + 40, (int) (ty2 / mt) + 40);
        float d = dx * dx + dy * dy + (hsh % 100) / 600F;
        if (d > 1F && ty2 < y + h - FLOOR * 0.4F) {
          G.icon(s, block(hsh % 3 == 0 ? "cobbled_deepslate" : "deepslate"), tx2, ty2, mt + 0.5F, mt + 0.5F);
          G.rect(s, tx2, ty2, mt, mt, G.alpha(0xFF000000, Math.min(0.92F, 0.55F + (d - 1F) * 0.9F)));
        } else if (d > 0.84F) {
          G.rect(s, tx2, ty2, mt, mt, G.alpha(0xFF000000, (d - 0.84F) * 2.2F)); // Schatten am Rand der Kammer
        }
      }
    }
    // die Raender verschwinden im Dunkel
    for (int i = 0; i < 5; i++) {
      float band = w * 0.045F;
      G.rect(s, x + i * band, y, band, h, G.alpha(0xFF000000, 0.5F - i * 0.1F));
      G.rect(s, x + w - (i + 1) * band, y, band, h, G.alpha(0xFF000000, 0.5F - i * 0.1F));
    }
  }

  /** In die Wand eingelassene runde Nische mit Steinring (fuer das Gluecksrad). */
  public static void niche(Stack s, float cx, float cy, float r) {
    float rr = r + 10;
    for (float yy = -rr; yy < rr; yy += 2) {
      float half = (float) Math.sqrt(rr * rr - yy * yy);
      G.rect(s, cx - half, cy + yy, half * 2, 2, 0xD0030208);
    }
    int stones = 16;
    for (int i = 0; i < stones; i++) {
      double a = Math.PI * 2 * i / stones;
      float sx = cx + (float) Math.sin(a) * (rr + 7), sy = cy - (float) Math.cos(a) * (rr + 7);
      tile(s, i % 4 == 0 ? "chiseled_deepslate" : "polished_deepslate", sx - 9, sy - 9, 18);
    }
  }

  /** Eckige Nische mit Rahmen aus poliertem Tiefenschiefer (fuer einen Sticker). */
  public static void alcove(Stack s, float x, float y, float w, float h) {
    float t = 12F;
    for (float xx = -t; xx < w + t - 0.5F; xx += t) {
      tile(s, "polished_deepslate", x + xx, y - t, t);
      tile(s, "polished_deepslate", x + xx, y + h, t);
    }
    for (float yy = 0; yy < h - 0.5F; yy += t) {
      G.icon(s, block("polished_deepslate"), x - t, y + yy, t + 0.5F, Math.min(t, h - yy) + 0.5F);
      G.icon(s, block("polished_deepslate"), x + w, y + yy, t + 0.5F, Math.min(t, h - yy) + 0.5F);
    }
    tile(s, "chiseled_deepslate", x + w / 2F - t / 2F, y - t, t);
    G.rect(s, x, y, w, h, 0xE0030208);
    G.rect(s, x, y, w, 4, 0x60000000);
  }

  /**
   * Lichtstrahl in der Farbe {@code glow}, der aus einem leuchtenden Fleck auf dem Boden bis {@code top} aufsteigt.
   */
  public static void crackBeam(Stack s, float cx, float base, float top, int glow, float pulse, int seed) {
    long now = System.currentTimeMillis();
    float bw = 9F, len = base - top;
    // Strahl: viele schmale Streifen, zur Mitte hin heller, nach oben weich ausblendend (keine sichtbaren Kanten)
    int steps = 24;
    for (int i = 0; i < steps; i++) {
      float f = i / (float) steps, y0 = base - len * (i + 1) / steps, hh = len / steps + 0.5F, fade = (1F - f) * (1F - f);
      for (int k = 0; k < 5; k++) {
        float wide = bw * (1.6F - k * 0.3F) * (1F + f * 0.4F);
        G.rect(s, cx - wide, y0, wide * 2, hh, G.alpha(k == 4 ? 0xFFFFFFFF : glow, (0.05F + k * 0.035F) * fade * (0.85F + 0.15F * pulse)));
      }
    }
    for (int k = 0; k < 9; k++) { // feine Lichtpunkte steigen im Strahl auf
      float up = (now / 18F + k * 53 + seed * 13) % len, rx = cx + (float) Math.sin(k * 2.3 + now / 700.0) * bw * 0.6F;
      G.rect(s, rx - 1, base - up, 2, 2, G.alpha(0xFFFFFFFF, 0.6F * (1F - up / len)));
    }
    // runder, weicher Lichtfleck auf dem Boden: aus duennen Zeilen aufgebaut, zum Rand hin immer schwaecher
    float rx = 46F, ry = 11F;
    for (int layer = 0; layer < 5; layer++) {
      float sx = rx * (1F - layer * 0.19F), sy = ry * (1F - layer * 0.19F);
      for (float dy = -sy; dy < sy; dy += 1F) {
        float half = sx * (float) Math.sqrt(Math.max(0F, 1F - (dy * dy) / (sy * sy)));
        G.rect(s, cx - half, base + dy, half * 2, 1F, G.alpha(layer == 4 ? 0xFFFFFFFF : glow, 0.035F + layer * 0.012F + 0.01F * pulse));
      }
    }
  }

  /** Nach dem Hineinzoomen blendet das Dunkel langsam aus; am Ende des Bildschirms aufrufen. */
  public void veil(Stack s, float x, float y, float w, float h) {
    long since = System.currentTimeMillis() - this.enteredAt - ZOOM_MS;
    if (this.enteredAt > 0 && since >= 0 && since < FADE_MS) {
      G.rect(s, x, y, w, h, G.alpha(0xFF000000, 1F - since / (float) FADE_MS));
    }
  }
}
