package de.ulgu.fkreis.core.vanish.gfx;

import net.labymod.api.client.gui.icon.Icon;
import net.labymod.api.client.gui.screen.ScreenContext;
import net.labymod.api.client.gui.screen.state.ScreenCanvas;
import net.labymod.api.client.render.matrix.Stack;

/**
 * Zeichnen ueber LabyMods Canvas (ScreenContext#canvas()).
 *
 * Seit Minecraft 1.21.6 werden Bildschirme verzoegert gezeichnet. Die Canvas sammelt alles ein
 * und zeichnet es zum richtigen Zeitpunkt. Alle eigenen Bildschirme zeichnen deshalb hierueber.
 */
public final class Cv {

  private static ScreenCanvas canvas;

  private Cv() {
  }

  /** Wird zu Beginn jedes Frames vom Bildschirm aufgerufen. */
  public static void begin(ScreenContext context, Stack s) {
    canvas = context.canvas();
  }

  public static void end() {
    canvas = null;
  }

  public static boolean active() {
    return canvas != null;
  }

  public static boolean rect(float x, float y, float w, float h, int argb) {
    if (canvas == null) {
      return false;
    }
    canvas.submitRelativeRect(x, y, w, h, argb);
    return true;
  }

  public static boolean icon(Icon ic, float x, float y, float w, float h) {
    if (canvas == null || ic == null) {
      return false;
    }
    canvas.submitIcon(ic, x, y, w, h);
    return true;
  }

  /** Text an (x, y). Skalierung kommt ueber den Stack (siehe G.text). */
  public static boolean text(String t, float x, float y, int argb, boolean shadow) {
    if (canvas == null) {
      return false;
    }
    canvas.submitText(t, x, y, argb, 1F, 0);
    return true;
  }
}
