package de.ulgu.fkreis.core.ui;

import de.ulgu.fkreis.core.FkreisAddon;
import de.ulgu.fkreis.core.L;
import de.ulgu.fkreis.core.data.Mob;
import de.ulgu.fkreis.core.data.Mobs;
import de.ulgu.fkreis.core.data.ProfileData;
import de.ulgu.fkreis.core.gfx.G;
import de.ulgu.fkreis.core.net.Net;
import java.util.UUID;
import net.labymod.api.client.gui.screen.key.Key;
import net.labymod.api.client.gui.screen.activity.AutoActivity;
import net.labymod.api.client.render.matrix.Stack;

/**
 * Steckbrief als Holzrahmen-Karte (Buchgroesse, mittig). Holzart in den Einstellungen waehlbar.
 * Alles untereinander: Kopf, Name, Plaketten, dann Zeilen (Alter, Geburtstag, Land, Bundesland, Freundschaft, Mob).
 * Laedt der Steckbrief nicht, steht nach 6 Sekunden "Kein Steckbrief geteilt" statt ewig "wird geladen".
 */
@AutoActivity
public class ProfileScrollActivity extends FkActivity implements Net.ProfileListener {

  private final UUID uuid;
  private final String playerName;
  private Net.Result result = Net.Result.LOADING;
  private ProfileData data;
  private long closingAt = -1;

  public ProfileScrollActivity(UUID uuid, String playerName) {
    this.uuid = uuid;
    this.playerName = playerName;
    Net net = FkreisAddon.get().net();
    net.addListener(this);
    net.requestProfile(uuid);
    G.sound("item.book.page_turn", 0.8F);
  }

  @Override
  public void onProfile(UUID uuid, Net.Result result, ProfileData data) {
    if (!uuid.equals(this.uuid)) {
      return;
    }
    this.result = result;
    if (data != null) {
      this.data = data;
    }
  }

  @Override
  public void onCloseScreen() {
    super.onCloseScreen();
    FkreisAddon.get().net().removeListener(this);
  }

  // ------------------------------------------------------------------ Holzrahmen-Karte

  private static final float CW = 176;      // Kartenbreite (ca. Buchgroesse)
  private static final float FRAME = 8;     // Holzrahmen
  private static final float PAD = 10;
  private static final float ROW = 17;
  private static final float ROW_GAP = 3;
  private static final int PAPER = 0xFFF3EAD6;
  private static final int ROW_BG = 0xFFE6D8B8;
  private static final int INK = 0xFF2A1D0C;
  private static final int INK_MID = 0xFF7A6040;

  @Override
  protected void draw(Stack s, float mx, float my) {
    float t = this.age();
    float open = G.easeOut(Math.min(1F, t / 0.3F));
    if (this.closingAt > 0) {
      float c = (System.currentTimeMillis() - this.closingAt) / 200F;
      open = 1F - G.easeOut(Math.min(1F, c));
      if (c >= 1F) {
        this.displayPreviousScreen();
        return;
      }
    }
    G.rect(s, this.x, this.y, this.w, this.h, G.alpha(0xB0000000, open));

    de.ulgu.fkreis.core.data.WoodType wood =
        de.ulgu.fkreis.core.data.WoodType.parse(FkreisAddon.get().configuration().profile().wood().get());
    float cw = Math.min(CW, this.w - 40);
    float ch = Math.min(this.cardHeight(), this.h - 40);
    float cx = this.x + (this.w - cw) / 2F;
    float cy = this.y + (this.h - ch) / 2F - 6 + (1F - open) * 10F;

    // Schatten + Holzrahmen
    G.rect(s, cx + 3, cy + 4, cw, ch, G.alpha(0x60000000, open));
    G.planks(s, cx, cy, cw, ch, G.alpha(wood.plank, open), 7);
    int edge = G.alpha(G.shade(wood.plank, 0.45F), open);
    G.outline(s, cx, cy, cw, ch, 1, edge);
    G.rect(s, cx + 1, cy + 1, cw - 2, 1, G.alpha(G.shade(wood.plank, 1.2F), open));
    // Innenkarte
    float ix = cx + FRAME, iy = cy + FRAME, iw = cw - 2 * FRAME, ih = ch - 2 * FRAME;
    G.rect(s, ix - 1, iy - 1, iw + 2, ih + 2, edge);
    G.rect(s, ix, iy, iw, ih, G.alpha(PAPER, open));
    // Naegel in den Ecken
    float n = FRAME / 2F - 1;
    for (float[] p : new float[][] {{cx + n, cy + n}, {cx + cw - n - 2, cy + n}, {cx + n, cy + ch - n - 2}, {cx + cw - n - 2, cy + ch - n - 2}}) {
      G.rect(s, p[0], p[1], 2, 2, G.alpha(0xFFB4B4B4, open));
      G.rect(s, p[0] + 1, p[1] + 1, 1, 1, G.alpha(0xFF5A5A5A, open));
    }

    float a = Math.max(0F, Math.min(1F, (open - 0.4F) / 0.6F));
    if (a > 0.02F) {
      this.content(s, ix, iy, iw, ih, a, edge);
    }
    G.centered(s, L.t("profile.close"), this.x + this.w / 2F, cy + ch + 8, G.alpha(0xFFFFFFFF, a), 0.85F, true);
  }

