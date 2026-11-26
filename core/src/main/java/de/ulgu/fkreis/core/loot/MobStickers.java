package de.ulgu.fkreis.core.loot;

import de.ulgu.fkreis.core.data.Mobs;
import de.ulgu.fkreis.core.gfx.G;
import de.ulgu.fkreis.core.ui.MobFaces;
import java.util.ArrayList;
import java.util.List;
import net.labymod.api.client.gui.icon.Icon;
import net.labymod.api.client.render.matrix.Stack;

/**
 * Mob-Sticker, die man mit Holy-Coins kauft. Das Gesicht kommt zur Laufzeit aus den Texturen des Spiels;
 * der Sticker selbst hat einen eigenen farbigen Glanzrahmen mit Muenzstempel, damit er nie mit einem
 * gewoehnlichen Spielerkopf-Sticker verwechselt wird (auch nicht, wenn jemand einen Mob-Skin traegt).
 */
public final class MobStickers {

  /** Preisstufen und ihre Rahmenfarben: einfach, besser, extrem gut, extrem selten. */
  public static final int[] PRICES = {25, 50, 75, 100};
  private static final int[] RIM = {0xFF6FA36B, 0xFF4A8FD8, 0xFFA05BD0, 0xFFF2C94C};
  private static final int[] BACK = {0xFFDDEFD9, 0xFFD6E8FB, 0xFFEADCF8, 0xFFFFF1BF};

  /** Besser, extrem gut, extrem selten; alle uebrigen Mobs sind "einfach". */
  private static final String[] BETTER = {"creeper", "enderman", "blaze", "piglin", "pillager", "witch", "fox", "wolf", "cat", "bee", "frog",
      "turtle", "dolphin", "parrot", "goat", "llama", "camel", "armadillo", "guardian", "phantom", "slime", "magma_cube", "hoglin",
      "strider", "vex", "ocelot", "polar_bear", "snow_golem", "zombified_piglin", "bogged", "parched", "nautilus"};
  private static final String[] GREAT = {"wither_skeleton", "vindicator", "evoker", "iron_golem", "wandering_trader", "ravager", "shulker",
      "ghast", "happy_ghast", "allay", "axolotl", "sniffer", "creaking", "breeze", "piglin_brute", "mooshroom", "panda", "copper_golem",
      "zoglin", "skeleton_horse", "zombie_horse", "zombie_nautilus", "camel_husk"};
  private static final String[] RARE = {"villager", "warden", "wither", "ender_dragon", "elder_guardian", "illusioner"};

  private static final String[][] TIERS = build();

  /** Alle Sticker hintereinander (fuer die Pruefseite). */
  public static List<String> all() {
    List<String> out = new ArrayList<>();
    for (String[] tier : TIERS) {
      for (String id : tier) {
        out.add(id);
      }
    }
    return out;
  }

  /** Alle Mobs des Addons auf die vier Stufen verteilen; der Illusionist kommt dazu (er hat im Spiel kein Spawn-Ei). */
  private static String[][] build() {
    List<String> rest = new ArrayList<>();
    for (de.ulgu.fkreis.core.data.Mob mob : Mobs.all()) {
      String id = mob.id();
      if (!id.equals("none") && !in(BETTER, id) && !in(GREAT, id) && !in(RARE, id)) {
        rest.add(id);
      }
    }
    return new String[][] {rest.toArray(new String[0]), BETTER, GREAT, RARE};
  }

  private static boolean in(String[] list, String id) {
    for (String s : list) {
      if (s.equals(id)) {
        return true;
      }
    }
    return false;
  }

  public static final String PREFIX = "mob:";
  /** Holo-Sticker gibt es nicht zu kaufen, nur mit sehr viel Glueck aus der Truhe. In Listen haengt ein * an der Mob-ID. */
  public static final String HOLO = "holo:";

  private MobStickers() {
  }

  public static String[] tier(int t) {
    return TIERS[t];
  }

  /**
   * Das Tagesangebot: vier zufaellige Sticker, fuer alle Spieler am selben Tag dieselben. Jeder Mob hat dieselbe Chance,
   * egal wie selten er ist. Bestimmt wird nur ueber das Datum, es wird nichts gespeichert oder abgefragt.
   */
  public static String[] daily() {
    List<String> all = new ArrayList<>();
    for (String[] tier : TIERS) {
      for (String id : tier) {
        all.add(id);
      }
    }
    java.util.Collections.sort(all);
    java.util.Random rnd = new java.util.Random(java.time.LocalDate.now().toEpochDay() * 0x9E3779B97F4A7C15L + 0x5DEECE66DL);
    String[] out = new String[Math.min(4, all.size())];
    for (int i = 0; i < out.length; i++) {
      out[i] = all.remove(rnd.nextInt(all.size()));
    }
    return out;
  }

  public static boolean holo(String key) {
    return key.endsWith("*");
  }

  private static String id(String key) {
    return holo(key) ? key.substring(0, key.length() - 1) : key;
  }

  /** Zufaelliger Mob fuer einen Holo-Sticker. */
  public static String random(java.util.Random rnd) {
    String[] tier = TIERS[rnd.nextInt(TIERS.length)];
    return tier[rnd.nextInt(tier.length)];
  }

  public static int tierOf(String key) {
    String mob = id(key);
    for (int t = 0; t < TIERS.length; t++) {
      for (String id : TIERS[t]) {
        if (id.equals(mob)) {
          return t;
        }
      }
    }
    return -1;
  }

  public static int price(String mob) {
    int t = tierOf(mob);
    return t < 0 ? 0 : PRICES[t];
  }

