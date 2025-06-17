package softarrecife.vista;

import softarrecife.conexion.MySQLConnection;

import javax.swing.*;
import java.awt.*;
import java.sql.*;

public class NIPMeseroDialog extends JDialog {

    public NIPMeseroDialog(JFrame parent) {
        super(parent, "Ingresar NIP", true);
        setSize(320, 180);
        setLocationRelativeTo(parent);
        setLayout(new BorderLayout());

        Color fondo = new Color(250, 250, 250);
        Color primario = new Color(30, 144, 255);
        Color texto = new Color(60, 60, 60);

        JPanel panel = new JPanel();
        panel.setBackground(fondo);
        panel.setBorder(BorderFactory.createEmptyBorder(20, 30, 20, 30));
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

        JTextField txtNip = new JTextField();
        txtNip.setPreferredSize(new Dimension(250, 36));
        txtNip.setMaximumSize(new Dimension(250, 36));
        txtNip.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Color.LIGHT_GRAY, 1, true),
                BorderFactory.createEmptyBorder(8, 12, 8, 12)
        ));
        txtNip.setFont(new Font("SansSerif", Font.PLAIN, 14));
        txtNip.setForeground(texto);
        txtNip.setToolTipText("Ingresa tu NIP");
        txtNip.putClientProperty("JTextField.placeholderText", "Ingresa tu NIP");

        JButton btnAceptar = new JButton("Ingresar");
        btnAceptar.setFocusPainted(false);
        btnAceptar.setBackground(primario);
        btnAceptar.setForeground(Color.WHITE);
        btnAceptar.setFont(new Font("SansSerif", Font.BOLD, 14));
        btnAceptar.setAlignmentX(Component.CENTER_ALIGNMENT);
        btnAceptar.setBorder(BorderFactory.createEmptyBorder(8, 16, 8, 16));

        panel.add(Box.createVerticalGlue());
        panel.add(txtNip);
        panel.add(Box.createRigidArea(new Dimension(0, 15)));
        panel.add(btnAceptar);
        panel.add(Box.createVerticalGlue());

        add(panel, BorderLayout.CENTER);

        btnAceptar.addActionListener(e -> {
            String nip = txtNip.getText().trim();

            try (Connection conn = MySQLConnection.getConnection()) {
                String sql = "SELECT id, nombre, tipo FROM usuarios WHERE nip = ?";
                PreparedStatement ps = conn.prepareStatement(sql);
                ps.setString(1, nip);
                ResultSet rs = ps.executeQuery();

                if (rs.next()) {
                    int usuarioId = rs.getInt("id");
                    String nombre = rs.getString("nombre");
                    String tipo = rs.getString("tipo");

                    dispose();

                    javax.swing.Timer timer = new javax.swing.Timer(100, e2 -> {
                        ComedorFrame comedor = new ComedorFrame(usuarioId, nombre, tipo);
                        comedor.setVisible(true);
                        comedor.setAlwaysOnTop(true);
                        comedor.toFront();
                        comedor.requestFocus();
                        comedor.setAlwaysOnTop(false);
                    });
                    timer.setRepeats(false);
                    timer.start();
                } else {
                    JOptionPane.showMessageDialog(this, "NIP incorrecto");
                }

            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(this, "Error al verificar NIP: " + ex.getMessage());
            }
        });

        getContentPane().setBackground(fondo);
        setVisible(true);
    }
}