  private int rows() {
    ProfileData d = this.data;
    if (d == null) {
      return 2;
    }
    long since = FkreisAddon.get().net().friendSince(this.uuid);
    boolean friend = since > 0 && !this.uuid.equals(Net.self());
    return 5 + (friend ? 1 : 0);
  }

  private float cardHeight() {
    float head = 40 + 6 + 13 + 11 + 16;
    return 2 * FRAME + PAD + head + this.rows() * (ROW + ROW_GAP) + PAD;
  }

  private void content(Stack s, float x, float y, float w, float h, float a, int edge) {
    int ink = G.alpha(INK, a), mid = G.alpha(INK_MID, a);
    float mid0 = x + w / 2F;
    float left = x + PAD, inner = w - 2 * PAD;
    float ty = y + PAD;

    // Kopf mittig mit Holzrand + Muenze
    float hs = 40;
    G.rect(s, mid0 - hs / 2F - 2, ty - 2, hs + 4, hs + 4, edge);
    G.icon(s, G.head(this.uuid), mid0 - hs / 2F, ty, hs, hs);
    long since = FkreisAddon.get().net().friendSince(this.uuid);
    boolean self = this.uuid.equals(Net.self());
    boolean friend = since > 0 && !self;
    int coin = friend ? de.ulgu.fkreis.core.coins.Coins.indexFor(since) : -1;
    if (friend) {
      G.icon(s, de.ulgu.fkreis.core.coins.Coins.icon(coin), mid0 + hs / 2F - 9, ty + hs - 9, 16, 16);
    }
    ty += hs + 6;
    G.centered(s, G.fit(this.playerName, inner, 1.2F), mid0, ty, ink, 1.2F, false);
    ty += 13;
    ProfileData d = this.data;
    if (d != null && d.name != null && !d.name.isEmpty()) {
      G.centered(s, G.fit("„" + d.name + "“", inner, 0.9F), mid0, ty, mid, 0.9F, false);
    }
    ty += 11;

    // Plaketten mittig
    java.util.List<String> chips = new java.util.ArrayList<>();
    java.util.List<int[]> cols = new java.util.ArrayList<>();
    if (self) {
      chips.add(L.t("profile.chip.self"));
      cols.add(new int[] {0xFF5A8AD0, 0xFFFFFFFF});
    } else if (friend) {
      chips.add(L.t("profile.chip.friend", sinceText(since)));
      cols.add(new int[] {0xFFE9B115, 0xFF2A2218});
    }
    de.ulgu.fkreis.core.data.Store store = FkreisAddon.get().store();
    if (!self && store.given.contains(this.uuid.toString()) && store.hasStickerFrom(this.uuid.toString())) {
      chips.add(L.t("profile.chip.sticker"));
      cols.add(new int[] {0xFF5AA84A, 0xFFFFFFFF});
    }
    float total = 0;
    for (String c : chips) {
      total += G.width(c) * 0.7F + 10 + 4;
    }
    float chx = mid0 - (total - 4) / 2F;
    for (int i = 0; i < chips.size(); i++) {
      chx = this.chip(s, chips.get(i), chx, ty, cols.get(i)[0], cols.get(i)[1], a);
    }
    ty += 16;

    if (this.result == Net.Result.LOADING && d == null) {
      int dots = (int) (this.age() * 3) % 4;
      G.centered(s, L.t("profile.loading") + " " + ".".repeat(dots), mid0, ty + 8, mid, 0.9F, false);
      return;
    }
    if (d == null) {
      String msg = this.result == Net.Result.NOT_FRIEND ? L.t("profile.notfriend") : L.t("profile.none");
      for (String line : G.wrap(msg, inner, 0.85F)) {
        G.centered(s, line, mid0, ty, ink, 0.85F, false);
        ty += 10;
      }
      return;
    }

    // Zeilen untereinander
    String bday = ProfileData.normalizeBirthday(d.birthday);
    String bsub = null;
    int[] dm = de.ulgu.fkreis.core.data.Birthdays.parse(bday);
    if (dm != null) {
      long days = de.ulgu.fkreis.core.data.Birthdays.daysUntil(dm[0], dm[1]);
      bsub = days == 0 ? L.t("cal.today") : days == 1 ? L.t("cal.tomorrow") : L.t("cal.indays", days);
    }
    ty = this.row(s, left, ty, inner, L.t("profile.age"), d.age, null, null, 1F, a);
    ty = this.row(s, left, ty, inner, L.t("profile.birthday"), bday, bsub, null, 1F, a);
    ty = this.row(s, left, ty, inner, L.t("profile.country"), d.country, null, null, 1F, a);
    ty = this.row(s, left, ty, inner, L.t("profile.state"), d.state, null, null, 1F, a);
    if (friend) {
      ty = this.row(s, left, ty, inner, L.t("profile.friendship"), de.ulgu.fkreis.core.coins.Coins.name(coin), null,
          de.ulgu.fkreis.core.coins.Coins.icon(coin), 1F, a);
    }
    Mob mob = Mobs.byId(d.mob);
    net.labymod.api.client.gui.icon.Icon face = null;
    float ratio = 1F;
    if (mob != Mobs.NONE) {
      face = MobFaces.face(mob.id());
      if (face != null) {
        ratio = MobFaces.ratio(mob.id());
      } else {
        face = mob.icon();
      }
    }
    this.row(s, left, ty, inner, L.t("profile.mob"), mob != Mobs.NONE ? mob.name() : null, null, face, ratio, a);

    if (d.birthdayToday()) {
      float pulse = 0.75F + 0.25F * (float) Math.sin(this.age() * 5);
      G.centered(s, L.t("profile.bdaytoday"), mid0, y + h - 9, G.alpha(G.mix(0xFFB8860B, 0xFFC0392B, pulse), a), 0.75F, false);
    }
  }

