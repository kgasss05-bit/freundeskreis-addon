package de.ulgu.fkreis.core.match;

import de.ulgu.fkreis.core.FkreisAddon;
import de.ulgu.fkreis.core.L;
import de.ulgu.fkreis.core.gfx.Backdrop;
import de.ulgu.fkreis.core.gfx.G;
import de.ulgu.fkreis.core.loot.Loot;
import de.ulgu.fkreis.core.net.MatchShare;
import de.ulgu.fkreis.core.ui.FkActivity;
import de.ulgu.fkreis.core.ui.Screens;
import java.util.List;
import net.labymod.api.client.gui.screen.activity.AutoActivity;
import net.labymod.api.client.gui.screen.key.Key;
import net.labymod.api.client.render.matrix.Stack;
import net.labymod.api.client.resources.ResourceLocation;

/**
 * Freunde-Match: erst der Fragebogen (eine Frage nach der anderen), danach die Suche nach Spielern auf dem Server,
 * die aehnlich ticken. Es geht um Freundschaft; gezeigt wird nur die Uebereinstimmung in Prozent.
 */
@AutoActivity
public class MatchActivity extends FkActivity {

  /** Farbwelten: Hintergrund oben/unten, Karte, Akzent, dunkler Akzent, Text, blasser Text, Knopf, Knopf unter der Maus. */
  private static final String[] THEMES = {"pink", "blue", "mint", "lilac", "classic", "peach", "lemon"};
  private static final int[][] COLORS = {
      {0xFFFFD6E7, 0xFFFFBFD8, 0xFFFFF4F8, 0xFFE8528F, 0xFFB03468, 0xFF4A2038, 0xFF9A6F84, 0xFFFFD9E8, 0xFFFFC2DA},
      {0xFFD6ECFF, 0xFFBBDDFF, 0xFFF4FAFF, 0xFF3A8FD8, 0xFF215A94, 0xFF1C3550, 0xFF6F8AA6, 0xFFD9ECFF, 0xFFBFE0FF},
      {0xFFD9F5E6, 0xFFBDE8D2, 0xFFF4FFF9, 0xFF2E9A6B, 0xFF1F6F4C, 0xFF173B2C, 0xFF6E9684, 0xFFD4F2E2, 0xFFBDE8D2},
      {0xFFE9DEFF, 0xFFD3C2FF, 0xFFF9F5FF, 0xFF8E5BD0, 0xFF5F3A94, 0xFF2E1F4A, 0xFF8A7AA6, 0xFFE4D9FF, 0xFFD3C2FF},
      {0, 0, 0xF21C1F28, 0xFFF2C94C, 0xFF8A6A10, 0xFFFFFFFF, 0xFF9AA3B5, 0xFF2F3440, 0xFF4A4F5C},
      {0xFFFFE3D1, 0xFFFFCFB3, 0xFFFFF7F1, 0xFFE8793F, 0xFFA9501F, 0xFF4A2A18, 0xFF9A7A68, 0xFFFFE2CF, 0xFFFFCFB3},
      {0xFFFFF6C9, 0xFFFFEC9E, 0xFFFFFDF0, 0xFFD9A400, 0xFF8F6C00, 0xFF403300, 0xFF8F8660, 0xFFFFF3BF, 0xFFFFEC9E}};

  private final MatchData data = MatchData.get();
  private boolean choosing;
  private int[] c = COLORS[4];
  private int at;
  private boolean quiz;
  private float cx, cy, cw, ch;
  private float shownProgress;
  private long resetAskAt, sendAskAt;
  /** Wann die Frage gewechselt hat (zum Hereingleiten) und wann zuletzt ein Feld der Leiste voll wurde (zum Aufblitzen). */
  private long turnedAt, levelAt;
  private int shownSlot = -1;
  /** true, wenn der Fragebogen schon fertig ist und man nur einzelne Antworten durchsieht oder aendert. */
  private boolean review;

  /** Aus den Einstellungen: nur die Farbwelt aussuchen. */
  public MatchActivity(boolean chooseTheme) {
    this();
    this.choosing = chooseTheme || this.choosing;
  }

  public MatchActivity() {
    this.choosing = this.data.theme.isEmpty();
    if (!this.data.theme.isEmpty()) {
      Loot.get().grant("theme:" + this.data.theme); // schon gewaehlte Farbe bleibt frei
    }
    int open = this.data.firstOpen();
    this.quiz = !this.data.submitted && open >= 0;
    this.at = Math.max(0, open);
    if (this.data.submitted && !this.choosing) {
      this.search();
    }
    G.click();
  }

