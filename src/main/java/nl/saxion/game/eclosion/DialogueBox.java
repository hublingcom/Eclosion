package nl.saxion.game.eclosion;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import nl.saxion.gameapp.GameApp;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * DIALOGUE BOX - visual-novel style dialogue you can put on top of ANY screen.
 *
 *  - big standing characters on the left / right; the one speaking is bright and in front,
 *    the others are darker and step back a little
 *  - wide see-through text box with a name tab, typewriter text and a soft "blip" voice
 *  - LOG button (read earlier lines) and SKIP button (fast-forward until the next choice)
 *  - choices that change what comes next
 *  - Hal's real name "Souichi" is censored automatically (story bible 5.7)
 *
 * How to use (inside a screen):
 *
 *   DialogueBox dialogue = new DialogueBox();
 *
 *   show():   dialogue.load();
 *             dialogue.addCharacter("Ageha", "images/ageha_stand.png", DialogueBox.LEFT);
 *             dialogue.addExpression("Ageha", "angry", "images/ageha_stand_angry.png");   // optional
 *             dialogue.addCharacter("Bluma", "images/Bluma_base.png", DialogueBox.RIGHT);
 *             dialogue.say("Bluma", "Good morning!");
 *             dialogue.say("Ageha", "...Go away.", "angry");      // 3rd parameter = expression
 *             dialogue.ask("Ageha", "...", "Fine.", "No.");       // choices
 *             dialogue.start();
 *
 *   render(): dialogue.update(delta, getMouseX(), getMouseY());
 *             if (!dialogue.isActive()) { ...normal game input... }
 *             ...draw your screen...
 *             dialogue.draw();                                  // ALWAYS last
 *             dialogue.getLastChoice()                          // 0, 1, ... after an ask()
 *
 *   hide():   dialogue.dispose();
 *
 * Controls: ENTER / SPACE / left click = next (first press finishes the line)
 *           UP/DOWN + ENTER = choice      L = log      hold CTRL = skip
 *
 * Images are optional: a character without an image just has no standing picture.
 * Needs: fonts/basic.ttf, audio/blip.wav (optional)
 */
public class DialogueBox {

    public static final int LEFT = 0, RIGHT = 1;

    // ---------- data ----------
    static class Line {
        String speaker, text, expression;
        String[] choices;
    }

    static class Character {
        String name;
        int side;
        HashMap<String, String> textures = new HashMap<>();   // expression -> texture key
        String current = "default";
        boolean visible = false;
        float focus = 0;      // 0 = in the background, 1 = speaking (animated)
    }

    // ---------- layout (world 1280 x 720) ----------
    static final float BOX_X = 50, BOX_Y = 20, BOX_W = 1180, BOX_H = 200;
    static final float CHAR_H = 560;                       // height of a standing character
    static final float LEFT_X = 300, RIGHT_X = 980;        // center x of the two character spots
    static final float CHARS_PER_SECOND = 45;

    // buttons on top of the box (right side)
    static final float BTN_W = 96, BTN_H = 34, BTN_Y = BOX_Y + BOX_H + 8;
    static final float LOG_X = BOX_X + BOX_W - 2 * BTN_W - 12, SKIP_X = BOX_X + BOX_W - BTN_W;

    ArrayList<Line> lines = new ArrayList<>();
    ArrayList<String> history = new ArrayList<>();         // for the log
    HashMap<String, Character> characters = new HashMap<>();
    HashMap<String, Float> voicePitch = new HashMap<>();
    HashMap<String, Color> nameColor = new HashMap<>();
    ArrayList<String> loadedTextures = new ArrayList<>();

    int index = 0;
    boolean active = false;
    float typed = 0;
    int lastBlip = 0;
    int choiceCursor = 0;
    int lastChoice = -1;
    float openAnim = 0;
    boolean showLog = false;
    boolean skipping = false;
    float skipTimer = 0;
    float clock = 0;
    boolean hasBlip = false;

    // ======================= SETUP =======================

