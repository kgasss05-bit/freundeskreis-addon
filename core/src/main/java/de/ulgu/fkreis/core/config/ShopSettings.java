package de.ulgu.fkreis.core.config;

import de.ulgu.fkreis.core.loot.StickerGalleryActivity;
import de.ulgu.fkreis.core.loot.StickerShopActivity;
import de.ulgu.fkreis.core.ui.Screens;
import net.labymod.api.client.gui.screen.widget.widgets.input.ButtonWidget.ButtonSetting;
import net.labymod.api.configuration.loader.Config;
import net.labymod.api.configuration.settings.Setting;
import net.labymod.api.util.MethodOrder;

/** Unterseite "Sticker-Laden" (getrennt von der taeglichen Truhe). */
public class ShopSettings extends Config {

  @ButtonSetting
  public void open(Setting setting) {
    Screens.open(new StickerShopActivity());
  }

  /** Alle Sticker ansehen. */
  @MethodOrder(after = "open")
  @ButtonSetting
  public void gallery(Setting setting) {
    Screens.open(new StickerGalleryActivity());
  }
}
