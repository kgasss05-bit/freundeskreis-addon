package de.ulgu.fkreis.core.ui;

import de.ulgu.fkreis.core.FkreisAddon;
import de.ulgu.fkreis.core.L;
import de.ulgu.fkreis.core.data.Store;
import de.ulgu.fkreis.core.data.WoodType;
import de.ulgu.fkreis.core.gfx.G;
import de.ulgu.fkreis.core.gfx.PixelFont;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.labymod.api.client.gui.screen.key.Key;
import net.labymod.api.client.gui.screen.activity.AutoActivity;
import net.labymod.api.client.render.matrix.Stack;

/**
 * Notizbuch einer Person: aufgeschlagenes Buch in der Buchfarbe aus dem Regal.
 * Pro Seite 2 x 3 Notizzettel, pro Doppelseite 12. Unten "+" fuer eine neue Notiz.
 * Notiz gedrueckt halten und auf den Papierkorb ziehen = loeschen. Klick auf eine Notiz = bearbeiten.
 */
@AutoActivity
public class NotesActivity extends FkActivity {

  private static final int PER_PAGE = 6;
  private static final int PER_SPREAD = 12;
  private static final int[] NOTE_COLORS = {0xFFFFF3B0, 0xFFDDF2C6, 0xFFD3E6FA, 0xFFFAD6DF, 0xFFF7DDBE, 0xFFE6DAF5};
  private static final int MAX_CHARS = 70;

  private static final class Anim {
    float x = Float.NaN, y, scale = 0.2F, pop = 1F;
  }

  private final UUID uuid;
  private final String playerName;
  private final boolean fromShelf;
  private final List<String> notes;
  private final List<Anim> anims = new ArrayList<>();
  private final BookFrame book = new BookFrame();
  private final int cover;

  private int spread;
  private int editing = -1;
  private int pressed = -1;
  private float pressX, pressY;
  private long pressTime;
  private boolean dragging;
  private float dragX, dragY;
  private int deleting = -1;
  private long deleteStart;
  private float trashAnim, trashShake;

  public NotesActivity(UUID uuid, String playerName, boolean fromShelf) {
    this.uuid = uuid;
    this.playerName = playerName;
    this.fromShelf = fromShelf;
    this.notes = FkreisAddon.get().store().notesOf(uuid.toString());
    for (int i = 0; i < this.notes.size(); i++) {
      this.anims.add(new Anim());
    }
    this.cover = G.shade(G.bookColor(uuid.toString()), 0.95F);
    // Wer angeklickt wird, steht ab jetzt im Regal
    FkreisAddon.get().store().touch(uuid.toString(), playerName);
  }

  private Store store() {
    return FkreisAddon.get().store();
  }

  private int spreads() {
    // immer Platz fuer das "+ Notiz"-Feld hinter der letzten Notiz
    return Math.max(1, this.notes.size() / PER_SPREAD + 1);
  }

  // ------------------------------------------------------------------ Layout
  // Ziegel-Hintergrund, Buch mit Leder-Einband, unten im Einband Seitenzahl, Papierkorb unten rechts auf dem Hintergrund.
  // Ohne Drehen/Zoomen gezeichnet – LabyMods neue Zeichen-Methode verschiebt sonst Flaechen und Text.

  private static final float PX = 1.0F;

  private float bx, by, bw, bh, pageW, leftX, rightX, pageY, pageH, gridTop, band;

  private void layout() {
    this.bw = Math.min(520, this.w * 0.72F);
    this.bh = Math.min(this.h - 56, this.bw * 0.62F);
    this.bx = this.x + (this.w - this.bw) / 2F;
    this.by = this.y + (this.h - this.bh) / 2F - 10;
    float side = 12, spine = 10;
    this.band = 22;
    this.pageW = (this.bw - 2 * side - spine) / 2F;
    this.leftX = this.bx + side;
    this.rightX = this.bx + side + this.pageW + spine;
    this.pageY = this.by + side;
    this.pageH = this.bh - side - this.band;
    this.gridTop = this.pageY + 30;
    this.book.layout(this.bx, this.by, this.bw, this.bh, 0);
  }

