package nl.saxion.game.eclosion.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.math.Rectangle;
import nl.saxion.gameapp.GameApp;
import nl.saxion.gameapp.screens.ScalableGameScreen;
import nl.saxion.game.eclosion.audio.Bgm;
import nl.saxion.game.eclosion.audio.Sfx;
import nl.saxion.game.eclosion.explore.ExploreScreen;

/**
 * The first screen: black, the game title in big letters, music.
 *
 *   1. "Press any key" (any key or a click)
 *   2. the menu: New Game / Continue / Settings / Exit
 *      - New Game: fades out (music too) and the game starts in Ageha's room
 *      - Continue: greyed out until there is a save system
 *      - Settings: music volume, sound effects volume, fullscreen
 *      - Exit: closes the game
 *
 * Controls: Up/Down or W/S, Enter/Space/E to choose, Left/Right or A/D to change a setting,
 * Esc = back. The mouse works too (point and click).
 * No background picture for now.
 */
public class TitleScreen extends ScalableGameScreen {
    /**
     * The game title "Eclosion", written in Shippori Mincho (SIL Open Font License), saved as a picture
     * (textures/ui/title_eclosion.png, drawn at 2x size so it stays sharp).
     */
    private static final String TITLE_IMAGE = "title_eclosion";
    private static final float TITLE_SCALE = 0.5f;
    private static final String MENU_FONT = "title_menu_font";
    private static final String HINT_FONT = "title_hint_font";
    private static final float MUSIC_BASE = 0.5f;            // the title music at full settings volume
    private static final String FIRST_SCREEN = "AgehaRoomScreen";

    private static final Color SELECTED = new Color(232 / 255f, 228 / 255f, 220 / 255f, 1f);
    private static final Color NORMAL = new Color(140 / 255f, 136 / 255f, 132 / 255f, 1f);
    private static final Color DISABLED = new Color(64 / 255f, 61 / 255f, 66 / 255f, 1f);
    private static final Color MARKER = new Color(150 / 255f, 60 / 255f, 70 / 255f, 1f);         // same red as the dialogue choices

    private static final String[] MENU = {"New Game", "Continue", "Settings", "Exit"};
    private static final boolean[] MENU_ENABLED = {true, false, true, true};   // Continue: no saving yet
    private static final String[] SETTINGS = {"Music", "Sound effects", "Fullscreen", "Back"};

    private enum Page { PRESS_KEY, MENU, SETTINGS }

    private final ScreenFader fader = new ScreenFader();
    private final GlyphLayout layout = new GlyphLayout();
    private Page page;
    private int selected;
    private float time;
    private float menuAlpha;                                  // the menu fades in after "press any key"
    private boolean starting;
    private float lastMouseX = Float.NaN, lastMouseY = Float.NaN;
    private final Rectangle[] rows = new Rectangle[4];       // where each line of the menu is (for the mouse)

    public TitleScreen() {
        super(1280, 720);
    }

    @Override
    public void show() {
        GameApp.addTexture(TITLE_IMAGE, "textures/ui/" + TITLE_IMAGE + ".png");
        GameApp.addFont(MENU_FONT, PixelFont.make(30));
        GameApp.addFont(HINT_FONT, PixelFont.make(24));
        Sfx.load();
        Settings.load();                     // volumes and fullscreen from last time
        Bgm.play(Bgm.TITLE, MUSIC_BASE);
        page = Page.PRESS_KEY;
        selected = 0;
        time = 0;
        menuAlpha = 0;
        starting = false;
        fader.fadeIn();
    }


    @Override
    public void render(float delta) {
        super.render(delta);
        time += delta;
        fader.update(delta);
        if (page != Page.PRESS_KEY) {
            menuAlpha = Math.min(1f, menuAlpha + delta / 0.5f);
        }

        Bgm.update(delta);                      // New Game: the music fades out together with the picture
        if (!starting && !fader.busy()) {
            handleInput();
        }

        draw();
        fader.render(getWorldWidth(), getWorldHeight());
        ClickEffect.updateAndDraw(getMouseX(), getMouseY());

        if (starting && fader.isDoneFadingOut()) {
            GameApp.switchScreen(FIRST_SCREEN);
        }
    }

    // ---------- input ----------

