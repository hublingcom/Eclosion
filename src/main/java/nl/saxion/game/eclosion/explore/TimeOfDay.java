package nl.saxion.game.eclosion.explore;

/**
 * Day or night, for the whole house.
 *
 * After a minigame it is night: every place is dark except around its lamps, and Ageha has to
 * go to his bed and sleep (E at the bed) before the next day starts.
 * A new game always starts during the day (Main.addStoryScreens calls startDay()).
 */
public final class TimeOfDay {
    private static boolean night = false;

    private TimeOfDay() {
    }

    public static boolean isNight() {
        return night;
    }

    public static void startNight() {
        night = true;
    }

    public static void startDay() {
        night = false;
    }
}
