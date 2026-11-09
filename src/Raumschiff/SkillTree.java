package Raumschiff;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.util.function.IntSupplier;
import java.util.function.Supplier;
import javax.swing.BorderFactory;
import javax.swing.JOptionPane;


 //Skilltree als ein eigener Spielbildschirm
 //Die Grafik wird auf das Spielfeld gezeichnet
 //Der eigentliche Kauf wird durch SaveManager geprüft.
public final class SkillTree {
    private static final String[] KEYS = {
            "damage", "startAsteroids", "health", "cooldown", "spawnRate", "timeLimit"
    };
    private static final String[] NAMES = {
            "Schaden", "Start-Asteroiden", "Leben", "Schuss-Cooldown", "Spawnrate", "Zeitlimit"
    };
    private static final String[] SHORT_LABELS = {
            "SCHADEN", "ASTEROIDEN", "LEBEN", "COOLDOWN", "SPAWNRATE", "ZEITLIMIT"
    };
    // Baumlayout: Schaden unten in der Mitte, darüber Asteroiden.
    // Leben und Cooldown stehen links/rechts von Asteroiden.
    // Spawnrate steht über Asteroiden, Zeitlimit rechts von Spawnrate.
    private static final double[] X_RATIOS = {.50, .50, .21, .79, .50, .79};

    private static final Color BACKGROUND_TOP = new Color(6, 18, 35);
    private static final Color BACKGROUND_BOTTOM = new Color(14, 43, 59);
    //Farbe des Textes
    private static final Color TEXT = new Color(236, 247, 255);
    private static final Color ACCENT = new Color(92, 205, 230);
    private static final Color UNLOCKED = new Color(23, 90, 105);
    private static final Color AFFORDABLE = new Color(27, 113, 103);
    private static final Color LockedButtonColor = new Color(33, 45, 56);
    private static final Color MAXED = new Color(72, 115, 68);

    private final Spielfeld spielfeld;
    private final SaveManager saveManager;
    private final Supplier<SaveManager.SaveData> currentSave;
    private final IntSupplier currentSlot;
    private final Runnable onUpgradePurchased;
    // Buttons bilden die klickbaren Knoten
    private final Button[] buttons = new Button[KEYS.length];
    private final Button backButton;
    private final Button startRoundButton;
    //Centers enthält die Mittelpunkte der Buttons um die Verbindungslinien zu zeichnen
    private final Point[] centers = new Point[KEYS.length];
    //Variable speichert den Index des Upgrades, über dem sich die Maus gerade befindet:
    //-1 kein Upgrade; 0 erstes Upgrade; 1 zweites Upgrade; etc.
    private int hoveredIndex = -1;
    //Variable speichert den Index des Upgrades, dessen Informationen gerade angezeigt werden:
    //0=Informationen zum ersten Upgrade, 1: Informationen zum zweiten Upgrade, 2: Informationen zum dritten Upgrade
    private int detailIndex = 0;
    //Zeigt an ob der Skilltree der jetzige Bildschirm ist
    private boolean active;

