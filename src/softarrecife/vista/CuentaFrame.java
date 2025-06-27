package softarrecife.vista;

import java.sql.*;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.HashSet;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.*;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.*;
import java.awt.FlowLayout;
import softarrecife.utils.WrapLayout;

import softarrecife.conexion.MySQLConnection;
import softarrecife.ImpresionCerrarCuenta;

public class CuentaFrame extends JFrame {

    private int mesaId;
    private String mesaNombre;
    private int cuentaId;
    private ComedorFrame comedorFrame;

    private JTable tabla;
    private JLabel lblTotal;
    private JLabel lblDescuento;

    private Set<Integer> productosYaComandeados = new HashSet<>();
    private boolean comandaImpresa = false;
    private boolean ticketYaImpreso = false;
    private boolean cuentaBloqueada = false;
    // Estados de botones interactivos
    private boolean modoEliminar = false;
    private boolean modoComentario = false;
    private boolean modoDescuento = false;
    private boolean modoDividir = false;

    private JButton btnAgregar;
    private JButton btnEliminar;
    private JButton btnReabrir;
    private JButton btnComentario;
    private JButton btnDescProducto;
    private JButton btnDescTotal;
    private JButton btnDividir;
    private JButton btnImprimir;
    private JButton btnCobrar;

    private enum ModoInteraccion {
        NINGUNO, ELIMINAR, DESCUENTO_PRODUCTO, COMENTARIO
    }
    private ModoInteraccion modoActual = ModoInteraccion.NINGUNO;

