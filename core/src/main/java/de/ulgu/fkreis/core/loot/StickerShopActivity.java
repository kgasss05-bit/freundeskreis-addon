package de.ulgu.fkreis.core.loot;

import de.ulgu.fkreis.core.L;
import de.ulgu.fkreis.core.gfx.Backdrop;
import de.ulgu.fkreis.core.gfx.Grotto;
import de.ulgu.fkreis.core.gfx.G;
import de.ulgu.fkreis.core.ui.FkActivity;
import de.ulgu.fkreis.core.ui.Screens;
import java.time.Duration;
import java.time.LocalDateTime;
import net.labymod.api.client.gui.screen.activity.AutoActivity;
import net.labymod.api.client.gui.screen.key.Key;
import net.labymod.api.client.render.matrix.Stack;

/**
 * Sticker-Grotte: jeden Tag schweben vier zufaellige Mob-Sticker (fuer alle Spieler dieselben) ueber Amethystdrusen
 * in einer Hoehle aus den Bloecken des Spiels. Anklicken hebt einen Sticker an und zeigt "Kaufen"; bezahlt wird mit Holy-Coins.
 */
@AutoActivity
public class StickerShopActivity extends FkActivity {

  private static final int[] GLOW = {0xFF7CFFB0, 0xFF7CC8FF, 0xFFD59AFF, 0xFFFFE27A};

  private final String[] offer = MobStickers.daily();
  private final float[] lift = new float[4];
  private final long[] boughtAt = new long[4];
  private int selected = -1;
  private long shakeUntil;
  private final Grotto grotto = new Grotto();

  /** 0 = Sticker-Grotte, 1 = die Holz-Grotte am anderen Ende der Schiene. */
  private int room;
  /** Zeitpunkt, zu dem die Lore losgefahren ist (0 = sie steht). */
  private long rideAt;
  private boolean switched = true, arriving = true;
  private int clacks;
  private int wood = -1;
  private final long[] woodBought = new long[Backdrop.IDS.length];
  /** Hinausfahren, dunkle Fahrt mit Schienengeraeuschen, hereinfahren. */
  private static final long OUT_MS = 700L, DARK_MS = 1800L, IN_MS = 800L;

  public StickerShopActivity() {
    G.click();
  }

  /** Oeffnet direkt die Holz-Grotte (aus der Hintergrund-Auswahl heraus). */
  public static StickerShopActivity woods() {
    StickerShopActivity a = new StickerShopActivity();
    a.room = 1;
    a.grotto.skipOutside();
    return a;
  }

  // ------------------------------------------------------------------ Schiene und Lore

  private float cartSize() {
    return Math.max(32F, Math.min(56F, this.w * 0.055F));
  }

  /** Wo die Lore steht: in der Sticker-Grotte rechts neben den Strahlen, in der Holz-Grotte links davon. */
  private float cartHome(int inRoom) {
    float cs = this.cartSize();
    if (inRoom == 0) {
      float[] last = this.slot(this.offer.length - 1);
      return Math.max(this.x + this.w * 0.80F, last[0] + last[2] / 2F + 20 + cs / 2F);
    }
    float[] first = this.woodSlot(0);
    return Math.min(this.x + this.w * 0.20F, first[0] - first[2] / 2F - 14 - cs / 2F);
  }

  private float railY() {
    return this.y + this.h - Grotto.FLOOR + 9;
  }

  private boolean riding() {
    return this.rideAt > 0 && System.currentTimeMillis() - this.rideAt < OUT_MS + DARK_MS + IN_MS;
  }

  private boolean cartHit(float mx, float my) {
    float cs = this.cartSize(), cx = this.cartHome(this.room);
    return G.in(mx, my, cx - cs / 2F - 5, this.railY() + 7 - cs * 0.5F - 5, cs + 10, cs * 0.5F + 12);
  }

