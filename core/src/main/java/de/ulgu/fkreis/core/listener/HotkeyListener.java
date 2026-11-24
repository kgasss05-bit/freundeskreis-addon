package de.ulgu.fkreis.core.listener;

import de.ulgu.fkreis.core.FkreisAddon;
import de.ulgu.fkreis.core.ui.Screens;
import java.util.HashSet;
import java.util.Set;
import net.labymod.api.Laby;
import net.labymod.api.client.gui.screen.key.Key;
import net.labymod.api.event.Subscribe;
import net.labymod.api.event.client.input.KeyEvent;

/** Frei waehlbare Tastenkombinationen (eine oder mehrere Tasten), nur im Spiel aktiv. */
public class HotkeyListener {

  private final FkreisAddon addon;
  private final Set<Key> down = new HashSet<>();

  public HotkeyListener(FkreisAddon addon) {
    this.addon = addon;
  }

  @Subscribe
  public void onKey(KeyEvent event) {
    Key key = event.key();
    if (event.state() == KeyEvent.State.UNPRESSED) {
      this.down.remove(key);
      return;
    }
    if (event.state() != KeyEvent.State.PRESS) {
      return;
    }
    this.down.add(key);
    if (!this.addon.on() || Laby.labyAPI().minecraft().minecraftWindow().isScreenOpened()) {
      return;
    }
    if (combo(this.addon.configuration().stickerBook().openKeys().get(), key)) {
      Screens.openStickerBook();
    } else if (combo(this.addon.configuration().notebook().openKeys().get(), key)) {
      Screens.openNotebook();
    } else if (combo(this.addon.configuration().profile().openKeys().get(), key)) {
      Screens.openOwnProfile();
    } else if (combo(this.addon.configuration().calendar().openKeys().get(), key)) {
      Screens.openCalendar();
    } else if (combo(this.addon.configuration().album().openKeys().get(), key)) {
      Screens.openAlbum();
    } else if (combo(this.addon.configuration().diary().openKeys().get(), key)) {
      Screens.openDiary();
    } else if (this.addon.configuration().vanish().enabled().get()
        && combo(this.addon.configuration().vanish().openKeys().get(), key)) {
      de.ulgu.fkreis.core.vanish.ui.Screens.openList();
    }
  }

  private boolean combo(Key[] keys, Key pressed) {
    if (keys == null || keys.length == 0) {
      return false;
    }
    boolean containsPressed = false;
    for (Key k : keys) {
      if (k == null) {
        continue;
      }
      if (k.equals(pressed)) {
        containsPressed = true;
      }
      if (!this.down.contains(k)) {
        return false;
      }
    }
    return containsPressed;
  }
}
