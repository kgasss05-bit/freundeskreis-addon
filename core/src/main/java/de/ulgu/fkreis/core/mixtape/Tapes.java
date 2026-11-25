package de.ulgu.fkreis.core.mixtape;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import de.ulgu.fkreis.core.FkreisAddon;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Die Kassettenkiste: eigene Songs (hoechstens 80) und geschenkte von Freunden (hoechstens 20).
 * Gespeichert wird nur lokal in labymod-neo/configs/fkreis/mixtape.json.
 */
public class Tapes {

  public static final int MAX_OWN = 80, MAX_FRIEND = 20, MAX_TITLE = 22;
  public static final int[] COLORS = {0xD8381C, 0xF08A1E, 0xF2C94C, 0x4CAF50, 0x3A8FD8, 0x8E5BD0, 0xF08DB4, 0x8B8F99, 0x3A3E48, 0xF4EFE2};

  public static final class Tape {
    public String title = "";
    public String link = "";
    /** Name des Freundes, der die Kassette geschenkt hat; leer bei eigenen. */
    public String from = "";
    public int color = COLORS[0];

    public boolean gift() {
      return this.from != null && !this.from.isEmpty();
    }
  }

  private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
  private static final Path FILE = Paths.get("labymod-neo", "configs", "fkreis", "mixtape.json");
  private static Tapes instance;

  public List<Tape> tapes = new ArrayList<>();
  public String background = "oak";

  public static Tapes get() {
    if (instance == null) {
      instance = load();
    }
    return instance;
  }

  private static Tapes load() {
    try {
      if (Files.exists(FILE)) {
        try (Reader r = Files.newBufferedReader(FILE, StandardCharsets.UTF_8)) {
          Tapes t = GSON.fromJson(r, Tapes.class);
          if (t != null) {
            if (t.tapes == null) {
              t.tapes = new ArrayList<>();
            }
            if (t.background == null) {
              t.background = "oak";
            }
            t.tapes.removeIf(x -> x == null || x.title == null || x.link == null);
            return t;
          }
        }
      }
    } catch (Exception e) {
      FkreisAddon.log().warn("Mixtapes konnten nicht geladen werden: " + e);
    }
    return new Tapes();
  }

  public void save() {
    try {
      Files.createDirectories(FILE.getParent());
      try (Writer w = Files.newBufferedWriter(FILE, StandardCharsets.UTF_8)) {
        GSON.toJson(this, w);
      }
    } catch (Exception e) {
      FkreisAddon.log().warn("Mixtapes konnten nicht gespeichert werden: " + e);
    }
  }

  public int count(boolean gifts) {
    int n = 0;
    for (Tape t : this.tapes) {
      if (t.gift() == gifts) {
        n++;
      }
    }
    return n;
  }

  /** Nur Links zu YouTube und Spotify werden angenommen. */
  public static boolean validLink(String link) {
    if (link == null || link.length() > 200 || link.indexOf(' ') >= 0) {
      return false;
    }
    String l = link.toLowerCase(Locale.ROOT);
    if (!l.startsWith("https://")) {
      return false;
    }
    String host = l.substring(8);
    int slash = host.indexOf('/');
    if (slash <= 0 || slash == host.length() - 1) {
      return false;
    }
    host = host.substring(0, slash);
    return host.equals("youtu.be") || host.equals("youtube.com") || host.equals("www.youtube.com") || host.equals("m.youtube.com")
        || host.equals("music.youtube.com") || host.equals("open.spotify.com");
  }
}