  /** Schiene vom Stellplatz bis zum Rand der Grotte und die Lore darauf (sie faehrt beim Anklicken hinaus und drueben herein). */
  private void drawCart(Stack s, float mx, float my) {
    long now = System.currentTimeMillis();
    float cs = this.cartSize(), home = this.cartHome(this.room), ry = this.railY();
    boolean right = this.room == 0; // in der Sticker-Grotte fuehrt die Schiene nach rechts hinaus, in der Holz-Grotte nach links
    float from = right ? home - cs * 0.9F : this.x, to = right ? this.x + this.w : home + cs * 0.9F;
    // Steinboden unter dem Gleis bis zum Bildrand, damit die Schiene nicht ins Leere fuehrt
    float floorY = this.y + this.h - Grotto.FLOOR;
    Grotto.floorStrip(s, this.x, this.y, this.w, this.h, from - 8, to + 8);
    for (int i = 0; i < 6; i++) { // zum Rand hin wird es dunkler, der Boden bleibt aber erkennbar
      float band = (to - from) * 0.12F, bx = right ? to - (i + 1) * band : from + i * band;
      G.rect(s, bx, floorY - 3, band, this.y + this.h - floorY + 3, G.alpha(0xFF000000, 0.10F * (right ? 6 - i : i + 1) / 2F));
    }
    // Gleis: Schwellen aus Holz, darauf zwei Schienen; schraeg von vorn gesehen
    G.rect(s, from, ry + 8, to - from, 3, 0x70000000);
    for (float sx = from + 2; sx < to - 4; sx += 9) {
      G.rect(s, sx, ry - 1, 5, 8, 0xFF5C4023);
      G.rect(s, sx, ry - 1, 5, 1, 0xFF7A5630);
    }
    for (int k = 0; k < 2; k++) {
      float yy = ry + k * 5;
      G.rect(s, from, yy, to - from, 2, 0xFF9C9CA4);
      G.rect(s, from, yy, to - from, 1, 0xFFD0D0D6);
      G.rect(s, from, yy + 2, to - from, 1, 0x80000000);
    }
    // Prellbock am Gleisende: zwei Pfosten, Querbalken mit rot-weisser Warntafel, schraege Stuetze nach hinten
    float bx0 = right ? from - 2 : to - 8, dir = right ? -1F : 1F;
    G.rect(s, bx0 + 1, ry + 7, 12, 3, 0x70000000);
    for (int i = 0; i < 6; i++) { // Stuetze
      G.rect(s, bx0 + 3 + dir * (3 + i * 2), ry - 12 + i * 3, 3, 4, 0xFF4A3218);
    }
    G.rect(s, bx0, ry - 16, 4, 24, 0xFF6B4A2A);
    G.rect(s, bx0, ry - 16, 1, 24, 0xFF8A6238);
    G.rect(s, bx0 + 6, ry - 16, 4, 24, 0xFF5C4023);
    G.rect(s, bx0 - 2, ry - 15, 14, 6, 0xFF3A2A16);
    for (int i = 0; i < 4; i++) {
      G.rect(s, bx0 - 1 + i * 3, ry - 14, 3, 4, i % 2 == 0 ? 0xFFC0392B : 0xFFF0F0F0);
    }
    // Lore: steht, faehrt hinaus (immer schneller) oder kommt herein (wird langsamer)
    float cx = home;
    long e = this.rideAt == 0 ? -1 : now - this.rideAt;
    if (e >= 0 && e < OUT_MS) {
      float f = e / (float) OUT_MS;
      float edge = right ? this.x + this.w + cs : this.x - cs;
      cx = home + (edge - home) * f * f;
    } else if (e >= OUT_MS && e < OUT_MS + DARK_MS) {
      cx = -9999F;
    } else if (e >= OUT_MS + DARK_MS) {
      float f = Math.min(1F, (e - OUT_MS - DARK_MS) / (float) IN_MS), ease = 1F - (1F - f) * (1F - f);
      float edge = right ? this.x + this.w + cs : this.x - cs;
      cx = edge + (home - edge) * ease;
    }
    if (cx > -9000F) {
      boolean over = this.rideAt == 0 && this.cartHit(mx, my);
      float bump = this.rideAt == 0 ? 0F : (float) Math.abs(Math.sin(now / 45.0)) * 1.2F;
      if (over) { // leuchtet auf wie der Hoehleneingang
        float pulse = 0.7F + 0.3F * (float) Math.sin(now / 200.0);
        for (int i = 0; i < 5; i++) {
          float grow = 3 + i * 4;
          G.rect(s, cx - cs / 2F - grow, ry + 7 - cs * 0.5F - grow, cs + 2 * grow, cs * 0.5F + 2 * grow, G.alpha(0xFFFFE27A, 0.07F * pulse));
        }
      }
      G.rect(s, cx - cs * 0.46F, ry + 6, cs * 0.92F, 3, 0x70000000); // Schatten auf dem Gleis
      cart(s, cx - cs / 2F, ry + 7 - cs * 0.5F - bump, cs, over);
    }
  }

