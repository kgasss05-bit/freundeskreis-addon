package de.ulgu.fkreis.core.mixtape;

import de.ulgu.fkreis.core.L;
import de.ulgu.fkreis.core.gfx.Backdrop;
import de.ulgu.fkreis.core.gfx.G;
import de.ulgu.fkreis.core.ui.FkActivity;
import de.ulgu.fkreis.core.ui.Screens;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.labymod.api.client.gui.screen.activity.AutoActivity;
import net.labymod.api.client.gui.screen.key.Key;
import net.labymod.api.client.render.matrix.Stack;

/**
 * Mixtapes: links die Holzkiste von der Seite (nur die Koepfe der Kassetten schauen heraus), rechts der Kassettenrekorder.
 * Kassette antippen hebt sie heraus, in den Rekorder ziehen spielt sie ab. Tippen sucht nach einem Song.
 */
@AutoActivity
public class MixtapeActivity extends FkActivity {

  private static final int PER = 20;

  private final Tapes box = Tapes.get();
  private final List<Tapes.Tape> view = new ArrayList<>();
  private final java.util.Map<Tapes.Tape, Float> lift = new java.util.HashMap<>();
  private boolean giftTab;
  private int page, pages = 1, found;
  private String query = "";
  private Tapes.Tape sel, drag;
  private boolean dragging;
  private float dragX, dragY, mxNow, myNow;
  private long insertedAt;
  private float s, ox, oy;

  public MixtapeActivity() {
    G.click();
  }

  private void layout() {
    this.s = Math.max(1F, Math.min(3F, Math.min((this.w - 30) / 320F, (this.h - 104) / 114F)));
    this.ox = this.x + (this.w - 320 * this.s) / 2F;
    this.oy = this.y + 66;
  }

  private void makeView() {
    List<Tapes.Tape> all = new ArrayList<>();
    String q = this.query.toLowerCase(Locale.ROOT);
    for (Tapes.Tape t : this.box.tapes) {
      if (q.isEmpty() ? t.gift() == this.giftTab : t.title.toLowerCase(Locale.ROOT).contains(q)) {
        all.add(t);
      }
    }
    this.found = all.size();
    this.pages = Math.max(1, (all.size() + PER - 1) / PER);
    this.page = Math.max(0, Math.min(this.page, this.pages - 1));
    this.view.clear();
    for (int i = this.page * PER; i < Math.min(all.size(), this.page * PER + PER); i++) {
      this.view.add(all.get(i));
    }
    if (this.sel != null && (!this.box.tapes.contains(this.sel) || this.sel == Player.current())) {
      this.sel = null;
    }
  }

  private float[] slot(int k) {
    int row = k / 10, col = k % 10;
    return new float[] {11 + col * 13 + row * 3, 46 - row * 8, row};
  }

  private float[] deck() {
    return new float[] {this.ox + 170 * this.s, this.oy, 150 * this.s, 112 * this.s};
  }

  private float[] btn(int i) { // drei Knoepfe unter der Kiste
    float bw = (150 * this.s - 8) / 3F;
    return new float[] {this.ox + i * (bw + 4), this.oy + 114 * this.s + 6, bw, 16};
  }

  private float[] trash() { // Muelleimer unten rechts in der Ecke, wie bei den Notizzetteln
    float t = 34;
    return new float[] {this.x + this.w - t - 14, this.y + this.h - t - 12, t, t};
  }

  private float[] tab(int i) { // 0 meine, 1 Freunde, 2 zurueck, 3 weiter
    float x0 = this.ox + 170 * this.s, tw = (150 * this.s - 60) / 2F;
    if (i < 2) {
      return new float[] {x0 + i * (tw + 3), this.y + 42, tw, 16};
    }
    return new float[] {x0 + 150 * this.s - (i == 2 ? 50 : 16), this.y + 42, 16, 16};
  }