  /** Eine Zeile: Label links, Wert rechts (optional kleines Icon davor und Zusatz in Gold). */
  private float row(Stack s, float x, float y, float w, String label, String value, String sub,
      net.labymod.api.client.gui.icon.Icon icon, float ratio, float a) {
    G.rect(s, x, y, w, ROW, G.alpha(ROW_BG, a));
    G.rect(s, x, y + ROW - 1, w, 1, G.alpha(0xFFD3C29C, a));
    G.text(s, label, x + 5, y + 5.5F, G.alpha(INK_MID, a), 0.75F);
    String v = value == null || value.isEmpty() ? "–" : value;
    float sc = 0.9F;
    float right = x + w - 5;
    if (sub != null) {
      float sw = G.width(sub) * 0.7F;
      G.text(s, sub, right - sw, y + 6, G.alpha(0xFFB07D1C, a), 0.7F);
      right -= sw + 5;
    }
    float labelW = G.width(label) * 0.75F + 12;
    String fv = G.fit(v, right - (x + labelW) - (icon != null ? 12 : 0), sc);
    float vw = G.width(fv) * sc;
    G.text(s, fv, right - vw, y + 5, G.alpha(INK, a), sc);
    if (icon != null) {
      float is = 10;
      float ih = Math.min(13, is * ratio);
      G.icon(s, icon, right - vw - is - 3, y + (ROW - ih) / 2F, is, ih);
    }
    return y + ROW + ROW_GAP;
  }

  private float chip(Stack s, String text, float x, float y, int bg, int fg, float a) {
    float w = G.width(text) * 0.7F + 10;
    G.rect(s, x, y, w, 11, G.alpha(bg, a));
    G.rect(s, x, y + 10, w, 1, G.alpha(G.shade(bg, 0.75F), a));
    G.text(s, text, x + 5, y + 2.5F, G.alpha(fg, a), 0.7F);
    return x + w + 4;
  }

  /** "3 Monaten" / "1 Jahr" / "2 Jahren" / "heute" */
  static String sinceText(long since) {
    long days = Math.max(0, (System.currentTimeMillis() - since) / 86_400_000L);
    if (days < 31) {
      return days <= 1 ? L.t("profile.today") : L.t("profile.days", days);
    }
    long months = days / 30;
    if (months < 12) {
      return months == 1 ? L.t("profile.month") : L.t("profile.months", months);
    }
    long years = months / 12;
    return years == 1 ? L.t("profile.year") : L.t("profile.years", years);
  }

  private void close() {
    if (this.closingAt < 0) {
      this.closingAt = System.currentTimeMillis();
      G.sound("item.book.page_turn", 1.1F);
    }
  }

  @Override
  protected boolean onClick(float mx, float my, boolean left) {
    this.close();
    return true;
  }

  @Override
  protected boolean onKey(Key key) {
    if (key == Key.ESCAPE) {
      this.close();
      return true;
    }
    return false;
  }
}
