package de.ulgu.fkreis.core.net;

import com.google.gson.Gson;
import de.ulgu.fkreis.core.FkreisAddon;
import de.ulgu.fkreis.core.diary.Diary;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import net.labymod.api.Laby;

/**
 * Tagebuch teilen – freiwillig und nur per Code.
 *
 * Wer teilt, erzeugt einen Code. Wer den Code kennt, kann das Tagebuch lesen (nicht aendern), solange
 * beide mit dem Addon auf demselben Server online sind. Ablauf ueber denselben Laby-Broadcast wie das Fotoalbum:
 * Anfrage "DRQ" (enthaelt nur einen Pruefwert aus Code + eigener UUID, nie den Code selbst) ->
 * der Besitzer schickt das Tagebuch gepackt und mit dem Code verschluesselt in Stuecken "DRC" zurueck.
 * Ohne den Code kann also niemand mitlesen, auch wenn er die Nachrichten sieht.
 */
public final class DiaryShare {

  private static final Gson GSON = new Gson();
  private static final String ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
  private static final int CHUNK = 6000, MAX_CHUNKS = 40;

  /** Was beim Leser ankommt. */
  public static final class Packet {
    public String n;          // Name des Besitzers
    public int l;             // Lederfarbe
    public String t;          // Naht
    public List<String[]> e;  // Eintraege: {Datum, Uhrzeit, Text}
  }

  /** Eine laufende Anfrage beim Leser. */
  public static final class Remote {
    public final String code;
    public final long started = System.currentTimeMillis();
    public volatile UUID owner;
    public volatile int received, total = -1;
    public volatile boolean failed;
    public volatile Packet packet;
    private String[] chunks;

    Remote(String code) {
      this.code = code;
    }
  }

  private volatile Remote remote;
  private final ConcurrentLinkedQueue<String[]> outbox = new ConcurrentLinkedQueue<>(); // {to, type, payload}
  private final Map<UUID, Long> lastRequest = new ConcurrentHashMap<>();
  private long lastSend;

  /** Neuer Code wie "K7QM-2XPA-9RTD". */
  public static String newCode() {
    SecureRandom r = new SecureRandom();
    StringBuilder b = new StringBuilder();
    for (int i = 0; i < 12; i++) {
      if (i > 0 && i % 4 == 0) {
        b.append('-');
      }
      b.append(ALPHABET.charAt(r.nextInt(ALPHABET.length())));
    }
    return b.toString();
  }

  /** Code vereinheitlichen: Grossbuchstaben, ohne Striche und Leerzeichen. */
  public static String norm(String code) {
    return code == null ? "" : code.toUpperCase().replaceAll("[^A-Z0-9]", "");
  }

  private static byte[] sha(String s) throws Exception {
    return MessageDigest.getInstance("SHA-256").digest(s.getBytes(StandardCharsets.UTF_8));
  }

  /** Pruefwert fuer die Anfrage: haengt am Code UND an der UUID des Fragenden, ist also nicht wiederverwendbar. */
  static String proof(String code, UUID requester) throws Exception {
    byte[] h = sha("fkreis-diary-ask|" + norm(code) + "|" + requester);
    StringBuilder b = new StringBuilder();
    for (int i = 0; i < 16; i++) {
      b.append(String.format("%02x", h[i]));
    }
    return b.toString();
  }

