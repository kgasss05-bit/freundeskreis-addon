package de.ulgu.fkreis.core.data;

/** Ein Foto im Fotoalbum: Kopie des Screenshots + Rahmen + kleiner Text. */
public class Photo {

  /** Dateiname im Album-Ordner (labymod-neo/configs/fkreis/album/). */
  public String file = "";
  public String frame = "GOLD";
  public String caption = "";
  public long added;

  public Photo() {
  }

  public Photo(String file, String frame, String caption) {
    this.file = file;
    this.frame = frame;
    this.caption = caption;
    this.added = System.currentTimeMillis();
  }
}
