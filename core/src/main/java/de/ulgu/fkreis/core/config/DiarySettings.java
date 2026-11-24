package de.ulgu.fkreis.core.config;

import net.labymod.api.client.gui.screen.key.Key;
import net.labymod.api.client.gui.screen.widget.widgets.input.ButtonWidget.ButtonSetting;
import net.labymod.api.client.gui.screen.widget.widgets.input.MultiKeybindWidget.MultiKeyBindSetting;
import net.labymod.api.client.gui.screen.widget.widgets.input.SwitchWidget.SwitchSetting;
import net.labymod.api.client.gui.screen.widget.widgets.input.TextFieldWidget.TextFieldSetting;
import net.labymod.api.configuration.loader.Config;
import net.labymod.api.configuration.loader.property.ConfigProperty;
import net.labymod.api.configuration.settings.Setting;
import net.labymod.api.util.MethodOrder;

/** Unterseite "Tagebuch": Erinnerung beim Server-Join, Nachfrage beim Beenden, Einband, Taste. */
public class DiarySettings extends Config {

  /** "Du hast heute noch kein Tagebuch geschrieben" beim Betreten eines Servers. */
  @SwitchSetting
  private final ConfigProperty<Boolean> remind = new ConfigProperty<>(true);

  /** Vor dem Beenden des Spiels nachfragen, ob man noch einen Eintrag schreiben moechte. */
  @SwitchSetting
  private final ConfigProperty<Boolean> askOnQuit = new ConfigProperty<>(true);

  @MultiKeyBindSetting
  private final ConfigProperty<Key[]> openKeys = new ConfigProperty<>(new Key[0]);

  /** Code, den dir jemand gegeben hat, um sein Tagebuch zu lesen. */
  @TextFieldSetting
  private final ConfigProperty<String> friendCode = new ConfigProperty<>("");

  @MethodOrder(after = "friendCode")
  @ButtonSetting
  public void openShared(Setting setting) {
    de.ulgu.fkreis.core.ui.Screens.openSharedDiary();
  }

  public ConfigProperty<String> friendCode() {
    return this.friendCode;
  }

  /** Knoepfe brauchen eine feste Position, sonst zeigt LabyMod sie nicht an. */
  @MethodOrder(after = "askOnQuit")
  @ButtonSetting
  public void open(Setting setting) {
    de.ulgu.fkreis.core.ui.Screens.openDiary();
  }

  @MethodOrder(after = "openKeys")
  @ButtonSetting
  public void cover(Setting setting) {
    de.ulgu.fkreis.core.ui.Screens.open(new de.ulgu.fkreis.core.diary.DiaryCoverActivity());
  }

  public ConfigProperty<Boolean> remind() {
    return this.remind;
  }

  public ConfigProperty<Boolean> askOnQuit() {
    return this.askOnQuit;
  }

  public ConfigProperty<Key[]> openKeys() {
    return this.openKeys;
  }
}