  /** Farben der gewaehlten Farbwelt (fuer die anderen Blockmatch-Bildschirme). */
  static int[] colors(MatchData data) {
    for (int i = 0; i < THEMES.length; i++) {
      if (THEMES[i].equals(data.theme)) {
        return COLORS[i];
      }
    }
    return COLORS[4];
  }

  private int themeIndex() {
    for (int i = 0; i < THEMES.length; i++) {
      if (THEMES[i].equals(this.data.theme)) {
        return i;
      }
    }
    return 4;
  }

  private boolean light() {
    return this.themeIndex() != 4;
  }

  private float[] swatch(int i) {
    float gap = 8, sw = Math.min(62, (this.cw - 32 - (THEMES.length - 1) * gap) / THEMES.length), total = THEMES.length * sw + (THEMES.length - 1) * gap;
    return new float[] {this.cx + (this.cw - total) / 2F + i * (sw + gap), this.cy + 84, sw, 62};
  }

  private void search() {
    FkreisAddon.get().net().matchShare().search(FkreisAddon.get().net());
  }

  private void layout() {
    this.cw = Math.min(this.w - 30, 430);
    this.ch = Math.min(this.h - 44, 262);
    this.cx = this.x + (this.w - this.cw) / 2F;
    this.cy = this.y + 30 + (this.h - 36 - this.ch) / 2F;
  }

  private float[] option(int i, int count) {
    float bh = 19, gap = 4, total = count * bh + (count - 1) * gap;
    float top = this.cy + this.ch - 34 - total;
    return new float[] {this.cx + 16, top + i * (bh + gap), this.cw - 32, bh};
  }

  private static final ResourceLocation XP_BACK = ResourceLocation.create("minecraft", "textures/gui/sprites/hud/experience_bar_background.png");
  private static final ResourceLocation XP_FILL = ResourceLocation.create("minecraft", "textures/gui/sprites/hud/experience_bar_progress.png");

  /**
   * Fortschritt als Erfahrungsleiste aus den Texturen des Spiels (182 x 5 Pixel), umgebaut auf zehn Felder:
   * jedes Feld ist ein 10 Pixel breites Stueck der Leiste und steht fuer zehn Fragen, also ein Pixel je Frage.
   */
  private void xpBar(Stack s, float progress) {
    float u = Math.max(1F, Math.min(3F, (this.cw - 32) / 101F)), bw = 101 * u, bh = 5 * u, seg = 10 * u;
    float bx = this.cx + (this.cw - bw) / 2F, by = this.cy + 30 - bh;
    long now = System.currentTimeMillis();
    int filled = Math.round(progress * 100F);
    for (int i = 0; i < 10; i++) {
      int sx = i == 0 ? 0 : i == 9 ? 171 : 81, sw = i == 9 ? 11 : 10;
      float x = bx + i * seg;
      G.icon(s, G.sprite(XP_BACK, sx, 0, sw, 5, 182, 5), x, by, sw * u, bh);
      int k = Math.max(0, Math.min(10, filled - i * 10));
      if (k > 0) {
        int fw = k == 10 ? sw : k;
        G.icon(s, G.sprite(XP_FILL, sx, 0, fw, 5, 182, 5), x, by, fw * u, bh);
      }
    }
    int level = (int) (progress * 10F + 0.001F);
    long flash = now - this.levelAt;
    if (flash < 700 && level > 0) { // Feld gerade voll geworden
      float a = 1F - flash / 700F;
      G.rect(s, bx + (level - 1) * seg - 2, by - 3, seg + 3, bh + 6, G.alpha(0xFFFFFFFF, 0.7F * a));
    }
    String n = String.valueOf(level);
    float sc = flash < 700 ? 1.3F + 0.5F * (1F - flash / 700F) : 1.3F, nx = this.cx + this.cw / 2F - G.width(n) * sc / 2F, ny = by - 2 - 8 * sc;
    for (int d = 0; d < 4; d++) {
      G.text(s, n, nx + (d == 0 ? -1 : d == 1 ? 1 : 0), ny + (d == 2 ? -1 : d == 3 ? 1 : 0), 0xFF000000, sc, false);
    }
    G.text(s, n, nx, ny, 0xFF80FF20, sc, false);
  }

