package softarrecife.vista;

import softarrecife.conexion.MySQLConnection;
import softarrecife.utils.Estilos;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;
import javax.swing.border.EmptyBorder;

public class GestionProductosFrame extends JFrame {

    private JTable tabla;
    private DefaultTableModel modelo;

    private JTextField txtNombre;
    private JTextField txtPrecio;
    private JComboBox<String> comboCategoria;
    private JComboBox<String> comboSubcategoria;

    public GestionProductosFrame() {
        setTitle("Gestión de Productos");
        setSize(900, 600);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        modelo = new DefaultTableModel();
        modelo.addColumn("ID");
        modelo.addColumn("Nombre");
        modelo.addColumn("Precio");
        modelo.addColumn("Categoría");
        modelo.addColumn("Subcategoría");

        tabla = new JTable(modelo);
        Estilos.estilizarTabla(tabla);
        JScrollPane scroll = new JScrollPane(tabla);
        add(scroll, BorderLayout.CENTER);

        JPanel panelFormulario = new JPanel(new GridBagLayout());
        panelFormulario.setBackground(Color.WHITE);
        panelFormulario.setBorder(new EmptyBorder(20, 20, 20, 20));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        txtNombre = new JTextField(15);
        Estilos.estilizarCampo(txtNombre, "Nombre");

        txtPrecio = new JTextField(10);
        Estilos.estilizarCampo(txtPrecio, "Precio");

        comboCategoria = new JComboBox<>(new String[]{"Bebidas", "Comida", "Postres", "Extras"});
        comboCategoria.setFont(Estilos.fuenteNormal);
        comboSubcategoria = new JComboBox<>();
        comboSubcategoria.setFont(Estilos.fuenteNormal);

        comboCategoria.addActionListener(e -> cargarSubcategorias());

        JButton btnAgregar = Estilos.crearBotonModerno("Agregar", null);
        btnAgregar.addActionListener(e -> agregarProducto());

        JButton btnEliminar = Estilos.crearBotonModerno("Eliminar", null);
        btnEliminar.addActionListener(e -> eliminarProducto());

        // Fila 1
        gbc.gridx = 0;
        gbc.gridy = 0;
        JLabel lblNombre = new JLabel("Nombre:");
        Estilos.estilizarLabel(lblNombre);
        panelFormulario.add(lblNombre, gbc);

        gbc.gridx = 1;
        panelFormulario.add(txtNombre, gbc);

        gbc.gridx = 2;
        JLabel lblPrecio = new JLabel("Precio:");
        Estilos.estilizarLabel(lblPrecio);
        panelFormulario.add(lblPrecio, gbc);

        gbc.gridx = 3;
        panelFormulario.add(txtPrecio, gbc);

// Fila 2
        gbc.gridx = 0;
        gbc.gridy = 1;
        JLabel lblCategoria = new JLabel("Categoría:");
        Estilos.estilizarLabel(lblCategoria);
        panelFormulario.add(lblCategoria, gbc);

        gbc.gridx = 1;
        panelFormulario.add(comboCategoria, gbc);

        gbc.gridx = 2;
        JLabel lblSubcategoria = new JLabel("Subcategoría:");
        Estilos.estilizarLabel(lblSubcategoria);
        panelFormulario.add(lblSubcategoria, gbc);

        gbc.gridx = 3;
        panelFormulario.add(comboSubcategoria, gbc);

        // Fila 3
        gbc.gridx = 1;
        gbc.gridy = 2;
        panelFormulario.add(btnAgregar, gbc);
        gbc.gridx = 2;
        panelFormulario.add(btnEliminar, gbc);

        add(panelFormulario, BorderLayout.SOUTH);

        cargarSubcategorias();
        cargarProductos();

        setVisible(true);
    }

    private void cargarSubcategorias() {
        comboSubcategoria.removeAllItems();
        String cat = (String) comboCategoria.getSelectedItem();
        if (cat == null) {
            return;
        }

        switch (cat) {
            case "Bebidas" ->
                comboSubcategoria.addItem("Litros preparados");
            case "Comida" ->
                comboSubcategoria.addItem("Mariscos");
            case "Postres" ->
                comboSubcategoria.addItem("Pasteles");
            case "Extras" ->
                comboSubcategoria.addItem("Promociones");
        }
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
