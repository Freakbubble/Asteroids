package Raumschiff;

import java.awt.*;
import java.awt.event.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Locale;
import java.text.NumberFormat;
import javax.imageio.ImageIO;
import javax.swing.*;

public class Spielfeld extends JPanel implements MouseListener, KeyListener, MouseMotionListener {

	// Abmessungen des Spielfelds; als Ausgangswert wird die Bildschirmgröße verwendet
	private final Dimension prefSize = new Dimension(Toolkit.getDefaultToolkit().getScreenSize());

	// Swing-Timer und Cursor
	private Timer t;
	private Cursor c;

	private boolean gameRunning;

	//Hintergrundgrafiken
	private BufferedImage titleScreen_image;
	private BufferedImage gameScreen_image;

	//Herzgrafik
	private BufferedImage heartImage;

	// Asteroiden und Beute
	// Listen aller aktiven Asteroiden und Erzobjekte; Spawnzeiten verhindern ungeplante Häufung.
	private ArrayList<Asteroid> asteroids;
	private ArrayList<Ore> ores;
	private int startingAsteroidcount = 12;
	private double spawnDelaySeconds = 5.0;
	private long lastAsteroidSpawnMillis;
	private long lastPlayerDamageMillis;

	// Zeitlimit der Runde
	// Die Zeitmessung verwendet nanoTime für aktive Zeit und wandelt für Anzeigen in Millisekunden um.
	private static final long NANOS_PER_MILLI = 1_000_000L;
	private long roundDurationMillis = 10_000L;
	private long remainingTimeMillis = 10_000L;
	// Die gesamte Zeitmessung liegt direkt im Spielfeld.
	//Pausierte Abschnitte werden nicht als vergangene Zeit gewertet
	private long elapsedBeforePauseNanos;
	private long activeSegmentStartedAtNanos;
	private boolean gamePaused;

	// Player- und Shot-Variablen
	// Raumschiff, Schüsse und Eingabestatus für Beschleunigung und Schusspause.
	private Player player;
	private ArrayList<Shot> shots;
	private boolean playerMove;
	private long nextShotAllowedAtMillis;
	private int shotCooldownMillis = 500;

	private double angle;
	private double mouseX;
	private double mouseY;

	// Screen definiert die vier sichtbaren Ansichten, GameMode unterscheidet normalen und Arcade-Modus.
	private enum Screen { SLOT_SELECTION, MAIN_MENU, SKILL_TREE, PLAYING }
	private enum GameMode { NORMAL, ARCADE }
	// Zuerst erscheint der ASTEROIDS-Titelbildschirm mit der Moduswahl
	private Screen currentScreen = Screen.MAIN_MENU;
	private GameMode gameMode = GameMode.NORMAL;
	private boolean normalStartRequested;
	//Arcade-spezifische Konstanten und Punktestand; der Rekord bleibt ein int.
	private static final int ARCADE_START_ASTEROIDS = 20;
	private static final long ARCADE_SPAWN_INTERVAL_MILLIS = 1_000L;
	private static final long ARCADE_SPAWN_RATE_STEP_SECONDS = 5L;
	private static final int ARCADE_ASTEROID_SIZE = 60;
	private int arcadeScore;
	private int arcadeHighscore;
	private long arcadeSecondsCredited;
	private final NumberFormat scoreFormat = NumberFormat.getIntegerInstance(Locale.GERMANY);

	//Oberflächenelemente für Hauptmenü, Slotwahl, laufendes Spiel und Skilltree.
	private Button optionsButton;
	private boolean optionsHasIcon;
	private Button startGameButton;
	private Button arcadeStartButton;
	private JLabel arcadeHighscoreLabel;
	private JLabel arcadePointsLabel;
	private Button skillTreeButton;
	private SkillTree skillTree;
	private Button backToMenuButton;
	private Button controlsButton;
	private final Button[] slotButtons = new Button[3];
	private final Button[] deleteSlotButtons = new Button[3];
	private JLabel slotSelectionTitle;
	private JLabel slotSelectionHelp;

	//Trennen der Spielstandsdaten eigentlichen Spielablauf
	private SaveManager saveManager;
	private SaveManager.SaveData saveData;
	// 0 heißt: noch kein Speicherplatz ausgewählt. Gültige Slots sind 1, 2 und 3.
	private int selectedSlot = 0;
	private boolean gameOver;

	//Konfiguriert das Panel und initialisiert Spielobjekte, Oberfläche und Eingabesteuerung.
	public Spielfeld() {
		setFocusable(true);
		setPreferredSize(prefSize);
		setLayout(null);
		gameRunning = false;
		initGame();
	}

	//Erzeugt das Raumschiff sowie eine leere Liste aktiver Schüsse
	private void initPlayer() {
		player = new Player(new Coordinate(prefSize.getWidth() / 2, prefSize.getHeight() * 0.9),
				10, 10, Math.PI, 0);
		shots = new ArrayList<>();
	}

	//Erzeugt die anfänglichen Asteroiden des aktuellen Spielmodus und leert die Erzliste.
	private void initAsteroid() {
		asteroids = new ArrayList<>();
		int count;
		if (gameMode == GameMode.ARCADE) {
			count = ARCADE_START_ASTEROIDS;

		} else {
			count = startingAsteroidcount;

		}
		for (int i = 0; i < count; i++) {
			asteroids.add(new Asteroid(prefSize, 60, 60, Math.random() * 2 * Math.PI,
					1, new Color(60, 60, 35)));

		}
		ores = new ArrayList<>();
	}



