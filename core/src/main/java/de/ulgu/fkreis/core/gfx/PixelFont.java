package de.ulgu.fkreis.core.gfx;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.labymod.api.client.gui.icon.Icon;
import net.labymod.api.client.render.matrix.Stack;

/**
 * Pixel-Schrift im Minecraft-Stil fuer die Notizzettel: "Monocraft" von Idrees Hassan (SIL Open Font License 1.1,
 * siehe textures/font/MONOCRAFT-LICENSE.txt). Alle Glyphen (je 6 x 9 Pixel, Tintenfarbe eingebacken)
 * liegen in textures/font/glyphs.png.
 */
public final class PixelFont {

  private static final int[] CODES = {33, 34, 35, 36, 37, 38, 39, 40, 41, 42, 43, 44, 45, 46, 47, 48, 49, 50, 51, 52, 53, 54, 55, 56, 57, 58, 59, 60, 61, 62, 63, 64, 65, 66, 67, 68, 69, 70, 71, 72, 73, 74, 75, 76, 77, 78, 79, 80, 81, 82, 83, 84, 85, 86, 87, 88, 89, 90, 91, 92, 93, 94, 95, 96, 97, 98, 99, 100, 101, 102, 103, 104, 105, 106, 107, 108, 109, 110, 111, 112, 113, 114, 115, 116, 117, 118, 119, 120, 121, 122, 123, 124, 125, 126, 176, 196, 214, 220, 223, 228, 246, 252, 8364};
  private static final int[] W = {2, 4, 6, 6, 6, 6, 2, 4, 4, 4, 6, 3, 6, 2, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 2, 3, 5, 6, 5, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 4, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 4, 6, 4, 6, 6, 3, 6, 6, 6, 6, 6, 5, 6, 6, 5, 6, 5, 5, 6, 6, 6, 6, 6, 6, 6, 5, 6, 6, 6, 6, 6, 6, 5, 2, 5, 6, 5, 6, 6, 6, 6, 6, 6, 6, 6};
  private static final int[] OFF = {2, 1, 0, 0, 0, 0, 2, 1, 1, 1, 0, 1, 0, 2, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 2, 1, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 1, 0, 0, 1, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 2, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
  private static final Map<Integer, Integer> INDEX = new HashMap<>();
  private static final Icon[] ICONS = new Icon[CODES.length];

  /** Zeilenhoehe bei Pixelgroesse 1. */
  public static final float LINE = 11F;
  private static final float SPACE = 4F;

  static {
    for (int i = 0; i < CODES.length; i++) {
      INDEX.put(CODES[i], i);
    }
  }

  private PixelFont() {
  }

  private static int index(char c) {
    Integer i = INDEX.get((int) c);
    if (i == null) {
      i = INDEX.get((int) '?');
    }
    return i;
  }

  private static final net.labymod.api.client.resources.ResourceLocation ATLAS =
      net.labymod.api.client.resources.ResourceLocation.create("fkreis", "textures/font/glyphs.png");

  /** Alle Glyphen liegen in einem Bild (16 pro Reihe, je 6 x 9 Pixel). */
  private static Icon icon(int i) {
    if (ICONS[i] == null) {
      ICONS[i] = G.sprite(ATLAS, (i % 16) * 6, (i / 16) * 9, 6, 9, 96, 64);
    }
    return ICONS[i];
  }

  public static float width(String t, float px) {
    float w = 0;
    for (int k = 0; k < t.length(); k++) {
      char c = t.charAt(k);
      w += (c == ' ' ? SPACE : W[index(c)]) * px;
    }
    return w;
  }

  /** Zeichnet eine Zeile (oben links = x/y). */
  public static void draw(Stack s, String t, float x, float y, float px) {
    float cx = x;
    for (int k = 0; k < t.length(); k++) {
      char c = t.charAt(k);
      if (c == ' ') {
        cx += SPACE * px;
        continue;
      }
      int i = index(c);
      G.icon(s, icon(i), cx - OFF[i] * px, y, 6 * px, 9 * px);
      cx += W[i] * px;
    }
  }

  /** Zeilenumbruch nach Breite (ganze Woerter; zu lange Woerter werden getrennt; \n = neue Zeile). */
  public static List<String> wrap(String text, float maxW, float px) {
    List<String> lines = new ArrayList<>();
    for (String para : text.split("\n", -1)) {
      StringBuilder line = new StringBuilder();
      for (String word : para.split(" ", -1)) {
        String cand = line.length() == 0 ? word : line + " " + word;
        if (width(cand, px) <= maxW) {
          line.setLength(0);
          line.append(cand);
          continue;
        }
        if (line.length() > 0) {
          lines.add(line.toString());
          line.setLength(0);
        }
        StringBuilder w = new StringBuilder();
        for (char c : word.toCharArray()) {
          if (width(w.toString() + c, px) > maxW && w.length() > 0) {
            lines.add(w.toString());
            w.setLength(0);
          }
          w.append(c);
        }
        line.append(w);
      }
      lines.add(line.toString());
    }
    return lines;
  }
}
