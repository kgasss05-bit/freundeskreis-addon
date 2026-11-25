package de.ulgu.fkreis.core.config;

import de.ulgu.fkreis.core.match.MatchActivity;
import de.ulgu.fkreis.core.ui.Screens;
import net.labymod.api.client.gui.screen.widget.widgets.input.ButtonWidget.ButtonSetting;
import net.labymod.api.client.gui.screen.widget.widgets.input.SliderWidget.SliderSetting;
import net.labymod.api.client.gui.screen.widget.widgets.input.SwitchWidget.SwitchSetting;
import net.labymod.api.configuration.loader.Config;
import net.labymod.api.configuration.loader.property.ConfigProperty;
import net.labymod.api.configuration.settings.Setting;
import net.labymod.api.util.MethodOrder;

/** Unterseite "Freunde-Match". */
public class MatchSettings extends Config {

  /** Wenn an: Sucht jemand auf dem Server nach passenden Spielern, werden deine Antworten zum Vergleichen geschickt. */
  @SwitchSetting
  private final ConfigProperty<Boolean> participate = new ConfigProperty<>(true);

  /** "Blockmatch" im Freundeskreis-Rad zeigen. */
  @SwitchSetting
  private final ConfigProperty<Boolean> inWheel = new ConfigProperty<>(true);

  /** Hintergrundmusik, solange Blockmatch offen ist. */
  @SwitchSetting
  private final ConfigProperty<Boolean> music = new ConfigProperty<>(true);

  /** Lautstaerke der Musik in Prozent. */
  @SliderSetting(min = 0, max = 100, steps = 5)
  private final ConfigProperty<Float> volume = new ConfigProperty<>(35F);

  @MethodOrder(after = "participate")
  @ButtonSetting
  public void open(Setting setting) {
    Screens.open(new MatchActivity());
  }

  /** Farbwelt des Menues aussuchen (Rosa, Hellblau, Mint, Flieder oder das dunkle Holz-Design). */
  @MethodOrder(after = "background")
  @ButtonSetting
  public void colors(Setting setting) {
    Screens.open(new MatchActivity(true));
  }

  /** Holz fuer den Hintergrund dieses Menues aussuchen. */
  @MethodOrder(after = "open")
  @ButtonSetting
  public void background(Setting setting) {
    Screens.open(new de.ulgu.fkreis.core.gfx.BackdropPickerActivity(false));
  }

  public ConfigProperty<Boolean> inWheel() {
    return this.inWheel;
  }

  public ConfigProperty<Float> volume() {
    return this.volume;
  }

  public ConfigProperty<Boolean> music() {
    return this.music;
  }

  public ConfigProperty<Boolean> participate() {
    return this.participate;
  }
}
