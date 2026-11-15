package de.ulgu.fkreis.core.ui;

import de.ulgu.fkreis.core.FkreisAddon;
import de.ulgu.fkreis.core.L;
import de.ulgu.fkreis.core.data.Store;
import de.ulgu.fkreis.core.data.WoodType;
import de.ulgu.fkreis.core.gfx.G;
import java.util.List;
import java.util.UUID;
import net.labymod.api.client.gui.screen.activity.AutoActivity;
import net.labymod.api.client.render.matrix.Stack;

/**
 * Notebook-Regal wie im alten Freundeskreis: 5 Faecher nebeneinander, je 5 Buecher.
 * Im Regal stehen NUR Leute, denen man eine Notiz geschrieben oder deren Buch man geoeffnet hat.
 * Kein Scrollen: werden es mehr als 5 x 4 Faecher, kommt unten eine Reihe dazu und alles wird kleiner.
 * Oben steht der Name + Anzahl Notizen des Buchs unter der Maus.
 */
@AutoActivity
public class NotebookActivity extends FkActivity {

  private static final int COLS = 5;
  private static final int PER = 5;
  private static final int MIN_ROWS = 4;

  private float[] pull = new float[0];

  /** Person, die gerade im Mausrad angeklickt wurde – ihr Buch leuchtet golden. */
  private final String highlight;

  public NotebookActivity() {
    this(null);
  }

  public NotebookActivity(String highlight) {
    this.highlight = highlight;
    G.sound("item.book.page_turn", 0.9F);
  }

  private List<String> people() {
    return FkreisAddon.get().store().shelf;
  }

  private int rows() {
    int need = (this.people().size() + COLS * PER - 1) / (COLS * PER);
    return Math.max(MIN_ROWS, need);
  }

  private float top() { return this.y + 26; }
  private float gapX() { return 14; }
  private float gapY() { return 10; }
  private float cw() { return (this.w - 2 * 16 - (COLS - 1) * this.gapX()) / COLS; }
  private float ch() { return (this.h - 26 - 10 - (this.rows() - 1) * this.gapY()) / this.rows(); }
  private float bookH() { return (this.ch() - 8) / PER; }

  private float[] bookRect(int i) {
    int comp = i / PER, inComp = i % PER;
    int col = comp % COLS, row = comp / COLS;
    float cx = this.x + 16 + col * (this.cw() + this.gapX());
    float cy = this.top() + row * (this.ch() + this.gapY());
    float off = Math.floorMod(i * 7 + 3, 4) * 1.5F;
    float bx = cx + 4 + off;
    float by = cy + 4 + inComp * this.bookH();
    return new float[] {bx, by, this.cw() - 10 - off, this.bookH() - 2};
  }

