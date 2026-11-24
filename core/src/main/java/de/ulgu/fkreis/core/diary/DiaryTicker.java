package de.ulgu.fkreis.core.diary;

import de.ulgu.fkreis.api.DiaryState;
import de.ulgu.fkreis.core.FkreisAddon;
import de.ulgu.fkreis.core.L;
import de.ulgu.fkreis.core.gfx.G;
import de.ulgu.fkreis.core.net.Net;
import de.ulgu.fkreis.core.ui.Screens;
import net.labymod.api.Laby;
import net.labymod.api.event.Subscribe;
import net.labymod.api.event.client.lifecycle.GameTickEvent;
import net.labymod.api.event.client.network.server.ServerDisconnectEvent;
import net.labymod.api.event.client.network.server.ServerJoinEvent;

/**
 * Tagebuch im Hintergrund:
 * - Erinnerung kurz nach dem Betreten eines Servers.
 * - Nachfrage "noch einen Eintrag schreiben?": auf Versionen mit dem Spiel-Eingriff direkt beim Beenden des Spiels,
 *   auf allen anderen Versionen stattdessen kurz nach dem Verlassen eines Servers.
 */
public class DiaryTicker {

  private final FkreisAddon addon;
  private long remindAt;
  private long askAt;

  public DiaryTicker(FkreisAddon addon) {
    this.addon = addon;
    DiaryState.shouldAsk = this::shouldAsk;
    DiaryState.openAsk = () -> Screens.now(new DiaryQuitActivity(true));
  }

  private boolean shouldAsk() {
    return this.addon.on() && this.addon.configuration().diary().askOnQuit().get() && !Diary.get().wroteToday();
  }

  @Subscribe
  public void onJoin(ServerJoinEvent event) {
    this.remindAt = System.currentTimeMillis() + 6000L; // erst kurz ankommen lassen
    this.askAt = 0;
  }

  @Subscribe
  public void onDisconnect(ServerDisconnectEvent event) {
    this.remindAt = 0;
    if (!DiaryState.hookActive && this.shouldAsk()) {
      this.askAt = System.currentTimeMillis() + 1500L; // erst das Hauptmenue erscheinen lassen
    }
  }

  @Subscribe
  public void onTick(GameTickEvent event) {
    long now = System.currentTimeMillis();
    if (this.remindAt > 0 && now >= this.remindAt) {
      this.remindAt = 0;
      if (Laby.labyAPI().minecraft().isIngame() && this.addon.on()
          && this.addon.configuration().diary().remind().get() && !Diary.get().wroteToday()) {
        Net.notify(L.t("diary.title"), L.t("diary.remind"), null);
        G.sound("block.note_block.chime", 1.2F);
      }
    }
    if (this.askAt > 0 && now >= this.askAt) {
      this.askAt = 0;
      if (!Laby.labyAPI().minecraft().isIngame() && this.shouldAsk()) {
        Screens.now(new DiaryQuitActivity(false));
      }
    }
  }
}
