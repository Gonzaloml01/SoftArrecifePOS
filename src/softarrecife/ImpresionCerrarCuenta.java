package softarrecife;

import softarrecife.utils.TicketPrinter;

import javax.print.PrintException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class ImpresionCerrarCuenta {

    /**
     * Imprime el ticket de cierre de cuenta.
     * @param lineas Listado de ítems: "Producto Cant PrecioTotal"
     * @param subtotal Subtotal sin propina
     * @param propina Valor de la propina
     * @param total Total pagado
     * @param metodoPago Ej: "Efectivo", "Tarjeta"
     */
    public static void imprimirTicket(List<String> lineas, double subtotal, double propina, double total, String metodoPago) {
        StringBuilder sb = new StringBuilder();
        int ancho = 32;

        // Encabezado
        sb.append(TicketPrinter.center("CASA BRISA BAR & COCTELERÍA", ancho)).append("\n");
        sb.append(TicketPrinter.center("Playa Miramar • Cd. Madero, Tamps", ancho)).append("\n");
        sb.append(TicketPrinter.center("Tel: 833-123-4567 • Cel: 833-765-4321", ancho)).append("\n");
        sb.append("\n");

        // Fecha y hora
        String fecha = new SimpleDateFormat("dd/MM/yyyy HH:mm").format(new Date());
        sb.append(fecha).append("\n");
        sb.append("-".repeat(ancho)).append("\n");

        // Detalle de productos
        sb.append("Item             Cant   Total\n");
        for (String item : lineas) {
            sb.append(item).append("\n");
        }
        sb.append("-".repeat(ancho)).append("\n");

        sb.append(String.format("%-20s %10.2f\n", "Subtotal:", subtotal));
        sb.append(String.format("%-20s %10.2f\n", "Propina:", propina));
        sb.append(String.format("%-20s %10.2f\n", "TOTAL:", total));
        sb.append(String.format("%-20s %10s\n", "Pago:", metodoPago));
        sb.append("-".repeat(ancho)).append("\n");

        sb.append(TicketPrinter.center("¡Gracias por su preferencia!", ancho)).append("\n");
        sb.append(TicketPrinter.center("Síguenos en Instagram @CasaBrisaBar", ancho)).append("\n");
        sb.append("\n\n\n"); // espacio para cortar

        // Enviar a impresora
        try {
            TicketPrinter.imprimir("Impresora Tickets NE‑511X", sb.toString());
        } catch (PrintException e) {
            e.printStackTrace();
            // Podrías mostrar un mensaje al usuario si lo deseas
        }
    }
}
