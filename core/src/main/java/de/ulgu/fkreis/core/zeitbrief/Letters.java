package de.ulgu.fkreis.core.zeitbrief;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import de.ulgu.fkreis.core.FkreisAddon;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Zeitbriefe: Briefe an dich selbst, die erst an einem gewaehlten Tag wieder auftauchen.
 * Nur lokal gespeichert (labymod-neo/configs/fkreis/zeitbrief.json). Bis zum Tag zeigt das Addon nichts davon an.
 */
public class Letters {

  public static final int MAX_CHARS = 500, MAX_WAITING = 50;
  public static final class Letter {
    public String text = "";
    /** Tag, an dem er geschrieben wurde, und Tag, ab dem er ankommt (yyyy-MM-dd). */
    public String written = "";
    public String due = "";
  }

  private static final Gson GSON = new GsonBuilder().create();
  private static final Path FILE = Paths.get("labymod-neo", "configs", "fkreis", "zeitbrief.json");
  private static Letters instance;

  public List<Letter> letters = new ArrayList<>();
  public String background = "spruce";

  public static Letters get() {
    if (instance == null) {
      instance = load();
    }
    return instance;
  }

  private static Letters load() {
    try {
      if (Files.exists(FILE)) {
        try (Reader r = Files.newBufferedReader(FILE, StandardCharsets.UTF_8)) {
          Letters l = GSON.fromJson(r, Letters.class);
          if (l != null) {
            if (l.letters == null) {
              l.letters = new ArrayList<>();
            }
            if (l.background == null) {
              l.background = "spruce";
            }
            l.letters.removeIf(x -> x == null || x.text == null || x.due == null || x.written == null);
            return l;
          }
        }
      }
    } catch (Exception e) {
      FkreisAddon.log().warn("Zeitbriefe konnten nicht geladen werden: " + e);
    }
    return new Letters();
  }

  public void save() {
    try {
      Files.createDirectories(FILE.getParent());
      try (Writer w = Files.newBufferedWriter(FILE, StandardCharsets.UTF_8)) {
        GSON.toJson(this, w);
      }
    } catch (Exception e) {
      FkreisAddon.log().warn("Zeitbriefe konnten nicht gespeichert werden: " + e);
    }
  }

  /** Der aelteste Brief, dessen Tag gekommen ist – oder null. */
  public Letter arrived() {
    String today = LocalDate.now().toString();
    Letter best = null;
    for (Letter l : this.letters) {
      if (l.due.compareTo(today) <= 0 && (best == null || l.due.compareTo(best.due) < 0)) {
        best = l;
      }
    }
    return best;
  }
}
