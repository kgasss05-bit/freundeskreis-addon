package de.ulgu.fkreis.core.coins;

import java.util.Collections;
import java.util.List;
import net.labymod.api.client.component.Component;
import net.labymod.api.client.entity.player.tag.tags.ComponentNameTag;
import net.labymod.api.client.render.matrix.Stack;
import net.labymod.api.client.render.state.entity.EntitySnapshot;
import net.labymod.api.laby3d.render.queue.SubmissionCollector;
import net.labymod.api.laby3d.render.queue.submissions.IconSubmission.DisplayMode;
import org.jetbrains.annotations.NotNull;

/** Kleine Freundschafts-Muenze rechts neben dem Namen (nur bei LabyMod-Freunden, nie bei dir selbst). */
public class CoinNameTag extends ComponentNameTag {

  private static final float SIZE = 9F;
  private int coin = -1;

  @Override
  protected @NotNull List<Component> buildComponents(EntitySnapshot snapshot) {
    this.coin = -1;
    if (snapshot.isDiscrete() || snapshot.isInvisible() || !snapshot.has(CoinKeys.COIN)) {
      return super.buildComponents(snapshot);
    }
    int c = snapshot.get(CoinKeys.COIN).coin();
    if (c < 0) {
      return super.buildComponents(snapshot);
    }
    this.coin = c;
    // Platzhalter, damit LabyMod Platz fuer die Muenze reserviert
    return Collections.singletonList(Component.text("   "));
  }

  @Override
  public void render(Stack stack, SubmissionCollector submissionCollector, EntitySnapshot snapshot) {
    super.render(stack, submissionCollector, snapshot);
    if (this.coin < 0) {
      return;
    }
    submissionCollector.submitIcon(stack, Coins.icon(this.coin), DisplayMode.NORMAL, 1F, -0.5F, SIZE, SIZE, -1);
  }
}