  /** Alle eigenen Mob-Sticker: erst die Holo-Sticker (mit *), dann die gekauften, teuerste zuerst. */
  public static List<String> owned() {
    List<String> out = new ArrayList<>();
    for (int t = TIERS.length - 1; t >= 0; t--) {
      for (String id : TIERS[t]) {
        if (Loot.get().owned.contains(HOLO + id)) {
          out.add(id + "*");
        }
      }
    }
    for (int t = TIERS.length - 1; t >= 0; t--) {
      for (String id : TIERS[t]) {
        if (Loot.get().owned.contains(PREFIX + id)) {
          out.add(id);
        }
      }
    }
    return out;
  }

  public static String name(String key) {
    if (id(key).equals("illusioner")) {
      return de.ulgu.fkreis.core.L.german() ? "Illusionist" : "Illusioner";
    }
    return Mobs.byId(id(key)).name();
  }

  /** Farben der Glitzerpunkte auf Holo-Stickern. */
  private static final int[] GLITTER = {0xFFFFFFFF, 0xFFFFF2B0, 0xFFFFD6EC, 0xFFCFE8FF, 0xFFE2D4FF};

  /**
   * Zeichnet den Sticker in das Feld (x, y, w, h): weisser Stanzrand mit abgeschnittenen Ecken, farbige Glanzflaeche,
   * Mob-Gesicht, Muenzstempel unten rechts, Name darunter. {@code locked} = noch nicht gekauft (abgedunkelt).
   */
  public static void draw(Stack s, String key, float x, float y, float w, float h, float hover, boolean locked) {
    String mob = id(key);
    boolean holo = holo(key);
    long now = System.currentTimeMillis();
    int t = Math.max(0, tierOf(mob));
    float ly = y - 2F * hover;
    G.rect(s, x + 2, ly + 3, w, h, 0x40000000);
    // Stanzrand mit abgeschnittenen Ecken
    G.rect(s, x + 2, ly, w - 4, h, 0xFFFFFFFF);
    G.rect(s, x, ly + 2, w, h - 4, 0xFFFFFFFF);
    // Rahmen in der Farbe der Seltenheit
    G.rect(s, x + 2, ly + 2, w - 4, h - 4, holo ? G.mix(0xFFB9C2D0, 0xFFFFFFFF, 0.5F + 0.5F * (float) Math.sin(now / 420.0)) : RIM[t]);
    float ix = x + 4, iy = ly + 4, iw = w - 8, ih = h - 16;
    G.rect(s, ix, iy, iw, ih, BACK[t]);
    if (holo) { // Glitzerfolie: silbrig heller Grund, darauf viele kleine Punkte, die durcheinander aufblinken
      G.rect(s, ix, iy, iw, ih, G.mix(RIM[t], 0xFF7C879E, 0.7F));
      G.rect(s, ix, iy, iw, ih / 2F, 0x18FFFFFF);
      for (int i = 0; i < 90; i++) {
        float a = (float) Math.sin(now / 170.0 + i * 2.39);
        if (a <= 0F) {
          continue;
        }
        float gx = ix + ((i * 7919) % 1000) / 1000F * (iw - 2), gy = iy + ((i * 104729) % 1000) / 1000F * (ih - 2);
        float size = i % 4 == 0 ? 2.4F : 1.5F;
        G.rect(s, gx, gy, size, size, G.alpha(GLITTER[i % GLITTER.length], a));
      }
    }
    // schraege Glanzstreifen; bei den seltensten wandern sie
    float shift = t == 3 || holo ? (now % 2400) / 2400F * (iw + ih) : iw * 0.35F;
    for (float yy = 0; yy < ih; yy += 2) {
      float sx = shift - yy;
      for (int k = 0; k < 2; k++) {
        float a = sx + k * 9, b = Math.max(0, a), e = Math.min(iw, a + (k == 0 ? 5 : 2));
        if (e > b) {
          G.rect(s, ix + b, iy + yy, e - b, 2, 0x70FFFFFF);
        }
      }
    }
    // Kopf aus der Spieltextur; nur wo noch kein Ausschnitt eingetragen ist, steht das Spawn-Ei aus dem Spiel
    float ratio = MobFaces.ratio(mob), fh = Math.min(ih - 6, (iw - 6) * ratio), fw = fh / ratio;
    float fx = ix + (iw - fw) / 2F, fy = iy + (ih - fh) / 2F;
    if (MobFaces.has(mob)) {
      G.rect(s, fx - 1, fy - 1, fw + 2, fh + 2, G.shade(RIM[t], 0.55F));
      MobFaces.draw(s, mob, fx, fy, fw, fh);
    } else {
      Icon egg = Mobs.byId(mob).icon();
      if (egg != null) {
        G.icon(s, egg, fx, fy, fw, fh);
      }
    }
    if (holo) { // Glitzer: kleine Sterne, die an festen Stellen aufblinken
      for (int i = 0; i < 7; i++) {
        float a = (float) Math.sin(now / 260.0 + i * 1.7);
        if (a > 0.35F) {
          float gx = ix + ((i * 37) % 100) / 100F * (iw - 4) + 2, gy = iy + ((i * 61) % 100) / 100F * (ih - 4) + 2;
          G.rect(s, gx - 0.5F, gy - 3, 1, 6, G.alpha(0xFFFFFFFF, a));
          G.rect(s, gx - 3, gy - 0.5F, 6, 1, G.alpha(0xFFFFFFFF, a));
        }
      }
    }
    // Muenzstempel: daran erkennt man den gekauften Sticker
    Loot.coin(s, x + w - 13, ly + h - 24, 1.5F);
    G.centered(s, G.fit(name(mob), w - 4, 0.8F), x + w / 2F, ly + h - 10, 0xFFFFFFFF, 0.8F, true);
    if (locked) {
      G.rect(s, x, ly, w, h, 0xB0000000);
    }
  }
}
