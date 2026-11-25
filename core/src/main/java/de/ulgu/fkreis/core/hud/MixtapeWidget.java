package de.ulgu.fkreis.core.hud;

import de.ulgu.fkreis.core.FkreisAddon;
import de.ulgu.fkreis.core.gfx.Cv;
import de.ulgu.fkreis.core.mixtape.Player;
import de.ulgu.fkreis.core.mixtape.Tapes;
import net.labymod.api.client.gui.hud.hudwidget.HudWidgetConfig;
import net.labymod.api.client.gui.hud.hudwidget.SimpleHudWidget;
import net.labymod.api.client.gui.hud.position.HudSize;
import net.labymod.api.client.gui.screen.ScreenContext;

/**
 * HUD-Widget "Kassettenrekorder": der kleine schwebende Rekorder, solange eine Kassette laeuft.
 * Platz und Groesse stellt man wie bei jedem Widget im HUD-Editor von LabyMod ein.
 */
public class MixtapeWidget extends SimpleHudWidget<HudWidgetConfig> {

  private static final float W = 75, H = 56;
  private final Tapes.Tape demo = new Tapes.Tape();

  public MixtapeWidget() {
    super("fkreis_mixtape", HudWidgetConfig.class);
    this.demo.title = "Mixtape";
    this.demo.color = Tapes.COLORS[4];
  }

  @Override
  public void render(RenderPhase phase, ScreenContext context, boolean isEditorContext, HudSize size) {
    size.set(W, H);
    if (!phase.canRender()) {
      return;
    }
    Tapes.Tape tape = Player.current();
    if (tape == null && isEditorContext) {
      tape = this.demo; // im Editor immer etwas zeigen, damit man es platzieren kann
    }
    if (tape == null) {
      return;
    }
    Cv.begin(context, context.stack());
    try {
      Player.drawRecorder(context.stack(), 0, 0, W / 150F, tape, tape == this.demo ? 0 : Player.since(), false);
    } finally {
      Cv.end();
    }
  }

  @Override
  public boolean isVisibleInGame() {
    return FkreisAddon.get().on() && Player.current() != null;
  }
}
