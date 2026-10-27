package Raumschiff;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseListener;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import javax.imageio.ImageIO;
import javax.swing.*;
import java.util.Iterator;

public class Spielfeld extends JPanel implements MouseListener, KeyListener, MouseMotionListener { // JPanel ist eine Klasse, in der
																				// gezeichnet werden kann
	//Abfragen der aktuellen Bildschirmgröße und Setzen der Dimension auf diese
	private final Dimension prefSize = new Dimension(Toolkit.getDefaultToolkit().getScreenSize());

	private int k = 0;

	private Timer t;
	private Timer s;

	private Cursor c;

	private boolean gameRunning;
	private BufferedImage spielfeld_image;

	// Asteroidvariablen
	private ArrayList<Asteroid> asteroids;
	private ArrayList<Ore> ores;
	private int startingAsteroidcount = 50;
	private int spwandelay = 5;


	// Player- und Shot-Variablen
	private Player player;
	private ArrayList<Shot> shots;
	private boolean playerMove;
	private boolean canShot;

	// Winkel für die Bewegungen des Spielers,der Schüsse und der AffineTransform-Klasse
	private double angle;

	//Mouseposition
	private double mouseX;
	private double mouseY;

	//Buttons für das Spielfeld
	private Button optionsButton;
	private Button startGameButton;

	public Spielfeld() {
		setFocusable(true);
		setPreferredSize(prefSize);
		setLayout(null);

		gameRunning = false;

		initGame(); // zum Erstellen der Oberfl�che (Ausgangszustand)
//		startGame(); // Starten des Timers. Dieser ruft die Methode doOnTick() auf, in der die
//						// Ver�nderungen passieren.

	}

	private void initPlayer() {
		player = new Player(new Coordinate(prefSize.getWidth() / 2, prefSize.getHeight() * 0.9), 10, 10, Math.PI, 0);
		shots = new ArrayList<>();
	}

	private void initAsteroid() {

		asteroids = new ArrayList<>();
		for (int i = 0; i < startingAsteroidcount; i++) {
			asteroids.add(new Asteroid(prefSize, 40, 40, Math.random()*2*Math.PI, 1,
					new Color(0, 0, 0)));
		}
		ores = new ArrayList<>();
	}

	private void initGame() {

		//Hinzufügen des Buttons der das Optionsmenü aufruft
		ImageIcon settingsIcon =
				new ImageIcon("res/assets/Option_Menu.png");

		optionsButton = new Button(settingsIcon,  48, 48);

		optionsButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if(gameRunning){
				pauseGame();
				}
				// Optionsmenü öffnen
			}
		});

		add(optionsButton);

		//Hinzufügen des Buttons der das Spiel startet

		startGameButton = new Button("Spiel starten",new Color (255,255, 255));
		startGameButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				gameRunning = true;

			revalidate(); //Die doLayout Methode wird noch einmal aufgerufen um die Position des Buttons zu verändern
			repaint();	  //Löschen des Buttons an der alten Position und zeichnen an der neuen Position
			}
		});

		add(startGameButton);

