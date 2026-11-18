package nl.saxion.game.eclosion.ui;

import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.math.Rectangle;
import nl.saxion.game.eclosion.audio.Bgm;
import nl.saxion.game.eclosion.audio.Sfx;
import nl.saxion.gameapp.GameApp;

/**
 * The menu that opens with Esc while walking around: the game stops, the screen goes dark and a
 * small panel shows Resume / Settings / Title Screen / Quit Game (same style as the dialogue box
 * and the title screen). The music goes quieter while it is open.
 *
 * Keys: Up/Down or W/S, Enter/Space/E to choose, Left/Right or A/D to change a setting,
 * Esc = back (on the first page: resume). The mouse works too.
 * Uses the dialogue fonts, so DialogueBox.loadAssets() must have been called.
 */
public class PauseMenu {
    public enum Result { NOTHING, TO_TITLE, QUIT }

    private enum Page { MAIN, SETTINGS, CONFIRM_TITLE, CONFIRM_QUIT }

    private static final String[] MAIN = {"Resume", "Settings", "Title Screen", "Quit Game"};
    private static final String[] SETTINGS = {"Music", "Sound effects", "Fullscreen", "Back"};
    private static final String[] CONFIRM = {"Yes", "No"};

    private static final Color DARKEN = new Color(0, 0, 0, 0.6f);
    private static final Color PANEL_COLOR = new Color(8 / 255f, 8 / 255f, 10 / 255f, 0.94f);
    private static final Color LINE_COLOR = new Color(138 / 255f, 135 / 255f, 144 / 255f, 1f);
    private static final Color INNER_LINE_COLOR = new Color(60 / 255f, 58 / 255f, 66 / 255f, 1f);
    private static final Color SELECTED = new Color(232 / 255f, 228 / 255f, 220 / 255f, 1f);
    private static final Color NORMAL = new Color(140 / 255f, 136 / 255f, 132 / 255f, 1f);
    private static final Color MARKER = new Color(150 / 255f, 60 / 255f, 70 / 255f, 1f);

    private static final float PANEL_W = 420, ROW_H = 44, TITLE_SPACE = 70, BOTTOM_SPACE = 26;

    private final GlyphLayout layout = new GlyphLayout();
    private boolean open = false;
    private Page page = Page.MAIN;
    private int selected = 0;
    private float lastMouseX = Float.NaN, lastMouseY = Float.NaN;

    public boolean isOpen() {
        return open;
    }

    public void open(float mouseX, float mouseY) {
        open = true;
        page = Page.MAIN;
        selected = 0;
        lastMouseX = mouseX;                 // the mouse only picks a line after it moves
        lastMouseY = mouseY;
        Bgm.setDimmed(true);
        Sfx.play(Sfx.CONFIRM);
    }

    private void close() {
        open = false;
        Bgm.setDimmed(false);
    }

    private String[] lines() {
        return switch (page) {
            case MAIN -> MAIN;
            case SETTINGS -> SETTINGS;
            default -> CONFIRM;
        };
    }

    private String title() {
        return switch (page) {
            case MAIN -> "Paused";
            case SETTINGS -> "Settings";
            case CONFIRM_TITLE -> "Return to the title screen?";
            case CONFIRM_QUIT -> "Quit the game?";
        };
    }

    // ---------- input ----------

    /** Handles keys and the mouse. Call every frame while open. */
    public Result update(float mouseX, float mouseY, float worldW, float worldH) {
        if (!open) {
            return Result.NOTHING;
        }
        if (GameApp.isKeyJustPressed(Input.Keys.ESCAPE)) {
            Sfx.play(Sfx.CONFIRM);
            if (page == Page.MAIN) {
                close();                     // Esc again = resume
            } else {
                backToMain();
            }
            return Result.NOTHING;
        }

        String[] lines = lines();
        int before = selected;
        if (GameApp.isKeyJustPressed(Input.Keys.UP) || GameApp.isKeyJustPressed(Input.Keys.W)) {
            selected = (selected - 1 + lines.length) % lines.length;
        }
        if (GameApp.isKeyJustPressed(Input.Keys.DOWN) || GameApp.isKeyJustPressed(Input.Keys.S)) {
            selected = (selected + 1) % lines.length;
        }
        int hovered = lineAt(mouseX, mouseY, worldW, worldH);
        if ((mouseX != lastMouseX || mouseY != lastMouseY) && hovered >= 0) {
            selected = hovered;
        }
        lastMouseX = mouseX;
        lastMouseY = mouseY;
        if (selected != before) {
            Sfx.play(Sfx.MOVE);
        }

        boolean choose = GameApp.isKeyJustPressed(Input.Keys.ENTER) || GameApp.isKeyJustPressed(Input.Keys.SPACE)
                || GameApp.isKeyJustPressed(Input.Keys.E);
        boolean clicked = GameApp.isButtonJustPressed(Input.Buttons.LEFT) && hovered >= 0;

        if (page == Page.SETTINGS) {
            settingsInput(choose, clicked, mouseX, worldW);
            return Result.NOTHING;
        }
        if (!choose && !clicked) {
            return Result.NOTHING;
        }
        Sfx.play(Sfx.CONFIRM);
        if (page == Page.MAIN) {
            switch (selected) {
                case 0 -> close();                                   // Resume
                case 1 -> goTo(Page.SETTINGS);
                case 2 -> goTo(Page.CONFIRM_TITLE);
                default -> goTo(Page.CONFIRM_QUIT);
            }
            return Result.NOTHING;
        }
        // a "Yes / No" page
        if (selected == 0) {
            Result result = page == Page.CONFIRM_TITLE ? Result.TO_TITLE : Result.QUIT;
            close();
            return result;
        }
        backToMain();
        return Result.NOTHING;
    }

