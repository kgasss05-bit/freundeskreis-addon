package de.ulgu.fkreis.core.vanish.ui;

import net.labymod.api.client.gui.mouse.MutableMouse;
import net.labymod.api.client.gui.screen.ScreenContext;
import net.labymod.api.client.gui.screen.activity.Activity;
import net.labymod.api.client.gui.screen.key.InputType;
import net.labymod.api.client.gui.screen.key.Key;
import net.labymod.api.client.gui.screen.key.MouseButton;
import net.labymod.api.client.render.matrix.Stack;

/**
 * Basis fuer alle selbst gezeichneten Bildschirme. Alles wird in draw() gezeichnet,
 * Maus/Tastatur kommen ueber die on...()-Methoden. So bleibt die LabyMod-Schnittstelle an EINER Stelle.
 */
public abstract class VActivity extends Activity {

  protected final long openedAt = System.currentTimeMillis();
  protected float x;
  protected float y;
  protected float w;
  protected float h;

  @Override
  public void render(ScreenContext context) {
    super.render(context);
    var b = this.document().bounds();
    this.x = b.getLeft();
    this.y = b.getTop();
    this.w = b.getWidth();
    this.h = b.getHeight();
    float mx = (float) context.mouse().getX();
    float my = (float) context.mouse().getY();
    de.ulgu.fkreis.core.vanish.gfx.Cv.begin(context, context.stack());
    try {
      this.draw(context.stack(), mx, my);
    } finally {
      de.ulgu.fkreis.core.vanish.gfx.Cv.end();
    }
  }

  protected abstract void draw(Stack s, float mx, float my);

  protected float age() {
    return (System.currentTimeMillis() - this.openedAt) / 1000F;
  }

  protected boolean onClick(float mx, float my, boolean left) {
    return false;
  }

  protected boolean onRelease(float mx, float my) {
    return false;
  }

  protected boolean onDrag(float mx, float my) {
    return false;
  }

  protected boolean onScroll(float mx, float my, float delta) {
    return false;
  }

  protected boolean onKey(Key key) {
    return false;
  }

  protected boolean onChar(char c) {
    return false;
  }

  @Override
  public boolean mouseClicked(MutableMouse mouse, MouseButton mouseButton) {
    if (this.onClick((float) mouse.getX(), (float) mouse.getY(), mouseButton == MouseButton.LEFT)) {
      return true;
    }
    return super.mouseClicked(mouse, mouseButton);
  }

  @Override
  public boolean mouseReleased(MutableMouse mouse, MouseButton mouseButton) {
    if (this.onRelease((float) mouse.getX(), (float) mouse.getY())) {
      return true;
    }
    return super.mouseReleased(mouse, mouseButton);
  }

  @Override
  public boolean mouseDragged(MutableMouse mouse, MouseButton button, double deltaX, double deltaY) {
    if (this.onDrag((float) mouse.getX(), (float) mouse.getY())) {
      return true;
    }
    return super.mouseDragged(mouse, button, deltaX, deltaY);
  }

  @Override
  public boolean mouseScrolled(MutableMouse mouse, double scrollDelta) {
    if (this.onScroll((float) mouse.getX(), (float) mouse.getY(), (float) scrollDelta)) {
      return true;
    }
    return super.mouseScrolled(mouse, scrollDelta);
  }

  @Override
  public boolean keyPressed(Key key, InputType type) {
    if (this.onKey(key)) {
      return true;
    }
    return super.keyPressed(key, type);
  }

  @Override
  public boolean charTyped(Key key, char character) {
    if (this.onChar(character)) {
      return true;
    }
    return super.charTyped(key, character);
  }

  /** Header wie im Screenshot: kleiner X-Knopf + Titel. Gibt true zurueck, wenn die Maus ueber dem X ist. */
  protected boolean header(Stack s, String title, float mx, float my) {
    float bx = this.x + 22, by = this.y + 14;
    boolean hover = de.ulgu.fkreis.core.vanish.gfx.G.in(mx, my, bx, by, 18, 18);
    de.ulgu.fkreis.core.vanish.gfx.G.rect(s, bx, by, 18, 18, hover ? 0xFF2E3446 : 0xFF1C2130);
    de.ulgu.fkreis.core.vanish.gfx.G.outline(s, bx, by, 18, 18, 1, 0xFF3A4152);
    de.ulgu.fkreis.core.vanish.gfx.G.rect(s, bx, by + 17, 18, 1, 0xFF0E1018);
    de.ulgu.fkreis.core.vanish.gfx.G.centered(s, "✕", bx + 9, by + 5, 0xFFE8E8E8, 1F, false);
    de.ulgu.fkreis.core.vanish.gfx.G.text(s, title, bx + 30, by + 4, 0xFFFFFFFF, 1.2F, false);
    return hover;
  }

  protected boolean headerHit(float mx, float my) {
    return de.ulgu.fkreis.core.vanish.gfx.G.in(mx, my, this.x + 22, this.y + 14, 18, 18);
  }
}
