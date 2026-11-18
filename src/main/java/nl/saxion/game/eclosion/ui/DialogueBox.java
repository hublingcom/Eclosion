package nl.saxion.game.eclosion.ui;

import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.IntConsumer;
import com.badlogic.gdx.math.Rectangle;
import nl.saxion.gameapp.GameApp;
import nl.saxion.game.eclosion.audio.Sfx;

/**
 * Small dialogue box in the bottom centre, with big portraits standing on the left and right of it.
 * Both portraits stay visible in full colour; the coloured name above the text shows who is speaking.
 * A DialogueLine.together(...) box shows several speakers in the same box (the box grows if needed).
 *
 * Usage in a screen:
 *   show():   DialogueBox.loadAssets();  dialogue.start(line1, line2, ...);
 *   render(): dialogue.update(delta);    ...draw your world...    dialogue.render(getWorldWidth());
 *   hide():   DialogueBox.disposeAssets();
 *
 * Space, Enter, E or a left click: show the whole box at once, or go to the next box.
 */
public class DialogueBox {
    public static final String TEXT_FONT = "dialogue_text";
    public static final String NAME_FONT = "dialogue_name";

    // Colours
    private static final Color BOX_COLOR = new Color(8 / 255f, 8 / 255f, 10 / 255f, 0.9f);
    private static final Color LINE_COLOR = new Color(138 / 255f, 135 / 255f, 144 / 255f, 1f);
    private static final Color INNER_LINE_COLOR = new Color(60 / 255f, 58 / 255f, 66 / 255f, 1f);
    private static final Color TEXT_COLOR = new Color(244 / 255f, 241 / 255f, 234 / 255f, 1f);
    private static final Color NAME_COLOR = new Color(169 / 255f, 163 / 255f, 181 / 255f, 1f);
    private static final Color ARROW_COLOR = new Color(150 / 255f, 60 / 255f, 70 / 255f, 1f);
    private static final Color CHOICE_DIM = new Color(140 / 255f, 136 / 255f, 132 / 255f, 1f);   // options not selected

    // Layout in world units (screens are 1280 x 720)
    public static final float BOX_WIDTH = 560;
    public static final float BOX_HEIGHT = 160;         // smallest height; grows for longer boxes
    private static final float BOX_Y = 24;              // bottom of the box (normal place)
    public static final float BOX_BOTTOM = BOX_Y;
    private static final float TOP_MARGIN = 24;         // space above the box when it is shown at the top

    /** Inner voice (no portraits) is shown at the top of the screen instead, e.g. when Ageha stands behind the box. */
    private boolean atTop = false;

    public void setAtTop(boolean atTop) {
        this.atTop = atTop;
    }
    private static final float PADDING = 24;
    private static final float BOTTOM_SPACE = 30;       // room for the arrow under the text
    private static final float GAP_BETWEEN_SPEAKERS = 10;
    private static final float DEFAULT_PORTRAIT_HEIGHT = 360;
    private static final float PORTRAIT_GAP = 10;       // space between the portrait and the box, so nothing is hidden

    /**
     * Height on screen per character. The key is the start of the texture name, so "ageha"
     * works for ageha_neutral, ageha_smile, ... The numbers are chosen so that all heads are
     * about the same size and taller characters stand higher (Hal is 190 cm).
     */
    private static final Map<String, Float> PORTRAIT_HEIGHTS = new HashMap<>();
    static {
        PORTRAIT_HEIGHTS.put("ageha", 335f);   // ageha_neutral (445 x 584 bust): his head drawn as big as Bluma's
        PORTRAIT_HEIGHTS.put("hal", 430f);
        PORTRAIT_HEIGHTS.put("bluma", 380f);   // bluma_neutral etc. (388 x 585): her head is as big as Ageha's
        PORTRAIT_HEIGHTS.put("niri", 315f);    // niri_happy etc. (383 x 388 bust, two wings): his head about as big as the others
    }

    /** Colour of each character's name in the box. Characters not in this list use NAME_COLOR. */
    private static final Map<String, Color> NAME_COLORS = new HashMap<>();
    static {
        NAME_COLORS.put("Ageha", new Color(176 / 255f, 128 / 255f, 92 / 255f, 1f));   // brown
        NAME_COLORS.put("Hal", new Color(165 / 255f, 42 / 255f, 62 / 255f, 1f));      // bordeaux red
        NAME_COLORS.put("Bluma", new Color(104 / 255f, 150 / 255f, 92 / 255f, 1f));   // green, like her eyes
        NAME_COLORS.put("Niri", new Color(206 / 255f, 166 / 255f, 82 / 255f, 1f));    // golden, like his eyes
    }

