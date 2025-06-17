package softarrecife.vista;

import softarrecife.conexion.MySQLConnection;

import javax.swing.*;
import java.awt.*;
import java.sql.*;

public class NIPMeseroDialog extends JDialog {

    public NIPMeseroDialog(JFrame parent) {
        super(parent, "Ingresar NIP", true);
        setSize(300, 150);
        setLocationRelativeTo(parent);

        JTextField txtNip = new JPasswordField();
        JButton btnAceptar = new JButton("Aceptar");

        setLayout(new GridLayout(3, 1));
        add(new JLabel("Ingrese su NIP:"));
        add(txtNip);
        add(btnAceptar);

        btnAceptar.addActionListener(e -> {
            String nip = txtNip.getText();

            try (Connection conn = MySQLConnection.getConnection()) {
                String sql = "SELECT id, nombre, tipo FROM usuarios WHERE nip = ?";
                PreparedStatement ps = conn.prepareStatement(sql);
                ps.setString(1, nip);
                ResultSet rs = ps.executeQuery();

                if (rs.next()) {
                    int usuarioId = rs.getInt("id");
                    String nombre = rs.getString("nombre");
                    String tipo = rs.getString("tipo");

                    ComedorFrame comedor = new ComedorFrame(usuarioId, nombre, tipo);
                    comedor.setVisible(true);
                    comedor.toFront();

                    dispose();
                } else {
                    JOptionPane.showMessageDialog(this, "NIP incorrecto");
                }

            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(this, "Error al verificar NIP: " + ex.getMessage());
            }
        });

        setVisible(true);
    }
}
