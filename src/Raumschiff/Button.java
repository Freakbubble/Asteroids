package Raumschiff;
import javax.swing.*;
import java.awt.*;

public class Button extends JButton {
    Font btnFont = new Font("Arial", Font.BOLD, 24); // Arial;

    public Button(String text) {
        super(text);
        setFont(btnFont);
        setBackground(new Color(0, 68, 68));
        setForeground(new Color(255, 255, 255, 179));
    }

    public Button(String text, Color color) {
        super(text);
        setFont(btnFont);
        setBackground(color);
    }

    //Button ohne Text aber mit Bild
    public Button(ImageIcon icon, int width, int height) {
        super();

        Image image = icon.getImage();
        Image scaledImage = image.getScaledInstance(
                width,
                height,
                Image.SCALE_SMOOTH
        );

        setIcon(new ImageIcon(scaledImage));

        setText("");
        setFocusPainted(false);
        setContentAreaFilled(false);
        setBorderPainted(false);

        setHorizontalAlignment(SwingConstants.CENTER);
        setVerticalAlignment(SwingConstants.CENTER);
    }
    }












