package softarrecife.vista;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.HashSet;
import java.awt.*;
import java.awt.event.*;
import java.sql.*;
import java.util.*;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.*;

import softarrecife.conexion.MySQLConnection;
import softarrecife.ImpresionCerrarCuenta;

public class CuentaFrame extends JFrame {

    private int mesaId;
    private String mesaNombre;
    private int cuentaId;
    private ComedorFrame comedorFrame;

    private JTable tabla;
    private JLabel lblTotal;

    private Set<Integer> productosYaComandeados = new HashSet<>();
    private boolean comandaImpresa = false;
    private boolean ticketYaImpreso = false;
    private boolean cuentaBloqueada = false;

    private JButton btnAgregar;
    private JButton btnEliminar;
    private JButton btnReabrir;

    public CuentaFrame(int mesaId, String mesaNombre, ComedorFrame comedorFrame) {
        this.mesaId = mesaId;
        this.mesaNombre = mesaNombre;
        this.comedorFrame = comedorFrame;

        crearCuentaSiNoExiste();
        verificarBloqueoDesdeBD();
        productosYaComandeados = obtenerIdsProductosDeCuenta();

        setTitle("Cuenta - " + mesaNombre + " (Atiende: " + obtenerNombreMesero() + ")");
        setSize(700, 500);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        construirUI();
        cargarDetalleCuenta();
        aplicarBloqueoVisual();

        setVisible(true);

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                if (!comandaImpresa) {
                    imprimirSoloProductosNuevos();
                    comandaImpresa = true;
                }
            }
        });
    }

    private void verificarBloqueoDesdeBD() {
        try (Connection conn = MySQLConnection.getConnection()) {
            String sql = "SELECT bloqueada FROM cuentas WHERE id = ?";
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, cuentaId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                cuentaBloqueada = rs.getBoolean("bloqueada");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void aplicarBloqueoVisual() {
        if (cuentaBloqueada || ticketYaImpreso) {
            btnAgregar.setEnabled(false);
            btnEliminar.setEnabled(false);
            if (btnReabrir != null) {
                btnReabrir.setVisible(true);
            }
        }
    }

    private Set<Integer> obtenerIdsProductosDeCuenta() {
        Set<Integer> ids = new HashSet<>();
        try (Connection conn = MySQLConnection.getConnection()) {
            String sql = "SELECT producto_id FROM detalle_cuenta WHERE cuenta_id = ?";
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, cuentaId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                ids.add(rs.getInt("producto_id"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return ids;
    }

    private void imprimirSoloProductosNuevos() {
        try (Connection conn = MySQLConnection.getConnection()) {
            String sql = "SELECT p.id, p.nombre, dc.cantidad, p.tipo "
                    + "FROM detalle_cuenta dc "
                    + "JOIN productos p ON dc.producto_id = p.id "
                    + "WHERE dc.cuenta_id = ?";
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, cuentaId);
            ResultSet rs = ps.executeQuery();

            Map<String, List<String>> nuevosPorTipo = new HashMap<>();
            Set<Integer> idsRecientes = new HashSet<>();

            while (rs.next()) {
                int idProd = rs.getInt("id");
                if (productosYaComandeados.contains(idProd)) {
                    continue;
                }

                String nombre = rs.getString("nombre");
                int cantidad = rs.getInt("cantidad");
                String tipo = rs.getString("tipo");

                String linea = String.format("%s x%d", nombre, cantidad);
                nuevosPorTipo.putIfAbsent(tipo, new ArrayList<>());
                nuevosPorTipo.get(tipo).add(linea);

                idsRecientes.add(idProd);
            }

            String mesero = obtenerNombreMesero();
            for (String tipo : nuevosPorTipo.keySet()) {
                List<String> lineas = nuevosPorTipo.get(tipo);
                if (!lineas.isEmpty()) {
                    ImpresionCerrarCuenta.imprimirComandaPorTipo(tipo, lineas, mesaNombre, mesero);
                }
            }

            productosYaComandeados.addAll(idsRecientes);
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error al imprimir comanda: " + e.getMessage());
        }
    }

    private void imprimirTicketCliente() {
        if (ticketYaImpreso || cuentaBloqueada) {
            return;
        }

        try (Connection conn = MySQLConnection.getConnection()) {
            List<String> lineas = new ArrayList<>();
            String sql = "SELECT p.nombre, dc.cantidad, dc.subtotal FROM detalle_cuenta dc JOIN productos p ON dc.producto_id = p.id WHERE dc.cuenta_id = ?";
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, cuentaId);
            ResultSet rs = ps.executeQuery();

            double total = 0;
            while (rs.next()) {
                String nombre = rs.getString("nombre");
                int cantidad = rs.getInt("cantidad");
                double subtotal = rs.getDouble("subtotal");
                total += subtotal;
                lineas.add(String.format("%-15s x%-2d $%.2f", nombre, cantidad, subtotal));
            }

            ImpresionCerrarCuenta.imprimirTicketSinAbrirCaja(lineas, total, 0, total, "PENDIENTE");
            ticketYaImpreso = true;
            cuentaBloqueada = true;
            btnAgregar.setEnabled(false);
            btnEliminar.setEnabled(false);
            actualizarBloqueoEnBD(true);
            btnReabrir.setVisible(true);
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error al imprimir ticket: " + e.getMessage());
        }
    }

    private void actualizarBloqueoEnBD(boolean estado) {
        try (Connection conn = MySQLConnection.getConnection()) {
            String sql = "UPDATE cuentas SET bloqueada = ? WHERE id = ?";
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setBoolean(1, estado);
            ps.setInt(2, cuentaId);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void imprimirComandaActualDesdeBD() {
        try (Connection conn = MySQLConnection.getConnection()) {
            String sql = "SELECT p.nombre, dc.cantidad, p.tipo "
                    + "FROM detalle_cuenta dc "
                    + "JOIN productos p ON dc.producto_id = p.id "
                    + "WHERE dc.cuenta_id = ?";
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, cuentaId);
            ResultSet rs = ps.executeQuery();

            Map<String, List<String>> productosPorTipo = new HashMap<>();

            while (rs.next()) {
                String nombre = rs.getString("nombre");
                int cantidad = rs.getInt("cantidad");
                String tipo = rs.getString("tipo");

                String linea = String.format("%s x%d", nombre, cantidad);
                productosPorTipo.putIfAbsent(tipo, new ArrayList<>());
                productosPorTipo.get(tipo).add(linea);
            }

            String mesero = obtenerNombreMesero();
            for (String tipo : productosPorTipo.keySet()) {
                List<String> lineas = productosPorTipo.get(tipo);
                if (!lineas.isEmpty()) {
                    ImpresionCerrarCuenta.imprimirComandaPorTipo(tipo, lineas, mesaNombre, mesero);
                }
            }

        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error al imprimir comanda: " + e.getMessage());
        }
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

        btnAgregar = new JButton("➕ Agregar producto");
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

        btnEliminar = new JButton("🗑 Eliminar producto");
        btnEliminar.setBackground(new Color(220, 53, 69));
        btnEliminar.setForeground(Color.WHITE);
        btnEliminar.setFocusPainted(false);
        btnEliminar.setFont(fuente);
        btnEliminar.setPreferredSize(new Dimension(180, 35));
        btnEliminar.addActionListener(e -> eliminarProductoSeleccionado());
        panelTop.add(btnEliminar);

        // Verificamos si la cuenta ya está bloqueada desde base de datos
        if (cuentaBloqueada || ticketYaImpreso) {
            btnAgregar.setEnabled(false);
            btnEliminar.setEnabled(false);
        }

        add(panelTop, BorderLayout.NORTH);

        tabla = new JTable();
        tabla.setFont(fuente);
        tabla.setRowHeight(25);
        JScrollPane scroll = new JScrollPane(tabla);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        add(scroll, BorderLayout.CENTER);

        JPanel panelBottom = new JPanel();
        panelBottom.setLayout(new BoxLayout(panelBottom, BoxLayout.LINE_AXIS));
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

        btnReabrir = new JButton("🔓 Reabrir cuenta");
        btnReabrir.setBackground(new Color(255, 193, 7));
        btnReabrir.setForeground(Color.BLACK);
        btnReabrir.setFont(fuente);
        btnReabrir.setPreferredSize(new Dimension(180, 35));
        btnReabrir.setVisible(cuentaBloqueada);

        btnReabrir.addActionListener(e -> {
            cuentaBloqueada = false;
            ticketYaImpreso = false;
            btnAgregar.setEnabled(true);
            btnEliminar.setEnabled(true);
            btnReabrir.setVisible(false);
            try (Connection conn = MySQLConnection.getConnection()) {
                PreparedStatement ps = conn.prepareStatement("UPDATE cuentas SET bloqueada = FALSE WHERE id = ?");
                ps.setInt(1, cuentaId);
                ps.executeUpdate();
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(this, "Error al desbloquear cuenta: " + ex.getMessage());
            }
        });

        panelBottom.add(lblTotal);
        panelBottom.add(Box.createHorizontalGlue()); // esto empuja los botones hacia la derecha
        panelBottom.add(btnReabrir);
        panelBottom.add(Box.createRigidArea(new Dimension(10, 0)));
        panelBottom.add(btnTicket);
        panelBottom.add(Box.createRigidArea(new Dimension(10, 0)));
        panelBottom.add(btnCobrar);

        add(panelBottom, BorderLayout.SOUTH);
    }

    private void crearCuentaSiNoExiste() {
        try (Connection conn = MySQLConnection.getConnection()) {
            String check = "SELECT id, bloqueada FROM cuentas WHERE mesa_id = ? AND estado = 'abierta'";
            PreparedStatement psCheck = conn.prepareStatement(check);
            psCheck.setInt(1, mesaId);
            ResultSet rs = psCheck.executeQuery();

            if (rs.next()) {
                cuentaId = rs.getInt("id");
                cuentaBloqueada = rs.getBoolean("bloqueada");
            } else {
                String insert = "INSERT INTO cuentas (mesa_id, usuario_id, estado, total, fecha, bloqueada) VALUES (?, ?, 'abierta', 0, NOW(), FALSE)";
                PreparedStatement psInsert = conn.prepareStatement(insert, Statement.RETURN_GENERATED_KEYS);
                psInsert.setInt(1, mesaId);
                psInsert.setInt(2, comedorFrame.getUsuarioId());
                psInsert.executeUpdate();
                rs = psInsert.getGeneratedKeys();
                if (rs.next()) {
                    cuentaId = rs.getInt(1);
                    cuentaBloqueada = false;
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

    private void eliminarProductoSeleccionado() {
        if (cuentaBloqueada) {
            JOptionPane.showMessageDialog(this, "La cuenta ya fue bloqueada. No se pueden eliminar productos.");
            return;
        }

        int fila = tabla.getSelectedRow();
        if (fila == -1) {
            JOptionPane.showMessageDialog(this, "Selecciona un producto para eliminar.");
            return;
        }

        String nombreProducto = tabla.getValueAt(fila, 0).toString();
        int confirm = JOptionPane.showConfirmDialog(this,
                "¿Eliminar \"" + nombreProducto + "\" de la cuenta?", "Confirmación",
                JOptionPane.YES_NO_OPTION);

        if (confirm == JOptionPane.YES_OPTION) {
            try (Connection conn = MySQLConnection.getConnection()) {
                String sql = "SELECT dc.id FROM detalle_cuenta dc "
                        + "JOIN productos p ON dc.producto_id = p.id "
                        + "WHERE dc.cuenta_id = ? AND p.nombre = ? LIMIT 1";
                PreparedStatement ps = conn.prepareStatement(sql);
                ps.setInt(1, cuentaId);
                ps.setString(2, nombreProducto);
                ResultSet rs = ps.executeQuery();

                if (rs.next()) {
                    int detalleId = rs.getInt("id");

                    String eliminar = "DELETE FROM detalle_cuenta WHERE id = ?";
                    PreparedStatement psEliminar = conn.prepareStatement(eliminar);
                    psEliminar.setInt(1, detalleId);
                    psEliminar.executeUpdate();

                    cargarDetalleCuenta();
                } else {
                    JOptionPane.showMessageDialog(this, "No se encontró el producto en la base de datos.");
                }

            } catch (SQLException e) {
                JOptionPane.showMessageDialog(this, "Error al eliminar producto: " + e.getMessage());
            }
        }
    }

    private void cerrarCuentaYRegistrarPago() {
        if (!cuentaBloqueada) {
            JOptionPane.showMessageDialog(this, "Primero imprime el ticket del cliente para bloquear la cuenta.");
            return;
        }

        String[] opciones = {"Efectivo", "Tarjeta", "Transferencia"};
        String metodo = (String) JOptionPane.showInputDialog(this, "Método de pago:", "Cobro",
                JOptionPane.QUESTION_MESSAGE, null, opciones, opciones[0]);
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

        double propina = 0;
        String[] tipoPropina = {"% Porcentaje", "$ Monto fijo", "Sin propina"};
        int opcionPropina = JOptionPane.showOptionDialog(this, "¿El cliente dejó propina?", "Propina",
                JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE, null, tipoPropina, tipoPropina[0]);

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
