# Freundeskreis – ULGU Software

LabyMod 4 addon by **Priester_Horst** (ULGU Software) for Minecraft 1.21 and newer.

## Features
- **Profile card** – name, nickname, age, birthday, country, state and a "mob identity", shown as a wooden card (wood type selectable)
- **Sticker book** – give every player one sticker, collect stickers in an album
- **Notebook shelf** – one notebook per LabyMod friend with notes you can drag to the trash
- **Friendship coins** – a coin next to a friend's name that changes with the length of the friendship
- **Birthday calendar** – birthday reminders, a cake next to the name on the birthday and a HUD widget "Next birthday"
- **Photo album** – save screenshots with frames and captions, optionally let others view your album
- **Vanish** – hide any player just for yourself (mouse-wheel entry or `/fk`), bring them back from the "Scroll of the Banished"; done purely through LabyMod render events
- **Diary** – a private leather-bound diary with one entry per day (several pages), optional reminder on server join, selectable cover (three leathers or a white book you dye yourself); can be shared read-only with a code
- **Mixtapes** – a box of cassette tapes: each tape has your own label and a YouTube or Spotify link; putting it into the recorder opens the link in the browser, an optional small recorder is shown on the HUD; tapes can be offered to friends, who have to accept
- **Blockmatch** – a questionnaire of 100 questions (values, humour, play style); afterwards the addon shows which players on the server with the addon answered most like you. It is about friendship, only a percentage is shown, and taking part can be switched off
- `/fk` quick menu, keybinds and mouse-wheel entries for everything

## How data is exchanged
Only through LabyConnect broadcasts (`sendBroadcastPayload`). There is no own server and no external service; nothing is posted in any chat.
Everything can be turned off in the addon settings.

## Building
```
./gradlew build createReleaseJar
```
The addon jar is written to `build/libs/`.

## Credits

Blockmatch music: "Slow S*x (Sexy music Nights)" by arose-xoo, from Pixabay (Pixabay Content License), https://pixabay.com/de/music/schlägt-slow-sx-sexy-music-nights-312276/
Pixel font for notes: [Monocraft](https://github.com/IdreesInc/Monocraft) by Idrees Hassan, SIL Open Font License 1.1 (see `core/src/main/resources/assets/fkreis/textures/font/MONOCRAFT-LICENSE.txt`).
