package de.ulgu.fkreis.core.config;

import de.ulgu.fkreis.core.loot.StickerShopActivity;
import de.ulgu.fkreis.core.ui.Screens;
import net.labymod.api.client.gui.screen.widget.widgets.input.ButtonWidget.ButtonSetting;
import net.labymod.api.client.gui.screen.widget.widgets.input.SwitchWidget.SwitchSetting;
import net.labymod.api.configuration.loader.Config;
import net.labymod.api.configuration.loader.property.ConfigProperty;
import net.labymod.api.configuration.settings.Setting;
import net.labymod.api.util.MethodOrder;

/** Unterseite "Sticker-Grotte" (getrennt vom Gluecksrad). */
public class ShopSettings extends Config {

  /** Eintrag im /fk-Menue zeigen. Die Seite braucht mindestens eine echte Einstellung, sonst blendet LabyMod sie aus. */
  @SwitchSetting
  private final ConfigProperty<Boolean> inHub = new ConfigProperty<>(true);

  @MethodOrder(after = "inHub")
  @ButtonSetting
  public void open(Setting setting) {
    Screens.open(new StickerShopActivity());
  }

  public ConfigProperty<Boolean> inHub() {
    return this.inHub;
  }
}