    private static final float LETTERS_PER_SECOND = 40;

    // ---------- typing sound ----------
    public static final String BLIP_SOUND = "dialogue_blip";
    private static final int LETTERS_PER_BLIP = 2;          // one blip every 2 letters
    private static final float BLIP_VOLUME = 0.30f;
    private static final float INNER_VOICE_VOLUME = 0.16f;  // Ageha's thoughts are quieter
    /** Each character has their own voice: a higher or lower blip. */
    private static final Map<String, Float> VOICE_PITCH = new HashMap<>();
    static {
        VOICE_PITCH.put("Ageha", 0.95f);
        VOICE_PITCH.put("Hal", 0.78f);      // tallest, lowest voice
        VOICE_PITCH.put("Bluma", 1.18f);
        VOICE_PITCH.put("Niri", 1.0f);      // he has his own sound (VOICE_SOUND)
    }

    /** A character with their own typing sound instead of the normal blip (resources/audio). */
    public static final String NIRI_BLIP = "dialogue_blip_niri";
    private static final Map<String, String> VOICE_SOUND = new HashMap<>();
    static {
        VOICE_SOUND.put("Niri", NIRI_BLIP);  // the same blip as the others, pitched up: young and bright
    }

    // ---------- double click: jump to the end ----------
    private static final long DOUBLE_CLICK_MS = 350;
    private long lastClickTime = 0;

    private final List<DialogueLine> lines = new ArrayList<>();
    private int index = 0;
    private float typeTimer = 0;
    private float blinkTimer = 0;
    private List<List<String>> wrapped = new ArrayList<>();   // rows of text, per speaker in the box
    private int wrappedForIndex = -1;

    private String leftPortrait = null;
    private String rightPortrait = null;

    // Choices shown under the last line (null = no choices)
    private List<String> choices = null;
    private IntConsumer onChoice = null;
    private int selected = 0;
    private final List<Rectangle> choiceAreas = new ArrayList<>();
    private float lastMouseX = Float.NaN, lastMouseY = Float.NaN;

    /**
     * Load the fonts. Retro pixel font (DotGothic16), made without smoothing so the pixels stay sharp.
     * Call this in show() of the screen.
     */
    public static void loadAssets() {
        GameApp.addFont(TEXT_FONT, PixelFont.make(24));
        GameApp.addFont(NAME_FONT, PixelFont.make(24));
        GameApp.addSound(BLIP_SOUND, "audio/dialogue_blip.wav");
        GameApp.addSound(NIRI_BLIP, "audio/dialogue_blip_niri.wav");
    }


    public static void disposeAssets() {
        GameApp.disposeFont(TEXT_FONT);
        GameApp.disposeFont(NAME_FONT);
        GameApp.disposeSound(BLIP_SOUND);
        GameApp.disposeSound(NIRI_BLIP);
    }

    /** Put a portrait on a side before the dialogue starts (null = empty). */
    public void setPortraits(String left, String right) {
        leftPortrait = left;
        rightPortrait = right;
    }

    public void start(DialogueLine... newLines) {
        lines.clear();
        for (DialogueLine line : newLines) {
            lines.add(line);
        }
        index = 0;
        typeTimer = 0;
        wrappedForIndex = -1;
        choices = null;
        onChoice = null;
        applyPortraits();
    }

    /**
     * Call right after start(...): when the last line is shown, these options appear under it.
     * Up/Down (or W/S, or the mouse) picks one, Space/Enter/E (or a click) chooses it.
     * The box then closes and onChoice gets the number of the option (0 = the first).
     */
    public void ask(IntConsumer onChoice, String... options) {
        this.choices = List.of(options);
        this.onChoice = onChoice;
        this.selected = 0;
    }

    public boolean isActive() {
        return index < lines.size();
    }

    /** True while the options are on screen (the last line is fully typed). */
    private boolean showingChoices() {
        return isActive() && choices != null && index == lines.size() - 1 && isBoxFinished();
    }

    public void update(float delta) {
        update(delta, Float.NaN, Float.NaN);
    }

