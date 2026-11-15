package de.ulgu.fkreis.core.ui;

import de.ulgu.fkreis.core.FkreisAddon;
import de.ulgu.fkreis.core.L;
import de.ulgu.fkreis.core.data.Mob;
import de.ulgu.fkreis.core.data.Mobs;
import de.ulgu.fkreis.core.gfx.G;
import java.util.ArrayList;
import java.util.List;
import net.labymod.api.client.gui.screen.key.Key;
import net.labymod.api.client.gui.screen.activity.AutoActivity;
import net.labymod.api.client.render.matrix.Stack;

/** Mob-Identifizierung: durchsuchbare 3-Spalten-Liste aller Mobs mit offiziellen Spawn-Ei-Icons. */
@AutoActivity
public class MobPickerActivity extends FkActivity {

  private static final float ROW = 22;
  private static final float GAP = 2;

  private String query = "";
  private float scroll;
  private float scrollAnim;
  private final float[] hoverAnim = new float[Mobs.all().size()];

  private List<Mob> filtered() {
    if (this.query.isEmpty()) {
      return Mobs.all();
    }
    List<Mob> out = new ArrayList<>();
    for (Mob m : Mobs.all()) {
      if (m.matches(this.query)) {
        out.add(m);
      }
    }
    return out;
  }

  private float gx() { return this.x + 22; }
  private float gw() { return this.w - 44; }
  private float gy() { return this.y + 80; }
  private float gh() { return this.h - 90; }

  @Override
  protected void draw(Stack s, float mx, float my) {
    // Kopfzeile ("X Mob-Identifizierung") zeichnet LabyMod in den Einstellungen selbst.
    Mob current = Mobs.byId(FkreisAddon.get().configuration().profile().mob().get());
    G.text(s, L.raw("fkreis.settings.profile.mobPicker.name") + ": " + current.name(), this.gx(), this.y + 46, 0xFFFFFFFF, 1.05F);
    String hint = L.t("mob.scroll");
    G.text(s, hint, this.gx() + this.gw() - G.width(hint) * 1.05F, this.y + 46, 0xFF8A8F99, 1.05F);

    // Suchfeld mit gruenem Rahmen und blinkendem Cursor
    float sy = this.y + 58;
    G.rect(s, this.gx(), sy, this.gw(), 18, 0xFF10131A);
    G.outline(s, this.gx(), sy, this.gw(), 18, 1, 0xFF4ADE80);
    boolean blink = (System.currentTimeMillis() / 500) % 2 == 0;
    G.text(s, L.t("mob.search") + " " + this.query + (blink ? "_" : ""), this.gx() + 6, sy + 5, 0xFF9AA0AA, 1F);

    // Liste
    List<Mob> list = this.filtered();
    int cols = 3;
    float cw = (this.gw() - GAP * (cols - 1)) / cols;
    int rows = (list.size() + cols - 1) / cols;
    float maxScroll = Math.max(0, rows * (ROW + GAP) - this.gh());
    this.scroll = Math.max(0, Math.min(maxScroll, this.scroll));
    this.scrollAnim = G.lerp(this.scrollAnim, this.scroll, 0.35F);

    float top = this.gy();
    for (int i = 0; i < list.size(); i++) {
      Mob mob = list.get(i);
      int c = i % cols, r = i / cols;
      float cx = this.gx() + c * (cw + GAP);
      float cy = top + r * (ROW + GAP) - this.scrollAnim;
      if (cy + ROW < top || cy > top + this.gh()) {
        continue;
      }
      int idx = Mobs.all().indexOf(mob);
      boolean hover = G.in(mx, my, cx, cy, cw, ROW) && my >= top && my <= top + this.gh();
      this.hoverAnim[idx] = G.lerp(this.hoverAnim[idx], hover ? 1F : 0F, 0.3F);
      boolean sel = mob == current;
      int bg = G.mix(0xFF252A36, 0xFF3B4254, this.hoverAnim[idx]);
      G.rect(s, cx, cy, cw, ROW, bg);
      if (sel) {
        G.rect(s, cx, cy, 2, ROW, 0xFF4ADE80);
      }
      float iconSize = 16 + 2 * this.hoverAnim[idx];
      G.icon(s, mob.icon(), cx + 8 - this.hoverAnim[idx], cy + (ROW - iconSize) / 2F, iconSize, iconSize);
      G.text(s, G.fit(mob.name(), cw - 36, 1.05F), cx + 30, cy + 7, sel ? 0xFF4ADE80 : 0xFFFFFFFF, 1.05F);
    }

    // Scrollbalken
    if (maxScroll > 0) {
      float barH = Math.max(20, this.gh() * this.gh() / (rows * (ROW + GAP)));
      float barY = top + (this.gh() - barH) * (this.scrollAnim / maxScroll);
      G.rect(s, this.gx() + this.gw() + 4, barY, 3, barH, 0xFF5A6070);
    }
  }

  @Override
  protected boolean onClick(float mx, float my, boolean left) {
    if (my < this.gy() || my > this.gy() + this.gh()) {
      return false;
    }
    List<Mob> list = this.filtered();
    int cols = 3;
    float cw = (this.gw() - GAP * (cols - 1)) / cols;
    for (int i = 0; i < list.size(); i++) {
      int c = i % cols, r = i / cols;
      float cx = this.gx() + c * (cw + GAP);
      float cy = this.gy() + r * (ROW + GAP) - this.scrollAnim;
      if (G.in(mx, my, cx, cy, cw, ROW)) {
        FkreisAddon.get().configuration().profile().mob().set(list.get(i).id());
        G.click();
        return true;
      }
    }
    return false;
  }

  @Override
  protected boolean onScroll(float mx, float my, float delta) {
    this.scroll -= delta * 24;
    return true;
  }

  @Override
  protected boolean onKey(Key key) {
    if (key == Key.BACK && !this.query.isEmpty()) {
      this.query = this.query.substring(0, this.query.length() - 1);
      this.scroll = 0;
      return true;
    }
    return false;
  }

  @Override
  protected boolean onChar(char c) {
    if (Character.isLetterOrDigit(c) || c == ' ' || c == '-') {
      if (this.query.length() < 24) {
        this.query += c;
        this.scroll = 0;
      }
      return true;
    }
    return false;
  }
}
