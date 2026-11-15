package de.ulgu.fkreis.core.ui;

import de.ulgu.fkreis.core.FkreisAddon;
import de.ulgu.fkreis.core.L;
import de.ulgu.fkreis.core.data.Frame;
import de.ulgu.fkreis.core.data.Photo;
import de.ulgu.fkreis.core.data.Store;
import de.ulgu.fkreis.core.data.WoodType;
import de.ulgu.fkreis.core.gfx.G;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import net.labymod.api.client.gui.screen.activity.AutoActivity;
import net.labymod.api.client.gui.screen.key.Key;
import net.labymod.api.client.render.matrix.Stack;

/**
 * Fotoalbum: aufgeschlagenes Buch, 2 Fotos pro Seite (4 pro Doppelseite), jedes im gewaehlten Rahmen
 * mit kleinem Text darunter. Klick = gross ansehen, Foto halten und auf den Papierkorb ziehen = loeschen,
 * gestricheltes Feld = neues Foto aus den Screenshots einkleben.
 */
@AutoActivity
public class AlbumActivity extends FkActivity {

  private static final int PER_SPREAD = 4;

  private int spread;
  private int pressed = -1;
  private float pressX, pressY;
  private boolean dragging;
  private float dragX, dragY;
  private int viewing = -1;
  private float trashAnim;

  private float bx, by, bw, bh, pageW, leftX, rightX, pageY, pageH;

  /** null = eigenes Album; sonst das Album dieser Person (nur ansehen). */
  private final java.util.UUID owner;
  private final String ownerName;

  public AlbumActivity() {
    this(null, null);
  }

  public AlbumActivity(java.util.UUID owner, String ownerName) {
    this.owner = owner;
    this.ownerName = ownerName;
    if (owner != null) {
      FkreisAddon.get().net().album().request(FkreisAddon.get().net(), owner);
    }
    G.click();
    List<Photo> p = this.photos();
    this.spread = Math.max(0, (p.size()) / PER_SPREAD);
    if (p.size() % PER_SPREAD == 0 && p.size() > 0) {
      this.spread = Math.max(0, this.spread - 1);
    }
    this.spread = Math.min(this.spread, this.spreads() - 1);
  }

  private boolean remote() {
    return this.owner != null;
  }

  private List<Photo> photos() {
    if (!this.remote()) {
      return FkreisAddon.get().store().photos;
    }
    List<Photo> l = new java.util.ArrayList<>();
    de.ulgu.fkreis.core.net.AlbumShare.Remote r = FkreisAddon.get().net().album().remote(this.owner);
    if (r != null) {
      for (de.ulgu.fkreis.core.net.AlbumShare.RemotePhoto rp : r.list()) {
        l.add(new Photo(rp.file.toAbsolutePath().toString(), rp.frame, rp.caption));
      }
    }
    return l;
  }

  public static Path albumDir() {
    return Store.dir().resolve("album");
  }

  private int spreads() {
    int n = this.photos().size();
    if (this.remote()) {
      return Math.max(1, (n + PER_SPREAD - 1) / PER_SPREAD);
    }
    return Math.max(1, n / PER_SPREAD + 1);
  }

  private void layout() {
    this.bw = Math.min(470, this.w * 0.5F);
    this.bh = Math.min(this.h - 60, this.bw * 0.62F);
    this.bx = this.x + (this.w - this.bw) / 2F;
    this.by = this.y + (this.h - this.bh) / 2F;
    float border = 6, spine = 8;
    this.pageW = (this.bw - 2 * border - spine) / 2F;
    this.leftX = this.bx + border;
    this.rightX = this.bx + border + this.pageW + spine;
    this.pageY = this.by + border;
    this.pageH = this.bh - 2 * border;
  }

  /** Rahmen-Rechteck fuer Platz 0..3 (0,1 linke Seite oben/unten, 2,3 rechte Seite). */
  private float[] slot(int local) {
    float px = local < 2 ? this.leftX : this.rightX;
    int row = local % 2;
    float slotH = this.pageH / 2F;
    float capH = 12;
    float fh = slotH - capH - 12;
    float fw = Math.min(this.pageW - 24, fh * 16F / 9F + 8);
    fh = Math.min(fh, (fw - 8) * 9F / 16F + 8);
    float fx = px + (this.pageW - fw) / 2F;
    float fy = this.pageY + row * slotH + 6;
    return new float[] {fx, fy, fw, fh};
  }

