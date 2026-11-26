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
 * Taegliches Gluecksrad: einmal am Tag 1 bis 10 Holy-Coins. Die Muenzen gibt es nur im Addon (kein echtes Geld, kein Kauf),
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
  /** So oft glitzert an einem Tag ueberhaupt ein Feld: 5 %. Wer dann genau dieses Feld trifft (1 zu 10), bekommt den Holo-Sticker. */
  public static final double GLITTER_CHANCE = 0.05;
  /** Einmal gewuerfelt und gespeichert; daraus und aus dem Datum ergibt sich das Glitzerfeld des Tages. */
  public long seed;
  /** Stand der Sticker-Sammlung; aeltere Staende (Testphase) werden beim Laden einmal geleert. */
  public String stickerSet = "";
  private static final String STICKER_SET = "2";
  /** Wie oft schon gedreht wurde; damit wuerfelt jede Drehung ihr Glitzerfeld neu. */
  public int spinCount;
  /** Freigeschaltete Dinge, z. B. "wood:cherry". */
  public List<String> owned = new ArrayList<>();
  /** Holz hinter dem Gluecksrad (wie bei den anderen Menues waehlbar und freischaltbar). */
  public String background = "oak";
  /** NUR ZUM VERGLEICHEN: Sticker zeigen statt der Koepfe die Spawn-Eier mit einer Aura in der Farbe ihrer Stufe. */

  /** Eigene Reihenfolge der Sticker im Sammelbuch: "p:<uuid>" fuer Spieler-Sticker, "mob:<id>" (Holo mit *) fuer Mob-Sticker. */
  public List<String> order = new ArrayList<>();

  /**
   * Bringt alle vorhandenen Sticker in die gespeicherte Reihenfolge. Neue Spieler-Sticker ruecken vor den ersten
   * Mob-Sticker (die Mobs wandern eins nach hinten), neue Mob-Sticker kommen ans Ende.
   */
  public List<String> arrange(List<String> players, List<String> mobs) {
    List<String> out = new ArrayList<>();
    for (String key : this.order) {
      if ((players.contains(key) || mobs.contains(key)) && !out.contains(key)) {
        out.add(key);
      }
    }
    for (String key : players) {
      if (!out.contains(key)) {
        int at = out.size();
        for (int i = 0; i < out.size(); i++) {
          if (out.get(i).startsWith(MobStickers.PREFIX)) {
            at = i;
            break;
          }
        }
        out.add(at, key);
      }
    }
    for (String key : mobs) {
      if (!out.contains(key)) {
        out.add(key);
      }
    }
    if (!out.equals(this.order)) {
      this.order = new ArrayList<>(out);
      this.save();
    }
    return out;
  }

  /** Verschiebt einen Sticker an eine andere Stelle im Sammelbuch. */
  public void move(int from, int to) {
    if (from < 0 || from >= this.order.size() || to < 0 || from == to) {
      return;
    }
    String key = this.order.remove(from);
    this.order.add(Math.min(to, this.order.size()), key);
    this.save();
  }

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
            if (l.background == null) {
              l.background = "oak";
            }
            if (l.order == null) {
              l.order = new ArrayList<>();
            }
            if (l.lastDay == null) {
              l.lastDay = "";
            }
            l.coins = Math.max(0, l.coins);
            if (!STICKER_SET.equals(l.stickerSet)) { // Mob-Sticker aus der Testphase einmal entfernen
              l.stickerSet = STICKER_SET;
              l.owned.removeIf(id -> id.startsWith("mob:") || id.startsWith("holo:"));
              l.order.removeIf(id -> id.startsWith("mob:"));
              l.save();
            }
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
   * An seltenen Tagen glitzert eines der zehn Felder. Liefert dessen Zahl (1 bis 10) oder 0, wenn heute keines glitzert.
   * Haengt nur am Datum und an einer einmal gewuerfelten, gespeicherten Zahl, laesst sich also nicht durch erneutes Oeffnen neu wuerfeln.
   */
  public int glitterNumber() {
    if (this.seed == 0L) {
      this.seed = RANDOM.nextLong() | 1L;
      this.save();
    }
    Random day = new Random(this.seed ^ ((LocalDate.now().toEpochDay() * 1000003L + this.spinCount) * 0x9E3779B97F4A7C15L));
    return day.nextDouble() < GLITTER_CHANCE ? 1 + day.nextInt(10) : 0;
  }

  /**
   * Gluecksrad drehen, hoechstens einmal am Tag: 1 bis 10 Muenzen. Landet man auf dem Glitzerfeld des Tages, gibt es
   * zusaetzlich einen Holo-Sticker eines zufaelligen Mobs ({@link #lastHolo}). Liefert false, wenn heute schon gedreht wurde.
   */
  public boolean open() {
    if (!this.canOpen()) {
      return false;
    }
    int glitter = this.glitterNumber();
    this.spinCount++;
    this.lastDay = LocalDate.now().toString();
    this.lastWin = 1 + RANDOM.nextInt(10);
    this.coins += this.lastWin;
    this.lastHolo = "";
    if (glitter == this.lastWin) {
      this.lastHolo = MobStickers.random(RANDOM);
      if (!this.owned.contains(MobStickers.HOLO + this.lastHolo)) {
        this.owned.add(MobStickers.HOLO + this.lastHolo);
      }
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
    if (coinIcon == null) {
      coinIcon = net.labymod.api.client.gui.icon.Icon.texture(
          net.labymod.api.client.resources.ResourceLocation.create("fkreis", "textures/ui/holy_coin.png"));
    }
    G.icon(s, coinIcon, x - u, y - u, 8 * u, 8 * u); // gepraegte Muenze im Stil der Freundschafts-Muenzen
  }

  private static net.labymod.api.client.gui.icon.Icon coinIcon;

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
