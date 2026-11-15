package de.ulgu.fkreis.core.data;

/** Steckbrief, so wie er zwischen Addon-Nutzern verschickt wird. */
public class ProfileData {

  public String name = "";
  public String age = "";
  public String birthday = "";
  public String country = "";
  /** Bundesland / Region – frei eingetippt. */
  public String state = "";
  public String mob = "none";
  public long received;

  /** Normalisiert "1.3", "01.03", "1.3." usw. zu "01.03." – ungueltige Eingaben (z. B. "01.0") werden leer. */
  public static String normalizeBirthday(String in) {
    if (in == null) {
      return "";
    }
    String s = in.trim().replace(',', '.').replace('/', '.').replace('-', '.');
    String[] parts = s.split("\\.");
    if (parts.length < 2) {
      return "";
    }
    try {
      int d = Integer.parseInt(parts[0].trim());
      int m = Integer.parseInt(parts[1].trim());
      if (d < 1 || d > 31 || m < 1 || m > 12 || parts[1].trim().isEmpty()) {
        return "";
      }
      return String.format("%02d.%02d.", d, m);
    } catch (NumberFormatException e) {
      return "";
    }
  }

  public boolean birthdayToday() {
    String b = normalizeBirthday(this.birthday);
    if (b.isEmpty()) {
      return false;
    }
    java.time.LocalDate now = java.time.LocalDate.now();
    return b.equals(String.format("%02d.%02d.", now.getDayOfMonth(), now.getMonthValue()));
  }
}
