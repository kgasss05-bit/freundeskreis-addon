package de.ulgu.fkreis.core.ui;

import de.ulgu.fkreis.core.FkreisAddon;
import de.ulgu.fkreis.core.L;
import de.ulgu.fkreis.core.gfx.G;
import de.ulgu.fkreis.core.net.Net;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.labymod.api.client.gui.screen.activity.AutoActivity;
import net.labymod.api.client.gui.screen.key.Key;
import net.labymod.api.client.render.matrix.Stack;

/**
 * Freundeskreis-Rad: Kopf + Name in der Mitte, alle Freundeskreis-Funktionen im Kreis drumherum
 * (gleiche Optik wie LabyMods Mausrad: dunkle Pillen mit rundem Punkt). Eintraege fliegen beim Oeffnen
 * aus der Mitte heraus, Hover hebt sie hervor. Klick = ausfuehren, ESC/Klick daneben = schliessen.
 */
@AutoActivity
public class RingActivity extends FkActivity {

  private static final class Entry {
    final String label;
    final Runnable action;
    float hover;

    Entry(String label, Runnable action) {
      this.label = label;
      this.action = action;
    }
  }

  private final UUID uuid;
  private final String name;
  private final List<Entry> entries = new ArrayList<>();

  public RingActivity(UUID uuid, String name) {
    this.uuid = uuid;
    this.name = name;
    FkreisAddon addon = FkreisAddon.get();
    if (addon.configuration().profile().inWheel().get()) {
      this.entries.add(new Entry(L.t("wheel.profile"), () -> Screens.now(new ProfileScrollActivity(uuid, name))));
    }
    if (addon.configuration().stickerBook().inWheel().get()) {
      if (addon.store().given.contains(uuid.toString())) {
        // Sticker ist schon verschenkt: stattdessen das Sammelbuch dieser Person ansehen
        this.entries.add(new Entry(L.t("wheel.stickerBook"), () -> Screens.now(new StickerBookActivity(uuid, name))));
      } else {
        this.entries.add(new Entry(L.t("wheel.sticker"), () -> {
          this.displayPreviousScreen();
          Ui.giveSticker(uuid, name);
        }));
      }
    }
    if (addon.configuration().notebook().inWheel().get()) {
      // Erst das Regal (die Person steht dann schon drin), von dort ins Notizbuch
      this.entries.add(new Entry(L.t("wheel.notes"), () -> {
        FkreisAddon.get().store().touch(uuid.toString(), name);
        Screens.now(new NotebookActivity(uuid.toString()));
      }));
    }
    if (addon.configuration().album().inWheel().get()) {
      this.entries.add(new Entry(L.t("wheel.album"), () -> Screens.now(new AlbumActivity(uuid, name))));
    }
    if (addon.configuration().match().inWheel().get() && !uuid.equals(Net.self())) {
      this.entries.add(new Entry(L.t("wheel.match"), () -> Screens.now(new de.ulgu.fkreis.core.match.MatchCompareActivity(uuid, name))));
    }
    if (addon.configuration().mixtape().inWheel().get() && !uuid.equals(Net.self())) {
      this.entries.add(new Entry(L.t("wheel.mixtape"), () -> Screens.now(new de.ulgu.fkreis.core.mixtape.TapeGiveActivity(uuid, name))));
    }
    de.ulgu.fkreis.core.vanish.Vanish vanish = de.ulgu.fkreis.core.vanish.Vanish.get();
    if (vanish != null && vanish.on() && addon.configuration().vanish().inWheel().get() && !uuid.equals(Net.self())) {
      boolean hidden = vanish.isHidden(uuid);
      this.entries.add(new Entry(L.t(hidden ? "wheel.vanishShow" : "wheel.vanishHide"), () -> {
        this.displayPreviousScreen();
        vanish.toggle(uuid, name);
      }));
    }
    G.click();
  }

  private float[] pos(int i, float open) {
    int n = this.entries.size();
    float cx = this.x + this.w / 2F, cy = this.y + this.h / 2F;
    double a = -Math.PI / 2 + (Math.PI * 2 * i) / Math.max(1, n);
    float r = 82F * open;
    return new float[] {cx + (float) Math.cos(a) * r, cy + (float) Math.sin(a) * r};
  }

  private float pillW(Entry e) {
    return G.width(e.label) * 1.05F + 30;
  }

  @Override
  protected void draw(Stack s, float mx, float my) {
    float open = G.easeBack(this.age() / 0.28F);
    G.rect(s, this.x, this.y, this.w, this.h, G.alpha(0x88000000, Math.min(1F, this.age() / 0.2F)));

    float cx = this.x + this.w / 2F, cy = this.y + this.h / 2F;
    // Mitte: Kopf + Name
    G.rect(s, cx - 22, cy - 26, 44, 44, 0xB0151821);
    G.icon(s, G.head(this.uuid), cx - 18, cy - 22, 36, 36);
    G.centered(s, this.name, cx, cy + 22, 0xFFFFFFFF, 1.1F, true);
    G.centered(s, L.t("wheel.title"), cx, cy - 40, 0xFFF2C94C, 1F, true);

    if (this.entries.isEmpty()) {
      G.centered(s, L.t("wheel.empty"), cx, cy + 40, 0xFFDDDDDD, 1F, true);
    }
    for (int i = 0; i < this.entries.size(); i++) {
      Entry e = this.entries.get(i);
      float[] p = this.pos(i, open);
      float pw = this.pillW(e), ph = 18;
      boolean right = p[0] >= cx - 1;
      float px = right ? p[0] - 9 : p[0] - pw + 9;
      float py = p[1] - ph / 2F;
      boolean over = G.in(mx, my, px, py, pw, ph);
      e.hover = G.lerp(e.hover, over ? 1F : 0F, 0.3F);
      int bg = G.mix(0xE01C1F26, 0xF0384052, e.hover);
      // Pille mit "abgerundeten" Ecken
      G.rect(s, px + 2, py, pw - 4, ph, bg);
      G.rect(s, px, py + 2, pw, ph - 4, bg);
      G.rect(s, px + 1, py + 1, pw - 2, ph - 2, bg);
      G.rect(s, px + 3, py + ph - 1, pw - 6, 1, 0x60000000);
      // runder Punkt auf der Kreis-Seite
      float dx = right ? px - 6 : px + pw - 12;
      G.rect(s, dx + 2, py + 1, 14, 16, 0xFF15171C);
      G.rect(s, dx, py + 3, 18, 12, 0xFF15171C);
      if (e.hover > 0.05F) {
        G.rect(s, dx + 5, py + 5, 8, 8, G.alpha(0xFFF2C94C, e.hover));
      }
      float tx = right ? px + 16 : px + 8;
      G.text(s, e.label, tx, py + 5, 0xFFFFFFFF, 1.05F, true);
    }
  }

  @Override
  protected boolean onClick(float mx, float my, boolean left) {
    float cx = this.x + this.w / 2F;
    for (int i = 0; i < this.entries.size(); i++) {
      Entry e = this.entries.get(i);
      float[] p = this.pos(i, 1F);
      float pw = this.pillW(e), ph = 18;
      boolean right = p[0] >= cx - 1;
      float px = right ? p[0] - 9 : p[0] - pw + 9;
      if (G.in(mx, my, px - 8, p[1] - ph / 2F, pw + 8, ph)) {
        G.click();
        e.action.run();
        return true;
      }
    }
    this.displayPreviousScreen();
    return true;
  }

  @Override
  protected boolean onKey(Key key) {
    if (key == Key.ESCAPE) {
      this.displayPreviousScreen();
      return true;
    }
    return false;
  }
}
