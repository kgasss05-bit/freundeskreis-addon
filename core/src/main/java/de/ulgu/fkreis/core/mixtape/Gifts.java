package de.ulgu.fkreis.core.mixtape;

import com.google.gson.JsonObject;
import de.ulgu.fkreis.core.FkreisAddon;
import de.ulgu.fkreis.core.L;
import de.ulgu.fkreis.core.gfx.G;
import de.ulgu.fkreis.core.net.Net;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Kassetten verschenken: Der Freund bekommt eine Anfrage und muss sie erst annehmen. */
public final class Gifts {

  /** Eine Kassette, die jemand schenken moechte und die noch auf Antwort wartet. */
  public static final class Offer {
    public final String fromName;
    public final Tapes.Tape tape;

    Offer(String fromName, Tapes.Tape tape) {
      this.fromName = fromName;
      this.tape = tape;
    }
  }

  private static final List<Offer> PENDING = new ArrayList<>();
  private static final Map<UUID, Long> LAST = new HashMap<>();

  private Gifts() {
  }

  public static Offer next() {
    return PENDING.isEmpty() ? null : PENDING.get(0);
  }

  public static void drop() {
    if (!PENDING.isEmpty()) {
      PENDING.remove(0);
    }
  }

  public static String pack(Tapes.Tape t) {
    JsonObject o = new JsonObject();
    o.addProperty("t", t.title);
    o.addProperty("l", t.link);
    o.addProperty("c", t.color);
    return Base64.getUrlEncoder().withoutPadding().encodeToString(o.toString().getBytes(StandardCharsets.UTF_8));
  }

  /** Eine Anfrage ist angekommen (Aufruf aus dem Netzwerk-Teil). */
  public static void onOffer(UUID from, String fromName, String payload) {
    FkreisAddon addon = FkreisAddon.get();
    if (payload == null || payload.length() > 800 || !addon.configuration().mixtape().acceptGifts().get()) {
      return;
    }
    long now = System.currentTimeMillis();
    Long last = LAST.get(from);
    if ((last != null && now - last < 10000L) || PENDING.size() >= 5) {
      return;
    }
    LAST.put(from, now);
    try {
      JsonObject o = new com.google.gson.Gson().fromJson(new String(Base64.getUrlDecoder().decode(payload), StandardCharsets.UTF_8), JsonObject.class);
      Tapes.Tape t = new Tapes.Tape();
      t.title = o.get("t").getAsString().replace('\n', ' ').trim();
      t.link = o.get("l").getAsString().trim();
      t.color = o.get("c").getAsInt() & 0xFFFFFF;
      String name = fromName == null ? "?" : fromName;
      t.from = name.length() > 16 ? name.substring(0, 16) : name;
      if (t.title.isEmpty() || t.title.length() > Tapes.MAX_TITLE || !Tapes.validLink(t.link)) {
        return;
      }
      PENDING.add(new Offer(t.from, t));
      Net.notify(L.t("mixtape.title"), L.t("mixtape.offerNote", t.from), G.head(from));
      G.sound("entity.experience_orb.pickup", 1.2F);
    } catch (RuntimeException e) {
      FkreisAddon.log().warn("Freundeskreis: Mixtape-Anfrage unlesbar: " + e);
    }
  }
}