    public SkillTree(Spielfeld spielfeld, SaveManager saveManager,
                     Supplier<SaveManager.SaveData> currentSave, IntSupplier currentSlot,
                     Runnable onUpgradePurchased, Runnable onBack, Runnable onStartRound) {
        this.spielfeld = spielfeld;
        this.saveManager = saveManager;
        this.currentSave = currentSave;
        this.currentSlot = currentSlot;
        this.onUpgradePurchased = onUpgradePurchased;

        //Button um zurück ins Menü zu kommen
        backButton = new Button("← Hauptmenü", new Color(28, 67, 85));
        backButton.setFont(new Font("Arial", Font.BOLD, 17));
        backButton.setForeground(TEXT);
        backButton.setBackground(new Color(28, 67, 85));
        backButton.setFocusPainted(false);
        backButton.setBorder(BorderFactory.createLineBorder(new Color(68, 155, 176), 2));
        backButton.addActionListener(e -> onBack.run());
        backButton.setVisible(false);
        spielfeld.add(backButton);

        //Button um mit aktuellem Speicherslot die nächste Runde starten
        startRoundButton = new Button("Runde starten", new Color(36, 130, 108));
        startRoundButton.setFont(new Font("Arial", Font.BOLD, 22));
        startRoundButton.setForeground(TEXT);
        startRoundButton.setFocusPainted(false);
        startRoundButton.setBorder(BorderFactory.createLineBorder(new Color(111, 224, 185), 3));
        startRoundButton.addActionListener(e -> {
            if (active && currentSlot.getAsInt() != 0) onStartRound.run();
        });
        startRoundButton.setVisible(false);
        spielfeld.add(startRoundButton);

        for (int i = 0; i < KEYS.length; i++) {
            // Jeder Knoten speichert seinen Index für Kauf- und Hover-Aktionen
            final int index = i;
            Button button = new Button("", LockedButtonColor);
            button.setFont(new Font("Arial", Font.BOLD, 15));
            button.setForeground(TEXT);
            button.setFocusPainted(false);
            button.setOpaque(true);
            button.setBackground(LockedButtonColor);
            button.setBorder(BorderFactory.createLineBorder(new Color(76, 111, 128), 2));
            button.addActionListener(e -> buyUpgrade(index));
            button.addMouseListener(new MouseAdapter() {
                //Zeigt beim Hovern über einem Upgrade-Buttons dessen Details an
                @Override public void mouseEntered(MouseEvent e) {
                    hoveredIndex = index;
                    detailIndex = index;
                    spielfeld.repaint();
                }
                //Beendet die Hover-Hervorhebung, wenn der Mauszeiger den Button verlässt
                @Override public void mouseExited(MouseEvent e) {
                    hoveredIndex = -1;
                    spielfeld.repaint();
                }
            });
            button.setVisible(false);
            buttons[i] = button;
            spielfeld.add(button);
        }
    }

    //Blendet alle Baum-Elemente ein oder aus und aktualisiert beim Öffnen ihren Zustand
    public void setActive(boolean active) {
        this.active = active;
        if (active) {
            detailIndex = 0;
            hoveredIndex = -1;
            refresh();
        }
        backButton.setVisible(active);
        startRoundButton.setVisible(active);
        for (Button button : buttons) button.setVisible(active);
        if (active) layout(spielfeld.getWidth(), spielfeld.getHeight());
    }

   //Berechnet abhängig von Fenstergröße und Positionsverhältnissen alle Knotenpositionen
    public void layout(int width, int height) {
        if (!active || width <= 0 || height <= 0) return;
        backButton.setBounds(22, 20, 175, 45);
        int startWidth = Math.min(260, Math.max(190, width / 3));
        //Startbutton ist rechts im Infobereich, die Beschreibung links bleibt vollständig lesbar.
        startRoundButton.setBounds(Math.max(40, width - startWidth - 57),
                height - 78, startWidth, 54);
        int nodeWidth = nodeWidth(width);
        int nodeHeight = nodeHeight(height);
        int top = Math.max(height < 650 ? 128 : 155, (int) (height * .235));
        int middle = Math.max(top + nodeHeight + 28, (int) (height * .435));
        int bottom = Math.max(middle + nodeHeight + 28, (int) (height * .635));
        //Die Zeilenpositionen folgen derselben Reihenfolge wie die Schlüssel in KEYS
        int[] ys = {bottom, middle, middle, middle, top, top};
        for (int i = 0; i < buttons.length; i++) {
            int x = (int) Math.round(width * X_RATIOS[i]);
            int y = ys[i];
            centers[i] = new Point(x, y);
            buttons[i].setBounds(x - nodeWidth / 2, y - nodeHeight / 2,
                    nodeWidth, nodeHeight);
        }
    }
    //Begrenzt die Knotenbreite auf einen Wert zwischen 105 und 180 Pixeln, basieren auf der Breite des Spielfelds
    private static int nodeWidth(int width) {
        return Math.clamp(width / 6, 105, 180);
    }

     //Setzt die Knotenhöhe entweder auf den Wert 72, 82 oder 94, basieren auf der Höhe des Spielfelds
    private static int nodeHeight(int height) {
        if (height < 650) {
            return 72;
        } else if (height < 850) {
            return 82;
        } else {
            return 94;
        }
    }

