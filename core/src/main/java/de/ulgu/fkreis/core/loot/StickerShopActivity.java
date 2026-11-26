package de.ulgu.fkreis.core.loot;

import de.ulgu.fkreis.core.L;
import de.ulgu.fkreis.core.gfx.Backdrop;
import de.ulgu.fkreis.core.gfx.G;
import de.ulgu.fkreis.core.ui.FkActivity;
import de.ulgu.fkreis.core.ui.Screens;
import java.time.Duration;
import java.time.LocalDateTime;
import net.labymod.api.client.gui.screen.activity.AutoActivity;
import net.labymod.api.client.gui.screen.key.Key;
import net.labymod.api.client.render.matrix.Stack;

/**
 * Sticker-Laden: jeden Tag stehen nur vier zufaellige Mob-Sticker im Angebot (fuer alle Spieler dieselben).
 * Gekauft wird mit Holy-Coins, gekaufte Sticker landen im Sammelbuch.
 */
@AutoActivity
public class StickerShopActivity extends FkActivity {

  private final String[] offer = MobStickers.daily();
  private final float[] hover = new float[4];

  public StickerShopActivity() {
    G.click();
  }

  private float[] cell(int i) {
    float gap = 18, cw = Math.min(76, (this.w - 40 - 3 * gap) / 4F), ch = Math.min(cw * 1.25F, this.h - 130);
    float total = this.offer.length * cw + (this.offer.length - 1) * gap;
    return new float[] {this.x + (this.w - total) / 2F + i * (cw + gap), this.y + 34 + (this.h - 34 - ch - 30) / 2F, cw, ch};
  }

  @Override
  protected void draw(Stack s, float mx, float my) {
    Backdrop.draw(s, "dark_oak", this.x, this.y, this.w, this.h);
    this.header(s, L.t("loot.shop"), mx, my);
    Loot.balance(s, this.x + this.w - 10, this.y + 38);
    for (int i = 0; i < this.offer.length; i++) {
      float[] r = this.cell(i);
      String mob = this.offer[i];
      boolean own = Loot.get().owned.contains(MobStickers.PREFIX + mob);
      boolean over = G.in(mx, my, r[0], r[1], r[2], r[3]);
      this.hover[i] = G.lerp(this.hover[i], over ? 1F : 0F, 0.25F);
      MobStickers.draw(s, mob, r[0], r[1], r[2], r[3], this.hover[i] * 2F, false);
      float cx = r[0] + r[2] / 2F, py = r[1] + r[3] + 9;
      if (own) { // schon gekauft
        G.rect(s, cx - 8, py - 2, 16, 14, 0xFF2E9A4B);
        G.centered(s, "\u2714", cx, py + 1, 0xFFFFFFFF, 1.1F, false);
      } else { // Preisschild
        int price = MobStickers.price(mob);
        String p = String.valueOf(price);
        float tw = G.width(p) * 1.2F + 16;
        G.rect(s, cx - tw / 2F - 4, py - 3, tw + 8, 16, 0xB0000000);
        Loot.coin(s, cx - tw / 2F, py - 1, 2F);
        G.text(s, p, cx - tw / 2F + 16, py + 1, Loot.get().coins >= price ? 0xFFF2C94C : 0xFFE06060, 1.2F, true);
      }
    }
    // wann das Angebot wechselt
    LocalDateTime now = LocalDateTime.now();
    long min = Duration.between(now, now.toLocalDate().plusDays(1).atStartOfDay()).toMinutes();
    G.centered(s, L.t("loot.next", String.format("%02d:%02d", min / 60, min % 60)), this.x + this.w / 2F, this.y + this.h - 16, 0xFFE8DCC0, 0.9F, true);
  }

  @Override
  protected boolean onClick(float mx, float my, boolean left) {
    if (this.headerHit(mx, my)) {
      Screens.close();
      return true;
    }
    for (int i = 0; i < this.offer.length; i++) {
      float[] r = this.cell(i);
      if (G.in(mx, my, r[0], r[1], r[2], r[3] + 28)) {
        String id = MobStickers.PREFIX + this.offer[i];
        if (Loot.get().owned.contains(id)) {
          G.click();
        } else if (Loot.get().buy(id)) {
          G.sound("entity.player.levelup", MobStickers.price(this.offer[i]) >= 100 ? 0.9F : 1.5F);
        } else {
          G.sound("block.note_block.bass", 0.7F); // zu wenig Muenzen
        }
        return true;
      }
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
