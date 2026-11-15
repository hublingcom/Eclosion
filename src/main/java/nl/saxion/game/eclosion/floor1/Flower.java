package nl.saxion.game.eclosion.floor1;

/**
 * One falling Queen of the Night flower.
 * x, y = center of the flower (LibGDX: y goes UP, so falling = y decreases).
 */
public class Flower {
    float x;
    float y;
    float startX;      // x where it spawned (used for the side-to-side sway)
    float speed;       // pixels per second downward
    float swayTime;    // own timer so every flower sways differently
    float swayAmount;  // how far it swings left/right
    float radius;
    float rotation;    // petals slowly turn while falling
    boolean wilted = false;  // withered flower: catching it costs points
    boolean caught = false;
    boolean missed = false;
}