  @Override
  protected void draw(Stack s, float mx, float my) {
    Store store = FkreisAddon.get().store();
    List<String> people = this.people();
    WoodType wood = WoodType.parse(FkreisAddon.get().configuration().notebook().wood().get());
    int rows = this.rows();
    int slots = rows * COLS * PER;
    if (this.pull.length != slots) {
      this.pull = new float[slots];
    }

    G.planks(s, this.x, this.y, this.w, this.h, wood.plank, 1);

    // Faecher: dunkle Rueckwand, Brett-Kante unten
    for (int comp = 0; comp < rows * COLS; comp++) {
      int col = comp % COLS, row = comp / COLS;
      float cx = this.x + 16 + col * (this.cw() + this.gapX());
      float cy = this.top() + row * (this.ch() + this.gapY());
      G.rect(s, cx - 3, cy - 3, this.cw() + 6, this.ch() + 6, G.shade(wood.plank, 0.62F));
      G.rect(s, cx, cy, this.cw(), this.ch(), wood.back);
      G.rect(s, cx, cy, this.cw(), 3, G.shade(wood.back, 0.55F));
      G.rect(s, cx, cy + this.ch() - 2, this.cw(), 2, G.shade(wood.plank, 0.85F));
    }

    int hovered = -1;
    for (int i = 0; i < slots; i++) {
      float[] r = this.bookRect(i);
      boolean real = i < people.size();
      boolean over = real && G.in(mx, my, r[0], r[1], r[2], r[3]);
      if (over) {
        hovered = i;
      }
      this.pull[i] = G.lerp(this.pull[i], over ? 1F : 0F, 0.28F);
      this.drawBook(s, i, r, real ? people.get(i) : null, store);
    }
    int mark = this.highlight == null ? -1 : people.indexOf(this.highlight);
    if (mark >= 0) {
      float[] r = this.bookRect(mark);
      float px = 4F * this.pull[mark];
      float pulse = 0.55F + 0.45F * (float) Math.sin(System.currentTimeMillis() / 220.0);
      G.rect(s, r[0] + px - 3, r[1] - 3, r[2] + 6, r[3] + 6, G.alpha(0xFFF2C94C, 0.18F * pulse));
      G.outline(s, r[0] + px - 3, r[1] - 3, r[2] + 6, r[3] + 6, 2, G.alpha(0xFFF2C94C, 0.6F + 0.4F * pulse));
    }
    if (hovered >= 0) {
      float[] r = this.bookRect(hovered);
      float px = 4F * this.pull[hovered];
      G.outline(s, r[0] + px - 2, r[1] - 2, r[2] + 4, r[3] + 4, 1, 0xFFF2C94C);
    }

    // Titel oben: Buch unter der Maus -> "Name – N Notizen", sonst "Notebook"
    String title = "Notebook";
    if (hovered >= 0) {
      String uuid = people.get(hovered);
      int n = store.notesOf(uuid).size();
      title = store.nameOf(uuid) + " – " + n + " " + (n == 1 ? L.t("notes.one") : L.t("notes.many"));
    } else if (mark >= 0) {
      int n = store.notesOf(this.highlight).size();
      title = store.nameOf(this.highlight) + " – " + n + " " + (n == 1 ? L.t("notes.one") : L.t("notes.many"));
    } else if (people.isEmpty()) {
      title = L.t("notebook.empty");
    }
    G.centered(s, title, this.x + this.w / 2F, this.y + 9, 0xFFFFFFFF, 1.1F, true);

    String back = "‹ " + L.t("notes.back.close") + "  (Esc)";
    boolean bh = G.in(mx, my, this.x + 4, this.y + this.h - 16, G.width(back) + 10, 14);
    G.rect(s, this.x + 4, this.y + this.h - 16, G.width(back) + 10, 13, bh ? 0xC0202020 : 0x90000000);
    G.text(s, back, this.x + 9, this.y + this.h - 13, bh ? 0xFFFFE08A : 0xFFFFFFFF, 1F, true);

    float fade = 1F - G.easeOut(this.age() / 0.3F);
    if (fade > 0.01F) {
      G.rect(s, this.x, this.y, this.w, this.h, G.alpha(0xFF000000, fade));
    }
  }

  private void drawBook(Stack s, int i, float[] r, String uuid, Store store) {
    boolean real = uuid != null;
    int color = G.bookColor(real ? uuid : "steve" + i);
    float px = 4F * (i < this.pull.length ? this.pull[i] : 0);
    float bx = r[0] + px, by = r[1], bw = r[2], bh = r[3];
    G.spine(s, bx, by, bw, bh, color, true);
    float tab = Math.max(2, bh * 0.18F);
    float lx = bx + 4 + tab + 2;
    float head = Math.max(4, bh - 6);
    if (real) {
      G.icon(s, G.head(UUID.fromString(uuid)), lx + 1, by + 3, head, head);
    } else {
      G.steveHead(s, lx + 1, by + 3, head);
    }
    String name = real ? store.nameOf(uuid) : "Steve";
    float scale = Math.max(0.5F, Math.min(1.15F, (bh - 4) / 10F));
    float maxW = bw - (lx - bx) - head - 12;
    G.text(s, G.fit(name, maxW, scale), lx + head + 4, by + (bh - 8 * scale) / 2F, real ? 0xFF222222 : 0xFF8A8A8A, scale);
  }

  @Override
  protected boolean onKey(net.labymod.api.client.gui.screen.key.Key key) {
    if (key == net.labymod.api.client.gui.screen.key.Key.ESCAPE) {
      Screens.close();
      return true;
    }
    return false;
  }

  @Override
  protected boolean onClick(float mx, float my, boolean left) {
    String back = "‹ " + L.t("notes.back.close") + "  (Esc)";
    if (G.in(mx, my, this.x + 4, this.y + this.h - 16, G.width(back) + 10, 14)) {
      Screens.close();
      return true;
    }
    List<String> people = this.people();
    Store store = FkreisAddon.get().store();
    for (int i = 0; i < people.size(); i++) {
      float[] r = this.bookRect(i);
      if (G.in(mx, my, r[0], r[1], r[2], r[3])) {
        String uuid = people.get(i);
        G.sound("item.book.page_turn", 1F);
        Screens.now(new NotesActivity(UUID.fromString(uuid), store.nameOf(uuid), true));
        return true;
      }
    }
    return false;
  }
}
