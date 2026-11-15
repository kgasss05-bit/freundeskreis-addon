package de.ulgu.fkreis.core.data;

/** Ein gesammelter Sticker (Kopf des Schenkenden). */
public class Sticker {

  public String uuid;
  public String name;
  public long time;

  public Sticker() {
  }

  public Sticker(String uuid, String name, long time) {
    this.uuid = uuid;
    this.name = name;
    this.time = time;
  }
}
