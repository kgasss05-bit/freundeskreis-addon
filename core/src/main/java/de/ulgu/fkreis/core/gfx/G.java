package de.ulgu.fkreis.core.gfx;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.labymod.api.Laby;
import net.labymod.api.client.gui.icon.Icon;
import net.labymod.api.client.render.RenderPipeline;
import net.labymod.api.client.render.matrix.Stack;
import net.labymod.api.client.resources.ResourceLocation;

/**
 * Alle Zeichen-Aufrufe an EINER Stelle. Falls sich in einer LabyMod-Version etwas an der
 * Render-API aendert, muss nur diese Datei angepasst werden.
 */
public final class G {

  public static final UUID STEVE = UUID.fromString("8667ba71-b85a-4004-af54-457a9734eed7");

  public static final Icon PAPER = tex("ui/paper.png");
  public static final Icon ROD = tex("ui/rod.png");
  public static final Icon ROD_CAP = tex("ui/rod_cap.png");
  public static final Icon TRASH = tex("ui/trash.png");
  public static final Icon TRASH_OPEN = tex("ui/trash_open.png");

  private G() {
  }

  public static Icon tex(String path) {
    return Icon.texture(ResourceLocation.create("fkreis", "textures/" + path));
  }

  // ---------------------------------------------------------------- Ausschnitte aus Minecraft-Texturen

  private static final java.util.Map<String, Icon> SPRITES = new java.util.HashMap<>();

  /** Ausschnitt aus einer Textur (z. B. Gesicht aus einem Skin), wird pro Ausschnitt einmal erzeugt. */
  public static Icon sprite(ResourceLocation loc, int x, int y, int w, int h, int texW, int texH) {
    String key = loc + "#" + x + "," + y + "," + w + "," + h;
    Icon icon = SPRITES.get(key);
    if (icon == null) {
      icon = Icon.sprite(loc, x, y, w, h, texW, texH);
      SPRITES.put(key, icon);
    }
    return icon;
  }

  private static final int[][] STEVE_FACE = {
      {0x2F200D, 0x2B1E0D, 0x2F1F0F, 0x281C0B, 0x241808, 0x2B1E0D, 0x2F200D, 0x2B1E0D},
      {0x2B1E0D, 0x2B1E0D, 0x2F200D, 0x2B1E0D, 0x2F200D, 0x241808, 0x2B1E0D, 0x2F1F0F},
      {0x2F1F0F, 0xB6896C, 0xBD8E72, 0xC69680, 0xBD8B72, 0xBD8E74, 0xAC765A, 0x342512},
      {0xAA7D66, 0xB4846D, 0xAA7D66, 0xAD806D, 0x9C725C, 0xBB8972, 0x9C694C, 0x9C694C},
      {0xB4846D, 0xFFFFFF, 0x523D89, 0xB57B67, 0xBB8972, 0x523D89, 0xFFFFFF, 0xAA7D66},
      {0x9C6346, 0xB37B62, 0xB78272, 0x6A4030, 0x6A4030, 0xBE886C, 0xA26A47, 0x805334},
      {0x905E43, 0x965F40, 0x40221B, 0xBE886C, 0xBE886C, 0x40221B, 0x8F5E3E, 0x815539},
      {0x6F452C, 0x6D432A, 0x40221B, 0x40221B, 0x40221B, 0x40221B, 0x83553B, 0x7A4E33}};

  private static final ResourceLocation STEVE_SKIN = ResourceLocation.create("minecraft", "textures/entity/player/wide/steve.png");

  /** Offizieller Standard-Steve-Kopf (nicht der Skin des Accounts "Steve"). */
  public static void steveHead(Stack s, float x, float y, float size) {
    Icon face = sprite(STEVE_SKIN, 8, 8, 8, 8, 64, 64);
    if (face != null) {
      icon(s, face, x, y, size, size);
      return;
    }
    float p = size / 8F;
    for (int j = 0; j < 8; j++) {
      for (int i = 0; i < 8; i++) {
        rect(s, x + i * p, y + j * p, p + 0.05F, p + 0.05F, 0xFF000000 | STEVE_FACE[j][i]);
      }
    }
  }

  public static Icon head(UUID uuid) {
    return Icon.head(uuid);
  }

  private static RenderPipeline rp() {
    return Laby.labyAPI().renderPipeline();
  }

  // ---------------------------------------------------------------- Rechtecke

  public static void rect(Stack s, float x, float y, float w, float h, int argb) {
    if (w <= 0 || h <= 0 || (argb >>> 24) == 0) {
      return;
    }
    if (Cv.rect(x, y, w, h, argb)) {
      return;
    }
    rp().rectangleRenderer().pos(x, y, x + w, y + h).color(argb).render(s);
  }

