package de.ulgu.fkreis.core.diary;

import de.ulgu.fkreis.core.L;
import de.ulgu.fkreis.core.gfx.G;
import de.ulgu.fkreis.core.ui.FkActivity;
import de.ulgu.fkreis.core.ui.Screens;
import net.labymod.api.Laby;
import net.labymod.api.client.gui.icon.Icon;
import net.labymod.api.client.gui.screen.activity.AutoActivity;
import net.labymod.api.client.gui.screen.key.Key;
import net.labymod.api.client.render.matrix.Stack;
import net.labymod.api.client.resources.ResourceLocation;

/**
 * Einband gestalten: helles, mittleres oder dunkles Leder – oder ein weisses Buch, das man selbst faerbt.
 * Gefaerbt wird wie in Minecraft: Farbstoff aus dem kleinen Inventar nehmen und aufs Buch ziehen.
 * Mehrere Farbstoffe mischen sich wie bei Lederruestung, der Wassereimer macht das Buch wieder weiss.
 * Die Farbstoff- und Eimer-Symbole kommen direkt aus dem Spiel (also auch aus dem Texturenpaket).
 */
@AutoActivity
public class DiaryCoverActivity extends FkActivity {

  private static final String[] LEATHERS = {"hell", "mittel", "dunkel", "eigen"};
  private static final String[] TRIMS = {"none", "gold", "silver"};
  private static final int WATER = 16, SLOT = 18;
  private static final Icon[] ITEMS = new Icon[17];

  private final Diary diary = Diary.get();
  /** Arbeitsfarbe des weissen Buchs (-1 = weiss), wird erst mit "Speichern" uebernommen. */
  private int work;
  private int held = -1;
  private boolean dragging;
  private float px, py, pw, ph;

  public DiaryCoverActivity() {
    this.work = this.diary.custom;
    G.click();
  }

  private static Icon item(int i) {
    if (ITEMS[i] == null) {
      String path = i == WATER ? "water_bucket" : Diary.DYE_IDS[i] + "_dye";
      ITEMS[i] = Icon.texture(ResourceLocation.create("minecraft", "textures/item/" + path + ".png"));
    }
    return ITEMS[i];
  }

  private boolean custom() {
    return "eigen".equals(this.diary.leather);
  }

  private int previewColor() {
    return this.custom() ? (this.work < 0 ? Diary.WHITE_BOOK : this.work) : this.diary.leatherColor();
  }

  private float[] book() {
    return new float[] {this.px + 16, this.py + 34, 98, 126};
  }

  private float[] leatherBtn(int i) {
    return new float[] {this.px + 130, this.py + 44 + i * 19, this.pw - 146, 16};
  }

  private float[] trimBtn(int i) {
    float bw = (this.pw - 146 - 8) / 3F;
    return new float[] {this.px + 130 + i * (bw + 4), this.py + 136, bw, 16};
  }

  private float[] slot(int i) {
    float x0 = this.px + (this.pw - 9 * SLOT) / 2F;
    return new float[] {x0 + (i % 9) * SLOT, this.py + 186 + (i / 9) * SLOT, SLOT, SLOT};
  }

  private float[] saveBtn() {
    return new float[] {this.px + this.pw / 2F - 40, this.py + this.ph - 24, 80, 16};
  }

