package de.ulgu.fkreis.core.vanish.ui;

import de.ulgu.fkreis.core.vanish.L;
import de.ulgu.fkreis.core.vanish.Vanish;
import de.ulgu.fkreis.core.vanish.gfx.G;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import net.labymod.api.client.gui.screen.activity.AutoActivity;
import net.labymod.api.client.render.matrix.Stack;
import net.labymod.api.client.resources.ResourceLocation;

/**
 * Die Schriftrolle der Verbannten (/vanish) im Minecraft-Pixel-Stil.
 *
 * 1. Eine zusammengerollte Schriftrolle mit rotem Wachssiegel erscheint.
 * 2. Klick aufs Siegel: es zerbricht, die Rolle rollt sich nach unten auf.
 * 3. Darauf stehen alle Ausgeblendeten mit Kopf und Namen. Klick auf einen Namen: die Person ist wieder
 *    sichtbar, Kopf und Name blenden sanft aus.
 */
@AutoActivity
public class BanishScrollActivity extends VActivity {

  // ------------------------------------------------------------------ Pixel-Texturen (werden 2x vergroessert)

  private static final ResourceLocation PAPER = tex("paper");   // 64 x 16, senkrecht kachelbar
  private static final ResourceLocation ROLL = tex("roll");     // 72 x 7
  private static final ResourceLocation CLOSED = tex("closed"); // 72 x 12
  private static final ResourceLocation SEAL = tex("seal");     // 10 x 10

  private static ResourceLocation tex(String name) {
    return ResourceLocation.create("fkreis", "textures/vanish/" + name + ".png");
  }

  private static final float K = 3F;                 // GUI-Einheiten pro Texturpixel
  private static final float ROLL_W = 72 * K;
  private static final float ROLL_H = 7 * K;
  private static final float PAPER_W = 64 * K;
  private static final float TILE_H = 16 * K;
  private static final float ROW_H = 22;
  private static final float TITLE_H = 40;
  private static final float FADE_TIME = 0.6F;

  private static final int INK = 0xFF3B2414;
  private static final int INK_SOFT = 0xFF6E4E30;

  // ------------------------------------------------------------------ Zustand

  private enum Phase { CLOSED, BREAKING, UNROLLING, OPEN }

  private final List<Entry> entries = new ArrayList<>();
  private final List<Bit> bits = new ArrayList<>();
  private final Random random = new Random();
  private Phase phase = Phase.CLOSED;
  private float phaseTime;
  private long lastNanos = System.nanoTime();
  private int scrollRow;
  private int visibleRows;

  // Layout (fuer Klicks gemerkt)
  private float cx;
  private float cy;
  private float paperX;
  private float paperY;
  private float listTop;
  private float sealX;
  private float sealY;

  public BanishScrollActivity() {
    for (Map.Entry<String, String> e : Vanish.get().store().entries()) {
      try {
        this.entries.add(new Entry(UUID.fromString(e.getKey()), e.getValue()));
      } catch (IllegalArgumentException ignored) {
      }
    }
  }

  // ================================================================== Zeichnen

  @Override
  protected void draw(Stack s, float mx, float my) {
    long now = System.nanoTime();
    float dt = Math.min(0.1F, (now - this.lastNanos) / 1_000_000_000F);
    this.lastNanos = now;
    this.phaseTime += dt;
    this.cx = this.x + this.w / 2F;
    this.cy = this.y + this.h / 2F;

    // Hintergrund abdunkeln
    G.rect(s, this.x, this.y, this.w, this.h, G.alpha(0xB0080506, G.easeOut(this.age() * 3F)));

    int rows = Math.max(1, this.entries.size());
    this.visibleRows = (int) Math.max(1, Math.min(rows, (this.h - 90 - TITLE_H) / ROW_H));
    float fullH = TITLE_H + this.visibleRows * ROW_H + 14;

    switch (this.phase) {
      case CLOSED:
        this.drawClosed(s, mx, my, false);
        break;
      case BREAKING:
        this.drawClosed(s, mx, my, true);
        if (this.phaseTime > 0.35F) {
          this.next(Phase.UNROLLING);
          G.sound("item.book.page_turn", 0.8F);
        }
        break;
      case UNROLLING: {
        float k = G.easeOut(Math.min(1F, this.phaseTime / 0.6F));
        this.drawOpen(s, mx, my, fullH, Math.max(2, fullH * k), dt, k >= 1F);
        if (k >= 1F) {
          this.next(Phase.OPEN);
        }
        break;
      }
      default:
        this.drawOpen(s, mx, my, fullH, fullH, dt, true);
    }
    this.drawBits(s, dt);
  }

  private void next(Phase p) {
    this.phase = p;
    this.phaseTime = 0;
  }

