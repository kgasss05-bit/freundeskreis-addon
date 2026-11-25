package de.ulgu.fkreis.core.net;

import de.ulgu.fkreis.core.FkreisAddon;
import de.ulgu.fkreis.core.match.MatchData;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Freunde-Match ueber LabyConnect: "FMQ" fragt alle Addon-Spieler auf dem Server, "FMA" ist die Antwort
 * (die 100 Antworten als Ziffern). Geantwortet wird nur, wenn man selbst mitmacht und den Fragebogen fertig hat.
 * Angezeigt wird beim Suchenden nur die Uebereinstimmung in Prozent, nie die einzelnen Antworten.
 */
public final class MatchShare {

  public static final class Hit {
    public final UUID uuid;
    public final String name;
    public final int percent;

    Hit(UUID uuid, String name, int percent) {
      this.uuid = uuid;
      this.name = name;
      this.percent = percent;
    }
  }

  private static final long WINDOW_MS = 20000L, ANSWER_GAP_MS = 3000L;

  private final List<Hit> hits = new ArrayList<>();
  private final Map<UUID, Long> answered = new HashMap<>();
  private long searchedAt;
  private UUID compareWith;
  private int comparePercent = -1;
  private long comparedAt;

  /** Sucht nach Spielern auf diesem Server. Gibt false zurueck, wenn nichts gesendet werden konnte. */
  public boolean search(Net net) {
    this.hits.clear();
    this.searchedAt = System.currentTimeMillis();
    return net.sendToAll("FMQ");
  }

  /** Fragt gezielt einen Spieler, um die Uebereinstimmung mit ihm zu berechnen. */
  public void compare(Net net, UUID other) {
    this.compareWith = other;
    this.comparePercent = -1;
    this.comparedAt = System.currentTimeMillis();
    net.sendBroadcast(other, "FMQ", null);
  }

  /** Ergebnis des Vergleichs mit diesem Spieler in Prozent, oder -1 solange (noch) keine Antwort da ist. */
  public int compared(UUID other) {
    return other.equals(this.compareWith) ? this.comparePercent : -1;
  }

  public List<Hit> hits() {
    return this.hits;
  }

  public long searchedAt() {
    return this.searchedAt;
  }

  void onRequest(Net net, UUID from) {
    FkreisAddon addon = FkreisAddon.get();
    MatchData mine = MatchData.get();
    if (!addon.configuration().match().participate().get() || !mine.submitted) {
      return;
    }
    long now = System.currentTimeMillis();
    Long last = this.answered.get(from);
    if (last != null && now - last < ANSWER_GAP_MS) {
      return;
    }
    this.answered.put(from, now);
    net.sendBroadcast(from, "FMA", mine.pack());
  }

  void onAnswer(UUID from, String fromName, String payload) {
    MatchData mine = MatchData.get();
    int[] other = MatchData.unpack(payload);
    if (other == null || !mine.submitted) {
      return;
    }
    if (from.equals(this.compareWith) && System.currentTimeMillis() - this.comparedAt < WINDOW_MS) {
      this.comparePercent = MatchData.percent(mine.answers, other);
    }
    if (System.currentTimeMillis() - this.searchedAt > WINDOW_MS) {
      return;
    }
    for (int i = 0; i < this.hits.size(); i++) {
      if (this.hits.get(i).uuid.equals(from)) {
        this.hits.remove(i);
        break;
      }
    }
    Hit hit = new Hit(from, fromName == null ? "?" : fromName, MatchData.percent(mine.answers, other));
    int at = 0;
    while (at < this.hits.size() && this.hits.get(at).percent >= hit.percent) {
      at++;
    }
    this.hits.add(at, hit);
    if (this.hits.size() > 30) {
      this.hits.remove(this.hits.size() - 1);
    }
  }
}