    /** mouseX / mouseY in world units, so the mouse can pick an option. */
    public void update(float delta, float mouseX, float mouseY) {
        if (!isActive()) {
            return;
        }
        int lettersBefore = visibleLetters(lines.get(index).getParts());
        typeTimer += delta;
        blinkTimer += delta;
        playTypingSound(lettersBefore);

        if (showingChoices()) {
            updateChoices(mouseX, mouseY);
            return;
        }

        // Double click: jump straight to the end of the conversation (the last box, fully shown)
        if (GameApp.isButtonJustPressed(Input.Buttons.LEFT)) {
            long now = System.currentTimeMillis();
            boolean doubleClick = now - lastClickTime < DOUBLE_CLICK_MS;
            lastClickTime = doubleClick ? 0 : now;
            if (doubleClick) {
                jumpToEnd();
                return;
            }
        }

        boolean advance = GameApp.isKeyJustPressed(Input.Keys.SPACE)
                || GameApp.isKeyJustPressed(Input.Keys.ENTER)
                || GameApp.isKeyJustPressed(Input.Keys.E)
                || GameApp.isButtonJustPressed(Input.Buttons.LEFT);

        if (advance) {
            if (isBoxFinished()) {
                index++;
                typeTimer = 0;
                blinkTimer = 0;
                applyPortraits();
            } else {
                typeTimer = 9999; // show the whole box at once
            }
        }
    }

    /** Go to the last box and show it completely (portraits change along the way, like reading it). */
    private void jumpToEnd() {
        while (index < lines.size() - 1) {
            index++;
            applyPortraits();
        }
        typeTimer = 9999;
        blinkTimer = 0;
    }

    /** A soft blip while letters appear; each speaker has their own pitch. Silent when a box is shown at once. */
    private void playTypingSound(int lettersBefore) {
        if (!isActive()) {
            return;
        }
        List<DialogueLine> parts = lines.get(index).getParts();
        int lettersNow = visibleLetters(parts);
        if (lettersNow <= lettersBefore || lettersNow - lettersBefore > 3) {
            return;   // nothing new, or the whole box was shown at once
        }
        boolean blipTurn = lettersNow / LETTERS_PER_BLIP != lettersBefore / LETTERS_PER_BLIP;
        int letter = lettersNow - 1;
        for (DialogueLine part : parts) {
            if (letter < part.text.length()) {
                char c = part.text.charAt(letter);
                // every dot of "..." gets its own soft, slightly lower blip (so a silent "..." is still heard);
                // letters only blip on every 2nd one; spaces and other marks stay quiet
                boolean dot = c == '.' || c == '…';
                if (!dot && (!blipTurn || !Character.isLetterOrDigit(c))) {
                    return;
                }
                boolean innerVoice = !hasName(part);
                float pitch = innerVoice ? 1.0f : VOICE_PITCH.getOrDefault(part.speaker, 1.0f);
                float volume = innerVoice ? INNER_VOICE_VOLUME : BLIP_VOLUME;
                if (dot) {
                    pitch *= 0.88f;
                    volume *= 0.8f;
                }
                pitch *= 0.97f + (float) Math.random() * 0.06f;   // tiny variation so it doesn't sound robotic
                String sound = innerVoice ? BLIP_SOUND : VOICE_SOUND.getOrDefault(part.speaker, BLIP_SOUND);
                GameApp.getSound(sound).play(volume * Sfx.volume, pitch, 0f);   // settings volume too
                return;
            }
            letter -= part.text.length();
        }
    }

    private void updateChoices(float mouseX, float mouseY) {
        int count = choices.size();
        int before = selected;
        if (GameApp.isKeyJustPressed(Input.Keys.UP) || GameApp.isKeyJustPressed(Input.Keys.W)) {
            selected = (selected - 1 + count) % count;
        }
        if (GameApp.isKeyJustPressed(Input.Keys.DOWN) || GameApp.isKeyJustPressed(Input.Keys.S)) {
            selected = (selected + 1) % count;
        }
        // the mouse only picks an option when it moves (so it doesn't fight the keys)
        boolean mouseMoved = mouseX != lastMouseX || mouseY != lastMouseY;
        int hovered = -1;
        for (int i = 0; i < choiceAreas.size(); i++) {
            if (choiceAreas.get(i).contains(mouseX, mouseY)) {
                hovered = i;
            }
        }
        if (mouseMoved && hovered >= 0) {
            selected = hovered;
        }
        lastMouseX = mouseX;
        lastMouseY = mouseY;
        if (selected != before) {
            Sfx.play(Sfx.MOVE);            // a soft tick when the marker moves to another option
        }

        boolean choose = GameApp.isKeyJustPressed(Input.Keys.SPACE)
                || GameApp.isKeyJustPressed(Input.Keys.ENTER)
                || GameApp.isKeyJustPressed(Input.Keys.E)
                || (GameApp.isButtonJustPressed(Input.Buttons.LEFT) && hovered >= 0);
        if (choose) {
            Sfx.play(Sfx.CONFIRM);
            int chosen = selected;
            IntConsumer callback = onChoice;
            choices = null;
            onChoice = null;
            index = lines.size();          // the conversation is over
            callback.accept(chosen);       // (this may start a new conversation)
        }
    }

