package de.ulgu.fkreis.core.net;

import com.google.gson.Gson;
import de.ulgu.fkreis.core.FkreisAddon;
import de.ulgu.fkreis.core.L;
import de.ulgu.fkreis.core.config.ProfileSettings;
import de.ulgu.fkreis.core.data.ProfileData;
import de.ulgu.fkreis.core.data.Sticker;
import de.ulgu.fkreis.core.data.Store;
import de.ulgu.fkreis.core.gfx.G;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;
import net.labymod.api.Laby;
import net.labymod.api.client.component.Component;
import net.labymod.api.event.Phase;
import net.labymod.api.event.Subscribe;
import net.labymod.api.event.client.lifecycle.GameTickEvent;
import net.labymod.api.event.labymod.labyconnect.session.LabyConnectBroadcastEvent;
import net.labymod.api.labyconnect.LabyConnectSession;
import net.labymod.api.labyconnect.protocol.model.friend.Friend;
import net.labymod.api.notification.Notification;

/**
 * Datenaustausch zwischen Addon-Nutzern ohne eigenen Server: ueber den LabyMod-Freundes-Chat (LabyConnect).
 * Jede Nachricht ist fuer Menschen lesbar ("[Freundeskreis] ...") und traegt am Ende einen kleinen Code {{FK2:...}}.
 */
public class Net {

  public enum Result { LOADING, LOADED, NONE, NOT_FRIEND, TIMEOUT }

  public interface ProfileListener {
    void onProfile(UUID uuid, Result result, ProfileData data);
  }

  private static final Gson GSON = new Gson();
  private static final long TIMEOUT_MS = 6000;

  private final FkreisAddon addon;
  private final ConcurrentLinkedQueue<Runnable> mainThread = new ConcurrentLinkedQueue<>();
  private final Map<UUID, Long> pending = new HashMap<>();
  private final List<ProfileListener> listeners = new ArrayList<>();
  private int ticks;

  public Net(FkreisAddon addon) {
    this.addon = addon;
  }

  // ------------------------------------------------------------------ Freunde

  public static final class FriendInfo {
    public final UUID uuid;
    public final String name;
    public final boolean online;

    FriendInfo(UUID uuid, String name, boolean online) {
      this.uuid = uuid;
      this.name = name;
      this.online = online;
    }
  }

  private LabyConnectSession session() {
    try {
      return Laby.labyAPI().labyConnect().getSession();
    } catch (Throwable t) {
      return null;
    }
  }

  public List<FriendInfo> friends() {
    List<FriendInfo> list = new ArrayList<>();
    LabyConnectSession session = this.session();
    if (session == null) {
      return list;
    }
    for (Friend friend : session.getFriends()) {
      list.add(new FriendInfo(friend.getUniqueId(), friend.getName(), friend.isOnline()));
    }
    list.sort((a, b) -> a.name.compareToIgnoreCase(b.name));
    return list;
  }

  private Friend friend(UUID uuid) {
    LabyConnectSession session = this.session();
    if (session == null) {
      return null;
    }
    for (Friend friend : session.getFriends()) {
      if (friend.getUniqueId().equals(uuid)) {
        return friend;
      }
    }
    return null;
  }

  public boolean isFriend(UUID uuid) {
    return this.friend(uuid) != null;
  }

  // ------------------------------------------------------------------ Freundschaftsdauer (Muenzen)

  private final java.util.Map<UUID, Long> sinceCache = new java.util.concurrent.ConcurrentHashMap<>();

  /**
   * Seit wann ist man mit dieser Person befreundet? 0 = kein Freund.
   * Es zaehlt der Tag, an dem das Addon die Freundschaft zum ersten Mal gesehen hat.
   */
  public long friendSince(UUID uuid) {
    Long v = this.sinceCache.get(uuid);
    return v == null ? 0 : v;
  }

  private void refreshFriendSince() {
    LabyConnectSession session = this.session();
    if (session == null) {
      return;
    }
    Store store = this.addon.store();
    boolean changed = false;
    java.util.Set<UUID> seen = new java.util.HashSet<>();
    for (Friend friend : session.getFriends()) {
      UUID id = friend.getUniqueId();
      seen.add(id);
      long since = 0;
      {
        Long first = store.friendSince.get(id.toString());
        if (first == null) {
          first = System.currentTimeMillis();
          store.friendSince.put(id.toString(), first);
          changed = true;
        }
        since = first;
      }
      this.sinceCache.put(id, since);
    }
    this.sinceCache.keySet().retainAll(seen);
    if (changed) {
      store.save();
    }
  }

  // ------------------------------------------------------------------ Steckbrief

