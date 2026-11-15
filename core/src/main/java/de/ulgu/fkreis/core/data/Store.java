package de.ulgu.fkreis.core.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Lokale Daten: Notizen, Sticker, zwischengespeicherte Steckbriefe. */
public class Store {

  private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
  private static final Path FILE = Paths.get("labymod-neo", "configs", "fkreis", "data.json");

  public Map<String, List<String>> notes = new HashMap<>();
  public List<Sticker> stickers = new ArrayList<>();
  public Set<String> given = new HashSet<>();
  public Map<String, ProfileData> profiles = new HashMap<>();
  public String lastBirthdayBroadcast = "";
  /** Leute im Regal (Reihenfolge = wann sie dazukamen) – nur wer eine Notiz hat oder angeklickt wurde. */
  public List<String> shelf = new ArrayList<>();
  public Map<String, String> names = new HashMap<>();
  /** Erster Tag, an dem das Addon die Freundschaft gesehen hat (falls LabyConnect keinen Zeitpunkt liefert). */
  public Map<String, Long> friendSince = new HashMap<>();
  /** Gespeicherter (geteilter) Geburtstag + wann er zuletzt geaendert wurde (30-Tage-Sperre). */
  public String birthdayCommitted = "";
  public long birthdayChangedAt;
  /** uuid -> Datum (yyyy-MM-dd), an dem sie laut Nachricht Geburtstag haben. */
  public Map<String, String> birthdayToday = new HashMap<>();
  /** uuid -> Datum, an dem wir schon gratuliert/benachrichtigt haben. */
  public Map<String, String> birthdayNotified = new HashMap<>();
  /** Fotoalbum. */
  public List<Photo> photos = new ArrayList<>();

  public static Path dir() {
    return FILE.getParent();
  }

  public static Store load() {
    try {
      if (Files.exists(FILE)) {
        try (Reader r = Files.newBufferedReader(FILE, StandardCharsets.UTF_8)) {
          Store s = GSON.fromJson(r, Store.class);
          if (s != null) {
            s.fixNulls();
            return s;
          }
        }
      }
    } catch (Exception e) {
      de.ulgu.fkreis.core.FkreisAddon.log().warn("Freundeskreis: " + e);
    }
    return new Store();
  }

  private void fixNulls() {
    if (this.notes == null) this.notes = new HashMap<>();
    if (this.stickers == null) this.stickers = new ArrayList<>();
    if (this.given == null) this.given = new HashSet<>();
    if (this.profiles == null) this.profiles = new HashMap<>();
    if (this.lastBirthdayBroadcast == null) this.lastBirthdayBroadcast = "";
    if (this.shelf == null) this.shelf = new ArrayList<>();
    if (this.names == null) this.names = new HashMap<>();
    if (this.friendSince == null) this.friendSince = new HashMap<>();
    if (this.birthdayCommitted == null) this.birthdayCommitted = "";
    if (this.birthdayToday == null) this.birthdayToday = new HashMap<>();
    if (this.birthdayNotified == null) this.birthdayNotified = new HashMap<>();
    if (this.photos == null) this.photos = new ArrayList<>();
  }

  public void save() {
    try {
      Files.createDirectories(FILE.getParent());
      try (Writer w = Files.newBufferedWriter(FILE, StandardCharsets.UTF_8)) {
        GSON.toJson(this, w);
      }
    } catch (Exception e) {
      de.ulgu.fkreis.core.FkreisAddon.log().warn("Freundeskreis: " + e);
    }
  }

  public List<String> notesOf(String uuid) {
    return this.notes.computeIfAbsent(uuid, k -> new ArrayList<>());
  }

  /** Person ins Regal stellen (falls noch nicht drin) und Namen merken. */
  public void touch(String uuid, String name) {
    if (name != null && !name.isEmpty()) {
      this.names.put(uuid, name);
    }
    if (!this.shelf.contains(uuid)) {
      this.shelf.add(uuid);
    }
    this.save();
  }

  public String nameOf(String uuid) {
    return this.names.getOrDefault(uuid, "?");
  }

  public boolean hasStickerFrom(String uuid) {
    for (Sticker s : this.stickers) {
      if (s.uuid.equals(uuid)) {
        return true;
      }
    }
    return false;
  }
}
