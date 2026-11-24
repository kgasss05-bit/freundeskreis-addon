package de.ulgu.fkreis.core.diary;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import de.ulgu.fkreis.core.FkreisAddon;
import de.ulgu.fkreis.core.L;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

/**
 * Das private Tagebuch: ein Eintrag pro Tag, nur lokal gespeichert (labymod-neo/configs/fkreis/tagebuch.json).
 * Es wird nie verschickt und ist fuer andere Spieler nicht sichtbar.
 */
public class Diary {

  public static final String PLATE = "plate";
  public static final int WHITE_BOOK = 0xECE8DF;
  public static final int MAX_CHARS = 6000;

  /** Die 16 Farbstoffe in Inventar-Reihenfolge: Item-Name und Farbe (wie beim Faerben von Lederruestung). */
  public static final String[] DYE_IDS = {"white", "light_gray", "gray", "black", "brown", "red", "orange", "yellow",
      "lime", "green", "cyan", "light_blue", "blue", "purple", "magenta", "pink"};
  public static final int[] DYE_RGB = {0xF9FFFE, 0x9D9D97, 0x474F52, 0x1D1D21, 0x835432, 0xB02E26, 0xF9801D, 0xFED83D,
      0x80C71F, 0x5E7C16, 0x169C9C, 0x3AB3DA, 0x3C44AA, 0x8932B8, 0xC74EBD, 0xF38BAA};

  public static final class Entry {
    public String text = "";
    public String time = "";
  }

  private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
  private static final Path FILE = Paths.get("labymod-neo", "configs", "fkreis", "tagebuch.json");
  private static Diary instance;

  /** Datum (yyyy-MM-dd) -> Eintrag. */
  public Map<String, Entry> entries = new TreeMap<>();
  /** hell, mittel, dunkel oder eigen (weisses Buch, selbst gefaerbt). */
  public String leather = "mittel";
  /** none, gold oder silver. */
  public String trim = "none";
  /** Farbe des selbst gefaerbten Buchs (RGB), -1 = noch weiss. */
  public int custom = -1;
  /** Code zum Teilen ("" = wird nicht geteilt). Wer ihn kennt, darf das Tagebuch lesen. */
  public String shareCode = "";

  public static Diary get() {
    if (instance == null) {
      instance = load();
    }
    return instance;
  }

  private static Diary load() {
    try {
      if (Files.exists(FILE)) {
        try (Reader r = Files.newBufferedReader(FILE, StandardCharsets.UTF_8)) {
          Diary d = GSON.fromJson(r, Diary.class);
          if (d != null) {
            if (d.entries == null) {
              d.entries = new TreeMap<>();
            }
            if (d.leather == null) {
              d.leather = "mittel";
            }
            if (d.trim == null) {
              d.trim = "none";
            }
            if (d.shareCode == null) {
              d.shareCode = "";
            }
            return d;
          }
        }
      }
    } catch (Exception e) {
      FkreisAddon.log().warn("Tagebuch konnte nicht geladen werden: " + e);
    }
    return new Diary();
  }

  public void save() {
    try {
      Files.createDirectories(FILE.getParent());
      try (Writer w = Files.newBufferedWriter(FILE, StandardCharsets.UTF_8)) {
        GSON.toJson(this, w);
      }
    } catch (Exception e) {
      FkreisAddon.log().warn("Tagebuch konnte nicht gespeichert werden: " + e);
    }
  }

  public static String today() {
    return LocalDate.now().toString();
  }

  public static String clock() {
    return LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"));
  }

  public boolean wroteToday() {
    Entry e = this.entries.get(today());
    return e != null && e.text != null && !e.text.trim().isEmpty();
  }

  public String text(String day) {
    Entry e = this.entries.get(day);
    return e == null || e.text == null ? "" : e.text;
  }

  public String time(String day) {
    Entry e = this.entries.get(day);
    return e == null || e.time == null ? "" : e.time;
  }

  public void write(String text) {
    Entry e = this.entries.computeIfAbsent(today(), k -> new Entry());
    e.text = text;
    e.time = clock();
  }

  /** Seiten des Buchs: Namensblatt, alle frueheren Tage mit Text, zuletzt immer heute. */
  public List<String> pages() {
    List<String> out = new ArrayList<>();
    out.add(PLATE);
    String today = today();
    for (Map.Entry<String, Entry> e : this.entries.entrySet()) {
      if (!e.getKey().equals(today) && e.getValue() != null && e.getValue().text != null && !e.getValue().text.trim().isEmpty()) {
        out.add(e.getKey());
      }
    }
    out.add(today);
    return out;
  }

  private static Locale locale() {
    return L.german() ? Locale.GERMAN : Locale.ENGLISH;
  }

  /** "Montag" */
  public static String weekday(String day) {
    try {
      return LocalDate.parse(day).format(DateTimeFormatter.ofPattern("EEEE", locale()));
    } catch (Exception e) {
      return "";
    }
  }

  /** "23. November 2026" */
  public static String date(String day) {
    try {
      Locale l = locale();
      return LocalDate.parse(day).format(DateTimeFormatter.ofPattern(l == Locale.GERMAN ? "d. MMMM yyyy" : "MMMM d, yyyy", l));
    } catch (Exception e) {
      return day;
    }
  }

  /** Lederfarbe des aktuell gewaehlten Einbands. */
  public int leatherColor() {
    switch (this.leather) {
      case "hell":
        return 0xB3814B;
      case "dunkel":
        return 0x56301A;
      case "eigen":
        return this.custom < 0 ? WHITE_BOOK : this.custom;
      default:
        return 0x8A5630;
    }
  }

  /**
   * Mischen wie bei Lederruestung: Mittelwert der Farben, danach so aufhellen, dass die Mischung kraeftig bleibt.
   * current < 0 bedeutet: noch ungefaerbt, die neue Farbe wird direkt uebernommen.
   */
  public static int mixDye(int current, int dye) {
    if (current < 0) {
      return dye & 0xFFFFFF;
    }
    int[] a = {current >> 16 & 255, current >> 8 & 255, current & 255};
    int[] b = {dye >> 16 & 255, dye >> 8 & 255, dye & 255};
    float r = (a[0] + b[0]) / 2F, g = (a[1] + b[1]) / 2F, bl = (a[2] + b[2]) / 2F;
    float avgMax = (Math.max(a[0], Math.max(a[1], a[2])) + Math.max(b[0], Math.max(b[1], b[2]))) / 2F;
    float max = Math.max(r, Math.max(g, bl));
    float gain = max <= 0 ? 1F : avgMax / max;
    int ri = Math.min(255, Math.round(r * gain)), gi = Math.min(255, Math.round(g * gain)), bi = Math.min(255, Math.round(bl * gain));
    return ri << 16 | gi << 8 | bi;
  }
}
