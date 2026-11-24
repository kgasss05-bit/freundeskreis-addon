package de.ulgu.fkreis.core.ui;

import de.ulgu.fkreis.core.FkreisAddon;
import de.ulgu.fkreis.core.L;
import de.ulgu.fkreis.core.data.Sticker;
import de.ulgu.fkreis.core.gfx.G;
import de.ulgu.fkreis.core.net.StickerShare;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import net.labymod.api.client.gui.screen.key.Key;
import net.labymod.api.client.gui.screen.activity.AutoActivity;
import net.labymod.api.client.render.matrix.Stack;

/**
 * Sammelbuch: dunkelblauer Nachthimmel mit diagonalem Lichtstreifen und ziehenden Pixelwolken,
 * aufgeschlagenes braunes Album. Links Titel "Sammelbuch", rechts "N Sticker gesammelt",
 * je Seite 3 x 3 Felder (Sticker = weisse Karte mit Kopf + Name, leer = gestrichelt mit "?").
 */
@AutoActivity
public class StickerBookActivity extends FkActivity {

  private static final int PER_PAGE = 9;
  private static final int PER_SPREAD = 18;
  private static final int COVER = 0xFF7A3E1F;

  private final BookFrame book = new BookFrame();
  private final float[] hover = new float[PER_SPREAD];
  private int spread;

  /** Bei einem fremden Sammelbuch: wem es gehoert und was die Person geschickt hat. Sonst null. */
  private final String ownerName;
  private final StickerShare.Remote remote;

  public StickerBookActivity() {
    this.ownerName = null;
    this.remote = null;
    G.sound("item.book.page_turn", 0.9F);
  }

  /** Sammelbuch einer anderen Person ansehen (nur lesen). */
  public StickerBookActivity(UUID owner, String ownerName) {
    this.ownerName = ownerName;
    this.remote = FkreisAddon.get().net().stickerShare().request(FkreisAddon.get().net(), owner);
    G.sound("item.book.page_turn", 0.9F);
  }

  private List<Sticker> stickers() {
    return this.remote != null ? this.remote.stickers : FkreisAddon.get().store().stickers;
  }

  private int spreads() {
    return Math.max(1, (this.stickers().size() + PER_SPREAD - 1) / PER_SPREAD);
  }

  private void layout() {
    float bw = Math.min(400, this.w - 40);
    float bh = Math.min(260, this.h - 50);
    this.book.layout(this.x + (this.w - bw) / 2F, this.y + (this.h - bh) / 2F - 4, bw, bh, 8);
  }

  private float[] slot(int local) {
    boolean right = local >= PER_PAGE;
    int k = local % PER_PAGE;
    int col = k % 3, row = k / 3;
    float px = right ? this.book.rightX : this.book.leftX;
    float top = this.book.pageY + 30;
    float gap = 8;
    float sw = Math.min(50, (this.book.pageW - 2 * 14 - 2 * gap) / 3F);
    float sh = Math.min(sw * 1.25F, (this.book.pageH - 30 - 10 - 2 * gap) / 3F);
    float gridW = 3 * sw + 2 * gap;
    float sx = px + (this.book.pageW - gridW) / 2F + col * (sw + gap);
    float sy = top + row * (sh + gap);
    return new float[] {sx, sy, sw, sh};
  }

  @Override
  protected void draw(Stack s, float mx, float my) {
    this.layout();
    this.sky(s);

    float cx = this.book.x + this.book.w / 2F;

    this.book.drawBase(s, COVER);
    G.centered(s, L.t("sticker.title"), this.book.leftX + this.book.pageW / 2F, this.book.pageY + 10, 0xFF3A2A1A, 1.55F, false);
    String sub = L.t("sticker.count", this.stickers().size());
    if (this.remote != null) {
      G.centered(s, G.fit(L.t("sticker.of", this.ownerName), this.book.pageW - 10, 0.85F), this.book.leftX + this.book.pageW / 2F, this.book.pageY + 24, 0xFF7A5A3A, 0.85F, false);
      if (this.remote.state == StickerShare.State.LOADING) {
        sub = L.t("sticker.loading");
      } else if (this.remote.state == StickerShare.State.PRIVATE) {
        sub = L.t("sticker.private");
      } else if (this.remote.state == StickerShare.State.NO_REPLY) {
        sub = L.t("sticker.noReply");
      }
    }
    G.centered(s, G.fit(sub, this.book.pageW - 10, 1.1F), this.book.rightX + this.book.pageW / 2F, this.book.pageY + 12, 0xFF7A5A3A, 1.1F, false);

    List<Sticker> list = this.stickers();
    int from = this.spread * PER_SPREAD;
    String tooltip = null;
    float tipX = 0, tipY = 0;
    for (int local = 0; local < PER_SPREAD; local++) {
      float[] r = this.slot(local);
      int idx = from + local;
      if (idx >= list.size()) {
        G.dashed(s, r[0], r[1], r[2], r[3], 0xFFCBB894);
        G.centered(s, "?", r[0] + r[2] / 2F, r[1] + r[3] / 2F - 7, 0xFFC9A27A, 1.6F, false);
        continue;
      }
      Sticker st = list.get(idx);
      boolean over = G.in(mx, my, r[0], r[1], r[2], r[3]);
      this.hover[local] = G.lerp(this.hover[local], over ? 1F : 0F, 0.25F);
      if (over) {
        tooltip = L.t("sticker.date", new SimpleDateFormat("dd.MM.yyyy").format(new Date(st.time)));
        tipX = mx;
        tipY = my;
      }
      this.sticker(s, st, r, this.hover[local], local);
    }
    this.book.drawFlip(s);

    if (this.remote != null && this.remote.state == StickerShare.State.NO_REPLY) {
      float hy = this.book.y + this.book.h + 18;
      for (String line : G.wrap(L.t("sticker.noReplyHint", this.ownerName), Math.min(this.w - 30, 380), 0.9F)) {
        G.centered(s, line, cx, hy, 0xFFE8DCC0, 0.9F, true);
        hy += 10;
      }
    }

    // Seitenzahl unter dem Buch + Pfeile
    int sp = this.spreads();
    float by = this.book.y + this.book.h + 4;
    G.centered(s, (this.spread + 1) + " / " + sp, cx, by, 0xFFE8DCC0, 1F, true);
    if (this.spread > 0) {
      G.centered(s, "‹", cx - 30, by - 3, G.in(mx, my, cx - 38, by - 4, 16, 14) ? 0xFFFFE08A : 0xFFE8DCC0, 1.6F, true);
    }
    if (this.spread < sp - 1) {
      G.centered(s, "›", cx + 30, by - 3, G.in(mx, my, cx + 22, by - 4, 16, 14) ? 0xFFFFE08A : 0xFFE8DCC0, 1.6F, true);
    }

    if (tooltip != null) {
      float tw = G.width(tooltip) + 8;
      G.rect(s, tipX + 8, tipY - 14, tw, 13, 0xE0101420);
      G.text(s, tooltip, tipX + 12, tipY - 11, 0xFFFFFFFF, 1F);
    }
  }

