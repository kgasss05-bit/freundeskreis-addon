package de.ulgu.fkreis.core.ui;

import de.ulgu.fkreis.core.gfx.G;
import java.util.HashMap;
import java.util.Map;
import net.labymod.api.Laby;
import net.labymod.api.client.gui.icon.Icon;
import net.labymod.api.client.render.matrix.Stack;
import net.labymod.api.client.resources.ResourceLocation;

/**
 * Mob-Koepfe, zur Laufzeit zusammengesetzt aus den Mob-Texturen des Spiels (es liegt keine Mojang-Grafik im Addon).
 * Ein Kopf besteht aus einem oder mehreren Ausschnitten, die auf einer kleinen Flaeche uebereinandergelegt werden,
 * z. B. Kopf + Schnauze oder beim Silberfisch die ersten beiden Koerperscheiben.
 * Mobs, fuer die noch kein Ausschnitt eingetragen ist, zeigen das Spawn-Ei.
 */
public final class MobFaces {

  private static final class Face {
    final String path;
    final int tw, th;
    /** Groesse der Flaeche in Texturpixeln und die Teile: je sx, sy, sw, sh (Ausschnitt) und dx, dy (Platz auf der Flaeche). */
    final float cw, ch;
    final float[][] parts;

    Face(String path, int tw, int th, float cw, float ch, float[][] parts) {
      this.path = path;
      this.tw = tw;
      this.th = th;
      this.cw = cw;
      this.ch = ch;
      this.parts = parts;
    }
  }

  private static final Map<String, Face> FACES = new HashMap<>();

  /** Ein einzelner Ausschnitt. */
  private static void one(String id, String path, int x, int y, int w, int h, int tw, int th) {
    FACES.put(id, new Face(path, tw, th, w, h, new float[][] {{x, y, w, h, 0, 0}}));
  }

  /** Mehrere Ausschnitte uebereinander; spaetere liegen oben. */
  private static void many(String id, String path, int tw, int th, float cw, float ch, float[][] parts) {
    FACES.put(id, new Face(path, tw, th, cw, ch, parts));
  }

  /** true ab Minecraft 1.21.5: seitdem liegen Kuh, Schwein und Huhn als Klima-Varianten in eigenen Dateien. */
  private static boolean variants() {
    try {
      String[] v = Laby.labyAPI().minecraft().getVersion().split("\\.");
      int major = Integer.parseInt(v[0]), minor = v.length > 1 ? Integer.parseInt(v[1]) : 0, patch = v.length > 2 ? Integer.parseInt(v[2]) : 0;
      return major > 1 || minor > 21 || (minor == 21 && patch >= 5);
    } catch (RuntimeException e) {
      return true;
    }
  }

