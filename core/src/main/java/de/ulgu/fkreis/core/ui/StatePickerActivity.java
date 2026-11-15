package de.ulgu.fkreis.core.ui;

import de.ulgu.fkreis.core.FkreisAddon;
import de.ulgu.fkreis.core.L;
import de.ulgu.fkreis.core.gfx.G;
import net.labymod.api.client.gui.screen.activity.AutoActivity;
import net.labymod.api.client.render.matrix.Stack;

/**
 * Einstellungs-Tab "Bundesland waehlen": alle 16 deutschen Bundeslaender zum Anklicken.
 * Wer woanders herkommt, tippt sein Bundesland / seinen Staat einfach oben ins Textfeld.
 */
@AutoActivity
public class StatePickerActivity extends FkActivity {

  public static final String[] STATES = {
      "Baden-Württemberg", "Bayern", "Berlin", "Brandenburg", "Bremen", "Hamburg", "Hessen",
      "Mecklenburg-Vorpommern", "Niedersachsen", "Nordrhein-Westfalen", "Rheinland-Pfalz", "Saarland",
      "Sachsen", "Sachsen-Anhalt", "Schleswig-Holstein", "Thüringen"};

  private float[] cell(int i) {
    int cols = 4;
    float gx = this.x + 22, gw = this.w - 44, top = this.y + 50;
    float cw = gw / cols, ch = 22;
    return new float[] {gx + (i % cols) * cw + 2, top + (i / cols) * (ch + 6), cw - 4, ch};
  }

  @Override
  protected void draw(Stack s, float mx, float my) {
    String cur = FkreisAddon.get().configuration().profile().state().get().trim();
    G.text(s, L.t("state.intro"), this.x + 24, this.y + 34, 0xFFB8BCC8, 1F);
    for (int i = 0; i < STATES.length; i++) {
      float[] r = this.cell(i);
      boolean sel = STATES[i].equalsIgnoreCase(cur);
      boolean over = G.in(mx, my, r[0], r[1], r[2], r[3]);
      G.rect(s, r[0], r[1], r[2], r[3], sel ? 0xFF3A4A2A : over ? 0xFF2E3446 : 0xFF1C2130);
      G.outline(s, r[0], r[1], r[2], r[3], 1, sel ? 0xFF8AD06A : 0xFF3A4152);
      G.centered(s, G.fit(STATES[i], r[2] - 8, 1F), r[0] + r[2] / 2F, r[1] + 7, sel ? 0xFFB8F0A0 : 0xFFFFFFFF, 1F, false);
    }
    float[] last = this.cell(STATES.length - 1);
    G.text(s, L.t("state.custom"), this.x + 24, last[1] + 36, 0xFF9AA0AA, 0.95F);
  }

  @Override
  protected boolean onClick(float mx, float my, boolean left) {
    for (int i = 0; i < STATES.length; i++) {
      float[] r = this.cell(i);
      if (G.in(mx, my, r[0], r[1], r[2], r[3])) {
        FkreisAddon.get().configuration().profile().state().set(STATES[i]);
        G.click();
        return true;
      }
    }
    return false;
  }
}
