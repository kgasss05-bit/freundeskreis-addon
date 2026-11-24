package de.ulgu.fkreis.core.config;

import de.ulgu.fkreis.core.ui.Screens;
import de.ulgu.fkreis.core.zeitbrief.LetterActivity;
import net.labymod.api.client.gui.screen.widget.widgets.input.ButtonWidget.ButtonSetting;
import net.labymod.api.client.gui.screen.widget.widgets.input.SwitchWidget.SwitchSetting;
import net.labymod.api.configuration.loader.Config;
import net.labymod.api.configuration.loader.property.ConfigProperty;
import net.labymod.api.configuration.settings.Setting;
import net.labymod.api.util.MethodOrder;

/** Unterseite "Zeitbrief". */
public class TimeLetterSettings extends Config {

  /** Beim Betreten eines Servers melden, wenn ein Brief angekommen ist. */
  @SwitchSetting
  private final ConfigProperty<Boolean> notifyArrival = new ConfigProperty<>(true);

  @MethodOrder(after = "notifyArrival")
  @ButtonSetting
  public void open(Setting setting) {
    Screens.open(new LetterActivity());
  }

  /** Holz fuer den Hintergrund dieses Menues aussuchen. */
  @MethodOrder(after = "open")
  @ButtonSetting
  public void background(Setting setting) {
    Screens.open(new de.ulgu.fkreis.core.gfx.BackdropPickerActivity(false));
  }

  public ConfigProperty<Boolean> notifyArrival() {
    return this.notifyArrival;
  }
}
