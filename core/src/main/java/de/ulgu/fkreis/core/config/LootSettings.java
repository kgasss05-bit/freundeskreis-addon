package de.ulgu.fkreis.core.config;

import de.ulgu.fkreis.core.loot.LootActivity;
import de.ulgu.fkreis.core.ui.Screens;
import net.labymod.api.client.gui.screen.widget.widgets.input.ButtonWidget.ButtonSetting;
import net.labymod.api.client.gui.screen.widget.widgets.input.SwitchWidget.SwitchSetting;
import net.labymod.api.configuration.loader.Config;
import net.labymod.api.configuration.loader.property.ConfigProperty;
import net.labymod.api.configuration.settings.Setting;
import net.labymod.api.util.MethodOrder;

/** Unterseite "Taegliche Truhe". */
public class LootSettings extends Config {

  /** Truhe einmal am Tag von selbst oeffnen, kurz nachdem man einen Server betreten hat. */
  @SwitchSetting
  private final ConfigProperty<Boolean> daily = new ConfigProperty<>(true);

  @MethodOrder(after = "daily")
  @ButtonSetting
  public void open(Setting setting) {
    Screens.open(new LootActivity());
  }

  public ConfigProperty<Boolean> daily() {
    return this.daily;
  }
}