    public void load() {
        GameApp.addFont("dlg_text", "fonts/basic.ttf", 30);
        GameApp.addFont("dlg_name", "fonts/basic.ttf", 28);
        GameApp.addFont("dlg_small", "fonts/basic.ttf", 19);
        GameApp.addFont("dlg_log", "fonts/basic.ttf", 24);
        hasBlip = Gdx.files.internal("audio/blip.wav").exists();
        if (hasBlip) GameApp.addSound("dlg_blip", "audio/blip.wav");
        voicePitch.put("Ageha", 0.75f);
        nameColor.put("Ageha", new Color(0.82f, 0.82f, 0.86f, 1));
    }

    /** Register a character with a standing picture on the LEFT or RIGHT. */
    public void addCharacter(String name, String imagePath, int side) {
        Character c = new Character();
        c.name = name;
        c.side = side;
        characters.put(name, c);
        addExpression(name, "default", imagePath);
    }

    /** Extra picture for the same character (e.g. "angry", "smile"). Missing files are ignored. */
    public void addExpression(String name, String expression, String imagePath) {
        Character c = characters.get(name);
        if (c == null || !Gdx.files.internal(imagePath).exists()) return;
        String key = "dlg_c_" + name + "_" + expression;
        GameApp.addTexture(key, imagePath);
        loadedTextures.add(key);
        c.textures.put(expression, key);
    }

    public void setVoicePitch(String speaker, float pitch) { voicePitch.put(speaker, pitch); }
    public void setNameColor(String speaker, Color c) { nameColor.put(speaker, c); }

    public void say(String speaker, String text) { say(speaker, text, null); }

    public void say(String speaker, String text, String expression) {
        Line l = new Line();
        l.speaker = speaker;
        l.text = censor(text);
        l.expression = expression;
        lines.add(l);
    }

    public void ask(String speaker, String text, String... choices) {
        say(speaker, text, null);
        lines.get(lines.size() - 1).choices = choices;
    }

    public void start() {
        if (lines.isEmpty()) return;
        index = 0;
        active = true;
        openAnim = 0;
        beginLine();
    }

    /** Remove queued lines (keeps characters and the log). Use before queuing the next part. */
    public void clear() {
        lines.clear();
        index = 0;
        active = false;
    }

    public boolean isActive() { return active; }
    public int getLastChoice() { return lastChoice; }

    /** name of whoever is talking right now (or null) */
    public String currentSpeaker() { return active ? lines.get(index).speaker : null; }

    String censor(String text) { return text.replace("Souichi", "S######"); }

    void beginLine() {
        typed = 0;
        lastBlip = 0;
        choiceCursor = 0;
        Line l = lines.get(index);
        Character c = characters.get(l.speaker);
        if (c != null) {
            c.visible = true;
            if (l.expression != null && c.textures.containsKey(l.expression)) c.current = l.expression;
        }
    }

    // ======================= UPDATE =======================

    /** Keyboard only (LOG / SKIP buttons can't be clicked). */
    public void update(float delta) {
        update(delta, -1000, -1000);
    }

