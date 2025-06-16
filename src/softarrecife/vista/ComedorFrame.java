// ComedorFrame.java - Vista principal de mesas con botones de gestión
package softarrecife.vista;

import softarrecife.conexion.MySQLConnection;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.sql.*;

public class ComedorFrame extends JFrame {

    private JPanel panelMesas;

    public ComedorFrame() {
        setTitle("Comedor");
        setSize(800, 600);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout());

        // Panel superior con botones
        JPanel panelBotones = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton btnAgregar = new JButton("➕ Agregar mesa");
        btnAgregar.addActionListener(e -> agregarMesa());
        panelBotones.add(btnAgregar);

        add(panelBotones, BorderLayout.NORTH);

        // Panel de mesas
        panelMesas = new JPanel();
        panelMesas.setLayout(new FlowLayout(FlowLayout.LEFT, 10, 10));

        JScrollPane scroll = new JScrollPane(panelMesas);
        add(scroll, BorderLayout.CENTER);

        cargarMesas();

        setVisible(true);
    }

    public void cargarMesas() {
        panelMesas.removeAll();

        try (Connection conn = MySQLConnection.getConnection()) {
            String sql = "SELECT * FROM mesas WHERE estado = 'ocupada'";
            PreparedStatement ps = conn.prepareStatement(sql);
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

    // Fragmento corregido para JPopupMenu al hacer clic derecho en una mesa
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

    private void eliminarMesa(int id) {
        int confirm = JOptionPane.showConfirmDialog(this, "¿Eliminar esta mesa?", "Confirmar", JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            try (Connection conn = MySQLConnection.getConnection()) {
                String sql = "DELETE FROM mesas WHERE id = ?";
                PreparedStatement ps = conn.prepareStatement(sql);
                ps.setInt(1, id);
                ps.executeUpdate();
                cargarMesas();
            } catch (SQLException e) {
                JOptionPane.showMessageDialog(this, "Error al eliminar mesa: " + e.getMessage());
            }
        }
    }

    private void verificarCierreDesdeContextual(int mesaId) {
        try (Connection conn = MySQLConnection.getConnection()) {
            // Verifica si hay cuenta abierta
            String sqlCuenta = "SELECT id FROM cuentas WHERE mesa_id = ? AND estado = 'abierta'";
            PreparedStatement psCuenta = conn.prepareStatement(sqlCuenta);
            psCuenta.setInt(1, mesaId);
            ResultSet rsCuenta = psCuenta.executeQuery();

            if (rsCuenta.next()) {
                int cuentaId = rsCuenta.getInt("id");

                // Verifica si hay productos en la cuenta
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
                    return;
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
