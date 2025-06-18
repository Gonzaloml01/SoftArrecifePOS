package softarrecife.utils;

import javax.print.*;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class TicketPrinter {

    /**
     * Imprime el ticket y abre la caja registradora.
     * @param impresoraNombre Nombre exacto en Windows: "Impresora Tickets NE‑511X"
     * @param contenido Contenido del ticket formateado
     * @throws PrintException En caso de error en impresión
     */
    public static void imprimir(String impresoraNombre, String contenido) throws PrintException {
        PrintService impresora = null;
        for (PrintService s : PrintServiceLookup.lookupPrintServices(null, null)) {
            if (s.getName().equalsIgnoreCase(impresoraNombre)) {
                impresora = s;
                break;
            }
        }
        if (impresora == null) {
            throw new PrintException("Impresora no encontrada: " + impresoraNombre);
        }

        byte[] abrirCaja = new byte[]{0x1B, 0x70, 0x00, 0x3C, (byte)0xFF}; // ESC p 0 <msb> <lsb>
        byte[] datos = contenido.getBytes();
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        try {
            out.write(datos);
            out.write(abrirCaja);
        } catch (IOException e) {
            e.printStackTrace();
        }

        DocPrintJob job = impresora.createPrintJob();
        DocFlavor flavor = DocFlavor.BYTE_ARRAY.AUTOSENSE;
        Doc doc = new SimpleDoc(out.toByteArray(), flavor, null);
        job.print(doc, null);
    }

    /** Centra un texto dentro de un ancho dado (rellena con espacios). */
    public static String center(String text, int width) {
        if (text.length() >= width) return text;
        int pad = (width - text.length()) / 2;
        return " ".repeat(pad) + text + " ".repeat(width - pad - text.length());
    }
}
