package softarrecife.vista;

import softarrecife.conexion.MySQLConnection;
import softarrecife.utils.Estilos;
import softarrecife.utils.ImpresionComanda;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;
import java.sql.*;
import java.util.List;
import java.util.ArrayList;
import java.util.stream.Collectors;

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

        String[] categorias = {"Bebidas", "Comida", "Postres", "Extras"};
        for (String cat : categorias) {
            JButton btn = Estilos.crearBotonPrincipal(cat);
            btn.addActionListener(e -> mostrarSubcategorias(cat));
            panelCategorias.add(btn);
        }
        add(panelCategorias, BorderLayout.NORTH);

        panelCentral = new JPanel();
        cardLayout = new CardLayout();
        panelCentral.setLayout(cardLayout);
        panelCentral.setBackground(Color.WHITE);
        add(panelCentral, BorderLayout.CENTER);

        crearPanelSubcategorias();
    }

    private void crearPanelSubcategorias() {
        panelCentral.add(crearPanel("Bebidas", new String[]{
                "Litros preparados", "Cocteles en copa", "Shots", "Cerveza",
                "Refrescos", "Jugos naturales", "Smoothies", "Limonadas / Naranjadas"
        }), "Bebidas");

        panelCentral.add(crearPanel("Comida", new String[]{
                "Mariscos", "Antojitos / Tacos", "Hamburguesas", "Botanas", "Ceviches / Tostadas"
        }), "Comida");

        panelCentral.add(crearPanel("Postres", new String[]{
                "Pasteles", "Helados / Nieves", "Malteadas dulces"
        }), "Postres");

        panelCentral.add(crearPanel("Extras", new String[]{
                "Promociones", "Desayunos", "Eventos especiales"
        }), "Extras");
    }

    private JPanel crearPanel(String categoria, String[] subcategorias) {
        JPanel panel = new JPanel(new GridLayout(0, 3, 12, 12));
        panel.setBorder(new EmptyBorder(20, 20, 20, 20));
        panel.setBackground(Color.WHITE);

        for (String sub : subcategorias) {
            JButton btn = Estilos.crearBotonSecundario(sub);
            btn.addActionListener(e -> mostrarProductos(categoria, sub));
            panel.add(btn);
        }
        return panel;
    }

    private void mostrarSubcategorias(String categoria) {
        cardLayout.show(panelCentral, categoria);
    }

    private void mostrarProductos(String categoria, String subcategoria) {
        JDialog dialogoTipos = new JDialog(this, "Tipos en: " + subcategoria, true);
        dialogoTipos.setSize(600, 400);
        dialogoTipos.setLocationRelativeTo(this);
        dialogoTipos.setLayout(new GridLayout(0, 3, 10, 10));

        try (Connection conn = MySQLConnection.getConnection()) {
            String sql = "SELECT DISTINCT tipo FROM productos WHERE categoria_principal = ? AND subcategoria = ?";
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setString(1, categoria);
            ps.setString(2, subcategoria);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                String tipo = rs.getString("tipo");
                if (tipo == null || tipo.isBlank()) continue;

                JButton btn = Estilos.crearBotonSecundario(tipo);
                btn.addActionListener(e -> {
                    dialogoTipos.dispose();
                    mostrarProductosPorTipo(categoria, subcategoria, tipo);
                });
                dialogoTipos.add(btn);
            }

        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error al cargar tipos: " + e.getMessage());
        }

        dialogoTipos.setVisible(true);
    }

    private void mostrarProductosPorTipo(String categoria, String subcategoria, String tipo) {
        JDialog dialogoProductos = new JDialog(this, "Productos: " + tipo, true);
        dialogoProductos.setSize(800, 600);
        dialogoProductos.setLocationRelativeTo(this);
        dialogoProductos.setLayout(new GridLayout(0, 3, 10, 10));

        try (Connection conn = MySQLConnection.getConnection()) {
            String sql = "SELECT id, nombre, precio FROM productos WHERE categoria_principal = ? AND subcategoria = ? AND tipo = ?";
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setString(1, categoria);
            ps.setString(2, subcategoria);
            ps.setString(3, tipo);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                int idProd = rs.getInt("id");
                String nombre = rs.getString("nombre");
                double precio = rs.getDouble("precio");

                JButton btn = Estilos.crearBotonProducto(
                        nombre + "<br>$" + precio,
                        e -> abrirDialogoCantidadYComentario(idProd, precio, categoria, nombre)
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

        // Cantidad
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

        // Comentario
        JTextField txtComentario = new JTextField();
        txtComentario.setBorder(BorderFactory.createTitledBorder("Comentario (opcional)"));

        // Botón agregar
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
                        ps.setInt(3, 1); // siempre 1 por fila
                        ps.setDouble(4, precio); // precio unitario
                        ps.setString(5, comentario);
                        ps.executeUpdate();
                    }

// actualizar total general
                    String update = "UPDATE cuentas SET total = total + ? WHERE id = ?";
                    ps = conn.prepareStatement(update);
                    ps.setDouble(1, subtotal); // cantidad * precio
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
}