  /**
   * Die Lore von der Seite als Pixelbild (20 x 10 Felder): graue Eisenwand mit versetzten Nieten, heller Rand links und
   * rechts, unten die beiden dunklen Raeder. Fest gezeichnet, damit sie in jeder Spielversion gleich aussieht.
   */
  private static void cart(Stack s, float x, float y, float cs, boolean lit) {
    float u = cs / 20F;
    G.rect(s, x, y, cs, 10 * u, 0xFFA0A4AE);
    G.rect(s, x + u, y, 18 * u, u, 0xFFA8ACB6); // oberer Rand etwas heller
    G.rect(s, x, y, u, 10 * u, 0xFFD2D8E4);
    G.rect(s, x + 19 * u, y, u, 10 * u, 0xFFC4CAD6);
    G.rect(s, x + u, y + 6 * u, 18 * u, 2 * u, 0xFF9A9EA8);
    for (int r = 1; r <= 5; r += 2) { // Nieten in drei Reihen
      for (int k = 3 + (r == 3 ? 1 : 0); k <= 17; k += 2) {
        G.rect(s, x + k * u, y + r * u, u, u, (k + r) % 3 == 0 ? 0xFF7C808A : 0xFF8A8E98);
      }
    }
    for (int k = 0; k < 2; k++) { // Raeder
      float wx = x + (k == 0 ? 3 : 13) * u;
      G.rect(s, wx, y + 9 * u, 4 * u, u, 0xFF3A3A40);
      G.rect(s, wx + u, y + 9 * u, 2 * u, u, 0xFF1A1A1E);
      G.rect(s, wx + u, y + 8 * u, 2 * u, u, 0xFF66666C);
    }
    if (lit) {
      G.rect(s, x, y, cs, 10 * u, 0x24FFE27A);
    }
  }

  /** Schwarzblende waehrend der Fahrt; im Dunkeln rattert die Lore ueber die Schienen und die Grotte wechselt. */
  private void rideVeil(Stack s) {
    if (this.rideAt == 0) {
      return;
    }
    long e = System.currentTimeMillis() - this.rideAt;
    if (e >= OUT_MS + DARK_MS + IN_MS) {
      this.rideAt = 0;
      G.sound("block.metal.place", 0.7F); // die Lore kommt zum Stehen
      return;
    }
    if (e >= OUT_MS + DARK_MS / 2 && !this.switched) {
      this.switched = true;
      this.room = 1 - this.room;
      this.selected = -1;
      this.wood = -1;
    }
    int due = (int) (e / 210L); // Schienenstoesse: tak-tak ... tak-tak
    if (e >= OUT_MS + DARK_MS && !this.arriving) { // auch beim Hereinrollen hoert man die Lore
      this.arriving = true;
      G.sound("entity.minecart.riding", 0.9F);
    }
    while (this.clacks <= due) {
      if (this.clacks % 3 != 2) {
        G.sound("block.metal.step", this.clacks % 3 == 0 ? 0.75F : 0.9F);
      }
      this.clacks++;
    }
    float a = e < OUT_MS ? e / (float) OUT_MS : e < OUT_MS + DARK_MS ? 1F : 1F - (e - OUT_MS - DARK_MS) / (float) IN_MS;
    G.rect(s, this.x, this.y, this.w, this.h, G.alpha(0xFF000000, Math.max(0F, Math.min(1F, a))));
  }

