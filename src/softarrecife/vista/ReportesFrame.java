package softarrecife.vista;

import javax.swing.*;
import javax.swing.border.EmptyBorder;

import com.itextpdf.text.*;
import com.itextpdf.text.pdf.*;
import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.Chunk;
import com.itextpdf.text.Font;
import com.itextpdf.text.FontFactory;
import com.itextpdf.text.PageSize;
import com.itextpdf.text.pdf.PdfWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import softarrecife.utils.ComboItem;

import com.toedter.calendar.JDateChooser;

import softarrecife.conexion.MySQLConnection;
import softarrecife.utils.Estilos;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.sql.*;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import softarrecife.utils.ComboItem;

public class ReportesFrame extends JFrame {

    private JDateChooser fechaInicioPicker;
    private JDateChooser fechaFinPicker;
    private JButton btnGenerarReporte;
    private JButton btnExportarPDF;
    private JTextArea areaResultados;
    private JComboBox<String> comboTipoReporte;

    private JComboBox<ComboItem> comboCategoria;
    private JComboBox<ComboItem> comboSubcategoria;
    private JComboBox<ComboItem> comboSubsubcategoria;

    private JLabel lblDesdeSeleccionada;
    private JLabel lblHastaSeleccionada;
    private DecimalFormat formato = new DecimalFormat("#,##0.00");

    public ReportesFrame() {
        setTitle("Summa POS - Reportes");
        setSize(1000, 650);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout());

        fechaInicioPicker = new JDateChooser();
        fechaFinPicker = new JDateChooser();

        comboTipoReporte = new JComboBox<>(new String[]{
            "Ventas totales",
            "Ventas por mesero",
            "Productos más vendidos",
            "Ventas por método de pago",
            "Ventas por turno",
            "Cuentas cerradas (detallado)",
            "Ventas por categoría"
        });

        comboCategoria = new JComboBox<>();
        comboSubcategoria = new JComboBox<>();
        comboSubsubcategoria = new JComboBox<>();

        btnGenerarReporte = Estilos.crearBotonPrincipal("📊 Generar Reporte");
        btnExportarPDF = Estilos.crearBotonSecundario("🖨️ Exportar a PDF");

        lblDesdeSeleccionada = new JLabel("Fecha desde: ---");
        lblHastaSeleccionada = new JLabel("Fecha hasta: ---");
        Estilos.estilizarLabel(lblDesdeSeleccionada);
        Estilos.estilizarLabel(lblHastaSeleccionada);

        fechaInicioPicker.setPreferredSize(new Dimension(150, 35));
        fechaFinPicker.setPreferredSize(new Dimension(150, 35));
        comboTipoReporte.setPreferredSize(new Dimension(220, 35));
        comboTipoReporte.setFont(Estilos.fuenteNormal);

        comboCategoria.setPreferredSize(new Dimension(160, 35));
        comboSubcategoria.setPreferredSize(new Dimension(160, 35));
        comboSubsubcategoria.setPreferredSize(new Dimension(160, 35));

        // Estilos a combos
        comboCategoria.setFont(Estilos.fuenteNormal);
        comboSubcategoria.setFont(Estilos.fuenteNormal);
        comboSubsubcategoria.setFont(Estilos.fuenteNormal);

        JPanel panelTop = new JPanel(new GridBagLayout());
        panelTop.setBackground(Estilos.grisClaro);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 10, 8, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridy = 0;

        gbc.gridx = 0;
        panelTop.add(new JLabel("Desde:"), gbc);
        gbc.gridx = 1;
        panelTop.add(fechaInicioPicker, gbc);
        gbc.gridx = 2;
        panelTop.add(new JLabel("Hasta:"), gbc);
        gbc.gridx = 3;
        panelTop.add(fechaFinPicker, gbc);
        gbc.gridx = 4;
        panelTop.add(comboTipoReporte, gbc);
        gbc.gridx = 5;
        panelTop.add(btnGenerarReporte, gbc);

        gbc.gridy = 1;
        gbc.gridx = 1;
        panelTop.add(lblDesdeSeleccionada, gbc);
        gbc.gridx = 3;
        panelTop.add(lblHastaSeleccionada, gbc);
        gbc.gridx = 5;
        panelTop.add(btnExportarPDF, gbc);

