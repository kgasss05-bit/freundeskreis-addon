package de.ulgu.fkreis.core.menu;

import de.ulgu.fkreis.core.FkreisAddon;
import de.ulgu.fkreis.core.L;
import de.ulgu.fkreis.core.ui.RingActivity;
import de.ulgu.fkreis.core.ui.Screens;
import net.labymod.api.client.component.Component;
import net.labymod.api.client.entity.player.Player;
import net.labymod.api.client.entity.player.interaction.BulletPoint;
import net.labymod.api.client.gui.icon.Icon;

/** EIN Eintrag im LabyMod-Mausrad: "Freundeskreis" – oeffnet das eigene Freundeskreis-Rad. */
public class FreundeskreisBullet implements BulletPoint {

  private final FkreisAddon addon;

  public FreundeskreisBullet(FkreisAddon addon) {
    this.addon = addon;
  }

  @Override
  public Component getTitle() {
    return Component.text(L.t("wheel.title"));
  }

  @Override
  public Icon getIcon() {
    return null;
  }

  @Override
  public void execute(Player player) {
    Screens.open(new RingActivity(player.getUniqueId(), player.getName()));
  }

  @Override
  public boolean isVisible(Player player) {
    return this.addon.on();
  }
}
