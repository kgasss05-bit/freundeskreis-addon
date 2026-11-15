package de.ulgu.fkreis.core.ui;

import de.ulgu.fkreis.core.FkreisAddon;
import de.ulgu.fkreis.core.L;
import de.ulgu.fkreis.core.data.Birthdays;
import de.ulgu.fkreis.core.data.WoodType;
import de.ulgu.fkreis.core.gfx.G;
import de.ulgu.fkreis.core.net.Net;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.labymod.api.client.gui.screen.activity.AutoActivity;
import net.labymod.api.client.gui.screen.key.Key;
import net.labymod.api.client.render.matrix.Stack;

/**
 * Kalender: links die naechsten Geburtstage (mit Countdown), rechts ein Monatsblatt mit Koepfen.
 * Klick auf Kopf/Karte = Steckbrief. ‹ › blaettert Monate. "Aktualisieren" holt die Steckbriefe der Online-Freunde.
 */
@AutoActivity
public class CalendarActivity extends FkActivity {

  private static final String[] DAYS_DE = {"Mo", "Di", "Mi", "Do", "Fr", "Sa", "So"};
  private static final String[] DAYS_EN = {"Mo", "Tu", "We", "Th", "Fr", "Sa", "Su"};

  private YearMonth month = YearMonth.now();
  private List<Birthdays.Entry> list = new ArrayList<>();
  private long listAt;
  private final List<float[]> clickBoxes = new ArrayList<>();
  private final List<UUID> clickTargets = new ArrayList<>();

  private float bx, by, bw, bh, pageW, leftX, rightX, pageY, pageH;

  public CalendarActivity() {
    G.click();
  }

  private void layout() {
    this.bw = Math.min(470, this.w * 0.5F);
    this.bh = Math.min(this.h - 60, this.bw * 0.6F);
    this.bx = this.x + (this.w - this.bw) / 2F;
    this.by = this.y + (this.h - this.bh) / 2F;
    float border = 6, spine = 8;
    this.pageW = (this.bw - 2 * border - spine) / 2F;
    this.leftX = this.bx + border;
    this.rightX = this.bx + border + this.pageW + spine;
    this.pageY = this.by + border;
    this.pageH = this.bh - 2 * border;
  }

  private float[] refreshRect() {
    float w = G.width(L.t("cal.refresh")) + 14;
    return new float[] {this.rightX + this.pageW - w - 8, this.pageY + this.pageH - 18, w, 13};
  }

  private float[] arrow(boolean right) {
    float cy = this.pageY + 8;
    return right ? new float[] {this.rightX + this.pageW - 22, cy, 14, 12} : new float[] {this.rightX + 8, cy, 14, 12};
  }

  @Override
  protected void draw(Stack s, float mx, float my) {
    this.layout();
    if (System.currentTimeMillis() - this.listAt > 1000) {
      this.list = Birthdays.upcoming(FkreisAddon.get().net());
      this.listAt = System.currentTimeMillis();
    }
    this.clickBoxes.clear();
    this.clickTargets.clear();

    WoodType wood = WoodType.parse(FkreisAddon.get().configuration().notebook().wood().get());
    G.planks(s, this.x, this.y, this.w, this.h, wood.plank, 5);
    G.centered(s, L.t("cal.title"), this.x + this.w / 2F, this.by - 18, 0xFFF1E6CA, 1.4F, true);


    G.rect(s, this.bx + 4, this.by + 5, this.bw, this.bh, 0x50000000);
    G.rect(s, this.bx, this.by, this.bw, this.bh, 0xFF34353F);
    G.outline(s, this.bx, this.by, this.bw, this.bh, 1, 0xFF22232A);
    G.rect(s, this.leftX, this.pageY, this.pageW, this.pageH, 0xFFE5D4A3);
    G.rect(s, this.rightX, this.pageY, this.pageW, this.pageH, 0xFFE8DAAC);
    G.rect(s, this.leftX + this.pageW - 6, this.pageY, 6, this.pageH, 0x14000000);
    G.rect(s, this.rightX, this.pageY, 8, this.pageH, 0xFFCDBE90);

    this.drawList(s, mx, my);
    this.drawMonth(s, mx, my);

    String back = "‹ " + L.t("notes.back.close") + "  (Esc)";
    boolean bh2 = G.in(mx, my, this.x + 8, this.y + this.h - 18, G.width(back) + 8, 14);
    G.text(s, back, this.x + 12, this.y + this.h - 15, bh2 ? 0xFFFFE08A : 0xFFFFFFFF, 1F, true);
  }

