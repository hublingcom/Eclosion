package nl.saxion.game.eclosion.explore;

import com.badlogic.gdx.math.Rectangle;

/**
 * A piece of furniture cut out of the room picture (textures/props). It is drawn again on top
 * when Ageha walks behind it, so it covers him. Its base line is the front edge on the floor.
 * Made with ExploreScreen.addProp.
 */
class Prop {
    final String texture;
    final Rectangle area;   // where it is drawn (world), exactly over the same spot in the picture
    final float baseY;      // world y of its front edge: characters above this line are behind it

    Prop(String texture, Rectangle area, float baseY) {
        this.texture = texture;
        this.area = area;
        this.baseY = baseY;
    }
}