  @Override
  protected void draw(Stack s, float mx, float my) {
    G.rect(s, this.x, this.y, this.w, this.h, 0xB0000000);
    this.header(s, L.t("diary.cover.title"), mx, my);
    this.pw = Math.min(this.w - 20, 300);
    this.ph = this.custom() ? 252 : 172;
    this.px = this.x + (this.w - this.pw) / 2F;
    this.py = this.y + Math.max(4, (this.h - this.ph) / 2F);
    G.rect(s, this.px + 3, this.py + 4, this.pw, this.ph, 0x60000000);
    G.rect(s, this.px, this.py, this.pw, this.ph, 0xF01C1F28);
    G.outline(s, this.px, this.py, this.pw, this.ph, 1, 0xFF3A4152);
    G.rect(s, this.px, this.py, this.pw, 2, 0xFFF2C94C);
    G.centered(s, L.t("diary.cover.title"), this.px + this.pw / 2F, this.py + 11, 0xFFF2C94C, 1.2F, true);

    float[] b = this.book();
    boolean overBook = G.in(mx, my, b[0], b[1], b[2], b[3]);
    if (this.custom() && this.held >= 0 && overBook) {
      G.outline(s, b[0] - 3, b[1] - 3, b[2] + 6, b[3] + 6, 1, 0xFFF2C94C);
    }
    G.rect(s, b[0] + 3, b[1] + 4, b[2], b[3], 0x60000000);
    G.icon(s, CoverArt.icon(this.previewColor(), this.diary.trim, Laby.labyAPI().getName()), b[0], b[1], b[2], b[3]);

    G.text(s, L.t("diary.cover.leather"), this.px + 130, this.py + 33, 0xFF9AA3B5, 0.85F, false);
    for (int i = 0; i < LEATHERS.length; i++) {
      this.button(s, this.leatherBtn(i), L.t("diary.cover." + LEATHERS[i]), mx, my, LEATHERS[i].equals(this.diary.leather));
    }
    G.text(s, L.t("diary.cover.trim"), this.px + 130, this.py + 125, 0xFF9AA3B5, 0.85F, false);
    for (int i = 0; i < TRIMS.length; i++) {
      this.button(s, this.trimBtn(i), L.t("diary.cover.trim" + Character.toUpperCase(TRIMS[i].charAt(0)) + TRIMS[i].substring(1)), mx, my, TRIMS[i].equals(this.diary.trim));
    }

    if (this.custom()) {
      String info = this.work < 0 ? L.t("diary.cover.white") : String.format("#%06X", this.work);
      if (this.work != this.diary.custom) {
        info += " · " + L.t("diary.cover.unsaved");
      }
      G.centered(s, info, this.px + this.pw / 2F, this.py + 168, 0xFFDFE4EE, 0.85F, false);
      // kleines Inventar wie in Minecraft
      float[] first = this.slot(0);
      G.rect(s, first[0] - 5, first[1] - 5, 9 * SLOT + 10, 2 * SLOT + 10, 0xFFC6C6C6);
      G.rect(s, first[0] - 5, first[1] - 5, 9 * SLOT + 10, 1, 0xFFFFFFFF);
      G.rect(s, first[0] - 5, first[1] - 5, 1, 2 * SLOT + 10, 0xFFFFFFFF);
      G.rect(s, first[0] - 5, first[1] + 2 * SLOT + 4, 9 * SLOT + 10, 1, 0xFF555555);
      G.rect(s, first[0] + 9 * SLOT + 4, first[1] - 5, 1, 2 * SLOT + 10, 0xFF555555);
      String tip = null;
      for (int i = 0; i < 18; i++) {
        float[] r = this.slot(i);
        boolean over = G.in(mx, my, r[0], r[1], r[2], r[3]);
        G.rect(s, r[0], r[1], SLOT, SLOT, 0xFF8B8B8B);
        G.rect(s, r[0], r[1], SLOT, 1, 0xFF373737);
        G.rect(s, r[0], r[1], 1, SLOT, 0xFF373737);
        G.rect(s, r[0], r[1] + SLOT - 1, SLOT, 1, 0xFFFFFFFF);
        G.rect(s, r[0] + SLOT - 1, r[1], 1, SLOT, 0xFFFFFFFF);
        if (i > WATER) {
          continue;
        }
        if (over || i == this.held) {
          G.rect(s, r[0] + 1, r[1] + 1, SLOT - 2, SLOT - 2, i == this.held ? 0x90F2C94C : 0x80FFFFFF);
        }
        G.icon(s, item(i), r[0] + 1, r[1] + 1, 16, 16);
        if (over) {
          tip = L.t(i == WATER ? "diary.cover.water" : "diary.dye." + Diary.DYE_IDS[i]);
        }
      }
      G.centered(s, tip != null ? tip : L.t("diary.cover.hint"), this.px + this.pw / 2F, this.py + 177, 0xFF9AA3B5, 0.75F, false);
      this.button(s, this.saveBtn(), L.t("diary.cover.save"), mx, my, this.work != this.diary.custom);
      if (this.held >= 0) {
        G.icon(s, item(this.held), mx - 8, my - 8, 16, 16); // Farbstoff haengt an der Maus
      }
    }
  }

  private void button(Stack s, float[] r, String label, float mx, float my, boolean selected) {
    boolean over = G.in(mx, my, r[0], r[1], r[2], r[3]);
    G.rect(s, r[0], r[1], r[2], r[3], selected ? 0xFF3A3010 : over ? 0xFF384052 : 0xFF2A2F3C);
    G.outline(s, r[0], r[1], r[2], r[3], 1, selected ? 0xFFF2C94C : 0xFF3A4152);
    G.centered(s, G.fit(label, r[2] - 6, 0.9F), r[0] + r[2] / 2F, r[1] + 4, selected ? 0xFFF2C94C : 0xFFFFFFFF, 0.9F, false);
  }

  private void apply() {
    if (this.held < 0) {
      return;
    }
    if (this.held == WATER) {
      this.work = -1;
      G.sound("item.bucket.empty", 1F);
    } else {
      this.work = Diary.mixDye(this.work, Diary.DYE_RGB[this.held]);
      G.sound("item.dye.use", 1F);
    }
  }

  @Override
  protected boolean onClick(float mx, float my, boolean left) {
    if (this.headerHit(mx, my)) {
      Screens.close();
      return true;
    }
    for (int i = 0; i < LEATHERS.length; i++) {
      float[] r = this.leatherBtn(i);
      if (G.in(mx, my, r[0], r[1], r[2], r[3])) {
        this.diary.leather = LEATHERS[i];
        this.work = this.diary.custom;
        this.held = -1;
        this.diary.save();
        G.click();
        return true;
      }
    }
    for (int i = 0; i < TRIMS.length; i++) {
      float[] r = this.trimBtn(i);
      if (G.in(mx, my, r[0], r[1], r[2], r[3])) {
        this.diary.trim = TRIMS[i];
        this.diary.save();
        G.click();
        return true;
      }
    }
    if (!this.custom()) {
      return true;
    }
    for (int i = 0; i <= WATER; i++) {
      float[] r = this.slot(i);
      if (G.in(mx, my, r[0], r[1], r[2], r[3])) {
        this.held = this.held == i ? -1 : i;
        this.dragging = this.held >= 0;
        G.click();
        return true;
      }
    }
    float[] b = this.book();
    if (G.in(mx, my, b[0], b[1], b[2], b[3])) {
      this.apply();
      return true;
    }
    float[] sv = this.saveBtn();
    if (G.in(mx, my, sv[0], sv[1], sv[2], sv[3])) {
      this.diary.custom = this.work;
      this.diary.save();
      G.sound("entity.player.levelup", 1.6F);
      return true;
    }
    this.held = -1;
    return true;
  }

  @Override
  protected boolean onRelease(float mx, float my) {
    if (this.dragging) {
      this.dragging = false;
      float[] b = this.book();
      if (this.custom() && G.in(mx, my, b[0], b[1], b[2], b[3])) {
        this.apply(); // Farbstoff wurde aufs Buch gezogen und losgelassen
      }
    }
    return false;
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
