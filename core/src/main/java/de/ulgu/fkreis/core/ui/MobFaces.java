package de.ulgu.fkreis.core.ui;

import de.ulgu.fkreis.core.gfx.G;
import java.util.HashMap;
import java.util.Map;
import net.labymod.api.client.gui.icon.Icon;
import net.labymod.api.client.resources.ResourceLocation;

/**
 * Echte Mob-Gesichter, ausgeschnitten aus den offiziellen Minecraft-Mob-Texturen.
 * Nur Mobs mit fester, seit Jahren gleicher Textur – alle anderen zeigen das Spawn-Ei.
 */
public final class MobFaces {

  private static final class Face {
    final String path;
    final int x, y, w, h, tw, th;

    Face(String path, int x, int y, int w, int h, int tw, int th) {
      this.path = path;
      this.x = x;
      this.y = y;
      this.w = w;
      this.h = h;
      this.tw = tw;
      this.th = th;
    }
  }

  private static final Map<String, Face> FACES = new HashMap<>();

  static {
    FACES.put("villager", new Face("textures/entity/villager/villager.png", 8, 8, 8, 10, 64, 64));
    FACES.put("zombie_villager", new Face("textures/entity/zombie_villager/zombie_villager.png", 8, 8, 8, 10, 64, 64));
    FACES.put("witch", new Face("textures/entity/witch.png", 8, 8, 8, 10, 64, 128));
    FACES.put("creeper", new Face("textures/entity/creeper/creeper.png", 8, 8, 8, 8, 64, 32));
    FACES.put("zombie", new Face("textures/entity/zombie/zombie.png", 8, 8, 8, 8, 64, 64));
    FACES.put("husk", new Face("textures/entity/zombie/husk.png", 8, 8, 8, 8, 64, 64));
    FACES.put("drowned", new Face("textures/entity/zombie/drowned.png", 8, 8, 8, 8, 64, 64));
    FACES.put("skeleton", new Face("textures/entity/skeleton/skeleton.png", 8, 8, 8, 8, 64, 32));
    FACES.put("wither_skeleton", new Face("textures/entity/skeleton/wither_skeleton.png", 8, 8, 8, 8, 64, 32));
    FACES.put("stray", new Face("textures/entity/skeleton/stray.png", 8, 8, 8, 8, 64, 32));
    FACES.put("enderman", new Face("textures/entity/enderman/enderman.png", 8, 8, 8, 8, 64, 32));
    FACES.put("blaze", new Face("textures/entity/blaze.png", 8, 8, 8, 8, 64, 32));
    FACES.put("iron_golem", new Face("textures/entity/iron_golem/iron_golem.png", 8, 8, 8, 10, 128, 128));
    FACES.put("piglin", new Face("textures/entity/piglin/piglin.png", 8, 8, 10, 8, 64, 64));
    FACES.put("pillager", new Face("textures/entity/illager/pillager.png", 8, 8, 8, 10, 64, 64));
    FACES.put("vindicator", new Face("textures/entity/illager/vindicator.png", 8, 8, 8, 10, 64, 64));
    FACES.put("evoker", new Face("textures/entity/illager/evoker.png", 8, 8, 8, 10, 64, 64));
    FACES.put("wandering_trader", new Face("textures/entity/wandering_trader.png", 8, 8, 8, 10, 64, 64));
  }

  private MobFaces() {
  }

  /** Gesicht als Icon oder null (dann Spawn-Ei verwenden). */
  public static Icon face(String id) {
    Face f = FACES.get(id);
    if (f == null) {
      return null;
    }
    return G.sprite(ResourceLocation.create("minecraft", f.path), f.x, f.y, f.w, f.h, f.tw, f.th);
  }

  /** Hoehe / Breite des Gesichts (Villager-Gesichter sind hoeher). */
  public static float ratio(String id) {
    Face f = FACES.get(id);
    return f == null ? 1F : f.h / (float) f.w;
  }
}
