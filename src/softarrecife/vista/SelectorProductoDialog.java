package softarrecife.vista;

import softarrecife.conexion.MySQLConnection;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.sql.*;

public class SelectorProductoDialog extends JDialog {
    private int cuentaId;
    private CuentaFrame cuentaFrame;
    private JPanel panelCentral;
    private CardLayout cardLayout;

    public SelectorProductoDialog(CuentaFrame cuentaFrame, int cuentaId) {
        super(cuentaFrame, "Seleccionar producto", true);
        this.cuentaId = cuentaId;
        this.cuentaFrame = cuentaFrame;
        setSize(800, 600);
        setLocationRelativeTo(cuentaFrame);
        setLayout(new BorderLayout());

        JPanel panelCategorias = new JPanel(new FlowLayout(FlowLayout.LEFT));
        String[] categorias = {"Bebidas", "Comida", "Postres", "Extras"};
        for (String cat : categorias) {
            JButton btn = new JButton(cat);
            btn.addActionListener(e -> mostrarSubcategorias(cat));
            panelCategorias.add(btn);
        }
        add(panelCategorias, BorderLayout.NORTH);

        panelCentral = new JPanel();
        cardLayout = new CardLayout();
        panelCentral.setLayout(cardLayout);
        add(panelCentral, BorderLayout.CENTER);

        crearPanelSubcategorias();
        setVisible(true);
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
        JPanel panel = new JPanel(new GridLayout(0, 3, 10, 10));
        for (String sub : subcategorias) {
            JButton btn = new JButton(sub);
            btn.addActionListener(e -> mostrarProductos(categoria, sub));
            panel.add(btn);
        }
        return panel;
    }

    private void mostrarSubcategorias(String categoria) {
        cardLayout.show(panelCentral, categoria);
    }

  private void mostrarProductos(String categoria, String subcategoria) {
    JDialog dialogoProductos = new JDialog(this, "Productos: " + subcategoria, true);
    dialogoProductos.setSize(600, 500);
    dialogoProductos.setLocationRelativeTo(this);
    dialogoProductos.setLayout(new FlowLayout(FlowLayout.LEFT, 15, 15));

    try (Connection conn = MySQLConnection.getConnection()) {
        String sql = "SELECT id, nombre, precio FROM productos WHERE categoria_principal = ? AND subcategoria = ?";
        PreparedStatement ps = conn.prepareStatement(sql);
        ps.setString(1, categoria);
        ps.setString(2, subcategoria);
        ResultSet rs = ps.executeQuery();

        while (rs.next()) {
            dialogoProductos.revalidate();
dialogoProductos.repaint();
            int idProd = rs.getInt("id");
            String nombre = rs.getString("nombre");
            double precio = rs.getDouble("precio");

            JButton btn = new JButton("<html><center>" + nombre + "<br>$" + precio + "</center></html>");
            btn.setPreferredSize(new Dimension(140, 70));
            btn.addActionListener(e -> {
                agregarProductoACuenta(idProd, precio);
                dialogoProductos.dispose(); // Cierra esta ventana
                this.dispose();             // Cierra el selector de subcategorías
            });

            dialogoProductos.add(btn);
        }

    } catch (SQLException e) {
        JOptionPane.showMessageDialog(this, "Error: " + e.getMessage());
    }

    dialogoProductos.setVisible(true);
}

    private void agregarProductoACuenta(int productoId, double precio) {
        try (Connection conn = MySQLConnection.getConnection()) {
            String insert = "INSERT INTO detalle_cuenta (cuenta_id, producto_id, cantidad, subtotal) VALUES (?, ?, 1, ?)";
            PreparedStatement ps = conn.prepareStatement(insert);
            ps.setInt(1, cuentaId);
            ps.setInt(2, productoId);
            ps.setDouble(3, precio);
            ps.executeUpdate();

            String update = "UPDATE cuentas SET total = total + ? WHERE id = ?";
            ps = conn.prepareStatement(update);
            ps.setDouble(1, precio);
            ps.setInt(2, cuentaId);
            ps.executeUpdate();

        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error al agregar: " + e.getMessage());
        }
    }
}