	// Initialisiert Speicherverwaltung, alle Menü- und Spieloberflächen, Bildressourcen, Listener, Timer und Skilltree.
	private void initGame() {
		try {
			saveManager = new SaveManager();
			saveData = new SaveManager.SaveData();
		} catch (IOException exception) {
			System.err.println("Spielstände konnten nicht geladen werden: " + exception.getMessage());
			saveData = new SaveManager.SaveData();
		}
		arcadeHighscore = ArcadeHighscore.load();

		//Aufbau des Spielstand-Menüs: Überschrift, Erklärung und drei auswählbare Speicherkarten
		slotSelectionTitle = new JLabel("SPIELSTAND WÄHLEN", SwingConstants.CENTER);
		slotSelectionTitle.setForeground(Color.WHITE);
		slotSelectionTitle.setFont(new Font("Arial", Font.BOLD, 42));
		add(slotSelectionTitle);

		slotSelectionHelp = new JLabel(
				"Spielstand anklicken zum Laden – mit dem roten X löschen.",
				SwingConstants.CENTER);
		slotSelectionHelp.setForeground(new Color(195, 220, 230));
		slotSelectionHelp.setFont(new Font("Arial", Font.PLAIN, 20));
		add(slotSelectionHelp);

		for (int i = 0; i < slotButtons.length; i++) {
			final int slot = i + 1;
			Button slotButton = new Button("Spielstand " + slot, new Color(23, 56, 73));
			slotButton.setForeground(Color.WHITE);
			slotButton.setFont(new Font("Arial", Font.BOLD, 22));
			slotButton.setFocusPainted(false);
			// Die Löschfläche gehört zur Slotkarte. So übermalt die Hover-Neuzeichnung
			// des Slot-Buttons das rote Kreuz nicht
			slotButton.setLayout(null);
			slotButton.setBorder(BorderFactory.createLineBorder(new Color(70, 156, 178), 3));
			slotButton.addActionListener(e -> chooseSlot(slot));
			slotButtons[i] = slotButton;
			add(slotButton);
		}

		// Das Löschkreuz liegt direkt am rechten oberen Rand jeder Slot-Karte.
		for (int i = 0; i < deleteSlotButtons.length; i++) {
			final int slot = i + 1;
			Button deleteButton = new Button("×", new Color(170, 35, 45));
			deleteButton.setForeground(Color.WHITE);
			deleteButton.setFont(new Font("Arial", Font.BOLD, 27));
			deleteButton.setFocusPainted(false);
			deleteButton.setMargin(new Insets(0, 0, 0, 0));
			deleteButton.setBorder(BorderFactory.createLineBorder(new Color(245, 94, 105), 2));
			deleteButton.setToolTipText("Spielstand " + slot + " löschen");
			deleteButton.addActionListener(e -> deleteSlot(slot));
			deleteSlotButtons[i] = deleteButton;
			//Ein Klick aufs Kreuz löst die Löschaktion aus.
			slotButtons[i].add(deleteButton);
		}

		// Rücknavigation der Slotwahl zum Hauptmenü.
		backToMenuButton = new Button("Zurück zum Hauptmenü", new Color(44, 65, 78));
		backToMenuButton.setForeground(Color.WHITE);
		backToMenuButton.setFont(new Font("Arial", Font.BOLD, 19));
		backToMenuButton.addActionListener(e -> {
			// Die eigentliche Bildschirmausgabe erfolgt in paintComponent().
			if (currentScreen == Screen.SLOT_SELECTION) {
				normalStartRequested = false;
				showScreen(Screen.MAIN_MENU);
			}
		});
		add(backToMenuButton);


		//Options-Button nutzt ein Bild, falls die Ressource vorhanden ist, ansonsten Text
		File optionsFile = new File("res/assets/Option_Menu.png");
		optionsHasIcon = optionsFile.isFile();
		if (optionsHasIcon) {
			optionsButton = new Button(new ImageIcon(optionsFile.getPath()), 48, 48);
		} else {
			optionsButton = new Button("Optionen", new Color(18, 73, 92));
			optionsButton.setFont(new Font("Arial", Font.BOLD, 18));
			optionsButton.setForeground(Color.WHITE);
		}
		optionsButton.setToolTipText("Optionen");
		optionsButton.addActionListener(e -> showOptionsDialog());
		add(optionsButton);

		// Das ControlsButton nutzt ein Bild, falls nicht vorhanden wird text genutzt
		File controlsButtonFile = new File("res/assets/Steuerung_Button.png");
		if (controlsButtonFile.isFile()) {
			controlsButton = new Button(new ImageIcon(controlsButtonFile.getPath()), 42, 42);
		} else {
			controlsButton = new Button("?", new Color(18, 73, 92));
			controlsButton.setForeground(Color.WHITE);
		}
		controlsButton.setToolTipText("Spielsteuerung anzeigen");
		controlsButton.addActionListener(e -> showControlsDialog());
		add(controlsButton);

		// Hauptmenü: Auswahl normaler Modus (mit Speicherstand) oder Arcade-Modus.
		startGameButton = new Button("Normal spielen", new Color(50, 144, 156));
		startGameButton.setForeground(Color.WHITE);
		startGameButton.addActionListener(e -> {
			if (currentScreen != Screen.MAIN_MENU) return;
			// Vor jedem normalen Spiel zuerst den Spielstand wählen. Anschließend
			// öffnet sich der Skilltree, von dem aus die Runde gestartet wird.
			normalStartRequested = true;
			showScreen(Screen.SLOT_SELECTION);
		});
		add(startGameButton);

		arcadeStartButton = new Button("Arcade-Modus", new Color(186, 78, 42));
		arcadeStartButton.setForeground(Color.WHITE);
		arcadeStartButton.addActionListener(e -> {
			if (currentScreen == Screen.MAIN_MENU) beginRun(GameMode.ARCADE);
		});
		add(arcadeStartButton);

		arcadeHighscoreLabel = new JLabel("", SwingConstants.CENTER);
		arcadeHighscoreLabel.setForeground(new Color(247, 212, 91));
		arcadeHighscoreLabel.setFont(new Font("Arial", Font.BOLD, 27));
		add(arcadeHighscoreLabel);

		arcadePointsLabel = new JLabel("Arcade: Überleben +100/s  |  Erz +500  |  Asteroid +300", SwingConstants.CENTER);
		arcadePointsLabel.setForeground(new Color(202, 231, 237));
		arcadePointsLabel.setFont(new Font("Arial", Font.BOLD, 17));
		add(arcadePointsLabel);

		skillTreeButton = new Button("Skilltree", new Color(0, 68, 68));
		skillTreeButton.setForeground(Color.WHITE);
		skillTreeButton.addActionListener(e -> openSkillTree());
		add(skillTreeButton);

		// Hintergrundbilder werden aus res/assets geladen; Notfalls existieren Ersatzhintergründe.
		File titleFile = new File("res/assets/Title_Screen.png");
		if (titleFile.isFile()) {
			try {
				titleScreen_image = ImageIO.read(titleFile);
			} catch (IOException e) {
				System.err.println("Titelgrafik nicht lesbar: " + e.getMessage());
			}
		}

		//Laden des Hintergrunds während einer aktiven Runde
		File gameScreenFile = new File("res/assets/Game_Screen.png");
		if (gameScreenFile.isFile()) {
			try {
				gameScreen_image = ImageIO.read(gameScreenFile);
			} catch (IOException e) {
				System.err.println("Spielhintergrund nicht lesbar: " + e.getMessage());
			}
		}

		//Lebenspunkte werden als Bilddatei geladen und mehrfach nebeneinander dargestellt
		File heartFile = new File("res/assets/health_icon.png");
		if (heartFile.isFile()) {
			try {
				heartImage = ImageIO.read(heartFile);
			} catch (IOException e) {
				System.err.println("Herzbild nicht lesbar: " + e.getMessage());
			}
		}

		initPlayer();
		applyUpgradesToPlayer(true);
		initAsteroid();

		// Die Eingabe-Listener verarbeiten Mausbewegung, Maustasten und Tastatur.
		addMouseListener(this);
		addMouseMotionListener(this);
		addKeyListener(this);

		c = new Cursor(Cursor.CROSSHAIR_CURSOR);
		setCursor(c);


		// Ein Timerereignis ist kein Garant für exakt 20 ms verstrichene Spielzeit;
		// deshalb berechnet die Rundenuhr ihre Zeit mit System.nanoTime().
		t = new Timer(20, e -> doOnTick());
		skillTree = new SkillTree(this, saveManager, () -> saveData, () -> selectedSlot,
				() -> {
					applyUpgradesToPlayer(false);
					refreshSlotButtons();
					repaint();
				}, () -> {
			showScreen(Screen.MAIN_MENU);
			requestFocusInWindow();
		}, () -> {
			if (currentScreen == Screen.SKILL_TREE && selectedSlot != 0) {
				beginRun(GameMode.NORMAL);
			}
		});
		showScreen(Screen.MAIN_MENU);
	}