  static {
    // menschenaehnliche Mobs: Kopfvorderseite 8 x 8 (Dorfbewohner-Arten 8 x 10)
    one("villager", "textures/entity/villager/villager.png", 8, 8, 8, 10, 64, 64);
    one("zombie_villager", "textures/entity/zombie_villager/zombie_villager.png", 8, 8, 8, 10, 64, 64);
    one("witch", "textures/entity/witch.png", 8, 8, 8, 10, 64, 128);
    one("creeper", "textures/entity/creeper/creeper.png", 8, 8, 8, 8, 64, 32);
    one("zombie", "textures/entity/zombie/zombie.png", 8, 8, 8, 8, 64, 64);
    one("husk", "textures/entity/zombie/husk.png", 8, 8, 8, 8, 64, 64);
    one("drowned", "textures/entity/zombie/drowned.png", 8, 8, 8, 8, 64, 64);
    one("skeleton", "textures/entity/skeleton/skeleton.png", 8, 8, 8, 8, 64, 32);
    one("wither_skeleton", "textures/entity/skeleton/wither_skeleton.png", 8, 8, 8, 8, 64, 32);
    one("stray", "textures/entity/skeleton/stray.png", 8, 8, 8, 8, 64, 32);
    one("bogged", "textures/entity/skeleton/bogged.png", 8, 8, 8, 8, 64, 32);
    one("parched", "textures/entity/skeleton/parched.png", 8, 8, 8, 8, 64, 32);
    one("enderman", "textures/entity/enderman/enderman.png", 8, 8, 8, 8, 64, 32);
    one("blaze", "textures/entity/blaze.png", 8, 8, 8, 8, 64, 32);
    one("iron_golem", "textures/entity/iron_golem/iron_golem.png", 8, 8, 8, 10, 128, 128);
    one("snow_golem", "textures/entity/snow_golem.png", 8, 8, 8, 8, 64, 64);
    one("piglin", "textures/entity/piglin/piglin.png", 8, 8, 10, 8, 64, 64);
    one("piglin_brute", "textures/entity/piglin/piglin_brute.png", 8, 8, 10, 8, 64, 64);
    one("zombified_piglin", "textures/entity/piglin/zombified_piglin.png", 8, 8, 10, 8, 64, 64);
    one("pillager", "textures/entity/illager/pillager.png", 8, 8, 8, 10, 64, 64);
    one("vindicator", "textures/entity/illager/vindicator.png", 8, 8, 8, 10, 64, 64);
    one("evoker", "textures/entity/illager/evoker.png", 8, 8, 8, 10, 64, 64);
    one("illusioner", "textures/entity/illager/illusioner.png", 8, 8, 8, 10, 64, 64);
    one("wandering_trader", "textures/entity/wandering_trader.png", 8, 8, 8, 10, 64, 64);
    one("warden", "textures/entity/warden/warden.png", 10, 42, 16, 16, 128, 128);
    one("wither", "textures/entity/wither/wither.png", 8, 8, 8, 8, 64, 64);
    one("ravager", "textures/entity/illager/ravager.png", 16, 16, 16, 20, 128, 128);
    one("vex", "textures/entity/illager/vex.png", 5, 5, 5, 5, 32, 32);
    one("allay", "textures/entity/allay/allay.png", 5, 5, 5, 5, 32, 32);
    // Tiere: Kopf, bei manchen mit Schnauze, Schnabel oder Nase darueber
    boolean v = variants();
    many("pig", v ? "textures/entity/pig/temperate_pig.png" : "textures/entity/pig/pig.png", 64, v ? 64 : 32, 8, 8,
        new float[][] {{8, 8, 8, 8, 0, 0}, {17, 17, 4, 3, 2, 4}});
    one("cow", v ? "textures/entity/cow/temperate_cow.png" : "textures/entity/cow/cow.png", 6, 6, 8, 8, 64, v ? 64 : 32);
    one("mooshroom", "textures/entity/cow/red_mooshroom.png", 6, 6, 8, 8, 64, v ? 64 : 32);
    many("chicken", v ? "textures/entity/chicken/temperate_chicken.png" : "textures/entity/chicken.png", 64, 32, 4, 6,
        new float[][] {{3, 3, 4, 6, 0, 0}, {16, 2, 4, 2, 0, 2}, {16, 6, 2, 2, 1, 4}});
    one("sheep", "textures/entity/sheep/sheep.png", 8, 8, 6, 6, 64, 32);
    many("wolf", "textures/entity/wolf/wolf.png", 64, 32, 6, 6, new float[][] {{4, 4, 6, 6, 0, 0}, {4, 14, 3, 3, 1.5F, 3}});
    many("fox", "textures/entity/fox/fox.png", 48, 32, 8, 6, new float[][] {{7, 11, 8, 6, 0, 0}, {9, 21, 4, 2, 2, 4}});
    many("cat", "textures/entity/cat/tabby.png", 64, 32, 5, 4, new float[][] {{5, 5, 5, 4, 0, 0}, {2, 26, 3, 2, 1, 2}});
    many("ocelot", "textures/entity/cat/ocelot.png", 64, 32, 5, 4, new float[][] {{5, 5, 5, 4, 0, 0}, {2, 26, 3, 2, 1, 2}});
    one("panda", "textures/entity/panda/panda.png", 9, 15, 13, 10, 64, 64);
    one("polar_bear", "textures/entity/bear/polarbear.png", 7, 7, 7, 7, 128, 64);
    one("rabbit", "textures/entity/rabbit/brown.png", 37, 5, 5, 4, 64, 32);
    one("goat", "textures/entity/goat/goat.png", 44, 56, 5, 7, 64, 64);
    one("bee", "textures/entity/bee/bee.png", 10, 10, 7, 7, 64, 64);
    one("parrot", "textures/entity/parrot/parrot_red_blue.png", 4, 4, 2, 3, 32, 32);
    one("turtle", "textures/entity/turtle/big_sea_turtle.png", 9, 6, 6, 5, 128, 64);
    one("axolotl", "textures/entity/axolotl/axolotl_lucy.png", 5, 6, 8, 5, 64, 64);
    one("dolphin", "textures/entity/dolphin.png", 6, 6, 8, 7, 64, 64);
    many("llama", "textures/entity/llama/creamy.png", 128, 64, 8, 8, new float[][] {{6, 20, 8, 8, 0, 0}, {9, 9, 4, 4, 2, 2}});
    float[][] horse = {{7, 20, 6, 5, 0, 0}, {5, 30, 4, 5, 1, 5}};
    many("horse", "textures/entity/horse/horse_brown.png", 64, 64, 6, 10, horse);
    many("donkey", "textures/entity/horse/donkey.png", 64, 64, 6, 10, horse);
    many("mule", "textures/entity/horse/mule.png", 64, 64, 6, 10, horse);
    many("skeleton_horse", "textures/entity/horse/horse_skeleton.png", 64, 64, 6, 10, horse);
    many("zombie_horse", "textures/entity/horse/horse_zombie.png", 64, 64, 6, 10, horse);
    // Monster und Wasserwesen
    one("spider", "textures/entity/spider/spider.png", 40, 12, 8, 8, 64, 32);
    one("cave_spider", "textures/entity/spider/cave_spider.png", 40, 12, 8, 8, 64, 32);
    one("ghast", "textures/entity/ghast/ghast.png", 16, 16, 16, 16, 64, 32);
    one("strider", "textures/entity/strider/strider.png", 16, 16, 16, 14, 64, 128);
    one("shulker", "textures/entity/shulker/shulker.png", 6, 58, 6, 6, 64, 64);
    one("phantom", "textures/entity/phantom.png", 5, 5, 7, 3, 64, 64);
    one("hoglin", "textures/entity/hoglin/hoglin.png", 80, 20, 14, 6, 128, 64);
    one("zoglin", "textures/entity/hoglin/zoglin.png", 80, 20, 14, 6, 128, 64);
    many("guardian", "textures/entity/guardian.png", 64, 64, 12, 12, new float[][] {{16, 16, 12, 12, 0, 0}, {9, 1, 2, 2, 5, 5}});
    many("elder_guardian", "textures/entity/guardian_elder.png", 64, 64, 12, 12, new float[][] {{16, 16, 12, 12, 0, 0}, {9, 1, 2, 2, 5, 5}});
    many("slime", "textures/entity/slime/slime.png", 64, 32, 8, 8,
        new float[][] {{6, 22, 6, 6, 1, 1}, {34, 2, 2, 2, 0.75F, 2}, {34, 6, 2, 2, 5.25F, 2}, {33, 9, 1, 1, 4, 5}, {8, 8, 8, 8, 0, 0}});
    many("magma_cube", "textures/entity/slime/magmacube.png", 64, 32, 8, 8, new float[][] {
        {8, 8, 8, 1, 0, 0}, {8, 9, 8, 1, 0, 1}, {32, 18, 8, 1, 0, 2}, {32, 27, 8, 1, 0, 3},
        {8, 12, 8, 1, 0, 4}, {8, 13, 8, 1, 0, 5}, {8, 14, 8, 1, 0, 6}, {8, 15, 8, 1, 0, 7}});
    one("squid", "textures/entity/squid/squid.png", 12, 12, 12, 16, 64, 32);
    one("glow_squid", "textures/entity/squid/glow_squid.png", 12, 12, 12, 16, 64, 32);
    one("pufferfish", "textures/entity/fish/pufferfish.png", 8, 8, 8, 8, 32, 32);
    one("cod", "textures/entity/fish/cod.png", 0, 7, 7, 4, 32, 32);
    one("salmon", "textures/entity/fish/salmon.png", 0, 8, 8, 5, 32, 32);
    one("tropical_fish", "textures/entity/fish/tropical_a.png", 0, 6, 6, 3, 32, 32);
    one("tadpole", "textures/entity/tadpole/tadpole.png", 3, 3, 3, 2, 16, 16);
    // Silberfisch und Endermite haben keinen Kopf: die vordersten beiden Koerperscheiben uebereinander
    many("silverfish", "textures/entity/silverfish.png", 64, 32, 4, 3, new float[][] {{2, 6, 4, 3, 0, 0}, {2, 2, 3, 2, 0.5F, 1}});
    many("endermite", "textures/entity/endermite.png", 64, 32, 6, 4, new float[][] {{5, 10, 6, 4, 0, 0}, {2, 2, 4, 3, 1, 1}});
    // Enderdrache von der Seite: Schnauze vor dem Schaedel
    many("ender_dragon", "textures/entity/enderdragon/dragon.png", 256, 256, 32, 16, new float[][] {{112, 46, 16, 16, 16, 0}, {176, 60, 16, 5, 0, 7}});
  }

