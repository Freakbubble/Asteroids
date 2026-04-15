package Raumschiff;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Color;

public class Player extends GameObject {
	// Variablen
	int shotsize = 2;
	int shotspeed = -4;
	private double acceleration = 0.5;
	private double maxSpeed = 3;

	public Player(Coordinate objectPosition, double width, double height, double movingAngle, double movingDistance) {

		// TODO Auto-generated constructor stub
		super(objectPosition, width, height);
		setMovingAngle(movingAngle); // Achtung der Winkel wird im Bogenma� angegeben!!
		setMovingDistance(movingDistance);
	}

	@Override
	public void makeMove() {
		// TODO Auto-generated method stub
		super.makeMove();
	}

	public void setAcceleration(double new_acceleration) {
		acceleration = new_acceleration;
	}
	
	public double getAcceleration(){
		return acceleration;
	}
	
	public void setMaxSpeed(double new_maxSpeed) {
		maxSpeed =new_maxSpeed;
	}
	
	public double getMaxSpeed() {
		return maxSpeed;
	}

	public Shot generateShot(double movingAngle_Raumschiff) {
		Shot shot = new Shot(
				new Coordinate(this.getObjectPosition().getX() - shotsize / 2, this.getObjectPosition().getY()),
				shotsize, shotsize, movingAngle_Raumschiff, shotspeed);
		return shot;
	}

	public void paintMe(Graphics g) {
		Graphics2D g2d = (Graphics2D) g;
		g2d.setColor(Color.RED);
		int x_pos = (int) this.getObjectPosition().getX();
		int y_pos = (int) this.getObjectPosition().getY();
		int[] x_poly = { x_pos, x_pos - 10, x_pos, x_pos + 10 };
		int[] y_poly = { y_pos, y_pos + 15, y_pos + 10, y_pos + 15 };
		g2d.fillPolygon(x_poly, y_poly, 4); // Zeichnung beginnt an der Spitze des Raumschiffs.
	}

}