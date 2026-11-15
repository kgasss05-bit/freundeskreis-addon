package de.ulgu.fkreis.core.gfx;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import net.labymod.api.Laby;
import net.labymod.api.client.gui.icon.Icon;
import net.labymod.api.client.resources.CompletableResourceLocation;
import net.labymod.api.client.resources.ResourceLocation;
import net.labymod.api.client.resources.texture.GameImage;
import net.labymod.api.client.resources.texture.SimpleTexture;
import net.labymod.api.client.resources.texture.TextureRepository;

/**
 * Laedt Bilder von der Festplatte (Screenshots / Album) als Textur.
 * Verkleinern passiert im Hintergrund, das Hochladen auf die Grafikkarte im Render-Thread.
 * Hochladen ueber den offiziellen Weg: GameImageProvider -> SimpleTexture -> TextureRepository.
 */
public final class PhotoTex {

  public static final class Tex {
    public final Icon icon;
    public final float ratio; // Breite / Hoehe

    Tex(Icon icon, float ratio) {
      this.icon = icon;
      this.ratio = ratio;
    }
  }

  private static final int MAX = 48;
  private static final Map<String, Tex> CACHE = new LinkedHashMap<String, Tex>(64, 0.75F, true) {
    @Override
    protected boolean removeEldestEntry(Map.Entry<String, Tex> eldest) {
      return this.size() > MAX;
    }
  };
  private static final Set<String> LOADING = ConcurrentHashMap.newKeySet();
  private static final ConcurrentLinkedQueue<Runnable> UPLOADS = new ConcurrentLinkedQueue<>();
  private static final ExecutorService EXEC = Executors.newSingleThreadExecutor(r -> {
    Thread t = new Thread(r, "Freundeskreis-Fotos");
    t.setDaemon(true);
    return t;
  });
  private static int counter;

  private PhotoTex() {
  }

  /** Bild holen (null = laedt noch). Im Render-Thread aufrufen. */
  public static Tex get(Path file, int maxWidth) {
    pump();
    String key = file.toAbsolutePath() + "@" + maxWidth;
    Tex t = CACHE.get(key);
    if (t != null) {
      return t;
    }
    if (LOADING.add(key)) {
      EXEC.submit(() -> decode(key, file, maxWidth));
    }
    return null;
  }

  private static void decode(String key, Path file, int maxWidth) {
    java.awt.image.BufferedImage dst = null;
    float ratio = 16F / 9F;
    try {
      java.awt.image.BufferedImage src = javax.imageio.ImageIO.read(file.toFile());
      if (src != null) {
        ratio = src.getWidth() / (float) Math.max(1, src.getHeight());
        int w = Math.min(maxWidth, src.getWidth());
        int h = Math.max(1, Math.round(w / ratio));
        dst = new java.awt.image.BufferedImage(w, h, java.awt.image.BufferedImage.TYPE_INT_ARGB);
        java.awt.Graphics2D g = dst.createGraphics();
        g.setRenderingHint(java.awt.RenderingHints.KEY_INTERPOLATION, java.awt.RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.drawImage(src, 0, 0, w, h, null);
        g.dispose();
      }
    } catch (Exception e) {
      de.ulgu.fkreis.core.FkreisAddon.log().warn("Foto konnte nicht gelesen werden: " + file.getFileName());
    }
    if (dst == null) {
      LOADING.remove(key);
      return;
    }
    final java.awt.image.BufferedImage img = dst;
    final float r = ratio;
    UPLOADS.add(() -> {
      Icon icon = upload(img);
      synchronized (CACHE) {
        CACHE.put(key, new Tex(icon, r));
      }
      LOADING.remove(key);
    });
  }

  /** Hoechstens 2 Bilder pro Bild hochladen, damit nichts ruckelt. */
  public static void pump() {
    for (int i = 0; i < 2; i++) {
      Runnable r = UPLOADS.poll();
      if (r == null) {
        return;
      }
      try {
        r.run();
      } catch (RuntimeException e) {
        de.ulgu.fkreis.core.FkreisAddon.log().warn("Foto-Upload fehlgeschlagen: " + e);
      }
    }
  }

  private static final ResourceLocation PLACEHOLDER = ResourceLocation.create("fkreis", "textures/ui/paper.png");

  /** Offizieller Weg (wie im Spotify-Addon): GameImage -> SimpleTexture -> registrieren. */
  private static Icon upload(java.awt.image.BufferedImage img) {
    ResourceLocation loc = ResourceLocation.create("fkreis", "album/photo_" + (counter++));
    GameImage image = Laby.references().gameImageProvider().getImage(img);
    SimpleTexture texture = SimpleTexture.simple(loc, image);
    CompletableResourceLocation done = new CompletableResourceLocation(PLACEHOLDER);
    texture.bindTo(new TextureRepository.TextureRegistrationCallback() {
      @Override
      public void onBeforeTextureRegistration() {
        texture.upload();
      }

      @Override
      public void onAfterTextureRegistration() {
        done.executeCompletableListeners(loc);
      }
    });
    return Icon.completable(done);
  }
}