  private MobFaces() {
  }

  public static boolean has(String id) {
    return FACES.containsKey(id);
  }

  /** Einzelnes Gesicht als Icon (nur bei Koepfen aus genau einem Ausschnitt), sonst null. */
  public static Icon face(String id) {
    Face f = FACES.get(id);
    if (f == null || f.parts.length != 1) {
      return null;
    }
    float[] p = f.parts[0];
    return G.sprite(ResourceLocation.create("minecraft", f.path), (int) p[0], (int) p[1], (int) p[2], (int) p[3], f.tw, f.th);
  }

  /** Hoehe / Breite des Kopfes. */
  public static float ratio(String id) {
    Face f = FACES.get(id);
    return f == null ? 1F : f.ch / f.cw;
  }

  /** Zeichnet den Kopf in das Feld (x, y, w, h); das Feld sollte das Seitenverhaeltnis {@link #ratio} haben. Liefert false, wenn es keinen gibt. */
  public static boolean draw(Stack s, String id, float x, float y, float w, float h) {
    Face f = FACES.get(id);
    if (f == null) {
      return false;
    }
    float ux = w / f.cw, uy = h / f.ch;
    ResourceLocation loc = ResourceLocation.create("minecraft", f.path);
    for (float[] p : f.parts) {
      G.icon(s, G.sprite(loc, (int) p[0], (int) p[1], (int) p[2], (int) p[3], f.tw, f.th), x + p[4] * ux, y + p[5] * uy, p[2] * ux, p[3] * uy);
    }
    return true;
  }
}