  private float[] foot(int i) { // 0 links, 1 rechts, 2 Mitte
    float bw = Math.min(120, (this.cw - 44) / 3F);
    float bx = i == 0 ? this.cx + 16 : i == 1 ? this.cx + this.cw - 16 - bw : this.cx + (this.cw - bw) / 2F;
    return new float[] {bx, this.cy + this.ch - 26, bw, 17};
  }

  private void button(Stack s, float[] r, String label, float mx, float my, boolean gold, boolean chosen) {
    boolean over = G.in(mx, my, r[0], r[1], r[2], r[3]);
    boolean strong = chosen || gold;
    int bg = strong ? (over ? G.shade(this.c[3], 1.12F) : this.c[3]) : over ? this.c[8] : this.c[7];
    G.rect(s, r[0], r[1], r[2], r[3], bg);
    G.rect(s, r[0], r[1] + r[3] - 2, r[2], 2, strong ? this.c[4] : G.shade(this.c[7], this.light() ? 0.86F : 0.5F));
    int ink = strong ? (this.light() ? 0xFFFFFFFF : 0xFF1A1400) : this.c[5];
    G.centered(s, G.fit(label, r[2] - 8, 0.95F), r[0] + r[2] / 2F, r[1] + 5, ink, 0.95F, false);
  }

  private void card(Stack s) {
    G.rect(s, this.cx + 4, this.cy + 5, this.cw, this.ch, 0x70000000);
    G.rect(s, this.cx, this.cy, this.cw, this.ch, this.c[2]);
    G.outline(s, this.cx, this.cy, this.cw, this.ch, 1, this.light() ? this.c[4] : 0xFF000000);
    G.rect(s, this.cx, this.cy, this.cw, 3, this.c[3]);
  }

  @Override
  protected void draw(Stack s, float mx, float my) {
    this.layout();
    MatchMusic.keepPlaying();
    this.c = COLORS[this.themeIndex()];
    if (this.light()) { // helle Farbwelt: sanfter Verlauf mit kleinen Pixel-Funkeln
      int bands = 12;
      for (int i = 0; i < bands; i++) {
        G.rect(s, this.x, this.y + this.h * i / bands, this.w, this.h / bands + 1, G.mix(this.c[0], this.c[1], i / (float) (bands - 1)));
      }
      long t = System.currentTimeMillis();
      for (int i = 0; i < 40; i++) {
        float px = this.x + ((i * 9301 + 49297) % 233280) / 233280F * this.w, py = this.y + ((i * 4253 + 1237) % 7919) / 7919F * this.h;
        float tw = (float) Math.abs(Math.sin(t / 900.0 + i));
        G.rect(s, px, py, 2, 2, G.alpha(0xFFFFFFFF, 0.25F + 0.6F * tw));
        if (i % 4 == 0) {
          G.rect(s, px - 2, py, 6, 2, G.alpha(0xFFFFFFFF, 0.3F * tw));
          G.rect(s, px, py - 2, 2, 6, G.alpha(0xFFFFFFFF, 0.3F * tw));
        }
      }
    } else {
      Backdrop.draw(s, this.data.background, this.x, this.y, this.w, this.h);
    }
    this.header(s, L.t("match.title"), mx, my);
    this.card(s);
    if (this.choosing) {
      G.centered(s, L.t("match.themeTitle"), this.cx + this.cw / 2F, this.cy + 30, this.c[3], 1.4F, false);
      G.centered(s, L.t("match.themeHint"), this.cx + this.cw / 2F, this.cy + 52, this.c[6], 0.9F, false);
      for (int i = 0; i < THEMES.length; i++) {
        float[] r = this.swatch(i);
        boolean over = G.in(mx, my, r[0], r[1], r[2], r[3]), sel = i == this.themeIndex() && !this.data.theme.isEmpty();
        G.rect(s, r[0], r[1], r[2], r[3], i == 4 ? 0xFF1C1F28 : COLORS[i][1]);
        G.rect(s, r[0] + 8, r[1] + 10, r[2] - 16, 14, COLORS[i][2]);
        G.rect(s, r[0] + 8, r[1] + 28, r[2] - 16, 9, COLORS[i][3]);
        G.rect(s, r[0] + 8, r[1] + 41, r[2] - 16, 9, COLORS[i][7]);
        G.outline(s, r[0], r[1], r[2], r[3], sel || over ? 2 : 1, sel ? this.c[3] : over ? this.c[5] : this.c[6]);
        if (!Loot.get().owns("theme:" + THEMES[i])) {
          Loot.locked(s, r[0], r[1], r[2], r[3], Loot.price("theme:" + THEMES[i]));
        }
        G.centered(s, L.t("match.theme" + i), r[0] + r[2] / 2F, r[1] + r[3] + 5, this.c[5], 0.85F, false);
      }
      this.button(s, this.foot(1), L.t("match.themeDone"), mx, my, true, false);
    } else if (this.quiz) {
      this.drawQuiz(s, mx, my);
    } else {
      this.drawResult(s, mx, my);
    }
  }

