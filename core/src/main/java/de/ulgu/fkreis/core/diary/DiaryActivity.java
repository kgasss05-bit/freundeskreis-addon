package de.ulgu.fkreis.core.diary;

import de.ulgu.fkreis.api.DiaryState;
import de.ulgu.fkreis.core.L;
import de.ulgu.fkreis.core.gfx.G;
import de.ulgu.fkreis.core.net.DiaryShare;
import de.ulgu.fkreis.core.ui.FkActivity;
import de.ulgu.fkreis.core.ui.Screens;
import java.util.List;
import net.labymod.api.Laby;
import net.labymod.api.client.gui.screen.activity.AutoActivity;
import net.labymod.api.client.gui.screen.key.Key;
import net.labymod.api.client.gui.screen.key.KeyHandler;
import net.labymod.api.client.render.matrix.Stack;

/**
 * Das aufgeschlagene Tagebuch: zwei Seiten im Ledereinband, jede Seite ein Tag.
 * Vorn das Namensblatt, die letzte Seite ist immer heute und nur dort kann man schreiben.
 * Geblaettert wird mit den Pfeilen unten links und rechts (oder dem Mausrad), wie im Minecraft-Buch.
 */
@AutoActivity
public class DiaryActivity extends FkActivity {

  private static final float LH = 11F;
  private static final int INK = 0xFF2C2540, SEPIA = 0xFF8A7245, SEPIA_DARK = 0xFF5D4721, ORN = 0xFFB39A62, PAGE = 0xFFF3E6C2;

  private Diary diary = Diary.get();
  /** Geteiltes Tagebuch einer anderen Person (nur lesen), sonst null. */
  private final DiaryShare.Remote remote;
  private boolean readOnly;
  private boolean loaded;
  private String ownerName;
  private boolean shareOpen;
  private final boolean quitAfter;
  /** Alle Buchseiten: Namensblatt, dann je Tag so viele Seiten, wie der Text braucht. */
  private final List<Pg> pages = new java.util.ArrayList<>();
  private String builtKey = "";
  private String today = Diary.today();
  private int view;
  private int maxLines = 12;
  private float bodyW = 140;
  private boolean jumpToEnd = true;
  /** Schreibmarke: Position im heutigen Text. -1 = noch nicht gesetzt (dann ans Ende). */
  private int caret = -1;
  private List<Ln> todayLines = new java.util.ArrayList<>();

  /** Eine Buchseite: Tag (oder Namensblatt), der wievielte Teil des Tages und die Zeilen darauf. */
  /** Eine umbrochene Zeile mit ihrer Position im Text (start bis end), damit man in den Text klicken kann. */
  /** Unsichtbares Zeichen im Text: "hier beginnt eine neue Seite". */
  private static final char BREAK = '\f';
  private static final String BREAK_S = "\f";

  private static final class Ln {
    final int start, end;
    final String text;

    Ln(String all, int start, int end) {
      this.start = start;
      this.end = end;
      String t = all.substring(start, end);
      this.text = t.endsWith("\n") || t.endsWith(BREAK_S) ? t.substring(0, t.length() - 1) : t;
    }
  }

  private static final java.util.Map<Character, Float> CW = new java.util.HashMap<>();

  private static float cw(char c) {
    Float w = CW.get(c);
    if (w == null) {
      w = G.width(String.valueOf(c));
      CW.put(c, w);
    }
    return w;
  }

  private static float tw(String t, int from, int to) {
    float w = 0;
    for (int i = from; i < to; i++) {
      w += cw(t.charAt(i));
    }
    return w;
  }

  /** Zeilenumbruch, bei dem jedes Zeichen genau zu einer Zeile gehoert (wichtig fuer den Cursor). */
  private static List<Ln> wrap(String t, float maxW, int perPage) {
    List<Ln> out = new java.util.ArrayList<>();
    int n = t.length(), ls = 0, i = 0, lastSpace = -1;
    float w = 0;
    while (i < n) {
      char c = t.charAt(i);
      if (c == BREAK) { // Seitenwechsel: Rest der Seite bleibt leer
        out.add(new Ln(t, ls, i + 1));
        while (out.size() % perPage != 0) {
          out.add(new Ln(t, i + 1, i + 1));
        }
        ls = i + 1;
        lastSpace = -1;
        w = 0;
        i++;
        continue;
      }
      if (c == '\n') {
        out.add(new Ln(t, ls, i + 1));
        ls = i + 1;
        lastSpace = -1;
        w = 0;
        i++;
        continue;
      }
      float cwid = cw(c);
      if (w + cwid > maxW && i > ls) {
        if (c == ' ') { // Leerzeichen darf am Zeilenende ueberhaengen
          out.add(new Ln(t, ls, i + 1));
          ls = i + 1;
          lastSpace = -1;
          w = 0;
          i++;
          continue;
        }
        int br = lastSpace >= ls ? lastSpace + 1 : i;
        out.add(new Ln(t, ls, br));
        ls = br;
        lastSpace = -1;
        w = tw(t, ls, i);
        continue;
      }
      if (c == ' ') {
        lastSpace = i;
      }
      w += cwid;
      i++;
    }
    out.add(new Ln(t, ls, n));
    return out;
  }

