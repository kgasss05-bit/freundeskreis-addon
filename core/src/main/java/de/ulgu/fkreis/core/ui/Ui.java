package de.ulgu.fkreis.core.ui;

import de.ulgu.fkreis.core.FkreisAddon;
import de.ulgu.fkreis.core.L;
import de.ulgu.fkreis.core.gfx.G;
import de.ulgu.fkreis.core.net.Net;
import java.util.UUID;

/** Kleine gemeinsame Aktionen fuer das Freundeskreis-Rad. */
public final class Ui {

  private Ui() {
  }

  public static void giveSticker(UUID uuid, String name) {
    Net.GiveResult r = FkreisAddon.get().net().giveSticker(uuid, name);
    switch (r) {
      case OK:
        Net.notify(L.t("sticker.give.title"), L.t("sticker.give.ok", name), G.head(uuid));
        G.sound("entity.item.pickup", 1.4F);
        break;
      case ALREADY:
        Net.notify(L.t("sticker.give.title"), L.t("sticker.give.already", name), G.head(uuid));
        break;
      case NOT_FRIEND:
        Net.notify(L.t("sticker.give.title"), L.t("sticker.give.notfriend", name), G.head(uuid));
        break;
      default:
        Net.notify(L.t("sticker.give.title"), L.t("sticker.give.failed"), null);
        break;
    }
  }

  /** Knopf "HUD-Widget einstellen": zeigt, wo LabyMods Widget-Editor zu finden ist. */
  public static void openHudEditor() {
    Net.notify(L.t("hud.title"), L.t("hud.where"), G.tex("ui/cake.png"));
  }
}
