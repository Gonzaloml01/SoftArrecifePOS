package softarrecife.vista;

import javax.swing.*;
import java.awt.*;
import softarrecife.conexion.MySQLConnection;
import softarrecife.modelo.Sesion;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;



public class MenuPrincipalFrame extends JFrame {

    public MenuPrincipalFrame(String nombreUsuario) {
        setTitle("Menú Principal - Bienvenido " + nombreUsuario);
        setSize(800, 600);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        setLayout(new GridLayout(2, 2, 20, 20));

        JButton btnTurno = new JButton("🔓 Abrir/Cerrar Turno");
        JButton btnComedor = new JButton("🍽️ Ir al Comedor");
        JButton btnProductos = new JButton("🛒 Gestión de Productos");
        JButton btnSalir = new JButton("🚪 Cerrar Sesión");

        btnTurno.addActionListener(e -> new TurnoFrame(Sesion.usuarioId));
        btnComedor.addActionListener(e -> {
            if (!hayTurnoAbierto(Sesion.usuarioId)) {
                JOptionPane.showMessageDialog(this, "Debes abrir un turno antes de usar el comedor.");
            } else {
                new ComedorFrame();
            }
        });

        btnProductos.addActionListener(e -> new GestionProductosFrame());
        btnSalir.addActionListener(e -> {
            dispose();
            new LoginFrame();
        });

        add(btnTurno);
        add(btnComedor);
        add(btnProductos);
        add(btnSalir);

        setVisible(true);
    }

    private boolean hayTurnoAbierto(int usuarioId) {
        try (Connection conn = MySQLConnection.getConnection()) {
            String sql = "SELECT COUNT(*) FROM turnos WHERE usuario_id = ? AND estado = 'abierto'";
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, usuarioId);
            ResultSet rs = ps.executeQuery();
            rs.next();
            return rs.getInt(1) > 0;
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error al verificar turno: " + e.getMessage());
            return false;
        }
    }

}
