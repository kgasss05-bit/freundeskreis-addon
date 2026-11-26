package de.ulgu.fkreis.core.match;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import de.ulgu.fkreis.core.FkreisAddon;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;

/**
 * Freunde-Match: 100 Fragen, davon 90 Aussagen (fuenf Stufen von "stimmt gar nicht" bis "stimmt genau")
 * und 10 Minecraft-Fragen mit drei Antworten, die zwischendrin kommen. Die Aussagen machen 99 % der
 * Uebereinstimmung aus, die Minecraft-Fragen zusammen 1 %. Gespeichert wird lokal in configs/fkreis/blockmatch.json.
 */
public class MatchData {

  public static final int TOTAL = 100, STATEMENTS = 90, FUN = 10, PER_TOPIC = 9;
  /** An diesen Stellen des Fragebogens steht eine Minecraft-Frage. */
  private static final int[] FUN_AT = {6, 15, 27, 34, 48, 53, 66, 71, 85, 93};
  /** je Platz: >= 0 Nummer der Aussage, sonst -(Nummer der Minecraft-Frage) - 1 */
  private static final int[] SLOT = new int[TOTAL];

  static {
    int st = 0, fun = 0;
    for (int i = 0; i < TOTAL; i++) {
      if (fun < FUN && FUN_AT[fun] == i) {
        SLOT[i] = -(fun++) - 1;
      } else {
        SLOT[i] = st++;
      }
    }
  }

  private static final Gson GSON = new GsonBuilder().create();
  private static final Path FILE = Paths.get("labymod-neo", "configs", "fkreis", "blockmatch.json");
  private static MatchData instance;

  /** Antwort je Platz: -1 offen, sonst 0..4 (Aussage) oder 0..2 (Minecraft-Frage). */
  public int[] answers = blank();
  public String background = "oak";
  /** Farbwelt des Menues: "" (noch nicht gewaehlt), pink, blue, mint, lilac oder classic. */
  public String theme = "";
  /** Wann die Antworten zuletzt zurueckgesetzt wurden (Millisekunden); das geht nur einmal pro Woche und nur vor dem Absenden. */
  public long lastReset;
  /** true, sobald am Ende auf "Absenden" gedrueckt wurde. Danach laesst sich nichts mehr aendern oder loeschen. */
  public boolean submitted;

  private static int[] blank() {
    int[] a = new int[TOTAL];
    Arrays.fill(a, -1);
    return a;
  }

  public static boolean isFun(int slot) {
    return SLOT[slot] < 0;
  }

  /** Nummer der Aussage (0..89) oder der Minecraft-Frage (0..9) an diesem Platz. */
  public static int number(int slot) {
    return SLOT[slot] < 0 ? -SLOT[slot] - 1 : SLOT[slot];
  }

  public static int options(int slot) {
    return isFun(slot) ? 3 : 5;
  }

  public static MatchData get() {
    if (instance == null) {
      instance = load();
    }
    return instance;
  }

  private static MatchData load() {
    try {
      if (Files.exists(FILE)) {
        try (Reader r = Files.newBufferedReader(FILE, StandardCharsets.UTF_8)) {
          MatchData d = GSON.fromJson(r, MatchData.class);
          if (d != null) {
            if (d.answers == null || d.answers.length != TOTAL) {
              d.answers = blank();
            }
            if (d.background == null) {
              d.background = "oak";
            }
            if (d.theme == null) {
              d.theme = "";
            }
            return d;
          }
        }
      }
    } catch (Exception e) {
      FkreisAddon.log().warn("Freunde-Match konnte nicht geladen werden: " + e);
    }
    return new MatchData();
  }

  public void save() {
    try {
      Files.createDirectories(FILE.getParent());
      try (Writer w = Files.newBufferedWriter(FILE, StandardCharsets.UTF_8)) {
        GSON.toJson(this, w);
      }
    } catch (Exception e) {
      FkreisAddon.log().warn("Freunde-Match konnte nicht gespeichert werden: " + e);
    }
  }

  public static final long RESET_EVERY_MS = 7L * 24 * 60 * 60 * 1000;

  /** Wie viele Tage man noch warten muss, bis man wieder zuruecksetzen darf (0 = jetzt moeglich). */
  public int daysUntilReset() {
    long left = this.lastReset + RESET_EVERY_MS - System.currentTimeMillis();
    return left <= 0 ? 0 : (int) Math.ceil(left / (24.0 * 60 * 60 * 1000));
  }

  /** Alle Antworten loeschen und die Wochenfrist neu starten (nur vor dem Absenden). */
  public void reset() {
    if (this.submitted) {
      return;
    }
    Arrays.fill(this.answers, -1);
    this.lastReset = System.currentTimeMillis();
    this.save();
  }

  /** Solange nicht abgesendet wurde, darf man Antworten frei aendern. */
  public boolean mayChange(int slot) {
    return !this.submitted;
  }

  public void change(int slot, int value) {
    if (!this.submitted) {
      this.answers[slot] = value;
      this.save();
    }
  }

  /** Endgueltig absenden: Erst jetzt nimmt man am Matching teil. */
  public void submit() {
    if (this.complete()) {
      this.submitted = true;
      this.save();
    }
  }

  public int answered() {
    int n = 0;
    for (int a : this.answers) {
      if (a >= 0) {
        n++;
      }
    }
    return n;
  }

  public boolean complete() {
    return this.answered() == TOTAL;
  }

  /** Erster offener Platz, oder -1 wenn alles beantwortet ist. */
  public int firstOpen() {
    for (int i = 0; i < TOTAL; i++) {
      if (this.answers[i] < 0) {
        return i;
      }
    }
    return -1;
  }

  /** Alle Antworten als 100 Ziffern. */
  public String pack() {
    StringBuilder sb = new StringBuilder(TOTAL);
    for (int a : this.answers) {
      sb.append((char) ('0' + Math.max(0, a)));
    }
    return sb.toString();
  }

  /** Liest fremde Antworten; null, wenn sie nicht vollstaendig oder ungueltig sind. */
  public static int[] unpack(String s) {
    if (s == null || s.length() != TOTAL) {
      return null;
    }
    int[] a = new int[TOTAL];
    for (int i = 0; i < TOTAL; i++) {
      int v = s.charAt(i) - '0';
      if (v < 0 || v >= options(i)) {
        return null;
      }
      a[i] = v;
    }
    return a;
  }

  /** Uebereinstimmung in Prozent: Aussagen zaehlen 99 %, die Minecraft-Fragen zusammen 1 %. */
  public static int percent(int[] mine, int[] other) {
    float diff = 0;
    int same = 0;
    for (int i = 0; i < TOTAL; i++) {
      if (isFun(i)) {
        if (mine[i] == other[i]) {
          same++;
        }
      } else {
        diff += Math.abs(mine[i] - other[i]);
      }
    }
    float statements = 1F - diff / (STATEMENTS * 4F);
    return Math.round((0.99F * statements + 0.01F * same / (float) FUN) * 100F);
  }
}
