package Raumschiff;


import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
// Properties speichert Werte in der Form „Schlüssel=Wert“ (z. B. money=0).
import java.util.Properties;


public class SaveManager {
    //Verwaltet drei voneinander unabhängige Spielstände (save1.txt bis save3.txt).
    private static final int SLOT_COUNT = 3;
    // Die Datei master.txt dient als Vorlage für neue oder zurückgesetzte Spielstände.
    private static final String MASTER_FILE = "master.txt";
    // Path stellt den Speicherort der Vorlage als Dateipfad dar.
    private final Path masterPath = Paths.get(MASTER_FILE);

    // Wird beim Erzeugen eines SaveManager-Objekts aufgerufen.
    //Stellt sicher, dass die Vorlage und alle drei Spielstandsdateien existieren.
    public SaveManager() throws IOException {
        ensureMasterFile();
        createMissingSlotsFromMaster();
    }

     //Erstellt die master.txt bei Bedarf und ergänzt fehlende Einträge,
     //falls die Datei bereits vorhanden ist

    private void ensureMasterFile() throws IOException {
        // Existiert noch keine Vorlage, wird sie mit allen Startwerten angelegt.
        if (!Files.exists(masterPath)) {
            Files.writeString(masterPath, buildTemplate());
            return;
        }

        // Vorhandene Werte bleiben erhalten; nur fehlende Schlüssel werden ergänzt.
        Properties properties = loadProperties(masterPath);
        // Merkt sich, ob mindestens ein neuer Eintrag ergänzt wurde.
        boolean changed = false;
        changed |= ensureProperty(properties, "money", "0");
        changed |= ensureProperty(properties, "damageLevel", "0");
        changed |= ensureProperty(properties, "cooldownLevel", "0");
        changed |= ensureProperty(properties, "healthLevel", "0");
        changed |= ensureProperty(properties, "startAsteroidsLevel", "0");
        // Ältere Dateien können noch „spawnLevel“ statt „spawnRateLevel“ verwenden.
        changed |= ensureProperty(properties, "spawnRateLevel", properties.getProperty("spawnLevel", "0"));
        changed |= ensureProperty(properties, "timeLimitLevel", "0");
        // Auch der alte Schlüssel wird für die Kompatibilität beibehalten.
        changed |= ensureProperty(properties, "spawnLevel", properties.getProperty("spawnRateLevel", "0"));
        // Nur dann erneut speichern, wenn tatsächlich etwas geändert wurde.
        if (changed) {
            storeProperties(masterPath, properties, "Raumschiff-Master-Spielstand");
        }
    }

    // Gibt den vollständigen Inhalt einer neuen Vorlage zurück. Alle Werte starten bei 0
    private String buildTemplate() {
        return """
                money=0
                damageLevel=0
                cooldownLevel=0
                healthLevel=0
                startAsteroidsLevel=0
                spawnRateLevel=0
                timeLimitLevel=0
                oreValueLevel=0
                spawnLevel=0
                """;
    }

    //Fügt einen Schlüssel mit seinem Standardwert ein, falls er noch nicht existiert

    private boolean ensureProperty(Properties properties, String key, String value) {
        if (properties.getProperty(key) == null) {
            properties.setProperty(key, value);
            return true;  //true = Eintrag wurde ergänzt
        }
        return false;     //false = Eintrag bereits vorhanden
    }

    // Liest die Schlüssel-Wert-Paare aus einer Textdatei in ein Properties-Objekt
    private Properties loadProperties(Path path) throws IOException {
        Properties properties = new Properties();
        // „try (...)“ schließt den Dateistream automatisch, auch bei einem Fehler.
        try (InputStream input = Files.newInputStream(path)) {
            properties.load(input);
        }
        return properties;
    }

    // Schreibt alle Schlüssel-Wert-Paare in die angegebene Datei
    private void storeProperties(Path path, Properties properties, String comment) throws IOException {
        try (OutputStream output = Files.newOutputStream(path)) {
            // „comment“ wird als Kommentarzeile am Anfang der Datei gespeichert.
            properties.store(output, comment);
        }
    }

    //Erstellt fehlende Dateien save1.txt, save2.txt und save3.txt
    // Bereits vorhandene Spielstände werden dabei nicht überschrieben