  static String seal(String code, String json) throws Exception {
    ByteArrayOutputStream zipped = new ByteArrayOutputStream();
    try (GZIPOutputStream z = new GZIPOutputStream(zipped)) {
      z.write(json.getBytes(StandardCharsets.UTF_8));
    }
    byte[] iv = new byte[12];
    new SecureRandom().nextBytes(iv);
    Cipher c = Cipher.getInstance("AES/GCM/NoPadding");
    c.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(sha("fkreis-diary-key|" + norm(code)), "AES"), new GCMParameterSpec(128, iv));
    byte[] enc = c.doFinal(zipped.toByteArray());
    byte[] out = new byte[iv.length + enc.length];
    System.arraycopy(iv, 0, out, 0, iv.length);
    System.arraycopy(enc, 0, out, iv.length, enc.length);
    return Base64.getUrlEncoder().withoutPadding().encodeToString(out);
  }

  static String open(String code, String sealed) throws Exception {
    byte[] in = Base64.getUrlDecoder().decode(sealed);
    Cipher c = Cipher.getInstance("AES/GCM/NoPadding");
    c.init(Cipher.DECRYPT_MODE, new SecretKeySpec(sha("fkreis-diary-key|" + norm(code)), "AES"), new GCMParameterSpec(128, in, 0, 12));
    byte[] zipped = c.doFinal(in, 12, in.length - 12);
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    try (GZIPInputStream z = new GZIPInputStream(new ByteArrayInputStream(zipped))) {
      byte[] buf = new byte[4096];
      int n;
      while ((n = z.read(buf)) > 0) {
        out.write(buf, 0, n);
      }
    }
    return new String(out.toByteArray(), StandardCharsets.UTF_8);
  }

  // ------------------------------------------------------------------ Leser

  public Remote remote() {
    return this.remote;
  }

  /** Tagebuch mit diesem Code anfragen. Die Antwort kommt nur, wenn der Besitzer auf demselben Server online ist. */
  public Remote request(Net net, String code) {
    Remote r = new Remote(norm(code));
    this.remote = r;
    try {
      net.sendBroadcast(null, "DRQ", proof(code, Net.self()));
    } catch (Exception e) {
      r.failed = true;
    }
    return r;
  }

  void onChunk(UUID from, String payload) {
    Remote r = this.remote;
    if (r == null || r.packet != null || r.failed || payload == null) {
      return;
    }
    try {
      int tilde = payload.indexOf('~'), slash = payload.indexOf('/');
      int idx = Integer.parseInt(payload.substring(0, slash)), total = Integer.parseInt(payload.substring(slash + 1, tilde));
      if (total < 1 || total > MAX_CHUNKS || idx < 0 || idx >= total) {
        return;
      }
      if (r.owner == null) {
        r.owner = from;
        r.total = total;
        r.chunks = new String[total];
      }
      if (!from.equals(r.owner) || total != r.total) {
        return;
      }
      if (r.chunks[idx] == null) {
        r.chunks[idx] = payload.substring(tilde + 1);
        r.received++;
      }
      if (r.received == r.total) {
        Packet p = GSON.fromJson(open(r.code, String.join("", r.chunks)), Packet.class);
        if (p == null || p.e == null) {
          r.failed = true;
        } else {
          r.packet = p;
        }
      }
    } catch (Throwable t) {
      r.failed = true; // falscher Code oder beschaedigte Daten
    }
  }

  // ------------------------------------------------------------------ Besitzer

  void onRequest(UUID from, String payload) {
    try {
      Diary diary = Diary.get();
      String code = norm(diary.shareCode);
      if (code.isEmpty() || payload == null || !FkreisAddon.get().on()) {
        return;
      }
      long now = System.currentTimeMillis();
      Long last = this.lastRequest.get(from);
      if (last != null && now - last < 5000L) {
        return; // hoechstens ein Versuch alle 5 Sekunden pro Person
      }
      this.lastRequest.put(from, now);
      if (!proof(code, from).equals(payload)) {
        return; // falscher Code: keine Antwort
      }
      Packet p = new Packet();
      p.n = Laby.labyAPI().getName();
      p.l = diary.leatherColor();
      p.t = diary.trim;
      p.e = new ArrayList<>();
      for (Map.Entry<String, Diary.Entry> e : diary.entries.entrySet()) {
        if (e.getValue() != null && e.getValue().text != null && !e.getValue().text.trim().isEmpty()) {
          p.e.add(new String[] {e.getKey(), e.getValue().time == null ? "" : e.getValue().time, e.getValue().text});
        }
      }
      String sealed = seal(code, GSON.toJson(p));
      while (sealed.length() > CHUNK * MAX_CHUNKS && p.e.size() > 1) { // sehr dickes Buch: aelteste Eintraege weglassen
        p.e.remove(0);
        sealed = seal(code, GSON.toJson(p));
      }
      int total = Math.max(1, (sealed.length() + CHUNK - 1) / CHUNK);
      for (int i = 0; i < total; i++) {
        String part = sealed.substring(i * CHUNK, Math.min(sealed.length(), (i + 1) * CHUNK));
        this.outbox.add(new String[] {from.toString(), "DRC", i + "/" + total + "~" + part});
      }
    } catch (Throwable t) {
      FkreisAddon.log().warn("Tagebuch teilen: " + t);
    }
  }

  /** Aus dem Tick: hoechstens alle 0,6 s ein Stueck verschicken (schont LabyConnect). */
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
}
