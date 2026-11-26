package de.ulgu.fkreis.core.loot;

import de.ulgu.fkreis.core.L;
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

  public StickerShopActivity() {
    G.click();
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
    Grotto.drawInside(s, this.x, this.y, this.w, this.h);
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
