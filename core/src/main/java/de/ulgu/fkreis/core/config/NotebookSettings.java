package de.ulgu.fkreis.core.config;

import de.ulgu.fkreis.core.ui.Screens;
import de.ulgu.fkreis.core.ui.WoodPickerActivity;
import net.labymod.api.client.gui.screen.activity.Activity;
import net.labymod.api.client.gui.screen.key.Key;
import net.labymod.api.client.gui.screen.widget.widgets.activity.settings.ActivitySettingWidget.ActivitySetting;
import net.labymod.api.client.gui.screen.widget.widgets.input.ButtonWidget.ButtonSetting;
import net.labymod.api.client.gui.screen.widget.widgets.input.MultiKeybindWidget.MultiKeyBindSetting;
import net.labymod.api.client.gui.screen.widget.widgets.input.SwitchWidget.SwitchSetting;
import net.labymod.api.configuration.loader.Config;
import net.labymod.api.configuration.loader.property.ConfigProperty;
import net.labymod.api.configuration.settings.Setting;
import net.labymod.api.util.MethodOrder;

/** Unterseite "Notizbuch" (Regal + Holzart). */
public class NotebookSettings extends Config {

  @SwitchSetting
  private final ConfigProperty<Boolean> inWheel = new ConfigProperty<>(true);

  @MultiKeyBindSetting
  private final ConfigProperty<Key[]> openKeys = new ConfigProperty<>(new Key[0]);

  /** Gewaehlte Holzart (WoodType-Name). */
  private final ConfigProperty<String> wood = new ConfigProperty<>("SPRUCE");

  @MethodOrder(after = "inWheel")
  @ActivitySetting
  public Activity woodPicker() {
    return new WoodPickerActivity(false);
  }

  @MethodOrder(after = "inWheel")
  @ButtonSetting
  public void open(Setting setting) {
    Screens.openNotebook();
  }

  public ConfigProperty<Boolean> inWheel() {
    return this.inWheel;
  }

  public ConfigProperty<Key[]> openKeys() {
    return this.openKeys;
  }

  public ConfigProperty<String> wood() {
    return this.wood;
  }
}
