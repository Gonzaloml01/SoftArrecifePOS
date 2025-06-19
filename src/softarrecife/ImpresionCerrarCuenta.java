package softarrecife;

import softarrecife.utils.TicketPrinter;

import javax.print.PrintException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import softarrecife.utils.TicketPrinter;

public class ImpresionCerrarCuenta {

    public static void imprimirTicket(List<String> lineas, double subtotal, double propina, double total, String metodoPago) {
        StringBuilder sb = new StringBuilder();
        int ancho = 32;

        sb.append(TicketPrinter.center("\n" + "\n" + "CASA BRISA BAR & COCTELERÍA", ancho)).append("\n");
        sb.append(TicketPrinter.center("Playa Miramar • Cd. Madero, Tamps", ancho)).append("\n");
        sb.append(TicketPrinter.center("Tel: 833-123-4567", ancho)).append("\n");
        sb.append("\n");

        String fecha = new SimpleDateFormat("dd/MM/yyyy HH:mm").format(new Date());
        sb.append(fecha).append("\n");
        sb.append("-".repeat(ancho)).append("\n");

        sb.append("Producto         Cant  Total\n");
        for (String item : lineas) {
            sb.append(item).append("\n");
        }
        sb.append("-".repeat(ancho)).append("\n");

        sb.append(String.format("%-20s %10.2f\n", "Subtotal:", subtotal));
        sb.append(String.format("%-20s %10.2f\n", "Propina:", propina));
        sb.append(String.format("%-20s %10.2f\n", "TOTAL:", total));
        sb.append(String.format("%-20s %10s\n", "Pago:", metodoPago));
        sb.append("-".repeat(ancho)).append("\n");

        sb.append(TicketPrinter.center("¡Gracias por su visita!" + "\n" + "\n" + "\n", ancho)).append("\n");
        sb.append("\n\n\n\n\n"); // corte de papel con salto

        try {
            TicketPrinter.imprimir("Impresora Tickets NE-511X", sb.toString());
        } catch (PrintException e) {
            e.printStackTrace();
        }
    }
}