  private void sticker(Stack s, Sticker st, float[] r, float hover, int local) {
    // ohne Drehen/Zoomen (sonst verschieben sich Flaechen und Text) – Hover hebt den Sticker leicht an
    float lx = r[0], ly = r[1] - 2F * hover;
    float cx = r[0] + r[2] / 2F;
    G.rect(s, lx + 2, ly + 3, r[2], r[3], 0x40000000);
    G.rect(s, lx, ly, r[2], r[3], 0xFFFFFFFF);
    G.rect(s, lx, ly + r[3] - 1, r[2], 1, 0xFFE2DED4);
    float head = r[2] - 12;
    G.icon(s, G.head(UUID.fromString(st.uuid)), lx + 6, ly + 5, head, head);
    float scale = 0.85F;
    G.centered(s, G.fit(st.name, r[2] - 4, scale), cx, ly + head + 9, 0xFF3A3A3A, scale, false);
  }

  /** Nachthimmel: Grundfarbe, diagonaler hellerer Streifen, Pixelwolken die langsam ziehen. */
  private void sky(Stack s) {
    G.rect(s, this.x, this.y, this.w, this.h, 0xFF16234A);
    for (float yy = 0; yy < this.h; yy += 3) {
      float start = this.w * 0.75F - yy * 0.9F;
      G.rect(s, this.x + Math.max(0, start), this.y + yy, Math.max(0, Math.min(this.w, this.w * 0.45F + Math.min(0, start))), 3, 0xFF1A2B57);
    }
    float t = this.age();
    this.cloud(s, ((this.w * 0.14F + t * 6F) % (this.w + 80)) - 40, this.h * 0.06F, 1F);
    this.cloud(s, ((this.w * 0.48F + t * 4F) % (this.w + 80)) - 40, this.h * 0.02F, 1.4F);
    this.cloud(s, ((this.w * 0.82F + t * 5F) % (this.w + 80)) - 40, this.h * 0.08F, 0.9F);
  }

  private void cloud(Stack s, float cx, float cy, float size) {
    float u = 6 * size;
    float x0 = this.x + cx, y0 = this.y + cy;
    G.rect(s, x0 + u, y0, u * 3, u, 0xFF5E5E5E);
    G.rect(s, x0, y0 + u, u * 5, u, 0xFF5A5A5A);
    G.rect(s, x0, y0 + u * 2, u * 5, u * 0.5F, 0xFF484848);
  }

  private void flipTo(int dir) {
    int target = this.spread + dir;
    if (target < 0 || target >= this.spreads() || this.book.flipping()) {
      return;
    }
    this.spread = target;
    this.book.flip(dir);
    G.sound("item.book.page_turn", 1F);
  }

  @Override
  protected boolean onClick(float mx, float my, boolean left) {
    float cx = this.book.x + this.book.w / 2F;
    float by = this.book.y + this.book.h + 4;
    if (G.in(mx, my, cx - 38, by - 4, 16, 14)) {
      this.flipTo(-1);
      return true;
    }
    if (G.in(mx, my, cx + 22, by - 4, 16, 14)) {
      this.flipTo(1);
      return true;
    }
    return false;
  }

  @Override
  protected boolean onScroll(float mx, float my, float delta) {
    this.flipTo(delta < 0 ? 1 : -1);
    return true;
  }

  @Override
  protected boolean onKey(Key key) {
    if (key == Key.ARROW_RIGHT) {
      this.flipTo(1);
      return true;
    }
    if (key == Key.ARROW_LEFT) {
      this.flipTo(-1);
      return true;
    }
    return false;
  }
}
