package softarrecife.vista;

import softarrecife.conexion.MySQLConnection;
import softarrecife.utils.Estilos;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class SelectorProductoDialog extends JDialog {

    private int cuentaId;
    private CuentaFrame cuentaFrame;
    private JPanel panelCentral;
    private CardLayout cardLayout;
    private List<ProductoSeleccionado> productosSeleccionados = new ArrayList<>();

    public SelectorProductoDialog(CuentaFrame cuentaFrame, int cuentaId) {
        super(cuentaFrame, "Seleccionar producto", true);
        this.cuentaId = cuentaId;
        this.cuentaFrame = cuentaFrame;

        setSize(800, 600);
        setLocationRelativeTo(cuentaFrame);
        setLayout(new BorderLayout());
        getContentPane().setBackground(Estilos.grisClaro);

        JPanel panelCategorias = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        panelCategorias.setBorder(new EmptyBorder(10, 10, 10, 10));
        panelCategorias.setBackground(Estilos.grisClaro);

        panelCentral = new JPanel();
        cardLayout = new CardLayout();
        panelCentral.setLayout(cardLayout);
        panelCentral.setBackground(Color.WHITE);

        cargarCategorias(panelCategorias);

        add(panelCategorias, BorderLayout.NORTH);
        add(panelCentral, BorderLayout.CENTER);
    }

    private void cargarCategorias(JPanel panelCategorias) {
        try (Connection conn = MySQLConnection.getConnection()) {
            String sql = "SELECT id, nombre FROM categorias";
            PreparedStatement ps = conn.prepareStatement(sql);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                int categoriaId = rs.getInt("id");
                String nombreCategoria = rs.getString("nombre");

                JButton btn = Estilos.crearBotonPrincipal(nombreCategoria);
                btn.addActionListener(e -> mostrarSubcategorias(nombreCategoria));
                panelCategorias.add(btn);

                JPanel subPanel = crearPanelSubcategorias(categoriaId, nombreCategoria);
                panelCentral.add(subPanel, nombreCategoria);
            }

        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error al cargar categorías: " + e.getMessage());
        }
    }

    private JPanel crearPanelSubcategorias(int categoriaId, String nombreCategoria) {
        JPanel panel = new JPanel(new GridLayout(0, 3, 12, 12));
        panel.setBorder(new EmptyBorder(20, 20, 20, 20));
        panel.setBackground(Color.WHITE);

        try (Connection conn = MySQLConnection.getConnection()) {
            String sql = "SELECT id, nombre FROM subcategorias WHERE categoria_id = ?";
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, categoriaId);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                int subId = rs.getInt("id");
                String subNombre = rs.getString("nombre");

                JButton btn = Estilos.crearBotonSecundario(subNombre);
                btn.addActionListener(e -> {
                    try (Connection c = MySQLConnection.getConnection()) {
                        String check = "SELECT COUNT(*) FROM subsubcategorias WHERE subcategoria_id = ?";
                        PreparedStatement ps2 = c.prepareStatement(check);
                        ps2.setInt(1, subId);
                        ResultSet rs2 = ps2.executeQuery();
                        if (rs2.next() && rs2.getInt(1) > 0) {
                            mostrarSubsubcategorias(subId, subNombre);
                        } else {
                            mostrarProductosPorSubcategoria(subId, subNombre);
                        }
                    } catch (SQLException ex) {
                        JOptionPane.showMessageDialog(this, "Error al verificar subsubcategorias: " + ex.getMessage());
                    }
                });
                panel.add(btn);
            }

        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error al cargar subcategorías: " + e.getMessage());
        }

        return panel;
    }

    private void mostrarSubcategorias(String nombreCategoria) {
        try (Connection conn = MySQLConnection.getConnection()) {
            String getIdSql = "SELECT id FROM categorias WHERE nombre = ?";
            PreparedStatement getIdPs = conn.prepareStatement(getIdSql);
            getIdPs.setString(1, nombreCategoria);
            ResultSet idRs = getIdPs.executeQuery();

            if (idRs.next()) {
                int categoriaId = idRs.getInt("id");

                JPanel nuevoPanel = crearPanelSubcategorias(categoriaId, nombreCategoria);
                panelCentral.add(nuevoPanel, nombreCategoria);
                cardLayout.show(panelCentral, nombreCategoria);
            }

        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error al mostrar subcategorías: " + e.getMessage());
        }
    }

    private void mostrarSubsubcategorias(int subcategoriaId, String nombreSubcategoria) {
        try (Connection conn = MySQLConnection.getConnection()) {
            String sql = "SELECT id, nombre FROM subsubcategorias WHERE subcategoria_id = ?";
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, subcategoriaId);
            ResultSet rs = ps.executeQuery();

            List<JButton> botones = new ArrayList<>();
            JDialog dialogoSubsub = new JDialog(this, "Seleccionar tipo en: " + nombreSubcategoria, true);
            dialogoSubsub.setSize(600, 400);
            dialogoSubsub.setLocationRelativeTo(this);
            dialogoSubsub.setLayout(new GridLayout(0, 3, 10, 10));

            while (rs.next()) {
                int subsubId = rs.getInt("id");
                String subsubNombre = rs.getString("nombre");

                JButton btn = Estilos.crearBotonSecundario(subsubNombre);
                btn.addActionListener(e -> {
                    dialogoSubsub.dispose();
                    mostrarProductosPorSubsubcategoria(subsubId, subsubNombre);
                });

                botones.add(btn);
                dialogoSubsub.add(btn);
            }

            if (botones.isEmpty()) {
                // No hay subsubcategorías, mostrar productos directamente
                mostrarProductosPorSubcategoria(subcategoriaId, nombreSubcategoria);
            } else {
                dialogoSubsub.setVisible(true);
            }

        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error al cargar sub-subcategorías: " + e.getMessage());
        }
    }

    private void mostrarProductosPorSubsubcategoria(int subsubId, String titulo) {
        JDialog dialogoProductos = new JDialog(this, "Productos: " + titulo, true);
        dialogoProductos.setSize(800, 600);
        dialogoProductos.setLocationRelativeTo(this);
        dialogoProductos.setLayout(new GridLayout(0, 3, 10, 10));

        try (Connection conn = MySQLConnection.getConnection()) {
            String sql = "SELECT id, nombre, precio FROM productos WHERE subsubcategoria_id = ?";
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, subsubId);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                int idProd = rs.getInt("id");
                String nombre = rs.getString("nombre");
                double precio = rs.getDouble("precio");

                JButton btn = Estilos.crearBotonProducto(
                        nombre + "<br>$" + precio,
                        e -> abrirDialogoCantidadYComentario(idProd, precio, titulo, nombre)
                );

                dialogoProductos.add(btn);
            }

        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error: " + e.getMessage());
        }

        dialogoProductos.setVisible(true);
    }

    private void abrirDialogoCantidadYComentario(int productoId, double precio, String categoria, String nombreProducto) {
        JDialog dialog = new JDialog(this, nombreProducto, true);
        dialog.setSize(350, 250);
        dialog.setLayout(new BorderLayout());
        dialog.setLocationRelativeTo(this);

        JPanel panel = new JPanel(new GridLayout(3, 1, 10, 10));
        panel.setBorder(new EmptyBorder(20, 20, 20, 20));

        JPanel cantidadPanel = new JPanel(new FlowLayout());
        JLabel lblCantidad = new JLabel("Cantidad:");
        JTextField txtCantidad = new JTextField("1", 3);
        JButton btnMenos = new JButton("-");
        JButton btnMas = new JButton("+");

        btnMenos.addActionListener(e -> {
            int actual = Integer.parseInt(txtCantidad.getText());
            if (actual > 1) {
                txtCantidad.setText(String.valueOf(actual - 1));
            }
        });

        btnMas.addActionListener(e -> {
            int actual = Integer.parseInt(txtCantidad.getText());
            txtCantidad.setText(String.valueOf(actual + 1));
        });

        cantidadPanel.add(lblCantidad);
        cantidadPanel.add(btnMenos);
        cantidadPanel.add(txtCantidad);
        cantidadPanel.add(btnMas);

        JTextField txtComentario = new JTextField();
        txtComentario.setBorder(BorderFactory.createTitledBorder("Comentario (opcional)"));

        JButton btnAgregar = new JButton("Agregar a cuenta");
        btnAgregar.addActionListener(e -> {
            try {
                int cantidad = Integer.parseInt(txtCantidad.getText());
                double subtotal = cantidad * precio;
                String comentario = txtComentario.getText();

                try (Connection conn = MySQLConnection.getConnection()) {
                    String insert = "INSERT INTO detalle_cuenta (cuenta_id, producto_id, cantidad, subtotal, comentario) VALUES (?, ?, ?, ?, ?)";
                    PreparedStatement ps = conn.prepareStatement(insert);

                    for (int i = 0; i < cantidad; i++) {
                        ps.setInt(1, cuentaId);
                        ps.setInt(2, productoId);
                        ps.setInt(3, 1);
                        ps.setDouble(4, precio);
                        ps.setString(5, comentario);
                        ps.executeUpdate();
                    }

                    String update = "UPDATE cuentas SET total = total + ? WHERE id = ?";
                    ps = conn.prepareStatement(update);
                    ps.setDouble(1, subtotal);
                    ps.setInt(2, cuentaId);
                    ps.executeUpdate();

                    productosSeleccionados.add(new ProductoSeleccionado(nombreProducto + " x" + cantidad, categoria));
                }
                dialog.dispose();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(dialog, "Error al agregar: " + ex.getMessage());
            }
        });

        panel.add(cantidadPanel);
        panel.add(txtComentario);
        panel.add(btnAgregar);

        dialog.add(panel, BorderLayout.CENTER);
        dialog.setVisible(true);
    }

    private static class ProductoSeleccionado {

        String nombre;
        String categoria;

        public ProductoSeleccionado(String nombre, String categoria) {
            this.nombre = nombre;
            this.categoria = categoria;
        }
    }

    private void mostrarProductosPorSubcategoria(int subId, String titulo) {
        JDialog dialogoProductos = new JDialog(this, "Productos: " + titulo, true);
        dialogoProductos.setSize(800, 600);
        dialogoProductos.setLocationRelativeTo(this);
        dialogoProductos.setLayout(new GridLayout(0, 3, 10, 10));

        try (Connection conn = MySQLConnection.getConnection()) {
            String sql = "SELECT id, nombre, precio FROM productos WHERE subcategoria_id = ?";
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, subId);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                int idProd = rs.getInt("id");
                String nombre = rs.getString("nombre");
                double precio = rs.getDouble("precio");

                JButton btn = Estilos.crearBotonProducto(
                        nombre + "<br>$" + precio,
                        e -> abrirDialogoCantidadYComentario(idProd, precio, titulo, nombre)
                );

                dialogoProductos.add(btn);
            }

        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error al cargar productos: " + e.getMessage());
        }

        dialogoProductos.setVisible(true);
    }
}