//		try {		//Laden der Bilddatei des Spielfeld
//			spielfeld_image = ImageIO.read( new File( "res/assets/background.jpg" ) );
//		} catch (IOException e) {
//			System.out.println(e);
//		}

		// asteroid und Player initiieren
		initPlayer();
		initAsteroid();
		canShot = true;



		// Registrieren des MouseListeners
		addMouseListener(this);
		
		//Registrieren des MouseMotionListeners
		addMouseMotionListener(this);

		// Registrieren des KeyListeners
		addKeyListener(this);

		// Mauszeiger wird zu Fadenkreuz
		c = new Cursor(Cursor.CROSSHAIR_CURSOR);
		this.setCursor(c);

		t = new Timer(20, new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				doOnTick();
			}
		});

		// Schuss-Timer
		s = new Timer(500, new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				canShot = true;
				s.stop();
			}
		});

	}

	private void startGame() {
		t.start();
	}

	public void pauseGame() {
		t.stop();
	}

	public void continueGame() {
		if (gameRunning) {
			t.start();
		}
	}

	// Prüft, ob ein Gegner von einem Schuss getroffen wurde
	private void asteroidHit() {

		// Methode direkt abbrechen, wenn keine Schüsse oder Asteroiden vorhanden sind
		if (shots.isEmpty() || asteroids.isEmpty()) {
			return;
		}

			Iterator<Shot> shotIterator = shots.iterator();
			Iterator<Asteroid> asteroidIterator =asteroids.iterator();

			while (shotIterator.hasNext()) {

				Shot shot = shotIterator.next();

				while (asteroidIterator.hasNext()) {

					Asteroid asteroid = asteroidIterator.next();

					if (Kollision.checkCollisionBetweenCircles(asteroid, shot)) {
						shotIterator.remove(); // Shot wird gelöscht
						asteroidIterator.remove();
						ores.add(asteroid.generateOre()); // Erz wird erzeugt
						break;
					}
				}
			}
		}

	
	//Berechnen des Winkels zwischen Raumschiff und Mauszeiger
	
	private double movingAngle() {
		
		double dx = player.getObjectPosition().getX() -mouseX;
		double dy = player.getObjectPosition().getY() - mouseY;
		double hypothe = Math.sqrt((dx * dx) + (dy * dy));
		double angle = Math.acos(dx/hypothe);

		if (dy >= 0){
			return angle;
		}else
			return -angle;
		}

		private void checkShot(){

			Iterator<Shot> iShot = shots.iterator();

			while(iShot.hasNext()){

				Shot shot = iShot.next();

				shot.makeMove();

				if(shot.isOut(prefSize)){

					iShot.remove();

				}
			}
		}

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

		private boolean checkPlayerMouseDistance() {

			double dx = player.getObjectPosition().getX() - mouseX;
			double dy = player.getObjectPosition().getY() - mouseY;

			double distancePlayerMouse = (dx * dx) + (dy * dy);
			double minDistance = ((Math.abs(3 * player.getMaxSpeed()) + Math.abs(2 * player.getAcceleration())) * 3 * Math.abs(player.getMaxSpeed()) / 2.25);

			if(distancePlayerMouse > (minDistance * minDistance)) {
				return true;
			} else {
				return false;
			}
		}


	private void doOnTick() {

		angle = movingAngle();

		// Die einzelnen Schuesse werden bewegt und auf Verlassen der
		// Spielflaeche ueberprueft.
		checkShot();

		// Die einzelnen Asteroiden werden bewegt und auf Verlassen der Spielflaeche ueberprueft
		checkAsteroid();
		asteroidHit();

		// Bewegt den Spieler
		player.setMovingAngle(angle);

		if(playerMove && checkPlayerMouseDistance()) {					//Playerbewegung mit Beschleunigung und maximalem Speed
			if(player.getMovingDistance()> player.getMaxSpeed()) {
			player.setMovingDistance(player.getMovingDistance() + player.getAcceleration());
		}
		}else{										// Simulierte Reibung beim Nichtdrücken der Tasten
			if(player.getMovingDistance() < 0) {
				player.setMovingDistance(player.getMovingDistance() -  0.5 * player.getAcceleration());
			}
		}
		player.makeMove();		
		repaint();
		for (Asteroid asteroid : asteroids){
		if(Kollision.checkPlayerAsteroidCollision(player, asteroid)){
			t.stop();
			s.stop();
		}
		}
	}

	public void paintComponent(Graphics g) {
		super.paintComponent(g);

		Graphics2D g2d = (Graphics2D) g;
		// Um die Kanten des Objekts zu glaetten
		g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

		if (!gameRunning) {
			g2d.setFont(new Font(Font.MONOSPACED, Font.BOLD, 20));
			g2d.setColor(Color.BLUE);
			g2d.drawString("Fuehre einen Doppelklick aus,", 20, 20);
			g2d.drawString("um das Spiel zu starten!", 20, 40);

		} else {
			// alles, was gemacht werden muss, w�hrend das Spiel l�uft
			g.drawImage(spielfeld_image, 0, 0, getWidth(), getHeight(), this);
			AffineTransform backup = g2d.getTransform();
			g2d.rotate(angle - Math.PI/2,player.getObjectPosition().getX(),player.getObjectPosition().getY());
			player.paintMe(g2d);
			g2d.setTransform(backup);

			// Zeichnen der Schüsse der Shot-ArrayList
			for (Shot shot : shots) {
					shot.paintMe(g2d);
			}

			// Zeichnen der Asteroiden der Asteroids-ArrayList
			for (Asteroid asteroid : asteroids) {
					asteroid.paintMe(g);
			}

			// Zeichnen der Erze der Ore-ArrayList
			for (Ore ore : ores) {
				ore.paintMe(g);
				}

			}
		Coordinate[] playerPoints = player.getPolgygonHitbox();

		 int[] x_poly= {(int)playerPoints[0].getX(), (int)playerPoints[1].getX(), (int)playerPoints[2].getX(), (int)playerPoints[3].getX()};
		 int[] y_poly= {(int)playerPoints[0].getY(), (int)playerPoints[1].getY(), (int)playerPoints[2].getY(), (int)playerPoints[3].getY()};
		 g2d.fillPolygon(x_poly, y_poly,4);



		}




	@Override
	public void mouseClicked(MouseEvent e) {
		// TODO Auto-generated method stub

	}

	@Override
	public void mouseEntered(MouseEvent e) {
		// TODO Auto-generated method stub

	}

	@Override
	public void mouseExited(MouseEvent e) {
		// TODO Auto-generated method stub

	}

	@Override
	public void mousePressed(MouseEvent e) {
		// TODO Auto-generated method stub
		switch(e.getButton()){
			case MouseEvent.BUTTON1:
				if(gameRunning && canShot) {	//neuen Schuss mit linker Maustaste erzeugen, nur wenn das Spiel läuft
					shots.add(player.generateShot(angle));
					canShot = false;
					s.start();
				}
				break;
		}
	}

	@Override
	public void mouseReleased(MouseEvent e) {
		// TODO Auto-generated method stub
		if (!gameRunning && e.getClickCount() == 2) {
			// Alle wichtigen Werte zur�cksetzen
			gameRunning = true;

			revalidate(); //Die doLayout Methode wird noch einmal aufgerufen um die Position des Buttons zu verändern
			repaint();	  //Löschen des Buttons an der alten Position und zeichnen an der neuen Position

			startGame();
		}
	}

	@Override
	public void keyPressed(KeyEvent e) {
		// TODO Auto-generated method stub
		switch (e.getKeyCode()) {
		case KeyEvent.VK_UP:
			playerMove=true;
			break;
			case KeyEvent.VK_W:
			playerMove = true;
			break;
		case KeyEvent.VK_SPACE:  // neuen Schuss mit Space-Taste erzeugen und in Array speichern
			if (canShot) {
				shots.add(player.generateShot(angle));
				canShot = false;
				s.start();
			}
			break;
		}

	}

	@Override
	public void keyReleased(KeyEvent e) {
		switch (e.getKeyCode()) {
			case KeyEvent.VK_UP:
				playerMove = false;
				break;
			case KeyEvent.VK_DOWN:
				player.setMovingDistance(0);
				break;
			case KeyEvent.VK_W:
				playerMove = false;
				break;
		}
	}

	@Override
	public void keyTyped(KeyEvent e) {
		// TODO Auto-generated method stub

	}

	@Override
	public void mouseDragged(MouseEvent e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void mouseMoved(MouseEvent e) {
		// TODO Auto-generated method stub
		mouseX = e.getX();
		mouseY = e.getY();
	}

	@Override
	public void doLayout() {
		super.doLayout();


		int buttonWidth = 48;
		int buttonHeight = 48;
		if (optionsButton != null) {
			if(!gameRunning){
				optionsButton.setBounds(
						(int) (0.5 * getWidth() - 0.5* buttonWidth),
						(int) (0.5 * getHeight()- 0.5* buttonHeight),
						buttonWidth,
						buttonHeight
				);
			}
			else{
				optionsButton.setBounds(
						getWidth() - buttonWidth -4,
						40,
						buttonWidth,
						buttonHeight
				);
			}
		}

	}

}
