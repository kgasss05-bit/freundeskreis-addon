package de.ulgu.fkreis.core.net;

import de.ulgu.fkreis.core.FkreisAddon;
import de.ulgu.fkreis.core.data.Photo;
import de.ulgu.fkreis.core.data.Store;
import de.ulgu.fkreis.core.ui.AlbumActivity;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Fotoalben anderer ansehen – ueber denselben Laby-Broadcast wie die Sticker.
 * Anfrage "ALQ" -> Besitzer schickt "ALC" (Anzahl) und danach jedes Foto einzeln als kleines JPEG ("ALP"),
 * hoechstens 24 Fotos, eins alle 0,6 s. Wer "Fotoalbum teilen" aus hat, antwortet mit "ALN".
 */
public final class AlbumShare {

  public static final int MAX_PHOTOS = 24;
  /** Breite der verschickten Bilder und Groesse eines Nachrichtenteils (Zeichen). */
  private static final int THUMB_WIDTH = 400, PART = 12000, MAX_PARTS = 8;

  public static final class RemotePhoto {
    public final Path file;
    public final String frame;
    public final String caption;

    RemotePhoto(Path file, String frame, String caption) {
      this.file = file;
      this.frame = frame;
      this.caption = caption;
    }
  }

  public static final class Remote {
    public volatile int total = -1;
    public volatile boolean denied;
    public final long started = System.currentTimeMillis();
    public final RemotePhoto[] photos = new RemotePhoto[MAX_PHOTOS];
    final String[][] parts = new String[MAX_PHOTOS][];

    public List<RemotePhoto> list() {
      List<RemotePhoto> l = new ArrayList<>();
      for (RemotePhoto p : this.photos) {
        if (p != null) {
          l.add(p);
        }
      }
      return l;
    }

    public int received() {
      return this.list().size();
    }
  }

  private final Map<UUID, Remote> remotes = new ConcurrentHashMap<>();
  private final ConcurrentLinkedQueue<String[]> outbox = new ConcurrentLinkedQueue<>(); // {to, type, payload}
  private final ExecutorService worker = Executors.newSingleThreadExecutor(r -> {
    Thread t = new Thread(r, "Freundeskreis-Album");
    t.setDaemon(true);
    return t;
  });
  private long lastSend;

  public Remote remote(UUID owner) {
    return this.remotes.get(owner);
  }

  /** Album von {@code owner} anfordern (setzt den Zwischenspeicher zurueck). */
  public void request(Net net, UUID owner) {
    this.remotes.put(owner, new Remote());
    net.sendBroadcast(owner, "ALQ", null);
  }

  /** Eingehende Anfrage: Fotos im Hintergrund verkleinern und in die Warteschlange legen. */
  void onRequest(UUID from) {
    FkreisAddon addon = FkreisAddon.get();
    if (!addon.configuration().album().share().get()) {
      this.outbox.add(new String[] {from.toString(), "ALN", null});
      return;
    }
    List<Photo> all = new ArrayList<>(addon.store().photos);
    int n = Math.min(MAX_PHOTOS, all.size());
    this.outbox.add(new String[] {from.toString(), "ALC", String.valueOf(n)});
    this.worker.submit(() -> {
      for (int i = 0; i < n; i++) {
        Photo p = all.get(all.size() - n + i);
        try {
          byte[] jpg = thumb(AlbumActivity.albumDir().resolve(p.file));
          if (jpg == null) {
            continue;
          }
          String cap = Base64.getUrlEncoder().withoutPadding().encodeToString(
              (p.caption == null ? "" : p.caption).getBytes(StandardCharsets.UTF_8));
          String img = Base64.getUrlEncoder().withoutPadding().encodeToString(jpg);
          String body = p.frame + "~" + cap + "~" + img;
          if (body.length() <= PART) {
            this.outbox.add(new String[] {from.toString(), "ALP", i + "~" + body});
          } else { // groessere Bilder in mehreren Teilen verschicken
            int parts = (body.length() + PART - 1) / PART;
            for (int k = 0; k < parts; k++) {
              String piece = body.substring(k * PART, Math.min(body.length(), (k + 1) * PART));
              this.outbox.add(new String[] {from.toString(), "ALX", i + "~" + k + "~" + parts + "~" + piece});
            }
          }
        } catch (Throwable t) {
          de.ulgu.fkreis.core.FkreisAddon.log().warn("Freundeskreis: " + t);
        }
      }
    });
  }

