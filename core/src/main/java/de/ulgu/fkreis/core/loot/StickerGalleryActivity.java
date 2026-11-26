package de.ulgu.fkreis.core.loot;

import de.ulgu.fkreis.core.L;
import de.ulgu.fkreis.core.gfx.Backdrop;
import de.ulgu.fkreis.core.gfx.G;
import de.ulgu.fkreis.core.ui.FkActivity;
import de.ulgu.fkreis.core.ui.Screens;
import java.util.List;
import net.labymod.api.client.gui.screen.activity.AutoActivity;
import net.labymod.api.client.gui.screen.key.Key;
import net.labymod.api.client.render.matrix.Stack;

/** Alle Mob-Sticker zum Ansehen (nur anschauen, nichts kaufen): 24 je Seite, Rechtsklick schaltet auf Holo um. */
@AutoActivity
public class StickerGalleryActivity extends FkActivity {

  private static final int COLS = 8, ROWS = 3, PER_PAGE = COLS * ROWS;

  private final List<String> all = MobStickers.all();
  private int page;
  private boolean holo;

  public StickerGalleryActivity() {
    G.click();
  }

  private float[] cell(int i) {
    float gap = 6, cw = Math.min(46, (this.w - 30 - (COLS - 1) * gap) / COLS), ch = Math.min(cw * 1.25F, (this.h - 76 - (ROWS - 1) * gap) / ROWS);
    float total = COLS * cw + (COLS - 1) * gap;
    return new float[] {this.x + (this.w - total) / 2F + (i % COLS) * (cw + gap), this.y + 40 + (i / COLS) * (ch + gap), cw, ch};
  }

  private int pages() {
    return Math.max(1, (this.all.size() + PER_PAGE - 1) / PER_PAGE);
  }

  private void turn(int dir) {
    this.page = Math.max(0, Math.min(this.pages() - 1, this.page + dir));
    G.sound("item.book.page_turn", 1F);
  }

  @Override
  protected void draw(Stack s, float mx, float my) {
    Backdrop.draw(s, "dark_oak", this.x, this.y, this.w, this.h);
    this.header(s, L.t("loot.gallery"), mx, my);
    for (int i = 0; i < PER_PAGE; i++) {
      int idx = this.page * PER_PAGE + i;
      if (idx >= this.all.size()) {
        break;
      }
      float[] r = this.cell(i);
      MobStickers.draw(s, this.all.get(idx) + (this.holo ? "*" : ""), r[0], r[1], r[2], r[3], 0F, false);
    }
    G.centered(s, (this.page + 1) + " / " + this.pages(), this.x + this.w / 2F, this.y + this.h - 18, 0xFFE8DCC0, 1F, true);
  }

  @Override
  protected boolean onClick(float mx, float my, boolean left) {
    if (this.headerHit(mx, my)) {
      Screens.close();
    } else if (!left) {
      this.holo = !this.holo;
      G.click();
    } else {
      this.turn(mx < this.x + this.w / 2F ? -1 : 1);
    }
    return true;
  }

  @Override
  protected boolean onScroll(float mx, float my, float delta) {
    this.turn(delta < 0 ? 1 : -1);
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
