package nl.saxion.game.eclosion.rooms;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import nl.saxion.game.eclosion.audio.Sfx;
import nl.saxion.game.eclosion.ui.DialogueBox;
import nl.saxion.game.eclosion.ui.NightClock;
import nl.saxion.game.eclosion.ui.ScreenFader;
import nl.saxion.gameapp.GameApp;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * The cutscenes with Ageha in his bed, played in his room. Each one is a list of steps:
 *
 * {@link #OPENING} - the start of the game. Ageha sleeps. Three knocks, he doesn't wake up.
 *   White gothic words are typed over the room ("Ignore it", "Forget it,"), then the screen goes black
 *   and fills with "Forget it". "Forget" is typed in the middle and gets three # signs (a name he
 *   can't remember), each with a hard hit. The room comes back, he opens his eyes, quicker knocks,
 *   he says "...". There is no getting-up animation: the screen goes black and he stands next to the bed.
 *
 * {@link #GO_TO_SLEEP} - at night, E at the bed. A short black, then he lies in bed and slowly closes
 *   his eyes. Black while the night passes, then the morning: he opens his eyes and stands next to
 *   the bed again.
 *
 * Change the order or the timing in those lists. The room is told what to show through {@link Room}.
 */
class BedCutscene {

    /** What the cutscene needs from the room it plays in. */
    interface Room {
        /** Show this bed picture (one of the pictures below) and hide the others. */
        void showBedPicture(String picture);

        /** Ageha's inner voice in the text box; the cutscene waits until {@link #textFinished()}. */
        void say(String text);

        /** Called while the screen is black at the end: Ageha is out of bed now. */
        void outOfBed();

        /** Called while the screen is black at night: Ageha lies in bed now (eyes open). */
        void lieDown();

        /** Called while the screen is black after he fell asleep: the night is over. */
        void nightIsOver();
    }

    // ---------- the bed pictures (props of the room) ----------
    static final String EMPTY_BED = "ageha_bed";
    static final String ASLEEP = "ageha_bed_sleeping";
    static final String HALF_OPEN = "ageha_bed_halfopen";
    static final String AWAKE = "ageha_bed_awake";
    static final String[] BED_WITH_AGEHA = {ASLEEP, HALF_OPEN, AWAKE};

    // ---------- the words over the room: white, typed letter by letter, trembling ----------
    private static final String BIG_FONT = "opening_big_text";
    private static final String FLOOD_FONT = "opening_flood_text";
    private static final Color TEXT_COLOR = new Color(235 / 255f, 232 / 255f, 228 / 255f, 1f);
    private static final float LETTERS_PER_SECOND = 22;      // "Ignore it", "Forget it,": a bit slower than the text box (40)
    private static final float SLOW_LETTERS_PER_SECOND = 5;  // "Forget": slowly, letter by letter
    private static final float TEXT_HOLD = 0.8f, TEXT_FADE_OUT = 0.4f, TEXT_GAP = 0.25f;
    private static final float SHAKE = 1.2f;                 // how much the words tremble (pixels)

    // "Forget ###": the # signs come one by one, a hard hit with each; the line shakes harder with each #
    private static final float FIRST_HASH_PAUSE = 0.6f;      // after "Forget" is typed
    private static final float HASH_DELAY = 1.3f;            // between the # signs
    private static final float HASH_HOLD = 0.4f, HASH_FADE_OUT = 0f, HASH_GAP = 0f;   // cut off right after the last #
    private static final float HASH_SHAKE = 1.4f;            // extra trembling per #

    // "Forget" with each #: white -> red at the first #, then a deeper, darker red; the # signs stay white
    private static final Color[] WORD_REDS = {
            new Color(200 / 255f, 30 / 255f, 34 / 255f, 1f),     // after the 1st #
            new Color(150 / 255f, 14 / 255f, 20 / 255f, 1f),     // after the 2nd #
            new Color(105 / 255f, 4 / 255f, 10 / 255f, 1f)};     // after the 3rd #: dark blood red
    private static final float RED_FADE_TIME = 0.35f;        // how fast the colour changes after each #

    // the screen filling up with "Forget it"; the words stay while "Forget" is typed in the middle
    private static final int FLOOD_WORDS = 60;
    private static final float FLOOD_INTERVAL = 0.05f;       // a new word every ... seconds
    private static final float FLOOD_HOLD = 0.6f;            // all words there, before "Forget" starts
    private static final float FLOOD_DIM = 0.45f;            // the words get dimmer while "Forget" is typed ...
    private static final float FLOOD_FADE_OUT = 1.2f;        // ... and fade away once it is there
    private static final float[][] FLOOD_SPOTS = floodSpots(); // x, y (0..1 of the screen), brightness

    private static final float DARK_TIME = 0.4f;             // the room going black after "Forget it,"
    private static final float NIGHT_PASSING = 1.6f;         // how long it stays black while he sleeps
    private static final float GETTING_UP = 0.6f;            // how long it stays black while he gets up
    private static final float LYING_DOWN = 0.6f;            // how long it stays black while he lies down

    // ---------- the steps ----------
    private enum Kind { PICTURE, WAIT, SOUND, DARK, LIGHT, TYPE_TEXT, FLOOD, SAY, LIE_DOWN, SLEEP, STAND_UP }

    /** One step of the cutscene. count = number of # signs (TYPE_TEXT) or flood words (FLOOD). */
    private record Step(Kind kind, String value, float seconds, int count) {
        Step(Kind kind, String value, float seconds) {
            this(kind, value, seconds, 0);
        }
    }

    private static Step picture(String bedPicture) {
        return new Step(Kind.PICTURE, bedPicture, 0);
    }

    private static Step waitFor(float seconds) {
        return new Step(Kind.WAIT, null, seconds);
    }

    private static Step sound(String sfx) {
        return new Step(Kind.SOUND, sfx, 0);
    }

    /** White words typed letter by letter in the middle, then they fade away. */
    private static Step typeText(String text) {
        return new Step(Kind.TYPE_TEXT, text, text.length() / LETTERS_PER_SECOND + TEXT_HOLD + TEXT_FADE_OUT + TEXT_GAP);
    }

    /** Words typed slowly, then # signs added one by one with a hard hit (a name he can't remember). */
    private static Step typeTextWithHashes(String text, int hashes) {
        return new Step(Kind.TYPE_TEXT, text, text.length() / SLOW_LETTERS_PER_SECOND + FIRST_HASH_PAUSE
                + HASH_DELAY * (hashes - 1) + HASH_HOLD + HASH_FADE_OUT + HASH_GAP, hashes);
    }

    /** The same words appear again and again until they cover the whole screen (they stay for the next step). */
    private static Step flood(String text) {
        return new Step(Kind.FLOOD, text, FLOOD_WORDS * FLOOD_INTERVAL + FLOOD_HOLD, FLOOD_WORDS);
    }

    /** The whole screen fades to black. */
    private static Step dark() {
        return new Step(Kind.DARK, null, DARK_TIME);
    }

    /** The black is gone at once: the room is back. */
    private static Step light() {
        return new Step(Kind.LIGHT, null, 0);
    }

    private static Step say(String text) {
        return new Step(Kind.SAY, text, 0);
    }

    /** Black, then he lies in bed (eyes open). */
    private static Step lieDown() {
        return new Step(Kind.LIE_DOWN, null, 0);
    }

    /** Black while the night passes, then the room comes back in the morning (he still lies in bed). */
    private static Step sleep() {
        return new Step(Kind.SLEEP, null, 0);
    }

    /** Black, then he stands next to the bed. */
    private static Step standUp() {
        return new Step(Kind.STAND_UP, null, 0);
    }

    private static final Step[] OPENING = {
            picture(ASLEEP), waitFor(2.0f),
            sound(Sfx.KNOCK), waitFor(2.2f),                     // he doesn't wake up
            typeText("Ignore it"),
            typeText("Forget it,"),
            dark(),                                              // from here on: all black
            flood("Forget it"),                                  // the whole screen fills with it ...
            typeTextWithHashes("Forget", 3),                     // ... "Forget" in the middle, then #  ##  ###
            light(),                                             // right after the last #: black and words gone at once
            waitFor(0.4f),
            picture(HALF_OPEN), waitFor(0.5f),                   // he slowly opens his eyes
            picture(AWAKE), waitFor(1.5f),                       // and they stay open
            sound(Sfx.KNOCK_FAST), waitFor(1.4f),                // the knocks again, quicker now
            say("..."),                                          // then he gets up
            waitFor(0.4f),
            standUp(),                                           // black, then he stands by the bed
    };

    private static final Step[] GO_TO_SLEEP = {
            lieDown(),                                           // black, then he lies in bed at night
            picture(AWAKE), waitFor(1.2f),
            picture(HALF_OPEN), waitFor(0.7f),                   // he slowly closes his eyes
            picture(ASLEEP), waitFor(1.4f),
            // LATER: the art and the dream cutscene of the minigame he played come here (not made yet)
            sleep(),                                             // black: the clock races through the night (NightClock)
            waitFor(1.2f),                                       // morning, still asleep
            picture(HALF_OPEN), waitFor(0.5f),                   // he slowly opens his eyes
            picture(AWAKE), waitFor(1.0f),
            standUp(),                                           // black, then he stands by the bed
    };

    /** Where the flood words go: a loose grid over the whole screen, in a random order (the same every time). */
    private static float[][] floodSpots() {
        Random random = new Random(7);
        List<float[]> spots = new ArrayList<>();
        int cols = 5, rows = 12;
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                float x = (c + 0.5f + (r % 2) * 0.5f - 0.25f + (random.nextFloat() - 0.5f) * 0.4f) / cols;
                float y = (r + 0.5f + (random.nextFloat() - 0.5f) * 0.3f) / rows;
                spots.add(new float[]{x, y, 0.45f + random.nextFloat() * 0.55f});
            }
        }
        Collections.shuffle(spots, random);
        return spots.toArray(new float[0][]);
    }

    // ---------- state ----------
    private final Room room;
    private final ScreenFader fader = new ScreenFader();
    private final NightClock clock = new NightClock();   // the night passing (Dawon), during sleep()
    private boolean clockDone = false;
    private final Random shake = new Random();
    private final GlyphLayout layout = new GlyphLayout();

    private Step[] script = OPENING;         // the cutscene that is playing
    private boolean playing = false;         // the cutscene is on (the player can't walk)
    private int stepIndex = 0;
    private float stepTimer = 0;
    private boolean waitingForText = false;  // a SAY step: wait until the player has read it
    private Kind blackout = null;            // LIE_DOWN, SLEEP or STAND_UP: fading to black and back
    private int lettersTyped = 0;            // for the typing sound
    private int hashesShown = 0;             // for the hit with each #
    private String floodText = null;         // the flood words, while they are on screen
    private int floodCount = 0;
    private float floodAlpha = 0;
    private float darkness = 0;              // 0 = the room, 1 = all black (while the words are shown)

    BedCutscene(Room room) {
        this.room = room;
        fader.fadeOutTime = 1.0f;   // slowly to black ...
        fader.fadeInTime = 1.0f;    // ... and slowly back
    }

    // ---------- what the room calls ----------
    /** The start of the game (Ageha asleep, the knocks, the words). */
    void startOpening() {
        start(OPENING);
    }

    /** At night, at the bed: he sleeps until the next morning. */
    void startSleeping() {
        start(GO_TO_SLEEP);
    }

    private void start(Step[] steps) {
        script = steps;
        playing = true;
        stepIndex = -1;
        waitingForText = false;
        blackout = null;
        floodText = null;
        floodCount = 0;
        darkness = 0;
        nextStep();
    }

    boolean isPlaying() {
        return playing;
    }

    /** True while the text box shows his "..." (then the room lets the text box work). */
    boolean isWaitingForText() {
        return waitingForText;
    }

    /** The room calls this when the text of a SAY step is closed. */
    void textFinished() {
        if (waitingForText) {
            waitingForText = false;
            nextStep();
        }
    }

    void update(float delta) {
        if (!playing) {
            return;
        }
        if (blackout != null) {
            updateBlackout(delta);
            return;
        }
        stepTimer += delta;
        if (stepIndex >= script.length) {
            return;
        }
        Step step = script[stepIndex];
        switch (step.kind()) {
            case DARK -> darkness = Math.min(1, stepTimer / step.seconds());
            case LIGHT -> darkness = 0;
            case FLOOD -> updateFlood(step);
            case TYPE_TEXT -> updateTyping(step);
            default -> { }
        }
        if (stepTimer >= step.seconds()) {
            if (step.kind() == Kind.FLOOD) {
                floodCount = step.count();
            }
            nextStep();
        }
    }

    /** Draws the black, the words and the fade (over the room, under the text box). */
    void render(float worldW, float worldH) {
        if (playing && blackout == null) {
            drawDarkness(worldW, worldH);
            GameApp.enableTransparency();
            GameApp.startSpriteRendering();
            drawFlood(worldW, worldH);
            drawLine(worldW, worldH);
            GameApp.endSpriteRendering();
            GameApp.disableTransparency();
        }
        fader.render(worldW, worldH);
        clock.render(worldW, worldH);            // only while the night is passing
    }

    /** Grenze Gotisch for the words; call when the room is shown. */
    static void loadFonts() {
        GameApp.addFont(BIG_FONT, gothicFont(64));
        GameApp.addFont(FLOOD_FONT, gothicFont(40));
    }

    static void disposeFonts() {
        GameApp.disposeFont(BIG_FONT);
        GameApp.disposeFont(FLOOD_FONT);
    }

    // ---------- going through the steps ----------
    /** Starts the next step; steps that take no time are done straight away. */
    private void nextStep() {
        stepTimer = 0;
        lettersTyped = 0;
        hashesShown = 0;
        while (++stepIndex < script.length) {
            Step step = script[stepIndex];
            if (step.kind() != Kind.FLOOD && step.kind() != Kind.TYPE_TEXT) {
                floodText = null;                // the flood only stays for the line that follows it
            }
            switch (step.kind()) {
                case PICTURE -> room.showBedPicture(step.value());
                case SOUND -> Sfx.play(step.value());
                case SAY -> {
                    room.say(step.value());
                    waitingForText = true;
                    return;
                }
                case LIE_DOWN, SLEEP, STAND_UP -> {
                    blackout = step.kind();
                    clockDone = false;
                    fader.blackTime = switch (step.kind()) {
                        case SLEEP -> NIGHT_PASSING;
                        case LIE_DOWN -> LYING_DOWN;
                        default -> GETTING_UP;
                    };
                    fader.fadeOut();
                    return;
                }
                default -> {                     // the other steps take time
                    return;
                }
            }
        }
        playing = false;                         // all steps done: the player can walk again
    }

    /** Fading to black and back; while it is black he lies down, the night passes or he gets up. */
    private void updateBlackout(float delta) {
        fader.update(delta);
        if (fader.isDoneFadingOut()) {
            // while it is black after he fell asleep, the clock races through the night (Dawon)
            if (blackout == Kind.SLEEP && !clockDone) {
                if (!clock.isRunning()) {
                    clock.start();
                }
                clock.update(delta);
                if (clock.isRunning()) {
                    return;
                }
                clockDone = true;
            }
            switch (blackout) {
                case LIE_DOWN -> room.lieDown();
                case SLEEP -> room.nightIsOver();
                default -> room.outOfBed();
            }
            fader.fadeIn();
        } else if (!fader.busy()) {
            blackout = null;                     // the screen is back
            nextStep();
        }
    }

    private void updateFlood(Step step) {
        floodText = step.value();
        floodAlpha = 1;
        int shown = Math.min(step.count(), (int) (stepTimer / FLOOD_INTERVAL));
        for (int i = floodCount; i < shown; i++) {
            if (i % 2 == 0) {                    // a soft blip with every other word
                GameApp.getSound(DialogueBox.BLIP_SOUND).play(0.16f * Sfx.volume, 0.75f + (i % 5) * 0.04f, 0f);
            }
        }
        floodCount = shown;
    }

    private void updateTyping(Step step) {
        boolean withHashes = step.count() > 0;
        float speed = withHashes ? SLOW_LETTERS_PER_SECOND : LETTERS_PER_SECOND;
        int letters = Math.min(step.value().length(), (int) (stepTimer * speed));
        for (int i = lettersTyped; i < letters; i++) {
            if ((withHashes || i % 2 == 0) && Character.isLetter(step.value().charAt(i))) {
                GameApp.getSound(DialogueBox.BLIP_SOUND).play(0.22f * Sfx.volume, withHashes ? 0.7f : 0.85f, 0f);
            }
        }
        lettersTyped = letters;

        float typed = step.value().length() / speed;
        if (floodText != null) {
            // the flood gets dimmer while the word is typed, then fades away
            floodAlpha = stepTimer < typed
                    ? 1 - (1 - FLOOD_DIM) * stepTimer / typed
                    : Math.max(0, FLOOD_DIM * (1 - (stepTimer - typed) / FLOOD_FADE_OUT));
            if (floodAlpha <= 0) {
                floodText = null;
            }
        }
        if (withHashes && stepTimer >= typed + FIRST_HASH_PAUSE) {
            int shown = Math.min(step.count(), (int) ((stepTimer - typed - FIRST_HASH_PAUSE) / HASH_DELAY) + 1);
            if (shown > hashesShown) {
                Sfx.play(Sfx.KNOCK_HARD);        // a hard hit with each #
                hashesShown = shown;
            }
        }
    }

    // ---------- drawing ----------
    private void drawDarkness(float worldW, float worldH) {
        if (darkness <= 0) {
            return;
        }
        GameApp.enableTransparency();
        GameApp.startShapeRenderingFilled();
        GameApp.drawRect(-10, -10, worldW + 20, worldH + 20, new Color(0, 0, 0, darkness));
        GameApp.endShapeRendering();
        GameApp.disableTransparency();
    }

    private void drawFlood(float worldW, float worldH) {
        if (floodText == null || floodAlpha <= 0) {
            return;
        }
        BitmapFont font = GameApp.getFont(FLOOD_FONT);
        layout.setText(font, floodText);
        for (int i = 0; i < Math.min(floodCount, FLOOD_SPOTS.length); i++) {
            float[] spot = FLOOD_SPOTS[i];
            font.setColor(TEXT_COLOR.r, TEXT_COLOR.g, TEXT_COLOR.b, floodAlpha * spot[2]);
            font.draw(GameApp.getSpriteBatch(), layout,
                    Math.round(spot[0] * worldW - layout.width / 2 + jitter(SHAKE)),
                    Math.round(spot[1] * worldH + layout.height / 2 + jitter(SHAKE)));
        }
    }

    /** The line in the middle ("Ignore it", "Forget it,", "Forget ###"). */
    private void drawLine(float worldW, float worldH) {
        if (stepIndex >= script.length || script[stepIndex].kind() != Kind.TYPE_TEXT || lettersTyped == 0) {
            return;
        }
        Step step = script[stepIndex];
        boolean withHashes = step.count() > 0;
        float fadeOut = withHashes ? HASH_FADE_OUT : TEXT_FADE_OUT;
        float fadeOutStart = step.seconds() - (withHashes ? HASH_GAP : TEXT_GAP) - fadeOut;
        float alpha = stepTimer < fadeOutStart ? 1
                : fadeOut <= 0 ? 0
                : Math.max(0, 1 - (stepTimer - fadeOutStart) / fadeOut);
        if (alpha <= 0) {
            return;
        }

        // centred on the whole line (with all its # signs), so it doesn't move while it is typed
        String fullText = withHashes ? step.value() + " " + "#".repeat(step.count()) : step.value();
        BitmapFont font = GameApp.getFont(BIG_FONT);
        layout.setText(font, fullText);
        float left = worldW / 2 - layout.width / 2;
        float top = worldH / 2 + layout.height / 2;
        float amount = SHAKE + hashesShown * HASH_SHAKE;
        float dx = jitter(amount), dy = jitter(amount);

        if (hashesShown > 0) {
            // "Forget" turns redder with every #; the # signs stay white
            Color word = wordColor();
            font.setColor(word.r, word.g, word.b, alpha);
            layout.setText(font, step.value() + " ");
            float hashX = left + layout.width;
            font.draw(GameApp.getSpriteBatch(), layout, Math.round(left + dx), Math.round(top + dy));
            font.setColor(TEXT_COLOR.r, TEXT_COLOR.g, TEXT_COLOR.b, alpha);
            layout.setText(font, "#".repeat(hashesShown));
            font.draw(GameApp.getSpriteBatch(), layout, Math.round(hashX + dx), Math.round(top + dy));
        } else {
            font.setColor(TEXT_COLOR.r, TEXT_COLOR.g, TEXT_COLOR.b, alpha);
            layout.setText(font, step.value().substring(0, lettersTyped));
            font.draw(GameApp.getSpriteBatch(), layout, Math.round(left + dx), Math.round(top + dy));
        }
    }

    /** The colour of "Forget" now: moving smoothly to the next red after each #. */
    private Color wordColor() {
        float typed = script[stepIndex].value().length() / SLOW_LETTERS_PER_SECOND;
        float sinceLast = stepTimer - typed - FIRST_HASH_PAUSE - (hashesShown - 1) * HASH_DELAY;
        float t = Math.min(1, Math.max(0, sinceLast / RED_FADE_TIME));
        int i = Math.min(hashesShown, WORD_REDS.length) - 1;
        Color from = i == 0 ? TEXT_COLOR : WORD_REDS[i - 1];
        return new Color(from).lerp(WORD_REDS[i], t);
    }

    /** A small random move every frame: the words tremble. */
    private float jitter(float amount) {
        return (shake.nextFloat() * 2 - 1) * amount;
    }

    /** Grenze Gotisch (OFL licence, resources/fonts), smooth. */
    private static BitmapFont gothicFont(int size) {
        FreeTypeFontGenerator generator = new FreeTypeFontGenerator(Gdx.files.internal("fonts/GrenzeGotisch.ttf"));
        FreeTypeFontGenerator.FreeTypeFontParameter parameter = new FreeTypeFontGenerator.FreeTypeFontParameter();
        parameter.size = size;
        parameter.color = Color.WHITE;
        parameter.minFilter = Texture.TextureFilter.Linear;
        parameter.magFilter = Texture.TextureFilter.Linear;
        BitmapFont font = generator.generateFont(parameter);
        generator.dispose();
        return font;
    }
}