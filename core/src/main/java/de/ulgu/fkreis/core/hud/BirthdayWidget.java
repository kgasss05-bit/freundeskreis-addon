package de.ulgu.fkreis.core.hud;

import de.ulgu.fkreis.core.FkreisAddon;
import de.ulgu.fkreis.core.L;
import de.ulgu.fkreis.core.data.Birthdays;
import java.util.List;
import net.labymod.api.client.gui.hud.hudwidget.text.TextHudWidget;
import net.labymod.api.client.gui.hud.hudwidget.text.TextHudWidgetConfig;
import net.labymod.api.client.gui.hud.hudwidget.text.TextLine;
import net.labymod.api.client.gui.hud.hudwidget.text.TextLine.State;

/** Schlichtes HUD-Widget: "Naechster Geburtstag: Melina – 19.11. (in 5 Tagen)". */
public class BirthdayWidget extends TextHudWidget<TextHudWidgetConfig> {

  private final FkreisAddon addon;
  private TextLine line;
  private int ticks;

  public BirthdayWidget(FkreisAddon addon) {
    super("fkreis_birthday");
    this.addon = addon;
  }

  @Override
  public void load(TextHudWidgetConfig config) {
    super.load(config);
    this.line = super.createLine(L.t("hud.label"), L.t("hud.none"));
  }

  @Override
  public void onTick(boolean isEditorContext) {
    if (this.line == null || (++this.ticks % 20 != 1 && !isEditorContext)) {
      return;
    }
    String value = null;
    try {
      if (this.addon.on()) {
        List<Birthdays.Entry> list = Birthdays.upcoming(this.addon.net());
        if (!list.isEmpty()) {
          Birthdays.Entry e = list.get(0);
          String when = e.daysUntil == 0 ? L.t("cal.today")
              : e.daysUntil == 1 ? L.t("cal.tomorrow") : L.t("cal.indays", e.daysUntil);
          value = e.name + " – " + e.date() + " (" + when + ")";
        }
      }
    } catch (Throwable ignored) {
    }
    if (value == null && isEditorContext) {
      value = L.t("hud.none");
    }
    this.line.updateAndFlush(value == null ? L.t("hud.none") : value);
    this.line.setState(value != null ? State.VISIBLE : State.HIDDEN);
  }
}