	 //Wählt den Spielmodus, setzt alle Rundenwerte zurück, wechselt in den Spielbildschirm und
	 //startet den Timer
	private void beginRun(GameMode mode) {
		gameMode = mode;
		normalStartRequested = false;
		resetRun();
		gameRunning = true;
		showScreen(Screen.PLAYING);
		startGame();
		requestFocusInWindow();
	}

	//Startet den Timer einer Runde und Initialisiert alle Zeitstempel
	private void startGame() {
		elapsedBeforePauseNanos = 0L;
		activeSegmentStartedAtNanos = System.nanoTime();
		remainingTimeMillis = roundDurationMillis;
		lastAsteroidSpawnMillis = 0;
		lastPlayerDamageMillis = -900;
		nextShotAllowedAtMillis = 0;
		gamePaused = false;
		t.start();
	}

	//Schaltet zwischen Menü, Slotwahl, Skilltree und Spiel um; aktualisiert Sichtbarkeit und Beschriftungen.
	//Merkt sich den aktuellen Bildschirm und blendet die dazugehörigen Bedienoberflächen ein
	private void showScreen(Screen screen) {
		currentScreen = screen;
		if (skillTree != null) skillTree.setActive(screen == Screen.SKILL_TREE);
		if (screen == Screen.SLOT_SELECTION) {
			if (normalStartRequested) {
				slotSelectionHelp.setText("Spielstand auswählen, um den Skilltree zu öffnen – rotes X zum Löschen.");
			} else {
				slotSelectionHelp.setText("Spielstand auswählen – rotes X zum Löschen.");
			}
			refreshSlotButtons();
		}
		if (arcadeHighscoreLabel != null) {
			arcadeHighscoreLabel.setText("ARCADE-HIGHSCORE: " + scoreFormat.format(arcadeHighscore));
		}
		doLayout();
		revalidate();
		repaint();
	}

	///Lädt den ausgewählten Spielstand und zeigt je nach Anlass Skilltree oder Hauptmenü.
	private void chooseSlot(int slot) {
		if (currentScreen != Screen.SLOT_SELECTION || gameRunning || saveManager == null) return;
		try {
			SaveManager.SaveData loadedData = saveManager.load(slot);
			selectedSlot = slot;
			saveData = loadedData;
			applyUpgradesToPlayer(true);
			if (normalStartRequested) {
				normalStartRequested = false;
				gameMode = GameMode.NORMAL;
				showScreen(Screen.SKILL_TREE);
			} else {
				showScreen(Screen.MAIN_MENU);
			}
		} catch (IOException exception) {
			JOptionPane.showMessageDialog(this,
					"Der Spielstand konnte nicht geladen werden:\n" + exception.getMessage(),
					"Ladefehler", JOptionPane.ERROR_MESSAGE);
		}
	}


	 //Liest die Spielstände aller drei Slots und zeigt Geld, Anzahl der Upgrade-Stufen und
	 //Auswahlstatus an
	private void refreshSlotButtons() {
		for (int slot = 1; slot <= slotButtons.length; slot++) {
			Button button = slotButtons[slot - 1];
			if (button == null) continue;
			try {
				if (saveManager == null) throw new IOException("Speicherverwaltung nicht verfügbar");
				SaveManager.SaveData data = saveManager.load(slot);
				String selectedHint = "";
				if (slot == selectedSlot) {
					selectedHint = "<br>Zuletzt ausgewählt";
				}
				button.setText("<html><center><br>SPIELSTAND " + slot
						+ "<br><br><font size='4'>Erz: " + data.money
						+ "<br>Upgrades: " + data.getTotalUpgradeLevels()
						+ selectedHint + "</font></center></html>");
				button.setEnabled(true);
			} catch (IOException exception) {
				button.setText("<html><center><br>SPIELSTAND " + slot
						+ "<br>nicht verfügbar</center></html>");
				button.setEnabled(false);
			}
			if (slot == selectedSlot) {
				button.setBackground(new Color(24, 91, 91));
			} else {
				button.setBackground(new Color(23, 56, 73));
			}
			deleteSlotButtons[slot - 1].setEnabled(button.isEnabled());
		}
	}

	//Fragt vor dem Zurücksetzen eines Slots nach einer Bestätigung und verwendet die Mastervorlage.

