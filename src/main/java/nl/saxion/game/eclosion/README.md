# Eclosion – game code

Everything of the game lives in this package (`nl.saxion.game.eclosion`).
The template files in `nl.saxion.game.yourgamename` (`Box`, `MainMenuScreen`, `YourGameScreen`) are **not used**
and are left untouched on purpose, so they don't clash with other branches.

## Running

**IntelliJ:** File > Open > choose the project folder (the one with `build.gradle`) and let IntelliJ load the
Gradle project. Then pick the run configuration **Eclosion** (top right) and press Run.
You need JDK 21 installed (File > Project Structure > SDK > Download JDK > version 21 if you don't have it).
Gradle always runs itself on JDK 21 (`gradle/gradle-daemon-jvm.properties`), so it also works when
IntelliJ's default JDK is newer (Gradle 8.12 does not run on JDK 25).

**Terminal:**

```
gradlew run
```

The game starts on the title screen: **any key → New Game** → the opening cutscene in Ageha's room.
If fullscreen was switched on in the settings, the game opens in fullscreen straight away.

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
TitleScreen ──New Game──► AgehaRoomScreen
                            1. opening cutscene (asleep → words → wakes up → stands by the bed)
                            2. first time at the door: Niri is there ("Follow me to the main hall")
                            3. after that the door leads out
                                   │ door (E)
                                   ▼
                          CorridorScreen ◄──hallway──► MainHallScreen (Bluma + Niri)
                                                          │ Bluma: "Minigame 1" / "Minigame 2"
                                                          ▼
                                              Floor1FlowerScreen   /   MiniGame2
                                              (back to the hall when done or with Esc)
                                                          │
                                                          ▼
                             NIGHT: the whole house is dark, Niri is gone from the hall,
                             Bluma only says "You should go sleep, Ageha." (no minigames).
                             Ageha walks back to his room, E at the bed: he lies down and
                             closes his eyes; the next morning he wakes up and it is day again.
```

**New Game** makes all places new (`Main.addStoryScreens()`), so a new game really starts from the beginning
(the cutscene plays again, Niri is at the door again, nothing has been looked at yet).

## Packages

| Package | What is in it |
|---|---|
| `explore` | The walking engine. **`ExploreScreen`** is the base class of every place Ageha walks around in: movement, click-to-walk, collision, light, night, items (E), doors and exits, other characters (standing or walking, fading in and out), drawing in depth order, pause menu. Helpers: `WalkGrid` (A* path finding for click-to-walk), `RoomOutline` (the black outside the room can't be clicked), `RoomLight` (characters take the colour of the floor light; lamps), `TimeOfDay` (day or night), `Npc`, `Prop`, `Interactable`. |
| `rooms` | The places: `AgehaRoomScreen` (+ Niri at the door, the bed, the safe keypad), `BedCutscene` (the opening of the game and going to sleep, both in Ageha's bed), `CorridorScreen`, `MainHallScreen` (+ Bluma and the minigame options). |
| `ui` | `DialogueBox` + `DialogueLine` (the text box with portraits, typing, voices, choices), `TitleScreen`, `PauseMenu`, `Settings` (saved volumes/fullscreen), `SafeKeypad`, `ScreenFader`, `ClickEffect`, `PixelFont`. |
| `audio` | `Sfx` (all sound effects) and `Bgm` (background music, keeps playing from room to room). |
| `minigames` | `Floor1FlowerScreen` + `Flower` ("Queen of the Night", made by **Dawon**) and `MiniGame2` ("Find the odd colour", made by **Giang**), both adapted to this game (see the comment at the top of each file). |

## Making a new place

Extend `ExploreScreen`, then fill in `buildRoom()` and `placePlayer()`. All positions are **pixels of the room
picture** (x, y from the top-left, like in Krita); `imageRect()` turns them into game coordinates.

```java
public class LibraryScreen extends ExploreScreen {
    public LibraryScreen() {
        super("library", "textures/rooms/library.png", 1670, 942);   // texture name, file, picture size
        agehaH = 90;                                                  // Ageha's height here (see "Sizes")
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

Then register it in `Main.addStoryScreens()`: `GameApp.addScreen("LibraryScreen", new LibraryScreen());`

Other tools in `ExploreScreen`:

| Tool | What it does |
|---|---|
| `extraFloor`, `stepFloor` | more walkable floor (hallways), stairs |
| `addExit`, `addExitNotMadeYet` | walking into a hallway end goes somewhere (or says "not ready yet") |
| `addProp`, `setPropVisible` | furniture that covers Ageha when he walks behind it; can be switched on and off |
| `addSpecial` | E runs your own code (a keypad, a cutscene, ...) |
| `addNpc` + `walkUpTo` | a character that stands in the room; E makes Ageha walk up and talk |
| `addMovingNpc` / `addNiri`, `showNpcAtImage`, `moveNpcToward`, `fadeNpcIn/Out`, `hideNpc` | a character that comes and goes and walks around |
| `overlayOpen`, `updateOverlay`, `renderOverlay`, `onTextFinished` | hooks for your own scenes (Ageha can't walk while `overlayOpen()` is true) |
| `agehaHidden`, `showAgehaWalking` | for cutscenes that hide Ageha or move him themselves |
| `music()` | the music of the place (`null` = quiet) |

## The cutscenes in bed (opening, going to sleep)

`rooms/BedCutscene.java` plays the scenes with Ageha in his bed. Each scene is one list of steps:
`OPENING` (the start of the game) and `GO_TO_SLEEP` (at night, E at the bed).
Steps: `picture(...)` (a bed picture), `waitFor(seconds)`, `sound(...)`, `typeText("...")`, `flood("...")`,
`typeTextWithHashes("Forget", 3)`, `dark()` / `light()`, `say("...")` (inner voice in the text box),
`lieDown()` (black, then he lies in bed), `sleep()` (black while the night passes) and `standUp()`
(black, he stands next to the bed). In `GO_TO_SLEEP` a comment marks where the art and the dream cutscene
of the minigame he played will come (not made yet).
To change the order or the timing, change those lists; the speeds and colours are constants at the top of the file.
The room only shows the bed pictures and the text box for it (`BedCutscene.Room`).

## Night

`TimeOfDay` says if it is day or night in the whole house. Coming back from a minigame makes it night
(`MainHallScreen.placePlayer`); sleeping in the bed makes it day again (`AgehaRoomScreen.nightIsOver`); a new game
starts during the day. Sleeping: black, he lies in bed and closes his eyes, black while the night passes,
then he wakes up in the morning. At night `ExploreScreen` draws a dark shade over every place, lighter around its lamps
(the lights added in `addLights()`); `NIGHT_DARKNESS` sets how dark it is. The corridor and the hall play
a darker version of the floor theme at night (`Bgm.FLOOR1_NIGHT`).

## Sizes

The places are drawn at different scales, so Ageha's height (`agehaH`) is set per place, matched by things
drawn in more than one picture (the doors, the window): room **98**, corridor **84**, main hall **87**.
Other characters are sized from Ageha: Bluma as tall as him, Niri **90%** (`NIRI_HEIGHT`).
Dialogue portraits have a height per character in `DialogueBox.PORTRAIT_HEIGHTS`.

## Dialogue voices

Every speaker has the same soft "blip" while their text is typed, at their own pitch (`DialogueBox.VOICE_PITCH`).
Niri has his own blip (`dialogue_blip_niri.wav`, `DialogueBox.VOICE_SOUND`). Names have their own colour
(`DialogueBox.NAME_COLORS`).

## Assets (`src/main/resources`)

| Folder | Contents |
|---|---|
| `textures/rooms` | Room pictures |
| `textures/props` | Furniture cut out of the room pictures (drawn over Ageha), the bed with Ageha in it (`ageha_bed_*`) |
| `textures/sprites` | Walk frames: `ageha_walk_<down/up/left/right>_<1-3>`, `bluma_walk_...`, `niri_walk_...` |
| `textures/portraits` | Dialogue portraits. In use: `ageha_neutral`, `bluma_neutral`, `niri_laugh`, `niri_sad`. Ready for later: `ageha_eyes_closed/serious/small_smile`, `bluma_sad/surprised/worried`, `niri_happy/smile`, `hal_base` |
| `textures/ui`, `textures/Background` | The title screen picture (`title_screen.png`), minigame 2 backgrounds |
| `audio` | Sound effects and music – sources and licences in `audio/CREDITS.txt` |
| `fonts` | DotGothic16 (pixel font for the text) and Grenze Gotisch (the words of the opening), both SIL Open Font License |

## Still to do (placeholders in the code)

- Mirror cutscene and the journal in Ageha's room (`AgehaRoomScreen`)
- The phone in the corridor (dialing)
- Real code for the safe (`SAFE_CODE`) and what is inside
- The other exits of the main hall and the second floor (`addExitNotMadeYet` in `MainHallScreen`)
- Saving the minigame rewards (`TODO` in `Floor1FlowerScreen`) and a save system for "Continue"

## Dawon's additions

| What | Where | How it works |
|---|---|---|
| Who is speaking | `ui/DialogueBox` (`updateSpeakerLight`) | the portrait of the one who is not speaking is shaded and 90% big; while the options are shown, Ageha (left) is lit |
| Memory items | `items/Items` | `Items.give(Items.HAIR_TIE)`, `Items.has(...)`; one place per floor (`Items.SLOTS`); pictures in `textures/items/<id>.png`; `Items.reset()` on New Game |
| Item found | `ui/ItemPopup` | the panel with the item's picture, name and text (used by minigame 1 for the hair tie) |
| Collection | `ui/CollectionMenu` | **I** or **Tab** while walking around: one place per floor, found items with their picture, the rest "?" |
| The night passing | `ui/NightClock` | during `sleep()` in `BedCutscene.GO_TO_SLEEP`: hazy night sky with scan lines, a pixel moon (made in code, fuller every night, full on Day 7) inside a dashed red ring of hours 23-06, the time racing in huge glowing digits. At 06:00 a deep grandfather-clock chime (`sfx_clock_chime`) strikes 4 times and the dawn turns red: blood moon, red mist, the last hours of the ring burn. Then the red day number rolls over: "DAY 3  15 JUNE / 4 DAYS UNTIL HIS 18TH BIRTHDAY". `NightClock.day` is the day of the week (`reset()` on New Game) |
| Day + collection hint | `ui/Hud` | always on screen while walking around: top left "DAY 2  14 JUNE / 5 DAYS LEFT" with a sun or moon, bottom right "[I] Collection". Fades out during dialogue, menus and cutscenes |
| Minigame 1 | `minigames/Floor1FlowerScreen` | 15 flowers clears it; at 15 the game asks: stop, or keep going for 25 (faster, more withered flowers, and under 15 at the end = the night is lost). 25 = the hair tie. Intro, choice and result use the dialogue box |