    private void handleInput() {
        if (page == Page.PRESS_KEY) {
            if (Gdx.input.isKeyJustPressed(Input.Keys.ANY_KEY) || GameApp.isButtonJustPressed(Input.Buttons.LEFT)) {
                Sfx.play(Sfx.CONFIRM);
                page = Page.MENU;
                selected = 0;
                lastMouseX = getMouseX();          // so the mouse doesn't pick a line right away
                lastMouseY = getMouseY();
            }
            return;
        }

        String[] lines = page == Page.MENU ? MENU : SETTINGS;
        int before = selected;
        if (GameApp.isKeyJustPressed(Input.Keys.UP) || GameApp.isKeyJustPressed(Input.Keys.W)) {
            selected = nextLine(lines, -1);
        }
        if (GameApp.isKeyJustPressed(Input.Keys.DOWN) || GameApp.isKeyJustPressed(Input.Keys.S)) {
            selected = nextLine(lines, +1);
        }
        // the mouse only picks a line when it moves (so it doesn't fight the keys)
        float mx = getMouseX(), my = getMouseY();
        int hovered = lineAt(mx, my, lines.length);
        if ((mx != lastMouseX || my != lastMouseY) && hovered >= 0 && enabled(hovered)) {
            selected = hovered;
        }
        lastMouseX = mx;
        lastMouseY = my;
        if (selected != before) {
            Sfx.play(Sfx.MOVE);
        }

        boolean choose = GameApp.isKeyJustPressed(Input.Keys.ENTER)
                || GameApp.isKeyJustPressed(Input.Keys.SPACE)
                || GameApp.isKeyJustPressed(Input.Keys.E);
        boolean clicked = GameApp.isButtonJustPressed(Input.Buttons.LEFT) && hovered >= 0 && enabled(hovered);

        if (page == Page.MENU) {
            if (choose || clicked) {
                chooseMenu(selected);
            }
        } else {
            settingsInput(choose, clicked, mx);
        }
    }

    /** The next line up (-1) or down (+1), skipping lines that can't be chosen. */
    private int nextLine(String[] lines, int direction) {
        int i = selected;
        do {
            i = (i + direction + lines.length) % lines.length;
        } while (!enabled(i));
        return i;
    }

    private boolean enabled(int line) {
        return page != Page.MENU || MENU_ENABLED[line];
    }

    private void chooseMenu(int line) {
        switch (line) {
            case 0 -> {                                    // New Game
                ExploreScreen.comeBackFrom(null);          // a new game starts in his bed, not at a door
                Sfx.play(Sfx.CONFIRM);
                Sfx.play(Sfx.TRANSITION);
                starting = true;
                fader.fadeOut();
                Bgm.fadeOut();
            }
            case 2 -> {                                    // Settings
                Sfx.play(Sfx.CONFIRM);
                page = Page.SETTINGS;
                selected = 0;
            }
            case 3 -> GameApp.quit();                      // Exit
            default -> { }                                 // Continue: not possible yet
        }
    }

    private void settingsInput(boolean choose, boolean clicked, float mouseX) {
        if (GameApp.isKeyJustPressed(Input.Keys.ESCAPE) || ((choose || clicked) && selected == 3)) {
            Sfx.play(Sfx.CONFIRM);
            page = Page.MENU;
            selected = 2;                                  // back on "Settings"
            return;
        }
        int change = 0;
        if (GameApp.isKeyJustPressed(Input.Keys.LEFT) || GameApp.isKeyJustPressed(Input.Keys.A)) change = -1;
        if (GameApp.isKeyJustPressed(Input.Keys.RIGHT) || GameApp.isKeyJustPressed(Input.Keys.D)) change = +1;
        if (clicked && selected <= 1) {
            change = mouseX < getWorldWidth() / 2 + 60 ? -1 : +1;   // click the left or right side of the value
        }
        if (selected == 0 && change != 0) {
            Sfx.musicVolume = step(Sfx.musicVolume, change);
            Bgm.applyVolume();
            Settings.save();
            Sfx.play(Sfx.MOVE);
        } else if (selected == 1 && change != 0) {
            Sfx.volume = step(Sfx.volume, change);
            Settings.save();
            Sfx.play(Sfx.MOVE);                            // so you hear how loud it is now
        } else if (selected == 2 && (change != 0 || choose || clicked)) {
            if (GameApp.isFullscreen()) {
                GameApp.switchToWindowedMode(1280, 720);
            } else {
                GameApp.switchToFullscreen();
            }
            Settings.save();
            Sfx.play(Sfx.CONFIRM);
        }
    }

    /** Volume in steps of 10%, between 0% and 100%. */
    private static float step(float value, int change) {
        int percent = Math.round(value * 10) + change;
        return Math.max(0, Math.min(10, percent)) / 10f;
    }

    // ---------- drawing ----------

