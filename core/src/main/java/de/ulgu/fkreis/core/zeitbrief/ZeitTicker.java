package de.ulgu.fkreis.core.zeitbrief;

import de.ulgu.fkreis.core.FkreisAddon;
import de.ulgu.fkreis.core.L;
import de.ulgu.fkreis.core.gfx.G;
import de.ulgu.fkreis.core.net.Net;
import net.labymod.api.Laby;
import net.labymod.api.event.Subscribe;
import net.labymod.api.event.client.lifecycle.GameTickEvent;
import net.labymod.api.event.client.network.server.ServerJoinEvent;

/** Meldet kurz nach dem Betreten eines Servers, wenn ein Zeitbrief angekommen ist. */
public class ZeitTicker {

  private final FkreisAddon addon;
  private long checkAt;
  private String toldFor = "";

  public ZeitTicker(FkreisAddon addon) {
    this.addon = addon;
  }

  @Subscribe
  public void onJoin(ServerJoinEvent event) {
    this.checkAt = System.currentTimeMillis() + 9000L;
  }

  @Subscribe
  public void onTick(GameTickEvent event) {
    if (this.checkAt == 0 || System.currentTimeMillis() < this.checkAt) {
      return;
    }
    this.checkAt = 0;
    if (!this.addon.on() || !this.addon.configuration().timeLetter().notifyArrival().get() || !Laby.labyAPI().minecraft().isIngame()) {
      return;
    }
    Letters.Letter l = Letters.get().arrived();
    if (l != null && !this.toldFor.equals(l.written + l.due)) {
      this.toldFor = l.written + l.due; // pro Sitzung nur einmal je Brief melden
      Net.notify(L.t("letter.title"), L.t("letter.arrivedNote"), null);
      G.sound("entity.experience_orb.pickup", 0.8F);
    }
  }
}