  /** Mitte + Groesse eines Zettels (lokaler Index 0..11, 0-5 linke Seite, 6-11 rechte Seite). */
  private float[] slot(int local) {
    boolean right = local >= PER_PAGE;
    int k = local % PER_PAGE;
    int col = k % 2, row = k / 2;
    float px = right ? this.rightX + 14 : this.leftX;
    float usable = this.pageW - 14;
    float pad = 8, gap = 8;
    float cw = (usable - 2 * pad - gap) / 2F;
    float ch = (this.pageY + this.pageH - 8 - this.gridTop - 2 * gap) / 3F;
    float nx = px + pad + col * (cw + gap);
    float ny = this.gridTop + row * (ch + gap);
    return new float[] {nx + cw / 2F, ny + ch / 2F, cw, ch};
  }

  /** Das Plus sitzt auf dem naechsten freien Platz (null, wenn der auf einer anderen Doppelseite liegt). */
  private float[] plusRect() {
    int idx = this.notes.size();
    if (idx / PER_SPREAD != this.spread) {
      return null;
    }
    float[] sl = this.slot(idx % PER_SPREAD);
    return new float[] {sl[0] - sl[2] / 2F, sl[1] - sl[3] / 2F, sl[2], sl[3]};
  }

  private float[] trashRect() {
    float size = 32;
    return new float[] {this.x + this.w - size - 14, this.y + this.h - size - 22, size, size};
  }

  private float[] backRect() {
    return new float[] {this.x + 8, this.y + this.h - 18, G.width(this.backLabel()) + 8, 14};
  }

  private String backLabel() {
    return "‹ " + (this.fromShelf ? L.t("notes.back.shelf") : L.t("notes.back.close")) + "  (Esc)";
  }

  private float[] arrow(boolean right) {
    float cy = this.by + this.bh - this.band / 2F - 6;
    float cx = this.bx + this.bw / 2F;
    return right ? new float[] {cx + 26, cy, 14, 12} : new float[] {cx - 40, cy, 14, 12};
  }

  // ------------------------------------------------------------------ Zeichnen

  private void bricks(Stack s) {
    G.rect(s, this.x, this.y, this.w, this.h, 0xFF33281B);
    float bhgt = 22, bwid = 72;
    int row = 0;
    for (float yy = this.y; yy < this.y + this.h; yy += bhgt, row++) {
      G.rect(s, this.x, yy, this.w, 2, 0xFF251C12);
      float off = (row % 2) * bwid / 2F;
      for (float xx = this.x - off; xx < this.x + this.w; xx += bwid) {
        G.rect(s, xx, yy, 2, bhgt, 0xFF251C12);
        G.rect(s, xx + 2, yy + 2, bwid - 2, 1, 0xFF3B2F20);
      }
    }
  }

  /** Leder-Einband: Grundfarbe, Maserung (feste Zufallspunkte), Ziernaht und dunkle Eckbeschlaege. */
  private void leather(Stack s) {
    int base = G.shade(this.cover, 0.8F);
    G.rect(s, this.bx + 4, this.by + 6, this.bw, this.bh, 0x60000000);
    G.rect(s, this.bx, this.by, this.bw, this.bh, base);
    long seed = this.uuid.getLeastSignificantBits();
    java.util.Random r = new java.util.Random(seed);
    for (int i = 0; i < 260; i++) {
      float gx = this.bx + r.nextFloat() * (this.bw - 3);
      float gy = this.by + r.nextFloat() * (this.bh - 2);
      float gw = 1 + r.nextFloat() * 3;
      G.rect(s, gx, gy, gw, 1, r.nextBoolean() ? G.shade(base, 1.12F) : G.shade(base, 0.86F));
    }
    G.rect(s, this.bx, this.by, this.bw, 2, G.shade(base, 1.25F));
    G.rect(s, this.bx, this.by + this.bh - 2, this.bw, 2, G.shade(base, 0.6F));
    G.outline(s, this.bx, this.by, this.bw, this.bh, 1, G.shade(base, 0.45F));
    // Ziernaht (gestrichelt, etwas heller)
    int stitch = G.shade(base, 1.45F);
    for (float xx = this.bx + 8; xx < this.bx + this.bw - 10; xx += 7) {
      G.rect(s, xx, this.by + 5, 4, 1, stitch);
      G.rect(s, xx, this.by + this.bh - 6, 4, 1, stitch);
    }
    for (float yy = this.by + 8; yy < this.by + this.bh - 10; yy += 7) {
      G.rect(s, this.bx + 5, yy, 1, 4, stitch);
      G.rect(s, this.bx + this.bw - 6, yy, 1, 4, stitch);
    }
    // Eckbeschlaege
    int metal = 0xFF8A7A5A, metalHi = 0xFFC8B88A;
    float c = 10;
    float[][] corners = {{this.bx, this.by}, {this.bx + this.bw - c, this.by},
        {this.bx, this.by + this.bh - c}, {this.bx + this.bw - c, this.by + this.bh - c}};
    for (float[] k : corners) {
      G.rect(s, k[0], k[1], c, c, metal);
      G.rect(s, k[0] + 1, k[1] + 1, c - 2, 1, metalHi);
      G.rect(s, k[0] + c / 2F - 1, k[1] + c / 2F - 1, 2, 2, 0xFF4A3E2A);
    }
  }

