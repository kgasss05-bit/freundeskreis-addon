package de.ulgu.fkreis.core.data;

/** Rahmenfarben fuers Fotoalbum: Grundfarbe, Licht- und Schattenkante. */
public enum Frame {
  GOLD(0xFFE9B115, 0xFFFFE08A, 0xFF9A6A08),
  SILBER(0xFFBFC4CC, 0xFFF2F4F8, 0xFF7A808A),
  HOLZ(0xFF8A5A2A, 0xFFB07A40, 0xFF5A3A1A),
  ROSA(0xFFE88AB0, 0xFFFFC8DC, 0xFFA04A70),
  SMARAGD(0xFF2AA860, 0xFF7AE8A8, 0xFF156A3A),
  SCHWARZ(0xFF2A2A2E, 0xFF5A5A60, 0xFF101012),
  WEISS(0xFFF2F2EE, 0xFFFFFFFF, 0xFFBDBDB4),
  HIMMEL(0xFF4A8AE8, 0xFF9AC8FF, 0xFF24508A);

  public final int base;
  public final int light;
  public final int dark;

  Frame(int base, int light, int dark) {
    this.base = base;
    this.light = light;
    this.dark = dark;
  }

  public static Frame parse(String s) {
    try {
      return valueOf(s);
    } catch (Exception e) {
      return GOLD;
    }
  }
}