  private void drawQuiz(Stack s, float mx, float my) {
    int slot = this.at, count = MatchData.options(slot);
    boolean fun = MatchData.isFun(slot);
    int nr = MatchData.number(slot);
    this.shownProgress = G.lerp(this.shownProgress, this.data.answered() / (float) MatchData.TOTAL, 0.12F);
    this.xpBar(s, this.shownProgress);
    G.text(s, L.t("match.count", slot + 1, MatchData.TOTAL), this.cx + 16, this.cy + 37, this.c[6], 0.8F, false);
    String topic = (fun ? L.t("match.topicFun") : L.t("match.topic" + nr / MatchData.PER_TOPIC)).toUpperCase();
    float tw = G.width(topic) * 0.8F;
    G.rect(s, this.cx + this.cw - 22 - tw, this.cy + 34, tw + 6, 12, fun ? this.c[4] : this.c[3]);
    G.text(s, topic, this.cx + this.cw - 19 - tw, this.cy + 37, this.light() ? 0xFFFFFFFF : 0xFF1A1400, 0.8F, false);
    // Frage: gleitet beim Wechsel von rechts herein
    if (this.shownSlot != slot) {
      this.shownSlot = slot;
      this.turnedAt = System.currentTimeMillis();
    }
    float slide = (1F - G.easeOut(Math.min(1F, (System.currentTimeMillis() - this.turnedAt) / 220F))) * 26F;
    String q = L.t(fun ? "match.fun" + nr : "match.st" + nr);
    List<String> lines = G.wrap(q, this.cw - 52, 1.25F);
    float top = this.option(0, count)[1], y = this.cy + 52 + Math.max(0F, (top - this.cy - 60 - lines.size() * 14) / 2F);
    G.rect(s, this.cx + 16 + slide, y - 2, 3, lines.size() * 14, this.c[3]);
    for (String line : lines) {
      G.text(s, line, this.cx + 26 + slide, y, this.c[5], 1.25F, !this.light());
      y += 14;
    }
    // Antworten
    for (int i = 0; i < count; i++) {
      String label = fun ? L.t("match.fun" + nr + "o" + i) : L.t("match.scale" + i);
      float[] r = this.option(i, count);
      this.button(s, r, label, mx, my, false, this.data.answers[slot] == i);
      if (fun) {
        G.rect(s, r[0], r[1], r[3], r[3], this.c[4]);
        G.centered(s, String.valueOf((char) ('A' + i)), r[0] + r[3] / 2F, r[1] + 5, 0xFFFFFFFF, 1F, false);
      }
    }
    if (this.at > 0) {
      this.button(s, this.foot(0), L.t("match.back"), mx, my, false, false);
    }
    if (this.review) {
      this.button(s, this.foot(2), L.t("match.toResult"), mx, my, true, false);
      if (this.at < MatchData.TOTAL - 1) {
        this.button(s, this.foot(1), L.t("match.next"), mx, my, false, false);
      }
    }

  }

