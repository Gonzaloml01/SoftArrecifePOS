package softarrecife;

import com.formdev.flatlaf.FlatLightLaf;
import javax.swing.UIManager;
import javax.swing.JFrame;
import javax.swing.JDialog;
import softarrecife.vista.LoginFrame;

public class Main {
    public static void main(String[] args) {
        try {
            FlatLightLaf.setup();
            UIManager.setLookAndFeel(new FlatLightLaf());

            // Esto activa el decorador personalizado de FlatLaf
            JFrame.setDefaultLookAndFeelDecorated(true);
            JDialog.setDefaultLookAndFeelDecorated(true);

        } catch (Exception e) {
            System.err.println("No se pudo aplicar FlatLaf.");
        }

        new LoginFrame();
    }
}
