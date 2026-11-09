package Raumschiff;

public final class UpgradeWerte {

    private UpgradeWerte(){}

    //Ohne Upgarde nur ein Schaden, wird um 1 pro Upgrade erhöht
    public static int getDamageValue(int level) {
        return 1 + Math.max(0, level);
    }

    //Ohne Upgarde nur ein Leben, wird um 1 pro Upgrade erhöht
    public static int getHealthValue(int level) {
        return 1 + Math.max(0, level);
    }

    //Ohne Upgrade 15 Asteroiden bei Rundenstart, wird um 4 Asteroiden pro Upgrade erhöht
    public static int getStartingAsteroidCount(int level) {
        return 15 + Math.max(0, level) * 4;
    }

    //Ohne Upgrade spawnt alle 5s ein Asteroid, wird um 0,75s pro Upgarde verringert
    public static double getAsteroidSpawnDelay(int level) {return Math.max(0.5, 5.0 - Math.max(0, level) * 0.75);}

    //Ohne Upgrade 500ms Pause zwischen zwei Schüssen, wird um 50ms pro Upgrade verringert
    public static int getShotCooldownMillis(int level) {
        return Math.max(200, 500 - Math.max(0, level) * 50);
    }

    //Ohne Upgarde 15s Rundendauer, erhöht sich um 5s pro Upgrade
    public static long getRoundDurationMillis(int level) {
        return (15L + Math.max(0, level) * 5L) * 1000L;
    }

    //Formatiert Sekunden ganzzahlig oder mit einer Nachkommastelle für die Anzeige
    public static String formatSeconds(double seconds) {
        if (Math.abs(seconds - Math.rint(seconds)) < 0.05) {
            return String.valueOf((int) Math.rint(seconds));
        }
        return String.format(java.util.Locale.US, "%.1f", seconds);
    }

}
