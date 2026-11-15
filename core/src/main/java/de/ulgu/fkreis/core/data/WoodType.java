package de.ulgu.fkreis.core.data;

import de.ulgu.fkreis.core.L;

/** Die 12 Holzarten fuers Regal (Farben nach den Minecraft-Bretter-Texturen). */
public enum WoodType {
  OAK("Eiche", "Oak", 0xFFB8945F, 0xFF3E2E1C),
  SPRUCE("Fichte", "Spruce", 0xFF6E5232, 0xFF2E2112),
  BIRCH("Birke", "Birch", 0xFFD7C98C, 0xFF4A4130),
  JUNGLE("Tropenholz", "Jungle", 0xFFA97452, 0xFF3E2A1D),
  ACACIA("Akazie", "Acacia", 0xFFAD5D32, 0xFF3F2414),
  DARK_OAK("Schwarzeiche", "Dark Oak", 0xFF4A3119, 0xFF22160A),
  MANGROVE("Mangrove", "Mangrove", 0xFF75352F, 0xFF2E1715),
  CHERRY("Kirsche", "Cherry", 0xFFE2B4AE, 0xFF4A3537),
  PALE_OAK("Blasseiche", "Pale Oak", 0xFFE8E2DE, 0xFF4D4744),
  BAMBOO("Bambus", "Bamboo", 0xFFD4BE5A, 0xFF4A4220),
  CRIMSON("Karmesin", "Crimson", 0xFF6B2F49, 0xFF2A121D),
  WARPED("Wirr", "Warped", 0xFF2B6A63, 0xFF10292A);

  private final String de;
  private final String en;
  public final int plank;
  public final int back;

  WoodType(String de, String en, int plank, int back) {
    this.de = de;
    this.en = en;
    this.plank = plank;
    this.back = back;
  }

  public String display() {
    return L.german() ? this.de : this.en;
  }

  public static WoodType parse(String name) {
    try {
      return WoodType.valueOf(name);
    } catch (Exception e) {
      return SPRUCE;
    }
  }
}
