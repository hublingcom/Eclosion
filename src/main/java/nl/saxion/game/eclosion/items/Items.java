package nl.saxion.game.eclosion.items;

import java.util.LinkedHashSet;
import java.util.Set;
import nl.saxion.gameapp.GameApp;

/**
 * The memory items Ageha finds (Dawon). One per floor; the true ending needs all of them.
 *
 *   Items.give(Items.HAIR_TIE);     // when the player earns it
 *   Items.has(Items.HAIR_TIE);
 *
 * The collection (I in the rooms) shows SLOTS places: found items with their picture, the rest as "?".
 * Pictures: resources/textures/items/<id>.png
 */
public final class Items {

    public static final class Item {
        public final String id;            // also the texture name, item_<id>
        public final String name;
        public final String floor;         // where it was found
        public final String description;

        Item(String id, String name, String floor, String description) {
            this.id = id;
            this.name = name;
            this.floor = floor;
            this.description = description;
        }

        public String texture() {
            return "item_" + id;
        }

        /** Loads the picture (once). */
        public void load() {
            if (!loaded.contains(id)) {
                GameApp.addTexture(texture(), "textures/items/" + id + ".png");
                loaded.add(id);
            }
        }
    }

    public static final Item HAIR_TIE = new Item("hair_tie", "Hair tie", "Floor 1 - Loneliness",
            "Caught in the petals of the Queen of the Night.");

    /** All items in floor order (add the next floors' items here). */
    public static final Item[] ALL = {HAIR_TIE};

    /** Places in the collection: one per floor. */
    public static final int SLOTS = 5;

    private static final Set<String> owned = new LinkedHashSet<>();
    private static final Set<String> loaded = new LinkedHashSet<>();

    private Items() {
    }

    public static void give(Item item) {
        owned.add(item.id);
    }

    public static boolean has(Item item) {
        return owned.contains(item.id);
    }

    public static int count() {
        return owned.size();
    }

    /** New game: nothing found yet. */
    public static void reset() {
        owned.clear();
    }

    /** The item in this collection place, or null if there is none (yet). */
    public static Item inSlot(int slot) {
        return slot < ALL.length ? ALL[slot] : null;
    }
}
