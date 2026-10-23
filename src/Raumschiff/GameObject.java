package Raumschiff;

public abstract class GameObject {
	private Coordinate objectPosition;
	private double width;
	private double height;
	protected double movingAngle;   //Achtung: Winkel ist im Bogenma� angegeben!
	protected double movingDistance;
	
	
	public GameObject(Coordinate objectPosition, double width, double height) {
		this.objectPosition = objectPosition;
		this.width = width;
		this.height = height;
		movingAngle = 0;	//das Objekt wird statisch erzeugt
		movingDistance = 0; //das Objekt wird statisch erzeugt
	}

	public Coordinate getObjectPosition() {
		return objectPosition;
	}

	public void setObjectPosition(Coordinate objectPosition) {
		this.objectPosition = objectPosition;
	}

	public double getWidth() {
		return width;
	}

	public void setWidth(double width) {
		this.width = width;
	}

	public double getHeight() {
		return height;
	}

	public void setHeight(double height) {
		this.height = height;
	}

	public double getMovingAngle() {
		return movingAngle;
	}

	public void setMovingAngle(double movingAngle) {
		this.movingAngle = movingAngle;
	}

	public double getMovingDistance() {
		return movingDistance;
	}

	public void setMovingDistance(double movingDistance) {
		this.movingDistance = movingDistance;
	}
		
	//Durch die Angabe "static" kann die Methode �ber den Klassennamen auch von anderen Klassen aus aufgerufen werden.
	public static Coordinate polarToCartesianCoordinates(double angle) {
		//X-Achse zeigt nach Osten, Y-Achse zeigt nach Sueden beim Zeichnen
		double x = Math.cos(angle);
		double y = Math.sin(angle);
		
		return new Coordinate(x,y);
	}
	
	public void moveGameObject() {
		Coordinate direction = polarToCartesianCoordinates(movingAngle);
		
		objectPosition.setX(objectPosition.getX() + direction.getX()*movingDistance);
		objectPosition.setY(objectPosition.getY() + direction.getY()*movingDistance);
	}

	public void makeMove() {
		moveGameObject();
	}

	public boolean checkCollision(GameObject otherObject) { // Prüfen ob zwei runde Objekte kollidieren
		// Mittelpunkt und Radius vom Objekt berechnen, das die Methode aufruft (this)
		double x1 = this.getObjectPosition().getX() + this.getWidth() / 2; // x- Koordinate des Objekts
		double y1 = this.getObjectPosition().getY() + this.getHeight() / 2; // y- Koordinate des Objekts
		double r1 = this.getHeight() / 2; // Radius des Objekts

		// Mittelpunkt und Radius des (other)Objekts berechnen mit dem es zu einer Kollision kommen könnte
		double x2 = otherObject.getObjectPosition().getX() + otherObject.getWidth() / 2; // x- Koordinate des
		double y2 = otherObject.getObjectPosition().getY() + otherObject.getHeight() / 2; // x- Koordinate des
		double r2 = otherObject.getHeight() / 2; // asteroid-Radius

		// 1. Schritt: Schneller Test mit AABB(Axis Aligned Bounding Box) Kollisionsüberprüfung
		// um zu sehen, ob es zwichen den beiden Kreisobjekten überhaupt zu einer Kollision kommen könnte
		double maxDistance = r1 + r2;
		if (Math.abs(x1 - x2) > maxDistance || Math.abs(y1 - y2) > maxDistance){
			return false;
		}
		// 2. Schritt: Ausführlicher Test ob beide Objekte wirklich kollidieren

		double dx = x1 - x2;
		double dy = y1 - y2;
		double rsum = r1 + r2;
		double hypote = (dx * dx) + (dy * dy);

		return hypote <= rsum * rsum;
	}


	
	protected abstract void paintMe(java.awt.Graphics g);
}
