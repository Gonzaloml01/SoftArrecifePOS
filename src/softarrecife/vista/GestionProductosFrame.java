package softarrecife.vista;

import softarrecife.conexion.MySQLConnection;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;

public class GestionProductosFrame extends JFrame {
    private JTable tabla;
    private DefaultTableModel modelo;

    private JTextField txtNombre;
    private JTextField txtPrecio;
    private JComboBox<String> comboCategoria;
    private JComboBox<String> comboSubcategoria;

    public GestionProductosFrame() {
        setTitle("Gestión de Productos");
        setSize(800, 600);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        modelo = new DefaultTableModel();
        modelo.addColumn("ID");
        modelo.addColumn("Nombre");
        modelo.addColumn("Precio");
        modelo.addColumn("Categoría");
        modelo.addColumn("Subcategoría");

        tabla = new JTable(modelo);
        JScrollPane scroll = new JScrollPane(tabla);
        add(scroll, BorderLayout.CENTER);

        JPanel panelFormulario = new JPanel(new GridLayout(3, 4, 10, 10));
        txtNombre = new JTextField();
        txtPrecio = new JTextField();

        comboCategoria = new JComboBox<>(new String[]{"Bebidas", "Comida", "Postres", "Extras"});
        comboSubcategoria = new JComboBox<>();

        comboCategoria.addActionListener(e -> cargarSubcategorias());

        panelFormulario.add(new JLabel("Nombre:"));
        panelFormulario.add(txtNombre);
        panelFormulario.add(new JLabel("Precio:"));
        panelFormulario.add(txtPrecio);

        panelFormulario.add(new JLabel("Categoría:"));
        panelFormulario.add(comboCategoria);
        panelFormulario.add(new JLabel("Subcategoría:"));
        panelFormulario.add(comboSubcategoria);

        JButton btnAgregar = new JButton("Agregar");
        btnAgregar.addActionListener(e -> agregarProducto());
        panelFormulario.add(btnAgregar);

        JButton btnEliminar = new JButton("Eliminar");
        btnEliminar.addActionListener(e -> eliminarProducto());
        panelFormulario.add(btnEliminar);

        add(panelFormulario, BorderLayout.SOUTH);

        cargarSubcategorias();
        cargarProductos();

        setVisible(true);
    }

    private void cargarSubcategorias() {
        comboSubcategoria.removeAllItems();
        String cat = (String) comboCategoria.getSelectedItem();
        if (cat == null) return;

        switch (cat) {
            case "Bebidas" -> comboSubcategoria.addItem("Litros preparados");
            case "Comida" -> comboSubcategoria.addItem("Mariscos");
            case "Postres" -> comboSubcategoria.addItem("Pasteles");
            case "Extras" -> comboSubcategoria.addItem("Promociones");
        }

        // Puedes añadir más subcategorías aquí...
    }

    private void cargarProductos() {
        modelo.setRowCount(0);
        try (Connection conn = MySQLConnection.getConnection()) {
            String sql = "SELECT * FROM productos";
            PreparedStatement ps = conn.prepareStatement(sql);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                modelo.addRow(new Object[]{
                        rs.getInt("id"),
                        rs.getString("nombre"),
                        rs.getDouble("precio"),
                        rs.getString("categoria_principal"),
                        rs.getString("subcategoria")
                });
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error al cargar: " + e.getMessage());
        }
    }

    private void agregarProducto() {
        String nombre = txtNombre.getText().trim();
        String precioTxt = txtPrecio.getText().trim();
        String categoria = (String) comboCategoria.getSelectedItem();
        String subcategoria = (String) comboSubcategoria.getSelectedItem();

        if (nombre.isEmpty() || precioTxt.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Faltan datos.");
            return;
        }

        try {
            double precio = Double.parseDouble(precioTxt);
            try (Connection conn = MySQLConnection.getConnection()) {
                String sql = "INSERT INTO productos (nombre, precio, categoria_principal, subcategoria) VALUES (?, ?, ?, ?)";
                PreparedStatement ps = conn.prepareStatement(sql);
                ps.setString(1, nombre);
                ps.setDouble(2, precio);
                ps.setString(3, categoria);
                ps.setString(4, subcategoria);
                ps.executeUpdate();
                cargarProductos();
                limpiarCampos();
            }
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Precio inválido.");
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error al guardar: " + e.getMessage());
        }
    }

    private void eliminarProducto() {
        int fila = tabla.getSelectedRow();
        if (fila == -1) {
            JOptionPane.showMessageDialog(this, "Selecciona un producto.");
            return;
        }

        int id = (int) modelo.getValueAt(fila, 0);
        try (Connection conn = MySQLConnection.getConnection()) {
            String sql = "DELETE FROM productos WHERE id = ?";
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, id);
            ps.executeUpdate();
            cargarProductos();
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error al eliminar: " + e.getMessage());
        }
    }

    private void limpiarCampos() {
        txtNombre.setText("");
        txtPrecio.setText("");
        comboCategoria.setSelectedIndex(0);
        cargarSubcategorias();
    }
}
