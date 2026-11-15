package de.ulgu.fkreis.core.ui;

import de.ulgu.fkreis.core.gfx.G;
import net.labymod.api.client.render.matrix.Stack;

/** Aufgeschlagenes Buch (Sammelbuch- und Notizbuch-Optik) inkl. Umblaetter-Animation. */
public final class BookFrame {

  public static final int PAGE = 0xFFF1E6CA;
  public static final int GUTTER = 0xFFD8CCB0;

  public float x, y, w, h;
  public float pageY, pageH, leftX, rightX, pageW;
  private long flipStart = -1;
  private int flipDir;

  public void layout(float x, float y, float w, float h, float bottomBand) {
    this.x = x;
    this.y = y;
    this.w = w;
    this.h = h;
    this.pageY = y + 8;
    this.pageH = h - 8 - bottomBand;
    this.pageW = (w - 16) / 2F - 3;
    this.leftX = x + 8;
    this.rightX = x + w / 2F + 3;
  }

  public void flip(int dir) {
    this.flipStart = System.currentTimeMillis();
    this.flipDir = dir;
  }

  public boolean flipping() {
    return this.flipStart > 0 && System.currentTimeMillis() - this.flipStart < 320;
  }

  public void drawBase(Stack s, int cover) {
    // Schatten
    G.rect(s, this.x + 3, this.y + 4, this.w, this.h, 0x55000000);
    // Einband
    G.rect(s, this.x, this.y, this.w, this.h, cover);
    G.outline(s, this.x, this.y, this.w, this.h, 1, G.shade(cover, 0.6F));
    G.rect(s, this.x + 1, this.y + 1, this.w - 2, 1, G.shade(cover, 1.2F));
    // Seiten
    G.rect(s, this.leftX, this.pageY, this.pageW, this.pageH, PAGE);
    G.rect(s, this.rightX, this.pageY, this.pageW, this.pageH, PAGE);
    // Bund-Schatten zur Mitte
    G.rect(s, this.leftX + this.pageW - 10, this.pageY, 10, this.pageH, 0x18000000);
    G.rect(s, this.leftX + this.pageW - 5, this.pageY, 5, this.pageH, GUTTER);
    G.rect(s, this.rightX, this.pageY, 5, this.pageH, GUTTER);
    G.rect(s, this.rightX, this.pageY, 10, this.pageH, 0x18000000);
    // Ruecken
    G.rect(s, this.x + this.w / 2F - 2, this.y + 4, 4, this.h - 8, G.shade(cover, 0.45F));
    // Seitenkanten (gestapeltes Papier)
    G.rect(s, this.leftX, this.pageY + this.pageH, this.pageW, 1, 0xFFCFC2A6);
    G.rect(s, this.rightX, this.pageY + this.pageH, this.pageW, 1, 0xFFCFC2A6);
  }

  /** Umblaettern: eine Seite klappt ueber den Ruecken auf die andere Seite. */
  public void drawFlip(Stack s) {
    if (!this.flipping()) {
      return;
    }
    float p = (System.currentTimeMillis() - this.flipStart) / 320F;
    float spine = this.x + this.w / 2F;
    float shrink = (float) Math.abs(Math.cos(Math.PI * p));
    float pw = this.pageW * shrink;
    boolean firstHalf = p < 0.5F;
    boolean onRight = this.flipDir > 0 ? firstHalf : !firstHalf;
    float px = onRight ? spine + 3 : spine - 3 - pw;
    int shade = G.mix(PAGE, 0xFFB8AC90, 1F - shrink);
    G.rect(s, px, this.pageY - 2 * (1F - shrink), pw, this.pageH + 4 * (1F - shrink), shade);
    G.rect(s, onRight ? px + pw - 1 : px, this.pageY, 1, this.pageH, 0x30000000);
  }
}
