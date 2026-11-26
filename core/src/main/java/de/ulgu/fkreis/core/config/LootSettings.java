package de.ulgu.fkreis.core.config;

import de.ulgu.fkreis.core.loot.LootActivity;
import de.ulgu.fkreis.core.ui.Screens;
import net.labymod.api.client.gui.screen.widget.widgets.input.ButtonWidget.ButtonSetting;
import net.labymod.api.client.gui.screen.widget.widgets.input.SwitchWidget.SwitchSetting;
import net.labymod.api.configuration.loader.Config;
import net.labymod.api.configuration.loader.property.ConfigProperty;
import net.labymod.api.configuration.settings.Setting;
import net.labymod.api.util.MethodOrder;

/** Unterseite "Gluecksrad". */
public class LootSettings extends Config {

  /** Einmal am Tag nach dem Betreten eines Servers an das Gluecksrad erinnern. */
  @SwitchSetting
  private final ConfigProperty<Boolean> daily = new ConfigProperty<>(true);

  @MethodOrder(after = "daily")
  @ButtonSetting
  public void open(Setting setting) {
    Screens.open(new LootActivity());
  }

  /** Holz hinter dem Gluecksrad aussuchen. */
  @MethodOrder(after = "open")
  @ButtonSetting
  public void background(Setting setting) {
    Screens.open(de.ulgu.fkreis.core.gfx.BackdropPickerActivity.forWheel());
  }

  public ConfigProperty<Boolean> daily() {
    return this.daily;
  }
}
