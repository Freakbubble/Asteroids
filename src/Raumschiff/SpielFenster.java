package Raumschiff;

import javax.swing.JFrame;  
import javax.swing.JMenuBar;	
import javax.swing.JMenu;		
import javax.swing.JMenuItem;	
import java.awt.event.ActionListener;	
import java.awt.event.ActionEvent;		

import java.awt.event.WindowAdapter;	
import java.awt.event.WindowEvent;		


public class SpielFenster extends JFrame{ //Durch Ableiten von JFrame werden z.B.Fenstereigenschaften geerbt
										  //Dar�ber hinaus ist JFrame ein Container f�r andere Objekte (JPanel, Menu,..)
	private Spielfeld spielfeld;
	
	public SpielFenster() {
		
		//Spielfeld erzeugen
		spielfeld = new Spielfeld();		
		
		registerWindowListener();    // WindowListener registrieren (z.B. Schlie�en des Fensters)
		createMenu();  // Erzeugen des Men�s
		
		add(spielfeld);  //Hinzuf�gen des Spielfeldes zum SpielFenster ; (add() erben alle von Container)
		pack();  //Ideale Gr��e berechnen
		
		this.setTitle("Asteroids");
		this.setLocation(10,10); //Linke obere Fensterecke festlegen
		this.setResizable(false);
		this.setVisible(true);
		
		
		repaint();
	}
	
	private void createMenu() {  
      
	    JMenuBar menuBar = new JMenuBar();  //Men�leiste erzeugen...
	    this.setJMenuBar(menuBar);	//...und dem JFrame hinzuf�gen
	         
	    JMenu fileMenu = new JMenu("Datei");  //Drei Men�s erzeugen...
	    JMenu gameMenu = new JMenu("Spiel");
	    JMenu prefMenu = new JMenu("Einstellungen");
	         
	    menuBar.add(fileMenu);    //... und zur Men�leiste hinzuf�gen    
	    menuBar.add(gameMenu);        
	    menuBar.add(prefMenu);
	        
	    addFileMenuItems(fileMenu); //Methode f�r Men�eintrag aufrufen
	    							// und Men�eintrag erzeugen
	    addSpielMenuItems(gameMenu);
	}
	     
	private void addFileMenuItems(JMenu fileMenu) {  
	         
	    JMenuItem quitItem = new JMenuItem("Ende");
	    fileMenu.add(quitItem); 
	    
	  // Erg�nzen eines ActionListeners (um auf einen Mausklick zu reagieren)
	    quitItem.addActionListener(new ActionListener() {	//es wird eine anonyme Klasse (sie hat keinen Namen)
			//definiert und gleichzeitig ein Objekt der Klasse erzeugt. Nachdem nach "new" ein Schnittstellenname
			// ("Actionlistener") verwendet wird, wird in der anonymen Klasse das angegebene Interface implementiert.
	        @Override
	        public void actionPerformed(ActionEvent e) {	//Die anonyme Klasse besitzt diese Methode. Sie wird beim Anklicken ausgef�hrt.
	            System.exit(0);
	        }
	    }); 
	}
	
	private void addSpielMenuItems(JMenu gameMenu) {  
        
	    JMenuItem pauseItem = new JMenuItem("Pause");
	    gameMenu.add(pauseItem); 	    	  
	    pauseItem.addActionListener(new ActionListener() {	
	        @Override
	        public void actionPerformed(ActionEvent e) {	//Die anonyme Klasse besitzt diese Methode. Sie wird beim Anklicken ausgef�hrt.
	            spielfeld.pauseGame();
	        }
	    }); 
	    
	    JMenuItem continueItem = new JMenuItem("Fortsetzen");
	    gameMenu.add(continueItem); 	    	  
	    continueItem.addActionListener(new ActionListener() {	
	        @Override
	        public void actionPerformed(ActionEvent e) {	//Die anonyme Klasse besitzt diese Methode. Sie wird beim Anklicken ausgef�hrt.
	            spielfeld.continueGame();
	        }
	    }); 
	}
	
	private void registerWindowListener() {        
	    addWindowListener(new WindowAdapter() {  
	    	//Hier wird von der Abstrakten Klasse WindowAdapter, einer abstrakten Klasse,
	    	//abgeleitet, die Fensterereignisse empf�ngt.
			//Es m�ssen nur ben�tigte Methoden der Klasse ausprogrammiert werden (sonst bleiben sie leer).
	        @Override
	        public void windowClosing(WindowEvent e) { 	        	
	        	System.exit(0); 
	        }
	        @Override
	        public void windowDeactivated(WindowEvent e) {
	            // hier k�nnen wir unser Spiel pausieren                
	        }
	        @Override
	        public void windowActivated(WindowEvent e) {
	            // hier k�nnen wir unser Spiel wieder fortsetzen
	        }            
	    });        
	}
}