  /** Alles beantwortet, aber noch nicht abgesendet: durchsehen, zuruecksetzen oder endgueltig absenden. */
  private void drawConfirm(Stack s, float mx, float my) {
    this.shownProgress = G.lerp(this.shownProgress, 1F, 0.12F);
    this.xpBar(s, this.shownProgress);
    G.text(s, L.t("match.doneTitle"), this.cx + 16, this.cy + 42, this.c[3], 1.2F, !this.light());
    float y = this.cy + 62;
    boolean sureSend = System.currentTimeMillis() - this.sendAskAt < 5000;
    for (String line : G.wrap(L.t(sureSend ? "match.sendWarn" : "match.doneText"), this.cw - 36, 1F)) {
      G.text(s, line, this.cx + 18, y, sureSend ? this.c[4] : this.c[5], 1F, false);
      y += 11;
    }
    int wait = this.data.daysUntilReset();
    boolean sure = wait == 0 && System.currentTimeMillis() - this.resetAskAt < 4000;
    if (wait > 0) {
      float[] r = this.foot(0);
      G.rect(s, r[0], r[1], r[2], r[3], G.alpha(this.c[7], 0.6F));
      G.centered(s, G.fit(L.t(wait == 1 ? "match.resetWait1" : "match.resetWait", wait), r[2] - 6, 0.85F), r[0] + r[2] / 2F, r[1] + 5, this.c[6], 0.85F, false);
    } else {
      this.button(s, this.foot(0), L.t(sure ? "match.resetSure" : "match.reset"), mx, my, false, sure);
    }
    this.button(s, this.foot(2), L.t("match.review"), mx, my, false, false);
    this.button(s, this.foot(1), L.t(sureSend ? "match.sendSure" : "match.send"), mx, my, true, false);
  }

  private static final String[] HEART = {
      "0011100011100", "0111110111110", "1111111111111", "1111111111111", "1111111111111", "1111111111111",
      "0111111111110", "0011111111100", "0001111111000", "0000111110000", "0000011100000", "0000001000000"};

  /** Pixel-Herz, von unten her bis {@code fill} gefuellt; u = Bildschirmpixel je Herzpixel. */
  private void heart(Stack s, float hx, float hy, float u, float fill) {
    int rows = HEART.length, cols = 13;
    float x0 = hx - cols * u / 2F, y0 = hy - rows * u / 2F, level = rows * (1F - fill);
    int empty = this.light() ? G.shade(this.c[7], 0.9F) : 0xFF3A3E48;
    for (int y = 0; y < rows; y++) {
      for (int x = 0; x < cols; x++) {
        if (HEART[y].charAt(x) != '1') {
          continue;
        }
        boolean border = y == 0 || x == 0 || x == cols - 1 || y == rows - 1 || HEART[y - 1].charAt(x) != '1' || HEART[y + 1].charAt(x) != '1'
            || HEART[y].charAt(x - 1) != '1' || HEART[y].charAt(x + 1) != '1';
        G.rect(s, x0 + x * u, y0 + y * u, u + 0.4F, u + 0.4F, border ? 0xFF7A1424 : y + 0.5F >= level ? 0xFFE23B4E : empty);
      }
    }
  }

