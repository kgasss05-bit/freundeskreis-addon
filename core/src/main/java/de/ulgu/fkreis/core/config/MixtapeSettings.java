package de.ulgu.fkreis.core.config;

import de.ulgu.fkreis.core.mixtape.MixtapeActivity;
import de.ulgu.fkreis.core.ui.Screens;
import net.labymod.api.client.gui.screen.widget.widgets.input.ButtonWidget.ButtonSetting;
import net.labymod.api.client.gui.screen.widget.widgets.input.SwitchWidget.SwitchSetting;
import net.labymod.api.configuration.loader.Config;
import net.labymod.api.configuration.loader.property.ConfigProperty;
import net.labymod.api.configuration.settings.Setting;
import net.labymod.api.util.MethodOrder;

/** Unterseite "Mixtapes". */
public class MixtapeSettings extends Config {

  /** "Mixtape schenken" im Freundeskreis-Rad zeigen. */
  @SwitchSetting
  private final ConfigProperty<Boolean> inWheel = new ConfigProperty<>(true);

  /** Andere duerfen dir Kassetten anbieten. */
  @SwitchSetting
  private final ConfigProperty<Boolean> acceptGifts = new ConfigProperty<>(true);

  /** Kleiner schwebender Rekorder oben rechts, solange eine Kassette laeuft. Aus: nur eine Zeile im Chat. */
  @SwitchSetting
  private final ConfigProperty<Boolean> hud = new ConfigProperty<>(true);

  /** Schaltet die Groesse des schwebenden Rekorders weiter: klein, mittel, gross. */
  @MethodOrder(after = "hud")
  @ButtonSetting
  public void hudSize(Setting setting) {
    de.ulgu.fkreis.core.mixtape.Tapes box = de.ulgu.fkreis.core.mixtape.Tapes.get();
    box.hudSize = (box.hudSize + 1) % 3;
    box.save();
    de.ulgu.fkreis.core.net.Net.notify(de.ulgu.fkreis.core.L.t("mixtape.title"), de.ulgu.fkreis.core.L.t("mixtape.hudSize" + box.hudSize), null);
  }

  @MethodOrder(after = "acceptGifts")
  @ButtonSetting
  public void open(Setting setting) {
    Screens.open(new MixtapeActivity());
  }

  public ConfigProperty<Boolean> inWheel() {
    return this.inWheel;
  }

  /** Holz fuer den Hintergrund dieses Menues aussuchen. */
  @MethodOrder(after = "open")
  @ButtonSetting
  public void background(Setting setting) {
    Screens.open(new de.ulgu.fkreis.core.gfx.BackdropPickerActivity(true));
  }

  public ConfigProperty<Boolean> hud() {
    return this.hud;
  }

  public ConfigProperty<Boolean> acceptGifts() {
    return this.acceptGifts;
  }
}