    public CuentaFrame(int mesaId, String mesaNombre, ComedorFrame comedorFrame) {
        this.mesaId = mesaId;
        this.mesaNombre = mesaNombre;
        this.comedorFrame = comedorFrame;

        crearCuentaSiNoExiste();
        verificarBloqueoDesdeBD();
        verificarEstadoCuenta();
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

    private void verificarEstadoCuenta() {
        try (Connection conn = MySQLConnection.getConnection()) {
            String sql = "SELECT estado FROM cuentas WHERE id = ?";
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, cuentaId);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                String estado = rs.getString("estado");

                if (estado.equalsIgnoreCase("cerrada")) {
                    cuentaBloqueada = true;
                    ticketYaImpreso = true;
                    aplicarBloqueoVisual(); // desactiva botones de edición
                    // PERO dejar habilitado cobrar si no se ha cobrado aún (esto depende de si agregas campo `pagada`)
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void construirUI() {
        Color verde = new Color(46, 125, 50);
        Color fondo = new Color(245, 245, 245);
        Font fuente = new Font("SansSerif", Font.PLAIN, 14);

        JPanel panelTop = new JPanel(new WrapLayout(FlowLayout.LEFT));
        panelTop.setBackground(fondo);
        panelTop.setBorder(new EmptyBorder(10, 10, 10, 10));

        // BOTÓN AGREGAR
        btnAgregar = new JButton("➕ Agregar producto");
        btnAgregar.setBackground(new Color(30, 144, 255));
        btnAgregar.setForeground(Color.WHITE);
        btnAgregar.setFocusPainted(false);
        btnAgregar.setFont(fuente);
        btnAgregar.setPreferredSize(new Dimension(180, 35));
        btnAgregar.addActionListener(e -> {
            SelectorProductoDialog selector = new SelectorProductoDialog(this, cuentaId);
            selector.setModal(true);
            selector.setVisible(true);
            cargarDetalleCuenta();
        });
        panelTop.add(btnAgregar);

        // BOTÓN ELIMINAR (con modo selección)
        btnEliminar = new JButton("🗑 Eliminar producto");
        btnEliminar.setBackground(new Color(220, 53, 69));
        btnEliminar.setForeground(Color.WHITE);
        btnEliminar.setFont(fuente);
        btnEliminar.setPreferredSize(new Dimension(180, 35));
        btnEliminar.addActionListener(e -> activarModoBoton("eliminar"));
        panelTop.add(btnEliminar);

        // BOTÓN COMENTARIO
        btnComentario = new JButton("💬 Comentario producto");
        btnComentario.setBackground(new Color(255, 193, 7));
        btnComentario.setForeground(Color.BLACK);
        btnComentario.setFont(fuente);
        btnComentario.setPreferredSize(new Dimension(180, 35));
        btnComentario.addActionListener(e -> activarModoBoton("comentario"));
        panelTop.add(btnComentario);

        // BOTÓN DESCUENTO PRODUCTO
        btnDescProducto = new JButton("🔻 Descuento producto");
        btnDescProducto.setBackground(new Color(102, 187, 106));
        btnDescProducto.setForeground(Color.BLACK);
        btnDescProducto.setFont(fuente);
        btnDescProducto.setPreferredSize(new Dimension(180, 35));
        btnDescProducto.addActionListener(e -> activarModoBoton("descuento"));
        panelTop.add(btnDescProducto);

        // BOTÓN DESCUENTO TOTAL
        btnDescTotal = new JButton("🧮 Descuento total");
        btnDescTotal.setBackground(new Color(66, 165, 245));
        btnDescTotal.setForeground(Color.WHITE);
        btnDescTotal.setFont(fuente);
        btnDescTotal.setPreferredSize(new Dimension(180, 35));
        btnDescTotal.addActionListener(e -> aplicarDescuentoTotal());
        panelTop.add(btnDescTotal);

        // BOTÓN DIVIDIR
        btnDividir = new JButton("✂️ Dividir cuenta");
        btnDividir.setBackground(new Color(240, 240, 240));
        btnDividir.setForeground(Color.BLACK);
        btnDividir.setFont(fuente);
        btnDividir.setPreferredSize(new Dimension(180, 35));
        btnDividir.addActionListener(e -> dividirCuenta());

        panelTop.add(btnDividir);

        if (cuentaBloqueada || ticketYaImpreso) {
            btnAgregar.setEnabled(false);
            btnEliminar.setEnabled(false);
            btnComentario.setEnabled(false);
            btnDescProducto.setEnabled(false);
            btnDescTotal.setEnabled(false);
            btnDividir.setEnabled(false);
        }

        add(panelTop, BorderLayout.NORTH);

        // TABLA
        tabla = new JTable();
        tabla.setFont(new Font("SansSerif", Font.PLAIN, 14));
        tabla.setRowHeight(28);
        tabla.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        tabla.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int fila = tabla.getSelectedRow();
                if (modoEliminar) {
                    eliminarProductoSeleccionado();
                    modoEliminar = false;
                    resetearBoton(btnEliminar);
                } else if (modoComentario) {
                    agregarComentarioAProducto();
                    modoComentario = false;
                    resetearBoton(btnComentario);
                } else if (modoDescuento) {
                    aplicarDescuentoAProducto();
                    modoDescuento = false;
                    resetearBoton(btnDescProducto);
                } else if (modoDividir) {
                    dividirCuenta();
                    modoDividir = false;
                    resetearBoton(btnDividir);
                }
            }
        });

        JScrollPane scrollPane = new JScrollPane(tabla);
        scrollPane.setBorder(new EmptyBorder(10, 10, 10, 10));
        add(scrollPane, BorderLayout.CENTER);

        // Panel inferior (total izquierda, botones derecha)
        JPanel panelInferior = new JPanel(new BorderLayout());

// Total principal
        lblTotal = new JLabel("Total: $0.00");
        lblTotal.setFont(new Font("SansSerif", Font.BOLD, 16));
        lblTotal.setHorizontalAlignment(SwingConstants.LEFT);
        lblTotal.setBorder(new EmptyBorder(10, 20, 0, 10));
        panelInferior.add(lblTotal, BorderLayout.WEST);

// Descuento visual
        lblDescuento = new JLabel("");  // inicialmente vacío
        lblDescuento.setFont(new Font("SansSerif", Font.PLAIN, 14));
        lblDescuento.setForeground(new Color(220, 53, 69));  // rojo suave
        lblDescuento.setHorizontalAlignment(SwingConstants.LEFT);
        lblDescuento.setBorder(new EmptyBorder(0, 20, 10, 10));
        panelInferior.add(lblDescuento, BorderLayout.CENTER);

// Botones (imprimir y cobrar)
        JPanel panelBotonesInferiores = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        panelBotonesInferiores.setBorder(new EmptyBorder(10, 10, 10, 20));

        btnImprimir = new JButton("🖨 Imprimir ticket");
        btnImprimir.setFont(fuente);
        btnImprimir.setEnabled(true);
        btnImprimir.setBackground(new Color(100, 149, 237));
        btnImprimir.setForeground(Color.WHITE);
        btnImprimir.addActionListener(e -> imprimirTicketCliente());
        panelBotonesInferiores.add(btnImprimir);

        btnCobrar = new JButton("💳 Cobrar y cerrar cuenta");
        btnCobrar.setFont(fuente);
        btnCobrar.setBackground(new Color(46, 125, 50));
        btnCobrar.setForeground(Color.WHITE);
        btnCobrar.addActionListener(e -> cobrarCuenta());
        panelBotonesInferiores.add(btnCobrar);

        btnReabrir = new JButton("🔓 Reabrir cuenta");
        btnReabrir.setFont(new Font("SansSerif", Font.PLAIN, 14));
        btnReabrir.setBackground(new Color(255, 193, 7));
        btnReabrir.setForeground(Color.BLACK);
        btnReabrir.setVisible(false); // se muestra solo si cuenta cerrada
        btnReabrir.addActionListener(e -> reabrirCuenta());
        panelBotonesInferiores.add(btnReabrir);

        panelInferior.add(panelBotonesInferiores, BorderLayout.EAST);
        add(panelInferior, BorderLayout.SOUTH);

        // Manejo de clics en la tabla según modo activo
        tabla.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int fila = tabla.getSelectedRow();
                if (fila != -1) {
                    if (modoEliminar) {
                        modoEliminar = false;
                        eliminarProductoSeleccionado();
                    } else if (modoComentario) {
                        modoComentario = false;
                        agregarComentarioAProducto();
                    } else if (modoDescuento) {
                        modoDescuento = false;
                        aplicarDescuentoAProducto();
                    } else if (modoDividir) {
                        modoDividir = false;
                        dividirCuenta();
                    }
                }
            }
        });

    }

    public void cargarDetalleCuenta() {
        DefaultTableModel model = new DefaultTableModel() {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        model.addColumn("ID");         // detalle_id (oculto)
        model.addColumn("Producto");
        model.addColumn("Cantidad");
        model.addColumn("Subtotal");
        model.addColumn("ProductoID"); // columna oculta con el producto_id

        double total = 0;

        try (Connection conn = MySQLConnection.getConnection()) {
            String sql = "SELECT dc.id, p.nombre, dc.cantidad, dc.subtotal, dc.comentario, dc.producto_id "
                    + "FROM detalle_cuenta dc JOIN productos p ON dc.producto_id = p.id WHERE cuenta_id = ?";
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, cuentaId);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                int idDetalle = rs.getInt("id");
                String nombre = rs.getString("nombre");
                int cantidad = rs.getInt("cantidad");
                double subtotal = rs.getDouble("subtotal");
                int productoId = rs.getInt("producto_id");
                String comentario = rs.getString("comentario");

                model.addRow(new Object[]{idDetalle, nombre, cantidad, subtotal, productoId});

                if (comentario != null && !comentario.trim().isEmpty()) {
                    model.addRow(new Object[]{"", "    → " + comentario, "", "", ""});
                }

                total += subtotal;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        // Consultar si hay descuento aplicado
        double descuento = 0;
        try (Connection conn = MySQLConnection.getConnection()) {
            String sql = "SELECT descuento FROM cuentas WHERE id = ?";
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, cuentaId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                descuento = rs.getDouble("descuento");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        // Mostrar total con o sin descuento
        if (descuento > 0) {
            double totalConDescuento = total * (1 - descuento / 100);
            lblTotal.setText("Total: $" + String.format("%.2f", totalConDescuento));
            lblDescuento.setText("Descuento aplicado: -" + descuento + "%");
        } else {
            lblTotal.setText("Total: $" + String.format("%.2f", total));
            lblDescuento.setText("");
        }

        tabla.setModel(model);

        // Ocultar columnas ID y ProductoID
        tabla.getColumnModel().getColumn(0).setMinWidth(0);
        tabla.getColumnModel().getColumn(0).setMaxWidth(0);
        tabla.getColumnModel().getColumn(0).setWidth(0);

        tabla.getColumnModel().getColumn(4).setMinWidth(0);
        tabla.getColumnModel().getColumn(4).setMaxWidth(0);
        tabla.getColumnModel().getColumn(4).setWidth(0);

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

    private void agregarComentarioAProducto() {
        int[] filas = tabla.getSelectedRows();

        if (filas.length == 0) {
            JOptionPane.showMessageDialog(this, "Selecciona un producto para agregar comentario.");
            return;
        }

        if (filas.length > 1) {
            JOptionPane.showMessageDialog(this, "Selecciona solo un producto para agregar comentario.");
            return;
        }

        int fila = filas[0];
        String nombre = tabla.getValueAt(fila, 1).toString();
        if (nombre.startsWith("→")) {
            JOptionPane.showMessageDialog(this, "Selecciona una fila de producto, no un comentario.");
            return;
        }

        int idDetalle = obtenerIdDetalleSeleccionado(fila);
        if (idDetalle == -1) {
            JOptionPane.showMessageDialog(this, "No se pudo obtener el ID del producto.");
            return;
        }

        String nuevoComentario = JOptionPane.showInputDialog(this, "Comentario para " + nombre);
        if (nuevoComentario != null) {
            try (Connection conn = MySQLConnection.getConnection()) {
                String sql = "UPDATE detalle_cuenta SET comentario = ? WHERE id = ?";
                PreparedStatement ps = conn.prepareStatement(sql);
                ps.setString(1, nuevoComentario);
                ps.setInt(2, idDetalle);
                ps.executeUpdate();
                cargarDetalleCuenta();
            } catch (SQLException e) {
                JOptionPane.showMessageDialog(this, "Error al guardar comentario: " + e.getMessage());
            }
        }
    }

    private void aplicarDescuentoAProducto() {
        int fila = tabla.getSelectedRow();
        if (fila == -1) {
            JOptionPane.showMessageDialog(this, "Selecciona un producto para aplicar descuento.");
            return;
        }

        String nombre = tabla.getValueAt(fila, 1).toString().trim();
        if (nombre.startsWith("→")) {
            JOptionPane.showMessageDialog(this, "Selecciona una fila de producto, no un comentario.");
            return;
        }

        String input = JOptionPane.showInputDialog(this, "Ingresa el porcentaje de descuento (ej. 10):");
        if (input == null || input.trim().isEmpty()) {
            return;
        }

        double porcentaje;
        try {
            porcentaje = Double.parseDouble(input.trim());
            if (porcentaje < 0 || porcentaje > 100) {
                JOptionPane.showMessageDialog(this, "Porcentaje inválido. Debe estar entre 0 y 100.");
                return;
            }
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Número inválido.");
            return;
        }

        int idDetalle = obtenerIdDetalleSeleccionado(fila);
        if (idDetalle == -1) {
            JOptionPane.showMessageDialog(this, "No se pudo obtener el ID del producto.");
            return;
        }

        try (Connection conn = MySQLConnection.getConnection()) {
            // Se recalcula el subtotal original y se aplica el nuevo
            String sql = "SELECT subtotal FROM detalle_cuenta WHERE id = ?";
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, idDetalle);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                double original = rs.getDouble("subtotal");
                double nuevoSubtotal = original * (1 - porcentaje / 100.0);

                String update = "UPDATE detalle_cuenta SET subtotal = ?, descuento = ? WHERE id = ?";
                PreparedStatement psUpdate = conn.prepareStatement(update);
                psUpdate.setDouble(1, nuevoSubtotal);
                psUpdate.setDouble(2, porcentaje);
                psUpdate.setInt(3, idDetalle);
                psUpdate.executeUpdate();
            }

            cargarDetalleCuenta();
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Error al aplicar el descuento.");
            ex.printStackTrace();
        }
    }

    private void aplicarDescuentoTotal() {
        if (cuentaBloqueada) {
            JOptionPane.showMessageDialog(this, "La cuenta ya fue cerrada.");
            return;
        }

        String input = JOptionPane.showInputDialog(this, "Ingresa el porcentaje de descuento (ej. 10 para 10%)");
        if (input == null || input.trim().isEmpty()) {
            return;
        }

        try {
            double porcentaje = Double.parseDouble(input);
            if (porcentaje <= 0 || porcentaje >= 100) {
                JOptionPane.showMessageDialog(this, "Porcentaje inválido.");
                return;
            }

            try (Connection conn = MySQLConnection.getConnection()) {
                // Aplicamos descuento
                String sql = "UPDATE cuentas SET descuento = ? WHERE id = ?";
                PreparedStatement ps = conn.prepareStatement(sql);
                ps.setDouble(1, porcentaje);
                ps.setInt(2, cuentaId);
                ps.executeUpdate();

                // ⬅️ Aquí colocas la línea para mostrar el mensaje visual
                lblDescuento.setText("Descuento aplicado: -" + porcentaje + "%");

                cargarDetalleCuenta(); // recarga el total con descuento aplicado
            }

        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Porcentaje no válido.");
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error al aplicar descuento: " + e.getMessage());
        }
    }

    private void recalcularTotalCuenta() {
        try (Connection conn = MySQLConnection.getConnection()) {
            String sql = "SELECT SUM(subtotal) as total FROM detalle_cuenta WHERE cuenta_id = ?";
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, cuentaId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                double total = rs.getDouble("total");
                String update = "UPDATE cuentas SET total = ? WHERE id = ?";
                ps = conn.prepareStatement(update);
                ps.setDouble(1, total);
                ps.setInt(2, cuentaId);
                ps.executeUpdate();
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error al recalcular total: " + e.getMessage());
        }
    }

    private void imprimirTicketCliente() {
        if (ticketYaImpreso || cuentaBloqueada) {
            return;
        }

        try (Connection conn = MySQLConnection.getConnection()) {
            List<String> lineas = new ArrayList<>();
            String sql = "SELECT p.nombre, dc.cantidad, dc.subtotal, dc.comentario FROM detalle_cuenta dc JOIN productos p ON dc.producto_id = p.id WHERE dc.cuenta_id = ?";
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, cuentaId);
            ResultSet rs = ps.executeQuery();

            double total = 0;
            while (rs.next()) {
                String nombre = rs.getString("nombre");
                int cantidad = rs.getInt("cantidad");
                double subtotal = rs.getDouble("subtotal");
                String comentario = rs.getString("comentario");

                total += subtotal;
                lineas.add(String.format("%-15s x%-2d $%.2f", nombre, cantidad, subtotal));
                if (comentario != null && !comentario.trim().isEmpty()) {
                    lineas.add("    → " + comentario);
                }
            }

            ImpresionCerrarCuenta.imprimirTicketSinAbrirCaja(lineas, total, 0, total, "PENDIENTE");

            // Bloquear la cuenta en la interfaz
            ticketYaImpreso = true;
            cuentaBloqueada = true;

            // Desactivar todos los botones de edición
            btnAgregar.setEnabled(false);
            btnEliminar.setEnabled(false);
            btnComentario.setEnabled(false);
            btnDescProducto.setEnabled(false);
            btnDescTotal.setEnabled(false);
            btnDividir.setEnabled(false);
            btnImprimir.setEnabled(false);

            // Activar sólo los necesarios
            btnCobrar.setEnabled(true);
            btnReabrir.setVisible(true);

            // Reflejar estado en base de datos
            actualizarBloqueoEnBD(true);

        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error al imprimir ticket: " + e.getMessage());
        }
    }

    private void imprimirSoloProductosNuevos() {
        try (Connection conn = MySQLConnection.getConnection()) {
            String sql = "SELECT p.id, p.nombre, dc.cantidad, p.tipo, dc.comentario "
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
                String comentario = rs.getString("comentario");

                String linea = String.format("%s x%d", nombre, cantidad);
                if (comentario != null && !comentario.trim().isEmpty()) {
                    linea += " → " + comentario;
                }

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

    private void verificarBloqueoDesdeBD() {
        try (Connection conn = MySQLConnection.getConnection()) {
            String sql = "SELECT estado FROM cuentas WHERE id = ?";
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, cuentaId);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                String estado = rs.getString("estado");
                cuentaBloqueada = !"abierta".equalsIgnoreCase(estado);
                ticketYaImpreso = cuentaBloqueada;
            }
        } catch (SQLException e) {
            e.printStackTrace();
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

    private void aplicarBloqueoVisual() {
        if (cuentaBloqueada || ticketYaImpreso) {
            btnAgregar.setEnabled(false);
            btnEliminar.setEnabled(false);
            if (btnReabrir != null) {
                btnReabrir.setVisible(true);
            }
        }
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

        String nombre = tabla.getValueAt(fila, 1).toString().trim(); // nombre en columna 1
        if (nombre.startsWith("→")) {
            JOptionPane.showMessageDialog(this, "No se puede eliminar un comentario.");
            return;
        }

        int idDetalle = obtenerIdDetalleSeleccionado(fila);
        if (idDetalle == -1) {
            JOptionPane.showMessageDialog(this, "No se pudo obtener el ID del producto seleccionado.");
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(this,
                "¿Eliminar \"" + nombre + "\" de la cuenta?", "Confirmación",
                JOptionPane.YES_NO_OPTION);

        if (confirm == JOptionPane.YES_OPTION) {
            eliminarDetallePorId(idDetalle);
            cargarDetalleCuenta();
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

    private void cobrarCuenta() {
        if (cuentaEstaCerradaEnBD()) {
            JOptionPane.showMessageDialog(this, "Esta cuenta ya fue cobrada.");
            return;
        }

        if (!ticketYaImpreso) {
            JOptionPane.showMessageDialog(this, "Primero debes imprimir el ticket.");
            return;
        }

        String[] metodosPago = {"Efectivo", "Tarjeta", "Transferencia"};
        String metodo = (String) JOptionPane.showInputDialog(
                this,
                "Selecciona el método de pago:",
                "Método de pago",
                JOptionPane.PLAIN_MESSAGE,
                null,
                metodosPago,
                metodosPago[0]
        );

        if (metodo == null) {
            return;
        }

        double total = calcularTotalCuenta();

        String inputPagado = JOptionPane.showInputDialog(this, "Monto pagado ($):", total);
        if (inputPagado == null) {
            return;
        }

        double pagado;
        try {
            pagado = Double.parseDouble(inputPagado);
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Monto inválido.");
            return;
        }

        double cambio = pagado - total;
        if (cambio < 0) {
            JOptionPane.showMessageDialog(this, "El monto pagado es menor al total.");
            return;
        }

        // Preguntar propina
        String[] opcionesPropina = {"0%", "5%", "10%", "15%", "Otro"};
        String seleccion = (String) JOptionPane.showInputDialog(
                this,
                "¿Desea dejar propina?",
                "Propina",
                JOptionPane.QUESTION_MESSAGE,
                null,
                opcionesPropina,
                opcionesPropina[2]
        );

        double propina = 0;
        if (seleccion != null) {
            if (seleccion.equals("Otro")) {
                String propinaCustom = JOptionPane.showInputDialog(this, "Propina en %:");
                try {
                    double porcentaje = Double.parseDouble(propinaCustom);
                    propina = total * (porcentaje / 100.0);
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(this, "Propina inválida.");
                    return;
                }
            } else {
                int porcentaje = Integer.parseInt(seleccion.replace("%", ""));
                propina = total * (porcentaje / 100.0);
            }
        }

        double totalConPropina = total + propina;

        // Guardar en BD
        try (Connection conn = MySQLConnection.getConnection()) {
            String sql = "UPDATE cuentas SET total = ?, metodo_pago = ?, propina = ?, estado = 'cerrada', fecha_cierre = NOW() WHERE id = ?";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setDouble(1, total);
                stmt.setString(2, metodo);
                stmt.setDouble(3, propina);
                stmt.setInt(4, cuentaId);
                stmt.executeUpdate();
            }

            // 🔄 Actualizar estado de la mesa a libre
            try (PreparedStatement psMesa = conn.prepareStatement("UPDATE mesas SET estado = 'libre' WHERE id = ?")) {
                psMesa.setInt(1, mesaId); // asegúrate que mesaId esté declarado correctamente en la clase
                psMesa.executeUpdate();
            }

            // Imprimir ticket definitivo (con método y propina)
            List<String> lineas = obtenerLineasTicket();
            ImpresionCerrarCuenta.imprimirTicketSinAbrirCaja(lineas, total, propina, totalConPropina, metodo);

            cuentaBloqueada = true;
            aplicarBloqueoVisual();

            JOptionPane.showMessageDialog(this, "Cuenta cerrada. Cambio: $" + String.format("%.2f", cambio));
            comedorFrame.cargarMesas(); // 🔄 Actualiza mesas en tiempo real
            dispose(); // Cierra ventana de cuenta

        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error al cerrar cuenta: " + e.getMessage());
        }
    }

    private void actualizarTotalConDescuento() {
        double totalSinDescuento = calcularTotalCuenta();
        double descuento = obtenerDescuentoTotalDeBD();
        double totalFinal = totalSinDescuento * (1 - (descuento / 100));

        lblTotal.setText(String.format("Total: $%.2f", totalFinal));
    }

    private double obtenerDescuentoTotalDeBD() {
        double descuento = 0.0;
        try (Connection conn = MySQLConnection.getConnection(); PreparedStatement stmt = conn.prepareStatement("SELECT descuento_total FROM cuentas WHERE id = ?")) {
            stmt.setInt(1, cuentaId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                descuento = rs.getDouble("descuento_total");
            }
        } catch (SQLException e) {
            System.err.println("Error al obtener descuento total: " + e.getMessage());
        }
        return descuento;
    }

    private int obtenerIdDetalleSeleccionado(int fila) {
        try {
            Object valor = tabla.getValueAt(fila, 0); // Columna 0 = ID oculto
            if (valor != null && !valor.toString().isBlank()) {
                return Integer.parseInt(valor.toString());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return -1; // Si no se pudo obtener
    }

    private int obtenerIdDetallePorFila(int fila) {
        try (Connection conn = MySQLConnection.getConnection()) {
            Object objNombre = tabla.getValueAt(fila, 0);
            Object objCantidad = tabla.getValueAt(fila, 1);

            if (objNombre == null || objCantidad == null) {
                return -1;
            }

            String nombre = objNombre.toString().trim();
            if (nombre.startsWith("→")) {
                return -1; // es comentario
            }
            int cantidad = Integer.parseInt(objCantidad.toString());

            String sql = "SELECT dc.id FROM detalle_cuenta dc "
                    + "JOIN productos p ON dc.producto_id = p.id "
                    + "WHERE p.nombre = ? AND dc.cuenta_id = ? AND dc.cantidad = ? LIMIT 1";

            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setString(1, nombre);
            ps.setInt(2, cuentaId);
            ps.setInt(3, cantidad);

            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getInt("id");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return -1;
    }

    private void eliminarDetallePorId(int idDetalle) {
        try (Connection conn = MySQLConnection.getConnection()) {
            String sql = "DELETE FROM detalle_cuenta WHERE id = ?";
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, idDetalle);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void activarModoBoton(String modo) {
        modoEliminar = modo.equals("eliminar");
        modoComentario = modo.equals("comentario");
        modoDescuento = modo.equals("descuento");
        modoDividir = modo.equals("dividir");

        // Estilo visual
        btnEliminar.setBackground(modoEliminar ? new Color(200, 0, 0) : new Color(220, 53, 69));
        btnComentario.setBackground(modoComentario ? new Color(255, 215, 0) : new Color(255, 193, 7));
        btnDescProducto.setBackground(modoDescuento ? new Color(60, 170, 90) : new Color(102, 187, 106));
        btnDividir.setBackground(modoDividir ? new Color(200, 200, 200) : new Color(240, 240, 240));

        // Mostrar mensaje solo en caso de dividir
        if (modoDividir) {
            tabla.clearSelection(); // para evitar confusiones
            JOptionPane.showMessageDialog(this, "Selecciona los productos que deseas mover a otra cuenta.");
        }
    }

    private void resetearBoton(JButton boton) {
        // Devuelve el botón a su color original
        if (boton == btnEliminar) {
            boton.setBackground(new Color(220, 53, 69));
        } else if (boton == btnComentario) {
            boton.setBackground(new Color(255, 193, 7));
        } else if (boton == btnDescProducto) {
            boton.setBackground(new Color(102, 187, 106));
        } else if (boton == btnDividir) {
            boton.setBackground(new Color(240, 240, 240));
        }
    }

    private void reabrirCuenta() {
        int opcion = JOptionPane.showConfirmDialog(this,
                "¿Seguro que deseas reabrir la cuenta?\nEsto permitirá modificarla nuevamente.",
                "Confirmar",
                JOptionPane.YES_NO_OPTION);

        if (opcion != JOptionPane.YES_OPTION) {
            return;
        }

        try (Connection conn = MySQLConnection.getConnection()) {
            String sql = "UPDATE cuentas SET estado = 'abierta', fecha_cierre = NULL, bloqueada = 0 WHERE id = ?";
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, cuentaId);
            ps.executeUpdate();
        } catch (SQLException ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error al reabrir la cuenta.");
            return;
        }

        cuentaBloqueada = false;
        ticketYaImpreso = false;

        btnAgregar.setEnabled(true);
        btnEliminar.setEnabled(true);
        btnComentario.setEnabled(true);
        btnDescProducto.setEnabled(true);
        btnDescTotal.setEnabled(true);
        btnDividir.setEnabled(true);
        btnImprimir.setEnabled(true);
        btnCobrar.setEnabled(true);
        btnReabrir.setVisible(false);

        cargarDetalleCuenta();

        JOptionPane.showMessageDialog(this, "Cuenta reabierta con éxito.");
    }

    private List<String> obtenerLineasTicket() {
        List<String> lineas = new ArrayList<>();

        try (Connection conn = MySQLConnection.getConnection()) {
            String sql = "SELECT p.nombre, dc.cantidad, dc.subtotal, dc.comentario "
                    + "FROM detalle_cuenta dc JOIN productos p ON dc.producto_id = p.id "
                    + "WHERE dc.cuenta_id = ?";
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, cuentaId);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                String nombre = rs.getString("nombre");
                int cantidad = rs.getInt("cantidad");
                double subtotal = rs.getDouble("subtotal");
                String comentario = rs.getString("comentario");

                lineas.add(String.format("%-15s x%-2d $%.2f", nombre, cantidad, subtotal));
                if (comentario != null && !comentario.trim().isEmpty()) {
                    lineas.add("    → " + comentario);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return lineas;
    }

    private boolean cuentaEstaCerradaEnBD() {
        try (Connection conn = MySQLConnection.getConnection()) {
            String sql = "SELECT estado FROM cuentas WHERE id = ?";
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, cuentaId);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                String estado = rs.getString("estado");
                return estado.equalsIgnoreCase("cerrada");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return true; // Por seguridad, si falla la consulta asumimos que está cerrada
    }

    private String generarNombreCuentaDividida(String nombreOriginal) {
        Set<String> sufijosExistentes = new HashSet<>();

        try (Connection conn = MySQLConnection.getConnection()) {
            String sql = "SELECT nombre FROM mesas WHERE nombre LIKE ? AND estado = 'ocupada'";
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setString(1, nombreOriginal + " - %");
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                String nombre = rs.getString("nombre");
                if (nombre.matches(nombreOriginal + " - [A-Z]")) {
                    String sufijo = nombre.substring(nombre.length() - 1);
                    sufijosExistentes.add(sufijo);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        // Buscar la primera letra disponible
        for (char c = 'A'; c <= 'Z'; c++) {
            if (!sufijosExistentes.contains(String.valueOf(c))) {
                return nombreOriginal + " - " + c;
            }
        }

        // Si se pasaron de Z, usar un número (raro pero posible)
        return nombreOriginal + " - " + (sufijosExistentes.size() + 1);
    }

    private void dividirCuenta() {
        int[] filasSeleccionadas = tabla.getSelectedRows();

        if (filasSeleccionadas.length == 0) {
            JOptionPane.showMessageDialog(this, "Selecciona al menos un producto para dividir.");
            return;
        }

        try (Connection conn = MySQLConnection.getConnection()) {
            conn.setAutoCommit(false);

            // 1. Crear nueva mesa
            String nuevoNombre = generarNombreCuentaDividida(mesaNombre);
            String insertMesaSQL = "INSERT INTO mesas (nombre, estado, usuario_id) VALUES (?, 'ocupada', ?)";
            PreparedStatement psMesa = conn.prepareStatement(insertMesaSQL, Statement.RETURN_GENERATED_KEYS);
            psMesa.setString(1, nuevoNombre);
            psMesa.setInt(2, comedorFrame.getUsuarioId());
            psMesa.executeUpdate();
            ResultSet rsMesa = psMesa.getGeneratedKeys();
            rsMesa.next();
            int nuevaMesaId = rsMesa.getInt(1);

            // 2. Crear nueva cuenta
            String insertCuentaSQL = "INSERT INTO cuentas (mesa_id, estado, fecha) VALUES (?, 'abierta', NOW())";
            PreparedStatement psCuenta = conn.prepareStatement(insertCuentaSQL, Statement.RETURN_GENERATED_KEYS);
            psCuenta.setInt(1, nuevaMesaId);
            psCuenta.executeUpdate();
            ResultSet rsCuenta = psCuenta.getGeneratedKeys();
            rsCuenta.next();
            int nuevaCuentaId = rsCuenta.getInt(1);

            // 3. Mover productos seleccionados
            for (int fila : filasSeleccionadas) {
                Object valorId = tabla.getValueAt(fila, 0);
                Object valorProducto = tabla.getValueAt(fila, 1);
                Object valorCantidad = tabla.getValueAt(fila, 2);
                Object valorSubtotal = tabla.getValueAt(fila, 3);

                // Si la fila es comentario o está vacía, se ignora
                if (valorId == null || valorProducto == null || valorProducto.toString().startsWith("→")) {
                    continue;
                }

                int detalleId = Integer.parseInt(valorId.toString());
                int productoId = obtenerProductoIdPorNombre(valorProducto.toString());
                int cantidad = Integer.parseInt(valorCantidad.toString());

                double subtotal;
                if (valorSubtotal instanceof Double) {
                    subtotal = (double) valorSubtotal;
                } else if (valorSubtotal instanceof Integer) {
                    subtotal = ((Integer) valorSubtotal).doubleValue();
                } else {
                    subtotal = Double.parseDouble(valorSubtotal.toString());
                }

                // Insertar en nueva cuenta
                String insertDetalle = "INSERT INTO detalle_cuenta (cuenta_id, producto_id, cantidad, subtotal) VALUES (?, ?, ?, ?)";
                PreparedStatement psInsert = conn.prepareStatement(insertDetalle);
                psInsert.setInt(1, nuevaCuentaId);
                psInsert.setInt(2, productoId);
                psInsert.setInt(3, cantidad);
                psInsert.setDouble(4, subtotal);
                psInsert.executeUpdate();

                // Eliminar de cuenta original
                String deleteDetalle = "DELETE FROM detalle_cuenta WHERE id = ?";
                PreparedStatement psDelete = conn.prepareStatement(deleteDetalle);
                psDelete.setInt(1, detalleId);
                psDelete.executeUpdate();
            }

            conn.commit();

            // 🔄 Actualizar interfaz
            comedorFrame.cargarMesas();
            cargarDetalleCuenta();
            actualizarTotal();

            // Abrir ventana con la nueva cuenta dividida
            SwingUtilities.invokeLater(() -> {
                try {
                    CuentaFrame nuevaCuenta = new CuentaFrame(nuevaMesaId, nuevoNombre, comedorFrame);
                    nuevaCuenta.setVisible(true);         // Mostrar la ventana
                    nuevaCuenta.toFront();                // Ponerla enfrente
                    nuevaCuenta.requestFocus();           // Darle foco
                } catch (Exception ex) {
                    ex.printStackTrace();
                    JOptionPane.showMessageDialog(this, "Error al abrir nueva cuenta: " + ex.getMessage());
                }
            });

            JOptionPane.showMessageDialog(this, "Cuenta dividida. Nueva mesa: " + nuevoNombre);

        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error al dividir cuenta: " + e.getMessage());
        }
    }

    private void cargarProductos() {
        DefaultTableModel modelo = (DefaultTableModel) tabla.getModel();
        modelo.setRowCount(0); // Limpia la tabla

        try (Connection conn = MySQLConnection.getConnection()) {
            String sql = "SELECT p.nombre, dc.cantidad, dc.subtotal "
                    + "FROM detalle_cuenta dc "
                    + "JOIN productos p ON dc.producto_id = p.id "
                    + "WHERE dc.cuenta_id = ?";
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, cuentaId);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                String nombre = rs.getString("nombre");
                int cantidad = rs.getInt("cantidad");
                double subtotal = rs.getDouble("subtotal");
                modelo.addRow(new Object[]{nombre, cantidad, subtotal});
            }

        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error al cargar productos: " + e.getMessage());
        }
    }

    private void actualizarTotal() {
        double total = 0.0;
        DefaultTableModel modelo = (DefaultTableModel) tabla.getModel();

        for (int i = 0; i < modelo.getRowCount(); i++) {
            Object valorSubtotal = modelo.getValueAt(i, 3); // Columna 3 = Subtotal

            // Ignorar si está vacío o es un comentario
            if (valorSubtotal == null || valorSubtotal.toString().isBlank()) {
                continue;
            }

            try {
                double subtotal;

                if (valorSubtotal instanceof Double) {
                    subtotal = (Double) valorSubtotal;
                } else if (valorSubtotal instanceof Integer) {
                    subtotal = ((Integer) valorSubtotal).doubleValue();
                } else {
                    subtotal = Double.parseDouble(valorSubtotal.toString());
                }

                total += subtotal;
            } catch (NumberFormatException ex) {
                // Ignora filas con texto no numérico (como comentarios)
            }
        }

        lblTotal.setText("Total: $" + String.format("%.2f", total));
    }

    private int obtenerProductoIdPorNombre(String nombreProducto) {
        try (Connection conn = MySQLConnection.getConnection()) {
            String sql = "SELECT id FROM productos WHERE nombre = ?";
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setString(1, nombreProducto);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getInt("id");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return -1; // Si no se encuentra
    }
}