  private static final class Pg {
    final String id;
    final int part, parts;
    final List<Ln> lines;

    Pg(String id, int part, int parts, List<Ln> lines) {
      this.id = id;
      this.part = part;
      this.parts = parts;
      this.lines = lines;
    }
  }
  private long dirtyAt;
  private boolean done;
  /** Das Buch liegt zuerst geschlossen da (Einband), ein Klick schlaegt es auf. */
  private boolean cover;
  private String flash = "";
  private long flashAt;
  private float cx0, cy0, cw0, ch0;
  private float bx, by, bw, bh;

  public DiaryActivity(boolean quitAfter) {
    this.quitAfter = quitAfter;
    this.remote = null;
    this.ownerName = Laby.labyAPI().getName();
    this.cover = !quitAfter; // nach "Ja, schreiben" beim Beenden geht es direkt zur heutigen Seite
    if (quitAfter) {
      DiaryState.screens++;
      G.sound("item.book.page_turn", 0.9F);
    } else {
      G.click();
    }
  }

  /** Tagebuch einer anderen Person oeffnen, die ihren Code weitergegeben hat. Nur zum Lesen. */
  public DiaryActivity(DiaryShare.Remote remote) {
    this.quitAfter = false;
    this.remote = remote;
    this.readOnly = true;
    this.cover = true;
    this.ownerName = "?";
    this.today = ""; // im fremden Buch ist keine Seite "heute", also nichts beschreibbar
    G.click();
  }

  /** Geteiltes Tagebuch: warten, bis alle Stuecke da sind. Gibt true zurueck, solange noch nichts zu zeigen ist. */
  private boolean loading(Stack s) {
    if (this.remote == null || this.loaded) {
      return false;
    }
    DiaryShare.Packet p = this.remote.packet;
    if (p != null) {
      Diary d = new Diary();
      for (String[] e : p.e) {
        if (e != null && e.length >= 3) {
          Diary.Entry en = new Diary.Entry();
          en.time = e[1];
          en.text = e[2];
          d.entries.put(e[0], en);
        }
      }
      d.leather = "eigen";
      d.custom = p.l & 0xFFFFFF;
      d.trim = p.t == null ? "none" : p.t;
      this.diary = d;
      this.ownerName = p.n == null || !p.n.matches("[A-Za-z0-9_]{1,16}") ? "?" : p.n;
      this.loaded = true;
      this.jumpToEnd = false;
      this.view = 0;
      this.builtKey = "";
      return false;
    }
    float cx = this.x + this.w / 2F, cy = this.y + this.h / 2F;
    long waited = System.currentTimeMillis() - this.remote.started;
    String msg;
    if (this.remote.failed) {
      msg = L.t("diary.shared.failed");
    } else if (this.remote.total > 0) {
      msg = L.t("diary.shared.loading") + " " + this.remote.received + " / " + this.remote.total;
    } else if (waited > 8000L) {
      msg = L.t("diary.shared.nobody");
    } else {
      msg = L.t("diary.shared.loading");
    }
    int i = 0;
    for (String line : G.wrap(msg, Math.min(this.w - 40, 300), 1F)) {
      G.centered(s, line, cx, cy - 10 + i++ * 11, 0xFFFFFFFF, 1F, true);
    }
    return true;
  }

  private float[] shareBtn() {
    return new float[] {this.x + this.w - 22 - 92, this.y + 15, 92, 16};
  }

  private float[] sharePanel() {
    float pw = Math.min(this.w - 20, 250), ph = 132;
    return new float[] {this.x + (this.w - pw) / 2F, this.y + (this.h - ph) / 2F, pw, ph};
  }

  private float[] shareAct(int i) {
    float[] p = this.sharePanel();
    float bw2 = (p[2] - 30) / 2F;
    return new float[] {p[0] + 10 + (i % 2) * (bw2 + 10), p[1] + 84 + (i / 2) * 21, bw2, 17};
  }

  private void smallBtn(Stack s, float[] r, String label, float mx, float my, boolean gold) {
    boolean over = G.in(mx, my, r[0], r[1], r[2], r[3]);
    G.rect(s, r[0], r[1], r[2], r[3], over ? 0xFF384052 : 0xFF2A2F3C);
    G.outline(s, r[0], r[1], r[2], r[3], 1, gold ? 0xFFF2C94C : 0xFF3A4152);
    G.centered(s, G.fit(label, r[2] - 6, 0.85F), r[0] + r[2] / 2F, r[1] + (r[3] - 7) / 2F, gold ? 0xFFF2C94C : 0xFFFFFFFF, 0.85F, false);
  }

