package softarrecife.vista;

import javax.swing.*;
import java.awt.*;
import java.sql.*;
import softarrecife.conexion.MySQLConnection;

public class NIPMeseroDialog extends JDialog {

    public NIPMeseroDialog(JFrame parent) {
        super(parent, "Ingresar NIP del Mesero", true);
        setSize(300, 150);
        setLocationRelativeTo(parent);
        setLayout(new BorderLayout());

        JTextField txtNip = new JPasswordField();
        JButton btnAceptar = new JButton("Entrar");

        JPanel panelCentro = new JPanel(new GridLayout(2, 1));
        panelCentro.add(new JLabel("NIP del mesero:"));
        panelCentro.add(txtNip);
        add(panelCentro, BorderLayout.CENTER);
        add(btnAceptar, BorderLayout.SOUTH);

        btnAceptar.addActionListener(e -> {
            String nip = txtNip.getText().trim();

            if (!nip.isEmpty()) {
                try (Connection conn = MySQLConnection.getConnection()) {
                    String sql = "SELECT * FROM usuarios WHERE nip = ?";
                    PreparedStatement ps = conn.prepareStatement(sql);
                    ps.setString(1, nip);
                    ResultSet rs = ps.executeQuery();

                    if (rs.next()) {
                        String tipo = rs.getString("tipo");
                        int id = rs.getInt("id");
                        String nombre = rs.getString("nombre");

                        if (tipo.equals("admin")) {
                            new ComedorFrame(true, id, nombre); // admin ve todo
                        } else {
                            new ComedorFrame(false, id, nombre); // mesero solo sus mesas
                        }

                        dispose();
                    } else {
                        JOptionPane.showMessageDialog(this, "NIP incorrecto.");
                    }

                } catch (SQLException ex) {
                    JOptionPane.showMessageDialog(this, "Error al validar NIP: " + ex.getMessage());
                }
            }
        });

        setVisible(true);
    }
}
