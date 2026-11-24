package de.ulgu.fkreis.core.vanish.listener;

import de.ulgu.fkreis.api.VanisherState;
import de.ulgu.fkreis.core.vanish.Vanish;
import java.util.UUID;
import net.labymod.api.Laby;
import net.labymod.api.client.component.Component;
import net.labymod.api.client.entity.LivingEntity;
import net.labymod.api.client.entity.player.Player;
import net.labymod.api.client.network.NetworkPlayerInfo;
import net.labymod.api.event.Subscribe;
import net.labymod.api.event.client.render.PlayerNameTagRenderEvent;
import net.labymod.api.event.client.render.entity.LivingEntityScaleEvent;
import net.labymod.api.event.client.render.entity.layers.ItemInHandLayerRenderEvent;
import net.labymod.api.event.client.render.model.entity.player.PlayerModelRenderEvent;

/**
 * Vanish ohne Eingriff ins Spiel: Ausgeblendete Spieler werden ueber LabyMods eigene Ereignisse unsichtbar gemacht.
 * Dadurch laeuft es auf jeder Minecraft-Version, die LabyMod unterstuetzt.
 * Koerper, Umhang und Ruestung werden nicht gezeichnet, das Item in der Hand und das Namensschild auch nicht;
 * zusaetzlich wird die Figur auf (fast) null verkleinert, damit auch Anbauteile verschwinden.
 */
public class RenderListener {

  private static final float GONE = 0.0001F;

  private final Vanish vanish;

  public RenderListener(Vanish vanish) {
    this.vanish = vanish;
  }

  private boolean hidden(UUID uuid) {
    return uuid != null && VanisherState.isHidden(uuid) && !uuid.equals(Laby.labyAPI().getUniqueId());
  }

  @Subscribe
  public void onModel(PlayerModelRenderEvent event) {
    Player player = event.player();
    if (player != null && this.hidden(player.getUniqueId())) {
      event.setCancelled(true);
    }
  }

  @Subscribe
  public void onScale(LivingEntityScaleEvent event) {
    LivingEntity entity = event.entity();
    if (entity instanceof Player && this.hidden(entity.getUniqueId())) {
      event.setScale(GONE);
    }
  }

  @Subscribe
  public void onHeldItem(ItemInHandLayerRenderEvent event) {
    LivingEntity entity = event.livingEntity();
    if (entity instanceof Player && this.hidden(entity.getUniqueId())) {
      event.setCancelled(true);
    }
  }

  @Subscribe
  public void onNameTag(PlayerNameTagRenderEvent event) {
    NetworkPlayerInfo info = event.getPlayerInfo();
    if (info == null || info.profile() == null) {
      return;
    }
    UUID uuid = info.profile().getUniqueId();
    if (!this.hidden(uuid)) {
      return;
    }
    if (event.context() == PlayerNameTagRenderEvent.Context.TAB_LIST) {
      // Tab-Liste: Ausgeblendete bekommen einen Totenkopf vor den Namen
      String name = this.vanish.store().hidden.get(uuid.toString());
      event.setNameTag(Component.text("☠ " + (name == null ? "?" : name)));
    } else {
      event.setCancelled(true);
    }
  }
}
