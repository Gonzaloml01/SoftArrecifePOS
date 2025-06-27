package softarrecife.vista;

import softarrecife.conexion.MySQLConnection;
import softarrecife.modelo.Sesion;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.sql.*;

public class LoginFrame extends JFrame {

    private JTextField txtUsuario;
    private JPasswordField txtPassword;

    public LoginFrame() {
        setTitle("Summa POS - Iniciar Sesión");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(400, 300);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        Color fondo = new Color(245, 245, 245);
        Color grisOscuro = new Color(50, 50, 50);
        Color azul = new Color(30, 144, 255); // Azul para el botón
        Font fuente = new Font("SansSerif", Font.PLAIN, 14);

        // Contenedor principal
        JPanel contenedor = new JPanel();
        contenedor.setBackground(fondo);
        contenedor.setBorder(new EmptyBorder(20, 40, 30, 40));
        contenedor.setLayout(new BoxLayout(contenedor, BoxLayout.Y_AXIS));

        JLabel titulo = new JLabel("Summa POS", SwingConstants.CENTER);
        titulo.setFont(new Font("SansSerif", Font.BOLD, 20));
        titulo.setAlignmentX(Component.CENTER_ALIGNMENT);
        titulo.setForeground(grisOscuro);

        txtUsuario = new JTextField("Usuario");
        txtPassword = new JPasswordField("Contraseña");

        estilizarCampo(txtUsuario);
        estilizarCampo(txtPassword);

        agregarPlaceholders(txtUsuario, "Usuario");
        agregarPlaceholders(txtPassword, "Contraseña");

        JButton btnLogin = new JButton("Iniciar sesión");
        btnLogin.setAlignmentX(Component.CENTER_ALIGNMENT);
        btnLogin.setFont(fuente);
        btnLogin.setBackground(azul); // Azul moderno
        btnLogin.setForeground(Color.WHITE);
        btnLogin.setFocusPainted(false);
        btnLogin.setBorder(new RoundedBorder(10));
        btnLogin.setMaximumSize(new Dimension(200, 40));

        contenedor.add(Box.createVerticalStrut(10));
        contenedor.add(titulo);
        contenedor.add(Box.createVerticalStrut(25));
        contenedor.add(txtUsuario);
        contenedor.add(Box.createVerticalStrut(15));
        contenedor.add(txtPassword);
        contenedor.add(Box.createVerticalStrut(25));
        contenedor.add(btnLogin);
        contenedor.add(Box.createVerticalStrut(10)); // Espacio debajo del botón

        add(contenedor, BorderLayout.CENTER);

        btnLogin.addActionListener(e -> validarLogin());
        getRootPane().setDefaultButton(btnLogin);

        setVisible(true);
    }

    private void estilizarCampo(JTextField campo) {
        campo.setMaximumSize(new Dimension(250, 40));
        campo.setFont(new Font("SansSerif", Font.PLAIN, 14));
        campo.setBorder(new RoundedBorder(10));
        campo.setHorizontalAlignment(SwingConstants.LEFT);
        campo.setMargin(new Insets(5, 10, 5, 10));
    }

    private void agregarPlaceholders(JTextField campo, String placeholder) {
        Color grisClaro = new Color(160, 160, 160);
        campo.setForeground(grisClaro);

        campo.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                if (campo.getText().equals(placeholder)) {
                    campo.setText("");
                    campo.setForeground(Color.BLACK);
                }
            }

            @Override
            public void focusLost(FocusEvent e) {
                if (campo.getText().isEmpty()) {
                    campo.setText(placeholder);
                    campo.setForeground(grisClaro);
                }
            }
        });
    }

  private void validarLogin() {
    String usuario = txtUsuario.getText();
    String pass = String.valueOf(txtPassword.getPassword());

    if (usuario.equals("Usuario") || pass.equals("Contraseña")) {
        JOptionPane.showMessageDialog(this, "Por favor ingresa tus credenciales.");
        return;
    }

    try (Connection conn = MySQLConnection.getConnection()) {
        String query = "SELECT * FROM usuarios WHERE BINARY usuario = ? AND BINARY contrasena = ?";
        PreparedStatement ps = conn.prepareStatement(query);
        ps.setString(1, usuario);
        ps.setString(2, pass);
        ResultSet rs = ps.executeQuery();

        if (rs.next()) {
            Sesion.usuarioId = rs.getInt("id");
            Sesion.nombre = rs.getString("nombre");
            Sesion.tipo = rs.getString("tipo");

            JOptionPane.showMessageDialog(this, "¡Bienvenido, " + Sesion.nombre + "!");
            dispose();
            new MenuPrincipalFrame(Sesion.usuarioId, Sesion.nombre, Sesion.tipo);
        } else {
            JOptionPane.showMessageDialog(this, "Usuario o contraseña incorrectos");
        }

    } catch (SQLException ex) {
        JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage());
    }
}

    // Borde redondeado personalizado
    class RoundedBorder extends LineBorder {
        private int radius;

        RoundedBorder(int radius) {
            super(new Color(200, 200, 200), 1, true);
            this.radius = radius;
        }

        @Override
        public Insets getBorderInsets(Component c) {
            return new Insets(this.radius + 2, this.radius + 5, this.radius + 2, this.radius + 5);
        }
    }
}
