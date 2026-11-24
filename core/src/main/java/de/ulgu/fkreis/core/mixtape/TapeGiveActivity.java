package de.ulgu.fkreis.core.mixtape;

import de.ulgu.fkreis.core.FkreisAddon;
import de.ulgu.fkreis.core.L;
import de.ulgu.fkreis.core.gfx.Backdrop;
import de.ulgu.fkreis.core.gfx.G;
import de.ulgu.fkreis.core.net.Net;
import de.ulgu.fkreis.core.ui.FkActivity;
import de.ulgu.fkreis.core.ui.Screens;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.labymod.api.client.gui.screen.activity.AutoActivity;
import net.labymod.api.client.gui.screen.key.Key;
import net.labymod.api.client.render.matrix.Stack;

/** Ueber das Freundeskreis-Rad: eine eigene Kassette aussuchen und dem Spieler anbieten. Er muss sie erst annehmen. */
@AutoActivity
public class TapeGiveActivity extends FkActivity {

  private static final int ROWS = 3, COLS = 4;

  private final UUID to;
  private final String name;
  private final List<Tapes.Tape> own = new ArrayList<>();
  private int page;

  public TapeGiveActivity(UUID to, String name) {
    this.to = to;
    this.name = name;
    for (Tapes.Tape t : Tapes.get().tapes) {
      if (!t.gift()) {
        this.own.add(t);
      }
    }
    G.click();
  }

  private int pages() {
    return Math.max(1, (this.own.size() + ROWS * COLS - 1) / (ROWS * COLS));
  }

  private float[] cell(int i) {
    float cw = 100, ch = 66;
    float x0 = this.x + (this.w - COLS * cw) / 2F, y0 = this.y + 62;
    return new float[] {x0 + (i % COLS) * cw + 4, y0 + (i / COLS) * ch, 92, 56};
  }

  @Override
  protected void draw(Stack s, float mx, float my) {
    Backdrop.draw(s, Tapes.get().background, this.x, this.y, this.w, this.h);
    this.header(s, L.t("mixtape.giveTitle", this.name), mx, my);
    if (this.own.isEmpty()) {
      G.centered(s, L.t("mixtape.giveEmpty"), this.x + this.w / 2F, this.y + this.h / 2F, 0xFFFFFFFF, 1F, true);
      return;
    }
    G.centered(s, L.t("mixtape.giveHint"), this.x + this.w / 2F, this.y + 44, 0xFFDFE4EE, 0.9F, true);
    int from = this.page * ROWS * COLS;
    for (int i = 0; i < ROWS * COLS && from + i < this.own.size(); i++) {
      float[] c = this.cell(i);
      boolean over = G.in(mx, my, c[0], c[1], c[2], c[3]);
      G.icon(s, TapeArt.lying(this.own.get(from + i), 0), c[0], c[1] - (over ? 3 : 0), c[2], c[3]);
    }
    G.centered(s, "< " + (this.page + 1) + "/" + this.pages() + " >", this.x + this.w / 2F, this.y + 62 + ROWS * 66 + 6, 0xFFDFE4EE, 1F, true);
  }

  @Override
  protected boolean onClick(float mx, float my, boolean left) {
    if (this.headerHit(mx, my)) {
      Screens.close();
      return true;
    }
    int from = this.page * ROWS * COLS;
    for (int i = 0; i < ROWS * COLS && from + i < this.own.size(); i++) {
      float[] c = this.cell(i);
      if (G.in(mx, my, c[0], c[1], c[2], c[3])) {
        Tapes.Tape t = this.own.get(from + i);
        boolean ok = FkreisAddon.get().net().offerTape(this.to, Gifts.pack(t));
        Net.notify(L.t("mixtape.title"), L.t(ok ? "mixtape.giveSent" : "mixtape.giveFailed", this.name), G.head(this.to));
        Screens.close();
        return true;
      }
    }
    float cx = this.x + this.w / 2F, py = this.y + 62 + ROWS * 66;
    if (G.in(mx, my, cx - 50, py, 50, 20)) {
      this.page = Math.max(0, this.page - 1);
    } else if (G.in(mx, my, cx, py, 50, 20)) {
      this.page = Math.min(this.pages() - 1, this.page + 1);
    }
    return true;
  }

  @Override
  protected boolean onScroll(float mx, float my, float delta) {
    this.page = Math.max(0, Math.min(this.pages() - 1, this.page + (delta < 0 ? 1 : -1)));
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
