package de.ulgu.fkreis.core.ui;

import de.ulgu.fkreis.core.FkreisAddon;
import java.util.UUID;
import net.labymod.api.Laby;
import net.labymod.api.client.gui.screen.activity.Activity;

/** Oeffnet die Bildschirme. Oeffnen wird um einen Tick verzoegert, damit z. B. der Chat sauber zugeht. */
public final class Screens {

  private static Activity next;
  private static int delay;

  private Screens() {
  }

  private static void show(Activity activity) {
    next = activity;
    delay = 1;
  }

  /** Wird jeden Tick aufgerufen. */
  public static void tick() {
    if (next == null) {
      return;
    }
    if (delay-- > 0) {
      return;
    }
    Activity a = next;
    next = null;
    Laby.labyAPI().minecraft().minecraftWindow().displayScreen(a);
  }

  public static void openStickerBook() {
    if (FkreisAddon.get().on()) {
      show(new StickerBookActivity());
    }
  }

  public static void openNotebook() {
    if (FkreisAddon.get().on()) {
      show(new NotebookActivity());
    }
  }

  public static void openNotes(UUID uuid, String name, boolean fromShelf) {
    if (FkreisAddon.get().on()) {
      show(new NotesActivity(uuid, name, fromShelf));
    }
  }

  public static void openProfile(UUID uuid, String name) {
    if (FkreisAddon.get().on()) {
      show(new ProfileScrollActivity(uuid, name));
    }
  }

  public static void openOwnProfile() {
    if (FkreisAddon.get().on()) {
      show(new ProfileScrollActivity(de.ulgu.fkreis.core.net.Net.self(), Laby.labyAPI().getName()));
    }
  }

  public static void openCalendar() {
    if (FkreisAddon.get().on()) {
      show(new CalendarActivity());
    }
  }

  public static void openAlbum() {
    if (FkreisAddon.get().on()) {
      show(new AlbumActivity());
    }
  }

  /** Beliebigen Bildschirm (verzoegert) oeffnen. */
  public static void open(Activity activity) {
    if (FkreisAddon.get().on()) {
      show(activity);
    }
  }

  /** Bildschirm ganz schliessen (zurueck ins Spiel) – verhindert Endlosschleifen zwischen Regal und Notizbuch. */
  public static void close() {
    Laby.labyAPI().minecraft().minecraftWindow().displayScreen((net.labymod.api.client.gui.screen.ScreenInstance) null);
  }

  /** Direkt (ohne Verzoegerung) – fuer Wechsel zwischen eigenen Bildschirmen. */
  public static void now(Activity activity) {
    Laby.labyAPI().minecraft().minecraftWindow().displayScreen(activity);
  }
}