    /** Pass the screen's getMouseX() and getMouseY() so the buttons and choices can be clicked. */
    public void update(float delta, float mouseX, float mouseY) {
        mx = mouseX;
        my = mouseY;
        clock += delta;
        if (!active) return;
        openAnim = Math.min(1f, openAnim + delta * 5);

        // characters slide forward / back smoothly
        String speaker = lines.get(index).speaker;
        for (Character c : characters.values()) {
            float target = c.name.equals(speaker) ? 1f : 0f;
            c.focus += (target - c.focus) * Math.min(1f, delta * 8);
        }

        // ---------- log ----------
        if (GameApp.isKeyJustPressed(Input.Keys.L) || clickedButton(LOG_X)) {
            showLog = !showLog;
            return;
        }
        if (showLog) {
            if (GameApp.isKeyJustPressed(Input.Keys.ESCAPE) || GameApp.isButtonJustPressed(Input.Buttons.LEFT)
                    || GameApp.isButtonJustPressed(Input.Buttons.RIGHT)) {
                showLog = false;
            }
            return;
        }

        // ---------- skip ----------
        if (clickedButton(SKIP_X)) skipping = !skipping;
        boolean fast = skipping || GameApp.isKeyPressed(Input.Keys.CONTROL_LEFT);

        Line line = lines.get(index);
        int before = (int) typed;
        typed = Math.min(line.text.length(), typed + CHARS_PER_SECOND * delta * (fast ? 6 : 1));
        boolean finishedTyping = typed >= line.text.length();

        int now = (int) typed;
        if (hasBlip && !fast && now != before && now / 2 != lastBlip) {
            lastBlip = now / 2;
            char ch = line.text.charAt(Math.max(0, now - 1));
            if (ch != ' ' && ch != '.') {
                float p = voicePitch.getOrDefault(line.speaker, 1.25f);
                GameApp.getSound("dlg_blip").play(0.35f, p * GameApp.random(0.95f, 1.05f), 0f);
            }
        }

        // ---------- choices: skipping always stops here ----------
        if (line.choices != null) {
            skipping = false;
            if (!finishedTyping) {
                if (pressedNext()) typed = line.text.length();
                return;
            }
            if (GameApp.isKeyJustPressed(Input.Keys.UP) || GameApp.isKeyJustPressed(Input.Keys.W)) {
                choiceCursor = (choiceCursor + line.choices.length - 1) % line.choices.length;
            }
            if (GameApp.isKeyJustPressed(Input.Keys.DOWN) || GameApp.isKeyJustPressed(Input.Keys.S)) {
                choiceCursor = (choiceCursor + 1) % line.choices.length;
            }
            int hovered = hoveredChoice(line);
            if (hovered >= 0) choiceCursor = hovered;
            if (GameApp.isKeyJustPressed(Input.Keys.ENTER) || GameApp.isKeyJustPressed(Input.Keys.SPACE)
                    || (hovered >= 0 && GameApp.isButtonJustPressed(Input.Buttons.LEFT))) {
                lastChoice = choiceCursor;
                history.add("  > " + line.choices[choiceCursor]);
                nextLine();
            }
            return;
        }

        if (fast) {
            skipTimer += delta;
            if (finishedTyping && skipTimer > 0.12f) { skipTimer = 0; nextLine(); }
            return;
        }

        if (pressedNext()) {
            if (!finishedTyping) typed = line.text.length();
            else nextLine();
        }
    }

    boolean pressedNext() {
        boolean click = GameApp.isButtonJustPressed(Input.Buttons.LEFT) && !overButton(LOG_X) && !overButton(SKIP_X);
        return click || GameApp.isKeyJustPressed(Input.Keys.ENTER) || GameApp.isKeyJustPressed(Input.Keys.SPACE);
    }

    void nextLine() {
        Line l = lines.get(index);
        history.add((l.speaker == null || l.speaker.isEmpty() ? "" : l.speaker + ":  ") + l.text);
        index++;
        if (index >= lines.size()) {
            active = false;
            skipping = false;
        } else {
            beginLine();
        }
    }

    // ---------- mouse helpers (world coordinates, passed in from the screen) ----------
    float mx = -1000, my = -1000;
    float mouseX() { return mx; }
    float mouseY() { return my; }

    boolean overButton(float x) { return GameApp.pointInRect(mouseX(), mouseY(), x, BTN_Y, BTN_W, BTN_H); }
    boolean clickedButton(float x) { return GameApp.isButtonJustPressed(Input.Buttons.LEFT) && overButton(x); }

    float choiceY(int i, int count) { return BOX_Y + BOX_H + 60 + (count - 1 - i) * 62; }
    static final float CHOICE_W = 520, CHOICE_H = 50, CHOICE_X = 640 - 260;

    int hoveredChoice(Line line) {
        for (int i = 0; i < line.choices.length; i++) {
            if (GameApp.pointInRect(mouseX(), mouseY(), CHOICE_X, choiceY(i, line.choices.length), CHOICE_W, CHOICE_H)) return i;
        }
        return -1;
    }

