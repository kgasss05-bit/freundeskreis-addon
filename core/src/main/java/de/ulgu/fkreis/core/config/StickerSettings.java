package de.ulgu.fkreis.core.config;

import de.ulgu.fkreis.core.ui.Screens;
import net.labymod.api.client.gui.screen.key.Key;
import net.labymod.api.client.gui.screen.widget.widgets.input.ButtonWidget.ButtonSetting;
import net.labymod.api.client.gui.screen.widget.widgets.input.MultiKeybindWidget.MultiKeyBindSetting;
import net.labymod.api.client.gui.screen.widget.widgets.input.SwitchWidget.SwitchSetting;
import net.labymod.api.configuration.loader.Config;
import net.labymod.api.configuration.loader.property.ConfigProperty;
import net.labymod.api.configuration.settings.Setting;
import net.labymod.api.util.MethodOrder;

/** Unterseite "Sammelbuch". */
public class StickerSettings extends Config {

  @SwitchSetting
  private final ConfigProperty<Boolean> inWheel = new ConfigProperty<>(true);

  @SwitchSetting
  private final ConfigProperty<Boolean> accept = new ConfigProperty<>(true);

  @MultiKeyBindSetting
  private final ConfigProperty<Key[]> openKeys = new ConfigProperty<>(new Key[0]);

  @MethodOrder(after = "accept")
  @ButtonSetting
  public void open(Setting setting) {
    Screens.openStickerBook();
  }

  public ConfigProperty<Boolean> inWheel() {
    return this.inWheel;
  }

  public ConfigProperty<Boolean> accept() {
    return this.accept;
  }

  public ConfigProperty<Key[]> openKeys() {
    return this.openKeys;
  }
}
