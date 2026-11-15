# Freundeskreis – ULGU Software

LabyMod 4 addon by **Priester_Horst** (ULGU Software) for Minecraft 1.21 and newer.

## Features
- **Profile card** – name, nickname, age, birthday, country, state and a "mob identity", shown as a wooden card (wood type selectable)
- **Sticker book** – give every player one sticker, collect stickers in an album
- **Notebook shelf** – one notebook per LabyMod friend with notes you can drag to the trash
- **Friendship coins** – a coin next to a friend's name that changes with the length of the friendship
- **Birthday calendar** – birthday reminders, a cake next to the name on the birthday and a HUD widget "Next birthday"
- **Photo album** – save screenshots with frames and captions, optionally let others view your album
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
Pixel font for notes: [Monocraft](https://github.com/IdreesInc/Monocraft) by Idrees Hassan, SIL Open Font License 1.1 (see `core/src/main/resources/assets/fkreis/textures/font/MONOCRAFT-LICENSE.txt`).
