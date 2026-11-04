package cl.duoc.eventos.service;

import cl.duoc.eventos.model.Ticket;
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Genera el PDF de un ticket personalizado.
 */
@Service
public class TicketPdfService {

    private static final Locale CHILE = Locale.forLanguageTag("es-CL");
    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd-MM-yyyy");
    private static final DateTimeFormatter FECHA_HORA = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss");
    private static final Color AZUL = new Color(22, 60, 110);
    private static final Color GRIS = new Color(235, 235, 235);

    public byte[] generar(Ticket ticket) {
        ByteArrayOutputStream salida = new ByteArrayOutputStream();
        Document documento = new Document(PageSize.A5.rotate(), 36, 36, 36, 36);
        PdfWriter.getInstance(documento, salida);
        documento.addTitle("Ticket N° " + ticket.getId());
        documento.open();

        PdfPTable cabecera = new PdfPTable(1);
        cabecera.setWidthPercentage(100);
        PdfPCell celda = new PdfPCell();
        celda.setBackgroundColor(AZUL);
        celda.setBorder(Rectangle.NO_BORDER);
        celda.setPadding(12);
        Paragraph nombreEvento = new Paragraph(ticket.getEvento().getNombre(),
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, Color.WHITE));
        nombreEvento.setAlignment(Element.ALIGN_CENTER);
        celda.addElement(nombreEvento);
        Paragraph numero = new Paragraph("Ticket N° " + ticket.getId() + "  ·  " + ticket.getEvento().getFecha().format(FECHA),
                FontFactory.getFont(FontFactory.HELVETICA, 11, Color.WHITE));
        numero.setAlignment(Element.ALIGN_CENTER);
        celda.addElement(numero);
        cabecera.addCell(celda);
        cabecera.setSpacingAfter(14);
        documento.add(cabecera);

        PdfPTable tabla = new PdfPTable(new float[]{2, 3});
        tabla.setWidthPercentage(100);
        fila(tabla, "Asistente", ticket.getNombreAsistente());
        fila(tabla, "Usuario", ticket.getUsuario());
        fila(tabla, "Zona", ticket.getZona());
        fila(tabla, "Precio", NumberFormat.getCurrencyInstance(CHILE).format(ticket.getCompra().getPrecioUnitario()));
        fila(tabla, "N° de reserva", String.valueOf(ticket.getCompra().getId()));
        fila(tabla, "Código", ticket.getCodigo());
        fila(tabla, "Emitido / actualizado", ticket.getFechaActualizacion().format(FECHA_HORA));
        documento.add(tabla);

        Paragraph pie = new Paragraph("Presenta este ticket y su código en el acceso al evento.",
                FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 9, Color.DARK_GRAY));
        pie.setSpacingBefore(10);
        pie.setAlignment(Element.ALIGN_CENTER);
        documento.add(pie);

        documento.close();
        return salida.toByteArray();
    }

    private static void fila(PdfPTable tabla, String etiqueta, String valor) {
        PdfPCell celdaEtiqueta = new PdfPCell(new Phrase(etiqueta, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10)));
        celdaEtiqueta.setBackgroundColor(GRIS);
        celdaEtiqueta.setPadding(6);
        PdfPCell celdaValor = new PdfPCell(new Phrase(valor, FontFactory.getFont(FontFactory.HELVETICA, 10)));
        celdaValor.setPadding(6);
        tabla.addCell(celdaEtiqueta);
        tabla.addCell(celdaValor);
    }
}