    private void createMissingSlotsFromMaster() throws IOException {
        for (int slot = 1; slot <= SLOT_COUNT; slot++) {
            Path slotPath = getSlotPath(slot);
            // Nur anlegen, wenn die Datei für diesen Speicherplatz noch fehlt
            if (!Files.exists(slotPath)) {
                Files.copy(masterPath, slotPath, StandardCopyOption.COPY_ATTRIBUTES);
            }
        }
    }

    // Ermittelt den Dateinamen zu einem Speicherplatz, z. B. 2 -> save2.txt. */
    private Path getSlotPath(int slot) {
        validateSlot(slot);
        return Paths.get("save" + slot + ".txt");
    }

    // Verhindert die Verwendung ungültiger Speicherplätze (nur 1 bis 3 erlaubt). */
    private void validateSlot(int slot) {
        if (slot < 1 || slot > SLOT_COUNT) {
            throw new IllegalArgumentException("Der Spielstand muss zwischen 1 und 3 liegen.");
        }
    }

     //Setzt einen Spielstand zurück, indem die Vorlage darüberkopiert wird.
     //REPLACE_EXISTING erlaubt ausdrücklich das Überschreiben der alten Datei.

    public void resetSlot(int slot) throws IOException {
        Files.copy(masterPath, getSlotPath(slot), StandardCopyOption.REPLACE_EXISTING);
    }


     //Lädt einen Spielstand aus seiner Datei und gibt die Werte als SaveData zurück
     // Fehlt die Datei, wird zuvor eine neue Kopie der master.txt erstellt

    public SaveData load(int slot) throws IOException {
        Path path = getSlotPath(slot);
        if (!Files.exists(path)) {
            Files.copy(masterPath, path);
        }

        // Alle Einträge aus der Spielstandsdatei einlesen.
        Properties properties = loadProperties(path);

        // Die Textwerte werden in Zahlen umgewandelt und in SaveData übertragen
        SaveData data = new SaveData();
        data.money = readInt(properties, "money", 0);
        data.damageLevel = readInt(properties, "damageLevel", 0);
        data.cooldownLevel = readInt(properties, "cooldownLevel", 0);
        data.healthLevel = readInt(properties, "healthLevel", 0);
        data.startAsteroidsLevel = readInt(properties, "startAsteroidsLevel", 0);
        // Falls der neue Name fehlt, wird der alte Name „spawnLevel“ gelesen.
        data.spawnRateLevel = readInt(properties, "spawnRateLevel",
                readInt(properties, "spawnLevel", 0));
        data.timeLimitLevel = readInt(properties, "timeLimitLevel", 0);
        return data;
    }


     //Liest einen ganzzahligen Wert aus Properties
     //Fehlende oder ungültige Zahlen werden durch den Standardwert ersetzt

    private int readInt(Properties properties, String key, int defaultValue) {
        try {
            //Begrenzung von negativen Zahlen auf 0
            return Math.max(0, Integer.parseInt(properties.getProperty(key, String.valueOf(defaultValue)).trim()));
        } catch (NumberFormatException exception) {
            return defaultValue;
        }
    }

    // Speichert die aktuellen Credits und Upgrade-Stufen in save1/2/3.txt
    public void save(int slot, SaveData data) throws IOException {
        // Jede Zahl wird in Text umgewandelt, da Properties Zeichenketten speichert
        Properties properties = new Properties();
        properties.setProperty("money", String.valueOf(data.money));
        properties.setProperty("damageLevel", String.valueOf(data.damageLevel));
        properties.setProperty("cooldownLevel", String.valueOf(data.cooldownLevel));
        properties.setProperty("healthLevel", String.valueOf(data.healthLevel));
        properties.setProperty("startAsteroidsLevel", String.valueOf(data.startAsteroidsLevel));
        properties.setProperty("spawnRateLevel", String.valueOf(data.spawnRateLevel));
        properties.setProperty("spawnLevel", String.valueOf(data.spawnRateLevel));
        properties.setProperty("timeLimitLevel", String.valueOf(data.timeLimitLevel));

        storeProperties(getSlotPath(slot), properties, "Raumschiff-Spielstand " + slot);
    }

