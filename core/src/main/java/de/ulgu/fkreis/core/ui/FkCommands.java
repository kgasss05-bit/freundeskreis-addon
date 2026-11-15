package de.ulgu.fkreis.core.ui;

import de.ulgu.fkreis.core.net.Net;
import net.labymod.api.Laby;
import net.labymod.api.client.chat.command.Command;

/** /sammelbuch, /notizbuch, /steckbrief, /kalender und /fotoalbum. */
public final class FkCommands {

  private FkCommands() {
  }

  public static class Sammelbuch extends Command {
    public Sammelbuch() {
      super("sammelbuch", "stickerbook");
    }

    @Override
    public boolean execute(String prefix, String[] arguments) {
      Screens.openStickerBook();
      return true;
    }
  }

  public static class Notizbuch extends Command {
    public Notizbuch() {
      super("notizbuch", "notebook");
    }

    @Override
    public boolean execute(String prefix, String[] arguments) {
      Screens.openNotebook();
      return true;
    }
  }

  public static class Steckbrief extends Command {
    public Steckbrief() {
      super("steckbrief", "profile");
    }

    @Override
    public boolean execute(String prefix, String[] arguments) {
      Screens.openProfile(Net.self(), Laby.labyAPI().getName());
      return true;
    }
  }

  public static class Hub extends Command {
    public Hub() {
      super("fk", "freundeskreis");
    }

    @Override
    public boolean execute(String prefix, String[] arguments) {
      Screens.open(new HubActivity());
      return true;
    }
  }

  public static class Kalender extends Command {
    public Kalender() {
      super("kalender", "calendar");
    }

    @Override
    public boolean execute(String prefix, String[] arguments) {
      Screens.openCalendar();
      return true;
    }
  }

  public static class Fotoalbum extends Command {
    public Fotoalbum() {
      super("fotoalbum", "album");
    }

    @Override
    public boolean execute(String prefix, String[] arguments) {
      Screens.openAlbum();
      return true;
    }
  }
}
