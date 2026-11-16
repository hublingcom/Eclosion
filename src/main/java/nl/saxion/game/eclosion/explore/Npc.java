package nl.saxion.game.eclosion.explore;

import com.badlogic.gdx.graphics.Color;

/**
 * Another character standing in a room (for now they only stand still and turn).
 * They are lit like Ageha, cast a shadow, and you can't walk through them.
 * Made with ExploreScreen.addNpc.
 */
public class Npc {
    /** Ageha's figure fills 311 of the 330 pixels of his frame; other characters are sized to match him. */
    static final float AGEHA_FIGURE = 311f / 330f;

    final String spritePrefix;      // e.g. "bluma_walk" -> textures/sprites/bluma_walk_down_2.png
    final float frameRatio;         // frame width / height
    final float heightFactor;       // drawn height = agehaH * heightFactor
    float x, y;                     // bottom-left of the sprite (world)
    Color tint;

    /** "down", "up", "left" or "right". */
    public String facing;

    Npc(String spritePrefix, float frameW, float frameH, float figureH, float relativeHeight) {
        this.spritePrefix = spritePrefix;
        this.frameRatio = frameW / frameH;
        // same figure height as Ageha (times relativeHeight), whatever padding the frame has
        this.heightFactor = AGEHA_FIGURE * (frameH / figureH) * relativeHeight;
    }
}