        gbc.gridy = 2;
        gbc.gridx = 1;
        panelTop.add(comboCategoria, gbc);
        gbc.gridx = 2;
        panelTop.add(comboSubcategoria, gbc);
        gbc.gridx = 3;
        panelTop.add(comboSubsubcategoria, gbc);

        add(panelTop, BorderLayout.NORTH);

        areaResultados = new JTextArea();
        areaResultados.setEditable(false);
        areaResultados.setFont(Estilos.fuenteNormal);
        areaResultados.setMargin(new Insets(10, 10, 10, 10));
        JScrollPane scroll = new JScrollPane(areaResultados);
        add(scroll, BorderLayout.CENTER);

        cargarCategorias();

        comboCategoria.addActionListener(e -> cargarSubcategorias());
        comboSubcategoria.addActionListener(e -> cargarSubsubcategorias());

        comboTipoReporte.addActionListener(e -> {
            String tipo = (String) comboTipoReporte.getSelectedItem();
            boolean mostrar = "Ventas por categoría".equals(tipo);
            comboCategoria.setVisible(mostrar);
            comboSubcategoria.setVisible(mostrar);
            comboSubsubcategoria.setVisible(mostrar);
        });

        btnGenerarReporte.addActionListener(e -> generarReporte());
        btnExportarPDF.addActionListener(e -> exportarReporteAPDF());
    }

    private void exportarReporteAPDF() {
        try {
            String contenido = areaResultados.getText();

            if (contenido == null || contenido.trim().isEmpty()) {
                JOptionPane.showMessageDialog(this, "No hay contenido para exportar.", "Advertencia", JOptionPane.WARNING_MESSAGE);
                return;
            }

            // Obtener tipo de reporte seleccionado
            String tipoReporte = (String) comboTipoReporte.getSelectedItem();
            if (tipoReporte == null || tipoReporte.trim().isEmpty()) {
                tipoReporte = "reporte";
            }

            String tipoFormateado = tipoReporte.toLowerCase().replace(" ", "_");
            String timestamp = new SimpleDateFormat("yyyy-MM-dd_HH-mm").format(new Date());
            String nombreSugerido = "reporte_" + tipoFormateado + "_" + timestamp + ".pdf";

            // Selección de archivo por el usuario
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setSelectedFile(new File(nombreSugerido));
            int seleccion = fileChooser.showSaveDialog(this);
            if (seleccion != JFileChooser.APPROVE_OPTION) {
                return; // Cancelado por el usuario
            }

            File archivo = fileChooser.getSelectedFile();

            // Crear documento PDF
            Document documento = new Document(PageSize.A4);
            PdfWriter.getInstance(documento, new FileOutputStream(archivo));
            documento.open();

            Font fuenteTitulo = new Font(Font.FontFamily.HELVETICA, 16, Font.BOLD);
            Font fuenteContenido = new Font(Font.FontFamily.COURIER, 12, Font.NORMAL);

            documento.add(new Paragraph("Reporte Summa POS", fuenteTitulo));
            String fechaGeneracion = new SimpleDateFormat("dd/MM/yyyy HH:mm").format(new Date());
            documento.add(new Paragraph("Fecha de generación: " + fechaGeneracion, fuenteContenido));
            documento.add(new Paragraph("Tipo de reporte: " + tipoReporte, fuenteContenido));
            documento.add(Chunk.NEWLINE);

            for (String linea : contenido.split("\n")) {
                documento.add(new Paragraph(linea, fuenteContenido));
            }

            documento.close();

            JOptionPane.showMessageDialog(this, "Reporte exportado correctamente a PDF.", "Éxito", JOptionPane.INFORMATION_MESSAGE);
            Desktop.getDesktop().open(archivo);

        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error al exportar el reporte: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
    }

    private void cargarCategorias() {
        comboCategoria.removeAllItems();
        comboCategoria.addItem(new ComboItem("Todas", -1));

        try (Connection conn = MySQLConnection.getConnection(); PreparedStatement ps = conn.prepareStatement("SELECT id, nombre FROM categorias")) {

            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                comboCategoria.addItem(new ComboItem(rs.getString("nombre"), rs.getInt("id")));
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void cargarSubcategorias() {
        comboSubcategoria.removeAllItems();
        comboSubcategoria.addItem(new ComboItem("Todas", -1));

        ComboItem categoriaSeleccionada = (ComboItem) comboCategoria.getSelectedItem();
        if (categoriaSeleccionada == null || categoriaSeleccionada.getId() == -1) {
            return;
        }

        try (Connection conn = MySQLConnection.getConnection(); PreparedStatement ps = conn.prepareStatement("SELECT id, nombre FROM subcategorias WHERE categoria_id = ?")) {

            ps.setInt(1, categoriaSeleccionada.getId());
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                comboSubcategoria.addItem(new ComboItem(rs.getString("nombre"), rs.getInt("id")));
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void cargarSubsubcategorias() {
        comboSubsubcategoria.removeAllItems();
        comboSubsubcategoria.addItem(new ComboItem("Todas", -1));

        ComboItem subcatSeleccionada = (ComboItem) comboSubcategoria.getSelectedItem();
        if (subcatSeleccionada == null || subcatSeleccionada.getId() == -1) {
            return;
        }

        try (Connection conn = MySQLConnection.getConnection(); PreparedStatement ps = conn.prepareStatement("SELECT id, nombre FROM subsubcategorias WHERE subcategoria_id = ?")) {

            ps.setInt(1, subcatSeleccionada.getId());
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                comboSubsubcategoria.addItem(new ComboItem(rs.getString("nombre"), rs.getInt("id")));
            }

        } catch (SQLException e) {
            e.printStackTrace();
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
                case "Ventas por categoría": {
                    int categoriaId = comboCategoria.getSelectedIndex() > 0 ? ((ComboItem) comboCategoria.getSelectedItem()).getId() : -1;
                    int subcategoriaId = comboSubcategoria.getSelectedIndex() > 0 ? ((ComboItem) comboSubcategoria.getSelectedItem()).getId() : -1;
                    int subsubcategoriaId = comboSubsubcategoria.getSelectedIndex() > 0 ? ((ComboItem) comboSubsubcategoria.getSelectedItem()).getId() : -1;

                    String sql = "SELECT p.nombre, SUM(d.cantidad) AS cantidad, SUM(d.subtotal) AS total "
                            + "FROM detalle_cuenta d "
                            + "JOIN productos p ON d.producto_id = p.id "
                            + "JOIN cuentas c ON d.cuenta_id = c.id "
                            + "WHERE c.estado = 'cerrada' AND c.fecha BETWEEN ? AND ? ";

                    if (categoriaId != -1) {
                        sql += "AND p.categoria_id = ? ";
                    }
                    if (subcategoriaId != -1) {
                        sql += "AND p.subcategoria_id = ? ";
                    }
                    if (subsubcategoriaId != -1) {
                        sql += "AND p.subsubcategoria_id = ? ";
                    }

                    sql += "GROUP BY p.nombre ORDER BY cantidad DESC";

                    PreparedStatement ps = conn.prepareStatement(sql);
                    int idx = 1;
                    ps.setString(idx++, fechaInicio);
                    ps.setString(idx++, fechaFin);
                    if (categoriaId != -1) {
                        ps.setInt(idx++, categoriaId);
                    }
                    if (subcategoriaId != -1) {
                        ps.setInt(idx++, subcategoriaId);
                    }
                    if (subsubcategoriaId != -1) {
                        ps.setInt(idx++, subsubcategoriaId);
                    }

                    ResultSet rs = ps.executeQuery();
                    int totalProductos = 0;
                    double totalVentas = 0;

                    reporte.append("▶ Ventas por categoría:\n");
                    while (rs.next()) {
                        int cantidad = rs.getInt("cantidad");
                        double total = rs.getDouble("total");
                        reporte.append("  " + rs.getString("nombre") + ": " + cantidad + " vendidos, $" + formato.format(total) + "\n");
                        totalProductos += cantidad;
                        totalVentas += total;
                    }

                    reporte.append("\n  Total unidades vendidas: " + totalProductos + " | Total: $" + formato.format(totalVentas) + "\n");
                    break;
                }
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Error al generar reporte: " + ex.getMessage());
        }

        areaResultados.setText(reporte.toString());
    }
}