  private void drawResult(Stack s, float mx, float my) {
    if (!this.data.submitted) {
      this.drawConfirm(s, mx, my);
      return;
    }
    MatchShare share = FkreisAddon.get().net().matchShare();
    List<MatchShare.Hit> hits = share.hits();
    long now = System.currentTimeMillis(), since = now - share.searchedAt();
    float mid = this.cx + this.cw / 2F, hy = this.cy + this.ch * 0.36F, hs = Math.min(56F, this.ch * 0.24F);
    java.util.UUID me = de.ulgu.fkreis.core.net.Net.self();
    G.centered(s, L.t("match.resultTitle"), mid, this.cy + 16, this.c[3], 1.4F, !this.light());
    if (hits.isEmpty() && since < 4000) {
      // Suche: der eigene Kopf in der Mitte sendet Ringe aus, darunter laeuft ein Balken aus Bloecken
      for (int k = 0; k < 3; k++) {
        float e = ((now / 1400F) + k / 3F) % 1F, rr = hs * 0.6F + e * hs * 1.5F;
        G.outline(s, mid - rr, hy - rr, rr * 2, rr * 2, 2, G.alpha(this.c[3], 0.75F * (1F - e)));
      }
      for (int k = 0; k < 6; k++) { // rundherum blitzen moegliche Treffer auf
        double a = k * Math.PI / 3 + now / 2600.0;
        float e = (float) Math.abs(Math.sin(now / 500.0 + k * 1.1));
        float qx = mid + (float) Math.cos(a) * hs * 2.1F, qy = hy + (float) Math.sin(a) * hs * 1.2F;
        G.rect(s, qx - 8, qy - 8, 16, 16, G.alpha(this.c[7], 0.35F + 0.5F * e));
        G.centered(s, "?", qx, qy - 4, G.alpha(this.c[5], 0.4F + 0.6F * e), 1F, false);
      }
      G.rect(s, mid - hs / 2F - 3, hy - hs / 2F - 3, hs + 6, hs + 6, this.c[3]);
      G.icon(s, G.head(me), mid - hs / 2F, hy - hs / 2F, hs, hs);
      float done = Math.min(1F, since / 4000F);
      int blocks = 20;
      float bw = 9, gap = 2, total = blocks * bw + (blocks - 1) * gap, bx = mid - total / 2F, by = hy + hs * 1.9F;
      G.rect(s, bx - 3, by - 3, total + 6, 16, this.light() ? this.c[4] : 0xFF000000);
      G.rect(s, bx - 2, by - 2, total + 4, 14, this.light() ? 0xFFFFFFFF : 0xFF14161C);
      float sweep = (now % 900) / 900F * (blocks + 4) - 2;
      for (int i = 0; i < blocks; i++) {
        boolean on = i < Math.round(done * blocks);
        int col = on ? G.mix(this.c[3], 0xFFFFFFFF, 0.55F * Math.max(0F, 1F - Math.abs(i - sweep) / 2F)) : this.light() ? G.shade(this.c[7], 0.92F) : 0xFF2A2E38;
        G.rect(s, bx + i * (bw + gap), by, bw, 10, col);
      }
      G.centered(s, L.t("match.searching") + "...".substring(0, 1 + (int) (since / 400 % 3)), mid, by + 20, this.c[5], 1F, false);
    } else if (hits.isEmpty()) {
      // niemand gefunden: der eigene Kopf, daneben ein leerer Platz
      float off = hs * 1.3F;
      G.rect(s, mid - off - hs / 2F - 3, hy - hs / 2F - 3, hs + 6, hs + 6, this.c[3]);
      G.icon(s, G.head(me), mid - off - hs / 2F, hy - hs / 2F, hs, hs);
      G.dashed(s, mid + off - hs / 2F - 3, hy - hs / 2F - 3, hs + 6, hs + 6, this.c[6]);
      G.centered(s, "?", mid + off, hy - 10, this.c[6], 2.4F, false);
      G.centered(s, "\u2026", mid, hy - 6, this.c[6], 1.6F, false);
      float y = hy + hs * 0.9F;
      for (String line : G.wrap(L.t("match.nobody"), this.cw - 60, 1F)) {
        G.centered(s, line, mid, y, this.c[5], 1F, false);
        y += 12;
      }
    } else {
      // Treffer: beide Koepfe, dazwischen das Herz mit der Prozentzahl
      MatchShare.Hit top = hits.get(0);
      float off = hs * 1.7F, pop = G.easeBack(Math.min(1F, (now - this.openedAt) / 500F));
      G.rect(s, mid - off - hs / 2F - 3, hy - hs / 2F - 3, hs + 6, hs + 6, this.c[3]);
      G.icon(s, G.head(me), mid - off - hs / 2F, hy - hs / 2F, hs, hs);
      G.rect(s, mid + off - hs / 2F - 3, hy - hs / 2F - 3, hs + 6, hs + 6, this.c[3]);
      G.icon(s, G.head(top.uuid), mid + off - hs / 2F, hy - hs / 2F, hs, hs);
      this.heart(s, mid, hy, Math.max(2F, hs / 13F) * pop, top.percent / 100F);
      G.centered(s, top.percent + "%", mid, hy - 7, 0xFFFFFFFF, 1.5F, true);
      G.centered(s, L.t("match.best"), mid, hy + hs * 0.85F, this.c[6], 0.9F, false);
      G.centered(s, top.name, mid, hy + hs * 0.85F + 13, this.c[5], 1.6F, !this.light());
      float y = hy + hs * 0.85F + 38;
      for (String line : G.wrap(L.t("match.oneHint"), this.cw - 60, 0.9F)) {
        G.centered(s, line, mid, y, this.c[6], 0.9F, false);
        y += 10;
      }
    }
    this.button(s, this.foot(2), L.t("match.again"), mx, my, true, false);
  }

