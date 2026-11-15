package de.ulgu.fkreis.core.coins;

import de.ulgu.fkreis.core.gfx.G;
import java.util.Collections;
import java.util.List;
import net.labymod.api.client.component.Component;
import net.labymod.api.client.entity.player.tag.tags.ComponentNameTag;
import net.labymod.api.client.gui.icon.Icon;
import net.labymod.api.client.render.matrix.Stack;
import net.labymod.api.client.render.state.entity.EntitySnapshot;
import net.labymod.api.laby3d.render.queue.SubmissionCollector;
import net.labymod.api.laby3d.render.queue.submissions.IconSubmission.DisplayMode;
import org.jetbrains.annotations.NotNull;

/** Kleiner Geburtstags-Kuchen rechts neben dem Namen – nur an dem Tag, an dem die Person Geburtstag hat. */
public class CakeNameTag extends ComponentNameTag {

  private static final float SIZE = 9F;
  private static Icon cakeIcon;
  private boolean show;

  private static Icon cake() {
    if (cakeIcon == null) {
      cakeIcon = G.tex("ui/cake.png");
    }
    return cakeIcon;
  }

  @Override
  protected @NotNull List<Component> buildComponents(EntitySnapshot snapshot) {
    this.show = false;
    if (snapshot.isDiscrete() || snapshot.isInvisible() || !snapshot.has(CoinKeys.COIN)) {
      return super.buildComponents(snapshot);
    }
    if (!snapshot.get(CoinKeys.COIN).cake()) {
      return super.buildComponents(snapshot);
    }
    this.show = true;
    return Collections.singletonList(Component.text("   "));
  }

  @Override
  public void render(Stack stack, SubmissionCollector submissionCollector, EntitySnapshot snapshot) {
    super.render(stack, submissionCollector, snapshot);
    if (!this.show) {
      return;
    }
    float bob = (float) Math.sin(System.currentTimeMillis() / 300.0) * 0.4F;
    submissionCollector.submitIcon(stack, cake(), DisplayMode.NORMAL, 1F, -0.8F + bob, SIZE, SIZE, -1);
  }
}
