package de.ulgu.fkreis.core.gfx;

import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;
import net.labymod.api.client.gui.icon.Icon;

/** Kleiner Pixel-Maler: zeichnet Rechtecke und 5x7-Schrift in ein Bild und macht daraus eine Textur. */
public final class Px {

  private static final Map<Character, String> GL = new HashMap<>();

  static {
    String[] g = {"A01110100011000111111100011000110001", "B11110100011000111110100011000111110", "C01110100011000010000100001000101110",
        "D11110100011000110001100011000111110", "E11111100001000011110100001000011111", "F11111100001000011110100001000010000",
        "G01110100011000010111100011000101111", "H10001100011000111111100011000110001", "I01110001000010000100001000010001110",
        "J00111000100001000010000101001001100", "K10001100101010011000101001001010001", "L10000100001000010000100001000011111",
        "M10001110111010110101100011000110001", "N10001110011010110011100011000110001", "O01110100011000110001100011000101110",
        "P11110100011000111110100001000010000", "Q01110100011000110001101011001001101", "R11110100011000111110101001001010001",
        "S01111100001000001110000010000111110", "T11111001000010000100001000010000100", "U10001100011000110001100011000101110",
        "V10001100011000110001100010101000100", "W10001100011000110101101011101110001", "X10001100010101000100010101000110001",
        "Y10001100010101000100001000010000100", "Z11111000010001000100010001000011111", "001110100011001110101110011000101110",
        "100100011000010000100001000010001110", "201110100010000100010001000100011111", "311110000010000101110000010000111110",
        "400010001100101010010111110001000010", "511111100001111000001000011000101110", "600110010001000011110100011000101110",
        "711111000010001000100010000100001000", "801110100011000101110100011000101110", "901110100011000101111000010001001100",
        "-00000000000000001110000000000000000", ".00000000000000000000000000110001100", "!00100001000010000100001000000000100"};
    for (String s : g) {
      GL.put(s.charAt(0), s.substring(1));
    }
  }

  public final BufferedImage img;

  public Px(int w, int h) {
    this.img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
  }

  /** Deckendes Rechteck in RGB. */
  public void r(float x, float y, float w, float h, int rgb) {
    this.a(x, y, w, h, 0xFF000000 | rgb);
  }

  /** Rechteck in ARGB (ersetzt, mischt nicht). */
  public void a(float x, float y, float w, float h, int argb) {
    int x0 = Math.max(0, Math.round(x)), y0 = Math.max(0, Math.round(y));
    int x1 = Math.min(this.img.getWidth(), Math.round(x) + Math.round(w)), y1 = Math.min(this.img.getHeight(), Math.round(y) + Math.round(h));
    for (int yy = y0; yy < y1; yy++) {
      for (int xx = x0; xx < x1; xx++) {
        this.img.setRGB(xx, yy, argb);
      }
    }
  }

  private static String norm(String s) {
    return s.toUpperCase().replace('Ä', 'A').replace('Ö', 'O').replace('Ü', 'U').replace('ß', 'S');
  }

  public static int textW(String s) {
    int w = 0;
    String n = norm(s);
    for (int i = 0; i < n.length(); i++) {
      w += GL.containsKey(n.charAt(i)) ? 6 : 3;
    }
    return w;
  }

  /** 5x7-Pixelschrift; maxW &lt;= 0 heisst ohne Begrenzung. */
  public void text(String s, int x, int y, int rgb, int maxW) {
    String n = norm(s);
    int cx = x;
    for (int i = 0; i < n.length(); i++) {
      String g = GL.get(n.charAt(i));
      int w = g != null ? 6 : 3;
      if (maxW > 0 && cx + w - x > maxW) {
        break;
      }
      if (g != null) {
        for (int k = 0; k < 35; k++) {
          if (g.charAt(k) == '1') {
            this.r(cx + k % 5, y + k / 5, 1, 1, rgb);
          }
        }
      }
      cx += w;
    }
  }

  /** Fertiges Bild als Textur, {@code up}-fach vergroessert (bleibt pixelig scharf). Im Render-Thread aufrufen. */
  public Icon icon(int up) {
    int w = this.img.getWidth(), h = this.img.getHeight();
    BufferedImage big = new BufferedImage(w * up, h * up, BufferedImage.TYPE_INT_ARGB);
    for (int y = 0; y < h * up; y++) {
      for (int x = 0; x < w * up; x++) {
        big.setRGB(x, y, this.img.getRGB(x / up, y / up));
      }
    }
    return PhotoTex.uploadImage(big);
  }

  public static int shade(int rgb, float f) {
    int r = Math.max(0, Math.min(255, Math.round((rgb >> 16 & 255) * f)));
    int g = Math.max(0, Math.min(255, Math.round((rgb >> 8 & 255) * f)));
    int b = Math.max(0, Math.min(255, Math.round((rgb & 255) * f)));
    return r << 16 | g << 8 | b;
  }

  public static int mix(int a, int b, float t) {
    int r = Math.round((a >> 16 & 255) * (1 - t) + (b >> 16 & 255) * t);
    int g = Math.round((a >> 8 & 255) * (1 - t) + (b >> 8 & 255) * t);
    int bl = Math.round((a & 255) * (1 - t) + (b & 255) * t);
    return r << 16 | g << 8 | bl;
  }
}
