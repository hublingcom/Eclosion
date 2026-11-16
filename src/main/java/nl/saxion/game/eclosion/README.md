# Eclosion – game code

Everything of the game lives in this package (`nl.saxion.game.eclosion`).
The template files in `nl.saxion.game.yourgamename` (`Box`, `MainMenuScreen`, `YourGameScreen`) are **not used**
and are left untouched on purpose, so they don't clash with other branches.

## Running

Open the project in IntelliJ (Java 21) and run `nl.saxion.game.Main`, or from a terminal:

```
gradlew run
```

The game starts on the title screen: **any key → New Game** → Ageha's room.

## Controls

| What | Keys |
|---|---|
| Walk | WASD / arrow keys, or **left click** somewhere (Ageha finds a way around furniture) |
| Look at something / use a door | **E** (only E – clicking only walks) |
| Next line of text | Space / Enter / E / click. **Double click** = jump to the end of the conversation |
| Choose an option | Up/Down (W/S) + Enter/Space/E, or click it |
| Pause menu | **Esc** (Resume / Settings / Title Screen / Quit Game) |

## The flow of the demo

```
TitleScreen ──New Game──► AgehaRoomScreen ◄──door (E)──► CorridorScreen ◄──hallway──► MainHallScreen
                                                                                         │
                                                       Bluma: "Minigame 1" / "Minigame 2"│
                                                                                         ▼
                                                            Floor1FlowerScreen   /   MiniGame2
                                                            (back to the hall when done or with Esc)
```

## Packages

| Package | What is in it |
|---|---|
| `explore` | The walking engine. **`ExploreScreen`** is the base class of every place Ageha walks around in: movement, click-to-walk, collision, light, items (E), doors and exits, other characters, drawing in depth order, pause menu. Helpers: `WalkGrid` (A* path finding for click-to-walk), `RoomOutline` (the black outside the room can't be clicked), `RoomLight` (characters take the colour of the floor light), `Npc`, `Prop`, `Interactable`. |
| `rooms` | The places: `AgehaRoomScreen` (+ the safe keypad), `CorridorScreen`, `MainHallScreen` (+ Bluma and the minigame options). |
| `ui` | `DialogueBox` + `DialogueLine` (the text box with portraits, typing, choices), `TitleScreen`, `PauseMenu`, `Settings` (saved volumes/fullscreen), `SafeKeypad`, `ScreenFader`, `ClickEffect`, `PixelFont`. |
| `audio` | `Sfx` (all sound effects) and `Bgm` (background music, keeps playing from room to room). |
| `minigames` | `Floor1FlowerScreen` + `Flower` ("Queen of the Night", made by **Dawon**) and `MiniGame2` ("Find the odd colour", made by **Giang**), both adapted to this game (see the comment at the top of each file). |

## Making a new place

Extend `ExploreScreen`, then fill in `buildRoom()` and `placePlayer()`. All positions are **pixels of the room
picture** (x, y from the top-left, like in Krita); `imageRect()` turns them into game coordinates.

```java
public class LibraryScreen extends ExploreScreen {
    public LibraryScreen() {
        super("library", "textures/rooms/library.png", 1670, 942);   // texture name, file, picture size
    }

    @Override
    protected void buildRoom() {
        floor = imageRect(300, 500, 1000, 300);                       // where Ageha's feet may go
        solids.add(imageRect(600, 520, 120, 40));                     // a table's footprint
        addItem("Book", imageRect(610, 470, 60, 50),
                lines("An old book..."),                              // first time
                lines("Still the same book."));                       // after that
        addDoor("Door", imageRect(900, 480, 90, 40), "CorridorScreen");
    }

    @Override
    protected void placePlayer() {
        placeFeetAtImage(940, 560, "up");
    }
}
```

Then register it in `Main.java`: `GameApp.addScreen("LibraryScreen", new LibraryScreen());`

Other tools in `ExploreScreen`: `extraFloor` (hallways), `stepFloor` (stairs), `addExit` (walk into a hallway end
to go somewhere), `addExitNotMadeYet`, `addProp` (furniture that covers Ageha when he walks behind it),
`addNpc` + `walkUpTo` (a character to talk to), `addSpecial` (E runs your own code), `music()`.

## Assets (`src/main/resources`)

| Folder | Contents |
|---|---|
| `textures/rooms` | Room pictures |
| `textures/props` | Furniture cut out of the room pictures (drawn over Ageha) |
| `textures/sprites` | Walk frames: `ageha_walk_<down/up/left/right>_<1-3>`, `bluma_walk_...` |
| `textures/portraits` | Dialogue portraits (`ageha_*`, `bluma_*`, `hal_base`) |
| `textures/ui`, `textures/Background` | Title text, minigame 2 backgrounds |
| `audio` | Sound effects and music – sources and licences in `audio/CREDITS.txt` |
| `fonts` | DotGothic16 (pixel font, SIL Open Font License) |

## Still to do (placeholders in the code)

- Mirror cutscene and the journal in Ageha's room (`AgehaRoomScreen`)
- The phone in the corridor (dialing)
- Real code for the safe (`SAFE_CODE`) and what is inside
- The other exits of the main hall and the second floor (`addExitNotMadeYet` in `MainHallScreen`)
- Saving the minigame rewards (`TODO` in `Floor1FlowerScreen`) and a save system for "Continue"
