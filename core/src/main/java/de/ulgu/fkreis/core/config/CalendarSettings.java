package de.ulgu.fkreis.core.config;

import de.ulgu.fkreis.core.ui.Screens;
import net.labymod.api.client.gui.screen.key.Key;
import net.labymod.api.client.gui.screen.widget.widgets.input.ButtonWidget.ButtonSetting;
import net.labymod.api.client.gui.screen.widget.widgets.input.MultiKeybindWidget.MultiKeyBindSetting;
import net.labymod.api.client.gui.screen.widget.widgets.input.SwitchWidget.SwitchSetting;
import net.labymod.api.client.gui.screen.widget.widgets.input.TextFieldWidget.TextFieldSetting;
import net.labymod.api.configuration.loader.Config;
import net.labymod.api.configuration.loader.property.ConfigProperty;
import net.labymod.api.configuration.settings.Setting;
import net.labymod.api.util.MethodOrder;

/** Unterseite "Kalender": eigener Geburtstag (nur alle 30 Tage aenderbar), Hinweise, Kuchen, Tasten. */
public class CalendarSettings extends Config {

  @TextFieldSetting
  private final ConfigProperty<String> birthday = new ConfigProperty<>("");

  @SwitchSetting
  private final ConfigProperty<Boolean> reminders = new ConfigProperty<>(true);

  @SwitchSetting
  private final ConfigProperty<Boolean> cake = new ConfigProperty<>(true);

  @MultiKeyBindSetting
  private final ConfigProperty<Key[]> openKeys = new ConfigProperty<>(new Key[0]);

  @MethodOrder(after = "birthday")
  @ButtonSetting
  public void open(Setting setting) {
    Screens.openCalendar();
  }

  public ConfigProperty<String> birthday() {
    return this.birthday;
  }

  public ConfigProperty<Boolean> reminders() {
    return this.reminders;
  }

  public ConfigProperty<Boolean> cake() {
    return this.cake;
  }

  public ConfigProperty<Key[]> openKeys() {
    return this.openKeys;
  }
}
