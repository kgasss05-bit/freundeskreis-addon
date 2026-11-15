package de.ulgu.fkreis.core.ui;

import de.ulgu.fkreis.core.FkreisAddon;
import de.ulgu.fkreis.core.L;
import de.ulgu.fkreis.core.data.Frame;
import de.ulgu.fkreis.core.data.Photo;
import de.ulgu.fkreis.core.gfx.G;
import de.ulgu.fkreis.core.gfx.PhotoTex;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import net.labymod.api.client.gui.screen.activity.AutoActivity;
import net.labymod.api.client.gui.screen.key.Key;
import net.labymod.api.client.render.matrix.Stack;

/**
 * Galerie deiner Screenshots (neueste zuerst). Rechtsklick auf ein Bild -> "Ins Fotoalbum speichern"
 * (Linksklick geht auch). Danach: Rahmen waehlen, kleinen Text schreiben, "Einkleben".
 */
@AutoActivity
public class PhotoPickerActivity extends FkActivity {

  private static final int COLS = 4;
  private static final int ROWS = 3;
  private static final int MAX_CAPTION = 48;

  private final List<Path> shots = new ArrayList<>();
  private int page;
  private int menuFor = -1;
  private float menuX, menuY;
  private Path chosen;
  private Frame frame;
  private String caption = "";

  public PhotoPickerActivity() {
    this.frame = Frame.parse(FkreisAddon.get().configuration().album().lastFrame().get());
    Path dir = Paths.get("screenshots");
    try (java.nio.file.DirectoryStream<Path> st = Files.newDirectoryStream(dir, "*.{png,jpg,jpeg,PNG,JPG,JPEG}")) {
      for (Path p : st) {
        this.shots.add(p);
      }
    } catch (java.io.IOException ignored) {
      // kein screenshots-Ordner -> leere Liste
    }
    this.shots.sort((a, b) -> {
      try {
        return Files.getLastModifiedTime(b).compareTo(Files.getLastModifiedTime(a));
      } catch (Exception e) {
        return 0;
      }
    });
    G.click();
  }

  private int pages() {
    return Math.max(1, (this.shots.size() + COLS * ROWS - 1) / (COLS * ROWS));
  }

  private float[] cell(int local) {
    float gx = this.x + 30, gy = this.y + 44, gw = this.w - 60, gh = this.h - 44 - 40;
    float cw = gw / COLS, ch = gh / ROWS;
    float tw = cw - 12, th = Math.min(ch - 12, tw * 9F / 16F);
    tw = th * 16F / 9F;
    int c = local % COLS, r = local / COLS;
    return new float[] {gx + c * cw + (cw - tw) / 2F, gy + r * ch + (ch - th) / 2F, tw, th};
  }

  private float[] menuRect() {
    return new float[] {this.menuX, this.menuY, Math.max(150, G.width(L.t("album.save")) + 20), 18};
  }

  // ---------------------------------------------------------------- Dialog-Layout

  private float dw, dh, dx, dy;

  private void dialogLayout() {
    this.dw = Math.min(360, this.w - 40);
    this.dh = Math.min(this.h - 30, 290);
    this.dx = this.x + (this.w - this.dw) / 2F;
    this.dy = this.y + (this.h - this.dh) / 2F;
  }

  private float[] swatch(int i) {
    int n = Frame.values().length;
    float size = 22, gap = 8;
    float total = n * size + (n - 1) * gap;
    float sx = this.dx + (this.dw - total) / 2F;
    return new float[] {sx + i * (size + gap), this.dy + this.dh - 108, size, size};
  }

  private float[] field() {
    return new float[] {this.dx + 16, this.dy + this.dh - 64, this.dw - 32, 18};
  }

  private float[] button(boolean ok) {
    float bw = (this.dw - 48) / 2F;
    return new float[] {ok ? this.dx + 16 + bw + 16 : this.dx + 16, this.dy + this.dh - 34, bw, 20};
  }

