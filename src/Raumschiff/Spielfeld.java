package Raumschiff;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseListener;
import java.awt.geom.AffineTransform;
import java.awt.Toolkit;
import java.util.ArrayList;
import javax.swing.JPanel;
import javax.swing.Timer;
import java.util.Iterator;

public class Spielfeld extends JPanel implements MouseListener, KeyListener, MouseMotionListener { // JPanel ist eine Klasse, in der
																				// gezeichnet werden kann
	//Abfragen der aktuellen Bildschirmgröße und Setzen der Dimension auf diese
	private final Dimension prefSize = new Dimension(Toolkit.getDefaultToolkit().getScreenSize());

	private Timer t;
	private Timer s;

	private Cursor c;

	private boolean gameRunning;

	// Asteroidvariablen
	private boolean[] asteroidAlive;
	private Asteroid[] asteroid;
	private Ore[] ore;

	// Player- und Shot-Variablen
	private Player player;
	private ArrayList<Shot> shots;
	private boolean playerMoveUp;

	// Winkel für die Bewegungen des Spielers,der Schüsse und der AffineTransform-Klasse
	private double angle;

	//Mouseposition
	private double mouseX;
	private double mouseY;

	public Spielfeld() {
		setFocusable(true);
		setPreferredSize(prefSize);

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

		asteroid = new Asteroid[1000];
		asteroidAlive = new boolean[1000];
		for (int i = 0; i < asteroid.length; i++) {
			asteroid[i] = new Asteroid(new Coordinate(prefSize.getWidth() / 2, prefSize.getHeight() / 2), 30, 30, Math.random()*2*Math.PI, 1,
					new Color(0, 0, 0));
			asteroidAlive[i] = true;
		}
		ore = new Ore[15];
	}

	private void initGame() {

		// asteroid und Player initiieren
		initPlayer();
		initAsteroid();

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

		// Respawn Timer
		s = new Timer(500, new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				for (int i = 0; i < asteroidAlive.length ; i++) {
					asteroidAlive[i] = true;
				}
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

			Iterator<Shot> shotIterator = shots.iterator();

			while (shotIterator.hasNext()) {

				Shot shot = shotIterator.next();

				double sx = shot.getObjectPosition().getX() + shot.getWidth() / 2; // x- Koordinate des Shot-Mittelpunkts
				double sy = shot.getObjectPosition().getY() + shot.getHeight() / 2; // y- Koordinate des Shot-Mittelpunkts
				double sr = shot.getHeight() / 2; // Shot-Radius
				for (int j = 0; j < asteroid.length; j++) {
					double ex = asteroid[j].getObjectPosition().getX() + asteroid[j].getWidth() / 2; // x- Koordinate des asteroid-Mittelpunkts
					double ey = asteroid[j].getObjectPosition().getY() + asteroid[j].getHeight() / 2; // x- Koordinate des
					// asteroid-Mittelpunkts
					double er = asteroid[j].getHeight() / 2; // asteroid-Radius

					if (asteroid[j].checkCollision(sx, ex, sy, ey, sr, er)) {
						shotIterator.remove(); // Shot wird gelöscht
						asteroidAlive[j] = false;
						ore[j] = asteroid[j].generateOre();// Asteroid wird zerstört
						s.start(); // Respawn-Timer starten
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


	private void doOnTick() {

		angle = movingAngle();

		// Die einzelnen Sch�sse werden bewegt und auf Verlassen der
		// Spielfl�che �berpr�ft.
		checkShot();

		for (int i = 0; i < asteroidAlive.length; i++) {
			if (asteroidAlive[i]) { // Bewegen des lebendigen asteroid

				asteroid[i].makeMove();

//				if (asteroid[i].getObjectPosition().getX() <= 0
//						|| asteroid[i].getObjectPosition().getX() + asteroid[i].getWidth() >= prefSize.getWidth()) {
//					asteroid[i].bounce(); // Abprallen des Gegners am Rand
//
//				}
			}
		}
		asteroidHit();


		// move player
		player.setMovingAngle(angle);
		if(playerMoveUp) {					//Playerbewegung mit Beschleunigung und maximalem Speed
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
	}

	public void paintComponent(Graphics g) {
		super.paintComponent(g);

		Graphics2D g2d = (Graphics2D) g;
		// Um die Kanten des Objekts zu gl�tten
		g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

		if (!gameRunning) {
			g2d.setFont(new Font(Font.MONOSPACED, Font.BOLD, 20));
			g2d.setColor(Color.BLUE);
			g2d.drawString("Fuehre einen Doppelklick aus,", 20, 20);
			g2d.drawString("um das Spiel zu starten!", 20, 40);

		} else {
			// alles, was gemacht werden muss, w�hrend das Spiel l�uft
			AffineTransform backup = g2d.getTransform();
			g2d.rotate(angle - Math.PI/2,player.getObjectPosition().getX(),player.getObjectPosition().getY());
			player.paintMe(g2d);
			g2d.setTransform(backup);

			for (Shot shot : shots) {
					shot.paintMe(g2d);
			}

			for (int i = 0; i < asteroidAlive.length; i++) {
				if (asteroidAlive[i]) { // Zeichnen des asteroid
					asteroid[i].paintMe(g);
				}
			}

			for (int i = 0; i < ore.length; i++) {
				if(ore[i] != null){
				ore[i].paintMe(g);
				}

			}

		}


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
				if(gameRunning) {	//neuen Schuss mit linker Maustaste erzeugen, nur wenn das Spiel läuft
					shots.add(player.generateShot(angle));
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
			startGame();
		}
	}

	@Override
	public void keyPressed(KeyEvent e) {
		// TODO Auto-generated method stub
		switch (e.getKeyCode()) {
		case KeyEvent.VK_UP:
			playerMoveUp=true; 
			break;
			case KeyEvent.VK_W:
			playerMoveUp = true;
			break;
		case KeyEvent.VK_SPACE:  // neuen Schuss mit Space-Taste erzeugen und in Array speichern
			shots.add(player.generateShot(angle));
			break;
		}

	}

	@Override
	public void keyReleased(KeyEvent e) {
		switch (e.getKeyCode()) {
			case KeyEvent.VK_UP:
				playerMoveUp = false;
				break;
			case KeyEvent.VK_DOWN:
				player.setMovingDistance(0);
				break;
			case KeyEvent.VK_W:
				playerMoveUp = false;
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

}