  @Override
  protected void draw(Stack s, float mx, float my) {
    this.layout();
    this.bricks(s);
    this.leather(s);

    // Seiten
    G.rect(s, this.leftX, this.pageY, this.pageW, this.pageH, 0xFFF1E6CA);
    G.rect(s, this.rightX, this.pageY, this.pageW, this.pageH, 0xFFF1E6CA);
    G.rect(s, this.leftX + this.pageW - 16, this.pageY, 16, this.pageH, 0xFFE0D3B4);
    G.rect(s, this.leftX + this.pageW - 6, this.pageY, 6, this.pageH, 0xFFD2C4A2);
    G.rect(s, this.rightX, this.pageY, 6, this.pageH, 0xFFD2C4A2);
    G.rect(s, this.rightX + 6, this.pageY, 10, this.pageH, 0xFFE0D3B4);
    G.rect(s, this.leftX, this.pageY + this.pageH, this.pageW, 1, 0xFFC8B890);
    G.rect(s, this.rightX, this.pageY + this.pageH, this.pageW, 1, 0xFFC8B890);
    // Ruecken
    G.rect(s, this.bx + this.bw / 2F - 4, this.by + 4, 8, this.bh - 8, G.shade(this.cover, 0.45F));
    G.rect(s, this.bx + this.bw / 2F - 1, this.by + 4, 2, this.bh - 8, G.shade(this.cover, 0.65F));

    // Kopfzeilen: links Kopf + Name, rechts "Notizen" + Anzahl
    G.icon(s, G.head(this.uuid), this.leftX + 10, this.pageY + 7, 16, 16);
    G.text(s, G.fit(this.playerName, this.pageW - 50, 1.25F), this.leftX + 32, this.pageY + 10, 0xFF2A2218, 1.25F);
    G.text(s, L.t("notes.title"), this.rightX + 22, this.pageY + 10, 0xFF6A5030, 1.1F);
    String cnt = String.valueOf(this.notes.size());
    G.text(s, cnt, this.rightX + this.pageW - 12 - G.width(cnt) * 1.1F, this.pageY + 10, 0xFF6A5030, 1.1F);

    int from = this.spread * PER_SPREAD;
    for (int i = from; i < Math.min(this.notes.size(), from + PER_SPREAD); i++) {
      if (i == this.pressed && this.dragging) {
        continue;
      }
      this.drawNote(s, i, mx, my, false);
    }

    // Plus auf dem naechsten freien Platz
    float[] p = this.plusRect();
    if (p != null && !this.dragging) {
      boolean ph = G.in(mx, my, p[0], p[1], p[2], p[3]);
      this.ghostNote(s, p[0], p[1], p[2], p[3], ph);
    }

    // Seitenzahl + Pfeile im unteren Einband
    int sp = this.spreads();
    G.centered(s, (this.spread + 1) + " / " + sp, this.bx + this.bw / 2F, this.by + this.bh - this.band / 2F - 4,
        0xFFF1E6CA, 1F, true);
    if (this.spread > 0) {
      float[] al = this.arrow(false);
      G.text(s, "‹", al[0] + 4, al[1], G.in(mx, my, al[0], al[1], al[2], al[3]) ? 0xFFFFE08A : 0xFFF1E6CA, 1.4F, true);
    }
    if (this.spread < sp - 1) {
      float[] ar = this.arrow(true);
      G.text(s, "›", ar[0] + 4, ar[1], G.in(mx, my, ar[0], ar[1], ar[2], ar[3]) ? 0xFFFFE08A : 0xFFF1E6CA, 1.4F, true);
    }

    this.drawTrash(s, mx, my);

    float[] back = this.backRect();
    boolean bh2 = G.in(mx, my, back[0], back[1], back[2], back[3]);
    G.text(s, this.backLabel(), back[0] + 4, back[1] + 3, bh2 ? 0xFFFFE08A : 0xFFFFFFFF, 1F, true);
    G.centered(s, L.t("notes.hint"), this.x + this.w / 2F, this.y + this.h - 15, 0xFFCFC6B0, 0.9F, true);

    if (this.pressed >= 0 && this.dragging && this.pressed < this.notes.size()) {
      this.drawNote(s, this.pressed, mx, my, true);
    }
  }

