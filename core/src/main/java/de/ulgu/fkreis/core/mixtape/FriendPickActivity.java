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
import net.labymod.api.client.gui.screen.activity.AutoActivity;
import net.labymod.api.client.gui.screen.key.Key;
import net.labymod.api.client.render.matrix.Stack;

/** Aus der Kassettenkiste: einen Freund aussuchen, der die Kassette angeboten bekommt. Er muss sie erst annehmen. */
@AutoActivity
public class FriendPickActivity extends FkActivity {

  private static final int ROWS = 8;

  private final Tapes.Tape tape;
  private final List<Net.FriendInfo> friends = new ArrayList<>();
  private int page;

  public FriendPickActivity(Tapes.Tape tape) {
    this.tape = tape;
    for (Net.FriendInfo f : FkreisAddon.get().net().friends()) { // wer online ist, steht oben
      if (f.online) {
        this.friends.add(f);
      }
    }
    for (Net.FriendInfo f : FkreisAddon.get().net().friends()) {
      if (!f.online) {
        this.friends.add(f);
      }
    }
    G.click();
  }

  private int pages() {
    return Math.max(1, (this.friends.size() + ROWS - 1) / ROWS);
  }

  private float[] row(int i) {
    float rw = 220;
    return new float[] {this.x + (this.w - rw) / 2F, this.y + 110 + i * 22, rw, 20};
  }

  @Override
  protected void draw(Stack s, float mx, float my) {
    Backdrop.draw(s, Tapes.get().background, this.x, this.y, this.w, this.h);
    this.header(s, L.t("mixtape.pickTitle"), mx, my);
    float cx = this.x + this.w / 2F;
    G.icon(s, TapeArt.lying(this.tape, 0), cx - 46, this.y + 36, 92, 56);
    G.centered(s, L.t(this.friends.isEmpty() ? "mixtape.pickEmpty" : "mixtape.pickHint"), cx, this.y + 96, 0xFFDFE4EE, 0.9F, true);
    int from = this.page * ROWS;
    for (int i = 0; i < ROWS && from + i < this.friends.size(); i++) {
      Net.FriendInfo f = this.friends.get(from + i);
      float[] r = this.row(i);
      boolean over = f.online && G.in(mx, my, r[0], r[1], r[2], r[3]);
      G.rect(s, r[0], r[1], r[2], r[3], over ? 0xF04A4F5C : 0xE01C1F28);
      G.icon(s, G.head(f.uuid), r[0] + 2, r[1] + 2, 16, 16);
      G.text(s, f.name, r[0] + 24, r[1] + 6, f.online ? 0xFFFFFFFF : 0xFF6C7486, 1F, false);
      G.text(s, L.t(f.online ? "mixtape.pickOnline" : "mixtape.pickOffline"), r[0] + r[2] - 50, r[1] + 7, f.online ? 0xFF7BD88F : 0xFF6C7486, 0.8F, false);
    }
    if (this.pages() > 1) {
      G.centered(s, "< " + (this.page + 1) + "/" + this.pages() + " >", cx, this.y + 110 + ROWS * 22 + 6, 0xFFDFE4EE, 1F, true);
    }
  }

  @Override
  protected boolean onClick(float mx, float my, boolean left) {
    if (this.headerHit(mx, my)) {
      Screens.now(new MixtapeActivity());
      return true;
    }
    int from = this.page * ROWS;
    for (int i = 0; i < ROWS && from + i < this.friends.size(); i++) {
      Net.FriendInfo f = this.friends.get(from + i);
      float[] r = this.row(i);
      if (f.online && G.in(mx, my, r[0], r[1], r[2], r[3])) {
        boolean ok = FkreisAddon.get().net().offerTape(f.uuid, Gifts.pack(this.tape));
        Net.notify(L.t("mixtape.title"), L.t(ok ? "mixtape.giveSent" : "mixtape.giveFailed", f.name), G.head(f.uuid));
        Screens.now(new MixtapeActivity());
        return true;
      }
    }
    float cx = this.x + this.w / 2F, py = this.y + 110 + ROWS * 22;
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
      Screens.now(new MixtapeActivity());
      return true;
    }
    return false;
  }
}
