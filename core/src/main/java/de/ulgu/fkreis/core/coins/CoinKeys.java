package de.ulgu.fkreis.core.coins;

import net.labymod.api.laby3d.renderer.snapshot.ExtraKey;

public final class CoinKeys {

  public static final ExtraKey<CoinSnapshot> COIN = ExtraKey.of("fkreis_coin", CoinSnapshot.class);

  private CoinKeys() {
  }
}
