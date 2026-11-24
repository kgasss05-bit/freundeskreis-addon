package de.ulgu.fkreis.core.mixtape;

import de.ulgu.fkreis.core.L;
import de.ulgu.fkreis.core.gfx.Backdrop;
import de.ulgu.fkreis.core.gfx.G;
import de.ulgu.fkreis.core.gfx.Sys;
import de.ulgu.fkreis.core.ui.FkActivity;
import de.ulgu.fkreis.core.ui.Screens;
import net.labymod.api.client.gui.screen.activity.AutoActivity;
import net.labymod.api.client.gui.screen.key.Key;
import net.labymod.api.client.gui.screen.key.KeyHandler;
import net.labymod.api.client.render.matrix.Stack;

/** Neue Kassette anlegen oder eine bearbeiten: Aufdruck (Songname), YouTube-Link und Farbe. */
@AutoActivity
public class TapeEditActivity extends FkActivity {

  private final Tapes box = Tapes.get();
  private final Tapes.Tape target;
  private final Tapes.Tape work = new Tapes.Tape();
  private int focus;
  private String error = "";
  private float px, py, pw, ph;

  public TapeEditActivity(Tapes.Tape target) {
    this.target = target;
    if (target != null) {
      this.work.title = target.title;
      this.work.link = target.link;
      this.work.color = target.color;
      this.work.from = target.from;
    } else {
      this.work.color = Tapes.COLORS[this.box.tapes.size() % Tapes.COLORS.length];
    }
    G.click();
  }

  private float[] field(int i) {
    return new float[] {this.px + 12, this.py + 100 + i * 34, this.pw - 24, 16};
  }

  private float[] swatch(int i) {
    return new float[] {this.px + 12 + i * 21, this.py + 172, 18, 18};
  }

  private float[] btn(int i) {
    float bw = (this.pw - 30) / 2F;
    return new float[] {this.px + 12 + i * (bw + 6), this.py + this.ph - 26, bw, 16};
  }

  @Override
  protected void draw(Stack s, float mx, float my) {
    Backdrop.draw(s, this.box.background, this.x, this.y, this.w, this.h);
    this.pw = 236;
    this.ph = 236;
    this.px = this.x + (this.w - this.pw) / 2F;
    this.py = this.y + Math.max(4, (this.h - this.ph) / 2F);
    G.rect(s, this.px + 3, this.py + 4, this.pw, this.ph, 0x60000000);
    G.rect(s, this.px, this.py, this.pw, this.ph, 0xFF1C1F28);
    G.outline(s, this.px, this.py, this.pw, this.ph, 1, 0xFF000000);
    G.rect(s, this.px, this.py, this.pw, 2, 0xFFF2C94C);
    G.centered(s, L.t(this.target == null ? "mixtape.editNew" : "mixtape.editOld"), this.px + this.pw / 2F, this.py + 9, 0xFFF2C94C, 1.1F, true);
    Tapes.Tape show = this.work;
    if (show.title.isEmpty()) {
      show = new Tapes.Tape();
      show.title = L.t("mixtape.label");
      show.color = this.work.color;
    }
    G.icon(s, TapeArt.lying(show, 0), this.px + this.pw / 2F - 46, this.py + 26, 92, 56);

    String[] labels = {L.t("mixtape.fieldTitle"), L.t("mixtape.fieldLink")};
    boolean blink = (System.currentTimeMillis() / 500) % 2 == 0;
    for (int i = 0; i < 2; i++) {
      float[] f = this.field(i);
      G.text(s, labels[i], f[0], f[1] - 10, 0xFF9AA3B5, 0.8F, false);
      G.rect(s, f[0], f[1], f[2], f[3], 0xFF101319);
      G.outline(s, f[0], f[1], f[2], f[3], 1, this.focus == i ? 0xFFF2C94C : 0xFF000000);
      String v = i == 0 ? this.work.title : this.work.link;
      String shown = v;
      while (!shown.isEmpty() && G.width(shown) * 0.9F > f[2] - 14) { // langer Link: das Ende zeigen
        shown = shown.substring(1);
      }
      G.text(s, shown + (this.focus == i && blink ? "_" : ""), f[0] + 4, f[1] + 4, 0xFFFFFFFF, 0.9F, false);
    }
    G.text(s, L.t("mixtape.fieldColor"), this.px + 12, this.py + 162, 0xFF9AA3B5, 0.8F, false);
    for (int i = 0; i < Tapes.COLORS.length; i++) {
      float[] r = this.swatch(i);
      G.rect(s, r[0], r[1], r[2], r[3], 0xFF000000 | Tapes.COLORS[i]);
      G.outline(s, r[0], r[1], r[2], r[3], 1, Tapes.COLORS[i] == this.work.color ? 0xFFFFFFFF : 0xFF000000);
    }
    G.centered(s, this.error.isEmpty() ? L.t("mixtape.pasteHint") : this.error, this.px + this.pw / 2F, this.py + 196, this.error.isEmpty() ? 0xFF6C7486 : 0xFFFF8A7A, 0.75F, false);
    for (int i = 0; i < 2; i++) {
      float[] b = this.btn(i);
      boolean over = G.in(mx, my, b[0], b[1], b[2], b[3]);
      G.rect(s, b[0], b[1], b[2], b[3], i == 1 ? (over ? 0xFFFFDB6E : 0xFFF2C94C) : over ? 0xFF4A4F5C : 0xFF3A3E48);
      G.centered(s, L.t(i == 1 ? "mixtape.save" : "mixtape.cancel"), b[0] + b[2] / 2F, b[1] + 4, i == 1 ? 0xFF1A1400 : 0xFFFFFFFF, 0.9F, false);
    }
  }

