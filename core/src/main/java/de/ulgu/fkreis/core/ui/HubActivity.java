package de.ulgu.fkreis.core.ui;

import de.ulgu.fkreis.core.L;
import de.ulgu.fkreis.core.gfx.G;
import de.ulgu.fkreis.core.net.Net;
import java.util.ArrayList;
import java.util.List;
import net.labymod.api.Laby;
import net.labymod.api.client.gui.screen.activity.AutoActivity;
import net.labymod.api.client.gui.screen.key.Key;
import net.labymod.api.client.render.matrix.Stack;

/** /fk – kleines Schnellmenue: alle Freundeskreis-Bereiche mit einem Klick. */
@AutoActivity
public class HubActivity extends FkActivity {

  private static final class Item {
    final String label;
    final int color;
    final Runnable action;
    float hover;

    Item(String label, int color, Runnable action) {
      this.label = label;
      this.color = color;
      this.action = action;
    }
  }

  private final List<Item> items = new ArrayList<>();

  public HubActivity() {
    this.items.add(new Item(L.t("hub.sticker"), 0xFF3A6AC8, () -> Screens.now(new StickerBookActivity())));
    this.items.add(new Item(L.t("hub.notebook"), 0xFF7A4A2A, () -> Screens.now(new NotebookActivity())));
    this.items.add(new Item(L.t("hub.calendar"), 0xFFC0392B, () -> Screens.now(new CalendarActivity())));
    this.items.add(new Item(L.t("hub.album"), 0xFFB8860B, () -> Screens.now(new AlbumActivity())));
    this.items.add(new Item(L.t("hub.profile"), 0xFF4A8A3A,
        () -> Screens.now(new ProfileScrollActivity(Net.self(), Laby.labyAPI().getName()))));
    this.items.add(new Item(L.t("hub.coins"), 0xFF8A5AB8, () -> Screens.now(new CoinGalleryActivity())));
    this.items.add(new Item(L.t("hub.diary"), 0xFF8A5630, () -> Screens.now(new de.ulgu.fkreis.core.diary.DiaryActivity(false))));
    this.items.add(new Item(L.t("hub.mixtape"), 0xFF3A8FD8, () -> Screens.now(new de.ulgu.fkreis.core.mixtape.MixtapeActivity())));
    this.items.add(new Item(L.t("hub.match"), 0xFF2E9A8F, () -> Screens.now(new de.ulgu.fkreis.core.match.MatchActivity())));
    this.items.add(new Item(L.t("hub.vanish"), 0xFF7A1F1F, () -> {
      Screens.close();
      de.ulgu.fkreis.core.vanish.ui.Screens.openList();
    }));
    G.click();
  }

  private float[] rect(int i) {
    float bw = 150, bh = 26, gap = 8;
    float total = 2 * bw + gap;
    float x0 = this.x + (this.w - total) / 2F;
    int rows = (this.items.size() + 1) / 2;
    float y0 = this.y + this.h / 2F - (rows * bh + (rows - 1) * gap) / 2F + 8;
    return new float[] {x0 + (i % 2) * (bw + gap), y0 + (i / 2) * (bh + gap), bw, bh};
  }

  @Override
  protected void draw(Stack s, float mx, float my) {
    float open = G.easeOut(this.age() / 0.25F);
    G.rect(s, this.x, this.y, this.w, this.h, G.alpha(0xB0000000, open));
    float[] first = this.rect(0), last = this.rect(this.items.size() - 1);
    float px = first[0] - 14, py = first[1] - 40, pw = 2 * first[2] + 8 + 28, ph = last[1] + last[3] - first[1] + 62;
    G.rect(s, px + 3, py + 4, pw, ph, 0x60000000);
    G.rect(s, px, py, pw, ph, 0xF01C1F28);
    G.outline(s, px, py, pw, ph, 1, 0xFF3A4152);
    G.rect(s, px, py, pw, 2, 0xFFF2C94C);
    G.centered(s, "Freundeskreis", px + pw / 2F, py + 12, 0xFFF2C94C, 1.3F, true);
    for (int i = 0; i < this.items.size(); i++) {
      Item it = this.items.get(i);
      float[] r = this.rect(i);
      boolean over = G.in(mx, my, r[0], r[1], r[2], r[3]);
      it.hover = G.lerp(it.hover, over ? 1F : 0F, 0.3F);
      float lift = -1.5F * it.hover;
      G.rect(s, r[0], r[1] + lift, r[2], r[3], G.mix(0xFF2A2F3C, 0xFF3A4152, it.hover));
      G.rect(s, r[0], r[1] + lift, 4, r[3], it.color);
      G.rect(s, r[0], r[1] + r[3] - 1 + lift, r[2], 1, 0xFF12141C);
      G.text(s, it.label, r[0] + 12, r[1] + 9 + lift, 0xFFFFFFFF, 1.05F, true);
    }
    G.centered(s, "ULGU Software", px + pw / 2F, py + ph - 12, 0xFF5A6070, 0.8F, false);
  }

  @Override
  protected boolean onClick(float mx, float my, boolean left) {
    for (int i = 0; i < this.items.size(); i++) {
      float[] r = this.rect(i);
      if (G.in(mx, my, r[0], r[1], r[2], r[3])) {
        G.click();
        this.items.get(i).action.run();
        return true;
      }
    }
    Screens.close();
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
