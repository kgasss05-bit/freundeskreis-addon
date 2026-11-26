package de.ulgu.fkreis.core.loot;

import de.ulgu.fkreis.core.L;
import de.ulgu.fkreis.core.gfx.Backdrop;
import de.ulgu.fkreis.core.gfx.G;
import de.ulgu.fkreis.core.ui.FkActivity;
import de.ulgu.fkreis.core.ui.Screens;
import net.labymod.api.client.gui.icon.Icon;
import net.labymod.api.client.gui.screen.activity.AutoActivity;
import net.labymod.api.client.gui.screen.key.Key;
import net.labymod.api.client.render.matrix.Stack;
import net.labymod.api.client.resources.ResourceLocation;

/**
 * Die taegliche Truhe: anklicken, sie springt auf und viele Muenzen fallen heraus. Man sammelt sie mit dem Beutel
 * (Bundle-Textur aus dem Spiel) ein, der am Mauszeiger haengt und in Gold zeigt, wie viele Holy-Coins er enthaelt.
 */
@AutoActivity
public class LootActivity extends FkActivity {

  private static final float U = 4F; // Bildschirmpixel je Truhenpixel
  private static final int CW = 34, BODY = 18, LID = 10;

  private final Loot loot = Loot.get();
  private final boolean wasOpenable = this.loot.canOpen();
  private static final Icon BUNDLE = Icon.texture(ResourceLocation.create("minecraft", "textures/item/bundle.png"));

  private long wonAt, doneAt;
  private int win;
  /** Die herausgefallenen Muenzen: Landeplatz, Flugdauer, Startverzoegerung und ob sie schon im Beutel sind. */
  private float[] px = new float[0], py = new float[0], dur = new float[0], delay = new float[0];
  private boolean[] got = new boolean[0];
  private int collected;
  /** Der Beutel liegt unten in der Ecke, bis man ihn anklickt; dann haengt er am Mauszeiger und sammelt Muenzen ein. */
  private boolean holding;
  /** Der Holo-Sticker wird direkt mit der Maus angeklickt (ohne Beutel). */
  private boolean holoTaken;
  private float holoX, holoY, holoW, holoH;

  public LootActivity() {
    G.click();
  }

  private float cx() {
    return this.x + this.w / 2F;
  }

  private float top() { // Oberkante des Truhenkoerpers
    return this.y + this.h / 2F + 6;
  }

  /** Verteilt die Muenzen rund um die Truhe: genau so viele, wie man gewonnen hat. */
  private void scatter() {
    int n = this.win;
    java.util.Random rnd = new java.util.Random(System.nanoTime());
    this.px = new float[n];
    this.py = new float[n];
    this.dur = new float[n];
    this.delay = new float[n];
    this.got = new boolean[n];
    this.collected = 0;
    float reach = Math.min(this.w / 2F - 24, 190), by = this.top();
    for (int i = 0; i < n; i++) {
      float side = rnd.nextBoolean() ? 1F : -1F;
      this.px[i] = this.cx() + side * (20 + rnd.nextFloat() * (reach - 20));
      this.py[i] = Math.min(this.y + this.h - 16, by + 8 + rnd.nextFloat() * (BODY * U + 34));
      this.dur[i] = 0.55F + rnd.nextFloat() * 0.45F;
      this.delay[i] = 0.15F + i * 0.07F;
    }
  }

  /** So viele Holy-Coins sind sichtbar schon im Beutel (gutgeschrieben wird sofort beim Oeffnen, das hier ist nur die Anzeige). */
  private int shown() {
    if (this.wonAt == 0 || this.got.length == 0) {
      return this.loot.coins;
    }
    return this.loot.coins - this.win + this.collected;
  }

