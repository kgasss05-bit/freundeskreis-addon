package de.ulgu.fkreis.core.loot;

import de.ulgu.fkreis.core.L;
import de.ulgu.fkreis.core.gfx.Backdrop;
import de.ulgu.fkreis.core.gfx.G;
import de.ulgu.fkreis.core.ui.FkActivity;
import de.ulgu.fkreis.core.ui.Screens;
import net.labymod.api.client.gui.screen.activity.AutoActivity;
import net.labymod.api.client.gui.screen.key.Key;
import net.labymod.api.client.render.matrix.Stack;

/**
 * Das taegliche Gluecksrad: einmal am Tag drehen, das Feld unter dem Zeiger (1 bis 10) wird sofort als Holy-Coins
 * gutgeschrieben. An seltenen Tagen glitzert eines der Felder; wer dort landet, bekommt zusaetzlich einen Holo-Sticker.
 * Was gewonnen wird, steht schon beim Klick fest ({@link Loot#open()}); das Rad dreht sich nur dorthin.
 */
@AutoActivity
public class LootActivity extends FkActivity {

  /** Reihenfolge der Felder im Uhrzeigersinn, grosse und kleine Zahlen gemischt. */
  private static final int[] FIELDS = {1, 6, 3, 9, 2, 7, 10, 4, 8, 5};
  private static final int[] COLORS = {0xFFC8372D, 0xFF2F6FB5, 0xFFE08A1E, 0xFF2E9A5B, 0xFF8E44AD, 0xFFD4A017, 0xFF1F8A8A, 0xFFC2417A, 0xFF4A58C4, 0xFF6FA82E};
  private static final float FIELD_DEG = 360F / FIELDS.length;
  private static final long SPIN_MS = 5200L;

  private final Loot loot = Loot.get();
  private boolean wasOpenable = this.loot.canOpen();
  private long spunAt;
  private float fromDeg, toDeg;
  private int lastTick = -1;
  private boolean landed;
  private long idleFrom;
  /** Index des Glitzerfeldes von heute in FIELDS, -1 wenn heute keines glitzert. */
  private int glitter = indexOf(this.loot.glitterNumber());

  public LootActivity() {
    G.click();
  }

  private static int indexOf(int number) {
    for (int i = 0; i < FIELDS.length; i++) {
      if (FIELDS[i] == number) {
        return i;
      }
    }
    return -1;
  }

  private float radius() {
    return Math.max(50F, Math.min(190F, Math.min(this.w / 2F - 40, (this.h - 34) / 2F - 20))); // fuellt den Bildschirm, auch auf grossen Monitoren
  }

  private float cx() {
    return this.x + this.w / 2F;
  }

  private float cy() {
    return this.y + 34 + (this.h - 34) / 2F + 2;
  }

  /** Mitte eines Feldes auf dem Rad in Grad (0 = oben, im Uhrzeigersinn), ohne Drehung. */
  private static float center(int field) {
    return field * FIELD_DEG + FIELD_DEG / 2F;
  }

  /** Welches Feld liegt bei diesem Winkel auf dem Rad (Index in FIELDS). */
  private static int fieldAt(float deg) {
    float d = ((deg % 360F) + 360F) % 360F;
    return Math.min(FIELDS.length - 1, (int) (d / FIELD_DEG));
  }

  /** Aktuelle Drehung des Rades in Grad. */
  private float rotation(long now) {
    if (this.spunAt == 0) {
      return this.wasOpenable ? this.toDeg + ((now - this.idleFrom) % 36000L) / 100F : this.toDeg; // wartet: dreht sich ganz langsam
    }
    float t = Math.min(1F, (now - this.spunAt) / (float) SPIN_MS);
    float e = 1F - (float) Math.pow(1F - t, 4);
    return this.fromDeg + (this.toDeg - this.fromDeg) * e;
  }

  private void spin() {
    long now = System.currentTimeMillis();
    this.fromDeg = this.rotation(now) % 360F;
    if (!this.loot.open()) {
      return;
    }
    // dieser Winkel auf dem Rad soll am Ende oben unter dem Zeiger stehen
    float target = center(Math.max(0, indexOf(this.loot.lastWin))) + (float) (Math.random() - 0.5) * FIELD_DEG * 0.6F;
    // Rad um -target drehen bringt das Feld nach oben; dazu sechs volle Umdrehungen
    float end = 360F - target;
    this.toDeg = this.fromDeg + 360F * 6 + (((end - this.fromDeg) % 360F) + 360F) % 360F;
    this.spunAt = now;
    this.lastTick = -1;
    G.sound("block.note_block.pling", 0.8F);
  }