  /** Wie viele Zeilen passen auf einen Zettel? */
  private int maxLines(float ch) {
    return Math.max(1, (int) ((ch - 8) / (PixelFont.LINE * PX)));
  }

  /**
   * Platzhalter-Zettel auf dem naechsten freien Feld: ganz leichter Hintergrund, gestrichelte Ecken
   * und ein halb durchsichtiges Plus. Mit der Maus darueber wird alles etwas kraeftiger.
   */
  private void ghostNote(Stack s, float x, float y, float w, float h, boolean hover) {
    G.rect(s, x, y, w, h, hover ? 0x66FFFBE8 : 0x26FFFBE8);
    int line = hover ? 0xE06B4A2A : 0x8C8A6A3A;
    float t = 1.5F;
    float len = Math.min(w, h) * 0.22F;
    // vier Ecken, jeweils zwei gestrichelte Schenkel
    dashH(s, x, y, len, t, line);
    dashV(s, x, y, len, t, line);
    dashH(s, x + w - len, y, len, t, line);
    dashV(s, x + w - t, y, len, t, line);
    dashH(s, x, y + h - t, len, t, line);
    dashV(s, x, y + h - len, len, t, line);
    dashH(s, x + w - len, y + h - t, len, t, line);
    dashV(s, x + w - t, y + h - len, len, t, line);
    // Plus in der Mitte
    float arm = Math.min(w, h) * (hover ? 0.4F : 0.36F);
    float th = Math.max(2F, arm / 5F);
    int plus = hover ? 0x996B4A2A : 0x596B4A2A;
    float cx = x + w / 2F, cy = y + h / 2F;
    G.rect(s, cx - th / 2F, cy - arm / 2F, th, arm, plus);
    G.rect(s, cx - arm / 2F, cy - th / 2F, arm / 2F - th / 2F, th, plus);
    G.rect(s, cx + th / 2F, cy - th / 2F, arm / 2F - th / 2F, th, plus);
  }

  private static void dashH(Stack s, float x, float y, float len, float t, int argb) {
    for (float i = 0; i < len; i += 7) {
      G.rect(s, x + i, y, Math.min(4, len - i), t, argb);
    }
  }

  private static void dashV(Stack s, float x, float y, float len, float t, int argb) {
    for (float i = 0; i < len; i += 7) {
      G.rect(s, x, y + i, t, Math.min(4, len - i), argb);
    }
  }

  private void drawNote(Stack s, int i, float mx, float my, boolean dragged) {
    while (this.anims.size() < this.notes.size()) {
      this.anims.add(new Anim());
    }
    Anim a = this.anims.get(i);
    float[] slot = this.slot(i % PER_SPREAD);
    float tx = slot[0], ty = slot[1], target = 1F;
    if (dragged) {
      tx = this.dragX;
      ty = this.dragY;
    }
    if (i == this.deleting) {
      float[] t = this.trashRect();
      float pr = (System.currentTimeMillis() - this.deleteStart) / 260F;
      tx = t[0] + t[2] / 2F;
      ty = t[1] + t[3] / 2F;
      target = Math.max(0.05F, 1F - pr);
    }
    if (Float.isNaN(a.x)) {
      a.x = slot[0];
      a.y = slot[1];
    }
    float k = dragged ? 0.6F : 0.3F;
    a.x = G.lerp(a.x, tx, k);
    a.y = G.lerp(a.y, ty, k);
    a.scale = G.lerp(a.scale, target, 0.3F);

    float cw = slot[2] * a.scale, ch = slot[3] * a.scale;
    float lx = a.x - cw / 2F, ly = a.y - ch / 2F;
    boolean hover = !this.dragging && G.in(mx, my, lx, ly, cw, ch);
    if (hover && !dragged) {
      ly -= 1;
    }
    int color = NOTE_COLORS[Math.floorMod(i, NOTE_COLORS.length)];
    G.rect(s, lx + 2, ly + 2, cw, ch, dragged ? 0x50000000 : 0x30000000);
    G.rect(s, lx, ly, cw, ch, color);
    G.rect(s, lx, ly, cw, 3, G.shade(color, 0.88F));
    G.rect(s, lx, ly + ch - 1, cw, 1, G.shade(color, 0.82F));
    G.rect(s, lx + cw / 2F - 7, ly - 2, 14, 4, 0xB0FFFFFF);
    if (a.scale < 0.6F) {
      return;
    }

    String text = this.notes.get(i);
    boolean edit = i == this.editing;
    List<String> lines = PixelFont.wrap(text, cw - 8, PX);
    int maxLines = this.maxLines(ch);
    float lineH = PixelFont.LINE * PX;
    float ty2 = ly + 6;
    if (text.isEmpty() && !edit) {
      return;
    }
    for (int l = 0; l < Math.min(lines.size(), maxLines); l++) {
      PixelFont.draw(s, lines.get(l), lx + 4, ty2, PX);
      if (edit && l == Math.min(lines.size(), maxLines) - 1 && (System.currentTimeMillis() / 450) % 2 == 0) {
        G.rect(s, lx + 4 + PixelFont.width(lines.get(l), PX) + 1, ty2, 1, lineH - 2, 0xFF3A3020);
      }
      ty2 += lineH;
    }
    if (edit) {
      G.outline(s, lx - 1, ly - 1, cw + 2, ch + 2, 1, 0xFFE8B830);
    }
  }

