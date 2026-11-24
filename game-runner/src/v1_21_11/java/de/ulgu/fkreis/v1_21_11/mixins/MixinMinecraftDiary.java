package de.ulgu.fkreis.v1_21_11.mixins;

import de.ulgu.fkreis.api.DiaryState;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Tagebuch: Beim Beenden des Spiels (X am Fenster, Alt+F4, "Spiel beenden") wird einmal nachgefragt,
 * ob man vorher noch einen Eintrag schreiben moechte. Man wird nie festgehalten:
 * ein zweiter Versuch zu schliessen beendet das Spiel sofort.
 */
@Mixin(Minecraft.class)
public class MixinMinecraftDiary {

  @Inject(method = "stop", at = @At("HEAD"), cancellable = true)
  private void fkreis$askDiary(CallbackInfo ci) {
    if (DiaryState.allow) {
      return;
    }
    long now = System.currentTimeMillis();
    if (DiaryState.busy) {
      if (now - DiaryState.askedAt > 800L) {
        DiaryState.allow = true; // zweiter Versuch: sofort beenden
        return;
      }
      fkreis$keepOpen();
      ci.cancel();
      return;
    }
    boolean ask = false;
    try {
      ask = DiaryState.shouldAsk != null && DiaryState.openAsk != null && DiaryState.shouldAsk.getAsBoolean();
    } catch (Throwable ignored) {
    }
    if (!ask) {
      return;
    }
    DiaryState.busy = true;
    DiaryState.askedAt = now;
    try {
      DiaryState.openAsk.run();
    } catch (Throwable t) {
      DiaryState.busy = false;
      return;
    }
    fkreis$keepOpen();
    ci.cancel();
  }

  /** Nimmt den Schliessen-Wunsch des Fensters zurueck, damit das Spiel weiterlaeuft. */
  private static void fkreis$keepOpen() {
    try {
      long window = GLFW.glfwGetCurrentContext();
      if (window != 0L) {
        GLFW.glfwSetWindowShouldClose(window, false);
      }
    } catch (Throwable ignored) {
    }
  }

  @Inject(method = "tick", at = @At("HEAD"))
  private void fkreis$diaryTick(CallbackInfo ci) {
    DiaryState.hookActive = true;
    if (DiaryState.quitNow) {
      DiaryState.quitNow = false;
      DiaryState.allow = true;
      ((Minecraft) (Object) this).stop();
      return;
    }
    // Frage wurde weggeklickt (ESC): Beenden abbrechen, beim naechsten Mal wieder fragen
    if (DiaryState.busy && DiaryState.screens <= 0 && System.currentTimeMillis() - DiaryState.askedAt > 2500L) {
      DiaryState.busy = false;
    }
  }
}