  // ------------------------------------------------------------------ Holz-Grotte

  /** Platz eines Holzes: zwei Reihen zu sechs, hinten hoeher, vorn tiefer. {cx, base, groesse, y}. */
  private float[] woodSlot(int i) {
    int col = i % 6, row = i / 6;
    float step = Math.min(92F, this.w * 0.60F / 6F), size = Math.max(22F, Math.min(40F, step * 0.62F));
    float x0 = this.x + this.w / 2F - step * 2.5F + this.w * 0.01F + (row == 1 ? step * 0.5F : 0F);
    float base = this.y + this.h - Grotto.FLOOR + 14 + row * 12;
    float top = this.y + 54, room = base - top - 30;
    float sy = top + (row == 0 ? room * 0.08F : room * 0.52F);
    return new float[] {x0 + col * step, base, size, sy};
  }

  private float[] woodButton() {
    return new float[] {this.x + this.w / 2F - 70, this.y + this.h - 32, 140, 18};
  }

  private void drawWoods(Stack s, float mx, float my) {
    Grotto.drawInside(s, this.x, this.y, this.w, this.h);
    this.drawCart(s, mx, my); // zuerst Boden, Gleis und Lore, damit nichts davon ueber Preisen und Texten liegt
    this.header(s, L.t("loot.woods"), mx, my);
    Loot.balance(s, this.x + this.w - 10, this.y + 38);
    long now = System.currentTimeMillis();
    for (int i = 0; i < Backdrop.IDS.length; i++) {
      float[] p = this.woodSlot(i);
      String id = "wood:" + Backdrop.IDS[i];
      int price = Loot.price(id), glow = GLOW[price == 0 ? 0 : price <= 20 ? 1 : price <= 25 ? 2 : 3];
      boolean own = Loot.get().owns(id), sel = i == this.wood;
      boolean over = G.in(mx, my, p[0] - p[2] / 2F - 4, p[3] - 4, p[2] + 8, p[2] + 8);
      float pulse = 0.6F + 0.4F * (float) Math.sin(now / 500.0 + i * 1.3);
      float sy = p[3] + (float) Math.sin(now / 620.0 + i * 1.1) * 3F - (sel ? 5F : over ? 2F : 0F), sx = p[0] - p[2] / 2F;
      Grotto.crackBeam(s, p[0], p[1], sy + p[2], glow, pulse, i);
      if (sel || over) {
        G.rect(s, sx - 3, sy - 3, p[2] + 6, p[2] + 6, G.alpha(glow, 0.30F + 0.12F * pulse));
      }
      G.rect(s, sx - 1, sy - 1, p[2] + 2, p[2] + 2, 0xFF1A1208);
      Backdrop.swatch(s, Backdrop.IDS[i], sx, sy, p[2], p[2]);
      if (own) {
        G.rect(s, p[0] - 7, sy + p[2] / 2F - 7, 14, 14, 0xFF2E9A4B);
        G.centered(s, "✔", p[0], sy + p[2] / 2F - 4, 0xFFFFFFFF, 1F, false);
      } else {
        String n = String.valueOf(price);
        float tw = G.width(n) * 0.9F + 12;
        G.rect(s, p[0] - tw / 2F - 2, sy + p[2] + 2, tw + 4, 12, 0xA0000000);
        Loot.coin(s, p[0] - tw / 2F, sy + p[2] + 4, 1.3F);
        G.text(s, n, p[0] - tw / 2F + 11, sy + p[2] + 4, Loot.get().coins >= price ? 0xFFF2C94C : 0xFFE06060, 0.9F, true);
      }
      if (this.woodBought[i] > 0 && now - this.woodBought[i] < 900) {
        float e = (now - this.woodBought[i]) / 900F;
        for (int k = 0; k < 12; k++) {
          double a = k / 12.0 * Math.PI * 2;
          G.rect(s, p[0] + (float) Math.cos(a) * 44 * e, sy + p[2] / 2F + (float) Math.sin(a) * 44 * e, 3, 3, G.alpha(glow, 1F - e));
        }
      }
    }
    if (this.wood >= 0) { // unten: Name des gewaehlten Holzes und Kaufen
      String id = "wood:" + Backdrop.IDS[this.wood], name = L.t("backdrop." + Backdrop.IDS[this.wood]);
      int price = Loot.price(id);
      float[] b = this.woodButton();
      if (Loot.get().owns(id)) {
        G.centered(s, name + " · " + L.t("loot.bought"), b[0] + b[2] / 2F, b[1] + 5, 0xFF7CFFB0, 0.95F, true);
      } else {
        boolean ok = Loot.get().coins >= price, bo = G.in(mx, my, b[0], b[1], b[2], b[3]);
        G.rect(s, b[0], b[1], b[2], b[3], ok ? (bo ? 0xFFFFE08A : 0xFFF2C94C) : 0xFF5A5068);
        G.rect(s, b[0], b[1] + b[3] - 2, b[2], 2, ok ? 0xFF8A6A10 : 0xFF3A3246);
        G.centered(s, G.fit(ok ? name + " · " + L.t("loot.buy", price) : L.t("loot.poor"), b[2] - 6, 0.9F), b[0] + b[2] / 2F, b[1] + 5, ok ? 0xFF1A1400 : 0xFFC9C0D8, 0.9F, false);
      }
    }
    this.grotto.veil(s, this.x, this.y, this.w, this.h);
    this.rideVeil(s);
  }