  @Override
  protected void draw(Stack s, float mx, float my) {
    Backdrop.draw(s, "dark_oak", this.x, this.y, this.w, this.h);
    G.rect(s, this.x, this.y, this.w, this.h, 0x50000000);
    this.header(s, L.t("loot.title"), mx, my);
    long now = System.currentTimeMillis();
    boolean opened = this.wonAt > 0 || !this.wasOpenable;
    float t = this.wonAt > 0 ? (now - this.wonAt) / 1000F : opened ? 9F : 0F;
    float lift = G.easeBack(Math.min(1F, t / 0.35F)); // 0 = zu, 1 = Deckel oben
    float bob = opened ? 0F : (float) Math.sin(now / 300.0) * 3F;
    float bx = this.cx() - CW * U / 2F, by = this.top() + bob;

    if (opened) { // Lichtstrahlen aus der Truhe
      float grow = Math.min(1F, t / 0.5F);
      for (int i = 0; i < 9; i++) {
        float wHalf = 16 + i * 13 * grow, yy = by - 6 - i * 16 * grow;
        G.rect(s, this.cx() - wHalf, yy, wHalf * 2, 17, G.alpha(0xFFFFE9A0, (0.34F - i * 0.035F) * grow));
      }
    } else { // geschlossen: leises Funkeln ringsum
      for (int i = 0; i < 6; i++) {
        float a = (float) Math.abs(Math.sin(now / 500.0 + i * 1.3));
        float qx = this.cx() + (float) Math.cos(i * 1.05 + now / 2400.0) * 92, qy = by + 20 + (float) Math.sin(i * 1.05 + now / 2400.0) * 56;
        G.rect(s, qx - 1, qy - 4, 2, 8, G.alpha(0xFFFFF2A8, a));
        G.rect(s, qx - 4, qy - 1, 8, 2, G.alpha(0xFFFFF2A8, a));
      }
    }
    this.chest(s, bx, by, lift);

    // die Muenzen: fliegen im Bogen heraus, bleiben liegen und verschwinden im Beutel, sobald er sie beruehrt
    for (int i = 0; i < this.got.length; i++) {
      if (this.got[i]) {
        continue;
      }
      float e = (t - this.delay[i]) / this.dur[i];
      if (e < 0F) {
        continue;
      }
      if (e < 1F) {
        float sx = this.cx(), sy = by - 6;
        float cxp = sx + (this.px[i] - sx) * e, cyp = sy + (this.py[i] - sy) * e - (float) Math.sin(Math.PI * e) * 110F;
        Loot.coin(s, cxp - 6, cyp - 6, 2F);
        continue;
      }
      G.rect(s, this.px[i] - 5, this.py[i] + 5, 12, 3, 0x50000000);
      Loot.coin(s, this.px[i] - 6, this.py[i] - 6, 2F);
      if (Math.abs(Math.sin(now / 350.0 + i * 0.9)) > 0.93) { // kurzes Aufblitzen
        G.rect(s, this.px[i] - 1, this.py[i] - 9, 2, 6, 0xFFFFFFFF);
        G.rect(s, this.px[i] - 3, this.py[i] - 7, 6, 2, 0xFFFFFFFF);
      }
      if (this.holding && Math.abs(mx - this.px[i]) < 20 && Math.abs(my - this.py[i]) < 20) {
        this.got[i] = true;
        this.collected++;
        G.sound("entity.experience_orb.pickup", 0.7F + 0.9F * this.collected / this.got.length);
        if (this.collected == this.got.length) {
          this.doneAt = now;
          G.sound("entity.player.levelup", 1.3F);
        }
      }
    }
    boolean holo = this.wonAt > 0 && !this.loot.lastHolo.isEmpty();
    if (holo) { // statt Muenzen steigt die Holo-Karte direkt aus der Truhe; man nimmt sie mit einem Klick
      float rise = G.easeOut(Math.max(0F, Math.min(1F, (t - 0.2F) / 0.7F)));
      float hw = 22 + 38 * rise, hh = hw * 1.25F;
      float hx = this.cx() - hw / 2F, hy = by - 10 - (hh + 44) * rise + (rise >= 1F ? (float) Math.sin(now / 400.0) * 4F : 0F);
      boolean over = rise >= 1F && !this.holoTaken && G.in(mx, my, hx, hy, hw, hh);
      MobStickers.draw(s, this.loot.lastHolo + "*", hx, hy, hw, hh, over ? 2F : 0F, false);
      this.holoX = hx;
      this.holoY = hy;
      this.holoW = rise >= 1F ? hw : 0F;
      this.holoH = hh;
      if (this.holoTaken) {
        G.centered(s, L.t("loot.holo"), this.cx(), hy - 16, 0xFFFFFFFF, 1.4F, true);
      }
    } else if (this.doneAt > 0) {
      float pop = G.easeBack(Math.min(1F, (now - this.doneAt) / 350F));
      float sc = 4F * pop;
      G.centered(s, "+" + this.win, this.cx(), by - 96 - 4 * sc, 0xFFF2C94C, sc, true);
      G.centered(s, L.t("loot.name"), this.cx(), by - 56, 0xFFFFFFFF, 1.2F, true);
    } else if (opened && this.wonAt == 0) {
      G.centered(s, L.t("loot.tomorrow"), this.cx(), by - 40, 0xFFFFFFFF, 1.1F, true);
    }

    // der Beutel: liegt unten rechts in der Ecke, nach einem Klick haengt er am Mauszeiger; in Gold steht der Inhalt darauf
    float bs = 40;
    float[] corner = this.corner();
    float bxm = this.holding ? mx - bs / 2F + 6 : corner[0], bym = this.holding ? my - bs / 2F + 8 : corner[1];
    if (!this.holding && this.wonAt > 0 && this.collected < this.got.length) { // Muenzen liegen bereit: Beutel wippt
      bym -= (float) Math.abs(Math.sin(now / 250.0)) * 4F;
    }
    G.icon(s, BUNDLE, bxm, bym, bs, bs);
    String n = String.valueOf(this.shown());
    float nx = bxm + bs / 2F;
    for (int d = 0; d < 4; d++) {
      G.centered(s, n, nx + (d == 0 ? -1 : d == 1 ? 1 : 0), bym + bs - 15 + (d == 2 ? -1 : d == 3 ? 1 : 0), 0xFF000000, 1.1F, false);
    }
    G.centered(s, n, nx, bym + bs - 15, 0xFFF2C94C, 1.1F, false);
  }

