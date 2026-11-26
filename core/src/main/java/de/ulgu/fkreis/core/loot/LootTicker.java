package de.ulgu.fkreis.core.loot;

import de.ulgu.fkreis.core.FkreisAddon;
import de.ulgu.fkreis.core.L;
import de.ulgu.fkreis.core.gfx.G;
import de.ulgu.fkreis.core.net.Net;
import net.labymod.api.Laby;
import net.labymod.api.event.Subscribe;
import net.labymod.api.event.client.lifecycle.GameTickEvent;
import net.labymod.api.event.client.network.server.ServerDisconnectEvent;
import net.labymod.api.event.client.network.server.ServerJoinEvent;

/**
 * Erinnert einmal am Tag kurz nach dem Betreten eines Servers mit einer Benachrichtigung daran,
 * dass das Gluecksrad noch gedreht werden kann. Es oeffnet sich nichts von selbst.
 */
public class LootTicker {

  private final FkreisAddon addon;
  private long remindAt;
  private String remindedDay = "";

  public LootTicker(FkreisAddon addon) {
    this.addon = addon;
  }

  @Subscribe
  public void onJoin(ServerJoinEvent event) {
    this.remindAt = System.currentTimeMillis() + 8000L; // erst kurz ankommen lassen
  }

  @Subscribe
  public void onDisconnect(ServerDisconnectEvent event) {
    this.remindAt = 0;
  }

  @Subscribe
  public void onTick(GameTickEvent event) {
    if (this.remindAt == 0 || System.currentTimeMillis() < this.remindAt) {
      return;
    }
    this.remindAt = 0;
    String today = java.time.LocalDate.now().toString();
    if (!this.addon.on() || !this.addon.configuration().loot().daily().get() || !Loot.get().canOpen()
        || today.equals(this.remindedDay) || !Laby.labyAPI().minecraft().isIngame()) {
      return;
    }
    this.remindedDay = today; // am selben Tag nicht bei jedem Serverwechsel wieder melden
    Net.notify(L.t("loot.title"), L.t("loot.ready"), null);
    G.sound("block.note_block.chime", 1.4F);
  }
}
