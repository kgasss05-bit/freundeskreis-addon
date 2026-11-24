package de.ulgu.fkreis.core.zeitbrief;

import de.ulgu.fkreis.core.L;
import de.ulgu.fkreis.core.gfx.Backdrop;
import de.ulgu.fkreis.core.gfx.G;
import de.ulgu.fkreis.core.gfx.Sys;
import de.ulgu.fkreis.core.ui.FkActivity;
import de.ulgu.fkreis.core.ui.Screens;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import net.labymod.api.client.gui.screen.activity.AutoActivity;
import net.labymod.api.client.gui.screen.key.Key;
import net.labymod.api.client.gui.screen.key.KeyHandler;
import net.labymod.api.client.render.matrix.Stack;

/**
 * Zeitbrief: Zettel aus dem Umschlag ziehen, schreiben, zurueckschieben (der Brief geht von selbst zu),
 * Tag waehlen, verschicken. Am Tag selbst: Brief oeffnen, der Zettel kommt heraus.
 */
@AutoActivity
public class LetterActivity extends FkActivity {

  /** SEALED = der Brief ist zu, jetzt wird der Tag gewaehlt. */
  private enum St { WRITE, SEALED, SENT, ARRIVED, READ }

  private static final int EX = 40, EY = 150, MAX_LINES = 15;
  private static final DateTimeFormatter DMY = DateTimeFormatter.ofPattern("dd.MM.yyyy");

  private final Letters store = Letters.get();
  private St st = St.WRITE;
  private String text = "";
  private float out, outT, flap = -1, flapT = -1, fly;
  private boolean grabPaper, wasOut;
  private float grabY, grabOut;
  private Letters.Letter mail;
  private LocalDate due = LocalDate.now().plusYears(1);
  private YearMonth month = YearMonth.from(this.due);
  private long last = System.nanoTime(), paperAt;
  private float s, ox, oy, panelX, panelW, btnY;
  private final List<Object[]> buttons = new ArrayList<>();

  public LetterActivity() {
    this.mail = this.store.arrived();
    if (this.mail != null) { // ein Brief ist angekommen: versiegelt zeigen
      this.st = St.ARRIVED;
      this.text = this.mail.text;
      this.flap = this.flapT = 1;
    }
    G.click();
  }

  /** Zurueck aus dem Siegel-Editor: da weitermachen, wo man war. */
  LetterActivity(LetterActivity prev) {
    this.st = prev.st;
    this.text = prev.text;
    this.out = prev.out;
    this.outT = prev.outT;
    this.flap = prev.flap;
    this.flapT = prev.flapT;
    this.wasOut = prev.wasOut;
  }

  private float lx(float mx) {
    return (mx - this.ox) / this.s;
  }

  private float ly(float my) {
    return (my - this.oy) / this.s;
  }

  private float paperTop() {
    return 156 - this.out * 148;
  }

  private void layout() {
    this.panelW = 190;
    this.s = Math.max(0.8F, Math.min(2.6F, Math.min((this.h - 48) / 260F, (this.w - this.panelW - 40) / 240F)));
    float total = 240 * this.s + 14 + this.panelW;
    this.ox = this.x + (this.w - total) / 2F;
    this.oy = this.y + 38 + Math.max(0, (this.h - 48 - 260 * this.s) / 2F);
    this.panelX = this.ox + 240 * this.s + 14;
  }

  private List<String> lines(String t) {
    List<String> outLines = new ArrayList<>();
    float ts = 0.62F * this.s;
    for (String para : t.split("\n", -1)) {
      if (para.isEmpty()) {
        outLines.add("");
      } else {
        outLines.addAll(G.wrap(para, 124 * this.s, ts));
      }
    }
    return outLines;
  }

  private void tick(float dt, float mx, float my) {
    float lx = this.lx(mx), ly = this.ly(my);
    if (this.out > 0.95F) {
      this.wasOut = true;
    }
    if (this.st == St.WRITE && !this.grabPaper && this.wasOut && this.out == 0 && this.outT == 0 && !this.text.trim().isEmpty()) {
      this.st = St.SEALED; // Zettel ist wieder drin: der Brief geht von selbst zu
      this.flapT = 1;
      G.sound("item.book.put", 1F);
    }
    if (this.grabPaper) {
      this.out = this.outT = Math.max(0, Math.min(1, this.grabOut + (this.grabY - ly) / 148F));
    } else {
      this.out += (this.outT - this.out) * Math.min(1, dt * 9);
      if (Math.abs(this.outT - this.out) < 0.004F) {
        this.out = this.outT;
      }
    }
    this.flap += (this.flapT - this.flap) * Math.min(1, dt * 7);
    if (Math.abs(this.flapT - this.flap) < 0.01F) {
      this.flap = this.flapT;
    }
    if (this.st == St.SENT) {
      this.fly = Math.min(1, this.fly + dt / 0.9F);
    }
    long now = System.currentTimeMillis();
    if (this.paperAt > 0 && now >= this.paperAt) {
      this.paperAt = 0;
      this.outT = 1;
      G.sound("item.book.page_turn", 1F);
    }
  }

