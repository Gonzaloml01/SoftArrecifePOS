package softarrecife;

import javax.print.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class ImpresionCerrarCuenta {

    public static void imprimirTicket(List<String> productos, double total, double propina, double totalConPropina, String metodoPago) {
        StringBuilder sb = new StringBuilder();
        int ancho = 32;

        // Encabezado
        sb.append(center("CASA BRISA BAR & COCTELERÍA", ancho)).append("\n");
        sb.append(center("Playa Miramar - Cd. Madero, Tamps", ancho)).append("\n");
        sb.append(center("Tel: 833-123-4567", ancho)).append("\n");
        sb.append("\n");
        sb.append("Fecha: ").append(new SimpleDateFormat("dd/MM/yyyy HH:mm").format(new Date())).append("\n");
        sb.append("Metodo: ").append(metodoPago).append("\n");
        sb.append("-".repeat(ancho)).append("\n");

        // Productos
        for (String linea : productos) {
            sb.append(linea).append("\n");
        }

        // Totales
        sb.append("-".repeat(ancho)).append("\n");
        sb.append(String.format("%-20s %10.2f\n", "TOTAL:", total));
        if (propina > 0) {
            sb.append(String.format("%-20s %10.2f\n", "PROPINA:", propina));
            sb.append(String.format("%-20s %10.2f\n", "TOTAL FINAL:", totalConPropina));
        }

        // Mensaje de cierre
        sb.append("\n");
        sb.append(center("Gracias por su visita!", ancho)).append("\n");

        // Cortar papel
        sb.append("\n\n");

        try {
            PrintService printService = PrintServiceLookup.lookupDefaultPrintService();
            if (printService != null) {
                DocPrintJob job = printService.createPrintJob();
                byte[] bytes = sb.toString().getBytes();
                Doc doc = new SimpleDoc(bytes, DocFlavor.BYTE_ARRAY.AUTOSENSE, null);
                job.print(doc, null);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static String center(String text, int width) {
        if (text.length() >= width) {
            return text;
        }
        int pad = (width - text.length()) / 2;
        return " ".repeat(pad) + text;
    }

    public static void imprimirTicketSinAbrirCaja(List<String> productos, double total, double propina, double totalConPropina, String metodoPago) {
        StringBuilder sb = new StringBuilder();
        int ancho = 32;

        // Encabezado
        sb.append(center("CASA BRISA BAR & COCTELERÍA", ancho)).append("\n");
        sb.append(center("Playa Miramar - Cd. Madero, Tamps", ancho)).append("\n");
        sb.append(center("Tel: 833-123-4567", ancho)).append("\n");
        sb.append("\n");
        sb.append("Fecha: ").append(new SimpleDateFormat("dd/MM/yyyy HH:mm").format(new Date())).append("\n");
        sb.append("Metodo: ").append(metodoPago).append("\n");
        sb.append("-".repeat(ancho)).append("\n");

        // Productos
        for (String linea : productos) {
            sb.append(linea).append("\n");
        }

        // Totales
        sb.append("-".repeat(ancho)).append("\n");
        sb.append(String.format("%-20s %10.2f\n", "TOTAL:", total));
        if (propina > 0) {
            sb.append(String.format("%-20s %10.2f\n", "PROPINA:", propina));
            sb.append(String.format("%-20s %10.2f\n", "TOTAL FINAL:", totalConPropina));
        }

        // Mensaje de cierre
        sb.append("\n");
        sb.append(center("Gracias por su visita!", ancho)).append("\n");

        // ⚠️ NO añadimos \n\n\n para evitar abrir caja (ni ningún ESC/POS)
        // Si lo haces normalmente con códigos como: sb.append((char)27 + "p" + ...); evítalos aquí.
        try {
            PrintService printService = PrintServiceLookup.lookupDefaultPrintService();
            if (printService != null) {
                DocPrintJob job = printService.createPrintJob();
                byte[] bytes = sb.toString().getBytes();
                Doc doc = new SimpleDoc(bytes, DocFlavor.BYTE_ARRAY.AUTOSENSE, null);
                job.print(doc, null);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