  /** Kleines Fenster "Tagebuch teilen": Code erzeugen, kopieren, erneuern oder das Teilen beenden. */
  private void drawShare(Stack s, float mx, float my) {
    float[] p = this.sharePanel();
    G.rect(s, this.x, this.y, this.w, this.h, 0x90000000);
    G.rect(s, p[0], p[1], p[2], p[3], 0xF51C1F28);
    G.outline(s, p[0], p[1], p[2], p[3], 1, 0xFF3A4152);
    G.rect(s, p[0], p[1], p[2], 2, 0xFFF2C94C);
    float cx = p[0] + p[2] / 2F;
    G.centered(s, L.t("diary.share"), cx, p[1] + 9, 0xFFF2C94C, 1.1F, true);
    boolean on = !this.diary.shareCode.isEmpty();
    int i = 0;
    for (String line : G.wrap(L.t(on ? "diary.shareOnText" : "diary.shareOffText"), p[2] - 20, 0.8F)) {
      if (i < 4) {
        G.centered(s, line, cx, p[1] + 24 + i * 9, 0xFFC8CEDE, 0.8F, false);
      }
      i++;
    }
    if (on) {
      G.rect(s, cx - 62, p[1] + 62, 124, 16, 0xFF12141C);
      G.outline(s, cx - 62, p[1] + 62, 124, 16, 1, 0xFFF2C94C);
      G.centered(s, this.diary.shareCode, cx, p[1] + 66, 0xFFFFFFFF, 1.1F, false);
      this.smallBtn(s, this.shareAct(0), L.t("diary.shareCopy"), mx, my, true);
      this.smallBtn(s, this.shareAct(1), L.t("diary.shareNew"), mx, my, false);
      this.smallBtn(s, this.shareAct(2), L.t("diary.shareStop"), mx, my, false);
      this.smallBtn(s, this.shareAct(3), L.t("diary.shareClose"), mx, my, false);
    } else {
      this.smallBtn(s, this.shareAct(0), L.t("diary.shareCreate"), mx, my, true);
      this.smallBtn(s, this.shareAct(1), L.t("diary.shareClose"), mx, my, false);
    }
  }

  private boolean clickShare(float mx, float my) {
    boolean on = !this.diary.shareCode.isEmpty();
    for (int i = 0; i < (on ? 4 : 2); i++) {
      float[] r = this.shareAct(i);
      if (!G.in(mx, my, r[0], r[1], r[2], r[3])) {
        continue;
      }
      G.click();
      int act = on ? i : (i == 0 ? 1 : 3); // 0 kopieren, 1 neuer Code, 2 beenden, 3 schliessen
      if (act == 0) {
        this.say(Clip.set(this.diary.shareCode) ? "diary.shareCopied" : "diary.shareCopyFail");
      } else if (act == 1) {
        this.diary.shareCode = DiaryShare.newCode();
        this.diary.save();
      } else if (act == 2) {
        this.diary.shareCode = "";
        this.diary.save();
      } else {
        this.shareOpen = false;
      }
      return true;
    }
    float[] p = this.sharePanel();
    if (!G.in(mx, my, p[0], p[1], p[2], p[3])) {
      this.shareOpen = false;
    }
    return true;
  }

  private void drawFlash(Stack s) {
    if (!this.flash.isEmpty() && System.currentTimeMillis() - this.flashAt < 1800L) {
      float fw = G.width(this.flash) * 0.9F + 12;
      G.rect(s, this.x + (this.w - fw) / 2F, this.y + this.h - 20, fw, 13, 0xE01C1F28);
      G.centered(s, this.flash, this.x + this.w / 2F, this.y + this.h - 17, 0xFFF2C94C, 0.9F, false);
    }
  }

  private Pg pg(int i) {
    return i >= 0 && i < this.pages.size() ? this.pages.get(i) : null;
  }

  private boolean isToday(Pg p) {
    return p != null && this.today.equals(p.id);
  }

  /** Schreiben geht, sobald eine heutige Seite aufgeschlagen ist. */
  private boolean writable() {
    return !this.readOnly && this.isToday(this.pg(this.view)) || this.isToday(this.pg(this.view + 1));
  }

  /** Masse des Buchs und daraus: wie viele Zeilen auf eine Seite passen. */
  private void layout() {
    this.bw = Math.min(this.w - 30, 440);
    this.bh = Math.min(this.h - 46, 262);
    this.bx = this.x + (this.w - this.bw) / 2F;
    this.by = this.y + (this.h - this.bh) / 2F + 8;
    float half = (this.bw - 18) / 2F, ph = this.bh - 18;
    this.maxLines = Math.max(3, (int) ((ph - 22 - 37) / LH));
    this.bodyW = half - 44;
  }

