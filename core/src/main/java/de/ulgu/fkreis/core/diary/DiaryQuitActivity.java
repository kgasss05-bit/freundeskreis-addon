package de.ulgu.fkreis.core.diary;

import de.ulgu.fkreis.api.DiaryState;
import de.ulgu.fkreis.core.FkreisAddon;
import de.ulgu.fkreis.core.L;
import de.ulgu.fkreis.core.gfx.G;
import de.ulgu.fkreis.core.ui.FkActivity;
import de.ulgu.fkreis.core.ui.Screens;
import net.labymod.api.client.gui.screen.activity.AutoActivity;
import net.labymod.api.client.gui.screen.key.Key;
import net.labymod.api.client.render.matrix.Stack;

/** Frage beim Beenden: "Moechtest du vorher noch einen Tagebucheintrag machen?" */
@AutoActivity
public class DiaryQuitActivity extends FkActivity {

  private boolean never;
  private boolean answered;
  private float px, py, pw, ph;

  /** true = das Spiel wird gerade beendet (wartet auf die Antwort); false = nur Nachfrage nach dem Server-Verlassen. */
  private final boolean quitting;

  public DiaryQuitActivity(boolean quitting) {
    this.quitting = quitting;
    if (quitting) {
      DiaryState.screens++;
    }
    G.sound("block.note_block.pling", 0.8F);
  }

  private float[] yes() {
    return new float[] {this.px + 14, this.py + 52, (this.pw - 36) / 2F, 20};
  }

  private float[] no() {
    float bw = (this.pw - 36) / 2F;
    return new float[] {this.px + 22 + bw, this.py + 52, bw, 20};
  }

  private float[] box() {
    return new float[] {this.px + 14, this.py + 82, 9, 9};
  }

  @Override
  protected void draw(Stack s, float mx, float my) {
    G.rect(s, this.x, this.y, this.w, this.h, 0xC0000000);
    this.pw = Math.min(this.w - 30, 250);
    this.ph = 104;
    this.px = this.x + (this.w - this.pw) / 2F;
    this.py = this.y + (this.h - this.ph) / 2F;
    G.rect(s, this.px + 3, this.py + 4, this.pw, this.ph, 0x60000000);
    G.rect(s, this.px, this.py, this.pw, this.ph, 0xF01C1F28);
    G.outline(s, this.px, this.py, this.pw, this.ph, 1, 0xFF3A4152);
    G.rect(s, this.px, this.py, this.pw, 2, 0xFFF2C94C);
    G.centered(s, L.t("diary.quit.title"), this.px + this.pw / 2F, this.py + 11, 0xFFF2C94C, 1.2F, true);
    int i = 0;
    for (String line : G.wrap(L.t("diary.quit.text"), this.pw - 24, 1F)) {
      G.centered(s, line, this.px + this.pw / 2F, this.py + 28 + i++ * 10, 0xFFFFFFFF, 1F, false);
    }
    this.button(s, this.yes(), L.t("diary.quit.yes"), mx, my, true);
    this.button(s, this.no(), L.t(this.quitting ? "diary.quit.no" : "diary.quit.later"), mx, my, false);
    float[] b = this.box();
    G.rect(s, b[0], b[1], b[2], b[3], 0xFF12141C);
    G.outline(s, b[0], b[1], b[2], b[3], 1, 0xFF3A4152);
    if (this.never) {
      G.rect(s, b[0] + 2, b[1] + 2, 5, 5, 0xFFF2C94C);
    }
    G.text(s, L.t("diary.quit.never"), b[0] + 14, b[1] + 1, 0xFF9AA3B5, 0.85F, false);
  }

  private void button(Stack s, float[] r, String label, float mx, float my, boolean gold) {
    boolean over = G.in(mx, my, r[0], r[1], r[2], r[3]);
    G.rect(s, r[0], r[1], r[2], r[3], over ? 0xFF384052 : 0xFF2A2F3C);
    G.outline(s, r[0], r[1], r[2], r[3], 1, gold ? 0xFFF2C94C : 0xFF3A4152);
    G.rect(s, r[0], r[1] + r[3] - 2, r[2], 2, gold ? 0xFFB8860B : 0xFF12141C);
    G.centered(s, label, r[0] + r[2] / 2F, r[1] + 6, gold ? 0xFFF2C94C : 0xFFFFFFFF, 1F, true);
  }

  private void leave() {
    this.answered = true;
    if (this.never) {
      FkreisAddon.get().configuration().diary().askOnQuit().set(false);
    }
    if (!this.quitting) {
      Screens.close();
      return;
    }
    DiaryState.screens = Math.max(0, DiaryState.screens - 1);
    DiaryState.quitNow = true;
  }

  /** Doch im Spiel bleiben. */
  private void stay() {
    this.answered = true;
    if (this.quitting) {
      DiaryState.screens = Math.max(0, DiaryState.screens - 1);
      DiaryState.busy = false;
    }
    Screens.close();
  }

  @Override
  protected boolean onClick(float mx, float my, boolean left) {
    float[] y = this.yes(), n = this.no(), b = this.box();
    if (G.in(mx, my, y[0], y[1], y[2], y[3])) {
      G.click();
      this.answered = true;
      if (!this.quitting) {
        Screens.now(new DiaryActivity(false));
        return true;
      }
      DiaryActivity book = new DiaryActivity(true); // zaehlt sich selbst als offenen Bildschirm
      DiaryState.screens = Math.max(1, DiaryState.screens - 1);
      Screens.now(book);
      return true;
    }
    if (G.in(mx, my, n[0], n[1], n[2], n[3])) {
      G.click();
      this.leave();
      return true;
    }
    if (G.in(mx, my, b[0] - 2, b[1] - 2, this.pw - 28, 13)) {
      G.click();
      this.never = !this.never;
      return true;
    }
    return true;
  }

  @Override
  protected boolean onKey(Key key) {
    if (key == Key.ESCAPE) {
      this.stay();
      return true;
    }
    return false;
  }

  @Override
  public void onCloseScreen() {
    super.onCloseScreen();
    if (!this.answered) {
      this.answered = true;
      if (this.quitting) {
        DiaryState.screens = Math.max(0, DiaryState.screens - 1);
        DiaryState.busy = false;
      }
    }
  }
}
