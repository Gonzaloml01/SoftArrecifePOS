package softarrecife.vista;

import softarrecife.conexion.MySQLConnection;

import javax.swing.*;
import java.awt.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class RegistrarMeserosFrame extends JFrame {

    public RegistrarMeserosFrame() {
        setTitle("Registrar nuevo mesero");
        setSize(400, 250);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        JPanel panel = new JPanel(new GridLayout(4, 2, 10, 10));

        JLabel lblNombre = new JLabel("Nombre del mesero:");
        JTextField txtNombre = new JTextField();

        JLabel lblUsuario = new JLabel("Usuario (opcional):");
        JTextField txtUsuario = new JTextField();

        JLabel lblNip = new JLabel("NIP de acceso:");
        JTextField txtNip = new JTextField();

        JButton btnGuardar = new JButton("Registrar");

        btnGuardar.addActionListener(e -> {
            String nombre = txtNombre.getText().trim();
            String nip = txtNip.getText().trim();
            String usuario = txtUsuario.getText().trim();

            if (nombre.isEmpty() || nip.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Nombre y NIP son obligatorios.");
                return;
            }

            try (Connection conn = MySQLConnection.getConnection()) {
                String sql = "INSERT INTO usuarios (nombre, nip, tipo, usuario) VALUES (?, ?, 'mesero', ?)";
                PreparedStatement ps = conn.prepareStatement(sql);
                ps.setString(1, nombre);
                ps.setString(2, nip);
                ps.setString(3, usuario.isEmpty() ? null : usuario);
                ps.executeUpdate();

                JOptionPane.showMessageDialog(this, "Mesero registrado con éxito.");
                dispose();

            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(this, "Error al registrar mesero: " + ex.getMessage());
            }
        });

        panel.add(lblNombre);
        panel.add(txtNombre);
        panel.add(lblUsuario);
        panel.add(txtUsuario);
        panel.add(lblNip);
        panel.add(txtNip);
        panel.add(new JLabel());
        panel.add(btnGuardar);

        add(panel);
        setVisible(true);
    }
} 