  /** Verteilt die Eintraege auf Seiten. Ein langer Tag bekommt einfach mehrere Seiten. */
  private void rebuild() {
    String todayText = this.diary.text(this.today);
    if (this.caret < 0 || this.caret > todayText.length()) {
      this.caret = todayText.length();
    }
    String key = this.maxLines + "|" + this.bodyW + "|" + todayText.length() + "|" + todayText.hashCode();
    if (!key.equals(this.builtKey)) {
      this.builtKey = key;
      this.pages.clear();
      for (String id : this.diary.pages()) {
        if (Diary.PLATE.equals(id)) {
          this.pages.add(new Pg(id, 0, 1, new java.util.ArrayList<>()));
          continue;
        }
        if (this.readOnly && this.diary.text(id).trim().isEmpty()) {
          continue; // leere Seite "heute" gibt es im fremden Buch nicht
        }
        List<Ln> wrapped = wrap(this.diary.text(id), this.bodyW, this.maxLines);
        if (this.today.equals(id)) {
          this.todayLines = wrapped;
        }
        int parts = Math.max(1, (wrapped.size() + this.maxLines - 1) / this.maxLines);
        for (int p = 0; p < parts; p++) {
          int from = Math.min(wrapped.size(), p * this.maxLines), to = Math.min(wrapped.size(), from + this.maxLines);
          this.pages.add(new Pg(id, p, parts, new java.util.ArrayList<>(wrapped.subList(from, to))));
        }
      }
    }
    if (this.jumpToEnd) { // beim Oeffnen und beim Schreiben: die Seite mit der Schreibmarke zeigen
      this.jumpToEnd = false;
      int target = this.pages.size() - 1, line = this.caretLine();
      for (int i = 0; i < this.pages.size(); i++) {
        Pg p = this.pages.get(i);
        if (this.today.equals(p.id) && p.part == line / this.maxLines) {
          target = i;
        }
      }
      int nv = target - (target % 2);
      if (nv != this.view && this.view != 0) {
        G.sound("item.book.page_turn", 1F);
      }
      this.view = nv;
    }
    this.view = Math.max(0, Math.min(this.view, this.pages.size() - 1));
    this.view -= this.view % 2;
  }

  /** In welcher Zeile des heutigen Eintrags steht die Schreibmarke? */
  private int caretLine() {
    for (int i = 0; i < this.todayLines.size(); i++) {
      Ln l = this.todayLines.get(i);
      boolean last = i == this.todayLines.size() - 1;
      if (this.caret >= l.start && (this.caret < l.end || (last && this.caret == l.end))) {
        return i;
      }
    }
    return Math.max(0, this.todayLines.size() - 1);
  }

  /** Textposition in einer Zeile, die am naechsten an der Pixel-Position x liegt. */
  private int offsetAt(Ln l, float x) {
    String all = this.diary.text(this.today);
    float w = 0;
    int max = l.start + l.text.length();
    for (int i = l.start; i < max; i++) {
      float c = cw(all.charAt(i));
      if (x < w + c / 2F) {
        return i;
      }
      w += c;
    }
    return max;
  }

  @Override
  protected void draw(Stack s, float mx, float my) {
    G.rect(s, this.x, this.y, this.w, this.h, G.alpha(0xFF000000, Math.min(0.62F, this.age() / 0.2F * 0.62F)));
    this.header(s, this.readOnly ? L.t("diary.shared.title", this.ownerName) : L.t("diary.title"), mx, my);
    if (this.loading(s)) {
      return;
    }
    if (!this.readOnly) {
      this.smallBtn(s, this.shareBtn(), L.t("diary.share"), mx, my, !this.diary.shareCode.isEmpty());
    }
    if (this.cover) {
      this.drawCover(s, mx, my);
      if (this.shareOpen) {
        this.drawShare(s, mx, my);
      }
      this.drawFlash(s);
      return;
    }

    this.layout();
    this.rebuild();
    int leather = 0xFF000000 | this.diary.leatherColor();

    // Ledereinband mit Naht
    G.rect(s, this.bx + 3, this.by + 4, this.bw, this.bh, 0x60000000);
    G.rect(s, this.bx - 1, this.by - 1, this.bw + 2, this.bh + 2, 0xFF1A0F08);
    G.rect(s, this.bx, this.by, this.bw, this.bh, leather);
    G.rect(s, this.bx, this.by, this.bw, 1, G.shade(leather, 1.25F));
    G.rect(s, this.bx, this.by + this.bh - 2, this.bw, 2, G.shade(leather, 0.7F));
    int thread = "gold".equals(this.diary.trim) ? 0xFFD9AA2E : "silver".equals(this.diary.trim) ? 0xFFC2C9D4 : G.shade(leather, 1.5F);
    G.dashed(s, this.bx + 3, this.by + 3, this.bw - 6, this.bh - 6, thread);

    // Seitenstapel und die zwei Seiten
    float px = this.bx + 9, py = this.by + 8, pw = this.bw - 18, ph = this.bh - 18, half = pw / 2F, mid = px + half;
    G.rect(s, px - 2, py + 2, pw + 4, ph, 0xFFCDB880);
    G.rect(s, px - 1, py + 1, pw + 2, ph, 0xFFE6D7AB);
    G.rect(s, px, py, pw, ph, PAGE);
    for (int i = 0; i < 10; i++) {
      int sh = G.alpha(0xFF46300F, (10 - i) / 10F * 0.30F);
      G.rect(s, mid - 1 - i, py, 1, ph, sh);
      G.rect(s, mid + i, py, 1, ph, sh);
    }
    this.speckles(s, px, py, pw, ph);

    this.page(s, this.pg(this.view), this.view, px, py, half, ph, true);
    this.page(s, this.pg(this.view + 1), this.view + 1, mid, py, half, ph, false);

    // rotes Lesezeichen-Band in der Buchmitte
    float ry = this.by - 3, rh = this.bh + 12;
    G.rect(s, mid - 4, ry, 8, rh, 0xFF9E1C1C);
    G.rect(s, mid - 2, ry, 4, rh, 0xFFC92F2F);
    G.rect(s, mid - 1, ry, 1, rh, 0xFFE35050);
    G.rect(s, mid - 4, ry + rh, 3, 3, 0xFF9E1C1C);
    G.rect(s, mid + 1, ry + rh, 3, 3, 0xFF9E1C1C);
    G.rect(s, mid - 4, ry + rh + 3, 1, 2, 0xFF9E1C1C);
    G.rect(s, mid + 3, ry + rh + 3, 1, 2, 0xFF9E1C1C);

    // Pfeile wie im Minecraft-Buch
    if (this.view > 0) {
      this.arrow(s, px + 12, py + ph - 15, true, G.in(mx, my, px + 8, py + ph - 19, 28, 16));
    }
    if (this.view + 2 < this.pages.size() || this.canAddPage()) {
      this.arrow(s, px + pw - 30, py + ph - 15, false, G.in(mx, my, px + pw - 36, py + ph - 19, 28, 16));
    }

    if (this.shareOpen) {
      this.drawShare(s, mx, my);
    }
    this.drawFlash(s);
    if (this.dirtyAt > 0 && System.currentTimeMillis() - this.dirtyAt > 1000L) {
      this.dirtyAt = 0;
      this.diary.save();
    }
  }