  @Override
  protected void draw(Stack g, float mx, float my) {
    this.layout();
    long nowN = System.nanoTime();
    float dt = Math.min(0.05F, (nowN - this.last) / 1.0e9F);
    this.last = nowN;
    this.tick(dt, mx, my);
    float s = this.s;
    Backdrop.draw(g, this.store.background, this.x, this.y, this.w, this.h);
    this.header(g, L.t("letter.title"), mx, my);

    float fy = -this.fly * 300;
    float ex = this.ox + EX * s, ey = this.oy + (EY + fy) * s, ew = LetterArt.EW * s, eh = LetterArt.EH * s;
    if (this.fly < 1 && ey + eh > this.y) {
      G.rect(g, ex + 3 * s, ey + eh, ew, 4 * s, 0x66000000);
      G.icon(g, LetterArt.back(), ex, ey, ew, eh);
      if (this.flap < 0) {
        float fh = LetterArt.APEX * s * -this.flap;
        G.icon(g, LetterArt.flapUp(), ex, ey - fh, ew, fh);
      }
      if (this.flap <= 0.2F) { // Zettel
        float pt = this.paperTop() + fy, pb = Math.min(pt + 150, EY + fy + LetterArt.EH - 3);
        float px = this.ox + 52 * s, py = this.oy + pt * s, ph = (pb - pt) * s;
        if (ph > 0) {
          G.rect(g, px, py, 136 * s, ph, 0xFFFBF6E6);
          G.rect(g, px, py, 136 * s, s, 0xFFFFFFFF);
          G.rect(g, px + 135 * s, py, s, ph, 0xFFD6CCA8);
          for (float yy = pt + 24; yy < pb - 2; yy += 8) {
            G.rect(g, px + 6 * s, this.oy + yy * s, 124 * s, Math.max(1, s / 2F), 0xFFE0D6B4);
          }
          for (int i = 0; i < 3; i++) { // Griff zum Ziehen
            G.rect(g, px + 48 * s, py + (4 + i * 3) * s, 40 * s, Math.max(1, s / 2F), 0xFFA89C74);
          }
          if (this.out > 0.95F) {
            List<String> ls = this.lines(this.text);
            boolean caret = this.st == St.WRITE && (System.currentTimeMillis() / 500) % 2 == 0;
            for (int i = 0; i < ls.size() && i < MAX_LINES; i++) {
              String line = ls.get(i) + (caret && i == ls.size() - 1 ? "_" : "");
              G.text(g, line, px + 6 * s, py + (17 + i * 8) * s, 0xFF2A2018, 0.62F * s, false);
            }
            if (this.st == St.WRITE && this.text.isEmpty()) {
              G.text(g, L.t("letter.placeholder"), px + 6 * s, py + 17 * s, 0xFFA89C74, 0.62F * s, false);
            }
          } else if (!this.text.isEmpty()) {
            for (int i = 0; i < 3; i++) {
              G.rect(g, px + 10 * s, py + (27 + i * 8) * s, (60 + (i * 37) % 50) * s, Math.max(1, s / 2F), 0xFF8A8068);
            }
          }
        }
      }
      G.icon(g, LetterArt.front(), ex, ey, ew, eh);
      if (this.flap > 0) {
        G.icon(g, LetterArt.flapDown(), ex, ey, ew, (LetterArt.APEX + 2) * s * this.flap);
      }
    }
    this.panel(g, mx, my);
  }

  // ------------------------------------------------------------------ rechte Spalte

  private void add(String label, boolean gold, Runnable action) {
    this.buttons.add(new Object[] {label, gold, action});
  }

  private float[] btnRect(int i) {
    return new float[] {this.panelX, this.btnY + i * 21, this.panelW, 17};
  }

  private float[] dayRect(int cell) {
    float cw = this.panelW / 7F;
    return new float[] {this.panelX + (cell % 7) * cw, this.btnY - 104 + (cell / 7) * 13, cw, 13};
  }

  private boolean pickable(LocalDate d) {
    LocalDate today = LocalDate.now();
    return d.isAfter(today) && !d.isAfter(today.plusYears(10));
  }

