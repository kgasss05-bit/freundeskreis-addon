package de.ulgu.fkreis.core.config;

import de.ulgu.fkreis.core.ui.CoinGalleryActivity;
import net.labymod.api.client.gui.screen.activity.Activity;
import net.labymod.api.client.gui.screen.widget.widgets.activity.settings.ActivitySettingWidget.ActivitySetting;
import net.labymod.api.client.gui.screen.widget.widgets.input.SwitchWidget.SwitchSetting;
import net.labymod.api.configuration.loader.Config;
import net.labymod.api.configuration.loader.property.ConfigProperty;
import net.labymod.api.util.MethodOrder;

/** Unterseite "Freundschafts-Muenzen". */
public class CoinSettings extends Config {

  @SwitchSetting
  private final ConfigProperty<Boolean> enabled = new ConfigProperty<>(true);

  @MethodOrder(after = "enabled")
  @ActivitySetting
  public Activity gallery() {
    return new CoinGalleryActivity();
  }

  public ConfigProperty<Boolean> enabled() {
    return this.enabled;
  }
}