  /** Geschlossenes Buch: der Ledereinband mit Name. Anklicken schlaegt es auf. */
  private void drawCover(Stack s, float mx, float my) {
    this.ch0 = Math.min(this.h - 70, 216);
    this.cw0 = this.ch0 * 112F / 144F;
    this.cx0 = this.x + (this.w - this.cw0) / 2F;
    this.cy0 = this.y + (this.h - this.ch0) / 2F + 2;
    boolean over = G.in(mx, my, this.cx0, this.cy0, this.cw0, this.ch0);
    float lift = over ? -3 : 0;
    G.rect(s, this.cx0 + 5, this.cy0 + 7, this.cw0, this.ch0, 0x70000000);
    G.icon(s, CoverArt.icon(this.diary.leatherColor(), this.diary.trim, this.ownerName), this.cx0, this.cy0 + lift, this.cw0, this.ch0);
    G.centered(s, L.t("diary.openHint"), this.x + this.w / 2F, this.cy0 + this.ch0 + 10, over ? 0xFFF2C94C : 0xFFDFE4EE, 0.9F, true);
    boolean wrote = this.diary.wroteToday();
    String state = this.readOnly ? L.t("diary.shared.readOnly") : wrote ? L.t("diary.todayDone") : L.t("diary.todayOpen");
    G.centered(s, state, this.x + this.w / 2F, this.cy0 + this.ch0 + 21, this.readOnly ? 0xFF9AA3B5 : wrote ? 0xFF8FE39A : 0xFFFF9A8A, 0.8F, true);
  }

  private void openBook() {
    this.cover = false;
    G.sound("item.book.page_turn", 0.9F);
  }

  private void speckles(Stack s, float px, float py, float pw, float ph) {
    for (int i = 0; i < 90; i++) {
      float fx = ((i * 7919) % 997) / 997F, fy = ((i * 6007) % 991) / 991F;
      G.rect(s, px + 3 + fx * (pw - 6), py + 3 + fy * (ph - 6), 1, 1, 0x22825F28);
    }
  }

  private void corners(Stack s, float X, float Y, float W, float H) {
    float[][] c = {{X + 5, Y + 5, 1, 1}, {X + W - 6, Y + 5, -1, 1}, {X + 5, Y + H - 6, 1, -1}, {X + W - 6, Y + H - 6, -1, -1}};
    for (float[] k : c) {
      G.rect(s, k[2] > 0 ? k[0] : k[0] - 9, k[1], 10, 1, ORN);
      G.rect(s, k[0], k[3] > 0 ? k[1] : k[1] - 9, 1, 10, ORN);
      G.rect(s, k[2] > 0 ? k[0] + 2 : k[0] - 7, k[1] + 2 * k[3], 6, 1, ORN);
      G.rect(s, k[0] + 2 * k[2], k[3] > 0 ? k[1] + 2 : k[1] - 7, 1, 6, ORN);
      G.rect(s, k[0] + 4 * k[2] - (k[2] < 0 ? 1 : 0), k[1] + 4 * k[3] - (k[3] < 0 ? 1 : 0), 2, 2, ORN);
    }
  }

  private void divider(Stack s, float cx, float y, float halfW) {
    G.rect(s, cx - halfW, y, halfW - 6, 1, 0xFFC9B27A);
    G.rect(s, cx + 6, y, halfW - 6, 1, 0xFFC9B27A);
    G.rect(s, cx - 1, y - 2, 2, 5, ORN);
    G.rect(s, cx - 2, y - 1, 4, 3, ORN);
    G.rect(s, cx - 3, y, 6, 1, ORN);
  }