    // ======================= DRAW =======================

    /** Call this LAST in your render(), after everything else is drawn. */
    public void draw() {
        if (!active) return;
        Line line = lines.get(index);
        float a = openAnim;

        drawCharacters();

        // ---------- box, name tab, buttons ----------
        GameApp.enableTransparency();
        GameApp.startShapeRenderingFilled();
        GameApp.drawRoundedRect(BOX_X - 2, BOX_Y - 2, BOX_W + 4, BOX_H + 4, 18, new Color(0.75f, 0.72f, 0.7f, 0.55f * a));
        GameApp.drawRoundedRect(BOX_X, BOX_Y, BOX_W, BOX_H, 16, new Color(0.04f, 0.04f, 0.06f, 0.84f * a));
        GameApp.drawRect(BOX_X + 30, BOX_Y + BOX_H - 62, BOX_W - 60, 1, new Color(1, 1, 1, 0.08f * a)); // thin line under the name

        boolean hasName = line.speaker != null && !line.speaker.isEmpty();
        if (hasName) {
            float nw = GameApp.getTextWidth("dlg_name", line.speaker) + 56;
            Color accent = nameColor.getOrDefault(line.speaker, new Color(0.75f, 0.68f, 0.85f, 1));
            GameApp.drawRoundedRect(BOX_X + 28, BOX_Y + BOX_H - 24, nw, 50, 12, new Color(0.13f, 0.12f, 0.16f, 0.97f * a));
            GameApp.drawRect(BOX_X + 40, BOX_Y + BOX_H - 14, 4, 30, new Color(accent.r, accent.g, accent.b, a));
        }
        drawButton(LOG_X, overButton(LOG_X) || showLog);
        drawButton(SKIP_X, overButton(SKIP_X) || skipping);
        if (line.choices != null && typed >= line.text.length()) {
            for (int i = 0; i < line.choices.length; i++) {
                boolean sel = i == choiceCursor;
                GameApp.drawRoundedRect(CHOICE_X - 2, choiceY(i, line.choices.length) - 2, CHOICE_W + 4, CHOICE_H + 4, 14, new Color(0.8f, 0.78f, 0.75f, sel ? 0.9f : 0.35f));
                GameApp.drawRoundedRect(CHOICE_X, choiceY(i, line.choices.length), CHOICE_W, CHOICE_H, 12,
                        sel ? new Color(0.9f, 0.88f, 0.86f, 0.96f) : new Color(0.05f, 0.05f, 0.07f, 0.88f));
            }
        }
        GameApp.endShapeRendering();

        // ---------- texts ----------
        GameApp.startSpriteRendering();
        if (hasName) {
            GameApp.drawTextVerticallyCentered("dlg_name", line.speaker, BOX_X + 56, BOX_Y + BOX_H + 1, "gray-100");
        }
        GameApp.drawTextCentered("dlg_small", "LOG", LOG_X + BTN_W / 2, BTN_Y + BTN_H / 2, showLog ? "black" : "gray-300");
        GameApp.drawTextCentered("dlg_small", skipping ? "SKIP >>" : "SKIP", SKIP_X + BTN_W / 2, BTN_Y + BTN_H / 2, skipping ? "black" : "gray-300");

        String visible = line.text.substring(0, (int) typed);
        ArrayList<String> wrapped = wrap(visible, "dlg_text", BOX_W - 120);
        for (int i = 0; i < wrapped.size(); i++) {
            GameApp.drawTextVerticallyCentered("dlg_text", wrapped.get(i), BOX_X + 56, BOX_Y + BOX_H - 92 - i * 42, "white");
        }
        if (line.choices != null && typed >= line.text.length()) {
            for (int i = 0; i < line.choices.length; i++) {
                boolean sel = i == choiceCursor;
                GameApp.drawTextCentered("dlg_text", line.choices[i], 640, choiceY(i, line.choices.length) + CHOICE_H / 2, sel ? "black" : "gray-200");
            }
        } else if (typed >= line.text.length()) {
            float blink = 0.35f + 0.45f * (float) Math.abs(Math.sin(clock * 3));
            GameApp.drawTextVerticallyCentered("dlg_small", "v", BOX_X + BOX_W - 50, BOX_Y + 30 + (float) Math.sin(clock * 5) * 3, new Color(0.9f, 0.9f, 0.9f, blink));
        }
        GameApp.endSpriteRendering();

        if (showLog) drawLog();
    }

