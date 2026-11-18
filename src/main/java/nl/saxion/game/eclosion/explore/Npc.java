package nl.saxion.game.eclosion.explore;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Rectangle;

/**
 * Another character in a room (Bluma, Niri, ...). They are lit like Ageha and cast a shadow.
 * Made with ExploreScreen.addNpc (stands still, blocks Ageha) or addMovingNpc / addNiri
 * (comes and goes, walks with moveNpcToward, fades in and out).
 */
public class Npc {
    /** Ageha's figure fills 311 of the 330 pixels of his frame; other characters are sized to match him. */
    static final float AGEHA_FIGURE = 311f / 330f;

    final String spritePrefix;      // e.g. "bluma_walk" -> textures/sprites/bluma_walk_down_2.png
    final float frameRatio;         // frame width / height
    final float heightFactor;       // drawn height = agehaH * heightFactor
    float x, y;                     // bottom-left of the sprite (world)
    Color tint;
    boolean visible = true;         // a character can come in later (addMovingNpc starts hidden)
    float vx = 0, vy = 0;           // speed while going somewhere (moveNpcToward), for a soft start and stop
    boolean moving = false;         // walking right now (moveNpcToward): the walk frames play
    float walkDistance = 0;         // for the walk frames
    float alpha = 1;                // 0 = invisible ... 1 = fully there (for fading in and out)
    float fadeTo = 1;
    Rectangle block = null;         // the part Ageha can't walk into (null = he can walk through)

    /** "down", "up", "left" or "right". */
    public String facing;

    Npc(String spritePrefix, float frameW, float frameH, float figureH, float relativeHeight) {
        this.spritePrefix = spritePrefix;
        this.frameRatio = frameW / frameH;
        // same figure height as Ageha (times relativeHeight), whatever padding the frame has
        this.heightFactor = AGEHA_FIGURE * (frameH / figureH) * relativeHeight;
    }
}
