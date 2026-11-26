package de.ulgu.fkreis.core.loot;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import de.ulgu.fkreis.core.FkreisAddon;
import de.ulgu.fkreis.core.gfx.G;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import net.labymod.api.client.render.matrix.Stack;

/**
 * Taegliche Truhe: einmal am Tag 1 bis 10 Holy-Coins. Die Muenzen gibt es nur im Addon (kein echtes Geld, kein Kauf),
 * man schaltet damit zusaetzliche Hoelzer frei. Gespeichert wird lokal in configs/fkreis/loot.json.
 */
public final class Loot {

  private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
  private static final Path FILE = Paths.get("labymod-neo", "configs", "fkreis", "loot.json");
  private static final Random RANDOM = new Random();
  private static Loot instance;

  public int coins;
  /** Tag (JJJJ-MM-TT), an dem zuletzt geoeffnet wurde. */
  public String lastDay = "";
  public int lastWin;
  /** Mob des Holo-Stickers aus der letzten Truhe, sonst leer. */
  public transient String lastHolo = "";
  /** Chance auf einen Holo-Sticker je Truhe: 0,001 % (1 zu 100.000). */
  public static final double HOLO_CHANCE = 0.00001;
  /** Freigeschaltete Dinge, z. B. "wood:cherry". */
  public List<String> owned = new ArrayList<>();

  public static Loot get() {
    if (instance == null) {
      instance = load();
    }
    return instance;
  }

  private static Loot load() {
    try {
      if (Files.exists(FILE)) {
        try (Reader r = Files.newBufferedReader(FILE, StandardCharsets.UTF_8)) {
          Loot l = GSON.fromJson(r, Loot.class);
          if (l != null) {
            if (l.owned == null) {
              l.owned = new ArrayList<>();
            }
            if (l.lastDay == null) {
              l.lastDay = "";
            }
            l.coins = Math.max(0, l.coins);
            return l;
          }
        }
      }
    } catch (Exception e) {
      FkreisAddon.log().warn("Truhe konnte nicht geladen werden: " + e);
    }
    return new Loot();
  }

  public void save() {
    try {
      Files.createDirectories(FILE.getParent());
      try (Writer w = Files.newBufferedWriter(FILE, StandardCharsets.UTF_8)) {
        GSON.toJson(this, w);
      }
    } catch (Exception e) {
      FkreisAddon.log().warn("Truhe konnte nicht gespeichert werden: " + e);
    }
  }

  public boolean canOpen() {
    return !LocalDate.now().toString().equals(this.lastDay);
  }

  /**
   * Truhe oeffnen, hoechstens einmal am Tag: normalerweise 1 bis 10 Muenzen. Mit sehr viel Glueck liegt stattdessen
   * ein Holo-Sticker darin (dann gibt es keine Muenzen, {@link #lastHolo} nennt den Mob). Liefert false, wenn heute schon offen.
   */
  public boolean open() {
    if (!this.canOpen()) {
      return false;
    }
    this.lastDay = LocalDate.now().toString();
    this.lastHolo = "";
    this.lastWin = 0;
    if (RANDOM.nextDouble() < HOLO_CHANCE) {
      this.lastHolo = MobStickers.random(RANDOM);
      if (!this.owned.contains(MobStickers.HOLO + this.lastHolo)) {
        this.owned.add(MobStickers.HOLO + this.lastHolo);
      }
    } else {
      this.lastWin = 1 + RANDOM.nextInt(10);
      this.coins += this.lastWin;
    }
    this.save();
    return true;
  }

  /** Preis in Muenzen; 0 = von Anfang an frei. */
  public static int price(String id) {
    if (id.startsWith(MobStickers.PREFIX)) {
      return MobStickers.price(id.substring(MobStickers.PREFIX.length()));
    }
    switch (id) {
      case "wood:dark_oak":
        return 10;
      case "wood:jungle":
      case "wood:acacia":
        return 20;
      case "wood:mangrove":
      case "wood:cherry":
        return 25;
      case "wood:spruce":
      case "wood:bamboo":
      case "wood:crimson":
      case "wood:warped":
      case "wood:pale_oak":
        return 30;
      default:
        return 0; // Eiche und Birke sind von Anfang an frei, ebenso alle Blockmatch-Farben
    }
  }

  public boolean owns(String id) {
    return price(id) == 0 || this.owned.contains(id);
  }

  /** Etwas, das schon vor der Truhe benutzt wurde, bleibt ohne Bezahlen erhalten. */
  public void grant(String id) {
    if (!this.owns(id)) {
      this.owned.add(id);
      this.save();
    }
  }

  public boolean buy(String id) {
    int price = price(id);
    if (this.owns(id)) {
      return true;
    }
    if (this.coins < price) {
      return false;
    }
    this.coins -= price;
    this.owned.add(id);
    this.save();
    return true;
  }

  /** Kleine Pixel-Muenze, u = Groesse eines Muenzpixels. */
  public static void coin(Stack s, float x, float y, float u) {
    G.rect(s, x + u, y, 4 * u, 6 * u, 0xFF8A6A10);
    G.rect(s, x, y + u, 6 * u, 4 * u, 0xFF8A6A10);
    G.rect(s, x + u, y + u, 4 * u, 4 * u, 0xFFF2C94C);
    G.rect(s, x + u, y + u, 2 * u, u, 0xFFFFF2A8);
    G.rect(s, x + 3 * u, y + 3 * u, 2 * u, 2 * u, 0xFFD9A520);
  }

  /** Kontostand mit Muenze, rechtsbuendig an (right, y). */
  public static void balance(Stack s, float right, float y) {
    String n = String.valueOf(get().coins);
    float tw = G.width(n);
    G.rect(s, right - tw - 22, y - 3, tw + 22, 14, 0xB0000000);
    coin(s, right - tw - 18, y - 1, 1.5F);
    G.text(s, n, right - tw - 4, y, 0xFFF2C94C, 1F, true);
  }

  /** Schloss-Schleier mit Preis ueber einem noch nicht freigeschalteten Feld. */
  public static void locked(Stack s, float x, float y, float w, float h, int price) {
    G.rect(s, x, y, w, h, 0xA0000000);
    String n = String.valueOf(price);
    float tw = G.width(n) * 1.2F;
    float sx = x + (w - tw - 14) / 2F;
    coin(s, sx, y + h / 2F - 6, 2F);
    G.text(s, n, sx + 15, y + h / 2F - 4, get().coins >= price ? 0xFFF2C94C : 0xFFE06060, 1.2F, true);
  }
}