  private float[] trashRect() {
    float size = 34;
    return new float[] {this.x + this.w - size - 12, this.y + this.h - size - 10, size, size};
  }

  private float[] arrow(boolean right) {
    float cx = this.bx + this.bw / 2F;
    float ay = this.by + this.bh + 6;
    return right ? new float[] {cx + 22, ay, 16, 14} : new float[] {cx - 38, ay, 16, 14};
  }

  private Path file(Photo p) {
    return this.remote() ? java.nio.file.Paths.get(p.file) : albumDir().resolve(p.file);
  }

  @Override
  protected void draw(Stack s, float mx, float my) {
    this.layout();
    WoodType wood = WoodType.parse(FkreisAddon.get().configuration().notebook().wood().get());
    G.planks(s, this.x, this.y, this.w, this.h, wood.plank, 7);
    G.centered(s, this.remote() ? L.t("album.of", this.ownerName) : L.t("album.title"), this.x + this.w / 2F,
        this.by - 18, 0xFFF1E6CA, 1.4F, true);
    if (this.remote()) {
      de.ulgu.fkreis.core.net.AlbumShare.Remote r = FkreisAddon.get().net().album().remote(this.owner);
      String status = null;
      if (r == null || (r.total < 0 && System.currentTimeMillis() - r.started > 8000)) {
        status = L.t("album.noanswer", this.ownerName);
      } else if (r.denied) {
        status = L.t("album.private", this.ownerName);
      } else if (r.total == 0) {
        status = L.t("album.empty", this.ownerName);
      } else if (r.total < 0 || r.received() < r.total) {
        status = L.t("album.receiving", Math.max(0, r.received()), Math.max(0, r.total));
      }
      if (status != null) {
        G.centered(s, status, this.x + this.w / 2F, this.by - 6, 0xFFCFC6B0, 0.85F, true);
      }
    }


    // Einband dunkelrot, Seiten dunkles Fotoalbum-Papier
    G.rect(s, this.bx + 4, this.by + 5, this.bw, this.bh, 0x50000000);
    G.rect(s, this.bx, this.by, this.bw, this.bh, 0xFF5A2A2A);
    G.outline(s, this.bx, this.by, this.bw, this.bh, 1, 0xFF2E1414);
    G.rect(s, this.leftX, this.pageY, this.pageW, this.pageH, 0xFF2B2E36);
    G.rect(s, this.rightX, this.pageY, this.pageW, this.pageH, 0xFF2E313A);
    G.rect(s, this.leftX + this.pageW - 6, this.pageY, 6, this.pageH, 0x30000000);
    G.rect(s, this.rightX, this.pageY, 8, this.pageH, 0x30000000);

    List<Photo> ph = this.photos();
    int from = this.spread * PER_SPREAD;
    for (int i = from; i < Math.min(ph.size(), from + PER_SPREAD); i++) {
      if (i == this.pressed && this.dragging) {
        continue;
      }
      float[] r = this.slot(i - from);
      boolean over = !this.dragging && this.viewing < 0 && G.in(mx, my, r[0], r[1], r[2], r[3]);
      float lift = over ? 1.5F : 0F;
      Photo p = ph.get(i);
      PhotoDraw.framed(s, this.file(p), Frame.parse(p.frame), r[0], r[1] - lift, r[2], r[3], 400);
      String cap = p.caption == null || p.caption.isEmpty() ? "" : G.fit(p.caption, this.pageW - 12, 0.9F);
      G.centered(s, cap, r[0] + r[2] / 2F, r[1] + r[3] + 4, 0xFFF1E6CA, 0.9F, false);
    }
    // "+ Foto"-Feld
    int idx = ph.size();
    if (!this.remote() && idx / PER_SPREAD == this.spread && !this.dragging) {
      float[] r = this.slot(idx % PER_SPREAD);
      boolean over = G.in(mx, my, r[0], r[1], r[2], r[3]);
      if (over) {
        G.rect(s, r[0], r[1], r[2], r[3], 0x18FFFFFF);
      }
      G.dashed(s, r[0], r[1], r[2], r[3], over ? 0xFFF1E6CA : 0xFF9A9284);
      G.centered(s, "+", r[0] + r[2] / 2F, r[1] + r[3] / 2F - 12, 0xFFC8BFA8, 1.4F, false);
      G.centered(s, L.t("album.add"), r[0] + r[2] / 2F, r[1] + r[3] / 2F + 2, 0xFFC8BFA8, 0.95F, false);
    }

    int sp = this.spreads();
    if (sp > 1) {
      float ccx = this.bx + this.bw / 2F;
      G.centered(s, (this.spread + 1) + " / " + sp, ccx, this.by + this.bh + 9, 0xFFF1E6CA, 1F, true);
      if (this.spread > 0) {
        float[] al = this.arrow(false);
        G.centered(s, "‹", al[0] + 8, al[1], G.in(mx, my, al[0], al[1], al[2], al[3]) ? 0xFFFFE08A : 0xFFF1E6CA, 1.5F, true);
      }
      if (this.spread < sp - 1) {
        float[] ar = this.arrow(true);
        G.centered(s, "›", ar[0] + 8, ar[1], G.in(mx, my, ar[0], ar[1], ar[2], ar[3]) ? 0xFFFFE08A : 0xFFF1E6CA, 1.5F, true);
      }
    }
    String back = "‹ " + L.t("notes.back.close") + "  (Esc)";
    boolean bh2 = G.in(mx, my, this.x + 8, this.y + this.h - 18, G.width(back) + 8, 14);
    G.text(s, back, this.x + 12, this.y + this.h - 15, bh2 ? 0xFFFFE08A : 0xFFFFFFFF, 1F, true);
    G.centered(s, this.remote() ? L.t("album.viewhint") : L.t("album.hint"), this.x + this.w / 2F,
        this.y + this.h - 15, 0xFFCFC6B0, 0.85F, true);

    // Papierkorb (nur im eigenen Album)
    float[] t = this.trashRect();
    if (!this.remote()) {
    boolean overTrash = this.dragging && G.in(mx, my, t[0] - 6, t[1] - 6, t[2] + 12, t[3] + 12);
    this.trashAnim = G.lerp(this.trashAnim, overTrash ? 1F : (this.dragging ? 0.45F : 0F), 0.25F);
    float grow = 4F * this.trashAnim;
    if (overTrash) {
      G.rect(s, t[0] - 4 - grow, t[1] - 4 - grow, t[2] + 8 + 2 * grow, t[3] + 8 + 2 * grow, 0x40E04040);
    }
    G.icon(s, overTrash ? G.TRASH_OPEN : G.TRASH, t[0] - grow, t[1] - grow, t[2] + 2 * grow, t[3] + 2 * grow);
    }

    // gezogenes Foto
    if (this.dragging && this.pressed >= 0 && this.pressed < ph.size()) {
      float[] r = this.slot(this.pressed % PER_SPREAD);
      float w2 = r[2] * 0.8F, h2 = r[3] * 0.8F;
      Photo p = ph.get(this.pressed);
      PhotoDraw.framed(s, this.file(p), Frame.parse(p.frame), this.dragX - w2 / 2F, this.dragY - h2 / 2F, w2, h2, 400);
    }

    // grosse Ansicht
    if (this.viewing >= 0 && this.viewing < ph.size()) {
      Photo p = ph.get(this.viewing);
      G.rect(s, this.x, this.y, this.w, this.h, 0xC0000000);
      float vw = Math.min(this.w - 60, (this.h - 70) * 16F / 9F), vh = vw * 9F / 16F + vw * 0.06F;
      float vx = this.x + (this.w - vw) / 2F, vy = this.y + (this.h - vh) / 2F - 10;
      PhotoDraw.framed(s, this.file(p), Frame.parse(p.frame), vx, vy, vw, vh, 1280);
      if (p.caption != null && !p.caption.isEmpty()) {
        G.centered(s, p.caption, this.x + this.w / 2F, vy + vh + 8, 0xFFFFFFFF, 1.3F, true);
      }
      G.centered(s, new java.text.SimpleDateFormat("dd.MM.yyyy").format(new java.util.Date(p.added)),
          this.x + this.w / 2F, vy + vh + 24, 0xFFB8BCC8, 0.9F, true);
    }
  }

