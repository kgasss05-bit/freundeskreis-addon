package de.ulgu.fkreis.core.gfx;

import net.labymod.api.client.render.matrix.Stack;

/** Hintergruende fuer Bildschirme: die Holzarten aus dem Spiel als Bretterwand. */
public final class Backdrop {

  public static final String[] IDS = {"oak", "spruce", "birch", "dark_oak", "jungle", "acacia", "mangrove", "cherry"};
  private static final int[] WOOD = {0xA8834F, 0x6E4E2C, 0xC9B37A, 0x3F2A16, 0xA07350, 0xA85A32, 0x6F2A2D, 0xDFA9A4};

  private Backdrop() {
  }

  public static int index(String id) {
    for (int i = 0; i < IDS.length; i++) {
      if (IDS[i].equals(id)) {
        return i;
      }
    }
    return 0;
  }

  public static String next(String id) {
    return IDS[(index(id) + 1) % IDS.length];
  }

  /** Das Holz ohne Abdunkeln (fuer die Auswahl). */
  public static void swatch(Stack s, String id, float x, float y, float w, float h) {
    int i = index(id);
    G.planks(s, x, y, w, h, 0xFF000000 | WOOD[i], 7 + i);
  }

  public static void draw(Stack s, String id, float x, float y, float w, float h) {
    int i = index(id);
    G.planks(s, x, y, w, h, 0xFF000000 | WOOD[i], 7 + i);
    G.rect(s, x, y, w, h, 0x70000000); // etwas abdunkeln, damit alles davor gut lesbar bleibt
  }
}
