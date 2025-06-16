package softarrecife.vista;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import softarrecife.conexion.MySQLConnection;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class MenuPrincipalFrame extends JFrame {

    private int usuarioId;
    private String nombreUsuario;
    private String tipoUsuario;

    public MenuPrincipalFrame(int usuarioId, String nombreUsuario, String tipoUsuario) {
        this.usuarioId = usuarioId;
        this.nombreUsuario = nombreUsuario;
        this.tipoUsuario = tipoUsuario;

        setTitle("Menú Principal - Bienvenido " + nombreUsuario);
        setSize(800, 600);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);

        JPanel panelBotones = new JPanel(new GridLayout(3, 2, 20, 20));

        JButton btnTurno = new JButton("🔓 Abrir/Cerrar Turno");
        JButton btnComedor = new JButton("🍽️ Ir al Comedor");
        JButton btnProductos = new JButton("🛒 Gestión de Productos");
        JButton btnReportes = new JButton("📊 Reportes");
        JButton btnSalir = new JButton("🚪 Cerrar Sesión");

        btnTurno.addActionListener(e -> new TurnoFrame(usuarioId));

        btnComedor.addActionListener(e -> {
            if (!hayTurnoAbierto(usuarioId)) {
                JOptionPane.showMessageDialog(this, "Debes abrir un turno antes de usar el comedor.");
            } else {
                new NIPMeseroDialog(this);
            }
        });

        btnProductos.addActionListener(e -> new GestionProductosFrame());
        btnReportes.addActionListener(e -> new ReportesFrame());

        btnSalir.addActionListener(e -> {
            if (hayTurnoAbierto(usuarioId)) {
                JOptionPane.showMessageDialog(this, "Debes cerrar el turno antes de cerrar sesión.");
            } else {
                dispose();
                new LoginFrame();
            }
        });

        // Agregar botones según tipo de usuario
        panelBotones.add(btnTurno);
        panelBotones.add(btnComedor);

        if (tipoUsuario.equals("admin")) {
            panelBotones.add(btnProductos);
            panelBotones.add(btnReportes);
        }

        panelBotones.add(btnSalir);
        add(panelBotones);

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {  
                    dispose();
                    new LoginFrame();
            }
        });

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