  /** Zusammengerollte Schriftrolle mit Siegel. Schwebt leicht. */
  private void drawClosed(Stack s, float mx, float my, boolean breaking) {
    float drop = (1 - G.easeBack(Math.min(1F, this.age() / 0.45F))) * -30;
    float bob = (float) Math.sin(this.age() * 2.2F) * 1.5F;
    float w = 72 * K;
    float h = 12 * K;
    float x = this.cx - w / 2F;
    float y = this.cy - h / 2F + drop + bob;
    G.icon(s, G.sprite(CLOSED, 0, 0, 72, 12, 72, 12), x, y, w, h);

    float ss = 10 * K;
    this.sealX = this.cx - ss / 2F;
    this.sealY = y + (h - ss) / 2F;
    if (breaking) {
      // Siegel zerbricht: zwei Haelften fallen auseinander
      float t = this.phaseTime;
      float fall = t * t * 260;
      G.icon(s, G.sprite(SEAL, 0, 0, 5, 10, 10, 10), this.sealX - t * 70, this.sealY + fall, 5 * K, ss);
      G.icon(s, G.sprite(SEAL, 5, 0, 5, 10, 10, 10), this.sealX + 5 * K + t * 70, this.sealY + fall, 5 * K, ss);
      return;
    }
    boolean hover = G.in(mx, my, this.sealX, this.sealY, ss, ss);
    float pulse = hover ? 1.08F + 0.04F * (float) Math.sin(this.age() * 10) : 1F;
    float sw = ss * pulse;
    G.icon(s, G.sprite(SEAL, 0, 0, 10, 10, 10, 10), this.cx - sw / 2F, this.sealY + (ss - sw) / 2F, sw, sw);
    if (hover) {
      G.outline(s, this.cx - sw / 2F - 2, this.sealY + (ss - sw) / 2F - 2, sw + 4, sw + 4, 1, 0x88FFE7A0);
    }
    float a = Math.min(1F, Math.max(0F, (this.age() - 0.5F) * 2));
    G.centered(s, L.t("scroll.seal"), this.cx, y + h + 10, G.alpha(0xFFE8D8B8, a * (0.7F + 0.3F * (float) Math.sin(this.age() * 3))), 1F, true);
  }

  /** Geoeffnete Rolle: obere Rolle fest, Papier dazwischen, untere Rolle rollt nach unten ab. */
  private void drawOpen(Stack s, float mx, float my, float fullH, float paperH, float dt, boolean showContent) {
    this.paperX = this.cx - PAPER_W / 2F;
    this.paperY = this.cy - fullH / 2F;
    this.listTop = this.paperY + TITLE_H;

    // Papier (Kacheln, letzte Kachel abgeschnitten)
    for (float ty = 0; ty < paperH; ty += TILE_H) {
      float th = Math.min(TILE_H, paperH - ty);
      int texH = Math.max(1, (int) Math.ceil(th / K));
      G.icon(s, G.sprite(PAPER, 0, 0, 64, texH, 64, 16), this.paperX, this.paperY + ty, PAPER_W, texH * K);
    }
    if (showContent) {
      this.content(s, mx, my, dt);
    }
    float rx = this.cx - ROLL_W / 2F;
    G.icon(s, G.sprite(ROLL, 0, 0, 72, 7, 72, 7), rx, this.paperY - ROLL_H + 6, ROLL_W, ROLL_H);
    G.icon(s, G.sprite(ROLL, 0, 0, 72, 7, 72, 7), rx, this.paperY + paperH - 6, ROLL_W, ROLL_H);
  }

  private void content(Stack s, float mx, float my, float dt) {
    G.centered(s, L.t("scroll.title"), this.cx, this.paperY + 9, INK, 1.25F, false);
    G.centered(s, L.t("scroll.hint"), this.cx, this.paperY + 24, INK_SOFT, 0.7F, false);
    G.rect(s, this.paperX + 24, this.paperY + 34, PAPER_W - 48, 1, 0x664A2E18);

    if (this.entries.isEmpty()) {
      G.centered(s, L.t("scroll.empty"), this.cx, this.listTop + 6, INK_SOFT, 0.9F, false);
      return;
    }
    this.entries.removeIf(en -> en.fade >= FADE_TIME);
    if (this.entries.isEmpty()) {
      G.centered(s, L.t("scroll.empty"), this.cx, this.listTop + 6, INK_SOFT, 0.9F, false);
      return;
    }
    int max = Math.max(0, this.entries.size() - this.visibleRows);
    this.scrollRow = Math.max(0, Math.min(this.scrollRow, max));
    for (int i = 0; i < this.visibleRows && i + this.scrollRow < this.entries.size(); i++) {
      Entry e = this.entries.get(i + this.scrollRow);
      this.row(s, e, this.paperX + 14, this.listTop + i * ROW_H, PAPER_W - 28, mx, my, dt);
    }
    if (this.scrollRow > 0) {
      G.centered(s, "▲", this.cx, this.listTop - 7, INK_SOFT, 0.7F, false);
    }
    if (this.scrollRow < max) {
      G.centered(s, "▼", this.cx, this.listTop + this.visibleRows * ROW_H, INK_SOFT, 0.7F, false);
    }
  }

