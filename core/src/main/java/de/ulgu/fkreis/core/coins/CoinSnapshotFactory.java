package de.ulgu.fkreis.core.coins;

import de.ulgu.fkreis.core.FkreisAddon;
import net.labymod.api.client.entity.player.Player;
import net.labymod.api.laby3d.renderer.snapshot.Extras;
import net.labymod.api.laby3d.renderer.snapshot.LabySnapshotFactory;
import net.labymod.api.service.annotation.AutoService;

@AutoService(LabySnapshotFactory.class)
public class CoinSnapshotFactory extends LabySnapshotFactory<Player, CoinSnapshot> {

  private final FkreisAddon addon;

  public CoinSnapshotFactory(FkreisAddon addon) {
    super(CoinKeys.COIN);
    this.addon = addon;
  }

  @Override
  public CoinSnapshot create(Player player, Extras extras) {
    return new CoinSnapshot(player, extras, this.addon);
  }
}
