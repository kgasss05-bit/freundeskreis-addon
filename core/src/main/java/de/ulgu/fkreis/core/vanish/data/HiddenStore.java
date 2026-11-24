package de.ulgu.fkreis.core.vanish.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import de.ulgu.fkreis.api.VanisherState;
import de.ulgu.fkreis.core.vanish.Vanish;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Liste der ausgeblendeten Spieler. Gespeichert nach UUID – Namensaenderungen sind egal.
 * Datei: labymod-neo/configs/vanisher/hidden.json
 */
public class HiddenStore {

  private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
  private static final Path OLD_FILE = Paths.get("labymod-neo", "configs", "vanisher", "hidden.json");
  private static final Path FILE = Paths.get("labymod-neo", "configs", "fkreis", "vanish.json");

  /** uuid -> zuletzt bekannter Name (nur fuer die Anzeige in der Liste). */
  public Map<String, String> hidden = new LinkedHashMap<>();

  public static HiddenStore load() {
    HiddenStore store = null;
    try {
      // Liste aus dem alten, einzelnen Vanisher-Addon uebernehmen (nur beim ersten Start)
      Path from = Files.exists(FILE) ? FILE : OLD_FILE;
      if (Files.exists(from)) {
        try (Reader r = Files.newBufferedReader(from, StandardCharsets.UTF_8)) {
          store = GSON.fromJson(r, HiddenStore.class);
        }
      }
    } catch (Exception e) {
      Vanish.log().warn("Vanisher: Liste konnte nicht geladen werden: " + e);
    }
    if (store == null) {
      store = new HiddenStore();
    }
    if (store.hidden == null) {
      store.hidden = new LinkedHashMap<>();
    }
    store.syncState();
    return store;
  }

  public void save() {
    try {
      Files.createDirectories(FILE.getParent());
      try (Writer w = Files.newBufferedWriter(FILE, StandardCharsets.UTF_8)) {
        GSON.toJson(this, w);
      }
    } catch (Exception e) {
      Vanish.log().warn("Vanisher: Liste konnte nicht gespeichert werden: " + e);
    }
  }

  /** Schreibt die Liste in den gemeinsamen Zustand, den die Mixins lesen. */
  private void syncState() {
    VanisherState.HIDDEN.clear();
    for (String key : this.hidden.keySet()) {
      try {
        VanisherState.HIDDEN.add(UUID.fromString(key));
      } catch (IllegalArgumentException ignored) {
      }
    }
  }

  public boolean isHidden(UUID uuid) {
    return uuid != null && this.hidden.containsKey(uuid.toString());
  }

  public void hide(UUID uuid, String name) {
    this.hidden.put(uuid.toString(), name == null ? "?" : name);
    VanisherState.HIDDEN.add(uuid);
    this.save();
  }

  public void show(UUID uuid) {
    this.hidden.remove(uuid.toString());
    VanisherState.HIDDEN.remove(uuid);
    this.save();
  }

  public void showAll() {
    this.hidden.clear();
    VanisherState.HIDDEN.clear();
    this.save();
  }

  /** Aktualisiert den angezeigten Namen, falls die Person sich umbenannt hat. */
  public void updateName(UUID uuid, String name) {
    String key = uuid.toString();
    if (name != null && this.hidden.containsKey(key) && !name.equals(this.hidden.get(key))) {
      this.hidden.put(key, name);
      this.save();
    }
  }

  public List<Map.Entry<String, String>> entries() {
    return new ArrayList<>(this.hidden.entrySet());
  }
}
