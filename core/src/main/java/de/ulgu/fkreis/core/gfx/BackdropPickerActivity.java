package de.ulgu.fkreis.core.gfx;

import de.ulgu.fkreis.core.L;
import de.ulgu.fkreis.core.mixtape.Tapes;
import de.ulgu.fkreis.core.ui.FkActivity;
import de.ulgu.fkreis.core.ui.Screens;
import de.ulgu.fkreis.core.match.MatchData;
import net.labymod.api.client.gui.screen.activity.AutoActivity;
import net.labymod.api.client.gui.screen.key.Key;
import net.labymod.api.client.render.matrix.Stack;

/** Aus den Addon-Einstellungen: das Holz fuer den Hintergrund der Mixtapes oder des Freunde-Match aussuchen. */
@AutoActivity
public class BackdropPickerActivity extends FkActivity {

  private final boolean mixtape;

  public BackdropPickerActivity(boolean mixtape) {
    this.mixtape = mixtape;
    G.click();
  }

  private String current() {
    return this.mixtape ? Tapes.get().background : MatchData.get().background;
  }

  private float[] cell(int i) {
    float cw = 104, ch = 70;
    float x0 = this.x + (this.w - 4 * cw) / 2F, y0 = this.y + Math.max(44, (this.h - 2 * ch) / 2F);
    return new float[] {x0 + (i % 4) * cw + 4, y0 + (i / 4) * ch, 96, 52};
  }

  @Override
  protected void draw(Stack s, float mx, float my) {
    Backdrop.draw(s, this.current(), this.x, this.y, this.w, this.h);
    this.header(s, L.t(this.mixtape ? "backdrop.titleMixtape" : "backdrop.titleMatch"), mx, my);
    for (int i = 0; i < Backdrop.IDS.length; i++) {
      float[] c = this.cell(i);
      boolean sel = Backdrop.IDS[i].equals(this.current()), over = G.in(mx, my, c[0], c[1], c[2], c[3]);
      Backdrop.swatch(s, Backdrop.IDS[i], c[0], c[1], c[2], c[3]);
      G.outline(s, c[0] - 1, c[1] - 1, c[2] + 2, c[3] + 2, sel ? 2 : 1, sel ? 0xFFF2C94C : over ? 0xFFFFFFFF : 0xFF000000);
      G.centered(s, L.t("backdrop." + Backdrop.IDS[i]), c[0] + c[2] / 2F, c[1] + c[3] + 4, sel ? 0xFFF2C94C : 0xFFFFFFFF, 0.9F, true);
    }
  }

  @Override
  protected boolean onClick(float mx, float my, boolean left) {
    if (this.headerHit(mx, my)) {
      Screens.close();
      return true;
    }
    for (int i = 0; i < Backdrop.IDS.length; i++) {
      float[] c = this.cell(i);
      if (G.in(mx, my, c[0], c[1], c[2], c[3])) {
        if (this.mixtape) {
          Tapes.get().background = Backdrop.IDS[i];
          Tapes.get().save();
        } else {
          MatchData.get().background = Backdrop.IDS[i];
          MatchData.get().save();
        }
        G.click();
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