  private boolean clickWoods(float mx, float my) {
    float[] b = this.woodButton();
    if (this.wood >= 0 && G.in(mx, my, b[0], b[1], b[2], b[3])) {
      String id = "wood:" + Backdrop.IDS[this.wood];
      if (!Loot.get().owns(id)) {
        if (Loot.get().buy(id)) {
          this.woodBought[this.wood] = System.currentTimeMillis();
          G.sound("entity.player.levelup", 1.4F);
          G.sound("block.wood.place", 1F);
        } else {
          G.sound("block.note_block.bass", 0.7F);
        }
      }
      return true;
    }
    for (int i = Backdrop.IDS.length - 1; i >= 0; i--) {
      float[] p = this.woodSlot(i);
      if (G.in(mx, my, p[0] - p[2] / 2F - 4, p[3] - 6, p[2] + 8, p[2] + 22)) {
        this.wood = i;
        G.sound("block.wood.hit", 1F + (i % 6) * 0.08F);
        return true;
      }
    }
    this.wood = -1;
    return true;
  }

  /** Mitte, Stickergroesse und Bodenlinie eines Platzes: {cx, base, sw, sh}. */
  private float[] slot(int i) {
    float sw = Math.max(36F, Math.min(56F, this.w * 0.62F / 4F - 20)), sh = sw * 1.25F;
    float step = Math.min(104F, this.w * 0.62F / 4F), x0 = this.x + this.w / 2F - step * (this.offer.length - 1) / 2F;
    return new float[] {x0 + i * step, this.y + this.h - Grotto.FLOOR + 18, sw, sh};
  }

  private float[] buyButton(int i) {
    float[] s = this.slot(i);
    return new float[] {s[0] - 42, s[1] + 16, 84, 18};
  }