  public static void outline(Stack s, float x, float y, float w, float h, float t, int argb) {
    rect(s, x, y, w, t, argb);
    rect(s, x, y + h - t, w, t, argb);
    rect(s, x, y + t, t, h - 2 * t, argb);
    rect(s, x + w - t, y + t, t, h - 2 * t, argb);
  }

  /** Gestrichelter Rahmen (leere Sammelbuch-Felder). */
  public static void dashed(Stack s, float x, float y, float w, float h, int argb) {
    for (float i = 0; i < w; i += 6) {
      rect(s, x + i, y, Math.min(3, w - i), 1, argb);
      rect(s, x + i, y + h - 1, Math.min(3, w - i), 1, argb);
    }
    for (float i = 0; i < h; i += 6) {
      rect(s, x, y + i, 1, Math.min(3, h - i), argb);
      rect(s, x + w - 1, y + i, 1, Math.min(3, h - i), argb);
    }
  }

  // ---------------------------------------------------------------- Text

  public static void text(Stack s, String t, float x, float y, int argb, float scale, boolean shadow) {
    if (t == null || t.isEmpty() || (argb >>> 24) == 0) {
      return;
    }
    s.push();
    s.translate(x, y, 0F);
    s.scale(scale, scale, 1F);
    Cv.text(t, 0, 0, argb, shadow);
    s.pop();
  }

  public static void text(Stack s, String t, float x, float y, int argb, float scale) {
    text(s, t, x, y, argb, scale, false);
  }

  public static void centered(Stack s, String t, float cx, float y, int argb, float scale, boolean shadow) {
    text(s, t, cx - width(t) * scale / 2F, y, argb, scale, shadow);
  }

  private static final java.util.Map<String, Float> WIDTHS = new java.util.HashMap<>();

  /** Textbreite in Minecraft-Pixeln (zwischengespeichert, damit nicht jeden Frame neu gemessen wird). */
  public static float width(String t) {
    if (t == null || t.isEmpty()) {
      return 0;
    }
    Float w = WIDTHS.get(t);
    if (w == null) {
      w = rp().componentRenderer().width(net.labymod.api.client.component.Component.text(t));
      if (WIDTHS.size() > 4000) {
        WIDTHS.clear();
      }
      WIDTHS.put(t, w);
    }
    return w;
  }

  /** Kuerzt einen Namen mit "…", damit er in die Breite passt (wie "Abschlepp…" im Regal). */
  public static String fit(String t, float maxWidth, float scale) {
    if (width(t) * scale <= maxWidth) {
      return t;
    }
    String cut = t;
    while (cut.length() > 1 && width(cut + "…") * scale > maxWidth) {
      cut = cut.substring(0, cut.length() - 1);
    }
    return cut + "…";
  }

  /** Bricht Text in Zeilen um (fuer Notizzettel). Respektiert Zeilenumbrueche. */
  public static List<String> wrap(String t, float maxWidth, float scale) {
    List<String> out = new ArrayList<>();
    for (String para : t.split("\n", -1)) {
      StringBuilder line = new StringBuilder();
      for (String word : para.split(" ", -1)) {
        String test = line.length() == 0 ? word : line + " " + word;
        if (width(test) * scale > maxWidth && line.length() > 0) {
          out.add(line.toString());
          line = new StringBuilder(word);
        } else {
          line = new StringBuilder(test);
        }
        while (width(line.toString()) * scale > maxWidth && line.length() > 1) {
          int cut = line.length() - 1;
          while (cut > 1 && width(line.substring(0, cut)) * scale > maxWidth) {
            cut--;
          }
          out.add(line.substring(0, cut));
          line = new StringBuilder(line.substring(cut));
        }
      }
      out.add(line.toString());
    }
    return out;
  }

  // ---------------------------------------------------------------- Icons

  public static void icon(Stack s, Icon icon, float x, float y, float w, float h) {
    if (icon != null && !Cv.icon(icon, x, y, w, h)) {
      icon.render(s, x, y, w, h);
    }
  }

  // ---------------------------------------------------------------- Transform

  public static void push(Stack s) {
    s.push();
  }

  public static void pop(Stack s) {
    s.pop();
  }

  public static void translate(Stack s, float x, float y) {
    s.translate(x, y, 0F);
  }

  public static void scale(Stack s, float f) {
    s.scale(f, f, 1F);
  }

  /** Dreht um den Punkt (cx, cy), Winkel in Grad. */
  public static void rotate(Stack s, float deg, float cx, float cy) {
    s.translate(cx, cy, 0F);
    s.rotate(deg, 0F, 0F, 1F);
    s.translate(-cx, -cy, 0F);
  }

  // ---------------------------------------------------------------- Farben & Animation

  public static int shade(int argb, float f) {
    int a = argb >>> 24;
    int r = clamp((int) (((argb >> 16) & 255) * f));
    int g = clamp((int) (((argb >> 8) & 255) * f));
    int b = clamp((int) ((argb & 255) * f));
    return (a << 24) | (r << 16) | (g << 8) | b;
  }

