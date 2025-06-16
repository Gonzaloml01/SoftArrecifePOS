package softarrecife.vista;

import softarrecife.conexion.MySQLConnection;
import softarrecife.modelo.Sesion;

import javax.swing.*;
import java.awt.*;
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
        setLayout(new GridLayout(3, 2, 20, 20));

        JPanel panelBotones = new JPanel(new GridLayout(3, 2, 10, 10));
        add(panelBotones);

        JButton btnTurno = new JButton("🔓 Abrir/Cerrar Turno");
        JButton btnComedor = new JButton("🍽️ Ir al Comedor");
        JButton btnProductos = new JButton("🛒 Gestión de Productos");
        JButton btnReportes = new JButton("📊 Reportes");
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

        btnReportes.addActionListener(e -> new ReportesFrame());

        btnSalir.addActionListener(e -> {
            if (hayTurnoAbierto(Sesion.usuarioId)) {
                JOptionPane.showMessageDialog(this, "No puedes cerrar sesión con un turno abierto. Cierra el turno primero.");
            } else {
                dispose();
                new LoginFrame();
            }
        });

        panelBotones.add(btnTurno);
        panelBotones.add(btnComedor);
        panelBotones.add(btnProductos);
        panelBotones.add(btnReportes);
        panelBotones.add(btnSalir);

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