  public static UUID self() {
    return Laby.labyAPI().getUniqueId();
  }

  public ProfileData ownProfile() {
    ProfileSettings p = this.addon.configuration().profile();
    ProfileData d = new ProfileData();
    d.name = p.nickname().get().trim();
    d.age = p.age().get().trim();
    d.birthday = this.addon.store().birthdayCommitted;
    d.country = p.country().get().trim();
    d.state = p.state().get().trim();
    d.mob = p.mob().get();
    return d;
  }

  public void addListener(ProfileListener l) {
    this.listeners.add(l);
  }

  public void removeListener(ProfileListener l) {
    this.listeners.remove(l);
  }

  private void fire(UUID uuid, Result result, ProfileData data) {
    for (ProfileListener l : new ArrayList<>(this.listeners)) {
      l.onProfile(uuid, result, data);
    }
  }

  /** Fragt den Steckbrief an. Ergebnis kommt ueber die Listener (Timeout nach 6 s statt ewig "wird geladen"). */
  public void requestProfile(UUID uuid) {
    if (uuid.equals(self())) {
      ProfileData own = this.ownProfile();
      this.fire(uuid, Result.LOADED, own);
      return;
    }
    // 1. Laby-Broadcast (jeder Addon-Nutzer auf demselben Server, keine Freundschaft noetig)
    boolean sent = this.broadcast(uuid, "REQ", null);
    // 2. Freunde auf anderen Servern: spaeter ueber den Laby-Chat nachfragen (siehe tick)
    if (sent || this.isFriend(uuid)) {
      this.pending.put(uuid, System.currentTimeMillis());
    } else {
      this.fire(uuid, Result.NOT_FRIEND, this.addon.store().profiles.get(uuid.toString()));
    }
  }

  // ------------------------------------------------------------------ Sticker

  public enum GiveResult { OK, ALREADY, NOT_FRIEND, FAILED }

  /**
   * Sticker schenken – an JEDEN Addon-Nutzer auf demselben Server, ganz ohne Freundschaft und ohne
   * Chat-Nachricht: ueber LabyConnect-Broadcasts (der Weg, den auch die alte Freundeskreis-Version nutzt).
   */
  public GiveResult giveSticker(UUID uuid, String name) {
    Store store = this.addon.store();
    if (store.given.contains(uuid.toString())) {
      return GiveResult.ALREADY;
    }
    if (!this.broadcast(uuid, "STK", null)) {
      return GiveResult.FAILED;
    }
    store.given.add(uuid.toString());
    store.save();
    return GiveResult.OK;
  }

  // ------------------------------------------------------------------ Laby-Broadcast

  private static final String KEY = "fkreis";
  private final AlbumShare album = new AlbumShare();

  public AlbumShare album() {
    return this.album;
  }

  private final MatchShare matchShare = new MatchShare();

  public MatchShare matchShare() {
    return this.matchShare;
  }

  /** Schickt eine Frage ohne Inhalt an alle Addon-Spieler auf dem Server. */
  boolean sendToAll(String type) {
    return this.broadcast(null, type, null);
  }

  private final StickerShare stickerShare = new StickerShare();

  public StickerShare stickerShare() {
    return this.stickerShare;
  }

  private final DiaryShare diaryShare = new DiaryShare();

  public DiaryShare diaryShare() {
    return this.diaryShare;
  }

  void sendBroadcast(UUID to, String type, String payload) {
    this.broadcast(to, type, payload);
  }

  /** Schickt eine kleine Nachricht ueber LabyConnect an alle Laby-Spieler auf dem Server; nur {@code to} reagiert. */
  private boolean broadcast(UUID to, String type, String payload) {
    LabyConnectSession session = this.session();
    if (session == null) {
      return false;
    }
    try {
      if (!session.isAuthenticated()) {
        return false;
      }
      com.google.gson.JsonObject o = new com.google.gson.JsonObject();
      o.addProperty("to", to == null ? "*" : to.toString());
      o.addProperty("t", type);
      o.addProperty("n", Laby.labyAPI().getName());
      if (payload != null) {
        o.addProperty("p", payload);
      }
      session.sendBroadcastPayload(KEY, o);
      return true;
    } catch (RuntimeException t) {
      de.ulgu.fkreis.core.FkreisAddon.log().warn("Freundeskreis: " + t);
      return false;
    }
  }

  /** Bietet einem Spieler eine Kassette an (er muss sie erst annehmen). */
  public boolean offerTape(UUID to, String payload) {
    return this.broadcast(to, "MTO", payload);
  }

