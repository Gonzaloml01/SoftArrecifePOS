package softarrecife.vista;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;
import softarrecife.conexion.MySQLConnection;

public class gastos extends JFrame {

    private JTextField txtDescripcion, txtMonto, txtCategoria;
    private JTable tablaGastos;
    private DefaultTableModel modeloTabla;
    private int usuarioId;
    private String nombreUsuario;

    public gastos(int usuarioId, String nombreUsuario) {
        this.usuarioId = usuarioId;
        this.nombreUsuario = nombreUsuario;

        setTitle("Registro de Gastos");
        setSize(700, 450);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout());

        JPanel panelFormulario = new JPanel(new GridLayout(4, 2, 10, 10));
        panelFormulario.setBorder(BorderFactory.createTitledBorder("Nuevo Gasto"));

        txtDescripcion = new JTextField();
        txtMonto = new JTextField();
        txtCategoria = new JTextField();

        panelFormulario.add(new JLabel("Descripción:"));
        panelFormulario.add(txtDescripcion);
        panelFormulario.add(new JLabel("Monto:"));
        panelFormulario.add(txtMonto);
        panelFormulario.add(new JLabel("Categoría:"));
        panelFormulario.add(txtCategoria);

        JButton btnRegistrar = new JButton("Registrar Gasto");
        panelFormulario.add(new JLabel()); // Espacio vacío
        panelFormulario.add(btnRegistrar);

        add(panelFormulario, BorderLayout.NORTH);

        modeloTabla = new DefaultTableModel(new String[]{"Descripción", "Monto", "Categoría", "Fecha", "Turno ID"}, 0);
        tablaGastos = new JTable(modeloTabla);
        add(new JScrollPane(tablaGastos), BorderLayout.CENTER);

        btnRegistrar.addActionListener(e -> registrarGasto());

        cargarGastos();
    }

    private void registrarGasto() {
        String descripcion = txtDescripcion.getText().trim();
        String montoTexto = txtMonto.getText().trim();
        String categoria = txtCategoria.getText().trim();

        if (descripcion.isEmpty() || montoTexto.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Completa todos los campos obligatorios.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        double monto;
        try {
            monto = Double.parseDouble(montoTexto);
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Monto inválido.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        int turnoId = obtenerTurnoActivoId();
        if (turnoId == -1) {
            JOptionPane.showMessageDialog(this, "No hay un turno activo. No se puede registrar el gasto.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        try (Connection conn = MySQLConnection.getConnection()) {
            String sql = "INSERT INTO gastos (descripcion, monto, categoria, fecha, registrado_por, turno_id) VALUES (?, ?, ?, CURDATE(), ?, ?)";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, descripcion);
            stmt.setDouble(2, monto);
            stmt.setString(3, categoria);
            stmt.setString(4, nombreUsuario);
            stmt.setInt(5, turnoId);
            stmt.executeUpdate();

            JOptionPane.showMessageDialog(this, "Gasto registrado correctamente.");
            cargarGastos();
            txtDescripcion.setText("");
            txtMonto.setText("");
            txtCategoria.setText("");
        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error al registrar el gasto.", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private int obtenerTurnoActivoId() {
        try (Connection conn = MySQLConnection.getConnection()) {
            String sql = "SELECT id FROM turnos WHERE cerrado = 0 AND usuario_id = ?";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setInt(1, usuarioId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getInt("id");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return -1; // No hay turno abierto
    }

    private void cargarGastos() {
        modeloTabla.setRowCount(0);
        try (Connection conn = MySQLConnection.getConnection()) {
            String sql = "SELECT descripcion, monto, categoria, fecha, turno_id FROM gastos ORDER BY fecha DESC";
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(sql);
            while (rs.next()) {
                modeloTabla.addRow(new Object[]{
                    rs.getString("descripcion"),
                    rs.getDouble("monto"),
                    rs.getString("categoria"),
                    rs.getDate("fecha"),
                    rs.getInt("turno_id")
                });
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