  private int photoAt(float mx, float my) {
    int from = this.spread * PER_SPREAD;
    for (int i = from; i < Math.min(this.photos().size(), from + PER_SPREAD); i++) {
      float[] r = this.slot(i - from);
      if (G.in(mx, my, r[0], r[1], r[2], r[3])) {
        return i;
      }
    }
    return -1;
  }

  @Override
  protected boolean onClick(float mx, float my, boolean left) {
    if (this.viewing >= 0) {
      this.viewing = -1;
      return true;
    }
    this.layout();
    int sp = this.spreads();
    if (this.spread > 0 && G.in(mx, my, this.arrow(false)[0] - 3, this.arrow(false)[1] - 3, 22, 20)) {
      this.spread--;
      G.click();
      return true;
    }
    if (this.spread < sp - 1 && G.in(mx, my, this.arrow(true)[0] - 3, this.arrow(true)[1] - 3, 22, 20)) {
      this.spread++;
      G.click();
      return true;
    }
    int idx = this.photos().size();
    if (!this.remote() && idx / PER_SPREAD == this.spread) {
      float[] r = this.slot(idx % PER_SPREAD);
      if (G.in(mx, my, r[0], r[1], r[2], r[3])) {
        G.click();
        Screens.now(new PhotoPickerActivity());
        return true;
      }
    }
    int at = this.photoAt(mx, my);
    if (at >= 0) {
      this.pressed = at;
      this.pressX = mx;
      this.pressY = my;
      this.dragging = false;
      return true;
    }
    if (my > this.y + this.h - 20 && mx < this.x + 140) {
      Screens.close();
      return true;
    }
    return false;
  }