  private void reply(UUID to, String type, String payload) {
    this.broadcast(to, type, payload);
  }

  @Subscribe
  public void onBroadcast(LabyConnectBroadcastEvent event) {
    try {
      if (event.action() != LabyConnectBroadcastEvent.Action.RECEIVE || !KEY.equals(event.getKey())) {
        return;
      }
      UUID from = event.getSender();
      if (from == null || from.equals(self()) || event.getPayload() == null || !event.getPayload().isJsonObject()) {
        return;
      }
      com.google.gson.JsonObject o = event.getPayload().getAsJsonObject();
      String to = o.has("to") ? o.get("to").getAsString() : "";
      if (!"*".equals(to) && !self().toString().equals(to)) {
        return;
      }
      String type = o.has("t") ? o.get("t").getAsString() : "";
      String name = o.has("n") ? o.get("n").getAsString() : "?";
      if (name.length() > 16 || !name.matches("[A-Za-z0-9_]+")) {
        name = "?";
      }
      String payload = o.has("p") ? o.get("p").getAsString() : null;
      String fromName = name;
      this.mainThread.add(() -> this.handle(from, fromName, type, payload));
    } catch (RuntimeException t) {
      de.ulgu.fkreis.core.FkreisAddon.log().warn("Freundeskreis: " + t);
    }
  }

  // ------------------------------------------------------------------ Empfang

  private void handle(UUID from, String fromName, String type, String payload) {
    if (!this.addon.on()) {
      return;
    }
    Store store = this.addon.store();
    switch (type) {
      case "REQ": {
        if (this.addon.configuration().profile().share().get()) {
          String json = GSON.toJson(this.ownProfile());
          String b64 = Base64.getUrlEncoder().encodeToString(json.getBytes(StandardCharsets.UTF_8));
          this.reply(from, "PRF", b64);
        } else {
          this.reply(from, "NOP", null);
        }
        break;
      }
      case "PRF": {
        try {
          String json = new String(Base64.getUrlDecoder().decode(payload), StandardCharsets.UTF_8);
          ProfileData data = GSON.fromJson(json, ProfileData.class);
          data.received = System.currentTimeMillis();
          store.profiles.put(from.toString(), data);
          store.names.put(from.toString(), fromName);
          store.save();
          this.pending.remove(from);
          this.fire(from, Result.LOADED, data);
          if (data.birthdayToday()) {
            this.birthdayNotice(from, fromName);
          }
        } catch (Exception e) {
          this.fire(from, Result.NONE, null);
        }
        break;
      }
      case "NOP": {
        this.pending.remove(from);
        this.fire(from, Result.NONE, null);
        break;
      }
      case "STK": {
        if (!this.addon.configuration().stickerBook().accept().get()) {
          return;
        }
        if (store.hasStickerFrom(from.toString())) {
          return;
        }
        store.stickers.add(new Sticker(from.toString(), fromName, System.currentTimeMillis()));
        store.save();
        notify(L.t("sticker.got.title"), L.t("sticker.got.text", fromName), G.head(from));
        G.sound("entity.player.levelup", 1.6F);
        break;
      }
      case "FMQ": {
        this.matchShare.onRequest(this, from);
        break;
      }
      case "FMA": {
        this.matchShare.onAnswer(from, fromName, payload);
        break;
      }
      case "MTO": {
        de.ulgu.fkreis.core.mixtape.Gifts.onOffer(from, fromName, payload);
        break;
      }
      case "SBQ": {
        this.stickerShare.onRequest(this, from);
        break;
      }
      case "SBL": {
        this.stickerShare.onList(from, payload);
        break;
      }
      case "SBN": {
        this.stickerShare.onDenied(from);
        break;
      }
      case "DRQ": {
        this.diaryShare.onRequest(from, payload);
        break;
      }
      case "DRC": {
        this.diaryShare.onChunk(from, payload);
        break;
      }
      case "ALQ": {
        this.album.onRequest(from);
        break;
      }
      case "ALC": {
        this.album.onCount(from, payload);
        break;
      }
      case "ALN": {
        this.album.onDenied(from);
        break;
      }
      case "ALP": {
        this.album.onPhoto(from, payload);
        break;
      }
      case "BDY": {
        store.birthdayToday.put(from.toString(), LocalDate.now().toString());
        store.names.put(from.toString(), fromName);
        store.save();
        this.birthdayNotice(from, fromName);
        break;
      }
      default:
        break;
    }
  }