  private void panel(Stack g, float mx, float my) {
    this.buttons.clear();
    String hint;
    switch (this.st) {
      case WRITE:
        if (this.out > 0.95F) {
          hint = L.t("letter.hintWrite");
        } else if (this.text.trim().isEmpty()) {
          hint = L.t("letter.hintPull");
        } else {
          hint = L.t("letter.hintClose");
        }
        break;
      case SEALED:
        hint = L.t("letter.hintDate", this.due.format(DMY));
        this.add(L.t("letter.in1m"), false, () -> this.pick(LocalDate.now().plusMonths(1)));
        this.add(L.t("letter.in1y"), false, () -> this.pick(LocalDate.now().plusYears(1)));
        this.add(L.t("letter.in10y"), false, () -> this.pick(LocalDate.now().plusYears(10)));
        this.add(L.t("letter.send"), true, this::send);
        break;
      case SENT:
        hint = L.t("letter.hintSent");
        this.add(L.t("letter.another"), true, () -> Screens.now(new LetterActivity()));
        break;
      case ARRIVED:
        hint = L.t("letter.hintArrived", this.date(this.mail.written));
        this.add(L.t("letter.openMail"), true, this::openMail);
        break;
      default:
        hint = L.t("letter.hintRead", this.date(this.mail.written));
        this.add(L.t("letter.done"), true, Screens::close);
        break;
    }
    float py = this.oy + 20;
    for (String line : G.wrap(hint, this.panelW, 0.9F)) {
      G.text(g, line, this.panelX, py, 0xFFFFFFFF, 0.9F, true);
      py += 10;
    }
    py += 6;
    if (this.st == St.SEALED) { // schlichter Kalender zum Tag-Aussuchen
      G.rect(g, this.panelX - 4, py - 3, this.panelW + 8, 112, 0xD0101319);
      String title = this.month.getMonthValue() + " / " + this.month.getYear();
      G.centered(g, title, this.panelX + this.panelW / 2F, py + 1, 0xFFF2C94C, 1F, false);
      G.text(g, "<<", this.panelX + 2, py + 1, 0xFFDFE4EE, 1F, false);
      G.text(g, "<", this.panelX + 26, py + 1, 0xFFDFE4EE, 1F, false);
      G.text(g, ">", this.panelX + this.panelW - 32, py + 1, 0xFFDFE4EE, 1F, false);
      G.text(g, ">>", this.panelX + this.panelW - 16, py + 1, 0xFFDFE4EE, 1F, false);
      this.btnY = py + 118;
      int first = this.month.atDay(1).getDayOfWeek().getValue() - 1; // Montag = 0
      for (int d = 1; d <= this.month.lengthOfMonth(); d++) {
        float[] r = this.dayRect(first + d - 1 + 7);
        LocalDate date = this.month.atDay(d);
        boolean ok = this.pickable(date), sel = date.equals(this.due);
        if (sel) {
          G.rect(g, r[0] + 1, r[1], r[2] - 2, r[3] - 1, 0xFFF2C94C);
        } else if (ok && G.in(mx, my, r[0], r[1], r[2], r[3])) {
          G.rect(g, r[0] + 1, r[1], r[2] - 2, r[3] - 1, 0xFF3A4152);
        }
        G.centered(g, String.valueOf(d), r[0] + r[2] / 2F, r[1] + 3, sel ? 0xFF1A1400 : ok ? 0xFFFFFFFF : 0xFF5A6070, 0.8F, false);
      }
    } else {
      this.btnY = py;
    }
    for (int i = 0; i < this.buttons.size(); i++) {
      float[] r = this.btnRect(i);
      boolean gold = (Boolean) this.buttons.get(i)[1], over = G.in(mx, my, r[0], r[1], r[2], r[3]);
      G.rect(g, r[0], r[1], r[2], r[3], gold ? (over ? 0xFFFFDB6E : 0xFFF2C94C) : over ? 0xFF4A4F5C : 0xFF3A3E48);
      G.rect(g, r[0], r[1] + r[3] - 2, r[2], 2, gold ? 0xFF8A6A10 : 0xFF15171C);
      G.centered(g, G.fit((String) this.buttons.get(i)[0], r[2] - 6, 0.9F), r[0] + r[2] / 2F, r[1] + 4, gold ? 0xFF1A1400 : 0xFFFFFFFF, 0.9F, false);
    }
  }

  private String date(String iso) {
    try {
      return LocalDate.parse(iso).format(DMY);
    } catch (RuntimeException e) {
      return iso;
    }
  }

  private void pick(LocalDate d) {
    this.due = d;
    this.month = YearMonth.from(d);
  }

  /** Der Tag ist da: Brief aufmachen, der Zettel kommt heraus. Damit ist er zugestellt. */
  private void openMail() {
    if (this.st != St.ARRIVED) {
      return;
    }
    this.st = St.READ;
    this.flapT = -1;
    this.paperAt = System.currentTimeMillis() + 500;
    this.store.letters.remove(this.mail);
    this.store.save();
    G.sound("item.book.put", 1.2F);
  }