    //Kauft ein Upgrade und speichert es sofort. Gibt false zurück, wenn zu wenig
    //Credits vorhanden sind oder das Upgrade seine maximale Stufe erreicht hat
    public boolean purchaseUpgrade(int slot, SaveData data, String upgrade) throws IOException {
        // Aktuelle Stufe, erlaubte Höchststufe und Preis ermitteln.
        int level = data.getLevel(upgrade);
        int maxLevel = getMaxLevel(upgrade);
        int cost = getUpgradeCost(upgrade, level);
        // Kein Kauf möglich, wenn Stufe 5 erreicht ist oder Credits fehlen
        if (level >= maxLevel || data.money < cost) {
            return false;
        }

        // Preis abziehen, Upgrade um eine Stufe erhöhen und sofort speichern
        data.money -= cost;
        data.setLevel(upgrade, level + 1);
        save(slot, data);
        return true;
    }

     //Berechnet den Preis für die NÄCHSTE Stufe eines Upgrades
     //Beispiel: „damage“ kostet bei Stufe 0 insgesamt 12 Credits,
     //bei Stufe 1 dann 24 Credits. Unbekannte Namen lösen einen Fehler aus.

    public int getUpgradeCost(String upgrade, int currentLevel) {
        switch (upgrade) {
            case "damage": return 12 + currentLevel * 12;
            case "cooldown": return 14 + currentLevel * 12;
            case "health": return 14 + currentLevel * 12;
            case "startAsteroids": return 10 + currentLevel * 10;
            case "spawnRate": return 18 + currentLevel * 14;
            case "timeLimit": return 16 + currentLevel * 14;
            default: throw new IllegalArgumentException("Unbekanntes Upgrade: " + upgrade);
        }
    }

    // Gibt die maximal mögliche Stufe eines bekannten Upgrades zurück: 5
    public int getMaxLevel(String upgrade) {
        switch (upgrade) {
            case "damage":
            case "cooldown":
            case "health":
            case "startAsteroids":
            case "spawnRate":
            case "timeLimit":
                return 5;
            default:
                throw new IllegalArgumentException("Unbekanntes Upgrade: " + upgrade);
        }
    }


     //Fügt dem gewählten Spielstand Spielgeld hinzu und speichert diese Änderung sofort,
     //dabei werden null oder negative Beträge ignoriert

    public void addCredits(int slot, SaveData data, int amount) throws IOException {
        if (amount <= 0) return;
        data.money += amount;
        save(slot, data);
    }


     //Enthält die Daten eines einzelnen Spielstands im Arbeitsspeicher.
     //Die öffentlichen Felder enthalten Spielgeld und die Stufen der Upgrades.

    public static class SaveData {
        //Aktuell verfügbares Spielgeld.
        public int money;
        //Jede Variable speichert die derzeit gekaufte Stufe des Upgrades.
        public int damageLevel;
        public int cooldownLevel;
        public int healthLevel;
        public int startAsteroidsLevel;
        public int spawnRateLevel;
        public int timeLimitLevel;

        // Liefert die aktuelle Stufe des durch seinen Namen angegebenen Upgrades
        public int getLevel(String upgrade) {
            switch (upgrade) {
                case "damage": return damageLevel;
                case "cooldown": return cooldownLevel;
                case "health": return healthLevel;
                case "startAsteroids": return startAsteroidsLevel;
                case "spawnRate": return spawnRateLevel;
                case "timeLimit": return timeLimitLevel;
                default: throw new IllegalArgumentException("Unbekanntes Upgrade: " + upgrade);
            }
        }

        //Setzt die Stufe des angegebenen Upgrades auf den übergebenen Wert
        public void setLevel(String upgrade, int level) {
            switch (upgrade) {
                case "damage": damageLevel = level; break;
                case "cooldown": cooldownLevel = level; break;
                case "health": healthLevel = level; break;
                case "startAsteroids": startAsteroidsLevel = level; break;
                case "spawnRate": spawnRateLevel = level; break;
                case "timeLimit": timeLimitLevel = level; break;
                default: throw new IllegalArgumentException("Unbekanntes Upgrade: " + upgrade);
            }
        }


//         Addiert sechs Upgrade-Stufen zu einem Gesamtwert

        public int getTotalUpgradeLevels() {
            return damageLevel + cooldownLevel + healthLevel
                    + startAsteroidsLevel + spawnRateLevel + timeLimitLevel;
        }
    }
}