  private void button(Stack st, float[] r, String label, float mx, float my, boolean gold, boolean enabled) {
    boolean over = enabled && G.in(mx, my, r[0], r[1], r[2], r[3]);
    int bg = !enabled ? 0xFF22252D : gold ? (over ? 0xFFFFDB6E : 0xFFF2C94C) : over ? 0xFF4A4F5C : 0xFF3A3E48;
    G.rect(st, r[0], r[1], r[2], r[3], bg);
    G.rect(st, r[0], r[1] + r[3] - 2, r[2], 2, gold && enabled ? 0xFF8A6A10 : 0xFF15171C);
    G.centered(st, G.fit(label, r[2] - 4, 0.85F), r[0] + r[2] / 2F, r[1] + 4, !enabled ? 0xFF6C7486 : gold ? 0xFF1A1400 : 0xFFFFFFFF, 0.85F, false);
  }

  @Override
  protected void draw(Stack st, float mx, float my) {
    this.layout();
    this.makeView();
    this.mxNow = mx;
    this.myNow = my;
    float s = this.s;
    Backdrop.draw(st, this.box.background, this.x, this.y, this.w, this.h);
    this.header(st, L.t("mixtape.title"), mx, my);

    // Suchfeld
    float sw = 150 * s;
    G.rect(st, this.ox, this.y + 42, sw, 16, 0xFF101319);
    G.outline(st, this.ox, this.y + 42, sw, 16, 1, 0xFF000000);
    boolean blink = (System.currentTimeMillis() / 500) % 2 == 0;
    G.text(st, this.query.isEmpty() ? L.t("mixtape.search") : this.query + (blink ? "_" : ""), this.ox + 5, this.y + 46,
        this.query.isEmpty() ? 0xFF6C7486 : 0xFFFFFFFF, 0.9F, false);
    // Reiter und Blaettern
    this.button(st, this.tab(0), L.t("mixtape.tabOwn", this.box.count(false), Tapes.MAX_OWN), mx, my, this.query.isEmpty() && !this.giftTab, true);
    this.button(st, this.tab(1), L.t("mixtape.tabGifts", this.box.count(true), Tapes.MAX_FRIEND), mx, my, this.query.isEmpty() && this.giftTab, true);
    this.button(st, this.tab(2), "<", mx, my, false, this.page > 0);
    this.button(st, this.tab(3), ">", mx, my, false, this.page < this.pages - 1);
    float[] t2 = this.tab(2);
    G.centered(st, (this.page + 1) + "/" + this.pages, t2[0] + 25, t2[1] + 4, 0xFFDFE4EE, 0.8F, false);

    // Kassetten: hinten zuerst, dann die Vorderseite der Kiste davor
    int hover = this.dragging ? -1 : this.hit(mx, my);
    for (int k = this.view.size() - 1; k >= 0; k--) {
      Tapes.Tape t = this.view.get(k);
      if (t == Player.current() || (this.dragging && t == this.drag)) {
        continue;
      }
      float[] p = this.slot(k);
      float target = t == this.sel ? 26 : k == hover ? 9 : 0;
      float l = G.lerp(this.lift.getOrDefault(t, 0F), target, 0.3F);
      this.lift.put(t, l);
      float tx = this.ox + p[0] * s, ty = this.oy + (p[1] - l) * s;
      G.icon(st, TapeArt.standing(t.color), tx, ty, 12 * s, 44 * s);
      if (t == this.sel) {
        G.rect(st, tx - s, ty - s, 14 * s, s, 0xFFF2C94C);
        G.rect(st, tx - s, ty, s, 30 * s, 0xFFF2C94C);
        G.rect(st, tx + 12 * s, ty, s, 30 * s, 0xFFF2C94C);
      }
    }
    G.icon(st, TapeArt.crate(), this.ox + 6 * s, this.oy + 58 * s, 140 * s, 54 * s);

    // Knoepfe unter der Kiste
    boolean ownSel = this.sel != null;
    this.button(st, this.btn(0), L.t("mixtape.new"), mx, my, true, true);
    this.button(st, this.btn(1), L.t("mixtape.edit"), mx, my, false, ownSel);
    this.button(st, this.btn(2), L.t("mixtape.give"), mx, my, false, ownSel && !this.sel.gift());

    // Rekorder
    float[] d = this.deck();
    Player.drawRecorder(st, d[0], d[1], s, Player.current(), this.insertedAt == 0 ? 0 : this.insertedAt, true);
    if (this.dragging && G.in(mx, my, d[0], d[1], d[2], d[3])) {
      G.outline(st, d[0] + 64 * s, d[1] + 49 * s, 70 * s, 46 * s, s, 0xFFF2C94C);
    }
    // Muelleimer: Kassette hineinziehen wirft sie weg
    float[] tr = this.trash();
    boolean openLid = this.dragging && this.over(mx, my, new float[] {tr[0] - 10, tr[1] - 10, tr[2] + 20, tr[3] + 20});
    G.icon(st, openLid ? G.TRASH_OPEN : G.TRASH, tr[0], tr[1], tr[2], tr[3]);

    if (this.dragging && this.drag != null) {
      G.icon(st, TapeArt.lying(this.drag, 0), mx - 23 * s, my - 14 * s, 46 * s, 28 * s);
    }
    // Geschenk-Anfrage eines Freundes
    Gifts.Offer offer = Gifts.next();
    if (offer != null) {
      float[] c = this.card();
      G.rect(st, this.x, this.y, this.w, this.h, 0xB0000000);
      G.rect(st, c[0], c[1], c[2], c[3], 0xFF1C1F28);
      G.outline(st, c[0], c[1], c[2], c[3], 1, 0xFF000000);
      G.rect(st, c[0], c[1], c[2], 2, 0xFFF2C94C);
      G.centered(st, G.fit(L.t("mixtape.offerTitle", offer.fromName), c[2] - 12, 1F), c[0] + c[2] / 2F, c[1] + 10, 0xFFF2C94C, 1F, true);
      G.icon(st, TapeArt.lying(offer.tape, 0), c[0] + c[2] / 2F - 46, c[1] + 26, 92, 56);
      G.centered(st, L.t("mixtape.offerText"), c[0] + c[2] / 2F, c[1] + 88, 0xFFDFE4EE, 0.85F, false);
      this.button(st, this.cardBtn(0), L.t("mixtape.decline"), mx, my, false, true);
      this.button(st, this.cardBtn(1), L.t("mixtape.accept"), mx, my, true, true);
    }
    // nach dem Einlegen: Klappe zu, Spulen drehen sich, dann schliesst sich das Menue
    if (this.insertedAt > 0 && System.currentTimeMillis() - this.insertedAt > 1600) {
      this.insertedAt = 0;
      Screens.close();
    }
  }