  private void page(Stack s, Pg pg, int no, float X, float Y, float W, float H, boolean left) {
    this.corners(s, X, Y, W, H);
    float cx = X + W / 2F;
    if (pg == null) {
      this.divider(s, cx, Y + H / 2F, W * 0.3F);
      return;
    }
    String id = pg.id;
    if (Diary.PLATE.equals(id)) {
      this.plate(s, X, Y, W, H);
      return;
    }
    boolean isToday = this.today.equals(id), lastPart = pg.part == pg.parts - 1;
    // Datum und Uhrzeit klein in der aeusseren Ecke – muss man nicht selbst eintragen
    String day = Diary.weekday(id) + ", " + Diary.date(id);
    String time = this.diary.time(id);
    if (time.isEmpty() || (isToday && !this.diary.wroteToday())) {
      time = Diary.clock();
    }
    time = L.t("diary.time", time);
    if (pg.parts > 1) {
      time += "  ·  " + L.t("diary.part", pg.part + 1, pg.parts);
    }
    float sc = 0.8F;
    float dx = left ? X + 20 : X + W - 20 - G.width(day) * sc, tx = left ? X + 20 : X + W - 20 - G.width(time) * sc;
    G.text(s, day, dx, Y + 9, SEPIA_DARK, sc, false);
    G.text(s, time, tx, Y + 18, SEPIA, sc, false);
    this.divider(s, cx, Y + 31, W / 2F - 18);

    // Schreiblinien mit roter Randlinie
    float top = Y + 37;
    int lines = this.maxLines;
    for (int i = 0; i < lines; i++) {
      G.rect(s, X + 12, top + i * LH + LH - 1, W - 24, 1, 0x738C6E3C);
    }
    G.rect(s, X + 25, top, 1, lines * LH, 0x8CC85A46);
    float textX = X + 30;
    if (isToday && this.diary.text(id).isEmpty()) {
      G.text(s, L.t("diary.placeholder"), textX, top + 1, 0xFFA8946A, 1F, false);
    }
    for (int i = 0; i < pg.lines.size() && i < lines; i++) {
      G.text(s, pg.lines.get(i).text, textX, top + i * LH + 1, isToday ? INK : 0xFF3F3550, 1F, false);
    }
    if (isToday && (System.currentTimeMillis() / 500L) % 2 == 0) {
      int cl = this.caretLine();
      if (cl / this.maxLines == pg.part && cl < this.todayLines.size()) {
        Ln l = this.todayLines.get(cl);
        String all = this.diary.text(this.today);
        float cxp = tw(all, l.start, Math.min(this.caret, l.start + l.text.length()));
        G.rect(s, textX + cxp, top + (cl % this.maxLines) * LH, 1, 9, INK);
      }
    }
    // Seitenzahl, auf der letzten heutigen Seite zusaetzlich der Zeichenzaehler
    G.centered(s, "– " + no + " –", cx, Y + H - 15, SEPIA, 0.8F, false);
    if (isToday && lastPart) {
      String cnt = this.diary.text(id).length() + " / " + Diary.MAX_CHARS;
      G.text(s, cnt, left ? X + W - 20 - G.width(cnt) * 0.7F : X + 20, Y + H - 14, SEPIA, 0.7F, false);
    }
  }

  /** Namensblatt ganz vorn: "Dieses Tagebuch gehoert ...". */
  private void plate(Stack s, float X, float Y, float W, float H) {
    float cx = X + W / 2F, bw2 = Math.min(W - 40, 150), bh2 = 78, bx2 = cx - bw2 / 2F, by2 = Y + (H - bh2) / 2F;
    G.outline(s, bx2 - 3, by2 - 3, bw2 + 6, bh2 + 6, 1, 0xFFC9B27A);
    G.outline(s, bx2, by2, bw2, bh2, 1, ORN);
    G.centered(s, L.t("diary.owner"), cx, by2 + 12, SEPIA, 0.8F, false);
    String name = this.ownerName;
    float ns = Math.min(1.3F, (bw2 - 14) / Math.max(1F, G.width(name)));
    G.centered(s, name, cx, by2 + 27, SEPIA_DARK, ns, false);
    this.divider(s, cx, by2 + 47, bw2 / 2F - 22);
    List<String> p = this.diary.pages();
    String first = p.size() > 1 ? p.get(1) : this.today;
    G.centered(s, L.t("diary.since", Diary.date(first)), cx, by2 + bh2 - 16, SEPIA, 0.75F, false);
  }

  private void arrow(Stack s, float ax, float ay, boolean left, boolean hover) {
    int col = hover ? 0xFFC0502A : 0xFF8A3B1E;
    G.rect(s, left ? ax + 5 : ax, ay + 3, 13, 3, col);
    for (int i = 0; i < 5; i++) {
      G.rect(s, left ? ax + i : ax + 17 - i, ay + 4 - i, 1, 1 + 2 * i, col);
    }
  }

  /** Eine neue leere Seite fuer heute gibt es, wenn man auf der letzten Seite steht und dort schon etwas steht. */
  private boolean canAddPage() {
    if (this.readOnly || this.pages.isEmpty() || this.view + 2 < this.pages.size()) {
      return false;
    }
    Pg last = this.pages.get(this.pages.size() - 1);
    if (!this.isToday(last) || this.diary.text(this.today).length() >= Diary.MAX_CHARS) {
      return false;
    }
    for (Ln l : last.lines) {
      if (!l.text.trim().isEmpty()) {
        return true;
      }
    }
    return false;
  }