  private void drawList(Stack s, float mx, float my) {
    float px = this.leftX + 10, py = this.pageY + 9, pw = this.pageW - 20;
    G.text(s, L.t("cal.next"), px, py, 0xFF3A2A1A, 1.1F);
    py += 16;
    if (this.list.isEmpty()) {
      for (String line : G.wrap(L.t("cal.empty"), pw, 0.85F)) {
        G.text(s, line, px, py, 0xFF8A7A5A, 0.85F);
        py += 10;
      }
      return;
    }
    float cardH = Math.min(34, (this.pageY + this.pageH - 8 - py) / 5F - 4);
    for (int i = 0; i < Math.min(5, this.list.size()); i++) {
      Birthdays.Entry e = this.list.get(i);
      boolean today = e.daysUntil == 0;
      boolean over = G.in(mx, my, px, py, pw, cardH);
      int bg = today ? 0xFFF6D9A0 : 0xFFF2E6C0;
      G.rect(s, px, py, pw, cardH, over ? G.shade(bg, 1.04F) : bg);
      G.outline(s, px, py, pw, cardH, 1, today ? 0xFFC8A860 : 0x30000000);
      float head = cardH - 8;
      G.icon(s, G.head(e.uuid), px + 4, py + 4, head, head);
      G.text(s, G.fit(e.name, pw - head - 64, 0.95F), px + head + 10, py + cardH / 2F - 8, 0xFF2A2218, 0.95F);
      G.text(s, e.date(), px + head + 10, py + cardH / 2F + 2, 0xFF7A6A4A, 0.8F);
      String when = today ? L.t("cal.today") : e.daysUntil == 1 ? L.t("cal.tomorrow") : L.t("cal.indays", e.daysUntil);
      float ww = G.width(when) * 0.85F;
      G.text(s, when, px + pw - ww - 6, py + cardH / 2F - 3, today ? 0xFFC04A2A : 0xFF7A6A4A, 0.85F);
      if (today) {
        G.icon(s, G.tex("ui/cake.png"), px + pw - ww - 20, py + cardH / 2F - 6, 11, 11);
      }
      this.clickBoxes.add(new float[] {px, py, pw, cardH});
      this.clickTargets.add(e.uuid);
      py += cardH + 4;
    }
  }