  private float[] card() {
    float cw = 230, ch = 132;
    return new float[] {this.x + (this.w - cw) / 2F, this.y + (this.h - ch) / 2F, cw, ch};
  }

  private float[] cardBtn(int i) {
    float[] c = this.card();
    return new float[] {c[0] + 10 + i * ((c[2] - 26) / 2F + 6), c[1] + c[3] - 26, (c[2] - 26) / 2F, 16};
  }

  /** Welche Kassette liegt unter der Maus? Vordere Reihen gewinnen. */
  private int hit(float mx, float my) {
    float lx = (mx - this.ox) / this.s, ly = (my - this.oy) / this.s;
    int best = -1;
    float bestRow = 9;
    for (int k = 0; k < this.view.size(); k++) {
      Tapes.Tape t = this.view.get(k);
      if (t == Player.current()) {
        continue;
      }
      float[] p = this.slot(k);
      float top = p[1] - this.lift.getOrDefault(t, 0F);
      if (lx >= p[0] - 1 && lx <= p[0] + 13 && ly >= top - 2 && ly < 58 && p[2] < bestRow) {
        best = k;
        bestRow = p[2];
      }
    }
    return best;
  }

  private boolean over(float mx, float my, float[] r) {
    return G.in(mx, my, r[0], r[1], r[2], r[3]);
  }

  private void insert(Tapes.Tape t) {
    this.sel = null;
    this.insertedAt = System.currentTimeMillis();
    G.sound("block.wooden_trapdoor.close", 0.8F);
    Player.play(t);
  }

