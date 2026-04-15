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
//import java.util.ArrayList;

import javax.swing.JPanel;
import javax.swing.Timer;

public class Spielfeld extends JPanel implements MouseListener, KeyListener, MouseMotionListener { // JPanel ist eine Klasse, in der
																				// gezeichnet werden kann

	private final Dimension prefSize = new Dimension(800, 400); // Die Dimension des Spielfeldes k�nnte auch anders
																// gew�hlt werden...

	private Timer t;
	private Timer s;

	private Cursor c;

	private boolean isStopped;

	// Enemyvariablen
	private boolean enemyAlive;
	private Enemy enemy;

	// Player- und Shot-Variablen
	private Player player;
	private Shot[] shots;
	private boolean playerMoveUp;

	//Mouseposition
	private double mouseX;
	private double mouseY;

	public Spielfeld() {
		setFocusable(true);
		setPreferredSize(prefSize);

		isStopped = true;

		initGame(); // zum Erstellen der Oberfl�che (Ausgangszustand)
		startGame(); // Starten des Timers. Dieser ruft die Methode doOnTick() auf, in der die
						// Ver�nderungen passieren.

	}

	private void initPlayer() {
		player = new Player(new Coordinate(prefSize.getWidth() / 2, prefSize.getHeight() * 0.9), 10, 10, Math.PI, 0);
		shots = new Shot[5];
	}

	private void initEnemy() {

		enemy = new Enemy(new Coordinate(prefSize.getWidth() / 2, prefSize.getHeight() / 2), 20, 20, 0, 1,
				new Color(0, 0, 0));

		enemyAlive = true;
	}

	private void initGame() {

		// Enemy und Player initiieren
		initPlayer();
		initEnemy();

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
				enemyAlive = true;
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
		if (!isStopped) {
			t.start();
		}
	}

	// Prüft, ob ein Gegner von einem Schuss getroffen wurde
	private void enemyHit() {

		for (int i = 0; i < shots.length; i++) {
			Shot shot = shots[i];
			if (shot == null)
				continue;
			double sx = shot.getObjectPosition().getX() + shot.getWidth() / 2; // x- Koordinate des Shot-Mittelpunkts
			double sy = shot.getObjectPosition().getY() + shot.getHeight() / 2; // y- Koordinate des Shot-Mittelpunkts
			double sr = shot.getHeight() / 2; // Shot-Radius

			double ex = enemy.getObjectPosition().getX() + enemy.getWidth() / 2; // x- Koordinate des Enemy-Mittelpunkts
			double ey = enemy.getObjectPosition().getY() + enemy.getHeight() / 2; // x- Koordinate des
																					// Enemy-Mittelpunkts
			double er = enemy.getHeight() / 2; // Enemy-Radius

			if (enemy.checkCollision(sx, ex, sy, ey, sr, er)) {
				shots[i] = null; // Shot wird gelöscht
				enemyAlive = false; // Enemy stirbt
				s.start(); // Respawn-Timer starten
				break;
			}
		}
	}
	
	//Berechnen den Winkel zwischen Raumschiff und Mauszeiger
	
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


	private void doOnTick() {

		// Die einzelnen Sch�sse werden bewegt und auf Verlassen der
		// Spielfl�che �berpr�ft.
		for (int i = 0; i < shots.length; i++) {
			if (shots[i] != null) { // Bewegung des Schusses
				shots[i].makeMove();

				// test if shot is out
				if (shots[i].getObjectPosition().getY() < 0 ||
						shots[i].getObjectPosition().getY() + shots[i].getHeight()> prefSize.getHeight() ||
						shots[i].getObjectPosition().getX()< 0 ||
					shots[i].getObjectPosition().getX() + shots[i].getWidth() > prefSize.getWidth()){ // remove shot from array
					shots[i] = null;
				}

			}

		}
		
		if (enemyAlive) { // Bewegen des lebendigen Enemy

			enemy.makeMove();
			enemyHit();

			if (enemy.getObjectPosition().getX() <= 0
					|| enemy.getObjectPosition().getX() + enemy.getWidth() >= prefSize.getWidth()) {
				
				enemy.bounce(); // Abprallen des Gegners am Rand
				
			}
		}

		// move player
		player.setMovingAngle(movingAngle());
		if(playerMoveUp) {					//Playerbewegung mit Beschleunigung und maximalem Speed
			player.setMovingDistance(-5);
//			if(player.getMovingDistance()<= player.getMaxSpeed()) {
//			player.setMovingDistance(player.getMovingDistance() + player.getAcceleration());

		}else {										// Simulierte Reibung beim Nichtdrücken der Tasten
			player.setMovingDistance(0);
//			if(player.getMovingDistance() > 0) {
//				player.setMovingDistance(player.getMovingDistance() - 0.25 * player.getAcceleration());
//			}
		}
		player.makeMove();		
		repaint();
	}

	public void paintComponent(Graphics g) {
		super.paintComponent(g);

		Graphics2D g2d = (Graphics2D) g;
		// Um die Kanten des Objekts zu gl�tten
		g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

		if (isStopped) {
			g2d.setFont(new Font(Font.MONOSPACED, Font.BOLD, 20));
			g2d.setColor(Color.BLUE);
			g2d.drawString("Fuehre einen Doppelklick aus,", 20, 20);
			g2d.drawString("um das Spiel zu starten!", 20, 40);

		} else {
			// alles, was gemacht werden muss, w�hrend das Spiel l�uft
			player.paintMe(g2d);

			for (int i = 0; i < shots.length; i++) {
				if (shots[i] != null) {
					shots[i].paintMe(g2d);
				}
			}
			if (enemyAlive) { // Zeichnen des Enemy
				enemy.paintMe(g);
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

	}

	@Override
	public void mouseReleased(MouseEvent e) {
		// TODO Auto-generated method stub
		if (isStopped && e.getClickCount() == 2) {
			// Alle wichtigen Werte zur�cksetzen
			isStopped = false;
		}
	}

	@Override
	public void keyPressed(KeyEvent e) {
		// TODO Auto-generated method stub
		switch (e.getKeyCode()) {
		case KeyEvent.VK_UP:
			playerMoveUp=true; 
			break;
		case KeyEvent.VK_DOWN:
			player.setMovingDistance(5);
			break;
		case KeyEvent.VK_SPACE:  // neuen Schuss mit Space-Taste erzeugen und in Array speichern
			for (int i = 0; i < shots.length; i++) {
				if (shots[i] == null) { // Falls ein Platz "frei" ist.
					shots[i] = player.generateShot(movingAngle());
					break;
				}
			}
		}
	}

	@Override
	public void keyReleased(KeyEvent e) {
		switch (e.getKeyCode()) {
		case KeyEvent.VK_UP:
			playerMoveUp = false; 
		case KeyEvent.VK_DOWN:
			player.setMovingDistance(0);
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
