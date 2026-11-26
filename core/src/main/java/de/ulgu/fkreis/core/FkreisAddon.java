package de.ulgu.fkreis.core;

import de.ulgu.fkreis.core.config.FkreisConfig;
import de.ulgu.fkreis.core.data.Store;
import de.ulgu.fkreis.core.listener.HotkeyListener;
import de.ulgu.fkreis.core.coins.CakeNameTag;
import de.ulgu.fkreis.core.coins.CoinNameTag;
import de.ulgu.fkreis.core.hud.BirthdayWidget;
import de.ulgu.fkreis.core.menu.FreundeskreisBullet;
import de.ulgu.fkreis.core.net.Net;
import de.ulgu.fkreis.core.ui.FkCommands;
import net.labymod.api.addon.LabyAddon;
import net.labymod.api.client.entity.player.tag.PositionType;
import net.labymod.api.models.addon.annotation.AddonMain;
import net.labymod.api.util.logging.Logging;

/**
 * Freundeskreis – ULGU Software.
 * Steckbrief, Sammelbuch, Notizbuch-Regal, Freundschafts-Muenzen, Kalender (Geburtstage + HUD) und Fotoalbum.
 */
@AddonMain
public class FkreisAddon extends LabyAddon<FkreisConfig> {

  private static FkreisAddon instance;

  private static final Logging LOG = Logging.create("Freundeskreis");

  /** Logger des Addons (statt System.out). */
  public static Logging log() {
    return LOG;
  }

  private Store store;
  private Net net;

  @Override
  protected void enable() {
    instance = this;
    this.registerSettingCategory();

    this.store = Store.load();
    this.net = new Net(this);

    this.registerListener(this.net);
    this.registerListener(new HotkeyListener(this));

    // Ein einziger Eintrag "Freundeskreis" im LabyMod-Mausrad -> eigenes Rad
    this.labyAPI().interactionMenuRegistry().register(new FreundeskreisBullet(this));

    // Freundschafts-Muenze rechts neben den Namen anderer Spieler
    this.labyAPI().tagRegistry().registerAfter("labymod_role", "fkreis_coin",
        PositionType.RIGHT_TO_NAME, new CoinNameTag());
    // Geburtstags-Kuchen (nur am Geburtstag) gleich daneben
    this.labyAPI().tagRegistry().registerAfter("fkreis_coin", "fkreis_cake",
        PositionType.RIGHT_TO_NAME, new CakeNameTag());

    // HUD-Widget "Naechster Geburtstag"
    try {
      this.labyAPI().hudWidgetRegistry().register(new BirthdayWidget(this));
      this.labyAPI().hudWidgetRegistry().register(new de.ulgu.fkreis.core.hud.MixtapeWidget());
    } catch (Throwable t) {
      this.logger().info("HUD-Widget konnte nicht registriert werden: " + t);
    }

    this.registerCommand(new FkCommands.Sammelbuch());
    this.registerCommand(new FkCommands.Notizbuch());
    this.registerCommand(new FkCommands.Steckbrief());
    this.registerCommand(new FkCommands.Hub());
    this.registerCommand(new FkCommands.Kalender());
    this.registerCommand(new FkCommands.Fotoalbum());

    // Tagebuch: privat, nur ueber /fk oder die Taste – bewusst NICHT im Freundeskreis-Rad
    this.registerListener(new de.ulgu.fkreis.core.diary.DiaryTicker(this));
    this.registerListener(new de.ulgu.fkreis.core.loot.LootTicker(this));

    // Vanish: Spieler nur fuer dich ausblenden (Freundeskreis-Rad, /fk, Schriftrolle der Verbannten)
    de.ulgu.fkreis.core.vanish.Vanish vanish = de.ulgu.fkreis.core.vanish.Vanish.init(this);
    this.registerListener(new de.ulgu.fkreis.core.vanish.listener.TickListener(vanish));
    this.registerListener(new de.ulgu.fkreis.core.vanish.listener.RenderListener(vanish));

    this.logger().info("Freundeskreis (ULGU Software) geladen.");
  }

  @Override
  protected Class<FkreisConfig> configurationClass() {
    return FkreisConfig.class;
  }

  public static FkreisAddon get() {
    return instance;
  }

  public Store store() {
    return this.store;
  }

  public Net net() {
    return this.net;
  }

  public boolean on() {
    return this.configuration().enabled().get();
  }
}
