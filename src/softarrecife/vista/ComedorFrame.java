package softarrecife.vista;

import softarrecife.conexion.MySQLConnection;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.sql.*;
import softarrecife.utils.WrapLayout;

public class ComedorFrame extends JFrame {

    private JPanel panelMesas;
    private int usuarioId;
    private String nombreMesero;
    private String tipoUsuario;

    public ComedorFrame(int usuarioId, String nombreMesero, String tipoUsuario) {
        this.usuarioId = usuarioId;
        this.nombreMesero = nombreMesero;
        this.tipoUsuario = tipoUsuario;

        setTitle("Summa POS - Comedor de " + nombreMesero);
        setSize(900, 600);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout());

        Color fondo = new Color(245, 245, 245);
        Color primario = new Color(30, 144, 255);
        Font fuente = new Font("SansSerif", Font.PLAIN, 14);

        JPanel panelBotones = new JPanel(new FlowLayout(FlowLayout.LEFT));
        panelBotones.setBackground(fondo);

        JButton btnAgregar = new JButton("➕ Agregar mesa");
        btnAgregar.setFocusPainted(false);
        btnAgregar.setBackground(primario);
        btnAgregar.setForeground(Color.WHITE);
        btnAgregar.setFont(new Font("SansSerif", Font.BOLD, 13));
        btnAgregar.setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 15));

        panelBotones.add(btnAgregar);
        add(panelBotones, BorderLayout.NORTH);

        btnAgregar.addActionListener(e -> agregarMesa());

        panelMesas = new JPanel(new WrapLayout(FlowLayout.LEFT, 12, 12));
        panelMesas.setBackground(fondo);

        JScrollPane scroll = new JScrollPane(panelMesas);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        add(scroll, BorderLayout.CENTER);

        cargarMesas();

        setVisible(true);

        // Forzar al frente
        setAlwaysOnTop(true);
        toFront();
        requestFocus();
        setAlwaysOnTop(false);
    }

    public int getUsuarioId() {
        return usuarioId;
    }

    public void cargarMesas() {
        panelMesas.removeAll();

        try (Connection conn = MySQLConnection.getConnection()) {
            String sql;
            PreparedStatement ps;

            if (tipoUsuario.equals("admin")) {
                sql = "SELECT * FROM mesas WHERE estado = 'ocupada'";
                ps = conn.prepareStatement(sql);
            } else {
                sql = "SELECT * FROM mesas WHERE estado = 'ocupada' AND usuario_id = ?";
                ps = conn.prepareStatement(sql);
                ps.setInt(1, usuarioId);
            }

            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                int id = rs.getInt("id");
                String nombre = rs.getString("nombre");

                JButton mesaBtn = new JButton(nombre);
                mesaBtn.setPreferredSize(new Dimension(130, 80));
                mesaBtn.setBackground(Color.WHITE);
                mesaBtn.setFont(new Font("SansSerif", Font.BOLD, 14));
                mesaBtn.setFocusPainted(false);
                mesaBtn.setBorder(BorderFactory.createLineBorder(new Color(200, 200, 200)));

                mesaBtn.addMouseListener(new MouseAdapter() {
                    @Override
                    public void mousePressed(MouseEvent e) {
                        if (SwingUtilities.isRightMouseButton(e)) {
                            JPopupMenu menu = crearMenuContextual(id, nombre);
                            menu.show(e.getComponent(), e.getX(), e.getY());
                        } else if (SwingUtilities.isLeftMouseButton(e)) {
                            SwingUtilities.invokeLater(() -> new CuentaFrame(id, nombre, ComedorFrame.this));
                        }
                    }
                });

                panelMesas.add(mesaBtn);
            }

        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error al cargar mesas: " + e.getMessage());
        }

        panelMesas.revalidate();
        panelMesas.repaint();
    }

    private void agregarMesa() {
        String nombre = JOptionPane.showInputDialog(this, "Nombre de la nueva mesa:");
        if (nombre != null && !nombre.trim().isEmpty()) {
            try (Connection conn = MySQLConnection.getConnection()) {
                String sql = "INSERT INTO mesas (nombre, estado, usuario_id) VALUES (?, 'ocupada', ?)";
                PreparedStatement ps = conn.prepareStatement(sql);
                ps.setString(1, nombre.trim());
                ps.setInt(2, usuarioId);
                ps.executeUpdate();
                cargarMesas();
            } catch (SQLException e) {
                JOptionPane.showMessageDialog(this, "Error al agregar mesa: " + e.getMessage());
            }
        }
    }

    private JPopupMenu crearMenuContextual(int id, String nombre) {
        JPopupMenu menu = new JPopupMenu();

        JMenuItem itemCerrarCuenta = new JMenuItem("Cerrar cuenta");
        itemCerrarCuenta.addActionListener(e -> verificarCierreDesdeContextual(id));
        menu.add(itemCerrarCuenta);

        JMenuItem itemRenombrar = new JMenuItem("Renombrar mesa");
        itemRenombrar.addActionListener(e -> renombrarMesa(id, nombre));
        menu.add(itemRenombrar);

        return menu;
    }

    private void verificarCierreDesdeContextual(int mesaId) {
        try (Connection conn = MySQLConnection.getConnection()) {
            String sqlCuenta = "SELECT id FROM cuentas WHERE mesa_id = ? AND estado = 'abierta'";
            PreparedStatement psCuenta = conn.prepareStatement(sqlCuenta);
            psCuenta.setInt(1, mesaId);
            ResultSet rsCuenta = psCuenta.executeQuery();

            if (rsCuenta.next()) {
                int cuentaId = rsCuenta.getInt("id");

                String sqlDetalle = "SELECT COUNT(*) FROM detalle_cuenta WHERE cuenta_id = ?";
                PreparedStatement psDetalle = conn.prepareStatement(sqlDetalle);
                psDetalle.setInt(1, cuentaId);
                ResultSet rsDetalle = psDetalle.executeQuery();
                rsDetalle.next();
                int totalProductos = rsDetalle.getInt(1);

                if (totalProductos > 0) {
                    JOptionPane.showMessageDialog(this,
                            "Esta mesa tiene productos activos.\nDebes cerrar y cobrar la cuenta desde el botón principal.",
                            "Cuenta con productos", JOptionPane.WARNING_MESSAGE);
                    return;
                }

                PreparedStatement psEliminarCuenta = conn.prepareStatement("DELETE FROM cuentas WHERE id = ?");
                psEliminarCuenta.setInt(1, cuentaId);
                psEliminarCuenta.executeUpdate();

                PreparedStatement psEliminarMesa = conn.prepareStatement("DELETE FROM mesas WHERE id = ?");
                psEliminarMesa.setInt(1, mesaId);
                psEliminarMesa.executeUpdate();

                JOptionPane.showMessageDialog(this,
                        "La cuenta estaba vacía. Mesa eliminada exitosamente.",
                        "Mesa eliminada", JOptionPane.INFORMATION_MESSAGE);

            } else {
                PreparedStatement psEliminarMesa = conn.prepareStatement("DELETE FROM mesas WHERE id = ?");
                psEliminarMesa.setInt(1, mesaId);
                psEliminarMesa.executeUpdate();

                JOptionPane.showMessageDialog(this,
                        "La cuenta ya estaba cerrada. La mesa fue eliminada.",
                        "Mesa eliminada", JOptionPane.INFORMATION_MESSAGE);
            }

            cargarMesas();

        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this,
                    "Error al verificar cuenta: " + e.getMessage(),
                    "Error de conexión", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void renombrarMesa(int mesaId, String nombreActual) {
        String nuevoNombre = JOptionPane.showInputDialog(this, "Nuevo nombre para la mesa:", nombreActual);
        if (nuevoNombre != null && !nuevoNombre.trim().isEmpty()) {
            try (Connection conn = MySQLConnection.getConnection()) {
                String sql = "UPDATE mesas SET nombre = ? WHERE id = ?";
                PreparedStatement ps = conn.prepareStatement(sql);
                ps.setString(1, nuevoNombre);
                ps.setInt(2, mesaId);
                ps.executeUpdate();
                cargarMesas();
            } catch (SQLException e) {
                JOptionPane.showMessageDialog(this, "Error al renombrar mesa: " + e.getMessage());
            }
        }
    }
}
