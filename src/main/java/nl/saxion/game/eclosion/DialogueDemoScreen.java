package nl.saxion.game.eclosion;

import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import nl.saxion.gameapp.GameApp;
import nl.saxion.gameapp.screens.ScalableGameScreen;

/**
 * DIALOGUE DEMO - Day 1: the ghosts wake Ageha up and offer him the deal (story bible 1.1 / 2.8).
 * Shows how to use DialogueBox, including a choice that changes what is said next.
 *
 * !! The lines are PLACEHOLDERS. Kaan (writer) should replace them with the real script. !!
 *
 * Images (optional, the demo works without them):
 *   images/room.png, images/ageha_down_idle.png,
 *   images/Ageha_face_06_half_lidded.png, images/Ageha_face_07_serious.png, images/Bluma_base.png
 */
public class DialogueDemoScreen extends ScalableGameScreen {

    static final float W = 1280, H = 720;

    DialogueBox dialogue = new DialogueBox();
    int part;            // 0 = until the choice, 1 = answer to the choice, 2 = done
    float time;
    boolean hasRoom, hasAgeha;

    // ghosts floating around the room
    float[] gx = {380, 520, 760, 900, 640};
    float[] gy = {430, 520, 540, 420, 600};

    public DialogueDemoScreen() {
        super(1280, 720);
    }

    @Override
    public void show() {
        hasRoom = com.badlogic.gdx.Gdx.files.internal("images/room.png").exists();
        hasAgeha = com.badlogic.gdx.Gdx.files.internal("images/ageha_down_idle.png").exists();
        if (hasRoom) GameApp.addTexture("demo_room", "images/room.png");
        GameApp.addFont("demo_small", "fonts/basic.ttf", 24);

        dialogue.load();
        // standing pictures (placeholders until Chidera draws real standing art)
        dialogue.addCharacter("Ageha", "images/ageha_down_idle.png", DialogueBox.LEFT);
        dialogue.addCharacter("Bluma", "images/Bluma_base.png", DialogueBox.RIGHT);
        dialogue.setNameColor("Bluma", new com.badlogic.gdx.graphics.Color(0.95f, 0.85f, 0.45f, 1));
        dialogue.setVoicePitch("Bluma", 1.35f);
        dialogue.setVoicePitch("Ghosts", 1.6f);
        dialogue.setVoicePitch("???", 1.5f);

        part = 0;
        time = 0;
        dialogue.clear();
        dialogue.say("???", "AGEHA! You're awake! You're AWAKE!");
        dialogue.say("Ageha", "...Unfortunately.");
        dialogue.say("Bluma", "Seven days until your birthday! We counted! On the wall!");
        dialogue.say("Ghosts", "SEVEN!!");
        dialogue.say("Ageha", "Get out of my room.");
        dialogue.say("Bluma", "We will! Forever and ever! ...If you play our game first.");
        dialogue.say("Ageha", "...What game.");
        dialogue.say("Bluma", "One floor of the house, every day, for seven days. Clear them all and we leave you alone. Promise.");
        dialogue.ask("Ageha", "...", "Fine. Whatever.", "And if I say no?");
        dialogue.start();
    }

    @Override
    public void render(float delta) {
        super.render(delta);
        time += delta;
        dialogue.update(delta, getMouseX(), getMouseY());

        // when one part of the conversation is over, queue the next part
        if (!dialogue.isActive()) {
            if (part == 0) {
                dialogue.clear();
                if (dialogue.getLastChoice() == 1) {
                    dialogue.say("Bluma", "Then we sing. Every night. Very, very loudly.");
                    dialogue.say("Ghosts", "La la LAAA!");
                    dialogue.say("Ageha", "...Fine.");
                }
                dialogue.say("Bluma", "DEAL!! Floor one opens tomorrow!");
                dialogue.say("Ghosts", "Good night, Ageha!");
                dialogue.say("Ageha", "It's morning.");
                dialogue.start();
                part = 1;
            } else if (part == 1) {
                part = 2;
            }
        }
        if (part == 2 && GameApp.isKeyJustPressed(Input.Keys.ENTER)) {
            GameApp.switchScreen("TitleScreen");
            return;
        }

        draw();
    }

    void draw() {
        GameApp.clearScreen(0, 0, 0);
        GameApp.startSpriteRendering();
        if (hasRoom) {
            float scale = H / 902f;
            float rw = 1600 * scale;
            GameApp.drawTexture("demo_room", (W - rw) / 2, 0, rw, H);
        }
        GameApp.endSpriteRendering();

        // ghosts: soft white blobs floating around him (they bounce when they all shout)
        boolean shouting = dialogue.isActive() && "Ghosts".equals(dialogue.lines.get(dialogue.index).speaker);
        GameApp.enableTransparency();
        GameApp.startShapeRenderingFilled();
        GameApp.drawRect(0, 0, W, H, new Color(0, 0, 0, 0.3f));
        for (int i = 0; i < gx.length; i++) {
            float bob = (float) Math.sin(time * 2 + i * 1.7f) * 10 + (shouting ? (float) Math.abs(Math.sin(time * 12 + i)) * 18 : 0);
            float x = gx[i] + (float) Math.sin(time * 0.6f + i) * 25, y = gy[i] + bob;
            Color body = new Color(1, 1, 1, 0.45f);
            GameApp.drawCircle(x, y, 24, body);
            GameApp.drawRect(x - 24, y - 26, 48, 26, body);
            GameApp.drawCircle(x - 8, y + 3, 3.5f, new Color(0, 0, 0, 0.8f));
            GameApp.drawCircle(x + 8, y + 3, 3.5f, new Color(0, 0, 0, 0.8f));
        }
        GameApp.endShapeRendering();

        if (part == 2) {
            GameApp.startSpriteRendering();
            GameApp.drawTextCentered("demo_small", "End of dialogue demo - press ENTER", W / 2, 60, "gray-300");
            GameApp.endSpriteRendering();
        }

        dialogue.draw();     // always last
    }

    @Override
    public void hide() {
        if (hasRoom) GameApp.disposeTexture("demo_room");
        GameApp.disposeFont("demo_small");
        dialogue.dispose();
    }
}
