// ComedorFrame.java - Vista principal de mesas con botones de gestión

package softarrecife.vista;

import softarrecife.conexion.MySQLConnection;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.sql.*;

public class ComedorFrame extends JFrame {
    private JPanel panelMesas;

    public ComedorFrame() {
        setTitle("Comedor");
        setSize(800, 600);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout());

        // Panel superior con botones
        JPanel panelBotones = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton btnAgregar = new JButton("➕ Agregar mesa");
        btnAgregar.addActionListener(e -> agregarMesa());
        panelBotones.add(btnAgregar);

        add(panelBotones, BorderLayout.NORTH);

        // Panel de mesas
        panelMesas = new JPanel();
        panelMesas.setLayout(new FlowLayout(FlowLayout.LEFT, 10, 10));

        JScrollPane scroll = new JScrollPane(panelMesas);
        add(scroll, BorderLayout.CENTER);

        cargarMesas();

        setVisible(true);
    }

    public void cargarMesas() {
        panelMesas.removeAll();

        try (Connection conn = MySQLConnection.getConnection()) {
            String sql = "SELECT * FROM mesas WHERE estado = 'ocupada'";
            PreparedStatement ps = conn.prepareStatement(sql);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                int id = rs.getInt("id");
                String nombre = rs.getString("nombre");

                JButton mesaBtn = new JButton(nombre);
                mesaBtn.setPreferredSize(new Dimension(120, 80));

                mesaBtn.addMouseListener(new MouseAdapter() {
                    @Override
                    public void mousePressed(MouseEvent e) {
                        if (SwingUtilities.isRightMouseButton(e)) {
                            JPopupMenu menu = crearMenuContextual(id, nombre);
                            menu.show(e.getComponent(), e.getX(), e.getY());
                        } else if (SwingUtilities.isLeftMouseButton(e)) {
                            SwingUtilities.invokeLater(() -> new CuentaFrame(id, nombre, ComedorFrame.this));
                        }
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

    private void agregarMesa() {
        String nombre = JOptionPane.showInputDialog(this, "Nombre de la nueva mesa:");
        if (nombre != null && !nombre.trim().isEmpty()) {
            try (Connection conn = MySQLConnection.getConnection()) {
                String sql = "INSERT INTO mesas (nombre, estado) VALUES (?, 'ocupada')";
                PreparedStatement ps = conn.prepareStatement(sql);
                ps.setString(1, nombre.trim());
                ps.executeUpdate();
                cargarMesas();
            } catch (SQLException e) {
                JOptionPane.showMessageDialog(this, "Error al agregar mesa: " + e.getMessage());
            }
        }
    }

    private JPopupMenu crearMenuContextual(int id, String nombre) {
        JPopupMenu menu = new JPopupMenu();

        JMenuItem eliminar = new JMenuItem("Eliminar mesa");
        eliminar.addActionListener(e -> eliminarMesa(id));
        menu.add(eliminar);

        return menu;
    }

    private void eliminarMesa(int id) {
        int confirm = JOptionPane.showConfirmDialog(this, "¿Eliminar esta mesa?", "Confirmar", JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            try (Connection conn = MySQLConnection.getConnection()) {
                String sql = "DELETE FROM mesas WHERE id = ?";
                PreparedStatement ps = conn.prepareStatement(sql);
                ps.setInt(1, id);
                ps.executeUpdate();
                cargarMesas();
            } catch (SQLException e) {
                JOptionPane.showMessageDialog(this, "Error al eliminar mesa: " + e.getMessage());
            }
        }
    }
}