  @Override
  protected boolean onClick(float mx, float my, boolean left) {
    this.layout();
    if (this.headerHit(mx, my)) {
      this.data.save();
      Screens.close();
      return true;
    }
    float[] f0 = this.foot(0), f1 = this.foot(1);
    if (this.choosing) {
      for (int i = 0; i < THEMES.length; i++) {
        float[] r = this.swatch(i);
        if (G.in(mx, my, r[0], r[1], r[2], r[3])) {
          String id = "theme:" + THEMES[i];
          if (!Loot.get().owns(id)) {
            if (!Loot.get().buy(id)) { // zu wenig Muenzen
              G.sound("block.note_block.bass", 0.7F);
              return true;
            }
            G.sound("entity.experience_orb.pickup", 1.2F);
          }
          this.data.theme = THEMES[i];
          this.data.save();
          G.click();
          return true;
        }
      }
      if (G.in(mx, my, f1[0], f1[1], f1[2], f1[3])) {
        if (this.data.theme.isEmpty()) {
          this.data.theme = "classic";
          this.data.save();
        }
        this.choosing = false;
        if (this.data.submitted) {
          this.search();
        }
        G.click();
      }
      return true;
    }
    if (this.quiz) {
      int count = MatchData.options(this.at);
      for (int i = 0; i < count; i++) {
        float[] r = this.option(i, count);
        if (G.in(mx, my, r[0], r[1], r[2], r[3])) {
          if (this.review) { // fertige Antwort aendern: je Frage hoechstens zweimal am Tag
            if (this.data.mayChange(this.at) && this.data.answers[this.at] != i) {
              this.data.change(this.at, i);
              G.sound("block.note_block.hat", 0.8F + i * 0.15F);
            }
            return true;
          }
          boolean fresh = this.data.answers[this.at] < 0;
          this.data.answers[this.at] = i;
          this.data.save();
          if (fresh && this.data.answered() % 10 == 0) { // ein Feld der Leiste ist voll
            this.levelAt = System.currentTimeMillis() + 250;
            G.sound("entity.player.levelup", 1.5F);
          } else {
            G.sound("entity.experience_orb.pickup", 0.7F + (this.data.answered() % 10) * 0.06F);
          }
          if (this.at < MatchData.TOTAL - 1) {
            this.at++;
          } else if (this.data.complete()) {
            this.finish();
          } else {
            this.at = this.data.firstOpen();
          }
          return true;
        }
      }
      float[] f2 = this.foot(2);
      if (this.at > 0 && G.in(mx, my, f0[0], f0[1], f0[2], f0[3])) {
        this.at--;
        G.click();
      } else if (this.review && this.at < MatchData.TOTAL - 1 && G.in(mx, my, f1[0], f1[1], f1[2], f1[3])) {
        this.at++;
        G.click();
      } else if (this.review && G.in(mx, my, f2[0], f2[1], f2[2], f2[3])) {
        this.review = false;
        this.finish();
      }
      return true;
    }
    float[] f2 = this.foot(2);
    if (this.data.submitted) { // abgesendet: nichts mehr zu aendern, nur neu suchen
      if (G.in(mx, my, f2[0], f2[1], f2[2], f2[3])) {
        this.search();
        G.click();
      }
      return true;
    }
    if (G.in(mx, my, f0[0], f0[1], f0[2], f0[3])) {
      if (this.data.daysUntilReset() > 0) {
        return true;
      }
      if (System.currentTimeMillis() - this.resetAskAt < 4000) { // zweiter Klick: wirklich zuruecksetzen
        this.data.reset();
        this.resetAskAt = 0;
        this.quiz = true;
        this.review = false;
        this.at = 0;
        G.sound("item.book.page_turn", 0.8F);
      } else {
        this.resetAskAt = System.currentTimeMillis();
        G.click();
      }
    } else if (G.in(mx, my, f2[0], f2[1], f2[2], f2[3])) { // Antworten noch einmal durchsehen und aendern
      this.quiz = true;
      this.review = true;
      this.at = 0;
      G.click();
    } else if (G.in(mx, my, f1[0], f1[1], f1[2], f1[3])) {
      if (System.currentTimeMillis() - this.sendAskAt < 5000) { // zweiter Klick: endgueltig absenden
        this.data.submit();
        this.sendAskAt = 0;
        this.search();
        G.sound("entity.player.levelup", 1.4F);
      } else {
        this.sendAskAt = System.currentTimeMillis();
        G.click();
      }
    }
    return true;
  }

  private void finish() {
    this.quiz = false;
    this.review = false;
    G.click();
  }

  @Override
  public void onCloseScreen() {
    super.onCloseScreen();
    MatchMusic.stop();
    this.data.save();
  }

  @Override
  protected boolean onKey(Key key) {
    if (key == Key.ESCAPE) {
      this.data.save();
      Screens.close();
      return true;
    }
    return false;
  }
}
