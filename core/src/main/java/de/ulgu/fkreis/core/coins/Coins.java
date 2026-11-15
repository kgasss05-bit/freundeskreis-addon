package de.ulgu.fkreis.core.coins;

import de.ulgu.fkreis.core.L;
import net.labymod.api.client.gui.icon.Icon;
import net.labymod.api.client.resources.ResourceLocation;

/** Die 20 Freundschafts-Muenzen: Muenze 1 sofort, danach alle 6 Monate die naechste (Muenze 20 nach 9,5 Jahren). */
public final class Coins {

  public static final int COUNT = 20;
  public static final int MONTHS_PER_STEP = 6;

  private static final String[] DE = {"Holz", "Stein", "Kupfer", "Eisen", "Gold", "Redstone", "Lapis", "Smaragd",
      "Rosenquarz", "Amethyst", "Prismarin", "Diamant", "Schleim", "Netherit", "Lava", "Echo", "Eis", "Netherstern",
      "Drachenei", "Legendär"};
  private static final String[] EN = {"Wood", "Stone", "Copper", "Iron", "Gold", "Redstone", "Lapis", "Emerald",
      "Rose Quartz", "Amethyst", "Prismarine", "Diamond", "Slime", "Netherite", "Lava", "Echo", "Ice", "Nether Star",
      "Dragon Egg", "Legendary"};

  private static final Icon[] ICONS = new Icon[COUNT];

  private Coins() {
  }

  public static Icon icon(int index) {
    int i = Math.max(0, Math.min(COUNT - 1, index));
    if (ICONS[i] == null) {
      ICONS[i] = Icon.texture(ResourceLocation.create("fkreis", String.format("textures/coins/coin_%02d.png", i + 1)));
    }
    return ICONS[i];
  }

  public static String name(int index) {
    int i = Math.max(0, Math.min(COUNT - 1, index));
    return L.german() ? DE[i] : EN[i];
  }

  /** Muenze fuer eine Freundschaft, die seit sinceMillis besteht. */
  public static int indexFor(long sinceMillis) {
    if (sinceMillis <= 0) {
      return 0;
    }
    double months = (System.currentTimeMillis() - sinceMillis) / (1000.0 * 60 * 60 * 24 * 30.4375);
    return Math.max(0, Math.min(COUNT - 1, (int) (months / MONTHS_PER_STEP)));
  }

  /** "sofort", "nach 6 Monaten", "nach 1 Jahr", "nach 1,5 Jahren" ... */
  public static String when(int index) {
    int months = index * MONTHS_PER_STEP;
    if (months == 0) {
      return L.t("coins.now");
    }
    if (months < 12) {
      return L.t("coins.months", months);
    }
    int years = months / 12;
    boolean half = months % 12 != 0;
    String y = half ? years + (L.german() ? ",5" : ".5") : String.valueOf(years);
    return (years == 1 && !half) ? L.t("coins.year") : L.t("coins.years", y);
  }
}
