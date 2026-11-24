package de.ulgu.fkreis.core.vanish.ui;

import de.ulgu.fkreis.core.vanish.Vanish;
import net.labymod.api.Laby;
import net.labymod.api.client.gui.screen.activity.Activity;

/** Oeffnet Bildschirme um einen Tick verzoegert (damit z. B. der Chat sauber zugeht). */
public final class Screens {

  private static Activity next;
  private static int delay;

  private Screens() {
  }

  public static void tick() {
    if (next == null || delay-- > 0) {
      return;
    }
    Activity a = next;
    next = null;
    Laby.labyAPI().minecraft().minecraftWindow().displayScreen(a);
  }

  public static void close() {
    Laby.labyAPI().minecraft().minecraftWindow().displayScreen((net.labymod.api.client.gui.screen.ScreenInstance) null);
  }

  public static void openList() {
    if (Vanish.get() != null && Vanish.get().on()) {
      next = new BanishScrollActivity();
      delay = 1;
    }
  }
}
