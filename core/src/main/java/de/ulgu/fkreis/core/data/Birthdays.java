package de.ulgu.fkreis.core.data;

import de.ulgu.fkreis.core.net.Net;
import java.time.LocalDate;
import java.time.MonthDay;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Hilfen fuer Kalender und HUD: naechste Geburtstage, sortiert. */
public final class Birthdays {

  private Birthdays() {
  }

  public static final class Entry {
    public final UUID uuid;
    public final String name;
    public final int day;
    public final int month;
    public final long daysUntil;

    Entry(UUID uuid, String name, int day, int month, long daysUntil) {
      this.uuid = uuid;
      this.name = name;
      this.day = day;
      this.month = month;
      this.daysUntil = daysUntil;
    }

    public String date() {
      return String.format("%02d.%02d.", this.day, this.month);
    }
  }

  /** "TT.MM." -> {Tag, Monat} oder null. */
  public static int[] parse(String b) {
    String n = ProfileData.normalizeBirthday(b);
    if (n.isEmpty()) {
      return null;
    }
    return new int[] {Integer.parseInt(n.substring(0, 2)), Integer.parseInt(n.substring(3, 5))};
  }

  public static long daysUntil(int day, int month) {
    LocalDate today = LocalDate.now();
    try {
      MonthDay md = MonthDay.of(month, Math.min(day, java.time.Month.of(month).maxLength()));
      LocalDate next = md.atYear(today.getYear());
      if (next.isBefore(today)) {
        next = md.atYear(today.getYear() + 1);
      }
      return ChronoUnit.DAYS.between(today, next);
    } catch (Exception e) {
      return 999;
    }
  }

  public static List<Entry> upcoming(Net net) {
    List<Entry> list = new ArrayList<>();
    for (Map.Entry<UUID, String> e : net.knownBirthdays().entrySet()) {
      int[] dm = parse(e.getValue());
      if (dm == null) {
        continue;
      }
      list.add(new Entry(e.getKey(), net.nameOf(e.getKey()), dm[0], dm[1], daysUntil(dm[0], dm[1])));
    }
    list.sort((a, b) -> a.daysUntil != b.daysUntil ? Long.compare(a.daysUntil, b.daysUntil)
        : a.name.compareToIgnoreCase(b.name));
    return list;
  }
}