  @Override
  protected void draw(Stack s, float mx, float my) {
    if (!this.grotto.drawOutside(s, this.x, this.y, this.w, this.h, mx, my, L.t("loot.shop"))) {
      this.header(s, L.t("loot.shop"), mx, my);
      return;
    }
    if (this.room == 1) {
      this.drawWoods(s, mx, my);
      return;
    }
    Grotto.drawInside(s, this.x, this.y, this.w, this.h);
    this.drawCart(s, mx, my); // zuerst Boden, Gleis und Lore, damit nichts davon ueber Preisen und Texten liegt
    this.header(s, L.t("loot.shop"), mx, my);
    Loot.balance(s, this.x + this.w - 10, this.y + 38);
    long now = System.currentTimeMillis();
    for (int i = 0; i < this.offer.length; i++) {
      float[] p = this.slot(i);
      String mob = this.offer[i];
      int tier = Math.max(0, MobStickers.tierOf(mob)), glow = GLOW[tier], price = MobStickers.price(mob);
      boolean own = Loot.get().owned.contains(MobStickers.PREFIX + mob), sel = i == this.selected;
      boolean over = Math.abs(mx - p[0]) < p[2] / 2F + 10 && my > this.y + 44 && my < p[1] + 36;
      this.lift[i] = G.lerp(this.lift[i], sel ? 1F : over ? 0.4F : 0F, 0.2F);
      float pulse = 0.6F + 0.4F * (float) Math.sin(now / 500.0 + i * 1.3);
      float bob = (float) Math.sin(now / 620.0 + i * 1.57) * 5F;
      float sx = p[0] - p[2] / 2F + (sel && now < this.shakeUntil ? (float) Math.sin(now / 25.0) * 3F : 0F);
      float sy = Math.max(this.y + 50, p[1] - p[3] - Math.max(64F, (this.h - Grotto.FLOOR) * 0.30F)) - this.lift[i] * 14F + bob;
      // Riss im Boden, aus dem der Lichtstrahl bricht, der den Sticker traegt
      Grotto.crackBeam(s, p[0], p[1], this.y + 34, glow, pulse, i);
      if (sel || over) {
        G.rect(s, sx - 4, sy - 4, p[2] + 8, p[3] + 8, G.alpha(glow, 0.22F + 0.12F * pulse));
      }
      MobStickers.draw(s, mob, sx, sy, p[2], p[3], 0F, false);
      if (own) {
        G.rect(s, sx, sy, p[2], p[3], 0x88000000);
        G.rect(s, p[0] - 9, sy + p[3] / 2F - 10, 18, 18, 0xFF2E9A4B);
        G.centered(s, "✔", p[0], sy + p[3] / 2F - 6, 0xFFFFFFFF, 1.2F, false);
      }
      for (int k = 0; k < 5; k++) { // Funkeln rund um den Sticker
        float a = (float) Math.sin(now / 260.0 + k * 1.7 + i);
        if (a > 0.4F) {
          float gx = sx - 6 + ((k * 37 + i * 11) % (int) (p[2] + 12)), gy = sy - 6 + ((k * 61 + i * 17) % (int) (p[3] + 12));
          G.rect(s, gx, gy - 3, 1, 7, G.alpha(0xFFFFFFFF, a));
          G.rect(s, gx - 3, gy, 7, 1, G.alpha(0xFFFFFFFF, a));
        }
      }
      // unter der Druse: Preis, Knopf oder "gekauft"
      if (own) {
        G.centered(s, L.t("loot.bought"), p[0], p[1] + 20, 0xFF7CFFB0, 0.95F, true);
      } else if (sel) {
        float[] b = this.buyButton(i);
        boolean ok = Loot.get().coins >= price, bo = G.in(mx, my, b[0], b[1], b[2], b[3]);
        G.rect(s, b[0], b[1], b[2], b[3], ok ? (bo ? 0xFFFFE08A : 0xFFF2C94C) : 0xFF5A5068);
        G.rect(s, b[0], b[1] + b[3] - 2, b[2], 2, ok ? 0xFF8A6A10 : 0xFF3A3246);
        G.centered(s, G.fit(ok ? L.t("loot.buy", price) : L.t("loot.poor"), b[2] - 6, 0.9F), b[0] + b[2] / 2F, b[1] + 5, ok ? 0xFF1A1400 : 0xFFC9C0D8, 0.9F, false);
      } else {
        String n = String.valueOf(price);
        float tw = G.width(n) * 1.1F + 16;
        G.rect(s, p[0] - tw / 2F - 4, p[1] + 17, tw + 8, 15, 0xA0000000);
        Loot.coin(s, p[0] - tw / 2F, p[1] + 19, 1.8F);
        G.text(s, n, p[0] - tw / 2F + 15, p[1] + 20, Loot.get().coins >= price ? 0xFFF2C94C : 0xFFE06060, 1.1F, true);
      }
      if (this.boughtAt[i] > 0 && now - this.boughtAt[i] < 900) { // gekauft: Funkenring
        float e = (now - this.boughtAt[i]) / 900F;
        for (int k = 0; k < 14; k++) {
          double a = k / 14.0 * Math.PI * 2;
          G.rect(s, p[0] + (float) Math.cos(a) * 60 * e, sy + p[3] / 2F + (float) Math.sin(a) * 60 * e, 3, 3, G.alpha(glow, 1F - e));
        }
      }
    }
    LocalDateTime t = LocalDateTime.now();
    long min = Duration.between(t, t.toLocalDate().plusDays(1).atStartOfDay()).toMinutes();
    G.centered(s, L.t("loot.next", String.format("%02d:%02d", min / 60, min % 60)), this.x + this.w / 2F, this.y + this.h - 11, 0xFFC9BCE6, 0.8F, true);
    this.grotto.veil(s, this.x, this.y, this.w, this.h);
    this.rideVeil(s);
  }