  /** Passt der Text noch auf einen Zettel? (sonst wird nichts mehr angenommen) */
  private boolean fits(String text) {
    float[] slot = this.slot(0);
    return PixelFont.wrap(text, slot[2] - 8, PX).size() <= this.maxLines(slot[3]);
  }

  private void drawTrash(Stack s, float mx, float my) {
    float[] t = this.trashRect();
    boolean over = this.dragging && G.in(mx, my, t[0] - 8, t[1] - 8, t[2] + 16, t[3] + 16);
    this.trashAnim = G.lerp(this.trashAnim, over ? 1F : (this.dragging ? 0.45F : 0F), 0.25F);
    this.trashShake = G.lerp(this.trashShake, 0F, 0.12F);
    float grow = 5F * this.trashAnim;
    float shake = (float) Math.sin(System.currentTimeMillis() / 40.0) * 2F * this.trashShake;
    if (over) {
      G.rect(s, t[0] - 4 - grow, t[1] - 4 - grow, t[2] + 8 + 2 * grow, t[3] + 8 + 2 * grow, 0x40E04040);
    }
    G.icon(s, over || this.deleting >= 0 ? G.TRASH_OPEN : G.TRASH, t[0] - grow + shake, t[1] - grow,
        t[2] + 2 * grow, t[3] + 2 * grow);
    if (this.deleting >= 0 && System.currentTimeMillis() - this.deleteStart > 260) {
      this.finishDelete();
    }
  }

  // ------------------------------------------------------------------ Logik

  private int noteAt(float mx, float my) {
    int from = this.spread * PER_SPREAD;
    for (int i = Math.min(this.notes.size(), from + PER_SPREAD) - 1; i >= from; i--) {
      float[] slot = this.slot(i % PER_SPREAD);
      Anim a = i < this.anims.size() ? this.anims.get(i) : null;
      float ax = a == null || Float.isNaN(a.x) ? slot[0] : a.x;
      float ay = a == null || Float.isNaN(a.x) ? slot[1] : a.y;
      if (G.in(mx, my, ax - slot[2] / 2F, ay - slot[3] / 2F, slot[2], slot[3])) {
        return i;
      }
    }
    return -1;
  }

  private void stopEditing() {
    if (this.editing >= 0 && this.editing < this.notes.size() && this.notes.get(this.editing).trim().isEmpty()) {
      this.notes.remove(this.editing);
      this.anims.remove(this.editing);
    }
    this.editing = -1;
    this.store().save();
  }

  private void addNote() {
    this.stopEditing();
    this.notes.add("");
    Anim a = new Anim();
    a.pop = 1F;
    this.anims.add(a);
    this.editing = this.notes.size() - 1;
    int target = this.editing / PER_SPREAD;
    if (target != this.spread) {
      this.book.flip(target > this.spread ? 1 : -1);
      this.spread = target;
    }
    G.sound("item.book.page_turn", 1.4F);
  }