  @Override
  protected void draw(Stack s, float mx, float my) {
    Backdrop.draw(s, this.loot.background, this.x, this.y, this.w, this.h);
    this.header(s, L.t("loot.title"), mx, my);
    Loot.balance(s, this.x + this.w - 10, this.y + 38);
    long now = System.currentTimeMillis();
    float rot = this.rotation(now), r = this.radius(), cx = this.cx(), cy = this.cy();
    boolean spinning = this.spunAt > 0 && now - this.spunAt < SPIN_MS;

    if (this.spunAt > 0) { // Klacken, wenn eine Feldgrenze unter dem Zeiger durchlaeuft
      int tick = (int) Math.floor(rot / FIELD_DEG);
      if (spinning && tick != this.lastTick) {
        this.lastTick = tick;
        G.sound("block.note_block.hat", 1.2F);
      }
      if (!spinning && !this.landed) {
        this.landed = true;
        G.sound(this.loot.lastHolo.isEmpty() ? "entity.player.levelup" : "ui.toast.challenge_complete", 1.3F);
      }
    }

    // das Rad schwebt frei in der Mitte: weicher Lichtschein dahinter, keine Halterung
    for (int k = 0; k < 5; k++) {
      float rr = r + 20 - k * 4;
      for (float gy = -rr; gy < rr; gy += 4) {
        float hf = (float) Math.sqrt(rr * rr - gy * gy);
        G.rect(s, cx - hf, cy + gy, hf * 2, 4, 0x08FFE9A0);
      }
    }
    float hubR = Math.max(18F, r * 0.25F);
    boolean canSpin = this.spunAt == 0 && this.wasOpenable;
    boolean hubOver = canSpin && (mx - cx) * (mx - cx) + (my - cy) * (my - cy) < hubR * hubR;
    // Rad: Zeile fuer Zeile in kleinen Bloecken, jeder Block bekommt die Farbe seines Feldes
    float px = 2F;
    int frame = (int) (now / 110L);
    for (float yy = -r; yy < r; yy += px) {
      float half = (float) Math.sqrt(Math.max(0F, r * r - (yy + px / 2F) * (yy + px / 2F)));
      int runColor = 0;
      float runStart = -half;
      for (float xx = -half; xx <= half; xx += px) {
        float dist = (float) Math.sqrt(xx * xx + yy * yy);
        int col;
        if (xx + px > half || dist > r - 2) {
          col = 0xFF2B1A0C; // aeusserer Rand
        } else if (dist > r - 9) {
          col = dist > r - 4 ? 0xFFB8861E : dist > r - 7 ? 0xFFF2C94C : 0xFF8A6A10; // goldener Reif
        } else if (dist < hubR) {
          col = dist > hubR - 2.5F ? 0xFF2B1A0C : dist > hubR - 6 ? 0xFF8A6A10 : hubOver ? 0xFFFFE27A : yy < -hubR * 0.3F ? 0xFFFFE9A0 : 0xFFF2C94C; // goldene Nabe, zugleich der Knopf
        } else {
          float deg = (((float) Math.toDegrees(Math.atan2(xx, -yy)) - rot) % 360F + 360F) % 360F;
          int f = fieldAt(deg);
          float in = deg % FIELD_DEG;
          if (in < 1.4F || in > FIELD_DEG - 0.4F) {
            col = 0xFFF2C94C; // goldene Trennlinie
          } else {
            col = dist > r - 20 ? G.mix(COLORS[f], 0xFF000000, 0.28F) : dist < r * 0.4F ? G.mix(COLORS[f], 0xFFFFFFFF, 0.22F) : COLORS[f];
            if (f == this.glitter) { // Glitzerfeld: zufaellige Punkte blitzen weiss auf
              int hsh = ((int) xx * 73856093) ^ ((int) yy * 19349663) ^ (frame * 83492791);
              int v = (hsh ^ (hsh >>> 13)) & 0xFF;
              if (v > 204) {
                col = v > 238 ? 0xFFFFFFFF : G.mix(COLORS[f], 0xFFFFFFFF, 0.6F);
              }
            }
          }
        }
        if (col != runColor) {
          if (runColor != 0) {
            G.rect(s, cx + runStart, cy + yy, xx - runStart, px, runColor);
          }
          runColor = col;
          runStart = xx;
        }
      }
      if (runColor != 0) {
        G.rect(s, cx + runStart, cy + yy, half - runStart + px, px, runColor);
      }
    }
    // Lichter auf dem Reif blinken abwechselnd
    for (int k = 0; k < 20; k++) {
      double a = Math.toRadians(k * 18);
      float bx = cx + (float) Math.sin(a) * (r - 5.5F), by = cy - (float) Math.cos(a) * (r - 5.5F);
      G.rect(s, bx - 2, by - 2, 4, 4, 0xFF2B1A0C);
      G.rect(s, bx - 1, by - 1, 2, 2, (k + now / 160L) % 2 == 0 ? 0xFFFFFFFF : 0xFFC9881A);
    }
    // Zahlen stehen aufrecht in der Mitte ihres Feldes, mit schwarzem Rand
    for (int i = 0; i < FIELDS.length; i++) {
      double a = Math.toRadians(center(i) + rot);
      float nx = cx + (float) Math.sin(a) * r * 0.68F, ny = cy - (float) Math.cos(a) * r * 0.68F - 4 * Math.max(1.4F, r / 62F);
      String n = String.valueOf(FIELDS[i]);
      float ns = Math.max(1.4F, r / 62F);
      for (int d = 0; d < 4; d++) {
        G.centered(s, n, nx + (d == 0 ? -1 : d == 1 ? 1 : 0), ny + (d == 2 ? -1 : d == 3 ? 1 : 0), 0xFF000000, ns, false);
      }
      G.centered(s, n, nx, ny, i == this.glitter ? 0xFFFFF2A8 : 0xFFFFFFFF, ns, false);
    }
    if (canSpin) { // "Drehen" steht auf der goldenen Mitte
      String spin = L.t("loot.spin").toUpperCase();
      float sc = Math.min(2F, (hubR * 2 - 12) / Math.max(1F, G.width(spin)));
      G.centered(s, spin, cx, cy - 4 * sc, 0xFF2B1A0C, sc, false);
    }
    // Zeiger: haengt am obersten Licht des Reifs und ragt nach unten in das Rad hinein
    float pt = cy - r + 5;
    for (int i = 0; i < 8; i++) {
      G.rect(s, cx - 8 + i, pt + i * 2, 16 - 2 * i, 2, 0xFF2B1A0C);
      if (i < 7) {
        G.rect(s, cx - 6 + i, pt + i * 2, 12 - 2 * i, 2, i < 2 ? 0xFFFFF2A8 : 0xFFF2C94C);
      }
    }
    G.rect(s, cx - 4, cy - r + 1, 8, 8, 0xFF2B1A0C);
    G.rect(s, cx - 3, cy - r + 2, 6, 6, (now / 160L) % 2 == 0 ? 0xFFFFFFFF : 0xFFFFD27A);

    // Ergebnis
    if (this.landed) {
      float pop = G.easeBack(Math.min(1F, (now - this.spunAt - SPIN_MS) / 350F));
      boolean holo = !this.loot.lastHolo.isEmpty();
      float sc = 4F * pop, tx = holo ? cx - 50 : cx;
      G.rect(s, this.x, cy - (holo ? 48 : 34), this.w, holo ? 96 : 68, G.alpha(0xFF000000, 0.6F * pop));
      G.centered(s, "+" + this.loot.lastWin, tx, cy - 6 - 4 * sc, 0xFFF2C94C, sc, true);
      G.centered(s, L.t("loot.name"), tx, cy + 16, 0xFFFFFFFF, 1.2F, true);
      if (holo) { // Glitzerfeld getroffen: die Coins und dazu der Holo-Sticker
        float hw = 50 * pop, hh = hw * 1.25F;
        MobStickers.draw(s, this.loot.lastHolo + "*", cx + 30, cy - hh / 2F - 4, hw, hh, 0F, false);
        G.centered(s, "+ " + L.t("loot.holo"), cx + 55, cy + 32, 0xFFFFFFFF, 1F, true);
      }
    } else if (this.spunAt == 0 && !this.wasOpenable) {
      G.rect(s, this.x, cy - 12, this.w, 24, 0x8C000000);
      G.centered(s, L.t("loot.tomorrow"), cx, cy - 4, 0xFFFFFFFF, 1.1F, true);
    }
  }

  @Override
  protected boolean onClick(float mx, float my, boolean left) {
    if (this.headerHit(mx, my)) {
      Screens.close();
      return true;
    }
    if (this.spunAt == 0 && this.loot.canOpen()) { // nur die goldene Mitte dreht das Rad
      float dx = mx - this.cx(), dy = my - this.cy(), hubR = Math.max(18F, this.radius() * 0.25F) + 3;
      if (dx * dx + dy * dy < hubR * hubR) {
        this.spin();
      }
      return true;
    }
    if (this.landed && this.loot.canOpen()) { // noch Drehungen uebrig (Test): Rad wieder freigeben
      this.fromDeg = this.toDeg % 360F;
      this.toDeg = this.fromDeg;
      this.spunAt = 0;
      this.landed = false;
      this.wasOpenable = true;
      this.idleFrom = System.currentTimeMillis();
      this.glitter = indexOf(this.loot.glitterNumber());
      G.click();
    } else if (this.spunAt == 0 || this.landed) {
      Screens.close();
    }
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