  private static byte[] thumb(Path file) throws Exception {
    java.awt.image.BufferedImage src = javax.imageio.ImageIO.read(file.toFile());
    if (src == null) {
      return null;
    }
    int w = Math.min(THUMB_WIDTH, src.getWidth());
    int h = Math.max(1, Math.round(w * src.getHeight() / (float) src.getWidth()));
    // schrittweise halbieren, damit das verkleinerte Bild nicht pixelig wird
    java.awt.image.BufferedImage cur = src;
    while (cur.getWidth() / 2 >= w) {
      cur = scaled(cur, cur.getWidth() / 2, Math.max(1, cur.getHeight() / 2));
    }
    java.awt.image.BufferedImage dst = scaled(cur, w, h);
    javax.imageio.ImageWriter writer = javax.imageio.ImageIO.getImageWritersByFormatName("jpg").next();
    javax.imageio.ImageWriteParam param = writer.getDefaultWriteParam();
    param.setCompressionMode(javax.imageio.ImageWriteParam.MODE_EXPLICIT);
    param.setCompressionQuality(0.72F);
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    try (javax.imageio.stream.ImageOutputStream ios = javax.imageio.ImageIO.createImageOutputStream(out)) {
      writer.setOutput(ios);
      writer.write(null, new javax.imageio.IIOImage(dst, null, null), param);
    }
    writer.dispose();
    return out.toByteArray();
  }

  private static java.awt.image.BufferedImage scaled(java.awt.image.BufferedImage src, int w, int h) {
    java.awt.image.BufferedImage dst = new java.awt.image.BufferedImage(w, h, java.awt.image.BufferedImage.TYPE_INT_RGB);
    java.awt.Graphics2D g = dst.createGraphics();
    g.setRenderingHint(java.awt.RenderingHints.KEY_INTERPOLATION, java.awt.RenderingHints.VALUE_INTERPOLATION_BILINEAR);
    g.drawImage(src, 0, 0, w, h, null);
    g.dispose();
    return dst;
  }

  /** Ein Bild, das in mehreren Teilen ankommt: sammeln und am Ende wie ein normales Foto behandeln. */
  void onPart(UUID from, String payload) {
    Remote r = this.remotes.get(from);
    if (r == null || payload == null) {
      return;
    }
    String[] p = payload.split("~", 4);
    if (p.length < 4) {
      return;
    }
    try {
      int idx = Integer.parseInt(p[0]), k = Integer.parseInt(p[1]), parts = Integer.parseInt(p[2]);
      if (idx < 0 || idx >= MAX_PHOTOS || parts < 1 || parts > MAX_PARTS || k < 0 || k >= parts || p[3].length() > PART) {
        return;
      }
      String[] got = r.parts[idx];
      if (got == null || got.length != parts) {
        got = new String[parts];
        r.parts[idx] = got;
      }
      got[k] = p[3];
      StringBuilder all = new StringBuilder();
      for (String piece : got) {
        if (piece == null) {
          return;
        }
        all.append(piece);
      }
      r.parts[idx] = null;
      this.onPhoto(from, idx + "~" + all);
    } catch (Throwable t) {
      de.ulgu.fkreis.core.FkreisAddon.log().warn("Freundeskreis: " + t);
    }
  }

  /** Aus dem Tick: hoechstens alle 0,6 s eine Nachricht verschicken (schont LabyConnect). */
  void tick(Net net) {
    long now = System.currentTimeMillis();
    if (now - this.lastSend < 600) {
      return;
    }
    String[] m = this.outbox.poll();
    if (m == null) {
      return;
    }
    this.lastSend = now;
    net.sendBroadcast(UUID.fromString(m[0]), m[1], m[2]);
  }

  void onCount(UUID from, String payload) {
    Remote r = this.remotes.get(from);
    if (r == null) {
      return;
    }
    try {
      r.total = Math.min(MAX_PHOTOS, Integer.parseInt(payload));
    } catch (Exception e) {
      r.total = 0;
    }
  }

  void onDenied(UUID from) {
    Remote r = this.remotes.get(from);
    if (r != null) {
      r.denied = true;
      r.total = 0;
    }
  }

  void onPhoto(UUID from, String payload) {
    Remote r = this.remotes.get(from);
    if (r == null || payload == null) {
      return;
    }
    String[] p = payload.split("~", 4);
    if (p.length < 4) {
      return;
    }
    try {
      int idx = Integer.parseInt(p[0]);
      if (idx < 0 || idx >= MAX_PHOTOS) {
        return;
      }
      String caption = new String(Base64.getUrlDecoder().decode(p[2]), StandardCharsets.UTF_8);
      byte[] img = Base64.getUrlDecoder().decode(p[3]);
      Path dir = Store.dir().resolve("album_cache").resolve(from.toString());
      Files.createDirectories(dir);
      Path f = dir.resolve(idx + "_" + System.currentTimeMillis() + ".jpg");
      Files.write(f, img);
      r.photos[idx] = new RemotePhoto(f, p[1], caption.length() > 60 ? caption.substring(0, 60) : caption);
    } catch (Throwable t) {
      de.ulgu.fkreis.core.FkreisAddon.log().warn("Freundeskreis: " + t);
    }
  }
}
