package softarrecife.vista;

import softarrecife.conexion.MySQLConnection;
import javax.swing.*;
import java.awt.*;
import java.sql.*;
import softarrecife.modelo.Sesion;

public class LoginFrame extends JFrame {

    private JTextField txtUsuario;
    private JPasswordField txtPassword;

    public LoginFrame() {
        setTitle("SoftArrecife POS - Login");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(350, 200);
        setLocationRelativeTo(null);
        setLayout(new GridLayout(4, 1));

        txtUsuario = new JTextField();
        txtPassword = new JPasswordField();
        JButton btnLogin = new JButton("Iniciar sesión");

        add(new JLabel("Usuario:"));
        add(txtUsuario);
        add(new JLabel("Contraseña:"));
        add(txtPassword);
        add(btnLogin);

        btnLogin.addActionListener(e -> validarLogin());
        getRootPane().setDefaultButton(btnLogin);

        setVisible(true);
    }

    private void validarLogin() {
        String usuario = txtUsuario.getText();
        String pass = String.valueOf(txtPassword.getPassword());

        try (Connection conn = MySQLConnection.getConnection()) {
            String query = "SELECT * FROM usuarios WHERE usuario = ? AND contrasena = ?";
            PreparedStatement ps = conn.prepareStatement(query);
            ps.setString(1, usuario);
            ps.setString(2, pass);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                // ✅ Guardar datos en sesión
                Sesion.usuarioId = rs.getInt("id");
                Sesion.nombre = rs.getString("nombre");

                JOptionPane.showMessageDialog(this, "¡Bienvenido, " + Sesion.nombre + "!");
                dispose();
                new MenuPrincipalFrame(Sesion.nombre);

            } else {
                JOptionPane.showMessageDialog(this, "Usuario o contraseña incorrectos");
            }

        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage());
        }
    }
}