  @Override
  protected boolean onDrag(float mx, float my) {
    if (this.pressed < 0) {
      return false;
    }
    if (!this.remote() && !this.dragging && Math.abs(mx - this.pressX) + Math.abs(my - this.pressY) > 5) {
      this.dragging = true;
    }
    this.dragX = mx;
    this.dragY = my;
    return true;
  }

  @Override
  protected boolean onRelease(float mx, float my) {
    if (this.pressed < 0) {
      return false;
    }
    int p = this.pressed;
    boolean wasDrag = this.dragging;
    this.pressed = -1;
    this.dragging = false;
    if (!wasDrag) {
      this.viewing = p;
      G.click();
      return true;
    }
    float[] t = this.trashRect();
    if (G.in(mx, my, t[0] - 6, t[1] - 6, t[2] + 12, t[3] + 12) && p < this.photos().size()) {
      Photo removed = this.photos().remove(p);
      FkreisAddon.get().store().save();
      try {
        Files.deleteIfExists(this.file(removed));
      } catch (Exception ignored) {
      }
      G.sound("entity.item.pickup", 0.7F);
      this.spread = Math.min(this.spread, this.spreads() - 1);
    }
    return true;
  }

  @Override
  protected boolean onScroll(float mx, float my, float delta) {
    if (delta < 0 && this.spread < this.spreads() - 1) {
      this.spread++;
    } else if (delta > 0 && this.spread > 0) {
      this.spread--;
    }
    return true;
  }

  @Override
  protected boolean onKey(Key key) {
    if (key == Key.ESCAPE) {
      if (this.viewing >= 0) {
        this.viewing = -1;
      } else {
        Screens.close();
      }
      return true;
    }
    if (key == Key.ARROW_LEFT && this.spread > 0) {
      this.spread--;
      return true;
    }
    if (key == Key.ARROW_RIGHT && this.spread < this.spreads() - 1) {
      this.spread++;
      return true;
    }
    return false;
  }
}
