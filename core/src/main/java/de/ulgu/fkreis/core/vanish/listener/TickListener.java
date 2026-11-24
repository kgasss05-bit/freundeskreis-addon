package de.ulgu.fkreis.core.vanish.listener;

import de.ulgu.fkreis.api.VanisherState;
import de.ulgu.fkreis.core.vanish.Vanish;
import de.ulgu.fkreis.core.vanish.ui.Screens;
import net.labymod.api.Laby;
import net.labymod.api.client.entity.player.Player;
import net.labymod.api.client.world.ClientWorld;
import net.labymod.api.event.Subscribe;
import net.labymod.api.event.client.lifecycle.GameTickEvent;

/** Jeden Tick: an/aus weitergeben, Namen aktualisieren, Stummschaltung pruefen. */
public class TickListener {

  private final Vanish addon;
  private int ticks;

  public TickListener(Vanish addon) {
    this.addon = addon;
  }

  @Subscribe
  public void onTick(GameTickEvent event) {
    Screens.tick();
    VanisherState.enabled = this.addon.on();
    if (++this.ticks % 40 != 0) {
      return;
    }
    this.addon.voice().refresh();
    try {
      ClientWorld world = Laby.labyAPI().minecraft().clientWorld();
      if (world == null) {
        return;
      }
      for (Player p : world.getPlayers()) {
        this.addon.store().updateName(p.getUniqueId(), p.getName());
      }
    } catch (Throwable ignored) {
    }
  }
}