  private void startDelete(int i) {
    this.deleting = i;
    this.deleteStart = System.currentTimeMillis();
    this.trashShake = 1F;
    G.sound("entity.item.pickup", 0.6F);
  }

  private void finishDelete() {
    int i = this.deleting;
    this.deleting = -1;
    if (i >= 0 && i < this.notes.size()) {
      this.notes.remove(i);
      this.anims.remove(i);
      if (this.editing == i) {
        this.editing = -1;
      } else if (this.editing > i) {
        this.editing--;
      }
      this.store().save();
      if (this.spread >= this.spreads()) {
        this.spread = this.spreads() - 1;
      }
    }
  }

  private void flipTo(int dir) {
    int target = this.spread + dir;
    if (target < 0 || target >= this.spreads() || this.book.flipping()) {
      return;
    }
    this.stopEditing();
    this.spread = target;
    this.book.flip(dir);
    G.sound("item.book.page_turn", 1F);
  }

  @Override
  protected boolean onClick(float mx, float my, boolean left) {
    if (!left || this.deleting >= 0) {
      return true;
    }
    float[] p = this.plusRect();
    if (p != null && G.in(mx, my, p[0], p[1], p[2], p[3])) {
      this.addNote();
      return true;
    }
    float[] back = this.backRect();
    if (G.in(mx, my, back[0], back[1], back[2], back[3])) {
      this.goBack();
      return true;
    }
    float[] ar = this.arrow(true);
    if (G.in(mx, my, ar[0], ar[1], ar[2], ar[3])) {
      this.flipTo(1);
      return true;
    }
    float[] al = this.arrow(false);
    if (G.in(mx, my, al[0], al[1], al[2], al[3])) {
      this.flipTo(-1);
      return true;
    }
    int hit = this.noteAt(mx, my);
    if (hit >= 0) {
      this.pressed = hit;
      this.pressX = mx;
      this.pressY = my;
      this.pressTime = System.currentTimeMillis();
      this.dragging = false;
      return true;
    }
    this.stopEditing();
    return true;
  }

  @Override
  protected boolean onDrag(float mx, float my) {
    if (this.pressed < 0) {
      return false;
    }
    if (!this.dragging && (Math.abs(mx - this.pressX) > 3 || Math.abs(my - this.pressY) > 3)) {
      this.dragging = true;
      if (this.editing == this.pressed) {
        this.editing = -1;
      }
      G.sound("item.book.page_turn", 1.8F);
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
    int i = this.pressed;
    this.pressed = -1;
    if (this.dragging) {
      this.dragging = false;
      float[] t = this.trashRect();
      if (G.in(mx, my, t[0] - 6, t[1] - 6, t[2] + 12, t[3] + 12)) {
        this.startDelete(i);
      }
      return true;
    }
    if (this.editing != i) {
      this.stopEditing();
      this.editing = Math.min(i, this.notes.size() - 1);
      G.click();
    }
    return true;
  }

  @Override
  protected boolean onScroll(float mx, float my, float delta) {
    this.flipTo(delta < 0 ? 1 : -1);
    return true;
  }

  @Override
  protected boolean onKey(Key key) {
    if (this.editing >= 0) {
      String t = this.notes.get(this.editing);
      if (key == Key.ESCAPE) {
        this.stopEditing();
        return true;
      }
      if (key == Key.BACK) {
        if (!t.isEmpty()) {
          this.notes.set(this.editing, t.substring(0, t.length() - 1));
        }
        return true;
      }
      if (key == Key.ENTER) {
        if (t.length() < MAX_CHARS && this.fits(t + "\nx")) {
          this.notes.set(this.editing, t + "\n");
        }
        return true;
      }
      return false;
    }
    if (key == Key.ESCAPE) {
      this.goBack();
      return true;
    }
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

  @Override
  protected boolean onChar(char c) {
    if (this.editing < 0 || c < 32) {
      return false;
    }
    String t = this.notes.get(this.editing);
    if (t.length() < MAX_CHARS && this.fits(t + c)) {
      this.notes.set(this.editing, t + c);
    } else {
      G.sound("block.note_block.bass", 0.6F);
    }
    return true;
  }

  private void goBack() {
    this.stopEditing();
    this.store().save();
    if (this.fromShelf) {
      Screens.now(new NotebookActivity(this.uuid.toString()));
    } else {
      Screens.close();
    }
  }

  @Override
  public void onCloseScreen() {
    super.onCloseScreen();
    this.stopEditing();
  }
}
