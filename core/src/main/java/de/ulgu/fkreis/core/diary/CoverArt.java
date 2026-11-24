package de.ulgu.fkreis.core.diary;

import de.ulgu.fkreis.core.gfx.PhotoTex;
import java.awt.image.BufferedImage;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import net.labymod.api.client.gui.icon.Icon;

/**
 * Zeichnet den Ledereinband des Tagebuchs als Pixelgrafik (112 x 144) und laedt ihn als Textur hoch.
 * Leder mit Narbung, Buchruecken, Seitenkante, eingravierter Name; optional eine Naht aus Gold- oder Silberfaden.
 */
public final class CoverArt {

  private static final int W = 112, H = 144, UP = 3;
  private static final Map<Character, String> GL = new HashMap<>();
  private static final Map<String, Icon> CACHE = new LinkedHashMap<String, Icon>(32, 0.75F, true) {
    @Override
    protected boolean removeEldestEntry(Map.Entry<String, Icon> eldest) {
      return this.size() > 40;
    }
  };

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
        "_00000000000000000000000000000011111"};
    for (String s : g) {
      GL.put(s.charAt(0), s.substring(1));
    }
  }

  private CoverArt() {
  }

  /** Einband als Textur (zwischengespeichert). Im Render-Thread aufrufen. */
  public static Icon icon(int leather, String trim, String name) {
    int year = LocalDate.now().getYear();
    String key = leather + "|" + trim + "|" + name + "|" + year;
    Icon icon = CACHE.get(key);
    if (icon == null) {
      BufferedImage small = render(leather, trim, name, year);
      BufferedImage big = new BufferedImage(W * UP, H * UP, BufferedImage.TYPE_INT_ARGB);
      for (int y = 0; y < H * UP; y++) {
        for (int x = 0; x < W * UP; x++) {
          big.setRGB(x, y, small.getRGB(x / UP, y / UP));
        }
      }
      icon = PhotoTex.uploadImage(big);
      CACHE.put(key, icon);
    }
    return icon;
  }

  private static float hash(int a, int b) {
    int h = a * 374761393 + b * 668265263;
    h = (h ^ (h >> 13)) * 1274126177;
    return ((h ^ (h >> 16)) & 0x7FFFFFFF) / (float) 0x7FFFFFFF;
  }

  private static int mul(int rgb, float f) {
    int r = Math.max(0, Math.min(255, (int) ((rgb >> 16 & 255) * f)));
    int g = Math.max(0, Math.min(255, (int) ((rgb >> 8 & 255) * f)));
    int b = Math.max(0, Math.min(255, (int) ((rgb & 255) * f)));
    return r << 16 | g << 8 | b;
  }

  private static final class Canvas {
    final BufferedImage img = new BufferedImage(W, H, BufferedImage.TYPE_INT_ARGB);

    void p(int x, int y, int rgb) {
      if (x >= 0 && y >= 0 && x < W && y < H) {
        this.img.setRGB(x, y, 0xFF000000 | rgb);
      }
    }

    void b(int x, int y, int w, int h, int rgb) {
      for (int j = 0; j < h; j++) {
        for (int i = 0; i < w; i++) {
          this.p(x + i, y + j, rgb);
        }
      }
    }

    /** Eingravierter Text: dunkle Vertiefung mit heller Kante rechts unten. */
    void text(String s, int cx, int y, int sc, int dark, int light) {
      s = s.toUpperCase();
      int w = s.length() * 6 * sc - sc;
      for (int pass = 0; pass < 2; pass++) {
        int x = Math.round(cx - w / 2F);
        for (int n = 0; n < s.length(); n++) {
          String m = GL.get(s.charAt(n));
          if (m != null) {
            for (int i = 0; i < 35; i++) {
              if (m.charAt(i) == '1') {
                int px = x + (i % 5) * sc, py = y + (i / 5) * sc;
                if (pass == 0) {
                  this.b(px + 1, py + 1, sc, sc, light);
                } else {
                  this.b(px, py, sc, sc, dark);
                }
              }
            }
          }
          x += 6 * sc;
        }
      }
    }
  }

  static BufferedImage render(int leather, String trim, String name, int year) {
    Canvas c = new Canvas();
    for (int y = 0; y < H; y++) {
      for (int x = 0; x < W; x++) {
        float f = 0.92F + hash(x, y) * 0.15F;
        int d = Math.min(Math.min(x - 10, W - 4 - x), Math.min(y, H - 4 - y));
        if (d < 7) {
          f *= 0.84F + Math.max(0, d) * 0.023F;
        }
        if (hash(x * 3 + 1, y * 7) > 0.987F) {
          f *= 0.78F;
        }
        if (hash(x * 5, y * 2 + 3) > 0.992F) {
          f *= 1.16F;
        }
        if (x < 10) {
          f *= 0.74F + (x / 10F) * 0.12F;
          if (y % 26 < 2) {
            f *= 1.3F;
          }
          if (y % 26 == 2) {
            f *= 0.7F;
          }
        }
        c.p(x, y, mul(leather, f));
      }
    }
    c.b(W - 3, 2, 2, H - 4, 0xEFE2BD);
    c.b(10, H - 3, W - 12, 2, 0xE0D0A2);
    for (int y = 3; y < H - 3; y += 2) {
      c.p(W - 2, y, 0xCDB880);
    }
    c.b(0, 0, W, 1, 0x1A0F08);
    c.b(0, H - 1, W, 1, 0x1A0F08);
    c.b(0, 0, 1, H, 0x1A0F08);
    c.b(W - 1, 0, 1, H, 0x1A0F08);
    c.b(10, 1, 1, H - 2, mul(leather, 0.5F));

    int x0 = 16, x1 = W - 9, y0 = 6, y1 = H - 9;
    boolean metal = "gold".equals(trim) || "silver".equals(trim);
    if (!metal) {
      int st = mul(leather, 1.45F);
      for (int x = x0; x <= x1; x++) {
        if (x % 4 < 2) {
          c.p(x, y0, st);
          c.p(x, y1, st);
        }
      }
      for (int y = y0; y <= y1; y++) {
        if (y % 4 < 2) {
          c.p(x0, y, st);
          c.p(x1, y, st);
        }
      }
    } else {
      seam(c, leather, "gold".equals(trim) ? new int[] {0xF8E08A, 0xD9AA2E, 0xA07414} : new int[] {0xFBFCFE, 0xC2C9D4, 0x858E9C});
    }

    int cx = Math.round((x0 + x1) / 2F);
    int dark = mul(leather, 0.42F), light = mul(leather, 1.42F);
    String nm = name == null || name.isEmpty() ? "TAGEBUCH" : name.length() > 16 ? name.substring(0, 16) : name;
    String[] lines = {nm};
    int sc = nm.length() <= 5 ? 2 : 1;
    if (nm.length() > 10) {
      int cut = -1;
      float best = 99;
      for (int i = 1; i < nm.length() - 1; i++) {
        if (nm.charAt(i) == '_' && Math.abs(i + 1 - nm.length() / 2F) < best) {
          best = Math.abs(i + 1 - nm.length() / 2F);
          cut = i + 1;
        }
      }
      if (cut < 0 || cut > 10 || nm.length() - cut > 10) {
        cut = (nm.length() + 1) / 2;
      }
      lines = new String[] {nm.substring(0, cut), nm.substring(cut)};
    }
    c.text("TAGEBUCH", cx, 42, 1, dark, light);
    int ny = sc == 2 || lines.length == 2 ? 58 : 62;
    for (String l : lines) {
      c.text(l, cx, ny, sc, dark, light);
      ny += 7 * sc + 3;
    }
    int ly = ny + 3;
    c.b(cx - 18, ly, 37, 1, dark);
    c.b(cx - 18, ly + 1, 37, 1, light);
    c.text(String.valueOf(year), cx, y1 - 30, 1, dark, light);
    return c.img;
  }

  /** Schlichte Steppnaht aus Metallfaden mit eingezogenen Ecken und je einem kleinen Knoten. */
  private static void seam(Canvas c, int leather, int[] th) {
    int[] m = new int[W * H];
    int l = 18, r = 101, t = 8, b = 133, r0 = 7;
    for (int x = l + r0; x <= r - r0; x++) {
      if ((x - l) % 5 != 0) {
        set(m, x, t, 1);
        set(m, x, t + 1, 2);
        set(m, x, b - 1, 1);
        set(m, x, b, 2);
      }
    }
    for (int y = t + r0; y <= b - r0; y++) {
      if ((y - t) % 5 != 0) {
        set(m, l, y, 1);
        set(m, l + 1, y, 2);
        set(m, r - 1, y, 1);
        set(m, r, y, 2);
      }
    }
    int[][] corners = {{l, t, 1, 1}, {r, t, -1, 1}, {l, b, 1, -1}, {r, b, -1, -1}};
    for (int[] k : corners) {
      for (int p = 0; p <= r0 + 1; p++) {
        for (int q = 0; q <= r0 + 1; q++) {
          double d = Math.hypot(p, q);
          if (d >= r0 - 0.9 && d <= r0 + 0.9) {
            set(m, k[0] + k[2] * p, k[1] + k[3] * q, d < r0 ? 1 : 2);
          }
        }
      }
      for (int p = -1; p <= 1; p++) {
        for (int q = -1; q <= 1; q++) {
          if (Math.abs(p) + Math.abs(q) <= 1) {
            set(m, Math.round(k[0] + k[2] * 1.5F) + p, Math.round(k[1] + k[3] * 1.5F) + q, p == 0 && q == 0 ? 1 : 2);
          }
        }
      }
    }
    for (int y = 1; y < H - 1; y++) {
      for (int x = 1; x < W - 1; x++) {
        if (m[y * W + x] == 0 && (m[(y - 1) * W + x - 1] != 0 || m[(y - 1) * W + x] != 0 || m[y * W + x - 1] != 0)) {
          c.p(x, y, mul(leather, 0.6F));
        }
      }
    }
    for (int y = 0; y < H; y++) {
      for (int x = 0; x < W; x++) {
        int v = m[y * W + x];
        if (v != 0) {
          c.p(x, y, th[v == 1 ? 0 : (x + y) % 3 == 0 ? 2 : 1]);
        }
      }
    }
  }

  private static void set(int[] m, int x, int y, int v) {
    if (x > 11 && x < W - 4 && y > 1 && y < H - 4) {
      m[y * W + x] = v;
    }
  }
}
