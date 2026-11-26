package de.ulgu.fkreis.core.match;

import de.ulgu.fkreis.core.FkreisAddon;
import de.ulgu.fkreis.core.L;
import de.ulgu.fkreis.core.gfx.Backdrop;
import de.ulgu.fkreis.core.gfx.G;
import de.ulgu.fkreis.core.net.Net;
import de.ulgu.fkreis.core.ui.FkActivity;
import de.ulgu.fkreis.core.ui.Screens;
import java.util.UUID;
import net.labymod.api.Laby;
import net.labymod.api.client.gui.screen.activity.AutoActivity;
import net.labymod.api.client.gui.screen.key.Key;
import net.labymod.api.client.render.matrix.Stack;

/**
 * Ueber das Freundeskreis-Rad: Wie gut passt dieser Spieler zu mir? Die beiden Koepfe wandern aufeinander zu
 * und wieder weg; das Pixel-Herz dazwischen fuellt sich bis zur Prozentzahl. Gezeigt wird nur die Prozentzahl.
 */
@AutoActivity
public class MatchCompareActivity extends FkActivity {

  private static final long SUSPENSE_MS = 2600L, TIMEOUT_MS = 7000L;

  private final UUID other;
  private final String name;
  private final MatchData data = MatchData.get();
  private final boolean ready;
  private long revealAt;
  private boolean dinged;

  public MatchCompareActivity(UUID other, String name) {
    this.other = other;
    this.name = name;
    this.ready = this.data.submitted;
    if (this.ready) {
      FkreisAddon.get().net().matchShare().compare(FkreisAddon.get().net(), other);
    }
    G.click();
  }

  private float[] btn() {
    return new float[] {this.x + this.w / 2F - 70, this.y + this.h / 2F + 78, 140, 18};
  }

  @Override
  protected void draw(Stack s, float mx, float my) {
    MatchMusic.keepPlaying();
    int[] c = MatchActivity.colors(this.data);
    boolean light = c[0] != 0;
    if (light) {
      for (int i = 0; i < 12; i++) {
        G.rect(s, this.x, this.y + this.h * i / 12F, this.w, this.h / 12F + 1, G.mix(c[0], c[1], i / 11F));
      }
    } else {
      Backdrop.draw(s, this.data.background, this.x, this.y, this.w, this.h);
    }
    this.header(s, L.t("match.title"), mx, my);
    float cx = this.x + this.w / 2F, cy = this.y + this.h / 2F - 10;
    float cw = 330, ch = 200;
    G.rect(s, cx - cw / 2F + 4, cy - ch / 2F + 5, cw, ch, 0x70000000);
    G.rect(s, cx - cw / 2F, cy - ch / 2F, cw, ch, c[2]);
    G.outline(s, cx - cw / 2F, cy - ch / 2F, cw, ch, 1, light ? c[4] : 0xFF000000);
    G.rect(s, cx - cw / 2F, cy - ch / 2F, cw, 3, c[3]);

    long age = System.currentTimeMillis() - this.openedAt;
    int pct = this.ready ? FkreisAddon.get().net().matchShare().compared(this.other) : -1;
    if (pct >= 0 && this.revealAt == 0 && age >= SUSPENSE_MS) {
      this.revealAt = System.currentTimeMillis();
    }
    boolean failed = !this.ready || (pct < 0 && age > TIMEOUT_MS);
    float swing = 88;
    float hs = 48, hy = cy - 52;
    G.rect(s, cx - swing - hs / 2F - 2, hy - 2, hs + 4, hs + 4, c[3]);
    G.rect(s, cx + swing - hs / 2F - 2, hy - 2, hs + 4, hs + 4, c[3]);
    G.icon(s, G.head(Net.self()), cx - swing - hs / 2F, hy, hs, hs);
    G.icon(s, G.head(this.other), cx + swing - hs / 2F, hy, hs, hs);
    G.centered(s, G.fit(Laby.labyAPI().getName(), 110, 0.9F), cx - 100, hy + hs + 6, c[5], 0.9F, false);
    G.centered(s, G.fit(this.name, 110, 0.9F), cx + 100, hy + hs + 6, c[5], 0.9F, false);

    // Herz in der Mitte: steht still und zeigt am Ende nur die Prozentzahl
    float fill = 0;
    String inside = "";
    String line;
    boolean loading = false;
    if (!this.ready) {
      inside = "?";
      line = L.t("match.cmpFirst");
    } else if (this.revealAt > 0) {
      fill = pct / 100F;
      inside = pct + "%";
      if (!this.dinged) {
        this.dinged = true;
        G.sound("entity.player.levelup", pct >= 75 ? 1.6F : 1.1F);
      }
      line = L.t(pct >= 80 ? "match.cmpHigh" : pct >= 60 ? "match.cmpMid" : "match.cmpLow");
    } else if (failed) {
      inside = "?";
      line = L.t("match.cmpNone", this.name);
    } else {
      loading = true;
      line = L.t("match.cmpWait");
    }
    this.heart(s, cx, hy + hs / 2F, 4.2F, fill, c, light);
    if (!inside.isEmpty()) {
      G.centered(s, inside, cx, hy + hs / 2F - 9, 0xFFFFFFFF, 1.5F, true);
    }
    float y = cy + 30;
    for (String l : G.wrap(line, cw - 30, 1F)) {
      G.centered(s, l, cx, y, c[5], 1F, false);
      y += 11;
    }
    if (loading) { // Ladebalken aus Bloecken, ueber den ein Lichtstreifen laeuft
      float done = Math.min(pct >= 0 ? 1F : 0.92F, age / (float) SUSPENSE_MS);
      int blocks = 20;
      float bw = 9, gap = 2, total = blocks * bw + (blocks - 1) * gap, bx = cx - total / 2F, by = y + 6, bh = 10;
      G.rect(s, bx - 3, by - 3, total + 6, bh + 6, light ? c[4] : 0xFF000000);
      G.rect(s, bx - 2, by - 2, total + 4, bh + 4, light ? 0xFFFFFFFF : 0xFF14161C);
      float sweep = (age % 900) / 900F * (blocks + 4) - 2;
      for (int i = 0; i < blocks; i++) {
        float x = bx + i * (bw + gap);
        boolean on = i < Math.round(done * blocks);
        int col = on ? c[3] : light ? G.shade(c[7], 0.92F) : 0xFF2A2E38;
        if (on) {
          float glow = Math.max(0F, 1F - Math.abs(i - sweep) / 2F);
          col = G.mix(col, 0xFFFFFFFF, 0.55F * glow);
        }
        float pop = on && i == Math.round(done * blocks) - 1 ? 1.5F : 0F; // der neueste Block ragt kurz heraus
        G.rect(s, x, by - pop, bw, bh + pop * 2, col);
        if (on) {
          G.rect(s, x, by + bh - 2 + pop, bw, 2, c[4]);
        }
      }
      G.centered(s, Math.round(done * 100) + " %", cx, by + bh + 7, c[6], 0.85F, false);
    }
    float[] b = this.btn();
    boolean over = G.in(mx, my, b[0], b[1], b[2], b[3]);
    G.rect(s, b[0], b[1], b[2], b[3], over ? G.shade(c[3], 1.12F) : c[3]);
    G.centered(s, L.t(this.ready ? "match.cmpClose" : "match.cmpOpen"), b[0] + b[2] / 2F, b[1] + 5, light ? 0xFFFFFFFF : 0xFF1A1400, 0.95F, false);
  }