    /** A line with a portrait changes the portrait on its side (for example another expression). */
    private void applyPortraits() {
        if (!isActive()) {
            return;
        }
        for (DialogueLine part : lines.get(index).getParts()) {
            if (part.portrait == null) {
                continue;
            }
            if (part.side == DialogueLine.Side.LEFT) {
                leftPortrait = part.portrait;
            } else if (part.side == DialogueLine.Side.RIGHT) {
                rightPortrait = part.portrait;
            }
        }
    }

    /** The pixel font drawn twice, one pixel apart: thicker letters that are easier to read. */
    private static void drawBold(com.badlogic.gdx.graphics.g2d.BitmapFont font, String text, float x, float y) {
        font.draw(GameApp.getSpriteBatch(), text, x, y);
        font.draw(GameApp.getSpriteBatch(), text, x + 1, y);
    }

    public void render(float worldWidth) {
        render(worldWidth, 720);
    }

    public void render(float worldWidth, float worldHeight) {
        if (!isActive()) {
            return;
        }
        List<DialogueLine> parts = lines.get(index).getParts();
        float boxX = worldWidth / 2 - BOX_WIDTH / 2;
        float textX = boxX + PADDING;
        float textWidth = BOX_WIDTH - 2 * PADDING;

        BitmapFont nameFont = GameApp.getFont(NAME_FONT);
        BitmapFont textFont = GameApp.getFont(TEXT_FONT);
        float nameStep = nameFont.getLineHeight() + 4;
        float rowStep = textFont.getLineHeight() * 1.1f;

        // How tall does the box need to be for all the text?
        List<List<String>> rows = getWrappedRows(parts, textWidth);
        float contentHeight = 0;
        for (int i = 0; i < parts.size(); i++) {
            if (hasName(parts.get(i))) {
                contentHeight += nameStep;
            }
            contentHeight += rows.get(i).size() * rowStep;
            if (i > 0) {
                contentHeight += GAP_BETWEEN_SPEAKERS;
            }
        }
        boolean lastLineWithChoices = choices != null && index == lines.size() - 1;
        if (lastLineWithChoices) {
            // make room for the options from the start, so the box doesn't jump when they appear
            contentHeight += GAP_BETWEEN_SPEAKERS + choices.size() * rowStep;
        }
        float boxHeight = Math.max(BOX_HEIGHT, (PADDING - 6) + contentHeight + BOTTOM_SPACE);
        // normally at the bottom; inner voice can move to the top (portraits only fit next to a box at the bottom)
        boolean showAtTop = atTop && leftPortrait == null && rightPortrait == null;
        float boxY = showAtTop ? worldHeight - TOP_MARGIN - boxHeight : BOX_Y;
        float boxTop = boxY + boxHeight;

        GameApp.enableTransparency();

        // 1. Portraits, standing next to the box (not behind it)
        GameApp.startSpriteRendering();
        if (leftPortrait != null) {
            float h = portraitHeight(leftPortrait);
            float w = portraitWidth(leftPortrait, h);
            GameApp.drawTexture(leftPortrait, boxX - PORTRAIT_GAP - w, 0, w, h);
        }
        if (rightPortrait != null) {
            float h = portraitHeight(rightPortrait);
            float w = portraitWidth(rightPortrait, h);
            GameApp.drawTexture(rightPortrait, boxX + BOX_WIDTH + PORTRAIT_GAP, 0, w, h);
        }
        GameApp.endSpriteRendering();

        // 2. The box: dark band with a thin outer line and a fainter inner line
        GameApp.startShapeRenderingFilled();
        GameApp.drawRect(boxX, boxY, BOX_WIDTH, boxHeight, BOX_COLOR);
        GameApp.endShapeRendering();

        GameApp.setLineWidth(1.5f);
        GameApp.startShapeRenderingOutlined();
        GameApp.drawRect(boxX, boxY, BOX_WIDTH, boxHeight, LINE_COLOR);
        GameApp.drawRect(boxX + 5, boxY + 5, BOX_WIDTH - 10, boxHeight - 10, INNER_LINE_COLOR);
        GameApp.endShapeRendering();
        GameApp.setLineWidth(1f);

        if (isBoxFinished() && !showingChoices() && (blinkTimer % 1f) < 0.6f) {
            // small "next" arrow in the bottom centre
            float ax = boxX + BOX_WIDTH / 2;
            float ay = boxY + 14;
            GameApp.startShapeRenderingFilled();
            GameApp.setColor(ARROW_COLOR);
            GameApp.getShapeRenderer().triangle(ax - 7, ay + 6, ax + 7, ay + 6, ax, ay - 3);
            GameApp.endShapeRendering();
        }

        // 3. Names and text, typed one speaker after the other
        float y = boxTop - PADDING + 6;
        int lettersLeft = visibleLetters(parts);

        GameApp.startSpriteRendering();
        for (int i = 0; i < parts.size(); i++) {
            DialogueLine part = parts.get(i);
            if (i > 0) {
                if (lettersLeft <= 0) {
                    break; // the typing has not reached this speaker yet
                }
                y -= GAP_BETWEEN_SPEAKERS;
            }
            if (hasName(part)) {
                nameFont.setColor(NAME_COLORS.getOrDefault(part.speaker, NAME_COLOR));
                drawBold(nameFont, part.speaker + ":", textX, y);
                y -= nameStep;
            }

            textFont.setColor(TEXT_COLOR);
            int partLeft = Math.min(lettersLeft, part.text.length());
            for (String row : rows.get(i)) {
                if (partLeft <= 0) {
                    break;
                }
                drawBold(textFont, row.substring(0, Math.min(row.length(), partLeft)), textX, y);
                partLeft -= row.length() + 1; // +1 for the space that was removed when wrapping
                y -= rowStep;
            }
            lettersLeft -= part.text.length();
        }

        // 4. Options under the last line, the selected one bright with a small red arrow before it
        choiceAreas.clear();
        float markerY = 0;
        if (showingChoices()) {
            y -= GAP_BETWEEN_SPEAKERS;
            float optionX = textX + 26;
            for (int i = 0; i < choices.size(); i++) {
                textFont.setColor(i == selected ? TEXT_COLOR : CHOICE_DIM);
                drawBold(textFont, choices.get(i), optionX, y);
                float top = y + 4;
                choiceAreas.add(new Rectangle(textX, top - rowStep, textWidth, rowStep));
                if (i == selected) {
                    markerY = top - rowStep / 2;
                }
                y -= rowStep;
            }
        }
        GameApp.endSpriteRendering();

        if (showingChoices()) {
            float mx = textX + 6;
            GameApp.startShapeRenderingFilled();
            GameApp.setColor(ARROW_COLOR);
            GameApp.getShapeRenderer().triangle(mx, markerY + 7, mx, markerY - 7, mx + 10, markerY);
            GameApp.endShapeRendering();
        }
        GameApp.disableTransparency();
    }

