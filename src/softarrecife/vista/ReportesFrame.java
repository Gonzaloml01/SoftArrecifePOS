package softarrecife.vista;

import java.io.File;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.FileOutputStream;
import java.sql.*;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import com.itextpdf.text.*;
import com.itextpdf.text.pdf.*;
import com.toedter.calendar.JDateChooser;
import softarrecife.conexion.MySQLConnection;

public class ReportesFrame extends JFrame {

    private JDateChooser fechaInicioPicker;
    private JDateChooser fechaFinPicker;
    private JButton btnGenerarReporte;
    private JButton btnExportarPDF;
    private JTextArea areaResultados;
    private JComboBox<String> comboTipoReporte;
    private JLabel lblDesdeSeleccionada;
    private JLabel lblHastaSeleccionada;
    private DecimalFormat formato = new DecimalFormat("#,##0.00");

    public ReportesFrame() {
        setTitle("Summa POS - Reportes");
        setSize(950, 600);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout());

        Color fondo = new Color(245, 245, 245);
        Color primario = new Color(30, 144, 255);
        java.awt.Font fuente = new java.awt.Font("SansSerif", java.awt.Font.PLAIN, 15);

        fechaInicioPicker = new JDateChooser();
        fechaFinPicker = new JDateChooser();
        comboTipoReporte = new JComboBox<>(new String[]{
            "Ventas totales",
            "Ventas por mesero",
            "Productos más vendidos",
            "Ventas por método de pago",
            "Ventas por turno",
            "Cuentas cerradas (detallado)"
        });
        btnGenerarReporte = new JButton("📊 Generar Reporte");
        btnExportarPDF = new JButton("🖨️ Exportar a PDF");

        lblDesdeSeleccionada = new JLabel("Fecha desde: ---");
        lblHastaSeleccionada = new JLabel("Fecha hasta: ---");

        fechaInicioPicker.setPreferredSize(new Dimension(150, 35));
        fechaFinPicker.setPreferredSize(new Dimension(150, 35));
        comboTipoReporte.setPreferredSize(new Dimension(220, 35));
        comboTipoReporte.setFont(fuente);

        for (JButton btn : new JButton[]{btnGenerarReporte, btnExportarPDF}) {
            btn.setPreferredSize(new Dimension(220, 40));
            btn.setFocusPainted(false);
            btn.setBackground(primario);
            btn.setForeground(Color.WHITE);
            btn.setFont(new java.awt.Font("SansSerif", java.awt.Font.BOLD, 15));
        }

        fechaInicioPicker.getDateEditor().addPropertyChangeListener(evt -> {
            if ("date".equals(evt.getPropertyName())) {
                Date seleccion = fechaInicioPicker.getDate();
                if (seleccion != null) {
                    SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");
                    lblDesdeSeleccionada.setText("Fecha desde: " + sdf.format(seleccion));
                }
            }
        });

        fechaFinPicker.getDateEditor().addPropertyChangeListener(evt -> {
            if ("date".equals(evt.getPropertyName())) {
                Date seleccion = fechaFinPicker.getDate();
                if (seleccion != null) {
                    SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");
                    lblHastaSeleccionada.setText("Fecha hasta: " + sdf.format(seleccion));
                }
            }
        });

        JPanel panelTop = new JPanel(new GridBagLayout());
        panelTop.setBackground(fondo);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridy = 0;

        gbc.gridx = 0; panelTop.add(new JLabel("Desde:"), gbc);
        gbc.gridx = 1; panelTop.add(fechaInicioPicker, gbc);
        gbc.gridx = 2; panelTop.add(new JLabel("Hasta:"), gbc);
        gbc.gridx = 3; panelTop.add(fechaFinPicker, gbc);
        gbc.gridx = 4; panelTop.add(comboTipoReporte, gbc);
        gbc.gridx = 5; panelTop.add(btnGenerarReporte, gbc);

        gbc.gridy = 1;
        gbc.gridx = 1; panelTop.add(lblDesdeSeleccionada, gbc);
        gbc.gridx = 3; panelTop.add(lblHastaSeleccionada, gbc);
        gbc.gridx = 5; panelTop.add(btnExportarPDF, gbc);

        add(panelTop, BorderLayout.NORTH);

        areaResultados = new JTextArea();
        areaResultados.setEditable(false);
        areaResultados.setFont(new java.awt.Font("Monospaced", java.awt.Font.PLAIN, 14));
        areaResultados.setMargin(new Insets(10, 10, 10, 10));
        JScrollPane scroll = new JScrollPane(areaResultados);
        add(scroll, BorderLayout.CENTER);

        btnGenerarReporte.addActionListener(e -> generarReporte());
        btnExportarPDF.addActionListener(e -> exportarReporteAPDF());