  private static final String[] HEART = {
      "0011100011100", "0111110111110", "1111111111111", "1111111111111", "1111111111111", "1111111111111",
      "0111111111110", "0011111111100", "0001111111000", "0000111110000", "0000011100000", "0000001000000"};

  /** Pixel-Herz als Anzeige: u = Bildschirmpixel je Herzpixel, fill 0..1 = Fuellstand von unten. */
  private void heart(Stack s, float cx, float cy, float u, float fill, int[] c, boolean light) {
    int rows = HEART.length, cols = 13;
    float x0 = cx - cols * u / 2F, y0 = cy - rows * u / 2F;
    int empty = light ? G.shade(c[7], 0.9F) : 0xFF3A3E48, full = 0xFFE23B4E, edge = 0xFF7A1424;
    float level = rows * (1F - fill);
    for (int y = 0; y < rows; y++) {
      for (int x = 0; x < cols; x++) {
        if (HEART[y].charAt(x) != '1') {
          continue;
        }
        boolean border = y == 0 || x == 0 || x == cols - 1 || y == rows - 1 || HEART[y - 1].charAt(x) != '1' || HEART[y + 1].charAt(x) != '1'
            || HEART[y].charAt(x - 1) != '1' || HEART[y].charAt(x + 1) != '1';
        int col = border ? edge : y + 0.5F >= level ? full : empty;
        G.rect(s, x0 + x * u, y0 + y * u, u + 0.5F, u + 0.5F, col);
      }
    }
    G.rect(s, x0 + 2 * u, y0 + 2 * u, u, u * 2, 0x66FFFFFF); // Glanz
  }

  @Override
  protected boolean onClick(float mx, float my, boolean left) {
    float[] b = this.btn();
    if (this.headerHit(mx, my) || (this.ready && G.in(mx, my, b[0], b[1], b[2], b[3]))) {
      Screens.close();
    } else if (G.in(mx, my, b[0], b[1], b[2], b[3])) {
      Screens.now(new MatchActivity());
    }
    return true;
  }

  @Override
  public void onCloseScreen() {
    super.onCloseScreen();
    MatchMusic.stop();
  }

  @Override
  protected boolean onKey(Key key) {
    if (key == Key.ESCAPE) {
      Screens.close();
      return true;
    }
    return false;
  }
}
