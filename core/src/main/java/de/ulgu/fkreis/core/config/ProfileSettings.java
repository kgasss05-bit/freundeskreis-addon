package de.ulgu.fkreis.core.config;

import de.ulgu.fkreis.core.ui.MobPickerActivity;
import de.ulgu.fkreis.core.ui.Screens;
import net.labymod.api.client.gui.screen.activity.Activity;
import net.labymod.api.client.gui.screen.key.Key;
import net.labymod.api.client.gui.screen.widget.widgets.activity.settings.ActivitySettingWidget.ActivitySetting;
import net.labymod.api.client.gui.screen.widget.widgets.input.ButtonWidget.ButtonSetting;
import net.labymod.api.client.gui.screen.widget.widgets.input.MultiKeybindWidget.MultiKeyBindSetting;
import net.labymod.api.client.gui.screen.widget.widgets.input.SwitchWidget.SwitchSetting;
import net.labymod.api.client.gui.screen.widget.widgets.input.TextFieldWidget.TextFieldSetting;
import net.labymod.api.configuration.loader.Config;
import net.labymod.api.configuration.loader.annotation.IntroducedIn;
import net.labymod.api.configuration.loader.property.ConfigProperty;
import net.labymod.api.configuration.settings.Setting;
import net.labymod.api.util.MethodOrder;

/** Unterseite "Steckbrief". Der Geburtstag steht jetzt im Bereich "Kalender". */
public class ProfileSettings extends Config {

  @SwitchSetting
  private final ConfigProperty<Boolean> share = new ConfigProperty<>(true);

  @SwitchSetting
  private final ConfigProperty<Boolean> inWheel = new ConfigProperty<>(true);

  @TextFieldSetting
  private final ConfigProperty<String> nickname = new ConfigProperty<>("");

  @TextFieldSetting
  private final ConfigProperty<String> age = new ConfigProperty<>("");

  @TextFieldSetting
  private final ConfigProperty<String> country = new ConfigProperty<>("");

  /** Bundesland / Region – frei eintippen (z. B. Hessen, Texas, Bayern …). */
  @TextFieldSetting
  private final ConfigProperty<String> state = new ConfigProperty<>("");

  @IntroducedIn(namespace = "fkreis", value = "2.0.0")
  @MultiKeyBindSetting
  private final ConfigProperty<Key[]> openKeys = new ConfigProperty<>(new Key[0]);

  /** Alter Speicherort des Geburtstags (nur noch zum Uebernehmen in den Kalender, nicht sichtbar). */
  private final ConfigProperty<String> birthday = new ConfigProperty<>("");

  /** Gewaehlter Mob (Minecraft-ID). Wird ueber die Mob-Auswahl gesetzt. */
  private final ConfigProperty<String> mob = new ConfigProperty<>("none");

  @MethodOrder(after = "state")
  @ActivitySetting
  public Activity statePicker() {
    return new de.ulgu.fkreis.core.ui.StatePickerActivity();
  }

  @MethodOrder(after = "statePicker")
  @ActivitySetting
  public Activity mobPicker() {
    return new MobPickerActivity();
  }

  /** Holzart des Steckbrief-Rahmens (WoodType-Name). */
  private final ConfigProperty<String> wood = new ConfigProperty<>("OAK");

  @MethodOrder(after = "mobPicker")
  @IntroducedIn(namespace = "fkreis", value = "2.0.0")
  @ActivitySetting
  public Activity woodPicker() {
    return new de.ulgu.fkreis.core.ui.WoodPickerActivity(false, true);
  }

  public ConfigProperty<String> wood() {
    return this.wood;
  }

  @MethodOrder(after = "inWheel")
  @IntroducedIn(namespace = "fkreis", value = "2.0.0")
  @ButtonSetting
  public void open(Setting setting) {
    Screens.openOwnProfile();
  }

  public ConfigProperty<Boolean> share() {
    return this.share;
  }

  public ConfigProperty<Boolean> inWheel() {
    return this.inWheel;
  }

  public ConfigProperty<String> nickname() {
    return this.nickname;
  }

  public ConfigProperty<String> age() {
    return this.age;
  }

  public ConfigProperty<String> legacyBirthday() {
    return this.birthday;
  }

  public ConfigProperty<String> country() {
    return this.country;
  }

  public ConfigProperty<String> state() {
    return this.state;
  }

  public ConfigProperty<Key[]> openKeys() {
    return this.openKeys;
  }

  public ConfigProperty<String> mob() {
    return this.mob;
  }
}