  private void flip(int dir) {
    int nv = this.view + dir * 2;
    if (dir > 0 && nv >= this.pages.size() && this.canAddPage()) {
      String t = this.diary.text(this.today);
      this.edit(t + BREAK_S, t.length() + 1); // neue Seite aufschlagen, Schreibmarke dorthin
      G.sound("item.book.page_turn", 1F);
      return;
    }
    if (nv < 0 || nv >= this.pages.size()) {
      return;
    }
    this.view = nv;
    G.sound("item.book.page_turn", 1F);
  }

  /** Text des heutigen Eintrags aendern; danach zeigt das Buch wieder die Seite mit dem Textende. */
  private void edit(String text, int newCaret) {
    this.diary.write(text);
    this.caret = Math.max(0, Math.min(text.length(), newCaret));
    this.dirtyAt = System.currentTimeMillis();
    this.jumpToEnd = true;
  }

  private void moveCaret(int pos) {
    this.caret = Math.max(0, Math.min(this.diary.text(this.today).length(), pos));
    this.jumpToEnd = true;
  }

  /** Schreibmarke eine Zeile hoch (-1) oder runter (+1), moeglichst an derselben Stelle. */
  private void moveLine(int dir) {
    int cl = this.caretLine(), nl = cl + dir;
    // leere Fuellzeilen am Ende einer vorzeitig beendeten Seite ueberspringen
    while (nl > 0 && nl < this.todayLines.size() - 1 && this.todayLines.get(nl).start == this.todayLines.get(nl).end) {
      nl += dir;
    }
    if (nl < 0 || nl >= this.todayLines.size()) {
      return;
    }
    Ln l = this.todayLines.get(cl);
    float xPos = tw(this.diary.text(this.today), l.start, Math.min(this.caret, l.start + l.text.length()));
    this.moveCaret(this.offsetAt(this.todayLines.get(nl), xPos));
  }

  private void say(String key) {
    this.flash = L.t(key);
    this.flashAt = System.currentTimeMillis();
  }

  private void copy() {
    Pg r = this.pg(this.view + 1), l = this.pg(this.view);
    String day = this.writable() ? this.today : r != null && !Diary.PLATE.equals(r.id) ? r.id : l == null ? null : l.id;
    String text = day == null || Diary.PLATE.equals(day) ? "" : this.diary.text(day);
    if (text.isEmpty()) {
      this.say("diary.copyEmpty");
    } else if (Clip.set(text.replace(BREAK_S, "\n\n"))) {
      this.say("diary.copied");
      G.click();
    }
  }

  private void paste() {
    if (!this.writable()) {
      this.say(this.readOnly ? "diary.shared.readOnly" : "diary.pasteToday");
      return;
    }
    String clip = Clip.get().replace("\r\n", "\n").replace('\r', '\n').replace('\t', ' ');
    String t = this.diary.text(this.today);
    this.caret = Math.max(0, Math.min(t.length(), this.caret));
    StringBuilder in = new StringBuilder();
    for (int i = 0; i < clip.length(); i++) {
      char c = clip.charAt(i);
      if (c != '\n' && c < 32) {
        continue;
      }
      if (t.length() + in.length() >= Diary.MAX_CHARS) {
        break; // Tageslimit erreicht: der Rest passt nicht mehr
      }
      in.append(c);
    }
    int added = in.length();
    if (added > 0) {
      this.edit(t.substring(0, this.caret) + in + t.substring(this.caret), this.caret + added);
      this.say(added < clip.length() ? "diary.pastedPart" : "diary.pasted");
      G.click();
    } else {
      this.say(clip.isEmpty() ? "diary.pasteEmpty" : "diary.pasteFull");
      G.sound("block.note_block.bass", 0.6F);
    }
  }

  private void finish() {
    if (this.done) {
      return;
    }
    this.done = true;
    if (this.readOnly) {
      return; // fremdes Tagebuch wird nie gespeichert
    }
    String t = this.diary.text(this.today);
    int end = t.length();
    while (end > 0 && t.charAt(end - 1) == BREAK) {
      end--; // angefangene, aber leer gebliebene Seiten nicht mitspeichern
    }
    if (end < t.length()) {
      this.diary.write(t.substring(0, end));
    }
    this.diary.save();
    if (this.quitAfter) {
      DiaryState.screens = Math.max(0, DiaryState.screens - 1);
      DiaryState.quitNow = true; // Eintrag ist gespeichert, jetzt darf das Spiel schliessen
    }
  }