  public static int alpha(int argb, float a) {
    int al = clamp((int) ((argb >>> 24) * a));
    return (al << 24) | (argb & 0xFFFFFF);
  }

  public static int mix(int a, int b, float t) {
    int ar = (a >> 16) & 255, ag = (a >> 8) & 255, ab = a & 255, aa = a >>> 24;
    int br = (b >> 16) & 255, bg = (b >> 8) & 255, bb = b & 255, ba = b >>> 24;
    return (clamp((int) (aa + (ba - aa) * t)) << 24) | (clamp((int) (ar + (br - ar) * t)) << 16)
        | (clamp((int) (ag + (bg - ag) * t)) << 8) | clamp((int) (ab + (bb - ab) * t));
  }

  private static int clamp(int v) {
    return Math.max(0, Math.min(255, v));
  }

  public static float easeOut(float t) {
    t = Math.max(0F, Math.min(1F, t));
    return 1F - (1F - t) * (1F - t) * (1F - t);
  }

  public static float easeBack(float t) {
    t = Math.max(0F, Math.min(1F, t));
    float c1 = 1.70158F, c3 = c1 + 1F;
    return 1F + c3 * (float) Math.pow(t - 1, 3) + c1 * (float) Math.pow(t - 1, 2);
  }

  public static float lerp(float a, float b, float t) {
    return a + (b - a) * t;
  }

  public static boolean in(float mx, float my, float x, float y, float w, float h) {
    return mx >= x && mx < x + w && my >= y && my < y + h;
  }

  // ---------------------------------------------------------------- Sounds

  public static void click() {
    try {
      Laby.labyAPI().minecraft().sounds().playButtonPress();
    } catch (Throwable ignored) {
    }
  }

  public static void sound(String id, float pitch) {
    try {
      Laby.labyAPI().minecraft().sounds().playSound(ResourceLocation.create("minecraft", id), 0.8F, pitch);
    } catch (Throwable ignored) {
    }
  }

  // ---------------------------------------------------------------- Holz (Bretter)

  /** Zeichnet Minecraft-artige Holzbretter: Bretter-Fugen + Maserung (deterministisch, flackert nicht). */
  public static void planks(Stack s, float x, float y, float w, float h, int plank, int seed) {
    rect(s, x, y, w, h, plank);
    int seam = shade(plank, 0.72F);
    int grainDark = shade(plank, 0.86F);
    int grainLight = shade(plank, 1.10F);
    float ph = 16;
    int row = 0;
    for (float yy = y; yy < y + h; yy += ph, row++) {
      float rh = Math.min(ph, y + h - yy);
      rect(s, x, yy + rh - 1, w, 1, seam);
      // versetzte senkrechte Fugen wie bei Minecraft-Brettern
      float off = (row % 2 == 0) ? 0 : 24;
      for (float xx = x + off; xx < x + w; xx += 48) {
        rect(s, xx, yy, 1, rh - 1, seam);
      }
      // Maserung
      int r = seed * 31 + row * 17;
      for (float xx = x + 3; xx < x + w - 6; xx += 9) {
        r = r * 1103515245 + 12345;
        int k = (r >>> 16) & 7;
        float gy = yy + 2 + (k % 4) * 3;
        if (gy < yy + rh - 2) {
          rect(s, xx, gy, 4 + (k & 3), 1, (k & 1) == 0 ? grainDark : grainLight);
        }
      }
    }
  }

  /** Buchruecken wie im Regal: farbiger Ruecken, grauer Tab links, cremefarbenes Etikett, Seiten rechts. */
  public static void spine(Stack s, float x, float y, float w, float h, int color, boolean label) {
    rect(s, x, y, w, h, color);
    rect(s, x, y, w, 1, shade(color, 1.18F));
    rect(s, x, y + h - 1, w, 1, shade(color, 0.65F));
    rect(s, x + w - 3, y + 1, 2, h - 2, 0xFFF2EEE0);
    rect(s, x + w - 1, y, 1, h, shade(color, 0.6F));
    float tab = Math.max(2, h * 0.18F);
    rect(s, x + 3, y + 3, tab, h - 6, 0xFF2A2A2A);
    rect(s, x + 3.5F, y + 3.5F, tab - 1, h - 7, 0xFFB4B4B4);
    if (label) {
      rect(s, x + 4 + tab + 2, y + 2, w - (4 + tab + 2) - 6, h - 4, 0xFFEDE4C8);
    }
  }

  public static final int[] BOOK_COLORS = {
      0xFF3C7A38, 0xFF2E4E8E, 0xFF9E2428, 0xFF2A7878, 0xFF5C3A80, 0xFF8A5A2B, 0xFF3E434E, 0xFFB8902E
  };

  public static int bookColor(String key) {
    return BOOK_COLORS[Math.floorMod(key.hashCode() * 7 + 3, BOOK_COLORS.length)];
  }
}