    private void draw() {
        GameApp.clearScreen("black");
        GameApp.enableTransparency();
        GameApp.startSpriteRendering();
        float centerX = getWorldWidth() / 2;
        float middleY = getWorldHeight() / 2;

        // the title, a little above the middle
        float titleW = GameApp.getTextureWidth(TITLE_IMAGE) * TITLE_SCALE;
        float titleH = GameApp.getTextureHeight(TITLE_IMAGE) * TITLE_SCALE;
        GameApp.drawTexture(TITLE_IMAGE, Math.round(centerX - titleW / 2), Math.round(middleY + 130 - titleH / 2), titleW, titleH);

        if (page == Page.PRESS_KEY) {
            float pulse = 0.35f + 0.45f * (0.5f + 0.5f * (float) Math.sin(time * 2.2f));   // slowly breathes
            drawCentered(HINT_FONT, "Press any key", centerX, middleY - 120, NORMAL, pulse);
        } else {
            String[] lines = page == Page.MENU ? MENU : SETTINGS;
            for (int i = 0; i < lines.length; i++) {
                float y = lineY(i);
                Color color = !enabled(i) ? DISABLED : (i == selected ? SELECTED : NORMAL);
                if (page == Page.MENU || i == 3) {
                    drawCentered(MENU_FONT, lines[i], centerX, y, color, menuAlpha);
                } else {
                    // a setting: name on the left, value on the right
                    drawLeft(MENU_FONT, lines[i], centerX - 200, y, color, menuAlpha);
                    drawRight(MENU_FONT, settingValue(i), centerX + 200, y, color, menuAlpha);
                }
            }
        }
        GameApp.endSpriteRendering();

        // the small red marker in front of the chosen line (like the dialogue choices)
        if (page != Page.PRESS_KEY) {
            String[] lines = page == Page.MENU ? MENU : SETTINGS;
            float markerX = rows(lines.length)[selected].x - 22;
            float markerY = lineY(selected);
            GameApp.startShapeRenderingFilled();
            GameApp.setColor(new Color(MARKER.r, MARKER.g, MARKER.b, menuAlpha));
            GameApp.getShapeRenderer().triangle(markerX, markerY + 7, markerX, markerY - 7, markerX + 10, markerY);
            GameApp.endShapeRendering();
        }
        GameApp.disableTransparency();
    }

    private String settingValue(int line) {
        return switch (line) {
            case 0 -> "<  " + Math.round(Sfx.musicVolume * 100) + "%  >";
            case 1 -> "<  " + Math.round(Sfx.volume * 100) + "%  >";
            default -> GameApp.isFullscreen() ? "<  On  >" : "<  Off  >";
        };
    }

    /** Middle (y) of a line of the menu. */
    private float lineY(int line) {
        return getWorldHeight() / 2 - 60 - line * 52;
    }

    /** The areas of the lines (for the mouse and the marker). */
    private Rectangle[] rows(int count) {
        float centerX = getWorldWidth() / 2;
        for (int i = 0; i < count; i++) {
            float width;
            if (page == Page.SETTINGS && i < 3) {
                width = 400;                                // a setting is a whole row
            } else {
                layout.setText(GameApp.getFont(MENU_FONT), (page == Page.MENU ? MENU : SETTINGS)[i]);
                width = layout.width;
            }
            rows[i] = new Rectangle(centerX - width / 2, lineY(i) - 22, width, 44);
        }
        return rows;
    }

    private int lineAt(float mx, float my, int count) {
        Rectangle[] r = rows(count);
        for (int i = 0; i < count; i++) {
            Rectangle wide = new Rectangle(r[i].x - 30, r[i].y, r[i].width + 60, r[i].height);   // a bit easier to hit
            if (wide.contains(mx, my)) return i;
        }
        return -1;
    }

    /** Text centred on x, with its middle on y. */
    private void drawCentered(String fontName, String text, float x, float y, Color color, float alpha) {
        BitmapFont font = GameApp.getFont(fontName);
        font.setColor(color.r, color.g, color.b, alpha);
        layout.setText(font, text);
        font.draw(GameApp.getSpriteBatch(), layout, Math.round(x - layout.width / 2), Math.round(y + layout.height / 2));
    }

    private void drawLeft(String fontName, String text, float x, float y, Color color, float alpha) {
        BitmapFont font = GameApp.getFont(fontName);
        font.setColor(color.r, color.g, color.b, alpha);
        layout.setText(font, text);
        font.draw(GameApp.getSpriteBatch(), layout, Math.round(x), Math.round(y + layout.height / 2));
    }

    private void drawRight(String fontName, String text, float x, float y, Color color, float alpha) {
        BitmapFont font = GameApp.getFont(fontName);
        font.setColor(color.r, color.g, color.b, alpha);
        layout.setText(font, text);
        font.draw(GameApp.getSpriteBatch(), layout, Math.round(x - layout.width), Math.round(y + layout.height / 2));
    }

    @Override
    public void hide() {
        // (the music is stopped by the next screen when it plays its own)
        GameApp.disposeTexture(TITLE_IMAGE);
        GameApp.disposeFont(MENU_FONT);
        GameApp.disposeFont(HINT_FONT);
    }
}
