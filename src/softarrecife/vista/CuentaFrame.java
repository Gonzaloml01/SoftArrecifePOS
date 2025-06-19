package softarrecife.vista;

import softarrecife.conexion.MySQLConnection;
import softarrecife.utils.TicketPrinter;
import softarrecife.ImpresionCerrarCuenta;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

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

        crearCuentaSiNoExiste();
        setTitle("Cuenta - " + mesaNombre + " (Atiende: " + obtenerNombreMesero() + ")");
        setSize(700, 500);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        construirUI();
        cargarDetalleCuenta();
        setVisible(true);
    }

    public String getNombreMesa() {
        return mesaNombre;
    }

    private String obtenerNombreMesero() {
        try (Connection conn = MySQLConnection.getConnection()) {
            String sql = "SELECT u.nombre FROM cuentas c JOIN usuarios u ON c.usuario_id = u.id WHERE c.id = ?";
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

    public String getUsuarioActual() {
        return obtenerNombreMesero();
    }

    private void construirUI() {
        Color verde = new Color(46, 125, 50);
        Color fondo = new Color(245, 245, 245);
        Font fuente = new Font("SansSerif", Font.PLAIN, 14);

        JPanel panelTop = new JPanel(new FlowLayout(FlowLayout.LEFT));
        panelTop.setBackground(fondo);
        panelTop.setBorder(new EmptyBorder(10, 10, 10, 10));

        JButton btnAgregar = new JButton("➕ Agregar producto");
        btnAgregar.addActionListener(e -> {
            SelectorProductoDialog selector = new SelectorProductoDialog(this, cuentaId);
            selector.setModal(true);
            selector.setVisible(true);
            cargarDetalleCuenta();
        });
        btnAgregar.setBackground(new Color(30, 144, 255));
        btnAgregar.setForeground(Color.WHITE);
        btnAgregar.setFocusPainted(false);
        btnAgregar.setFont(fuente);
        btnAgregar.setPreferredSize(new Dimension(180, 35));

        panelTop.add(btnAgregar);
        add(panelTop, BorderLayout.NORTH);

        tabla = new JTable();
        tabla.setFont(fuente);
        tabla.setRowHeight(25);
        JScrollPane scroll = new JScrollPane(tabla);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        add(scroll, BorderLayout.CENTER);

        JPanel panelBottom = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        panelBottom.setBorder(new EmptyBorder(10, 10, 10, 10));
        panelBottom.setBackground(fondo);

        lblTotal = new JLabel("Total: $0.00");
        lblTotal.setFont(new Font("SansSerif", Font.BOLD, 18));
        panelBottom.add(lblTotal);

        JButton btnTicket = new JButton("🧾 Imprimir ticket");
        btnTicket.setBackground(new Color(100, 149, 237));
        btnTicket.setForeground(Color.WHITE);
        btnTicket.setFocusPainted(false);
        btnTicket.setFont(fuente);
        btnTicket.setPreferredSize(new Dimension(180, 35));
        btnTicket.addActionListener(e -> imprimirTicketCliente());

        JButton btnCobrar = new JButton("💳 Cobrar y cerrar cuenta");
        btnCobrar.setBackground(verde);
        btnCobrar.setForeground(Color.WHITE);
        btnCobrar.setFocusPainted(false);
        btnCobrar.setFont(fuente);
        btnCobrar.setPreferredSize(new Dimension(220, 35));
        btnCobrar.addActionListener(e -> cerrarCuentaYRegistrarPago());

        panelBottom.add(btnTicket);
        panelBottom.add(btnCobrar);
        add(panelBottom, BorderLayout.SOUTH);
    }

    private void crearCuentaSiNoExiste() {
        try (Connection conn = MySQLConnection.getConnection()) {
            String check = "SELECT id FROM cuentas WHERE mesa_id = ? AND estado = 'abierta'";
            PreparedStatement psCheck = conn.prepareStatement(check);
            psCheck.setInt(1, mesaId);
            ResultSet rs = psCheck.executeQuery();

            if (rs.next()) {
                cuentaId = rs.getInt("id");
            } else {
                String insert = "INSERT INTO cuentas (mesa_id, usuario_id, estado, total, fecha) VALUES (?, ?, 'abierta', 0, NOW())";
                PreparedStatement psInsert = conn.prepareStatement(insert, Statement.RETURN_GENERATED_KEYS);
                psInsert.setInt(1, mesaId);
                psInsert.setInt(2, comedorFrame.getUsuarioId());
                psInsert.executeUpdate();
                rs = psInsert.getGeneratedKeys();
                if (rs.next()) {
                    cuentaId = rs.getInt(1);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void cargarDetalleCuenta() {
        DefaultTableModel model = new DefaultTableModel();
        model.addColumn("Producto");
        model.addColumn("Cantidad");
        model.addColumn("Subtotal");
        double total = 0;

        try (Connection conn = MySQLConnection.getConnection()) {
            String sql = "SELECT p.nombre, dc.cantidad, dc.subtotal FROM detalle_cuenta dc JOIN productos p ON dc.producto_id = p.id WHERE cuenta_id = ?";
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

        tabla.setRowHeight(28);
        tabla.setFont(new Font("SansSerif", Font.PLAIN, 14));
        tabla.setGridColor(new Color(230, 230, 230));
        tabla.setSelectionBackground(new Color(180, 205, 255));
        tabla.setSelectionForeground(Color.BLACK);

        JTableHeader header = tabla.getTableHeader();
        header.setBackground(new Color(33, 150, 243));
        header.setForeground(Color.WHITE);
        header.setFont(new Font("SansSerif", Font.BOLD, 14));
        header.setReorderingAllowed(false);

        tabla.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            private final Color color1 = new Color(245, 245, 245);
            private final Color color2 = new Color(230, 230, 230);

            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                if (!isSelected) {
                    c.setBackground(row % 2 == 0 ? color1 : color2);
                }
                return c;
            }
        });
    }

    private void imprimirTicketCliente() {
        try (Connection conn = MySQLConnection.getConnection()) {
            List<String> lineas = new ArrayList<>();
            String detalleSQL = "SELECT p.nombre, dc.cantidad, dc.subtotal FROM detalle_cuenta dc JOIN productos p ON dc.producto_id = p.id WHERE dc.cuenta_id = ?";
            PreparedStatement psDetalle = conn.prepareStatement(detalleSQL);
            psDetalle.setInt(1, cuentaId);
            ResultSet rs = psDetalle.executeQuery();

            double total = 0;
            while (rs.next()) {
                String nombre = rs.getString("nombre");
                int cant = rs.getInt("cantidad");
                double subtotal = rs.getDouble("subtotal");
                total += subtotal;
                String linea = String.format("%-15s x%-2d $%.2f", nombre, cant, subtotal);
                lineas.add(linea);
            }

            // No se abre la caja registradora al imprimir el ticket
            ImpresionCerrarCuenta.imprimirTicketSinAbrirCaja(lineas, total, 0, total, "PENDIENTE");

        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error al imprimir ticket: " + e.getMessage());
        }
    }

    private void cerrarCuentaYRegistrarPago() {
        String[] opciones = {"Efectivo", "Tarjeta", "Transferencia"};
        String metodo = (String) JOptionPane.showInputDialog(this, "Método de pago:", "Cobro", JOptionPane.QUESTION_MESSAGE, null, opciones, opciones[0]);
        if (metodo == null) return;

        double total = calcularTotalCuenta();
        double montoPagado = total;
        double cambio = 0;

        if (metodo.equals("Efectivo")) {
            String pagadoStr = JOptionPane.showInputDialog(this, "¿Con cuánto pagó el cliente?");
            if (pagadoStr == null) return;
            try {
                montoPagado = Double.parseDouble(pagadoStr);
                cambio = montoPagado - total;
            } catch (NumberFormatException e) {
                JOptionPane.showMessageDialog(this, "Monto inválido.");
                return;
            }
        }

        double propina = 0;
        String[] tipoPropina = {"% Porcentaje", "$ Monto fijo", "Sin propina"};
        int opcionPropina = JOptionPane.showOptionDialog(this, "¿El cliente dejó propina?", "Propina", JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE, null, tipoPropina, tipoPropina[0]);

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

            double totalConPropina = total + propina;
            List<String> lineas = new ArrayList<>();
            String detalleSQL = "SELECT p.nombre, dc.cantidad, dc.subtotal FROM detalle_cuenta dc JOIN productos p ON dc.producto_id = p.id WHERE dc.cuenta_id = ?";
            PreparedStatement psDetalle = conn.prepareStatement(detalleSQL);
            psDetalle.setInt(1, cuentaId);
            ResultSet rs = psDetalle.executeQuery();

            while (rs.next()) {
                String nombre = rs.getString("nombre");
                int cant = rs.getInt("cantidad");
                double subtotal = rs.getDouble("subtotal");
                String linea = String.format("%-15s x%-2d $%.2f", nombre, cant, subtotal);
                lineas.add(linea);
            }

            // Este sí abre la caja al imprimir el ticket final
            ImpresionCerrarCuenta.imprimirTicket(lineas, total, propina, totalConPropina, metodo);

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
