package softarrecife.vista;

import softarrecife.conexion.MySQLConnection;

import javax.swing.*;
import java.awt.*;
import java.sql.*;
import java.time.LocalDate;

public class ReportesFrame extends JFrame {

    private JSpinner spinnerFecha;
    private JTextArea areaResultado;

    public ReportesFrame() {
        setTitle("📊 Reporte de Ventas del Día");
        setSize(500, 400);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        // Panel superior: fecha y botón
        JPanel topPanel = new JPanel();
        topPanel.setLayout(new FlowLayout());

        spinnerFecha = new JSpinner(new SpinnerDateModel());
        spinnerFecha.setEditor(new JSpinner.DateEditor(spinnerFecha, "yyyy-MM-dd"));

        JButton btnGenerar = new JButton("Generar Reporte");
        btnGenerar.addActionListener(e -> generarReporte());

        topPanel.add(new JLabel("Selecciona fecha:"));
        topPanel.add(spinnerFecha);
        topPanel.add(btnGenerar);

        // Panel de resultados
        areaResultado = new JTextArea();
        areaResultado.setFont(new Font("Monospaced", Font.PLAIN, 14));
        areaResultado.setEditable(false);
        JScrollPane scroll = new JScrollPane(areaResultado);

        add(topPanel, BorderLayout.NORTH);
        add(scroll, BorderLayout.CENTER);

        setVisible(true);
    }

    private void generarReporte() {
        java.util.Date fecha = (java.util.Date) spinnerFecha.getValue();
        java.sql.Date sqlDate = new java.sql.Date(fecha.getTime());

        try (Connection conn = MySQLConnection.getConnection()) {
            String resumen = "📆 Reporte de ventas para: " + sqlDate + "\n\n";

            // Total de cuentas cerradas
            String q1 = "SELECT COUNT(*), SUM(total), SUM(propina) FROM cuentas WHERE DATE(fecha_cierre) = ? AND estado = 'cerrada'";
            PreparedStatement ps1 = conn.prepareStatement(q1);
            ps1.setDate(1, sqlDate);
            ResultSet rs1 = ps1.executeQuery();
            if (rs1.next()) {
                int totalCuentas = rs1.getInt(1);
                double totalVentas = rs1.getDouble(2);
                double totalPropinas = rs1.getDouble(3);
                resumen += "🧾 Cuentas cerradas: " + totalCuentas + "\n";
                resumen += "💵 Total vendido: $" + String.format("%.2f", totalVentas) + "\n";
                resumen += "💰 Propinas: $" + String.format("%.2f", totalPropinas) + "\n\n";
            }

            // Totales por método de pago
            String q2 = "SELECT metodo_pago, SUM(total) FROM cuentas WHERE DATE(fecha_cierre) = ? AND estado = 'cerrada' GROUP BY metodo_pago";
            PreparedStatement ps2 = conn.prepareStatement(q2);
            ps2.setDate(1, sqlDate);
            ResultSet rs2 = ps2.executeQuery();
            resumen += "📌 Ventas por método de pago:\n";
            while (rs2.next()) {
                String metodo = rs2.getString(1);
                double totalMetodo = rs2.getDouble(2);
                resumen += "  - " + metodo + ": $" + String.format("%.2f", totalMetodo) + "\n";
            }

            areaResultado.setText(resumen);
            generarPDF(sqlDate.toString(), resumen); // ✅ Generar PDF

        } catch (SQLException e) {
            areaResultado.setText("❌ Error al generar reporte:\n" + e.getMessage());
        }
    }

    private void generarPDF(String fecha, String contenido) {
        try {
            com.itextpdf.text.Document document = new com.itextpdf.text.Document();
            com.itextpdf.text.pdf.PdfWriter.getInstance(document, new java.io.FileOutputStream("Reporte_" + fecha + ".pdf"));
            document.open();
            com.itextpdf.text.Font font = new com.itextpdf.text.Font(com.itextpdf.text.Font.FontFamily.HELVETICA, 12);
            document.add(new com.itextpdf.text.Paragraph(contenido, font));
            document.close();
            JOptionPane.showMessageDialog(this, "✅ PDF generado: Reporte_" + fecha + ".pdf");
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "❌ Error al generar PDF:\n" + e.getMessage());
        }
    }
}
