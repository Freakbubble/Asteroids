package Raumschiff;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

public final class ArcadeHighscore {
    private static final Path FILE = Paths.get("res/game_files/arcade-highscore.txt");

    private ArcadeHighscore() { }

    // Lädt den nichtnegativen Highscore; bei fehlender Datei oder ungültigen Daten wird 0 verwendet
    public static int load() {
        if (!Files.exists(FILE)) return 0;
        try (InputStream input = Files.newInputStream(FILE)) {
            Properties properties = new Properties();
            properties.load(input);
            String data = properties.getProperty("highscore");
            return Math.max(0, Integer.parseInt(data.trim()));
        } catch (IOException | NumberFormatException exception) {
            System.err.println("Arcade-Highscore konnte nicht gelesen werden: " + exception.getMessage());
            return 0;
        }
    }

    //Schreibt die Punktzahl als nichtnegativen Int direkt in die Highscore-Properties-Datei
    public static void save(int score) throws IOException {
        Properties properties = new Properties();
        properties.setProperty("highscore", String.valueOf(Math.max(0, score)));
        // Wie bei den normalen Spielständen direkt in die Zieldatei schreiben.
        try (OutputStream output = Files.newOutputStream(FILE)) {
            properties.store(output, "Raumschiff-Arcade-Highscore");
        }
    }
}