  private void save() {
    this.work.title = this.work.title.trim();
    this.work.link = this.work.link.trim();
    if (this.work.title.isEmpty()) {
      this.error = L.t("mixtape.errTitle");
      return;
    }
    if (!Tapes.validLink(this.work.link)) {
      this.error = L.t("mixtape.errLink");
      return;
    }
    if (this.target != null) {
      this.target.title = this.work.title;
      this.target.link = this.work.link;
      this.target.color = this.work.color;
    } else {
      this.box.tapes.add(this.work);
    }
    this.box.save();
    G.sound("entity.player.levelup", 1.6F);
    Screens.now(new MixtapeActivity());
  }

  @Override
  protected boolean onClick(float mx, float my, boolean left) {
    for (int i = 0; i < 2; i++) {
      float[] f = this.field(i);
      if (G.in(mx, my, f[0], f[1], f[2], f[3])) {
        this.focus = i;
        return true;
      }
    }
    for (int i = 0; i < Tapes.COLORS.length; i++) {
      float[] r = this.swatch(i);
      if (G.in(mx, my, r[0], r[1], r[2], r[3])) {
        this.work.color = Tapes.COLORS[i];
        G.click();
        return true;
      }
    }
    float[] b0 = this.btn(0), b1 = this.btn(1);
    if (G.in(mx, my, b0[0], b0[1], b0[2], b0[3])) {
      Screens.now(new MixtapeActivity());
    } else if (G.in(mx, my, b1[0], b1[1], b1[2], b1[3])) {
      this.save();
    }
    return true;
  }

  private void type(String add) {
    StringBuilder sb = new StringBuilder(this.focus == 0 ? this.work.title : this.work.link);
    int max = this.focus == 0 ? Tapes.MAX_TITLE : 200;
    for (int i = 0; i < add.length() && sb.length() < max; i++) {
      char c = add.charAt(i);
      if (c >= 32 && (this.focus == 0 || c != ' ')) {
        sb.append(c);
      }
    }
    if (this.focus == 0) {
      this.work.title = sb.toString();
    } else {
      this.work.link = sb.toString();
    }
    this.error = "";
  }

  @Override
  protected boolean onKey(Key key) {
    if (key == Key.ESCAPE) {
      Screens.now(new MixtapeActivity());
      return true;
    }
    if (KeyHandler.isPaste(key)) {
      this.type(Sys.clipboard().trim());
      return true;
    }
    if (key == Key.ENTER) {
      if (this.focus == 0) {
        this.focus = 1;
      } else {
        this.save();
      }
      return true;
    }
    if (key == Key.BACK) {
      String v = this.focus == 0 ? this.work.title : this.work.link;
      if (!v.isEmpty()) {
        v = v.substring(0, v.length() - 1);
        if (this.focus == 0) {
          this.work.title = v;
        } else {
          this.work.link = v;
        }
      }
      return true;
    }
    return false;
  }

  @Override
  protected boolean onChar(char c) {
    if (c < 32) {
      return false;
    }
    this.type(String.valueOf(c));
    return true;
  }
}