  @Override
  protected void draw(Stack s, float mx, float my) {
    G.rect(s, this.x, this.y, this.w, this.h, 0xF0151821);
    boolean blocked = this.chosen != null;
    this.header(s, L.t("album.picker"), blocked ? -1 : mx, blocked ? -1 : my);

    if (this.shots.isEmpty()) {
      G.centered(s, L.t("album.noshots"), this.x + this.w / 2F, this.y + this.h / 2F - 4, 0xFFB8BCC8, 1F, true);
    }
    int from = this.page * COLS * ROWS;
    for (int i = from; i < Math.min(this.shots.size(), from + COLS * ROWS); i++) {
      float[] r = this.cell(i - from);
      boolean over = !blocked && this.menuFor < 0 && G.in(mx, my, r[0], r[1], r[2], r[3]);
      G.rect(s, r[0] - 2, r[1] - 2, r[2] + 4, r[3] + 4, over || i == this.menuFor ? 0xFFF2C94C : 0xFF2A2F3C);
      G.rect(s, r[0], r[1], r[2], r[3], 0xFF0E1016);
      PhotoTex.Tex t = PhotoTex.get(this.shots.get(i), 256);
      if (t == null) {
        G.centered(s, L.t("album.loading"), r[0] + r[2] / 2F, r[1] + r[3] / 2F - 3, 0xFF6A707C, 0.8F, false);
      } else {
        G.icon(s, t.icon, r[0], r[1], r[2], r[3]);
      }
    }
    if (this.pages() > 1) {
      G.centered(s, "‹   " + (this.page + 1) + " / " + this.pages() + "   ›", this.x + this.w / 2F,
          this.y + this.h - 26, 0xFFDDDDDD, 1F, true);
    }
    G.centered(s, L.t("album.pickhint"), this.x + this.w / 2F, this.y + this.h - 13, 0xFF8A90A0, 0.85F, true);

    // Rechtsklick-Menue
    if (this.menuFor >= 0 && !blocked) {
      float[] m = this.menuRect();
      boolean over = G.in(mx, my, m[0], m[1], m[2], m[3]);
      G.rect(s, m[0] + 2, m[1] + 2, m[2], m[3], 0x60000000);
      G.rect(s, m[0], m[1], m[2], m[3], over ? 0xFF3A4A6A : 0xFF12141C);
      G.outline(s, m[0], m[1], m[2], m[3], 1, 0xFF3A3E4A);
      G.text(s, L.t("album.save"), m[0] + 8, m[1] + 5, 0xFFFFFFFF, 1F, false);
    }

    if (blocked) {
      this.drawDialog(s, mx, my);
    }
  }

  private void drawDialog(Stack s, float mx, float my) {
    this.dialogLayout();
    G.rect(s, this.x, this.y, this.w, this.h, 0x90000000);
    G.rect(s, this.dx + 3, this.dy + 4, this.dw, this.dh, 0x60000000);
    G.rect(s, this.dx, this.dy, this.dw, this.dh, 0xFFE5D4A3);
    G.outline(s, this.dx, this.dy, this.dw, this.dh, 1, 0xFFB9A57A);
    G.centered(s, L.t("album.insert"), this.dx + this.dw / 2F, this.dy + 8, 0xFF3A2A1A, 1.2F, false);

    float pw = Math.min(this.dw - 80, (this.dh - 150) * 16F / 9F);
    float ph = pw * 9F / 16F + pw * 0.08F;
    PhotoDraw.framed(s, this.chosen, this.frame, this.dx + (this.dw - pw) / 2F, this.dy + 24, pw, ph, 400);

    G.text(s, L.t("album.frame"), this.dx + 16, this.dy + this.dh - 122, 0xFF3A2A1A, 0.9F);
    Frame[] fs = Frame.values();
    for (int i = 0; i < fs.length; i++) {
      float[] r = this.swatch(i);
      boolean sel = fs[i] == this.frame;
      boolean over = G.in(mx, my, r[0], r[1], r[2], r[3]);
      if (sel) {
        G.outline(s, r[0] - 3, r[1] - 3, r[2] + 6, r[3] + 6, 2, 0xFFC04A2A);
      }
      G.rect(s, r[0], r[1], r[2], r[3], fs[i].base);
      G.rect(s, r[0], r[1], r[2], 2, fs[i].light);
      G.rect(s, r[0], r[1] + r[3] - 2, r[2], 2, fs[i].dark);
      if (over && !sel) {
        G.outline(s, r[0] - 1, r[1] - 1, r[2] + 2, r[3] + 2, 1, 0xFF5A4A2A);
      }
    }
    G.centered(s, frameName(this.frame), this.dx + this.dw / 2F, this.dy + this.dh - 82, 0xFF7A6A4A, 0.8F, false);

    float[] f = this.field();
    G.text(s, L.t("album.caption"), f[0], f[1] - 10, 0xFF3A2A1A, 0.9F);
    G.rect(s, f[0], f[1], f[2], f[3], 0xFFF7F0DC);
    G.outline(s, f[0], f[1], f[2], f[3], 1, 0xFFB89A60);
    String shown = G.fit(this.caption, f[2] - 12, 1F);
    if (this.caption.isEmpty()) {
      G.text(s, L.t("album.captionhint"), f[0] + 5, f[1] + 5, 0xFFAA9A7A, 1F);
    } else {
      G.text(s, shown, f[0] + 5, f[1] + 5, 0xFF2A2A2A, 1F);
    }
    if ((System.currentTimeMillis() / 450) % 2 == 0) {
      G.rect(s, f[0] + 6 + (this.caption.isEmpty() ? 0 : G.width(shown)), f[1] + 4, 1, 10, 0xFF2A2A2A);
    }

    for (boolean ok : new boolean[] {false, true}) {
      float[] b = this.button(ok);
      boolean over = G.in(mx, my, b[0], b[1], b[2], b[3]);
      int bg = ok ? (over ? 0xFF5AA04A : 0xFF4A8A3A) : (over ? 0xFFD8C898 : 0xFFC8B888);
      G.rect(s, b[0], b[1], b[2], b[3], bg);
      G.outline(s, b[0], b[1], b[2], b[3], 1, 0x40000000);
      G.centered(s, ok ? L.t("album.glue") : L.t("album.cancel"), b[0] + b[2] / 2F, b[1] + 6,
          ok ? 0xFFFFFFFF : 0xFF3A2A1A, 1F, false);
    }
  }

