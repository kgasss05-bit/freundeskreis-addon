package de.ulgu.fkreis.core.config;

import de.ulgu.fkreis.core.ui.PhotoPickerActivity;
import de.ulgu.fkreis.core.ui.Screens;
import net.labymod.api.client.gui.screen.key.Key;
import net.labymod.api.client.gui.screen.widget.widgets.input.ButtonWidget.ButtonSetting;
import net.labymod.api.client.gui.screen.widget.widgets.input.MultiKeybindWidget.MultiKeyBindSetting;
import net.labymod.api.client.gui.screen.widget.widgets.input.SwitchWidget.SwitchSetting;
import net.labymod.api.configuration.loader.Config;
import net.labymod.api.configuration.loader.property.ConfigProperty;
import net.labymod.api.configuration.settings.Setting;
import net.labymod.api.util.MethodOrder;

/** Unterseite "Fotoalbum". */
public class AlbumSettings extends Config {

  /** Andere duerfen dein Album ueber das Mausrad ansehen. */
  @net.labymod.api.client.gui.screen.widget.widgets.input.SwitchWidget.SwitchSetting
  private final ConfigProperty<Boolean> share = new ConfigProperty<>(true);

  @net.labymod.api.client.gui.screen.widget.widgets.input.SwitchWidget.SwitchSetting
  private final ConfigProperty<Boolean> inWheel = new ConfigProperty<>(true);

  @MultiKeyBindSetting
  private final ConfigProperty<Key[]> openKeys = new ConfigProperty<>(new Key[0]);

  /** Zuletzt gewaehlter Rahmen (wird beim naechsten Foto vorausgewaehlt). */
  private final ConfigProperty<String> lastFrame = new ConfigProperty<>("GOLD");

  @MethodOrder(after = "openKeys")
  @ButtonSetting
  public void open(Setting setting) {
    Screens.openAlbum();
  }

  @MethodOrder(after = "open")
  @ButtonSetting
  public void add(Setting setting) {
    Screens.open(new PhotoPickerActivity());
  }

  public ConfigProperty<Boolean> share() {
    return this.share;
  }

  public ConfigProperty<Boolean> inWheel() {
    return this.inWheel;
  }

  public ConfigProperty<Key[]> openKeys() {
    return this.openKeys;
  }

  public ConfigProperty<String> lastFrame() {
    return this.lastFrame;
  }
}
