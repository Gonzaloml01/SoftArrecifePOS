package softarrecife;

import softarrecife.utils.TicketPrinter;

import javax.print.PrintException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class ImpresionComanda {

    public static void imprimir(String nombreMesa, String mesero, List<String> productos) {
        StringBuilder sb = new StringBuilder();
        int ancho = 32;

        sb.append(TicketPrinter.center("** COMANDA **", ancho)).append("\n");
        sb.append("Mesa: ").append(nombreMesa).append("\n");
        if (mesero != null && !mesero.isBlank()) {
            sb.append("Mesero: ").append(mesero).append("\n");
        }

        String hora = new SimpleDateFormat("dd/MM/yyyy HH:mm").format(new Date());
        sb.append("Hora: ").append(hora).append("\n");
        sb.append("-".repeat(ancho)).append("\n");

        for (String prod : productos) {
            sb.append("- ").append(prod).append("\n");
        }

        sb.append("-".repeat(ancho)).append("\n");
        sb.append("\n\n\n");

        try {
            TicketPrinter.imprimir("Impresora Tickets NE-511X", sb.toString());
        } catch (PrintException e) {
            e.printStackTrace();
        }
    }
}
