package softarrecife.vista;

import softarrecife.conexion.MySQLConnection;
import softarrecife.utils.Estilos;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;

public class GestionProductosFrame extends JFrame {

    private JTable tabla;
    private DefaultTableModel modelo;

    private JTextField txtNombre;
    private JTextField txtPrecio;
    private JComboBox<CategoriaItem> comboCategoria;
    private JComboBox<SubcategoriaItem> comboSubcategoria;

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

        comboCategoria = new JComboBox<>();
        comboCategoria.setFont(Estilos.fuenteNormal);
        comboCategoria.addActionListener(e -> cargarSubcategorias());

        comboSubcategoria = new JComboBox<>();
        comboSubcategoria.setFont(Estilos.fuenteNormal);

        JButton btnAgregar = Estilos.crearBotonModerno("Agregar", null);
        btnAgregar.addActionListener(e -> agregarProducto());

        JButton btnEliminar = Estilos.crearBotonModerno("Eliminar", null);
        btnEliminar.addActionListener(e -> eliminarProducto());

        gbc.gridx = 0; gbc.gridy = 0;
        panelFormulario.add(new JLabel("Nombre:"), gbc);
        gbc.gridx = 1;
        panelFormulario.add(txtNombre, gbc);

        gbc.gridx = 2;
        panelFormulario.add(new JLabel("Precio:"), gbc);
        gbc.gridx = 3;
        panelFormulario.add(txtPrecio, gbc);

        gbc.gridx = 0; gbc.gridy = 1;
        panelFormulario.add(new JLabel("Categoría:"), gbc);
        gbc.gridx = 1;
        panelFormulario.add(comboCategoria, gbc);

        gbc.gridx = 2;
        panelFormulario.add(new JLabel("Subcategoría:"), gbc);
        gbc.gridx = 3;
        panelFormulario.add(comboSubcategoria, gbc);

        gbc.gridx = 1; gbc.gridy = 2;
        panelFormulario.add(btnAgregar, gbc);
        gbc.gridx = 2;
        panelFormulario.add(btnEliminar, gbc);

        add(panelFormulario, BorderLayout.SOUTH);

        cargarCategorias();
        cargarProductos();

        setVisible(true);
    }

    private void cargarCategorias() {
        comboCategoria.removeAllItems();
        try (Connection conn = MySQLConnection.getConnection()) {
            String sql = "SELECT id, nombre FROM categorias";
            PreparedStatement ps = conn.prepareStatement(sql);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                comboCategoria.addItem(new CategoriaItem(rs.getInt("id"), rs.getString("nombre")));
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error al cargar categorías: " + e.getMessage());
        }
    }

    private void cargarSubcategorias() {
        comboSubcategoria.removeAllItems();
        CategoriaItem categoria = (CategoriaItem) comboCategoria.getSelectedItem();
        if (categoria == null) return;

        try (Connection conn = MySQLConnection.getConnection()) {
            String sql = "SELECT id, nombre FROM subcategorias WHERE categoria_id = ?";
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, categoria.getId());
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                comboSubcategoria.addItem(new SubcategoriaItem(rs.getInt("id"), rs.getString("nombre")));
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error al cargar subcategorías: " + e.getMessage());
        }
    }

    private void agregarProducto() {
        String nombre = txtNombre.getText().trim();
        String precioTxt = txtPrecio.getText().trim();
        CategoriaItem categoria = (CategoriaItem) comboCategoria.getSelectedItem();
        SubcategoriaItem subcategoria = (SubcategoriaItem) comboSubcategoria.getSelectedItem();

        if (nombre.isEmpty() || precioTxt.isEmpty() || categoria == null || subcategoria == null) {
            JOptionPane.showMessageDialog(this, "Faltan datos.");
            return;
        }

        try {
            double precio = Double.parseDouble(precioTxt);
            try (Connection conn = MySQLConnection.getConnection()) {
                String sql = "INSERT INTO productos (nombre, precio, categoria_id, subcategoria_id) VALUES (?, ?, ?, ?)";
                PreparedStatement ps = conn.prepareStatement(sql);
                ps.setString(1, nombre);
                ps.setDouble(2, precio);
                ps.setInt(3, categoria.getId());
                ps.setInt(4, subcategoria.getId());
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

    private void cargarProductos() {
        modelo.setRowCount(0);
        try (Connection conn = MySQLConnection.getConnection()) {
            String sql = """
                SELECT p.id, p.nombre, p.precio, c.nombre AS categoria, s.nombre AS subcategoria
                FROM productos p
                JOIN categorias c ON p.categoria_id = c.id
                JOIN subcategorias s ON p.subcategoria_id = s.id
            """;
            PreparedStatement ps = conn.prepareStatement(sql);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                modelo.addRow(new Object[]{
                    rs.getInt("id"),
                    rs.getString("nombre"),
                    rs.getDouble("precio"),
                    rs.getString("categoria"),
                    rs.getString("subcategoria")
                });
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error al cargar productos: " + e.getMessage());
        }
    }

    private void limpiarCampos() {
        txtNombre.setText("");
        txtPrecio.setText("");
        comboCategoria.setSelectedIndex(0);
        cargarSubcategorias();
    }
}

class CategoriaItem {
    int id;
    String nombre;

    CategoriaItem(int id, String nombre) {
        this.id = id;
        this.nombre = nombre;
    }

    public String toString() {
        return nombre;
    }

    public int getId() {
        return id;
    }
}

class SubcategoriaItem {
    int id;
    String nombre;

    SubcategoriaItem(int id, String nombre) {
        this.id = id;
        this.nombre = nombre;
    }

    public String toString() {
        return nombre;
    }

    public int getId() {
        return id;
    }
}