  private static String frameName(Frame f) {
    return L.t("frame." + f.name().toLowerCase());
  }

  private void glue() {
    try {
      Path dir = AlbumActivity.albumDir();
      Files.createDirectories(dir);
      String name = System.currentTimeMillis() + "_" + this.chosen.getFileName().toString();
      Files.copy(this.chosen, dir.resolve(name), StandardCopyOption.REPLACE_EXISTING);
      FkreisAddon.get().store().photos.add(new Photo(name, this.frame.name(), this.caption.trim()));
      FkreisAddon.get().store().save();
      FkreisAddon.get().configuration().album().lastFrame().set(this.frame.name());
      G.sound("item.book.page_turn", 1.2F);
      Screens.now(new AlbumActivity());
    } catch (Exception e) {
      de.ulgu.fkreis.core.FkreisAddon.log().warn("Freundeskreis: " + e);
      de.ulgu.fkreis.core.net.Net.notify(L.t("album.title"), L.t("album.failed"), null);
    }
  }

  @Override
  protected boolean onClick(float mx, float my, boolean left) {
    if (this.chosen != null) {
      this.dialogLayout();
      Frame[] fs = Frame.values();
      for (int i = 0; i < fs.length; i++) {
        float[] r = this.swatch(i);
        if (G.in(mx, my, r[0] - 2, r[1] - 2, r[2] + 4, r[3] + 4)) {
          this.frame = fs[i];
          G.click();
          return true;
        }
      }
      if (G.in(mx, my, this.button(true)[0], this.button(true)[1], this.button(true)[2], this.button(true)[3])) {
        this.glue();
        return true;
      }
      if (G.in(mx, my, this.button(false)[0], this.button(false)[1], this.button(false)[2], this.button(false)[3])) {
        this.chosen = null;
        G.click();
      }
      return true;
    }
    if (this.headerHit(mx, my)) {
      this.displayPreviousScreen();
      return true;
    }
    if (this.menuFor >= 0) {
      float[] m = this.menuRect();
      int target = this.menuFor;
      this.menuFor = -1;
      if (G.in(mx, my, m[0], m[1], m[2], m[3])) {
        this.open(target);
        return true;
      }
    }
    int from = this.page * COLS * ROWS;
    for (int i = from; i < Math.min(this.shots.size(), from + COLS * ROWS); i++) {
      float[] r = this.cell(i - from);
      if (G.in(mx, my, r[0], r[1], r[2], r[3])) {
        if (left) {
          this.open(i);
        } else {
          this.menuFor = i;
          this.menuX = Math.min(mx, this.x + this.w - 160);
          this.menuY = my;
          G.click();
        }
        return true;
      }
    }
    // Seitenwechsel per Klick auf ‹ / › unten
    if (my > this.y + this.h - 32 && my < this.y + this.h - 16) {
      if (mx < this.x + this.w / 2F - 10 && this.page > 0) {
        this.page--;
      } else if (mx > this.x + this.w / 2F + 10 && this.page < this.pages() - 1) {
        this.page++;
      }
      return true;
    }
    return true;
  }

  private void open(int i) {
    this.chosen = this.shots.get(i);
    this.caption = "";
    G.click();
  }

  @Override
  protected boolean onScroll(float mx, float my, float delta) {
    if (this.chosen != null) {
      return true;
    }
    this.menuFor = -1;
    if (delta < 0 && this.page < this.pages() - 1) {
      this.page++;
    } else if (delta > 0 && this.page > 0) {
      this.page--;
    }
    return true;
  }

  @Override
  protected boolean onChar(char c) {
    if (this.chosen == null) {
      return false;
    }
    if (c >= ' ' && this.caption.length() < MAX_CAPTION) {
      this.caption += c;
    }
    return true;
  }

  @Override
  protected boolean onKey(Key key) {
    if (key == Key.ESCAPE) {
      if (this.chosen != null) {
        this.chosen = null;
      } else if (this.menuFor >= 0) {
        this.menuFor = -1;
      } else {
        this.displayPreviousScreen();
      }
      return true;
    }
    if (this.chosen != null) {
      if (key == Key.BACK && !this.caption.isEmpty()) {
        this.caption = this.caption.substring(0, this.caption.length() - 1);
        return true;
      }
      if (key == Key.ENTER) {
        this.glue();
        return true;
      }
      return true;
    }
    if (key == Key.ARROW_LEFT && this.page > 0) {
      this.page--;
      return true;
    }
    if (key == Key.ARROW_RIGHT && this.page < this.pages() - 1) {
      this.page++;
      return true;
    }
    return false;
  }
}
