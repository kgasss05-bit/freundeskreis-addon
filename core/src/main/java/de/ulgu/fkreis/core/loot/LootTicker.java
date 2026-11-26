package de.ulgu.fkreis.core.loot;

import de.ulgu.fkreis.core.FkreisAddon;
import de.ulgu.fkreis.core.ui.Screens;
import net.labymod.api.Laby;
import net.labymod.api.event.Subscribe;
import net.labymod.api.event.client.lifecycle.GameTickEvent;
import net.labymod.api.event.client.network.server.ServerDisconnectEvent;
import net.labymod.api.event.client.network.server.ServerJoinEvent;

/** Oeffnet die taegliche Truhe kurz nach dem Betreten eines Servers, wenn sie heute noch nicht geoeffnet wurde. */
public class LootTicker {

  private final FkreisAddon addon;
  private long openAt;

  public LootTicker(FkreisAddon addon) {
    this.addon = addon;
  }

  @Subscribe
  public void onJoin(ServerJoinEvent event) {
    this.openAt = System.currentTimeMillis() + 5000L; // erst kurz ankommen lassen
  }

  @Subscribe
  public void onDisconnect(ServerDisconnectEvent event) {
    this.openAt = 0;
  }

  @Subscribe
  public void onTick(GameTickEvent event) {
    if (this.openAt == 0 || System.currentTimeMillis() < this.openAt) {
      return;
    }
    if (!this.addon.on() || !this.addon.configuration().loot().daily().get() || !Loot.get().canOpen()) {
      this.openAt = 0;
      return;
    }
    if (!Laby.labyAPI().minecraft().isIngame()) {
      this.openAt = 0;
      return;
    }
    if (Laby.labyAPI().minecraft().minecraftWindow().isScreenOpened()) {
      this.openAt = System.currentTimeMillis() + 2000L; // gerade ist ein anderes Menue offen: spaeter noch einmal versuchen
      return;
    }
    this.openAt = 0;
    Screens.now(new LootActivity());
  }
}