	private void deleteSlot(int slot) {
		if (currentScreen != Screen.SLOT_SELECTION || gameRunning || saveManager == null) return;

		int answer = JOptionPane.showConfirmDialog(this,
				"Spielstand " + slot + " wirklich löschen?\n"
						+ "Credits und alle Upgrades dieses Slots werden zurückgesetzt.",
				"Spielstand löschen", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
		if (answer != JOptionPane.YES_OPTION) return;

		try {
			saveManager.resetSlot(slot);
			if (selectedSlot == slot) {
				// Ein gelöschter aktiver Slot muss vor dem nächsten Rundenstart neu gewählt werden.
				selectedSlot = 0;
				saveData = saveManager.load(slot);
				applyUpgradesToPlayer(true);
			}
			refreshSlotButtons();
			showScreen(Screen.SLOT_SELECTION);
		} catch (IOException exception) {
			JOptionPane.showMessageDialog(this,
					"Spielstand konnte nicht zurückgesetzt werden:\n" + exception.getMessage(),
					"Speicherfehler", JOptionPane.ERROR_MESSAGE);
		}
	}

	//Pausiert das Spiel und bietet Fortsetzen, vorzeitiges Beenden oder Schließen an.
	//Pausiert vor dem Dialog die laufende Runde und bietet Fortsetzen, vorzeitiges Beenden und
	//Schließen an
	private void showOptionsDialog() {
		if (currentScreen != Screen.PLAYING || !gameRunning) return;

		pauseGame();
		if (!gameRunning) return;
		Object[] options = {"Weiter spielen", "Partie beenden (Hauptmenü)", "Spiel schließen"};
		int choice = JOptionPane.showOptionDialog(this,
				"Was möchten Sie tun?", "Optionen",
				JOptionPane.DEFAULT_OPTION, JOptionPane.PLAIN_MESSAGE,
				null, options, options[0]);

		if (choice == 1) {
			int confirm = JOptionPane.showConfirmDialog(this,
					"Die laufende Partie wird beendet. Ihr gespeicherter Fortschritt bleibt erhalten.\n"
							+ "Möchten Sie zum Hauptmenü zurückkehren?",
					"Partie beenden", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
			if (confirm == JOptionPane.YES_OPTION) {
				endRunEarly();
			} else {
				continueGame();
			}
		} else if (choice == 2) {
			int confirm = JOptionPane.showConfirmDialog(this,
					"Möchten Sie das Spiel wirklich schließen? Der Fortschritt wird gespeichert.",
					"Spiel schließen", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
			if (confirm == JOptionPane.YES_OPTION) {
				saveProgress();
				System.exit(0);
			} else {
				continueGame();
			}
		} else {
			continueGame();
		}
	}

	//Öffnet die Spielsteuerung als Grafik aus res/assets/Steuerung.png
	private void showControlsDialog() {
		boolean resumeAfterDialog = currentScreen == Screen.PLAYING && gameRunning && !gamePaused;
		if (resumeAfterDialog) {
			pauseGame();
		}
		try {
			File controlsFile = new File("res/assets/Steuerung.png");
			if (controlsFile.isFile()) {
				ImageIcon icon = new ImageIcon(controlsFile.getPath());
				if (icon.getIconWidth() > 0 && icon.getIconHeight() > 0) {
					// Verkleinert große Anleitungsbilder für kleinere Displays.
					int maxWidth = Math.min(760, Math.max(400, getWidth() - 90));
					int maxHeight = Math.min(520, Math.max(300, getHeight() - 130));
					double factor = Math.min(1.0, Math.min(
							(double) maxWidth / icon.getIconWidth(),
							(double) maxHeight / icon.getIconHeight()));
					int width = (int) Math.round(icon.getIconWidth() * factor);
					int height = (int) Math.round(icon.getIconHeight() * factor);
					Image scaledImage = icon.getImage().getScaledInstance(width, height, Image.SCALE_SMOOTH);
					JOptionPane.showMessageDialog(this, new JLabel(new ImageIcon(scaledImage)),
							"Spielsteuerung", JOptionPane.PLAIN_MESSAGE);
					return;
				}
			}
			// Sollte die Grafik fehlen, sind die Steuerungshinweise dennoch verfügbar
			JOptionPane.showMessageDialog(this,
					"Maus bewegen: Raumschiff ausrichten\n"
							+ "W oder Pfeil hoch: beschleunigen\n"
							+ "Pfeil runter: Bewegung stoppen\n"
							+ "Leertaste oder linke Maustaste: schießen\n"
							+ "Optionen: pausieren oder Partie beenden\n"
							+ "Skilltree: Escape öffnet das Hauptmenü",
					"Spielsteuerung", JOptionPane.INFORMATION_MESSAGE);
		} finally {
			if (resumeAfterDialog && gameRunning && gamePaused && currentScreen == Screen.PLAYING) {
				continueGame();
			}
		}
	}

	// Beendet eine laufende Partie manuell; normale Spielstände bleiben gespeichert.
	private void endRunEarly() {
		if (gameMode == GameMode.NORMAL) saveProgress();
		t.stop();
		elapsedBeforePauseNanos = getElapsedGameNanos();
		gamePaused = false;
		gameRunning = false;
		gameOver = true;
		playerMove = false;
		showScreen(Screen.MAIN_MENU);
	}

	//Überträgt die im Spielstand gekauften Upgrade-Stufen auf das Raumschiff und die Rundeneinstellungen.
	// Übersetzt die sechs gekauften Upgrade-Stufen mithilfe von UpgradeWerte in konkrete
	//Spielwerte
	private void applyUpgradesToPlayer(boolean restoreFullHealth) {
		if (player == null || saveData == null) return;
		player.setDamage(UpgradeWerte.getDamageValue(saveData.damageLevel));
		player.setMaxHealth(UpgradeWerte.getHealthValue(saveData.healthLevel));
		if (restoreFullHealth) player.healToFull();
		shotCooldownMillis = UpgradeWerte.getShotCooldownMillis(saveData.cooldownLevel);
		startingAsteroidcount = UpgradeWerte.getStartingAsteroidCount(saveData.startAsteroidsLevel);
		spawnDelaySeconds = UpgradeWerte.getAsteroidSpawnDelay(saveData.spawnRateLevel);
		roundDurationMillis = UpgradeWerte.getRoundDurationMillis(saveData.timeLimitLevel);
		// Upgrades werden im Hauptmenü gekauft, nicht während einer laufenden Runde.
		remainingTimeMillis = roundDurationMillis;
	}

	// Initialisiert Spieler, Asteroiden, Werte und Status vor einer neuen Runde.

	private void resetRun() {
		initPlayer();
		if (gameMode == GameMode.ARCADE) {
			// Arcade ist für alle Spieler gleich: keine Slot-Upgrades.
			player.setDamage(1);
			player.setMaxHealth(3);
			player.healToFull();
			shotCooldownMillis = 300;
			arcadeScore = 0;
			arcadeSecondsCredited = 0L;
		} else {
			applyUpgradesToPlayer(true);
		}
		initAsteroid();
		nextShotAllowedAtMillis = 0;
		playerMove = false;
		gameOver = false;
		gamePaused = false;
		remainingTimeMillis = roundDurationMillis;
	}

	//Schreibt den aktuellen normalen Spielstand; Arcade benutzt seinen eigenen Highscore.
	// Speichert im normalen Modus den aktuellen Slot
	public void saveProgress() {
		if (gameMode == GameMode.ARCADE) return;
		if (selectedSlot != 0 && saveManager != null && saveData != null) {
			try {
				saveManager.save(selectedSlot, saveData);
			} catch (IOException exception) {
				System.err.println("Spielstand konnte nicht gespeichert werden: " + exception.getMessage());
			}
		}
	}

	// Stoppt den Timer, ohne die bisher aktive Zeit zu verlieren

	public void pauseGame() {
		if (!gameRunning || gamePaused || currentScreen != Screen.PLAYING) return;
		// Den laufenden Zeitabschnitt einmalig sichern: Ab jetzt bleibt die Uhr stehen.
		elapsedBeforePauseNanos = getElapsedGameNanos();
		gamePaused = true;
		updateRemainingTime();
		if (!gameRunning) return; // Runde kann gerade abgelaufen sein.
		playerMove = false;
		t.stop();
		repaint();
	}

	//Führt den Timer nach einem Stopp weiter
	public void continueGame() {
		if (gameRunning && gamePaused && currentScreen == Screen.PLAYING) {
			// Ein neuer Zeitabschnitt beginnt; die Pausendauer wird nicht gezählt.
			activeSegmentStartedAtNanos = System.nanoTime();
			gamePaused = false;
			t.start();
			requestFocusInWindow();
		}
	}

	//Prüft Schüsse auf Treffer, entfernt zerstörte Objekte und erzeugt Erz sowie Arcade-Fragmente.
	private void asteroidHit() {
		// Ohne Schüsse oder Asteroiden muss keine Trefferprüfung stattfinden.
		if (shots.isEmpty() || asteroids.isEmpty()) return;

		// Erst nach Ende der Iteration ergänzen, damit es keine ConcurrentModificationException gibt.
		// Fragmente in separater Liste sammeln, um die Iteration über Asteroiden nicht zu verändern.
		ArrayList<Asteroid> spawnedFragments = new ArrayList<>();
		Iterator<Shot> shotIterator = shots.iterator();
		while (shotIterator.hasNext()) {
			Shot shot = shotIterator.next();
			Iterator<Asteroid> asteroidIterator = asteroids.iterator();
			while (asteroidIterator.hasNext()) {
				Asteroid asteroid = asteroidIterator.next();
				if (Kollision.checkCollisionBetweenCircles(asteroid, shot)) {
					// Im Arcade-Modus benötigt jeder Asteroid genau zwei Treffer.
					int additionalDamage;
					if (gameMode == GameMode.ARCADE) {
						additionalDamage = 1;
					} else {
						additionalDamage = shot.getDamage();
					}
					asteroid.setCurrentHealth(asteroid.getCurrentHealth() - player.getDamage()
							- additionalDamage);
					shotIterator.remove();
					if (asteroid.getCurrentHealth() <= 0) {
						asteroidIterator.remove();
						ores.add(asteroid.generateOre());
						if (gameMode == GameMode.ARCADE) {
							addArcadeScore(300);
							splitAsteroidInArcade(asteroid, spawnedFragments);
						}
					}
					break;
				}
			}
		}
		asteroids.addAll(spawnedFragments);
	}



	 //Die Teilungslogik  der Asteroiden
	 //liegt im Spielfeld; jedes Asteroid-Objekt stellt nur einen Asteroiden dar.

	private void splitAsteroidInArcade(Asteroid asteroid, ArrayList<Asteroid> spawnedFragments) {
		// Ein Ursprungsasteroid wird maximal zweimal geteilt: Generation 0 -> 1 -> 2.
		// Die Generation zählt bereits erfolgte Teilungen. Bei 2 endet der
		// Teilungsbaum, damit aus den Fragmenten nicht unbegrenzt neue entstehen.
		if (asteroid.getSplitGeneration() >= 2) return;

		// Fragmente sind 80 Prozent so breit und hoch wie ihr Ausgangsasteroid.
		double width = asteroid.getWidth() * 0.8;
		double height = asteroid.getHeight() * 0.8;
		double centerX = asteroid.getObjectPosition().getX() + asteroid.getWidth() / 2.0;
		double centerY = asteroid.getObjectPosition().getY() + asteroid.getHeight() / 2.0;
		// Genau zwei Durchläufe erzeugen genau zwei Fragmente.
		// Die Richtungen unterscheiden sich um -0,75 bzw. +0,75 Radiant.
		for (int i = 0; i < 2; i++) {
			double direction;
			if (i == 0) {
				direction = asteroid.getMovingAngle() - 0.75;
			} else {
				direction = asteroid.getMovingAngle() + 0.75;
			}
			double separation = Math.max(width, height) * 0.90;
			Coordinate position = new Coordinate(
					centerX + Math.cos(direction) * separation - width / 2.0,
					centerY + Math.sin(direction) * separation - height / 2.0);
			spawnedFragments.add(new Asteroid(position, width, height, direction,
					Math.max(1.2, Math.abs(asteroid.getMovingDistance()) * 1.18),
					Color.BLACK, asteroid.getSplitGeneration() + 1));
		}
	}

	//Erhöht die Punkte und speichert einen neuen Highscore im Arcade-Modus.
	private void addArcadeScore(int points) {
		if (gameMode != GameMode.ARCADE || points <= 0) return;
		arcadeScore += points;
		if (arcadeScore > arcadeHighscore) {
			arcadeHighscore = arcadeScore;
			try {
				ArcadeHighscore.save(arcadeHighscore);
			} catch (IOException exception) {
				System.err.println("Arcade-Highscore konnte nicht gespeichert werden: " + exception.getMessage());
			}
		}
	}

	//Berechnet anhand der Mausposition den Zielwinkel für das Raumschiff.
	private double movingAngle() {
		double dx = player.getObjectPosition().getX() - mouseX;
		double dy = player.getObjectPosition().getY() - mouseY;
		double hypothe = Math.sqrt((dx * dx) + (dy * dy));
		if (hypothe < 0.001) return angle;
		double newAngle = Math.acos(dx / hypothe);
		if (dy >= 0) {
			return newAngle;
		} else {
			return -newAngle;
		}
	}


	//Bewegt alle Projektile und entfernt Schüsse außerhalb des Spielfelds.

	private void checkShot() {
		Iterator<Shot> iShot = shots.iterator();
		while (iShot.hasNext()) {
			Shot shot = iShot.next();
			shot.makeMove();
			if (shot.isOut(prefSize)) {
				iShot.remove();
			}
		}
	}


	 //Bewegt Asteroiden und entfernt die, die weit außerhalb des Spielfelds sind
	private void checkAsteroid() {
		Iterator<Asteroid> cAsteroid = asteroids.iterator();
		while (cAsteroid.hasNext()) {
			Asteroid asteroid = cAsteroid.next();
			asteroid.makeMove();
			if (asteroid.isOut(prefSize)) {
				cAsteroid.remove();
			}
		}
	}

	//Lässt Erz verschwinden, zieht es bei Magnetreichweite an und schreibt die Belohnung gut.
	private void updateOres() {
		Iterator<Ore> oreIterator = ores.iterator();
		while (oreIterator.hasNext()) {
			Ore ore = oreIterator.next();
			if (ore.isExpired(getElapsedGameMillis())) {
				oreIterator.remove();
				continue;
			}

			double oreX = ore.getObjectPosition().getX() + ore.getWidth() / 2.0;
			double oreY = ore.getObjectPosition().getY() + ore.getHeight() / 2.0;
			double dx = player.getObjectPosition().getX() - oreX;
			double dy = player.getObjectPosition().getY() - oreY;
			double distance = Math.sqrt(dx * dx + dy * dy);

			if (distance <= player.getMagnetReach()) {
				double magnetStep = player.getMagnetStrength();
				if (distance <= magnetStep + Math.max(ore.getWidth(), ore.getHeight()) / 2.0) {
					oreIterator.remove();
					if (gameMode == GameMode.ARCADE) {
						addArcadeScore(500);
					} else if (saveManager != null && selectedSlot != 0) {
						try {
							saveManager.addMoney(selectedSlot, saveData, ore.getValue());
						} catch (IOException exception) {
							System.err.println("Erz konnte nicht gespeichert werden: " + exception.getMessage());
						}
					}
				} else if (distance > 0) {
					double moveX = dx / distance * magnetStep;
					double moveY = dy / distance * magnetStep;
					ore.getObjectPosition().setX(ore.getObjectPosition().getX() + moveX);
					ore.getObjectPosition().setY(ore.getObjectPosition().getY() + moveY);
				}
			}
		}
	}


	 //Berechnet für die n-te Arcade-Sekunde die Spawnanzahl. In Sekunde 1 bis 5 ist das ein
	 //Asteroid, danach steigt die Zahl alle fünf Sekunden um eins.

	private static int arcadeAsteroidsPerSecond(long spawnSecond) {
		return 1 + (int) ((spawnSecond - 1) / ARCADE_SPAWN_RATE_STEP_SECONDS);
	}

	// Erzeugt im Arcade-Modus fortlaufend Asteroiden; im Normalmodus wird nur bis zur Sollzahl nachgefüllt.
	private void spawnAsteroidIfNeeded() {
		long now = getElapsedGameMillis();
		// Arcade ist endlos; statt eines Rundentimers wird der Score
		// pro tatsächlich überlebter voller Sekunde fortgeschrieben.
		if (gameMode == GameMode.ARCADE) {
			// Jede vollständig gespielte Sekunde erhält ihre eigene Spawnrate.
			// Auch bei Timer-Verzögerungen werden die korrekten Sekunden nachgeholt.
			// Pausenzeiten sind in getElapsedGameMillis() nicht enthalten.
			while (now - lastAsteroidSpawnMillis >= ARCADE_SPAWN_INTERVAL_MILLIS) {
				long spawnSecond = lastAsteroidSpawnMillis / ARCADE_SPAWN_INTERVAL_MILLIS + 1;
				int asteroidCount = arcadeAsteroidsPerSecond(spawnSecond);
				for (int i = 0; i < asteroidCount; i++) {
					Asteroid asteroid = new Asteroid(prefSize, ARCADE_ASTEROID_SIZE,
							ARCADE_ASTEROID_SIZE, Math.random() * 2 * Math.PI,
							1, new Color(122, 137, 148));
					asteroid.setMaxHealth(3);
					asteroids.add(asteroid);
				}
				lastAsteroidSpawnMillis += ARCADE_SPAWN_INTERVAL_MILLIS;
			}
			return;
		}

		// Die Spawncooldown der Asteroiden
		long intervalMillis = (long) (spawnDelaySeconds * 1000L);
		if (asteroids.size() < startingAsteroidcount
				&& now - lastAsteroidSpawnMillis >= intervalMillis) {
			asteroids.add(new Asteroid(prefSize, 40, 40, Math.random() * 2 * Math.PI,
					1, new Color(35, 35, 35)));
			lastAsteroidSpawnMillis = now;
		}
	}

	// Prüft Zusammenstöße mit dem Raumschiff, zieht Leben ab und beendet bei Bedarf die Runde
	private void checkPlayerAsteroidCollisions() {
		long now = getElapsedGameMillis();
		//Verhindert dass man direkt danach noch mal getroffen werden kann
		if (now - lastPlayerDamageMillis < 900) return;
		Iterator<Asteroid> asteroidIterator = asteroids.iterator();
		while (asteroidIterator.hasNext()) {
			Asteroid asteroid = asteroidIterator.next();
			if (Kollision.checkPlayerAsteroidCollision(player, asteroid)) {
				asteroidIterator.remove();
				player.takeDamage(1);
				lastPlayerDamageMillis = now;
				if (player.getCurrentHealth() <= 0) {
					finishGame(false, "Spiel vorbei",
							"Ihr Raumschiff wurde zerstört. ");
				}
				break;
			}
		}
	}

	// Bisher aktiv gespielte Zeit; pausierte Abschnitte werden nicht mitgerechnet.
	private long getElapsedGameNanos() {
		long elapsed = elapsedBeforePauseNanos;
		if (gameRunning && !gamePaused) {
			elapsed += Math.max(0L, System.nanoTime() - activeSegmentStartedAtNanos);
		}
		return elapsed;
	}

	// Wandelt die aktive Spielzeit in Millisekunden um.
	private long getElapsedGameMillis() {
		return getElapsedGameNanos() / NANOS_PER_MILLI;
	}

	//Aktualisiert Rundenzeit oder Arcade-Überlebenspunkte
	private void updateRemainingTime() {
		if (!gameRunning) return;
		if (gameMode == GameMode.ARCADE) {
			// Nur vollständig überlebte Sekunden, ohne Pausendauer, zählen.
			long secondsSurvived = getElapsedGameMillis() / 1000L;
			if (secondsSurvived > arcadeSecondsCredited) {
				addArcadeScore((int) ((secondsSurvived - arcadeSecondsCredited) * 100L));
				arcadeSecondsCredited = secondsSurvived;
			}
			return; // Im Arcade-Modus gibt es kein Zeitlimit.
		}
		// Für den Normalmodus wird die echte aktive Dauer vom Zeitlimit
		// abgezogen. Die Rechnung erfolgt zunächst in Nanosekunden, damit
		// wiederholtes Runden bei jeder Timeraktualisierung vermieden wird.
		long remainingNanos = roundDurationMillis * NANOS_PER_MILLI - getElapsedGameNanos();
		if (remainingNanos <= 0L) {
			remainingTimeMillis = 0L;
			finishGame(true, "Runde geschafft",
					"Zeitlimit erreicht – Sie haben die Runde überlebt!\nDer Fortschritt wurde gespeichert.");
		} else {
			// Für die Anzeige aufrunden, damit nicht vorzeitig 0 erscheint.
			remainingTimeMillis = (remainingNanos + NANOS_PER_MILLI - 1L) / NANOS_PER_MILLI;
		}
	}

	//Speichert das Rundenergebnis, stoppt das Spiel und öffnet im Normalmodus den Skilltree.
	private void finishGame(boolean survived, String title, String message) {
		if (!gameRunning && currentScreen == Screen.MAIN_MENU) return;
		t.stop();
		elapsedBeforePauseNanos = getElapsedGameNanos();
		gamePaused = false;
		if (gameMode == GameMode.NORMAL) saveProgress();
		gameRunning = false;
		gameOver = true;
		playerMove = false;
		if (gameMode == GameMode.ARCADE) {
			title = "Arcade – Runde beendet";
			message = "Ihre Punkte: " + scoreFormat.format(arcadeScore)
					+ "\nArcade-Highscore: " + scoreFormat.format(arcadeHighscore)
					+ "\n\nZurück im Hauptmenü können Sie erneut starten.";
		}
		// Nach einer normalen Runde landet man direkt im Skilltree
		if (gameMode == GameMode.NORMAL && selectedSlot != 0) {
			showScreen(Screen.SKILL_TREE);
		} else {
			showScreen(Screen.MAIN_MENU);
		}
		JOptionPane.showMessageDialog(this, message, title, JOptionPane.INFORMATION_MESSAGE);
	}

	// Öffnet den Skilltree nur dann, wenn ein normaler Spielstand gewählt ist.
	private void openSkillTree() {
		if (currentScreen != Screen.MAIN_MENU || selectedSlot == 0 || saveManager == null) return;
		showScreen(Screen.SKILL_TREE);
	}

	// Prüft den Abstand zur Maus, damit das Raumschiff nicht zu nah an das Ziel beschleunigt
	private boolean checkPlayerMouseDistance() {
		double dx = player.getObjectPosition().getX() - mouseX;
		double dy = player.getObjectPosition().getY() - mouseY;
		double distancePlayerMouse = (dx * dx) + (dy * dy);
		double minDistance = ((Math.abs(3 * player.getMaxSpeed()) + Math.abs(2 * player.getAcceleration()))
				* 3 * Math.abs(player.getMaxSpeed()) / 2.25);
		return distancePlayerMouse > (minDistance * minDistance);
	}


	//Berechnet alle 20ms Bewegungen und Kollisionen der Objekte
	private void doOnTick() {
		if (!gameRunning || gamePaused || currentScreen != Screen.PLAYING) return;

		updateRemainingTime();
		if (!gameRunning) return;

		angle = movingAngle();
		checkShot();
		checkAsteroid();
		asteroidHit();
		spawnAsteroidIfNeeded();

		player.setMovingAngle(angle);
		if (playerMove && checkPlayerMouseDistance()) {
			if (player.getMovingDistance() > player.getMaxSpeed()) {
				player.setMovingDistance(player.getMovingDistance() + player.getAcceleration());
			}
		} else {
			if (player.getMovingDistance() < 0) {
				player.setMovingDistance(player.getMovingDistance() - 0.5 * player.getAcceleration());
			}
		}
		player.makeMove();
		updateOres();
		checkPlayerAsteroidCollisions();
		//Zeichnen der Objekte an der neuen Stelle
		repaint();
	}

	// Zeichnet je nach Bildschirm das Menü, den Skilltree oder Spielwelt und HUD.
	@Override
	public void paintComponent(Graphics g) {
		super.paintComponent(g);
		Graphics2D g2d = (Graphics2D) g;
		g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

		if (currentScreen == Screen.SLOT_SELECTION) {
			// Die Slot-Auswahl besitzt einen eigenen gemalten Verlaufshintergrund.
			GradientPaint background = new GradientPaint(0, 0, new Color(5, 20, 33),
					0, getHeight(), new Color(14, 60, 76));
			g2d.setPaint(background);
			g2d.fillRect(0, 0, getWidth(), getHeight());
			g2d.setColor(new Color(81, 152, 169, 60));
			for (int i = 0; i < 38; i++) {
				int x = (int) ((i * 331L + 139) % Math.max(1, getWidth()));
				int y = (int) ((i * 197L + 71) % Math.max(1, getHeight()));
				g2d.fillOval(x, y, 3, 3);
			}
		} else if (currentScreen == Screen.SKILL_TREE) {
			skillTree.paint(g2d, getWidth(), getHeight());
		} else if (currentScreen == Screen.MAIN_MENU) {
			if (titleScreen_image != null) {
				g2d.drawImage(titleScreen_image, 0, 0, getWidth(), getHeight(), this);
			} else {
				g2d.setPaint(new GradientPaint(0, 0, new Color(7, 24, 39),
						0, getHeight(), new Color(18, 70, 83)));
				g2d.fillRect(0, 0, getWidth(), getHeight());
				g2d.setColor(Color.WHITE);
				g2d.setFont(new Font("Arial", Font.BOLD, 50));
				String title = "ASTEROIDS";
				g2d.drawString(title,
						(getWidth() - g2d.getFontMetrics().stringWidth(title)) / 2,
						(int) (getHeight() * 0.30));
			}
		} else {
			// Der Hintergrund liegt absichtlich hinter den Spielobjekten.
			// Das Bild wird auf die aktuelle Panelgröße skaliert; fehlt es,
			// wird der vorhandene gezeichnete Ersatzhintergrund verwendet.
			if (gameScreen_image != null) {
				g2d.drawImage(gameScreen_image, 0, 0, getWidth(), getHeight(), this);
			} else {
				g2d.setPaint(new GradientPaint(0, 0, new Color(10, 20, 32),
						0, getHeight(), new Color(23, 36, 48)));
				g2d.fillRect(0, 0, getWidth(), getHeight());
				g2d.setColor(new Color(255, 255, 255, 30));
				for (int i = 0; i < 90; i++) {
					int x = (int) ((i * 211L + 19) % Math.max(1, getWidth()));
					int y = (int) ((i * 173L + 41) % Math.max(1, getHeight()));
					g2d.fillRect(x, y, 2, 2);
				}
			}

			// Die Drehung gilt nur für das Raumschiff: Der bisherige Graphics2D-
			// Transformationszustand wird gespeichert und danach wiederhergestellt.
			// Andernfalls würden Schüsse, Asteroiden und Anzeigen mitgedreht.
			AffineTransform backup = g2d.getTransform();
			g2d.rotate(angle - Math.PI / 2, player.getObjectPosition().getX(), player.getObjectPosition().getY());
			player.paintMe(g2d);
			g2d.setTransform(backup);

			for (Shot shot : shots) shot.paintMe(g2d);
			for (Asteroid asteroid : asteroids) asteroid.paintMe(g2d);
			for (Ore ore : ores) ore.paintMe(g2d);

			if (gameMode == GameMode.ARCADE) {
				drawArcadeHud(g2d);
			} else {
				drawHud(g2d);
			}
		}
	}

	// Zeichnet Punkte, Highscore und überlebte Zeit für den Arcade-Modus.
	private void drawArcadeHud(Graphics2D g2d) {
		// Arcade-Anzeige links oben: Punkte, Rekord und Überlebenszeit.
		int x = 20;
		int y = 20;
		g2d.setColor(new Color(0, 0, 0, 155));
		g2d.fillRoundRect(x - 10, y - 10, 340, 156, 18, 18);
		g2d.setFont(new Font("Arial", Font.BOLD, 19));
		g2d.setColor(new Color(254, 207, 87));
		g2d.drawString("ARCADE-MODUS", x, y + 13);
		g2d.setFont(new Font("Arial", Font.BOLD, 18));
		g2d.setColor(Color.WHITE);
		g2d.drawString("Punkte: " + scoreFormat.format(arcadeScore), x, y + 41);
		g2d.drawString("Highscore: " + scoreFormat.format(arcadeHighscore), x, y + 66);
		g2d.drawString("Überlebt: " + (getElapsedGameMillis() / 1000L) + " s", x, y + 91);
		drawHearts(g2d, x, y + 108);
	}

	// Zeichnet im Normalmodus die verbleibende Zeit und das vorhandene Erz
	private void drawHud(Graphics2D g2d) {
		// Normaler HUD-Bereich: Zeitbalken und Erzbestand.
		int boxX = 18;
		int boxY = 16;
		int barWidth = 230;
		int barHeight = 18;

		g2d.setColor(new Color(0, 0, 0, 125));
		g2d.fillRoundRect(boxX - 10, boxY - 8, 275, 145, 18, 18);

		g2d.setColor(Color.WHITE);
		g2d.setFont(new Font("Arial", Font.BOLD, 16));
		g2d.drawString("Zeitlimit", boxX, boxY - 2);

		g2d.setColor(new Color(50, 70, 85));
		g2d.fillRoundRect(boxX, boxY + 6, barWidth, barHeight, 12, 12);

		//Zeichnet den verbleibenden Zeitanteil als farbigen Balken
		double ratio;
		if (roundDurationMillis <= 0) {
			ratio = 0;
		} else {
			ratio = (double) remainingTimeMillis / roundDurationMillis;
		}
		ratio = Math.max(0, Math.min(1, ratio));
		int currentWidth = (int) Math.round(barWidth * ratio);
		g2d.setColor(new Color(246, 214, 74));
		g2d.fillRoundRect(boxX, boxY + 6, currentWidth, barHeight, 12, 12);
		g2d.setColor(new Color(18, 20, 23));
		g2d.drawRoundRect(boxX, boxY + 6, barWidth, barHeight, 12, 12);

		g2d.setColor(Color.WHITE);
		g2d.drawString("Zeit: " + UpgradeWerte.formatSeconds(remainingTimeMillis / 1000.0) + " s", boxX, 60);
		//Anzeigen des vorhandenen Erzes
		g2d.drawString("Erz: " + saveData.money, boxX, 84);
		drawHearts(g2d, boxX, 102);
	}

	//Zeichnet je Lebenspunkt eine Kopie von res/assets/Herz.png
	private void drawHearts(Graphics2D g2d, int x, int y) {
		int maxHealth = Math.max(0, player.getMaxHealth());

		int currentHealth = Math.max(0, Math.min(maxHealth, player.getCurrentHealth()));

		if (heartImage == null) {
			// Ersatzanzeige, falls eine PNG-Datei nicht mitgeliefert wurde.
			g2d.setColor(Color.WHITE);

			g2d.drawString("Leben: " + currentHealth + "/" + maxHealth, x, y + 18);

			return;

		}

		// Nur die noch vorhandenen Herzen zeichnen
		for (int i = 0; i < currentHealth; i++) {

			g2d.drawImage(heartImage, x + i * 34, y, 29, 29, this);

		}
	}



	 //Erzeugt einen Schuss nur bei aktivem Spiel und nach Ablauf des Cooldowns
	private void fireShotIfPossible() {
		if (gameRunning && !gamePaused && currentScreen == Screen.PLAYING
				&& getElapsedGameMillis() >= nextShotAllowedAtMillis) {
			shots.add(player.generateShot(angle));
			nextShotAllowedAtMillis = getElapsedGameMillis() + shotCooldownMillis;
		}
	}

	@Override
	public void mouseClicked(MouseEvent e) {
	}

	@Override
	public void mouseEntered(MouseEvent e) {
	}

	@Override
	public void mouseExited(MouseEvent e) {
	}


	 //Reagiert auf die linke Maustaste und versucht zu schießen.
	@Override
	public void mousePressed(MouseEvent e) {
		if (e.getButton() == MouseEvent.BUTTON1) {
			//Prüft ob Schuss möglich ist
			fireShotIfPossible();
		}
	}

	@Override
	public void mouseReleased(MouseEvent e) {
	}


	 //Steuert Bewegung und Schießen; Escape verlässt den Skilltree.
	@Override
	public void keyPressed(KeyEvent e) {
		if (currentScreen == Screen.SKILL_TREE && e.getKeyCode() == KeyEvent.VK_ESCAPE) {
			showScreen(Screen.MAIN_MENU);
			return;
		}
		if (currentScreen != Screen.PLAYING) return;
		switch (e.getKeyCode()) {
			case KeyEvent.VK_UP:
			case KeyEvent.VK_W:
				playerMove = true;
				break;
			case KeyEvent.VK_SPACE:
				fireShotIfPossible();
				break;
		}
	}


	//Beendet die Beschleunigung und verarbeitet das Anhalten per Abwärtstaste.
	//Beim Loslassen von W oder Pfeil hoch endet die aktive Beschleunigung
	@Override
	public void keyReleased(KeyEvent e) {
		if (currentScreen != Screen.PLAYING) return;
		switch (e.getKeyCode()) {
			case KeyEvent.VK_UP:
			case KeyEvent.VK_W:
				playerMove = false;
				break;
			case KeyEvent.VK_DOWN:
				player.setMovingDistance(0);
				break;
		}
	}


	@Override
	public void keyTyped(KeyEvent e) {
	}


	@Override
	public void mouseDragged(MouseEvent e) {
	}


	//Speichert die Mauskoordinaten für die Ausrichtung des Raumschiffs
	@Override
	public void mouseMoved(MouseEvent e) {
		mouseX = e.getX();
		mouseY = e.getY();
	}


	//Positioniert sichtbare Menü-, Slot- und Options-Elemente passend zur Bildschirmgröße.
	//Setzt Sichtbarkeit und Position aller Swing-Komponenten nach aktuellem Bildschirm und
	//Fenstergröße
	@Override
	public void doLayout() {
		super.doLayout();
		if (startGameButton == null || optionsButton == null || slotButtons[0] == null) return;

		//Sichtbare Steuerelemente und ihre Positionen hängen vom aktiven Bildschirm ab
		boolean menu = currentScreen == Screen.MAIN_MENU;
		boolean choosing = currentScreen == Screen.SLOT_SELECTION;
		boolean playing = currentScreen == Screen.PLAYING;
		if (skillTree != null) skillTree.layout(getWidth(), getHeight());

		startGameButton.setVisible(menu);
		arcadeStartButton.setVisible(menu);
		arcadeHighscoreLabel.setVisible(menu);
		arcadePointsLabel.setVisible(menu);
		skillTreeButton.setVisible(menu && selectedSlot != 0);
		optionsButton.setVisible(playing);
		controlsButton.setVisible(menu || playing);
		slotSelectionTitle.setVisible(choosing);
		slotSelectionHelp.setVisible(choosing);
		backToMenuButton.setVisible(choosing);
		for (Button slotButton : slotButtons) slotButton.setVisible(choosing);
		for (Button deleteButton : deleteSlotButtons) deleteButton.setVisible(choosing);

		if (menu) {
			// Menü: Der Hilfe-Button sitzt allein oben rechts.
			controlsButton.setBounds(getWidth() - 70, 24, 52, 52);
			int buttonWidth = Math.min(240, Math.max(180, (getWidth() - 100) / 2 - 14));
			int buttonHeight = 68;
			int gap = 22;
			int groupX = (getWidth() - 2 * buttonWidth - gap) / 2;
			int buttonY = (int) (getHeight() * 0.69);
			startGameButton.setBounds(groupX, buttonY, buttonWidth, buttonHeight);
			arcadeStartButton.setBounds(groupX + buttonWidth + gap, buttonY, buttonWidth, buttonHeight);
			arcadeHighscoreLabel.setBounds(0, buttonY - 104, getWidth(), 44);
			arcadePointsLabel.setBounds(0, buttonY - 48, getWidth(), 35);
			skillTreeButton.setBounds((getWidth() - 195) / 2, buttonY + buttonHeight + 46,
					195, 53);
		} else if (choosing) {
			int width = getWidth();
			int height = getHeight();
			slotSelectionTitle.setBounds(0, (int) (height * 0.22), width, 64);
			slotSelectionHelp.setBounds(0, (int) (height * 0.32), width, 42);
			int gap = 22;
			int cardWidth = Math.min(300, Math.max(155, (width - 100 - 2 * gap) / 3));
			int cardHeight = Math.min(195, Math.max(145, (int) (height * 0.20)));
			int startX = (width - (3 * cardWidth + 2 * gap)) / 2;
			int y = (int) (height * 0.42);
			for (int i = 0; i < slotButtons.length; i++) {
				int slotX = startX + i * (cardWidth + gap);
				slotButtons[i].setBounds(slotX, y, cardWidth, cardHeight);
				int crossSize = 35;
				deleteSlotButtons[i].setBounds(cardWidth - crossSize - 5,
						5, crossSize, crossSize);
			}
			backToMenuButton.setBounds((width - 270) / 2,
					(int) (height * 0.73), 270, 50);
		} else {
			int optionsWidth;
			int optionsHeight;
			if (optionsHasIcon) {
				optionsWidth = 75;
				optionsHeight = 75;
			} else {
				optionsWidth = 130;
				optionsHeight = 54;
			}
			//Plazieren des Options_Buttons oben rechts
			optionsButton.setBounds(getWidth() - optionsWidth - 12, 24,
					optionsWidth, optionsHeight);
			//Steuerungshilfe ist im Spiel links neben dem Options-Button
			controlsButton.setBounds(getWidth() - optionsWidth - 78, 34, 52, 52);
		}
	}
}

