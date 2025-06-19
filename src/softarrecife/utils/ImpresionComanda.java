package softarrecife.utils;

import javax.print.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class ImpresionComanda {

    public static void imprimir(String nombreMesa, String mesero, List<String> productos) {
        StringBuilder sb = new StringBuilder();
        int ancho = 32;

        sb.append(center("** COMANDA **", ancho)).append("\n");
        sb.append("Mesa: ").append(nombreMesa).append("\n");
        sb.append("Mesero: ").append(mesero).append("\n");
        sb.append("Hora: ").append(new SimpleDateFormat("dd/MM/yyyy HH:mm").format(new Date())).append("\n");
        sb.append("-".repeat(ancho)).append("\n");

        for (String prod : productos) {
            sb.append("- ").append(prod).append("\n");
        }

        sb.append("-".repeat(ancho)).append("\n");
        sb.append("\n\n\n");

        try {
            DocPrintJob job = PrintServiceLookup.lookupDefaultPrintService().createPrintJob();
            byte[] bytes = sb.toString().getBytes();
            DocFlavor flavor = DocFlavor.BYTE_ARRAY.AUTOSENSE;
            Doc doc = new SimpleDoc(bytes, flavor, null);
            job.print(doc, null);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static String center(String text, int width) {
        if (text.length() >= width) return text;
        int pad = (width - text.length()) / 2;
        return " ".repeat(pad) + text;
    }
} 