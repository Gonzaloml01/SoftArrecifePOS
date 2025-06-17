package softarrecife.vista;

import softarrecife.conexion.MySQLConnection;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.sql.*;

public class ComedorFrame extends JFrame {

    private JPanel panelMesas;
    private int usuarioId;
    private String nombreMesero;
    private String tipoUsuario;

    public ComedorFrame(int usuarioId, String nombreMesero, String tipoUsuario) {
        this.usuarioId = usuarioId;
        this.nombreMesero = nombreMesero;
        this.tipoUsuario = tipoUsuario;

        setTitle("Comedor - Mesero: " + nombreMesero);
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

            String sqlDetalle = """
                SELECT p.nombre, dc.cantidad
                FROM detalle_cuenta dc
                JOIN productos p ON dc.producto_id = p.id
                WHERE cuenta_id = ?
            """;
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
                // Hay productos: no se permite cerrar aún
                JOptionPane.showMessageDialog(this,
                        "Esta mesa tiene productos activos.\nDebes cerrar y cobrar la cuenta desde el botón principal.\n\nProductos:\n"
                        + productosList,
                        "Cuenta con productos", JOptionPane.WARNING_MESSAGE);
            } else {
                // ✅ No hay productos: cerrar y eliminar mesa visualmente
                String cerrarCuentaSQL = "UPDATE cuentas SET estado = 'cerrada', fecha_cierre = NOW() WHERE mesa_id = ? AND estado = 'abierta'";
                PreparedStatement psCerrar = conn.prepareStatement(cerrarCuentaSQL);
                psCerrar.setInt(1, mesaId);
                psCerrar.executeUpdate();

                String liberarMesa = "UPDATE mesas SET estado = 'libre' WHERE id = ?";
                PreparedStatement psMesa = conn.prepareStatement(liberarMesa);
                psMesa.setInt(1, mesaId);
                psMesa.executeUpdate();

                JOptionPane.showMessageDialog(this,
                        "La mesa estaba vacía. Eliminada exitosamente.",
                        "Mesa eliminada", JOptionPane.INFORMATION_MESSAGE);

                cargarMesas();
            }

        } else {
            // Ya no hay cuenta activa (ya fue cerrada y mesa liberada)
            JOptionPane.showMessageDialog(this,
                    "La cuenta ya fue cerrada. Esta mesa será removida automáticamente.",
                    "Cuenta cerrada", JOptionPane.INFORMATION_MESSAGE);
            cargarMesas(); // para limpiar visualmente
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
