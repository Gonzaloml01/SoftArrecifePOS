package softarrecife.vista;

import softarrecife.conexion.MySQLConnection;

import javax.swing.*;
import java.awt.*;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class TurnoFrame extends JFrame {

    private int usuarioId;

    public TurnoFrame(int usuarioId) {
        this.usuarioId = usuarioId;

        setTitle("Gestión de Turnos");
        setSize(400, 200);
        setLocationRelativeTo(null);
        setLayout(new FlowLayout());

        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        JButton btnAbrir = new JButton("🔓 Abrir turno");
        JButton btnCerrar = new JButton("🔒 Cerrar turno");

        btnAbrir.addActionListener(e -> abrirTurno());
        btnCerrar.addActionListener(e -> cerrarTurno());

        add(btnAbrir);
        add(btnCerrar);

        setVisible(true);
    }

    private boolean hayTurnoAbierto() {
        try (Connection conn = MySQLConnection.getConnection()) {
            String check = "SELECT COUNT(*) FROM turnos WHERE estado = 'abierto' AND usuario_id = ?";
            PreparedStatement ps = conn.prepareStatement(check);
            ps.setInt(1, usuarioId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    private void abrirTurno() {
        try (Connection conn = MySQLConnection.getConnection()) {
            String check = "SELECT COUNT(*) FROM turnos WHERE estado = 'abierto' AND usuario_id = ?";
            PreparedStatement ps = conn.prepareStatement(check);
            ps.setInt(1, usuarioId);
            ResultSet rs = ps.executeQuery();
            rs.next();
            if (rs.getInt(1) > 0) {
                JOptionPane.showMessageDialog(this, "Ya tienes un turno abierto.");
                return;
            }

            String fondoStr = JOptionPane.showInputDialog(this, "Ingresa el fondo de caja inicial:");
            if (fondoStr == null || fondoStr.trim().isEmpty()) {
                return;
            }

            double fondo = Double.parseDouble(fondoStr);

            String insert = "INSERT INTO turnos (usuario_id, hora_inicio, estado, fondo_inicio) VALUES (?, NOW(), 'abierto', ?)";
            ps = conn.prepareStatement(insert);
            ps.setInt(1, usuarioId);
            ps.setDouble(2, fondo);
            ps.executeUpdate();
            JOptionPane.showMessageDialog(this, "Turno abierto correctamente.");
            dispose(); // 👉 Esto cierra la ventana de TurnoFrame
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error: " + e.getMessage());
        }
    }

    private void cerrarTurno() {
        try (Connection conn = MySQLConnection.getConnection()) {
            // Verificar cuentas abiertas
            String checkCuentas = "SELECT COUNT(*) FROM cuentas WHERE estado = 'abierta'";
            PreparedStatement psCheck = conn.prepareStatement(checkCuentas);
            ResultSet rsCheck = psCheck.executeQuery();
            rsCheck.next();
            int abiertas = rsCheck.getInt(1);

            if (abiertas > 0) {
                JOptionPane.showMessageDialog(this,
                        "No puedes cerrar el turno. Hay " + abiertas + " cuenta(s) abierta(s).",
                        "Cierre bloqueado",
                        JOptionPane.WARNING_MESSAGE
                );
                return;
            }

            // Obtener datos del turno
            String turnoInfo = """
                SELECT t.id, t.hora_inicio, u.nombre, t.fondo_inicio
                FROM turnos t
                JOIN usuarios u ON t.usuario_id = u.id
                WHERE t.usuario_id = ? AND t.estado = 'abierto'
            """;
            PreparedStatement ps = conn.prepareStatement(turnoInfo);
            ps.setInt(1, usuarioId);
            ResultSet rs = ps.executeQuery();

            if (!rs.next()) {
                JOptionPane.showMessageDialog(this, "No tienes un turno abierto.");
                return;
            }

            int turnoId = rs.getInt("id");
            String horaInicio = rs.getString("hora_inicio");
            String nombre = rs.getString("nombre");
            double fondo = rs.getDouble("fondo_inicio");

            // Calcular ventas desde hora de inicio
            String ventasSQL = "SELECT SUM(total) FROM cuentas WHERE fecha >= ? AND estado = 'cerrada'";
            ps = conn.prepareStatement(ventasSQL);
            ps.setString(1, horaInicio);
            ResultSet rsVentas = ps.executeQuery();

            double ventas = 0;
            if (rsVentas.next()) {
                ventas = rsVentas.getDouble(1);
            }

            double totalEnCaja = fondo + ventas;

            // Leer propinas del txt
            double totalPropinas = 0;
            String archivoPropinas = "propinas_" + LocalDate.now() + ".txt";
            try (BufferedReader br = new BufferedReader(new FileReader(archivoPropinas))) {
                String linea;
                while ((linea = br.readLine()) != null) {
                    if (linea.contains("Propina: $")) {
                        String[] partes = linea.split("\\\\\\$");
                        if (partes.length > 1) {
                            totalPropinas += Double.parseDouble(partes[1]);
                        }
                    }
                }
            } catch (Exception e) {
                // Si no existe el archivo, no pasa nada
            }

            // Cerrar el turno
            String cerrar = "UPDATE turnos SET estado = 'cerrado', hora_cierre = NOW() WHERE id = ?";
            ps = conn.prepareStatement(cerrar);
            ps.setInt(1, turnoId);
            ps.executeUpdate();

            // Mostrar resumen tipo ticket
            String resumen = """
                --- CIERRE DE TURNO ---
                Usuario: %s
                Inicio: %s
                Fondo inicial: $%.2f
                Ventas: $%.2f
                Propinas: $%.2f
                Total en caja: $%.2f
                -------------------------
                Fecha cierre: %s
            """.formatted(nombre, horaInicio, fondo, ventas, totalPropinas, totalEnCaja, LocalDateTime.now());

            JTextArea text = new JTextArea(resumen);
            text.setFont(new Font("Monospaced", Font.PLAIN, 14));
            text.setEditable(false);
            JOptionPane.showMessageDialog(this, new JScrollPane(text), "Cierre de turno", JOptionPane.INFORMATION_MESSAGE);

            // Guardar también en archivo .txt
            String archivo = "cierre_turno_" + LocalDate.now() + ".txt";
            try (PrintWriter writer = new PrintWriter(new FileWriter(archivo))) {
                writer.print(resumen);
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this, "No se pudo guardar el resumen en archivo.");
            }

            dispose();
            System.exit(0);
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error SQL: " + e.getMessage());
        }
    }
}