  @Override
  protected boolean onClick(float mx, float my, boolean left) {
    this.layout();
    Gifts.Offer offer = Gifts.next();
    if (offer != null) {
      if (this.over(mx, my, this.cardBtn(1))) {
        if (this.box.count(true) >= Tapes.MAX_FRIEND) {
          de.ulgu.fkreis.core.net.Net.notify(L.t("mixtape.title"), L.t("mixtape.giftsFull", Tapes.MAX_FRIEND), null);
        } else {
          this.box.tapes.add(offer.tape);
          this.box.save();
          this.giftTab = true;
          this.query = "";
          this.page = 99;
          G.sound("entity.item.pickup", 1.2F);
        }
        Gifts.drop();
      } else if (this.over(mx, my, this.cardBtn(0))) {
        Gifts.drop();
        G.click();
      }
      return true;
    }
    if (this.headerHit(mx, my)) {
      Screens.close();
      return true;
    }
    for (int i = 0; i < 4; i++) {
      if (this.over(mx, my, this.tab(i))) {
        if (i < 2) {
          this.giftTab = i == 1;
          this.query = "";
          this.page = 0;
        } else {
          this.page += i == 2 ? -1 : 1;
        }
        this.sel = null;
        G.click();
        return true;
      }
    }
    int k = this.hit(mx, my);
    if (k >= 0) {
      Tapes.Tape t = this.view.get(k);
      this.sel = this.sel == t ? null : t;
      this.drag = t;
      this.dragging = false;
      this.dragX = mx;
      this.dragY = my;
      G.click();
      return true;
    }
    float[] d = this.deck();
    if (this.over(mx, my, d)) {
      float lx = (mx - d[0]) / this.s, ly = (my - d[1]) / this.s;
      if (ly >= 95 && ly <= 110 && lx >= 98 && lx <= 126) { // Auswerfen
        if (Player.current() != null) {
          Player.eject();
          this.insertedAt = 0;
          G.sound("block.wooden_trapdoor.open", 0.9F);
        }
      } else if (this.sel != null) {
        this.insert(this.sel);
      } else if (ly >= 95 && ly <= 110 && lx >= 62 && lx <= 90 && Player.current() != null) { // Abspielen: Song noch einmal starten
        Player.play(Player.current());
      }
      return true;
    }
    if (this.over(mx, my, this.btn(0))) {
      if (this.box.count(false) >= Tapes.MAX_OWN) {
        de.ulgu.fkreis.core.net.Net.notify(L.t("mixtape.title"), L.t("mixtape.ownFull", Tapes.MAX_OWN), null);
      } else {
        Screens.now(new TapeEditActivity(null));
      }
      return true;
    }
    if (this.sel != null && this.over(mx, my, this.btn(1))) {
      Screens.now(new TapeEditActivity(this.sel));
      return true;
    }
    if (this.sel != null && !this.sel.gift() && this.over(mx, my, this.btn(2))) {
      Screens.now(new FriendPickActivity(this.sel));
      return true;
    }
    this.sel = null;
    return true;
  }

  @Override
  protected boolean onDrag(float mx, float my) {
    if (this.drag != null && !this.dragging && Math.abs(mx - this.dragX) + Math.abs(my - this.dragY) > 5) {
      this.dragging = true;
      this.sel = this.drag;
    }
    return this.dragging;
  }

  @Override
  protected boolean onRelease(float mx, float my) {
    boolean was = this.dragging;
    Tapes.Tape t = this.drag;
    this.dragging = false;
    this.drag = null;
    if (was && t != null && this.over(mx, my, this.deck())) {
      this.insert(t);
    } else if (was && t != null && this.over(mx, my, new float[] {this.trash()[0] - 10, this.trash()[1] - 10, this.trash()[2] + 20, this.trash()[3] + 20})) {
      this.box.tapes.remove(t);
      this.box.save();
      this.sel = null;
      G.sound("block.composter.fill", 1F);
    }
    return was;
  }

  @Override
  protected boolean onScroll(float mx, float my, float delta) {
    this.page += delta < 0 ? 1 : -1;
    return true;
  }

  @Override
  protected boolean onKey(Key key) {
    if (key == Key.ESCAPE) {
      Screens.close();
      return true;
    }
    if (key == Key.BACK && !this.query.isEmpty()) {
      this.query = this.query.substring(0, this.query.length() - 1);
      this.page = 0;
      return true;
    }
    return false;
  }

  @Override
  protected boolean onChar(char c) {
    if (c < 32 || this.query.length() >= Tapes.MAX_TITLE || Gifts.next() != null) {
      return false;
    }
    this.query += c;
    this.page = 0;
    this.sel = null;
    return true;
  }
}