  private void drawMonth(Stack s, float mx, float my) {
    float px = this.rightX + 12, pw = this.pageW - 20;
    boolean de = L.german();
    String title = monthName(this.month.getMonthValue(), de) + " " + this.month.getYear();
    G.centered(s, title, this.rightX + this.pageW / 2F + 2, this.pageY + 9, 0xFF3A2A1A, 1.1F, false);
    for (boolean right : new boolean[] {false, true}) {
      float[] a = this.arrow(right);
      boolean over = G.in(mx, my, a[0], a[1], a[2], a[3]);
      G.centered(s, right ? "›" : "‹", a[0] + 7, a[1], over ? 0xFFB06A20 : 0xFF5A4A2A, 1.4F, false);
    }
    float cw = pw / 7F;
    float top = this.pageY + 26;
    String[] dn = de ? DAYS_DE : DAYS_EN;
    for (int i = 0; i < 7; i++) {
      G.centered(s, dn[i], px + i * cw + cw / 2F, top, 0xFF8A7A5A, 0.75F, false);
    }
    top += 10;
    int first = this.month.atDay(1).getDayOfWeek().getValue() - 1; // Mo = 0
    int days = this.month.lengthOfMonth();
    int rows = (first + days + 6) / 7;
    float ch = Math.min(26, (this.pageY + this.pageH - 24 - top) / rows);
    LocalDate today = LocalDate.now();
    for (int d = 1; d <= days; d++) {
      int p = first + d - 1;
      float cx = px + (p % 7) * cw, cy = top + (p / 7) * ch;
      boolean isToday = today.getYear() == this.month.getYear() && today.getMonthValue() == this.month.getMonthValue()
          && today.getDayOfMonth() == d;
      G.rect(s, cx + 1, cy + 1, cw - 2, ch - 2, isToday ? 0xFFD8E8C8 : 0xFFF2E6C0);
      G.text(s, String.valueOf(d), cx + 3, cy + 3, 0xFF5A4A2A, 0.65F);
      List<Birthdays.Entry> here = new ArrayList<>();
      for (Birthdays.Entry e : this.list) {
        if (e.day == d && e.month == this.month.getMonthValue()) {
          here.add(e);
        }
      }
      if (!here.isEmpty()) {
        G.outline(s, cx + 1, cy + 1, cw - 2, ch - 2, 1, 0xFFC04A2A);
        float hs = Math.min(ch - 9, cw - 6);
        Birthdays.Entry e = here.get((int) ((System.currentTimeMillis() / 1500) % here.size()));
        G.icon(s, G.head(e.uuid), cx + (cw - hs) / 2F, cy + ch - hs - 2, hs, hs);
        if (here.size() > 1) {
          G.text(s, "+" + (here.size() - 1), cx + cw - 9, cy + 2, 0xFFC04A2A, 0.6F);
        }
        this.clickBoxes.add(new float[] {cx, cy, cw, ch});
        this.clickTargets.add(e.uuid);
        if (G.in(mx, my, cx, cy, cw, ch)) {
          G.text(s, e.name, mx + 8, my - 10, 0xFFFFFFFF, 0.9F, true);
        }
      }
    }
    float[] r = this.refreshRect();
    boolean over = G.in(mx, my, r[0], r[1], r[2], r[3]);
    G.rect(s, r[0], r[1], r[2], r[3], over ? 0xFFD8C48A : 0xFFE5D4A3);
    G.outline(s, r[0], r[1], r[2], r[3], 1, 0xFFB9A57A);
    G.text(s, L.t("cal.refresh"), r[0] + 7, r[1] + 3, 0xFF3A2A1A, 0.8F);
  }

  private static String monthName(int m, boolean de) {
    String[] d = {"Januar", "Februar", "März", "April", "Mai", "Juni", "Juli", "August", "September", "Oktober", "November", "Dezember"};
    String[] e = {"January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December"};
    return (de ? d : e)[m - 1];
  }

  @Override
  protected boolean onClick(float mx, float my, boolean left) {
    for (boolean right : new boolean[] {false, true}) {
      float[] a = this.arrow(right);
      if (G.in(mx, my, a[0] - 3, a[1] - 3, a[2] + 6, a[3] + 6)) {
        this.month = right ? this.month.plusMonths(1) : this.month.minusMonths(1);
        G.click();
        return true;
      }
    }
    float[] r = this.refreshRect();
    if (G.in(mx, my, r[0], r[1], r[2], r[3])) {
      G.click();
      boolean ok = FkreisAddon.get().net().refreshBirthdays();
      Net.notify(L.t("cal.title"), ok ? L.t("cal.refreshing") : L.t("cal.wait"), null);
      return true;
    }
    for (int i = 0; i < this.clickBoxes.size(); i++) {
      float[] b = this.clickBoxes.get(i);
      if (G.in(mx, my, b[0], b[1], b[2], b[3])) {
        UUID u = this.clickTargets.get(i);
        G.click();
        Screens.now(new ProfileScrollActivity(u, FkreisAddon.get().net().nameOf(u)));
        return true;
      }
    }
    if (my > this.y + this.h - 20 && mx < this.x + 140) {
      Screens.close();
      return true;
    }
    return false;
  }

  @Override
  protected boolean onScroll(float mx, float my, float delta) {
    this.month = delta > 0 ? this.month.minusMonths(1) : this.month.plusMonths(1);
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
