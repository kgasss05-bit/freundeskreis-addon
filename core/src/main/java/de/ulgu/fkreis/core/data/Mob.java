package de.ulgu.fkreis.core.data;

import de.ulgu.fkreis.core.L;
import net.labymod.api.client.gui.icon.Icon;
import net.labymod.api.client.resources.ResourceLocation;

/** Ein Minecraft-Mob fuer die Mob-Identifizierung. Das Icon ist die OFFIZIELLE Spawn-Ei-Textur aus dem Spiel selbst. */
public final class Mob {

  private final String id;
  private final String de;
  private final String en;
  private Icon icon;

  public Mob(String id, String de, String en) {
    this.id = id;
    this.de = de;
    this.en = en;
  }

  public String id() {
    return this.id;
  }

  public String name() {
    if (this.id.equals("none")) {
      return L.t("mob.none");
    }
    return L.german() ? this.de : this.en;
  }

  public boolean matches(String query) {
    String q = query.toLowerCase();
    return this.de.toLowerCase().contains(q) || this.en.toLowerCase().contains(q) || this.id.contains(q);
  }

  /** Offizielle Minecraft-Textur: minecraft:textures/item/<mob>_spawn_egg.png (wird zur Laufzeit aus dem Spiel geladen). */
  public Icon icon() {
    if (this.id.equals("none")) {
      return null;
    }
    if (this.icon == null) {
      this.icon = Icon.texture(ResourceLocation.create("minecraft", "textures/item/" + this.id + "_spawn_egg.png"));
    }
    return this.icon;
  }
}
