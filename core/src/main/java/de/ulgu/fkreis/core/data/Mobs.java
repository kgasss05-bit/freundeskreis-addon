package de.ulgu.fkreis.core.data;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/** Alle Mobs mit Spawn-Ei (Stand Minecraft 1.21.11 / 26.x), alphabetisch nach Minecraft-ID wie im Original. */
public final class Mobs {

  public static final Mob NONE = new Mob("none", "Keine Angabe", "Not specified");

  private static final List<Mob> ALL = Collections.unmodifiableList(new ArrayList<>(Arrays.asList(
      NONE,
      new Mob("allay", "Allay", "Allay"),
      new Mob("armadillo", "Gürteltier", "Armadillo"),
      new Mob("axolotl", "Axolotl", "Axolotl"),
      new Mob("bat", "Fledermaus", "Bat"),
      new Mob("bee", "Biene", "Bee"),
      new Mob("blaze", "Lohe", "Blaze"),
      new Mob("bogged", "Sumpfskelett", "Bogged"),
      new Mob("breeze", "Böe", "Breeze"),
      new Mob("camel", "Kamel", "Camel"),
      new Mob("camel_husk", "Wüstenkamel", "Camel Husk"),
      new Mob("cat", "Katze", "Cat"),
      new Mob("cave_spider", "Höhlenspinne", "Cave Spider"),
      new Mob("chicken", "Huhn", "Chicken"),
      new Mob("cod", "Kabeljau", "Cod"),
      new Mob("copper_golem", "Kupfergolem", "Copper Golem"),
      new Mob("cow", "Kuh", "Cow"),
      new Mob("creaking", "Knarz", "Creaking"),
      new Mob("creeper", "Creeper", "Creeper"),
      new Mob("dolphin", "Delfin", "Dolphin"),
      new Mob("donkey", "Esel", "Donkey"),
      new Mob("drowned", "Ertrunkener", "Drowned"),
      new Mob("elder_guardian", "Großer Wächter", "Elder Guardian"),
      new Mob("ender_dragon", "Enderdrache", "Ender Dragon"),
      new Mob("enderman", "Enderman", "Enderman"),
      new Mob("endermite", "Endermite", "Endermite"),
      new Mob("evoker", "Magier", "Evoker"),
      new Mob("fox", "Fuchs", "Fox"),
      new Mob("frog", "Frosch", "Frog"),
      new Mob("ghast", "Ghast", "Ghast"),
      new Mob("glow_squid", "Leuchttintenfisch", "Glow Squid"),
      new Mob("goat", "Ziege", "Goat"),
      new Mob("guardian", "Wächter", "Guardian"),
      new Mob("happy_ghast", "Glücklicher Ghast", "Happy Ghast"),
      new Mob("hoglin", "Hoglin", "Hoglin"),
      new Mob("horse", "Pferd", "Horse"),
      new Mob("husk", "Wüstenzombie", "Husk"),
      new Mob("iron_golem", "Eisengolem", "Iron Golem"),
      new Mob("llama", "Lama", "Llama"),
      new Mob("magma_cube", "Magmawürfel", "Magma Cube"),
      new Mob("mooshroom", "Pilzkuh", "Mooshroom"),
      new Mob("mule", "Maultier", "Mule"),
      new Mob("nautilus", "Nautilus", "Nautilus"),
      new Mob("ocelot", "Ozelot", "Ocelot"),
      new Mob("panda", "Panda", "Panda"),
      new Mob("parched", "Ausgedörrter", "Parched"),
      new Mob("parrot", "Papagei", "Parrot"),
      new Mob("phantom", "Phantom", "Phantom"),
      new Mob("pig", "Schwein", "Pig"),
      new Mob("piglin", "Piglin", "Piglin"),
      new Mob("piglin_brute", "Piglin-Barbar", "Piglin Brute"),
      new Mob("pillager", "Plünderer", "Pillager"),
      new Mob("polar_bear", "Eisbär", "Polar Bear"),
      new Mob("pufferfish", "Kugelfisch", "Pufferfish"),
      new Mob("rabbit", "Kaninchen", "Rabbit"),
      new Mob("ravager", "Verwüster", "Ravager"),
      new Mob("salmon", "Lachs", "Salmon"),
      new Mob("sheep", "Schaf", "Sheep"),
      new Mob("shulker", "Shulker", "Shulker"),
      new Mob("silverfish", "Silberfischchen", "Silverfish"),
      new Mob("skeleton", "Skelett", "Skeleton"),
      new Mob("skeleton_horse", "Skelettpferd", "Skeleton Horse"),
      new Mob("slime", "Schleim", "Slime"),
      new Mob("sniffer", "Schnüffler", "Sniffer"),
      new Mob("snow_golem", "Schneegolem", "Snow Golem"),
      new Mob("spider", "Spinne", "Spider"),
      new Mob("squid", "Tintenfisch", "Squid"),
      new Mob("stray", "Eiswanderer", "Stray"),
      new Mob("strider", "Schreiter", "Strider"),
      new Mob("tadpole", "Kaulquappe", "Tadpole"),
      new Mob("trader_llama", "Händlerlama", "Trader Llama"),
      new Mob("tropical_fish", "Tropenfisch", "Tropical Fish"),
      new Mob("turtle", "Schildkröte", "Turtle"),
      new Mob("vex", "Plagegeist", "Vex"),
      new Mob("villager", "Dorfbewohner", "Villager"),
      new Mob("vindicator", "Diener", "Vindicator"),
      new Mob("wandering_trader", "Fahrender Händler", "Wandering Trader"),
      new Mob("warden", "Wärter", "Warden"),
      new Mob("witch", "Hexe", "Witch"),
      new Mob("wither", "Wither", "Wither"),
      new Mob("wither_skeleton", "Witherskelett", "Wither Skeleton"),
      new Mob("wolf", "Wolf", "Wolf"),
      new Mob("zoglin", "Zoglin", "Zoglin"),
      new Mob("zombie", "Zombie", "Zombie"),
      new Mob("zombie_horse", "Zombiepferd", "Zombie Horse"),
      new Mob("zombie_nautilus", "Zombie-Nautilus", "Zombie Nautilus"),
      new Mob("zombie_villager", "Zombiedorfbewohner", "Zombie Villager"),
      new Mob("zombified_piglin", "Zombifizierter Piglin", "Zombified Piglin")
  )));

  private Mobs() {
  }

  public static List<Mob> all() {
    return ALL;
  }

  public static Mob byId(String id) {
    for (Mob mob : ALL) {
      if (mob.id().equals(id)) {
        return mob;
      }
    }
    return NONE;
  }
}
