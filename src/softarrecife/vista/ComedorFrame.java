package softarrecife.vista;

import softarrecife.conexion.MySQLConnection;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.sql.*;

public class ComedorFrame extends JFrame {

    private JPanel panelMesas;
    private boolean esAdmin;
    private int usuarioId;
    private String nombreMesero;

    public ComedorFrame(boolean esAdmin, int usuarioId, String nombreMesero) {
        this.esAdmin = esAdmin;
        this.usuarioId = usuarioId;
        this.nombreMesero = nombreMesero;

        setTitle("Comedor - " + (esAdmin ? "Administrador" : "Mesero: " + nombreMesero));
        setSize(800, 600);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout());

        JPanel panelBotones = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton btnAgregar = new JButton("➕ Agregar mesa");
        btnAgregar.addActionListener(e -> agregarMesa());
        panelBotones.add(btnAgregar);
        add(panelBotones, BorderLayout.NORTH);

        panelMesas = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        JScrollPane scroll = new JScrollPane(panelMesas);
        add(scroll, BorderLayout.CENTER);

        cargarMesas();
        setVisible(true);
    }

    public void cargarMesas() {
        panelMesas.removeAll();

        try (Connection conn = MySQLConnection.getConnection()) {
            String sql = "SELECT m.* FROM mesas m "
                       + "JOIN cuentas c ON m.id = c.mesa_id "
                       + "WHERE c.estado = 'abierta'";

            if (!esAdmin) {
                sql += " AND c.usuario_id = ?";
            }

            PreparedStatement ps = conn.prepareStatement(sql);
            if (!esAdmin) {
                ps.setInt(1, usuarioId);
            }

            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                int id = rs.getInt("id");
                String nombre = rs.getString("nombre");

                JButton mesaBtn = new JButton(nombre);
                mesaBtn.setPreferredSize(new Dimension(120, 80));

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
                String sql = "INSERT INTO mesas (nombre, estado) VALUES (?, 'ocupada')";
                PreparedStatement ps = conn.prepareStatement(sql);
                ps.setString(1, nombre.trim());
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

                String sqlDetalle = "SELECT p.nombre, dc.cantidad FROM detalle_cuenta dc JOIN productos p ON dc.producto_id = p.id WHERE cuenta_id = ?";
                PreparedStatement psDetalle = conn.prepareStatement(sqlDetalle);
                psDetalle.setInt(1, cuentaId);
                ResultSet rsDetalle = psDetalle.executeQuery();

                StringBuilder productosList = new StringBuilder();
                while (rsDetalle.next()) {
                    productosList.append("- ")
                            .append(rsDetalle.getString("nombre"))
                            .append(" x")
                            .append(rsDetalle.getInt("cantidad"))
                            .append("\n");
                }

                if (productosList.length() > 0) {
                    JOptionPane.showMessageDialog(this,
                            "Esta mesa tiene productos activos.\n"
                            + "Debes cerrar y cobrar la cuenta desde el botón principal.\n\nProductos:\n"
                            + productosList.toString(),
                            "Cuenta con productos", JOptionPane.WARNING_MESSAGE);
                } else {
                    JOptionPane.showMessageDialog(this,
                            "La cuenta está vacía, puedes cerrarla desde el botón principal.",
                            "Cuenta vacía", JOptionPane.INFORMATION_MESSAGE);
                }

            } else {
                JOptionPane.showMessageDialog(this,
                        "No hay cuenta activa en esta mesa.",
                        "Sin cuenta", JOptionPane.INFORMATION_MESSAGE);
            }

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
