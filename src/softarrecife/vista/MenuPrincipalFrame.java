package softarrecife.vista;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.sql.*;
import softarrecife.conexion.MySQLConnection;

public class MenuPrincipalFrame extends JFrame {

    private int usuarioId;
    private String nombreUsuario;
    private String tipoUsuario;

    public MenuPrincipalFrame(int usuarioId, String nombreUsuario, String tipoUsuario) {
        this.usuarioId = usuarioId;
        this.nombreUsuario = nombreUsuario;
        this.tipoUsuario = tipoUsuario;

        setTitle("Summa POS - Bienvenido " + nombreUsuario);
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        setExtendedState(JFrame.MAXIMIZED_BOTH);

        setLayout(new BorderLayout());

        JLabel titulo = new JLabel("Menú Principal", SwingConstants.CENTER);
        titulo.setFont(new Font("SansSerif", Font.BOLD, 22));
        titulo.setForeground(new Color(30, 30, 30));
        titulo.setBorder(BorderFactory.createEmptyBorder(30, 0, 10, 0));
        add(titulo, BorderLayout.NORTH);

        // Panel con imagen de fondo
        PanelConFondo panelFondo = new PanelConFondo("/softarrecife/assets/fondo.png");
        panelFondo.setLayout(new GridBagLayout());

        // Panel de botones centrado
        JPanel panelBotones = new JPanel(new GridLayout(0, 2, 30, 30));
        panelBotones.setOpaque(true);
        panelBotones.setBackground(new Color(255, 255, 255, 210)); // blanco semi-transparente
        panelBotones.setBorder(BorderFactory.createEmptyBorder(30, 30, 30, 30));

        // Botones
        JButton btnTurno = crearBoton("🔓 Turno");
        JButton btnComedor = crearBoton("🍽️ Comedor");
        JButton btnProductos = crearBoton("🛒 Productos");
        JButton btnReportes = crearBoton("📊 Reportes");
        JButton btnSalir = crearBoton("🚪 Cerrar Sesión");

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
            dispose();
            new LoginFrame();
        });

        panelBotones.add(btnTurno);
        panelBotones.add(btnComedor);
        if (tipoUsuario.equals("admin")) {
            panelBotones.add(btnProductos);
            panelBotones.add(btnReportes);
        }
        panelBotones.add(btnSalir);

        panelFondo.add(panelBotones); // centrado por GridBagLayout

        add(panelFondo, BorderLayout.CENTER);

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                dispose();
                new LoginFrame();
            }
        });

        setVisible(true);
    }

    private JButton crearBoton(String texto) {
        JButton boton = new JButton(texto);
        boton.setFont(new Font("SansSerif", Font.BOLD, 16));
        boton.setBackground(new Color(240, 240, 240));
        boton.setFocusPainted(false);
        boton.setPreferredSize(new Dimension(200, 60));
        boton.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(200, 200, 200)),
                BorderFactory.createEmptyBorder(10, 20, 10, 20)
        ));
        return boton;
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

    class PanelConFondo extends JPanel {
        private Image fondo;

        public PanelConFondo(String ruta) {
            try {
                fondo = new ImageIcon(getClass().getResource(ruta)).getImage();
            } catch (Exception e) {
                System.err.println("No se pudo cargar la imagen de fondo");
            }
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (fondo != null) {
                g.drawImage(fondo, 0, 0, getWidth(), getHeight(), this);
            }
        }
    }
}