    private void goTo(Page next) {
        int from = selected;
        page = next;
        selected = next == Page.SETTINGS ? 0 : 1;   // Yes/No pages start on "No", to be safe
        if (from < 0) selected = 0;
    }

    private void backToMain() {
        selected = switch (page) {
            case SETTINGS -> 1;
            case CONFIRM_TITLE -> 2;
            case CONFIRM_QUIT -> 3;
            default -> 0;
        };
        page = Page.MAIN;
    }

    private void settingsInput(boolean choose, boolean clicked, float mouseX, float worldW) {
        if ((choose || clicked) && selected == 3) {
            Sfx.play(Sfx.CONFIRM);
            backToMain();
            return;
        }
        int change = 0;
        if (GameApp.isKeyJustPressed(Input.Keys.LEFT) || GameApp.isKeyJustPressed(Input.Keys.A)) change = -1;
        if (GameApp.isKeyJustPressed(Input.Keys.RIGHT) || GameApp.isKeyJustPressed(Input.Keys.D)) change = +1;
        if (clicked && selected <= 1) {
            change = mouseX < worldW / 2 + 60 ? -1 : +1;   // click the left or right side of the value
        }
        if (selected == 0 && change != 0) {
            Sfx.musicVolume = step(Sfx.musicVolume, change);
            Bgm.applyVolume();
            Settings.save();
            Sfx.play(Sfx.MOVE);
        } else if (selected == 1 && change != 0) {
            Sfx.volume = step(Sfx.volume, change);
            Settings.save();
            Sfx.play(Sfx.MOVE);
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

    // ---------- layout ----------

    private float panelHeight() {
        return TITLE_SPACE + lines().length * ROW_H + BOTTOM_SPACE;
    }

    private float panelY(float worldH) {
        return worldH / 2 - panelHeight() / 2;
    }

    /** Middle (y) of a line. */
    private float lineY(int line, float worldH) {
        return panelY(worldH) + panelHeight() - TITLE_SPACE - ROW_H / 2 - line * ROW_H;
    }

    private Rectangle row(int line, float worldW, float worldH) {
        return new Rectangle(worldW / 2 - PANEL_W / 2 + 20, lineY(line, worldH) - ROW_H / 2, PANEL_W - 40, ROW_H);
    }

    private int lineAt(float mx, float my, float worldW, float worldH) {
        for (int i = 0; i < lines().length; i++) {
            if (row(i, worldW, worldH).contains(mx, my)) return i;
        }
        return -1;
    }

    // ---------- drawing ----------

    public void render(float worldW, float worldH) {
        if (!open) {
            return;
        }
        float panelX = worldW / 2 - PANEL_W / 2, panelY = panelY(worldH), panelH = panelHeight();
        GameApp.enableTransparency();
        GameApp.startShapeRenderingFilled();
        GameApp.drawRect(-10, -10, worldW + 20, worldH + 20, DARKEN);
        GameApp.drawRect(panelX, panelY, PANEL_W, panelH, PANEL_COLOR);
        GameApp.endShapeRendering();
        GameApp.setLineWidth(1.5f);
        GameApp.startShapeRenderingOutlined();
        GameApp.drawRect(panelX, panelY, PANEL_W, panelH, LINE_COLOR);
        GameApp.drawRect(panelX + 5, panelY + 5, PANEL_W - 10, panelH - 10, INNER_LINE_COLOR);
        GameApp.endShapeRendering();
        GameApp.setLineWidth(1f);

        BitmapFont font = GameApp.getFont(DialogueBox.TEXT_FONT);
        GameApp.startSpriteRendering();
        drawText(font, title(), worldW / 2, panelY + panelH - 36, SELECTED, 0);
        String[] lines = lines();
        for (int i = 0; i < lines.length; i++) {
            Color color = i == selected ? SELECTED : NORMAL;
            float y = lineY(i, worldH);
            if (page == Page.SETTINGS && i < 3) {
                drawText(font, lines[i], panelX + 50, y, color, -1);
                drawText(font, settingValue(i), panelX + PANEL_W - 40, y, color, 1);
            } else {
                drawText(font, lines[i], worldW / 2, y, color, 0);
            }
        }
        GameApp.endSpriteRendering();

        // small red marker in front of the chosen line
        float markerX = page == Page.SETTINGS && selected < 3
                ? panelX + 28
                : worldW / 2 - textWidth(font, lines[selected]) / 2 - 22;
        float markerY = lineY(selected, worldH);
        GameApp.startShapeRenderingFilled();
        GameApp.setColor(MARKER);
        GameApp.getShapeRenderer().triangle(markerX, markerY + 7, markerX, markerY - 7, markerX + 10, markerY);
        GameApp.endShapeRendering();
        GameApp.disableTransparency();
    }

    private String settingValue(int line) {
        return switch (line) {
            case 0 -> "<  " + Math.round(Sfx.musicVolume * 100) + "%  >";
            case 1 -> "<  " + Math.round(Sfx.volume * 100) + "%  >";
            default -> GameApp.isFullscreen() ? "<  On  >" : "<  Off  >";
        };
    }

    private float textWidth(BitmapFont font, String text) {
        layout.setText(font, text);
        return layout.width;
    }

    /** align: -1 = left edge at x, 0 = centred on x, 1 = right edge at x. y = middle of the text. */
    private void drawText(BitmapFont font, String text, float x, float y, Color color, int align) {
        font.setColor(color);
        layout.setText(font, text);
        float left = align < 0 ? x : (align == 0 ? x - layout.width / 2 : x - layout.width);
        font.draw(GameApp.getSpriteBatch(), layout, Math.round(left), Math.round(y + layout.height / 2));
    }
}
