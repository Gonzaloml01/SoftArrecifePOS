// CuentaFrame.java - Corregido para cerrar cuenta y actualizar correctamente la base de datos sin eliminarla
package softarrecife.vista;

import softarrecife.conexion.MySQLConnection;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;
import java.time.LocalDateTime;

public class CuentaFrame extends JFrame {

    private int mesaId;
    private String mesaNombre;
    private int cuentaId;
    private ComedorFrame comedorFrame;

    private JTable tabla;
    private JLabel lblTotal;

    public CuentaFrame(int mesaId, String mesaNombre, ComedorFrame comedorFrame) {
        this.mesaId = mesaId;
        this.mesaNombre = mesaNombre;
        this.comedorFrame = comedorFrame;

        setTitle("Cuenta - " + mesaNombre + " (Atiende: " + obtenerNombreMesero() + ")");
        setSize(600, 500);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        crearCuentaSiNoExiste();
        construirUI();
        cargarDetalleCuenta();
        setVisible(true);
    }

    private void construirUI() {
        JPanel panelTop = new JPanel(new FlowLayout());

        JButton btnAgregar = new JButton("Agregar producto");
        btnAgregar.addActionListener(e -> {
            SelectorProductoDialog selector = new SelectorProductoDialog(this, cuentaId);
            selector.setModal(true);
            selector.setVisible(true);
            cargarDetalleCuenta();
        });

        panelTop.add(btnAgregar);
        add(panelTop, BorderLayout.NORTH);

        tabla = new JTable();
        JScrollPane scroll = new JScrollPane(tabla);
        add(scroll, BorderLayout.CENTER);

        lblTotal = new JLabel("Total: $0.0");
        lblTotal.setFont(new Font("Arial", Font.BOLD, 18));
        add(lblTotal, BorderLayout.SOUTH);

        JButton btnCobrar = new JButton("💳 Imprimir ticket y cerrar cuenta");
        btnCobrar.addActionListener(e -> cerrarCuentaEImprimir());
        add(btnCobrar, BorderLayout.WEST);
    }