        setVisible(true);
    }

   private void exportarReporteAPDF() {
    String contenido = areaResultados.getText();
    if (contenido.isEmpty()) {
        JOptionPane.showMessageDialog(this, "Primero genera un reporte para exportar.");
        return;
    }

    try {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Guardar reporte como PDF");
        fileChooser.setSelectedFile(new File("Reporte_SummaPOS.pdf"));

        int userSelection = fileChooser.showSaveDialog(this);
        if (userSelection != JFileChooser.APPROVE_OPTION) return;

        File archivoDestino = fileChooser.getSelectedFile();
        if (!archivoDestino.getName().toLowerCase().endsWith(".pdf")) {
            archivoDestino = new File(archivoDestino.getAbsolutePath() + ".pdf");
        }

        Document documento = new Document();
        PdfWriter.getInstance(documento, new FileOutputStream(archivoDestino));
        documento.open();

        // Título
        com.itextpdf.text.Font tituloFont = new com.itextpdf.text.Font(
            com.itextpdf.text.Font.FontFamily.HELVETICA, 18, com.itextpdf.text.Font.BOLD);
        Paragraph titulo = new Paragraph("Summa-POS\n\n", tituloFont);
        titulo.setAlignment(Element.ALIGN_CENTER);
        documento.add(titulo);

        // Encabezado
        SimpleDateFormat sdfVisual = new SimpleDateFormat("dd/MM/yyyy");
        String fechaGeneracion = sdfVisual.format(new Date());

        Paragraph encabezado = new Paragraph(
            "Reporte: " + comboTipoReporte.getSelectedItem() + "\n" +
            "Desde: " + lblDesdeSeleccionada.getText().replace("Fecha desde: ", "") +
            "  Hasta: " + lblHastaSeleccionada.getText().replace("Fecha hasta: ", "") + "\n" +
            "Generado el: " + fechaGeneracion + "\n" +
            "Ubicación: Playa Miramar, Tampico, Tamps.\n\n",
            new com.itextpdf.text.Font(com.itextpdf.text.Font.FontFamily.COURIER, 11)
        );
        documento.add(encabezado);

        // Cuerpo
        Paragraph parrafo = new Paragraph(
            contenido,
            new com.itextpdf.text.Font(com.itextpdf.text.Font.FontFamily.COURIER, 11)
        );
        documento.add(parrafo);

        documento.close();

        JOptionPane.showMessageDialog(this, "PDF guardado exitosamente:\n" + archivoDestino.getAbsolutePath());

        // Abrir automáticamente
        if (Desktop.isDesktopSupported()) {
            Desktop.getDesktop().open(archivoDestino);
        }

    } catch (Exception ex) {
        JOptionPane.showMessageDialog(this, "Error al exportar PDF: " + ex.getMessage());
    }
}


    private void generarReporte() {
        Date desde = fechaInicioPicker.getDate();
        Date hasta = fechaFinPicker.getDate();

        if (desde == null || hasta == null) {
            JOptionPane.showMessageDialog(this, "Selecciona un rango de fechas completo.");
            return;
        }

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        String fechaInicio = sdf.format(desde);
        String fechaFin = sdf.format(hasta);
        String tipo = (String) comboTipoReporte.getSelectedItem();

        StringBuilder reporte = new StringBuilder();

        try (Connection conn = MySQLConnection.getConnection()) {
            switch (tipo) {
                case "Ventas totales": {
                    PreparedStatement ps = conn.prepareStatement("SELECT COUNT(*) AS cuentas, SUM(total) AS total, SUM(propina) AS propinas FROM cuentas WHERE estado = 'cerrada' AND fecha BETWEEN ? AND ?");
                    ps.setString(1, fechaInicio);
                    ps.setString(2, fechaFin);
                    ResultSet rs = ps.executeQuery();
                    if (rs.next()) {
                        double total = rs.getDouble("total");
                        double propinas = rs.getDouble("propinas");
                        int cuentas = rs.getInt("cuentas");
                        reporte.append("▶ Ventas totales:\n");
                        reporte.append("  Cuentas cerradas: " + cuentas + "\n");
                        reporte.append("  Total vendido: $" + formato.format(total) + "\n");
                        reporte.append("  Propinas: $" + formato.format(propinas) + "\n");
                        reporte.append("  Subtotal sin propinas: $" + formato.format(total - propinas) + "\n");
                    }
                    break;
                }
                case "Ventas por mesero": {
                    PreparedStatement ps = conn.prepareStatement("SELECT u.nombre, COUNT(c.id) AS cuentas, SUM(c.total) AS total, SUM(c.propina) AS propinas FROM cuentas c JOIN usuarios u ON c.usuario_id = u.id WHERE c.estado = 'cerrada' AND c.fecha BETWEEN ? AND ? GROUP BY u.nombre");
                    ps.setString(1, fechaInicio);
                    ps.setString(2, fechaFin);
                    ResultSet rs = ps.executeQuery();
                    double total = 0, propinas = 0;
                    reporte.append("▶ Ventas por mesero:\n");
                    while (rs.next()) {
                        double t = rs.getDouble("total");
                        double p = rs.getDouble("propinas");
                        reporte.append("  " + rs.getString("nombre") + ": $" + formato.format(t) + " en " + rs.getInt("cuentas") + " cuentas\n");
                        total += t;
                        propinas += p;
                    }
                    reporte.append("\n  Total: $" + formato.format(total) + " | Propinas: $" + formato.format(propinas) + " | Subtotal: $" + formato.format(total - propinas) + "\n");
                    break;
                }
                case "Productos más vendidos": {
                    PreparedStatement ps = conn.prepareStatement("SELECT p.nombre, SUM(d.cantidad) AS cantidad FROM detalle_cuenta d JOIN productos p ON d.producto_id = p.id JOIN cuentas c ON d.cuenta_id = c.id WHERE c.estado = 'cerrada' AND c.fecha BETWEEN ? AND ? GROUP BY p.nombre ORDER BY cantidad DESC LIMIT 10");
                    ps.setString(1, fechaInicio);
                    ps.setString(2, fechaFin);
                    ResultSet rs = ps.executeQuery();
                    int total = 0;
                    reporte.append("▶ Productos más vendidos:\n");
                    while (rs.next()) {
                        int cantidad = rs.getInt("cantidad");
                        reporte.append("  " + rs.getString("nombre") + ": " + cantidad + " vendidos\n");
                        total += cantidad;
                    }
                    reporte.append("\n  Total (Top 10): " + total + " unidades\n");
                    break;
                }
                case "Ventas por método de pago": {
                    PreparedStatement ps = conn.prepareStatement("SELECT metodo_pago, COUNT(*) AS cantidad, SUM(total) AS total FROM cuentas WHERE estado = 'cerrada' AND fecha BETWEEN ? AND ? GROUP BY metodo_pago");
                    ps.setString(1, fechaInicio);
                    ps.setString(2, fechaFin);
                    ResultSet rs = ps.executeQuery();
                    double total = 0;
                    reporte.append("▶ Ventas por método de pago:\n");
                    while (rs.next()) {
                        double monto = rs.getDouble("total");
                        reporte.append("  " + rs.getString("metodo_pago") + ": $" + formato.format(monto) + " en " + rs.getInt("cantidad") + " pagos\n");
                        total += monto;
                    }
                    reporte.append("\n  Total: $" + formato.format(total) + "\n");
                    break;
                }
                case "Ventas por turno": {
                    PreparedStatement ps = conn.prepareStatement("SELECT turno_id, SUM(total) AS total FROM cuentas WHERE estado = 'cerrada' AND fecha BETWEEN ? AND ? GROUP BY turno_id");
                    ps.setString(1, fechaInicio);
                    ps.setString(2, fechaFin);
                    ResultSet rs = ps.executeQuery();
                    double total = 0;
                    reporte.append("▶ Ventas por turno:\n");
                    while (rs.next()) {
                        double monto = rs.getDouble("total");
                        reporte.append("  Turno " + rs.getInt("turno_id") + ": $" + formato.format(monto) + "\n");
                        total += monto;
                    }
                    reporte.append("\n  Total: $" + formato.format(total) + "\n");
                    break;
                }
                case "Cuentas cerradas (detallado)": {
                    PreparedStatement ps = conn.prepareStatement("SELECT id, fecha, total, propina, metodo_pago FROM cuentas WHERE estado = 'cerrada' AND fecha BETWEEN ? AND ? ORDER BY fecha DESC");
                    ps.setString(1, fechaInicio);
                    ps.setString(2, fechaFin);
                    ResultSet rs = ps.executeQuery();
                    int count = 0;
                    double total = 0, propinas = 0;
                    reporte.append("▶ Cuentas cerradas (detallado):\n");
                    while (rs.next()) {
                        double t = rs.getDouble("total");
                        double p = rs.getDouble("propina");
                        reporte.append("  Cuenta #" + rs.getInt("id") + ": $" + formato.format(t) + " | Propina: $" + formato.format(p) + " | Pago: " + rs.getString("metodo_pago") + " | Fecha: " + rs.getString("fecha") + "\n");
                        total += t;
                        propinas += p;
                        count++;
                    }
                    reporte.append("\n  Total cuentas: " + count + " | Total vendido: $" + formato.format(total) + " | Propinas: $" + formato.format(propinas) + "\n");
                    break;
                }
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Error al generar reporte: " + ex.getMessage());
        }

        areaResultados.setText(reporte.toString());
    }
}