    private boolean hasName(DialogueLine part) {
        return part.speaker != null && !part.speaker.isEmpty();
    }

    private float portraitHeight(String texture) {
        for (Map.Entry<String, Float> entry : PORTRAIT_HEIGHTS.entrySet()) {
            if (texture.startsWith(entry.getKey())) {
                return entry.getValue();
            }
        }
        return DEFAULT_PORTRAIT_HEIGHT;
    }

    private float portraitWidth(String texture, float height) {
        float ratio = (float) GameApp.getTextureWidth(texture) / GameApp.getTextureHeight(texture);
        return height * ratio;
    }

    private int totalLetters(List<DialogueLine> parts) {
        int total = 0;
        for (DialogueLine part : parts) {
            total += part.text.length();
        }
        return total;
    }

    private boolean isBoxFinished() {
        List<DialogueLine> parts = lines.get(index).getParts();
        return visibleLetters(parts) >= totalLetters(parts);
    }

    private int visibleLetters(List<DialogueLine> parts) {
        return Math.min(totalLetters(parts), (int) (typeTimer * LETTERS_PER_SECOND));
    }

    /** Splits each speaker's text into rows that fit inside the box (only recalculated when the box changes). */
    private List<List<String>> getWrappedRows(List<DialogueLine> parts, float maxWidth) {
        if (wrappedForIndex == index) {
            return wrapped;
        }
        wrapped = new ArrayList<>();
        for (DialogueLine part : parts) {
            List<String> partRows = new ArrayList<>();
            String current = "";
            for (String word : part.text.split(" ")) {
                String attempt = current.isEmpty() ? word : current + " " + word;
                if (!current.isEmpty() && GameApp.getTextWidth(TEXT_FONT, attempt) > maxWidth) {
                    partRows.add(current);
                    current = word;
                } else {
                    current = attempt;
                }
            }
            partRows.add(current);
            wrapped.add(partRows);
        }
        wrappedForIndex = index;
        return wrapped;
    }
}