    void drawButton(float x, boolean on) {
        GameApp.drawRoundedRect(x, BTN_Y, BTN_W, BTN_H, 10, on ? new Color(0.85f, 0.83f, 0.8f, 0.95f) : new Color(0.08f, 0.08f, 0.1f, 0.85f));
    }

    void drawCharacters() {
        SpriteBatch batch = GameApp.getSpriteBatch();
        GameApp.startSpriteRendering();
        // background characters first, the speaker last (in front)
        for (int pass = 0; pass < 2; pass++) {
            for (Character c : characters.values()) {
                if (!c.visible) continue;
                boolean speaking = c.focus > 0.5f;
                if ((pass == 0) == speaking) continue;
                String key = c.textures.get(c.current);
                if (key == null) key = c.textures.get("default");
                if (key == null) continue;

                float tw = GameApp.getTextureWidth(key), th = GameApp.getTextureHeight(key);
                float scale = 0.94f + 0.06f * c.focus;
                float h = CHAR_H * scale, w = h * tw / th;
                float cx = (c.side == LEFT ? LEFT_X : RIGHT_X) + (c.side == LEFT ? -1 : 1) * 25 * (1 - c.focus);
                float y = -10 * (1 - c.focus) + (speaking ? (float) Math.sin(clock * 2) * 2 : 0);   // speaker "breathes"
                float shade = 0.42f + 0.58f * c.focus;                                              // others are darker
                batch.setColor(shade, shade, shade * 1.03f, openAnim);
                GameApp.drawTexture(key, cx - w / 2, y, w, h);
                batch.setColor(Color.WHITE);
            }
        }
        GameApp.endSpriteRendering();
    }

    void drawLog() {
        GameApp.enableTransparency();
        GameApp.startShapeRenderingFilled();
        GameApp.drawRect(0, 0, 1280, 720, new Color(0, 0, 0, 0.88f));
        GameApp.endShapeRendering();
        GameApp.startSpriteRendering();
        GameApp.drawTextCentered("dlg_name", "LOG", 640, 680, "gray-200");
        // newest at the bottom, as many as fit on screen
        ArrayList<String> rows = new ArrayList<>();
        for (String h : history) rows.addAll(wrap(h, "dlg_log", 1080));
        int maxRows = 15;
        int start = Math.max(0, rows.size() - maxRows);
        for (int i = start; i < rows.size(); i++) {
            GameApp.drawTextVerticallyCentered("dlg_log", rows.get(i), 100, 620 - (i - start) * 38, "gray-300");
        }
        GameApp.drawTextCentered("dlg_small", "L / click to close", 640, 30, "gray-500");
        GameApp.endSpriteRendering();
    }

    ArrayList<String> wrap(String text, String font, float maxWidth) {
        ArrayList<String> out = new ArrayList<>();
        String current = "";
        for (String word : text.split(" ", -1)) {
            String test = current.isEmpty() ? word : current + " " + word;
            if (GameApp.getTextWidth(font, test) > maxWidth && !current.isEmpty()) {
                out.add(current);
                current = word;
            } else {
                current = test;
            }
        }
        out.add(current);
        return out;
    }

    public void dispose() {
        GameApp.disposeFont("dlg_text");
        GameApp.disposeFont("dlg_name");
        GameApp.disposeFont("dlg_small");
        GameApp.disposeFont("dlg_log");
        if (hasBlip) GameApp.disposeSound("dlg_blip");
        for (String k : loadedTextures) GameApp.disposeTexture(k);
        loadedTextures.clear();
        characters.clear();
        history.clear();
    }
}
