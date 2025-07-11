package softarrecife.vista;

import softarrecife.conexion.MySQLConnection;
import softarrecife.utils.WrapLayout;
import softarrecife.utils.Estilos;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ComedorFrame extends JFrame {

    private JPanel panelMesas;
    private int usuarioId;
    private String nombreMesero;
    private String tipoUsuario;

    private boolean modoEliminarActivo = false;
    private boolean modoRenombrarActivo = false;

    private final List<JButton> botonesSeleccionados = new ArrayList<>();
    private final List<Integer> idsSeleccionados = new ArrayList<>();

    public ComedorFrame(int usuarioId, String nombreMesero, String tipoUsuario) {
        this.usuarioId = usuarioId;
        this.nombreMesero = nombreMesero;
        this.tipoUsuario = tipoUsuario;

        setTitle("Summa POS - Comedor de " + nombreMesero);
        setSize(900, 600);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout());

        // COLORES
        Color fondo = new Color(245, 245, 245);
        Color azulSuave = new Color(100, 149, 237); // azul cornflower
        Color rojoSuave = new Color(255, 160, 160); // rojo pastel suave

        // ========== PANEL HEADER (Título y línea) ==========
        JPanel panelHeader = new JPanel(new BorderLayout());
        panelHeader.setBackground(new Color(240, 240, 240));

        JLabel lblTitulo = new JLabel("Comedor de Gonzalo", SwingConstants.CENTER);
        lblTitulo.setFont(new Font("SansSerif", Font.BOLD, 20));
        lblTitulo.setForeground(Color.DARK_GRAY);
        lblTitulo.setBorder(BorderFactory.createEmptyBorder(0, 0, 4, 0));

        JPanel lineaSeparadora = new JPanel();
        lineaSeparadora.setBackground(new Color(180, 180, 180));
        lineaSeparadora.setPreferredSize(new Dimension(1, 2));

        panelHeader.add(lblTitulo, BorderLayout.CENTER);
        panelHeader.add(lineaSeparadora, BorderLayout.SOUTH);

        // ========== PANEL BOTONES ==========
        JPanel panelBotones = new JPanel(new FlowLayout(FlowLayout.LEFT));
        panelBotones.setBackground(fondo);

        JButton btnAgregar = Estilos.crearBotonModernoFijo("➕ Agregar mesa", azulSuave);
        JButton btnRenombrarMesa = Estilos.crearBotonModernoFijo("➖ Renombrar Mesa", azulSuave);
        JButton btnCerrarMesa = Estilos.crearBotonModernoFijo("❌ Cerrar Mesa", rojoSuave);

        panelBotones.add(btnAgregar);
        panelBotones.add(btnRenombrarMesa);
        panelBotones.add(btnCerrarMesa);

        // ========== PANEL MESAS ==========
        panelMesas = new JPanel(new WrapLayout(FlowLayout.LEFT, 12, 12));
        panelMesas.setBackground(fondo);

        JScrollPane scroll = new JScrollPane(panelMesas);
        scroll.setBorder(BorderFactory.createEmptyBorder());

        // ========== PANEL CONTENIDO CENTRAL (Botones + mesas) ==========
        JPanel panelCentral = new JPanel(new BorderLayout());
        panelCentral.setBackground(fondo);
        panelCentral.add(panelBotones, BorderLayout.NORTH);
        panelCentral.add(scroll, BorderLayout.CENTER);

        // ========== AGREGAR TODO ==========
        add(panelHeader, BorderLayout.NORTH);
        add(panelCentral, BorderLayout.CENTER);

        // ========== EVENTOS ==========
        btnAgregar.addActionListener(e -> agregarMesa());

        btnRenombrarMesa.addActionListener(e -> {
            modoEliminarActivo = false;
            modoRenombrarActivo = true;
            limpiarSeleccion();
            JOptionPane.showMessageDialog(this, "Toca la mesa que deseas renombrar.");
        });

        btnCerrarMesa.addActionListener(e -> {
            modoEliminarActivo = true;
            modoRenombrarActivo = false;
            limpiarSeleccion();
            JOptionPane.showMessageDialog(this, "Selecciona las mesas a cerrar tocándolas.");
        });

        // Cargar las mesas
        cargarMesas();

        // Mostrar ventana
        setVisible(true);
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
        botonesSeleccionados.clear();
        idsSeleccionados.clear();

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

                mesaBtn.addActionListener(e -> {
                    if (modoEliminarActivo) {
                        toggleSeleccion(mesaBtn, id);
                    } else if (modoRenombrarActivo) {
                        renombrarMesa(id, nombre);
                        modoRenombrarActivo = false;
                        limpiarSeleccion();
                    } else {
                        SwingUtilities.invokeLater(() -> new CuentaFrame(id, nombre, ComedorFrame.this));
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

    private void toggleSeleccion(JButton boton, int idMesa) {
        if (idsSeleccionados.contains(idMesa)) {
            idsSeleccionados.remove(Integer.valueOf(idMesa));
            botonesSeleccionados.remove(boton);
            boton.setBackground(Color.WHITE);
        } else {
            idsSeleccionados.add(idMesa);
            botonesSeleccionados.add(boton);
            boton.setBackground(new Color(255, 100, 100));
        }

        if (!idsSeleccionados.isEmpty()) {
            int confirm = JOptionPane.showConfirmDialog(this, "¿Deseas cerrar las mesas seleccionadas?", "Confirmar", JOptionPane.YES_NO_OPTION);
            if (confirm == JOptionPane.YES_OPTION) {
                for (int id : idsSeleccionados) {
                    verificarCierreDesdeContextual(id);
                }
                modoEliminarActivo = false;
                limpiarSeleccion();
                cargarMesas();
            }
        }
    }

    private void limpiarSeleccion() {
        for (JButton btn : botonesSeleccionados) {
            btn.setBackground(Color.WHITE);
        }
        botonesSeleccionados.clear();
        idsSeleccionados.clear();
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

                String updateCuenta = "UPDATE cuentas SET estado = 'cerrada' WHERE id = ?";
                PreparedStatement psCerrarCuenta = conn.prepareStatement(updateCuenta);
                psCerrarCuenta.setInt(1, cuentaId);
                psCerrarCuenta.executeUpdate();

                String liberarMesa = "UPDATE mesas SET estado = 'libre' WHERE id = ?";
                PreparedStatement psLiberarMesa = conn.prepareStatement(liberarMesa);
                psLiberarMesa.setInt(1, mesaId);
                psLiberarMesa.executeUpdate();

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