  public static void notify(String title, String text, net.labymod.api.client.gui.icon.Icon icon) {
    try {
      Notification.Builder b = Notification.builder().title(Component.text(title)).text(Component.text(text));
      if (icon != null) {
        b.icon(icon);
      }
      b.buildAndPush();
    } catch (RuntimeException t) {
      de.ulgu.fkreis.core.FkreisAddon.log().warn("Freundeskreis: " + t);
    }
  }

  // ------------------------------------------------------------------ Tick

  @Subscribe
  public void onTick(GameTickEvent event) {
    if (event.phase() != Phase.POST) {
      return;
    }
    Runnable r;
    while ((r = this.mainThread.poll()) != null) {
      r.run();
    }
    de.ulgu.fkreis.core.ui.Screens.tick();

    long now = System.currentTimeMillis();
    Iterator<Map.Entry<UUID, Long>> it = this.pending.entrySet().iterator();
    while (it.hasNext()) {
      Map.Entry<UUID, Long> e = it.next();
      if (now - e.getValue() > TIMEOUT_MS) {
        it.remove();
        this.fire(e.getKey(), Result.TIMEOUT, this.addon.store().profiles.get(e.getKey().toString()));
      }
    }

    ++this.ticks;
    this.album.tick(this);
    this.diaryShare.tick(this);
    this.stickerShare.tick();
    if (this.ticks % 40 == 1) {
      this.refreshFriendSince();
    }
    if (this.ticks % 20 == 0) {
      this.birthdayField();
    }
    // Einmal am Tag: am eigenen Geburtstag allen Online-Freunden Bescheid geben + Hinweise
    if (this.ticks % 200 == 0 && this.addon.on()) {
      this.birthdayBroadcast();
      this.birthdayReminders();
    }
  }

  private void birthdayBroadcast() {
    if (!this.addon.configuration().profile().share().get()) {
      return;
    }
    ProfileData own = this.ownProfile();
    if (!own.birthdayToday()) {
      return;
    }
    String today = LocalDate.now().toString();
    Store store = this.addon.store();
    if (today.equals(store.lastBirthdayBroadcast)) {
      return;
    }
    LabyConnectSession session = this.session();
    if (session == null) {
      return;
    }
    this.broadcast(null, "BDY", null);
    store.lastBirthdayBroadcast = today;
    store.save();
  }

  // ------------------------------------------------------------------ Geburtstag (Kalender)

  private static final long DAY = 24L * 60 * 60 * 1000;
  private static final long LOCK = 30 * DAY;
  private static final long GRACE = 10L * 60 * 1000;
  private String lastRaw;
  private long stableSince;
  private String warnedInvalid = "";
  private boolean migrated;

  /** Ab wann darf der Geburtstag wieder geaendert werden? 0 = jetzt schon. */
  public long birthdayUnlockAt() {
    Store store = this.addon.store();
    if (store.birthdayCommitted.isEmpty()) {
      return 0;
    }
    long now = System.currentTimeMillis();
    if (now - store.birthdayChangedAt < GRACE) {
      return 0;
    }
    long at = store.birthdayChangedAt + LOCK;
    return at > now ? at : 0;
  }

  private static String date(long ms) {
    return new java.text.SimpleDateFormat("dd.MM.yyyy").format(new java.util.Date(ms));
  }

  /** Ueberwacht das Geburtstags-Feld im Kalender: speichern, Sperre (30 Tage), 10 Minuten zum Korrigieren. */
  private void birthdayField() {
    Store store = this.addon.store();
    var cal = this.addon.configuration().calendar();
    if (!this.migrated) {
      this.migrated = true;
      String legacy = this.addon.configuration().profile().legacyBirthday().get().trim();
      if (!legacy.isEmpty()) {
        if (cal.birthday().get().trim().isEmpty()) {
          cal.birthday().set(legacy);
        }
        if (store.birthdayCommitted.isEmpty()) {
          store.birthdayCommitted = ProfileData.normalizeBirthday(legacy);
          store.birthdayChangedAt = 0; // alter Eintrag: einmal frei aenderbar
          store.save();
        }
        this.addon.configuration().profile().legacyBirthday().set("");
      }
    }
    String raw = cal.birthday().get() == null ? "" : cal.birthday().get().trim();
    long now = System.currentTimeMillis();
    if (!raw.equals(this.lastRaw)) {
      this.lastRaw = raw;
      this.stableSince = now;
      return;
    }
    if (now - this.stableSince < 4000) {
      return; // noch am Tippen
    }
    String norm = ProfileData.normalizeBirthday(raw);
    String committed = store.birthdayCommitted;
    if (norm.equals(committed) && (!norm.isEmpty() || raw.isEmpty())) {
      if (!raw.equals(norm)) {
        cal.birthday().set(norm);
        this.lastRaw = norm;
      }
      return;
    }
    if (!raw.isEmpty() && norm.isEmpty()) {
      if (!raw.equals(this.warnedInvalid)) {
        this.warnedInvalid = raw;
        notify(L.t("bday.lock.title"), L.t("bday.invalid"), null);
      }
      return;
    }
    long unlock = this.birthdayUnlockAt();
    if (unlock == 0) {
      boolean grace = !committed.isEmpty() && now - store.birthdayChangedAt < GRACE;
      store.birthdayCommitted = norm;
      if (!grace) {
        store.birthdayChangedAt = now;
      }
      store.save();
      if (!raw.equals(norm)) {
        cal.birthday().set(norm);
      }
      this.lastRaw = norm;
      if (!norm.isEmpty()) {
        notify(L.t("bday.savedtitle"), L.t("bday.saved", norm, date(store.birthdayChangedAt + LOCK)), null);
      }
    } else {
      cal.birthday().set(committed);
      this.lastRaw = committed;
      notify(L.t("bday.lock.title"), L.t("bday.locked", date(unlock)), null);
    }
  }