  private void send() {
    if (!this.pickable(this.due)) {
      return;
    }
    if (this.store.letters.size() >= Letters.MAX_WAITING) {
      de.ulgu.fkreis.core.net.Net.notify(L.t("letter.title"), L.t("letter.tooMany"), null);
      return;
    }
    Letters.Letter l = new Letters.Letter();
    l.text = this.text;
    l.written = LocalDate.now().toString();
    l.due = this.due.toString();
    this.store.letters.add(l);
    this.store.save();
    this.st = St.SENT;
    G.sound("entity.arrow.shoot", 1.3F);
  }

  // ------------------------------------------------------------------ Maus und Tasten

  @Override
  protected boolean onClick(float mx, float my, boolean left) {
    this.layout();
    if (this.headerHit(mx, my)) {
      Screens.close();
      return true;
    }
    float lx = this.lx(mx), ly = this.ly(my);
    if (this.st == St.WRITE && this.flap < -0.9F) { // Zettel packen
      float pt = this.paperTop();
      if (lx >= 52 && lx <= 188 && ly >= pt - 6 && ly <= pt + (this.out > 0.5F ? 16 : 60)) {
        this.grabPaper = true;
        this.grabY = ly;
        this.grabOut = this.out;
        return true;
      }
    }
    if (this.st == St.ARRIVED && lx >= EX && lx <= EX + LetterArt.EW && ly >= EY && ly <= EY + LetterArt.EH) {
      this.openMail(); // ein Klick auf den Umschlag oeffnet ihn auch
      return true;
    }
    if (this.st == St.SEALED) {
      float top = this.btnY - 118;
      if (my >= top - 2 && my <= top + 11) { // Monat / Jahr blaettern
        float rel = mx - this.panelX;
        YearMonth m = rel >= 0 && rel < 22 ? this.month.minusYears(1) : rel >= 22 && rel < 44 ? this.month.minusMonths(1)
            : rel > this.panelW - 20 && rel <= this.panelW ? this.month.plusYears(1) : rel > this.panelW - 40 && rel <= this.panelW - 20 ? this.month.plusMonths(1) : null;
        YearMonth min = YearMonth.now(), max = YearMonth.now().plusYears(10);
        if (m != null) {
          this.month = m.isBefore(min) ? min : m.isAfter(max) ? max : m;
          G.click();
          return true;
        }
      }
      int first = this.month.atDay(1).getDayOfWeek().getValue() - 1;
      for (int d = 1; d <= this.month.lengthOfMonth(); d++) {
        float[] r = this.dayRect(first + d - 1 + 7);
        if (G.in(mx, my, r[0], r[1], r[2], r[3]) && this.pickable(this.month.atDay(d))) {
          this.due = this.month.atDay(d);
          G.click();
          return true;
        }
      }
    }
    for (int i = 0; i < this.buttons.size(); i++) {
      float[] r = this.btnRect(i);
      if (G.in(mx, my, r[0], r[1], r[2], r[3])) {
        G.click();
        ((Runnable) this.buttons.get(i)[2]).run();
        return true;
      }
    }
    return true;
  }

  @Override
  protected boolean onRelease(float mx, float my) {
    if (this.grabPaper) {
      this.grabPaper = false;
      this.outT = this.out > 0.5F ? 1 : 0;
      G.sound("item.book.page_turn", this.outT > 0 ? 1.1F : 0.9F);
    }
    return false;
  }

  private void type(String add) {
    StringBuilder sb = new StringBuilder(this.text);
    for (int i = 0; i < add.length() && sb.length() < Letters.MAX_CHARS; i++) {
      char c = add.charAt(i);
      if (c == '\n' || c >= 32) {
        sb.append(c);
        if (this.lines(sb.toString()).size() > MAX_LINES) { // der Zettel ist voll
          sb.setLength(sb.length() - 1);
          break;
        }
      }
    }
    this.text = sb.toString();
  }

  private boolean writing() {
    return this.st == St.WRITE && this.out > 0.95F;
  }

  @Override
  protected boolean onKey(Key key) {
    if (key == Key.ESCAPE) {
      Screens.close();
      return true;
    }
    if (!this.writing()) {
      return false;
    }
    if (KeyHandler.isPaste(key)) {
      this.type(Sys.clipboard().replace("\r\n", "\n").replace('\r', '\n').replace('\t', ' '));
      return true;
    }
    if (key == Key.ENTER) {
      this.type("\n");
      return true;
    }
    if (key == Key.BACK) {
      if (!this.text.isEmpty()) {
        this.text = this.text.substring(0, this.text.length() - 1);
      }
      return true;
    }
    return false;
  }

  @Override
  protected boolean onChar(char c) {
    if (!this.writing() || c < 32) {
      return false;
    }
    this.type(String.valueOf(c));
    return true;
  }
}