    //Sperrt Knoten anhand des Spielstands; freigeschaltete Knoten bleiben kaufbar
    //Liest den Spielstand und aktualisiert Sperrung, Stufen, Preise, Farben und Hinweise aller Knoten
    public void refresh() {
        SaveManager.SaveData save = currentSave.get();
        if (save == null || saveManager == null) return;
        for (int i = 0; i < KEYS.length; i++) {
            String key = KEYS[i];
            int level = save.getLevel(key);
            int maxLevel = saveManager.getMaxLevel(key);
            // Die Freischaltregel stammt aus SaveManager, damit Darstellung und Kaufprüfung identisch sind
            boolean unlocked = saveManager.isUpgradeUnlocked(save, key);
            boolean affordable = save.money >= saveManager.getUpgradeCost(key, level);
            Button button = buttons[i];
            String levelLabel;
            if (level >= maxLevel) {
                levelLabel = "MAX";
            } else {
                levelLabel = level + " / " + maxLevel;
            }
            String statusLabel;
            if (!unlocked) {
                statusLabel = "GESPERRT";
            } else if (level >= maxLevel) {
                statusLabel = "ABGESCHLOSSEN";
            } else if (affordable) {
                statusLabel = "KAUFBAR";
            } else {
                statusLabel = "ZU WENIG ERZ";
            }
            button.setText("<html><center>" + (i + 1) + " · " + SHORT_LABELS[i]
                    + "<br><font size='3'>" + levelLabel
                    + "</font><br><font size='2'>" + statusLabel
                    + "</font></center></html>");
            button.setEnabled(unlocked && level < maxLevel);
            if (unlocked) {
                button.setForeground(TEXT);
            } else {
                button.setForeground(new Color(150, 171, 181));
            }
            if (!unlocked) {
                button.setBackground(LockedButtonColor);
            } else if (level >= maxLevel) {
                button.setBackground(MAXED);
            } else if (affordable) {
                button.setBackground(AFFORDABLE);
            } else {
                button.setBackground(UNLOCKED);
            }
            Color borderColor;
            if (!unlocked) {
                borderColor = new Color(68, 83, 94);
            } else if (level >= maxLevel) {
                borderColor = new Color(144, 207, 130);
            } else {
                borderColor = ACCENT;
            }
            button.setBorder(BorderFactory.createLineBorder(borderColor, 2));
            String previous = saveManager.getPreviousUpgrade(key);
            if (unlocked) {
                button.setToolTipText(NAMES[i] + ": " + level + " / " + maxLevel);
            } else {
                button.setToolTipText("Gesperrt: zuerst mindestens 1 Stufe "
                        + displayName(previous) + " kaufen");
            }
        }
        spielfeld.repaint();
    }

