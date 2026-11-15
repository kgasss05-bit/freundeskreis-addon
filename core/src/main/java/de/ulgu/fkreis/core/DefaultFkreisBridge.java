package de.ulgu.fkreis.core;

import de.ulgu.fkreis.api.FkreisBridge;
import de.ulgu.fkreis.api.FkreisInfo;
import javax.inject.Singleton;
import net.labymod.api.models.Implements;

@Singleton
@Implements(FkreisBridge.class)
public class DefaultFkreisBridge implements FkreisBridge {

  @Override
  public String brand() {
    return FkreisInfo.BRAND;
  }
}