  @Override
  protected boolean onClick(float mx, float my, boolean left) {
    if (this.headerHit(mx, my)) {
      Screens.close();
      return true;
    }
    if (!this.grotto.inside()) { // noch draussen: nur der Hoehleneingang ist anklickbar
      this.grotto.click(mx, my, this.x, this.y, this.w, this.h);
      return true;
    }
    if (this.riding()) {
      return true;
    }
    if (this.cartHit(mx, my)) { // Lore angeklickt: sie faehrt los
      this.rideAt = System.currentTimeMillis();
      this.switched = false;
      this.arriving = false;
      this.clacks = 0;
      G.sound("entity.minecart.riding", 1F);
      return true;
    }
    if (this.room == 1) {
      return this.clickWoods(mx, my);
    }
    for (int i = 0; i < this.offer.length; i++) {
      float[] p = this.slot(i);
      String id = MobStickers.PREFIX + this.offer[i];
      boolean own = Loot.get().owned.contains(id);
      float[] b = this.buyButton(i);
      if (i == this.selected && !own && G.in(mx, my, b[0], b[1], b[2], b[3])) {
        if (Loot.get().buy(id)) {
          this.boughtAt[i] = System.currentTimeMillis();
          G.sound("entity.player.levelup", MobStickers.price(this.offer[i]) >= 100 ? 0.9F : 1.5F);
          G.sound("block.amethyst_block.chime", 1F);
        } else {
          this.shakeUntil = System.currentTimeMillis() + 350;
          G.sound("block.note_block.bass", 0.7F); // zu wenig Muenzen
        }
        return true;
      }
      if (Math.abs(mx - p[0]) < p[2] / 2F + 14 && my > this.y + 44 && my < p[1] + 10) {
        this.selected = i;
        G.sound("block.amethyst_block.hit", 1F + i * 0.12F);
        return true;
      }
    }
    this.selected = -1;
    return true;
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