  private float[] corner() {
    return new float[] {this.x + this.w - 52, this.y + this.h - 50};
  }

  /** Pixel-Truhe; lift 0 = geschlossen, 1 = Deckel aufgeklappt. */
  private void chest(Stack s, float bx, float by, float lift) {
    int edge = 0xFF2B1A0C, wood = 0xFF8A5A2B, dark = 0xFF6E451F, light = 0xFFA9743A, gold = 0xFFF2C94C, goldD = 0xFFB8891A;
    float w = CW * U, bh = BODY * U;
    G.rect(s, bx + 6, by + bh - 2, w - 4, 8, 0x60000000); // Schatten
    // Koerper
    G.rect(s, bx, by, w, bh, edge);
    G.rect(s, bx + U, by + U, w - 2 * U, bh - 2 * U, wood);
    for (int i = 1; i < 4; i++) {
      G.rect(s, bx + U, by + i * 4.4F * U, w - 2 * U, U / 2F, dark);
    }
    G.rect(s, bx + U, by + U, w - 2 * U, U, light);
    G.rect(s, bx + 4 * U, by, 2 * U, bh, goldD);
    G.rect(s, bx + w - 6 * U, by, 2 * U, bh, goldD);
    if (lift > 0.05F) { // Innenraum mit Goldschein
      G.rect(s, bx + U, by - 2 * U * lift, w - 2 * U, 3 * U * lift, 0xFF1A0F06);
      G.rect(s, bx + 2 * U, by - U * lift, w - 4 * U, 2 * U * lift, G.alpha(0xFFFFE27A, 0.9F));
    }
    // Deckel: wandert beim Oeffnen nach oben und wird flacher (aufgeklappt von vorn gesehen)
    float lh = LID * U * (1F - 0.45F * lift), ly = by - lh - (LID * 1.25F * U) * lift;
    G.rect(s, bx - U / 2F, ly, w + U, lh, edge);
    G.rect(s, bx + U / 2F, ly + U, w - U, lh - 2 * U, lift > 0.5F ? dark : wood);
    G.rect(s, bx + U / 2F, ly + U, w - U, U, light);
    G.rect(s, bx + 4 * U, ly, 2 * U, lh, goldD);
    G.rect(s, bx + w - 6 * U, ly, 2 * U, lh, goldD);
    G.rect(s, bx - U / 2F, ly + lh - U, w + U, U, goldD);
    // Schloss
    float lx = bx + w / 2F - 2.5F * U, lyy = lift > 0.05F ? ly + lh - 2 * U : by - 2 * U;
    G.rect(s, lx, lyy, 5 * U, 6 * U, edge);
    G.rect(s, lx + U, lyy + U, 3 * U, 4 * U, gold);
    G.rect(s, lx + 2 * U, lyy + 2 * U, U, 2 * U, edge);
  }

  @Override
  protected boolean onClick(float mx, float my, boolean left) {
    if (this.headerHit(mx, my)) {
      Screens.close();
      return true;
    }
    float[] corner = this.corner();
    if (G.in(mx, my, corner[0] - 4, corner[1] - 4, 48, 48) || (this.holding && G.in(mx, my, this.x + this.w - 60, this.y + this.h - 58, 60, 58))) {
      this.holding = !this.holding; // Beutel aufnehmen oder wieder ablegen
      G.sound("item.bundle.insert", 1F);
      return true;
    }
    if (this.holoW > 0 && !this.holoTaken && G.in(mx, my, this.holoX, this.holoY, this.holoW, this.holoH)) {
      this.holoTaken = true;
      this.doneAt = System.currentTimeMillis();
      G.sound("ui.toast.challenge_complete", 1F);
      return true;
    }
    if (this.wonAt == 0 && this.loot.canOpen()) {
      float bx = this.cx() - CW * U / 2F, by = this.top();
      if (G.in(mx, my, bx - 10, by - LID * U - 14, CW * U + 20, (BODY + LID) * U + 28)) {
        if (this.loot.open()) {
          this.win = this.loot.lastWin;
          this.wonAt = System.currentTimeMillis();
          this.scatter();
          G.sound("block.chest.open", 1F);
        }
      }
      return true;
    }
    if (this.wonAt == 0 || (this.doneAt > 0 && System.currentTimeMillis() - this.doneAt > 900)) {
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
