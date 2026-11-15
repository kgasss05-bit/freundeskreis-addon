package de.ulgu.fkreis.core.coins;

import de.ulgu.fkreis.core.FkreisAddon;
import de.ulgu.fkreis.core.net.Net;
import net.labymod.api.client.entity.player.Player;
import net.labymod.api.laby3d.renderer.snapshot.AbstractLabySnapshot;
import net.labymod.api.laby3d.renderer.snapshot.Extras;

/** Pro Spieler und Bild: welche Muenze (oder -1 = keine) und ob ein Geburtstags-Kuchen neben dem Namen steht. */
public class CoinSnapshot extends AbstractLabySnapshot {

  private final int coin;
  private final boolean cake;

  public CoinSnapshot(Player player, Extras extras, FkreisAddon addon) {
    super(extras);
    int c = -1;
    try {
      if (addon.on() && addon.configuration().coins().enabled().get()
          && !player.getUniqueId().equals(Net.self())) {
        long since = addon.net().friendSince(player.getUniqueId());
        if (since > 0) {
          c = Coins.indexFor(since);
        }
      }
    } catch (Throwable ignored) {
    }
    this.coin = c;
    boolean k = false;
    try {
      k = addon.on() && addon.configuration().calendar().cake().get()
          && addon.net().birthdayToday(player.getUniqueId());
    } catch (Throwable ignored) {
    }
    this.cake = k;
  }

  public boolean cake() {
    return this.cake;
  }

  public int coin() {
    return this.coin;
  }
}
