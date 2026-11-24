package de.ulgu.fkreis.core.net;

import de.ulgu.fkreis.core.FkreisAddon;
import de.ulgu.fkreis.core.data.Sticker;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * Sammelbuch eines anderen Spielers ansehen: Anfrage "SBQ", Antwort "SBL" (Liste) oder "SBN" (nicht freigegeben).
 * Laeuft wie alles andere nur ueber LabyConnect; uebertragen werden nur Kopf (UUID), Name und Datum der Sticker.
 */
public final class StickerShare {

  public enum State { LOADING, LOADED, PRIVATE, NO_REPLY }

  /** Das angefragte Sammelbuch einer Person. */
  public static final class Remote {
    public volatile State state = State.LOADING;
    public volatile List<Sticker> stickers = new ArrayList<>();
    final long askedAt = System.currentTimeMillis();
  }

  private static final int MAX_STICKERS = 150, MAX_PAYLOAD = 5500;
  private static final long TIMEOUT_MS = 6000L, ANSWER_GAP_MS = 3000L;

  private final Map<UUID, Remote> open = new HashMap<>();
  private final Map<UUID, Long> answered = new HashMap<>();

  /** Fragt das Sammelbuch von {@code owner} an. */
  public Remote request(Net net, UUID owner) {
    Remote r = new Remote();
    this.open.put(owner, r);
    net.sendBroadcast(owner, "SBQ", null);
    return r;
  }

  void tick() {
    long now = System.currentTimeMillis();
    for (Remote r : this.open.values()) {
      if (r.state == State.LOADING && now - r.askedAt > TIMEOUT_MS) {
        r.state = State.NO_REPLY;
      }
    }
  }

  /** Jemand moechte mein Sammelbuch sehen. */
  void onRequest(Net net, UUID from) {
    long now = System.currentTimeMillis();
    Long last = this.answered.get(from);
    if (last != null && now - last < ANSWER_GAP_MS) {
      return;
    }
    this.answered.put(from, now);
    FkreisAddon addon = FkreisAddon.get();
    if (!addon.configuration().stickerBook().share().get()) {
      net.sendBroadcast(from, "SBN", null);
      return;
    }
    List<Sticker> all = addon.store().stickers;
    int count = Math.min(MAX_STICKERS, all.size());
    String payload = pack(all, count);
    while (payload.length() > MAX_PAYLOAD && count > 1) {
      count = count * 3 / 4;
      payload = pack(all, count);
    }
    net.sendBroadcast(from, "SBL", payload);
  }

  void onList(UUID from, String payload) {
    Remote r = this.open.get(from);
    if (r == null) {
      return;
    }
    try {
      r.stickers = unpack(payload);
      r.state = State.LOADED;
    } catch (IOException | RuntimeException e) {
      r.state = State.NO_REPLY;
    }
  }

  void onDenied(UUID from) {
    Remote r = this.open.get(from);
    if (r != null) {
      r.state = State.PRIVATE;
    }
  }

  /** Die neuesten {@code count} Sticker, je Zeile "uuid|zeit|name". */
  static String pack(List<Sticker> all, int count) {
    StringBuilder sb = new StringBuilder();
    for (int i = Math.max(0, all.size() - count); i < all.size(); i++) {
      Sticker s = all.get(i);
      if (s == null || s.uuid == null) {
        continue;
      }
      String name = s.name == null ? "" : s.name.replace('\n', ' ').replace('|', ' ');
      sb.append(s.uuid.replace("-", "")).append('|').append(Long.toString(s.time, 36)).append('|').append(name).append('\n');
    }
    try {
      ByteArrayOutputStream bytes = new ByteArrayOutputStream();
      try (GZIPOutputStream gz = new GZIPOutputStream(bytes)) {
        gz.write(sb.toString().getBytes(StandardCharsets.UTF_8));
      }
      return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes.toByteArray());
    } catch (IOException e) {
      return "";
    }
  }

  static List<Sticker> unpack(String payload) throws IOException {
    List<Sticker> out = new ArrayList<>();
    if (payload == null || payload.isEmpty()) {
      return out;
    }
    byte[] raw = Base64.getUrlDecoder().decode(payload);
    ByteArrayOutputStream bytes = new ByteArrayOutputStream();
    try (GZIPInputStream gz = new GZIPInputStream(new ByteArrayInputStream(raw))) {
      byte[] buf = new byte[2048];
      int n;
      while ((n = gz.read(buf)) > 0) {
        bytes.write(buf, 0, n);
        if (bytes.size() > 64000) {
          throw new IOException("too large");
        }
      }
    }
    for (String line : new String(bytes.toByteArray(), StandardCharsets.UTF_8).split("\n")) {
      String[] p = line.split("\\|", 3);
      if (p.length < 3 || p[0].length() != 32 || out.size() >= MAX_STICKERS) {
        continue;
      }
      String h = p[0];
      String uuid = h.substring(0, 8) + "-" + h.substring(8, 12) + "-" + h.substring(12, 16) + "-" + h.substring(16, 20) + "-" + h.substring(20);
      UUID.fromString(uuid); // ungueltige Eintraege fliegen hier raus
      String name = p[2].length() > 16 ? p[2].substring(0, 16) : p[2];
      out.add(new Sticker(uuid, name, Long.parseLong(p[1], 36)));
    }
    return out;
  }
}