    //Prüft die Auswahl, gibt Kauf und Speicherung an SaveManager weiter und aktualisiert die Ansicht
    private void buyUpgrade(int index) {
        // Kein eigener zweiter Spielstand: der Skilltree liest die aktuellen Werte über einen Callback
        SaveManager.SaveData save = currentSave.get();
        if (!active || save == null || saveManager == null || currentSlot.getAsInt() == 0) return;
        String key = KEYS[index];
        detailIndex = index;
        if (!saveManager.isUpgradeUnlocked(save, key)) {
            refresh();
            return;
        }
        int level = save.getLevel(key);
        int cost = saveManager.getUpgradeCost(key, level);
        if (level >= saveManager.getMaxLevel(key)) return;
        if (save.money < cost) {
            JOptionPane.showMessageDialog(spielfeld,
                    "Für dieses Upgrade benötigen Sie " + cost + " Erz.",
                    "Nicht genügend Erz", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        try {
            if (saveManager.purchaseUpgrade(currentSlot.getAsInt(), save, key)) {
                onUpgradePurchased.run();
            }
        } catch (IOException exception) {
            JOptionPane.showMessageDialog(spielfeld,
                    "Das Upgrade konnte nicht gespeichert werden:\n" + exception.getMessage(),
                    "Speicherfehler", JOptionPane.ERROR_MESSAGE);
        }
        refresh();
    }

    //Zeichnet Baumhintergrund, Verbindungen zwischen freischaltbaren Upgrades und Informationsbereich
    public void paint(Graphics2D g, int width, int height) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        //Der Skilltree wird direkt in eine Graphics2D-Zeichenfläche gemalt
        g.setPaint(new GradientPaint(0, 0, BACKGROUND_TOP, 0, height, BACKGROUND_BOTTOM));
        g.fillRect(0, 0, width, height);
        g.setColor(new Color(67, 134, 157, 33));
        for (int x = 0; x < width; x += 36) g.drawLine(x, 0, x, height);
        for (int y = 0; y < height; y += 36) g.drawLine(0, y, width, y);
        g.setColor(new Color(110, 201, 225, 68));
        for (int i = 0; i < 85; i++) {
            int x = (i * 197 + 51) % Math.max(1, width);
            int y = (i * 149 + 31) % Math.max(1, height);
            g.fillOval(x, y, 2, 2);
        }

        g.setColor(TEXT);
        g.setFont(new Font("Arial", Font.BOLD, 37));
        centerString(g, "SKILLTREE", width / 2, 70);

        //Der Skilltree liest die aktuellen Werte des Speicherslots über einen Callback
        SaveManager.SaveData save = currentSave.get();
        g.setColor(new Color(24, 66, 82));
        g.fillRoundRect(width - 198, 20, 174, 48, 16, 16);
        g.setColor(ACCENT);
        g.setFont(new Font("Arial", Font.BOLD, 18));
        g.drawString(save == null ? "Erz: 0" : "Erz: " + save.money, width - 179, 50);
        g.setColor(new Color(181, 209, 220));

        int nodeWidth = nodeWidth(width);
        int nodeHeight = nodeHeight(height);
        // Die Verbindungen werden aus denselben Voraussetzungen wie die Käufe
        // berechnet. So kann die Grafik nicht von den Speicherregeln abweichen.
        for (int i = 0; i < KEYS.length; i++) {
            // Die grafische Verbindung wird aus getPreviousUpgrade abgeleitet statt separat festgelegt
            String prerequisite;
            if (saveManager == null) {
                prerequisite = null;
            } else {
                prerequisite = saveManager.getPreviousUpgrade(KEYS[i]);
            }
            int parentIndex = indexOfKey(prerequisite);
            if (parentIndex < 0) continue;
            Point from = centers[parentIndex];
            Point to = centers[i];
            if (from == null || to == null) continue;
            boolean connected = save != null && save.getLevel(prerequisite) >= 1;
            int strokeWidth;
            if (connected) {
                g.setColor(ACCENT);
                strokeWidth = 5;
            } else {
                g.setColor(new Color(82, 111, 122));
                strokeWidth = 3;
            }
            g.setStroke(new BasicStroke(strokeWidth,
                    BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            int dx = Integer.compare(to.x, from.x);
            int dy = Integer.compare(to.y, from.y);
            if (dx == 0) {
                int startY = from.y + dy * (nodeHeight / 2 + 3);
                int endY = to.y - dy * (nodeHeight / 2 + 11);
                g.drawLine(from.x, startY, to.x, endY);
                drawArrow(g, to.x, endY, 0, dy);
            } else if (dy == 0) {
                int startX = from.x + dx * (nodeWidth / 2 + 3);
                int endX = to.x - dx * (nodeWidth / 2 + 11);
                g.drawLine(startX, from.y, endX, to.y);
                drawArrow(g, endX, to.y, dx, 0);
            }
        }

        // Der Infobereich gehört zum gezeichneten Hintergrund
        int bottomCenter = centers[0] == null ? (int) (height * 0.635) : centers[0].y;
        int infoTop = Math.max((int) (height * 0.74), bottomCenter + nodeHeight / 2 + 19);
        int boxH = Math.max(84, height - infoTop - 18);
        g.setColor(new Color(5, 20, 33, 215));
        g.fillRoundRect(35, infoTop, Math.max(100, width - 70), boxH, 22, 22);
        g.setColor(new Color(71, 143, 163));
        g.setStroke(new BasicStroke(2));
        g.drawRoundRect(35, infoTop, Math.max(100, width - 70), boxH, 22, 22);
        if (save != null) drawInformation(g, infoTop, width, save);
    }

    //Zeigt Beschreibung, aktuelle Wirkung, nächste Stufe und Kosten des gewählten Knotens
    private void drawInformation(Graphics2D g, int infoTop, int width, SaveManager.SaveData save) {
        int index;
        if (hoveredIndex >= 0) {
            index = hoveredIndex;
        } else {
            index = detailIndex;
        }
        String key = KEYS[index];
        int level = save.getLevel(key);
        int max = saveManager.getMaxLevel(key);
        //Freischaltregel stammt aus SaveManager,
        boolean unlocked = saveManager.isUpgradeUnlocked(save, key);
        g.setFont(new Font("Arial", Font.BOLD, 20));
        g.setColor(ACCENT);
        g.drawString((index + 1) + ". " + NAMES[index], 57, infoTop + 29);
        g.setFont(new Font("Arial", Font.PLAIN, 15));
        g.setColor(TEXT);
        g.drawString(description(key), 57, infoTop + 53);
        String before = effect(key, level);
        String after;
        if (level >= max) {
            after = "Maximalstufe";
        } else {
            after = effect(key, level + 1);
        }
        g.drawString("Aktuell: " + before + "     Nächste Stufe: " + after,
                57, infoTop + 76);
        String state;
        if (!unlocked) {
            state = "Gesperrt – zuerst 1 Stufe "
                    + displayName(saveManager.getPreviousUpgrade(key)) + " kaufen";
        } else if (level >= max) {
            state = "Vollständig ausgebaut";
        } else {
            state = "Kosten für die nächste Stufe: " + saveManager.getUpgradeCost(key, level) + " Erz";
        }
        if (!unlocked) {
            g.setColor(new Color(247, 189, 111));
        } else {
            g.setColor(new Color(128, 228, 197));
        }
        g.drawString(state, 57, infoTop + 100);
        if (width > 900) {
            g.setColor(new Color(161, 195, 207));
            g.drawString("Maus über einen Knoten bewegen, um Details anzuzeigen.",
                    57, infoTop + 126);
        }
    }

    //Zeichnet einen Text horizontal zentriert um die angegebene X-Koordinate
    private static void centerString(Graphics2D g, String string, int x, int y) {
        FontMetrics fm = g.getFontMetrics();
        g.drawString(string, x - fm.stringWidth(string) / 2, y);
    }

    //Zeichnet eine Pfeilspitze am Ende einer gerichteten Baumverbindung
    private static void drawArrow(Graphics2D g, int x, int y, int dx, int dy) {
        int px = -dy;
        int py = dx;
        Polygon arrow = new Polygon();
        arrow.addPoint(x, y);
        arrow.addPoint(x - dx * 13 + px * 7, y - dy * 13 + py * 7);
        arrow.addPoint(x - dx * 13 - px * 7, y - dy * 13 - py * 7);
        g.fillPolygon(arrow);
    }

   //Sucht die Position des Upgrade-Schlüssels in KEYS
    private static int indexOfKey(String key) {
        if (key == null) return -1;
        for (int i = 0; i < KEYS.length; i++) {
            if (KEYS[i].equals(key)) return i;
        }
        //-1 bedeutet dass KEY nicht gefunden wurde
        return -1;
    }

    //Gibt den lesbaren Namen eines Schlüssels für Hinweise zurück
    private static String displayName(String key) {
        if (key == null) return "";
        for (int i = 0; i < KEYS.length; i++) if (KEYS[i].equals(key)) return NAMES[i];
        return key;
    }

    //Liefert den Informationstext für ein Upgrade
    private static String description(String key) {
        switch (key) {
            case "damage": return "Erhöht den Schaden Ihrer Schüsse.";
            case "cooldown": return "Verkürzt die Zeit zwischen zwei Schüssen.";
            case "health": return "Erhöht die maximalen Lebenspunkte des Raumschiffs.";
            case "startAsteroids": return "Erhöht die Anzahl der Asteroiden beim Rundenbeginn.";
            case "spawnRate": return "Neue Asteroiden erscheinen in kürzeren Abständen.";
            case "timeLimit": return "Verlängert die Rundenzeit pro Stufe um 15 Sekunden.";
            default: return "";
        }
    }

    //Liefert den Effekt eines Upgrades
    private static String effect(String key, int level) {
        switch (key) {
            case "damage": return UpgradeWerte.getDamageValue(level) + " Schaden";
            case "cooldown": return UpgradeWerte.getShotCooldownMillis(level) + " ms Cooldown";
            case "health": return UpgradeWerte.getHealthValue(level) + " Leben";
            case "startAsteroids": return UpgradeWerte.getStartingAsteroidCount(level) + " Asteroiden";
            case "spawnRate": return UpgradeWerte.formatSeconds(UpgradeWerte.getAsteroidSpawnDelay(level)) + " s Spawnabstand";
            case "timeLimit": return UpgradeWerte.formatSeconds(UpgradeWerte.getRoundDurationMillis(level) / 1000.0) + " s Rundenzeit";
            default: return "";
        }
    }
}