  /** Hat diese Person heute Geburtstag (soweit bekannt)? */
  public boolean birthdayToday(UUID uuid) {
    String today = LocalDate.now().toString();
    if (uuid.equals(self())) {
      ProfileData own = new ProfileData();
      own.birthday = this.addon.store().birthdayCommitted;
      return own.birthdayToday();
    }
    Store store = this.addon.store();
    if (today.equals(store.birthdayToday.get(uuid.toString()))) {
      return true;
    }
    ProfileData p = store.profiles.get(uuid.toString());
    return p != null && p.birthdayToday();
  }

  private void birthdayNotice(UUID uuid, String name) {
    Store store = this.addon.store();
    String today = LocalDate.now().toString();
    if (today.equals(store.birthdayNotified.get(uuid.toString()))) {
      return;
    }
    store.birthdayNotified.put(uuid.toString(), today);
    store.save();
    if (!this.addon.configuration().calendar().reminders().get()) {
      return;
    }
    notify(L.t("bday.title"), L.t("bday.text", name), G.head(uuid));
    G.sound("entity.player.levelup", 1.2F);
  }

  /** Einmal am Tag: eigener Geburtstag + Geburtstage, die wir schon kennen. */
  private void birthdayReminders() {
    Store store = this.addon.store();
    String today = LocalDate.now().toString();
    UUID me = self();
    if (this.birthdayToday(me) && !today.equals(store.birthdayNotified.get(me.toString()))) {
      store.birthdayNotified.put(me.toString(), today);
      store.save();
      if (this.addon.configuration().calendar().reminders().get()) {
        notify(L.t("bday.own.title"), L.t("bday.own.text"), G.head(me));
        G.sound("entity.player.levelup", 1.0F);
      }
    }
    for (FriendInfo f : this.friends()) {
      if (this.birthdayToday(f.uuid)) {
        this.birthdayNotice(f.uuid, f.name);
      }
    }
  }

  private long lastRefresh;

  /** Kalender "Aktualisieren": Steckbriefe der Online-Freunde neu holen (hoechstens einmal pro Minute). */
  public boolean refreshBirthdays() {
    long now = System.currentTimeMillis();
    if (now - this.lastRefresh < 60_000) {
      return false;
    }
    this.lastRefresh = now;
    for (FriendInfo f : this.friends()) {
      if (f.online) {
        this.requestProfile(f.uuid);
      }
    }
    return true;
  }

  /** Bekannte Geburtstage: uuid -> "TT.MM." (inkl. dir selbst). */
  public java.util.Map<UUID, String> knownBirthdays() {
    java.util.Map<UUID, String> map = new java.util.LinkedHashMap<>();
    Store store = this.addon.store();
    if (!store.birthdayCommitted.isEmpty()) {
      map.put(self(), store.birthdayCommitted);
    }
    for (java.util.Map.Entry<String, ProfileData> e : store.profiles.entrySet()) {
      String b = ProfileData.normalizeBirthday(e.getValue().birthday);
      if (!b.isEmpty()) {
        try {
          map.put(UUID.fromString(e.getKey()), b);
        } catch (Exception ignored) {
        }
      }
    }
    return map;
  }

  /** Anzeigename: Freundesliste, sonst gespeicherter Name. */
  public String nameOf(UUID uuid) {
    if (uuid.equals(self())) {
      return Laby.labyAPI().getName();
    }
    Friend f = this.friend(uuid);
    if (f != null) {
      return f.getName();
    }
    return this.addon.store().nameOf(uuid.toString());
  }
}
