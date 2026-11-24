package de.ulgu.fkreis.core.config;

import de.ulgu.fkreis.core.ui.Ui;
import net.labymod.api.addon.AddonConfig;
import net.labymod.api.client.gui.screen.widget.widgets.input.ButtonWidget.ButtonSetting;
import net.labymod.api.client.gui.screen.widget.widgets.input.SwitchWidget.SwitchSetting;
import net.labymod.api.configuration.loader.annotation.ConfigName;
import net.labymod.api.configuration.loader.annotation.IntroducedIn;
import net.labymod.api.configuration.loader.property.ConfigProperty;
import net.labymod.api.configuration.settings.Setting;
import net.labymod.api.util.MethodOrder;

/** Hauptseite der Einstellungen. */
@ConfigName("settings")
public class FkreisConfig extends AddonConfig {

  @SwitchSetting
  private final ConfigProperty<Boolean> enabled = new ConfigProperty<>(true);

  private final ProfileSettings profile = new ProfileSettings();

  private final StickerSettings stickerBook = new StickerSettings();

  private final NotebookSettings notebook = new NotebookSettings();

  @IntroducedIn(namespace = "fkreis", value = "2.0.0")
  private final CoinSettings coins = new CoinSettings();

  @IntroducedIn(namespace = "fkreis", value = "2.0.0")
  private final CalendarSettings calendar = new CalendarSettings();

  @IntroducedIn(namespace = "fkreis", value = "2.0.0")
  private final AlbumSettings album = new AlbumSettings();

  @IntroducedIn(namespace = "fkreis", value = "2.1.0")
  private final VanishSettings vanish = new VanishSettings();

  @IntroducedIn(namespace = "fkreis", value = "2.2.0")
  private final DiarySettings diary = new DiarySettings();

  @IntroducedIn(namespace = "fkreis", value = "2.3.0")
  private final MixtapeSettings mixtape = new MixtapeSettings();

  @IntroducedIn(namespace = "fkreis", value = "2.3.0")
  private final TimeLetterSettings timeLetter = new TimeLetterSettings();

  /** Ganz oben: Knopf "HUD-Widget einstellen". */
  @MethodOrder(after = "enabled")
  @IntroducedIn(namespace = "fkreis", value = "2.0.0")
  @ButtonSetting
  public void hudWidget(Setting setting) {
    Ui.openHudEditor();
  }

  @Override
  public ConfigProperty<Boolean> enabled() {
    return this.enabled;
  }

  public ProfileSettings profile() {
    return this.profile;
  }

  public StickerSettings stickerBook() {
    return this.stickerBook;
  }

  public NotebookSettings notebook() {
    return this.notebook;
  }

  public CoinSettings coins() {
    return this.coins;
  }

  public CalendarSettings calendar() {
    return this.calendar;
  }

  public MixtapeSettings mixtape() {
    return this.mixtape;
  }

  public TimeLetterSettings timeLetter() {
    return this.timeLetter;
  }

  public DiarySettings diary() {
    return this.diary;
  }

  public VanishSettings vanish() {
    return this.vanish;
  }

  public AlbumSettings album() {
    return this.album;
  }
}