  @Override
  protected boolean onClick(float mx, float my, boolean left) {
    if (this.headerHit(mx, my)) {
      this.finish();
      Screens.close();
      return true;
    }
    if (this.remote != null && !this.loaded) {
      return true;
    }
    if (this.shareOpen) {
      return this.clickShare(mx, my);
    }
    float[] sb = this.shareBtn();
    if (!this.readOnly && G.in(mx, my, sb[0], sb[1], sb[2], sb[3])) {
      G.click();
      this.shareOpen = true;
      return true;
    }
    if (this.cover) {
      if (G.in(mx, my, this.cx0, this.cy0, this.cw0, this.ch0)) {
        this.openBook();
      }
      return true;
    }
    float px = this.bx + 9, py = this.by + 8, pw = this.bw - 18, ph = this.bh - 18;
    if (G.in(mx, my, px + 8, py + ph - 19, 28, 16)) {
      this.flip(-1);
      return true;
    }
    if (G.in(mx, my, px + pw - 36, py + ph - 19, 28, 16)) {
      this.flip(1);
      return true;
    }
    // Klick in den heutigen Text: Schreibmarke genau dorthin setzen
    float half = pw / 2F, top = py + 37;
    for (int side = 0; side < 2; side++) {
      Pg pg = this.pg(this.view + side);
      float X = px + side * half;
      if (this.isToday(pg) && G.in(mx, my, X + 12, top - 2, half - 20, this.maxLines * LH + 4)) {
        int row = Math.max(0, Math.min(pg.lines.size() - 1, (int) ((my - top) / LH)));
        Ln lastLine = this.todayLines.isEmpty() ? null : this.todayLines.get(this.todayLines.size() - 1);
        while (row > 0 && pg.lines.get(row) != lastLine && pg.lines.get(row).start == pg.lines.get(row).end) {
          row--; // Klick in den leeren Rest der Seite: ans Ende des Geschriebenen
        }
        if (pg.lines.isEmpty()) {
          this.moveCaret(0);
        } else {
          this.moveCaret(this.offsetAt(pg.lines.get(row), mx - (X + 30)));
        }
        this.jumpToEnd = false;
        return true;
      }
    }
    return true;
  }

  @Override
  protected boolean onScroll(float mx, float my, float delta) {
    if (this.cover) {
      return true;
    }
    this.flip(delta < 0 ? 1 : -1);
    return true;
  }

  @Override
  protected boolean onKey(Key key) {
    if (key == Key.ESCAPE && this.shareOpen) {
      this.shareOpen = false;
      return true;
    }
    if (this.shareOpen || (this.remote != null && !this.loaded && key != Key.ESCAPE)) {
      return true;
    }
    if (key == Key.ESCAPE) {
      this.finish();
      Screens.close();
      return true;
    }
    if (this.cover) {
      if (key == Key.ENTER) {
        this.openBook();
      }
      return true;
    }
    // Strg+C kopiert den Eintrag der aufgeschlagenen Seite, Strg+V fuegt Text auf der heutigen Seite ein
    if (KeyHandler.isCopy(key)) {
      this.copy();
      return true;
    }
    if (KeyHandler.isPaste(key)) {
      this.paste();
      return true;
    }
    if (!this.writable()) {
      if (key == Key.ARROW_LEFT) {
        this.flip(-1);
        return true;
      }
      if (key == Key.ARROW_RIGHT) {
        this.flip(1);
        return true;
      }
      return false;
    }
    String t = this.diary.text(this.today);
    this.caret = Math.max(0, Math.min(t.length(), this.caret));
    if (key == Key.BACK) {
      if (this.caret > 0) {
        this.edit(t.substring(0, this.caret - 1) + t.substring(this.caret), this.caret - 1);
      }
      return true;
    }
    if (key == Key.ENTER) {
      if (t.length() < Diary.MAX_CHARS) {
        this.edit(t.substring(0, this.caret) + "\n" + t.substring(this.caret), this.caret + 1);
      }
      return true;
    }
    if (key == Key.ARROW_LEFT) {
      this.moveCaret(this.caret - 1);
      return true;
    }
    if (key == Key.ARROW_RIGHT) {
      this.moveCaret(this.caret + 1);
      return true;
    }
    if (key == Key.ARROW_UP) {
      this.moveLine(-1);
      return true;
    }
    if (key == Key.ARROW_DOWN) {
      this.moveLine(1);
      return true;
    }
    if (key == Key.DELETE) {
      if (this.caret < t.length()) {
        this.edit(t.substring(0, this.caret) + t.substring(this.caret + 1), this.caret);
      }
      return true;
    }
    if (key == Key.HOME || key == Key.END) {
      int cl = this.caretLine();
      if (cl < this.todayLines.size()) {
        Ln l = this.todayLines.get(cl);
        this.moveCaret(key == Key.HOME ? l.start : l.start + l.text.length());
      }
      return true;
    }
    return false;
  }

  @Override
  protected boolean onChar(char c) {
    if (this.shareOpen || this.cover || c < 32 || !this.writable()) {
      return false;
    }
    String t = this.diary.text(this.today);
    if (t.length() < Diary.MAX_CHARS) {
      this.caret = Math.max(0, Math.min(t.length(), this.caret));
      this.edit(t.substring(0, this.caret) + c + t.substring(this.caret), this.caret + 1);
    } else {
      G.sound("block.note_block.bass", 0.6F);
    }
    return true;
  }

  @Override
  public void onCloseScreen() {
    super.onCloseScreen();
    this.finish();
  }
}