    private void crearCuentaSiNoExiste() {
        try (Connection conn = MySQLConnection.getConnection()) {
            String check = "SELECT id FROM cuentas WHERE mesa_id = ? AND estado = 'abierta'";
            PreparedStatement ps = conn.prepareStatement(check);
            ps.setInt(1, mesaId);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                cuentaId = rs.getInt("id");
            } else {
                String insert = "INSERT INTO cuentas (mesa_id, estado, total, fecha) VALUES (?, 'abierta', 0, NOW())";
                ps = conn.prepareStatement(insert, Statement.RETURN_GENERATED_KEYS);
                ps.setInt(1, mesaId);
                ps.executeUpdate();
                rs = ps.getGeneratedKeys();
                if (rs.next()) {
                    cuentaId = rs.getInt(1);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private String obtenerNombreMesero() {
        try (Connection conn = MySQLConnection.getConnection()) {
            String sql = """
            SELECT u.nombre FROM cuentas c
            JOIN usuarios u ON c.usuario_id = u.id
            WHERE c.id = ?
        """;
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, cuentaId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getString("nombre");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return "Desconocido";
    }

    public void cargarDetalleCuenta() {
        DefaultTableModel model = new DefaultTableModel();
        model.addColumn("Producto");
        model.addColumn("Cantidad");
        model.addColumn("Subtotal");
        double total = 0;

        try (Connection conn = MySQLConnection.getConnection()) {
            String sql = """
                SELECT p.nombre, dc.cantidad, dc.subtotal
                FROM detalle_cuenta dc
                JOIN productos p ON dc.producto_id = p.id
                WHERE cuenta_id = ?
            """;
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, cuentaId);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                model.addRow(new Object[]{
                    rs.getString("nombre"),
                    rs.getInt("cantidad"),
                    rs.getDouble("subtotal")
                });
                total += rs.getDouble("subtotal");
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        tabla.setModel(model);
        lblTotal.setText("Total: $" + String.format("%.2f", total));
    }

    private void cerrarCuentaEImprimir() {
        String[] opciones = {"Efectivo", "Tarjeta", "Transferencia"};
        String metodo = (String) JOptionPane.showInputDialog(this, "Método de pago:",
                "Cobro", JOptionPane.QUESTION_MESSAGE, null, opciones, opciones[0]);

        if (metodo == null) {
            return;
        }

        double total = calcularTotalCuenta();
        double montoPagado = total;
        double cambio = 0;

        if (metodo.equals("Efectivo")) {
            String pagadoStr = JOptionPane.showInputDialog(this, "¿Con cuánto pagó el cliente?");
            if (pagadoStr == null) {
                return;
            }
            try {
                montoPagado = Double.parseDouble(pagadoStr);
                cambio = montoPagado - total;
            } catch (NumberFormatException e) {
                JOptionPane.showMessageDialog(this, "Monto inválido.");
                return;
            }
        }

        // Preguntar propina
        String[] tipoPropina = {"% Porcentaje", "$ Monto fijo", "Sin propina"};
        int opcionPropina = JOptionPane.showOptionDialog(this, "¿El cliente dejó propina?",
                "Propina", JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE, null,
                tipoPropina, tipoPropina[0]);

        double propina = 0;
        if (opcionPropina == 0) {
            String propinaStr = JOptionPane.showInputDialog(this, "¿Qué porcentaje dejó?");
            if (propinaStr != null) {
                try {
                    double porcentaje = Double.parseDouble(propinaStr);
                    propina = total * (porcentaje / 100);
                } catch (NumberFormatException e) {
                    JOptionPane.showMessageDialog(this, "Porcentaje inválido.");
                }
            }
        } else if (opcionPropina == 1) {
            String propinaStr = JOptionPane.showInputDialog(this, "¿Cuánto dejó de propina?");
            if (propinaStr != null) {
                try {
                    propina = Double.parseDouble(propinaStr);
                } catch (NumberFormatException e) {
                    JOptionPane.showMessageDialog(this, "Monto inválido.");
                }
            }
        }

        try (Connection conn = MySQLConnection.getConnection()) {
            String sql = "UPDATE cuentas SET estado = 'cerrada', metodo_pago = ?, total = ?, propina = ?, fecha_cierre = NOW() WHERE id = ?";
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setString(1, metodo);
            ps.setDouble(2, total);
            ps.setDouble(3, propina);
            ps.setInt(4, cuentaId);
            ps.executeUpdate();

            String liberar = "UPDATE mesas SET estado = 'libre' WHERE id = ?";
            ps = conn.prepareStatement(liberar);
            ps.setInt(1, mesaId);
            ps.executeUpdate();

            // Mostrar resumen
            String resumen = """
                --- Ticket Final ---
                Método de pago: %s
                Total: $%.2f
                Propina: $%.2f
                Monto pagado: $%.2f
                Cambio: $%.2f
                ---------------------
                ¡Gracias por su visita!
                """.formatted(metodo, total, propina, montoPagado, cambio);

            JTextArea text = new JTextArea(resumen);
            text.setFont(new Font("Monospaced", Font.PLAIN, 14));
            text.setEditable(false);
            JOptionPane.showMessageDialog(this, new JScrollPane(text), "Resumen de cuenta", JOptionPane.INFORMATION_MESSAGE);

            dispose();
            if (comedorFrame != null) {
                comedorFrame.cargarMesas();
            }

        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error al cerrar cuenta: " + e.getMessage());
        }
    }

    private double calcularTotalCuenta() {
        double total = 0;
        try (Connection conn = MySQLConnection.getConnection()) {
            String sql = "SELECT SUM(subtotal) as total FROM detalle_cuenta WHERE cuenta_id = ?";
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, cuentaId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                total = rs.getDouble("total");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return total;
    }
}