  private void row(Stack s, Entry e, float rx, float ry, float rw, float mx, float my, float dt) {
    // Angeklickt: Kopf und Name blenden sanft aus, danach verschwindet die Zeile
    float a = 1F;
    if (e.fade >= 0) {
      e.fade += dt;
      a = Math.max(0F, 1F - e.fade / FADE_TIME);
    }
    boolean hover = e.fade < 0 && G.in(mx, my, rx, ry, rw, ROW_H);
    if (hover) {
      G.rect(s, rx, ry + 1, rw, ROW_H - 2, 0xFFCEB784);
    }
    G.rect(s, rx + 3, ry + 3, 16, 16, G.alpha(0xFF3B2414, a));
    if (a > 0.05F) {
      G.icon(s, G.head(e.uuid), rx + 4, ry + 4, 14, 14);
      if (a < 1F) {
        G.rect(s, rx + 3, ry + 3, 16, 16, G.alpha(0xFFE4D2A5, 1F - a)); // Kopf verblasst ins Papier
      }
    }
    G.text(s, G.fit(e.name, rw - 32, 1F), rx + 26, ry + 7, G.alpha(INK, a), 1F, false);
  }



  // ================================================================== Splitter & Funken

  private void drawBits(Stack s, float dt) {
    for (Iterator<Bit> it = this.bits.iterator(); it.hasNext(); ) {
      Bit b = it.next();
      b.age += dt;
      if (b.age >= b.life) {
        it.remove();
        continue;
      }
      b.x += b.vx * dt;
      b.y += b.vy * dt;
      b.vy += b.g * dt;
      G.rect(s, b.x, b.y, b.size, b.size, G.alpha(b.color, 1 - b.age / b.life));
    }
  }

  // ================================================================== Eingabe

  @Override
  protected boolean onClick(float mx, float my, boolean left) {
    if (!left) {
      return false;
    }
    if (this.phase == Phase.CLOSED) {
      if (G.in(mx, my, this.sealX, this.sealY, 10 * K, 10 * K)) {
        this.next(Phase.BREAKING);
        G.sound("block.decorated_pot.shatter", 1.3F);
        for (int i = 0; i < 18; i++) {
          this.bits.add(Bit.wax(this.cx, this.sealY + 5 * K, this.random));
        }
      }
      return true;
    }
    if (this.phase != Phase.OPEN) {
      return true;
    }
    for (int i = 0; i < this.visibleRows && i + this.scrollRow < this.entries.size(); i++) {
      Entry e = this.entries.get(i + this.scrollRow);
      if (e.fade < 0 && G.in(mx, my, this.paperX + 14, this.listTop + i * ROW_H, PAPER_W - 28, ROW_H)) {
        e.fade = 0;
        Vanish.get().show(e.uuid);
        G.sound("item.book.page_turn", 1.4F);
        return true;
      }
    }
    return false;
  }

  @Override
  protected boolean onScroll(float mx, float my, float delta) {
    this.scrollRow -= (int) Math.signum(delta);
    return true;
  }

  // ================================================================== Hilfsklassen

  private static float hash(float v, int salt) {
    double h = Math.sin(v * 12.9898 + salt * 78.233) * 43758.5453;
    return (float) (h - Math.floor(h));
  }

  private static final class Entry {
    final UUID uuid;
    final String name;
    final int seed;
    float fade = -1;

    Entry(UUID uuid, String name) {
      this.uuid = uuid;
      this.name = name;
      this.seed = (int) (uuid.getLeastSignificantBits() & 0xFF);
    }
  }

  private static final class Bit {
    float x;
    float y;
    float vx;
    float vy;
    float g;
    float age;
    float life;
    float size;
    int color;



    static Bit wax(float x, float y, Random r) {
      Bit b = new Bit();
      b.x = x;
      b.y = y;
      b.vx = (r.nextFloat() - 0.5F) * 160;
      b.vy = -40 - r.nextFloat() * 80;
      b.g = 420;
      b.life = 0.8F;
      b.size = 2 + r.nextInt(2);
      b.color = r.nextBoolean() ? 0xFFB0201E : 0xFF7A0C0E;
      return b;
    }
  }
}
