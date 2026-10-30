package cl.duoc.eventos.service;

import cl.duoc.eventos.dto.ResumenCompra;
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Genera el archivo PDF con el resumen de una compra de entradas.
 */
@Service
public class ResumenPdfService {

    private static final Locale CHILE = Locale.forLanguageTag("es-CL");
    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd-MM-yyyy");
    private static final DateTimeFormatter FECHA_HORA = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss");
    private static final Color GRIS = new Color(235, 235, 235);

    public byte[] generar(ResumenCompra resumen) {
        ByteArrayOutputStream salida = new ByteArrayOutputStream();
        Document documento = new Document(PageSize.A4, 50, 50, 60, 50);
        PdfWriter.getInstance(documento, salida);
        documento.addTitle("Resumen de compra N° " + resumen.numeroResumen());
        documento.open();

        Font titulo = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18);
        Font subtitulo = FontFactory.getFont(FontFactory.HELVETICA, 11, Color.DARK_GRAY);

        Paragraph encabezado = new Paragraph("Resumen de compra N° " + resumen.numeroResumen(), titulo);
        encabezado.setAlignment(Element.ALIGN_CENTER);
        documento.add(encabezado);
        Paragraph empresa = new Paragraph("Sistema de registro y venta de entradas", subtitulo);
        empresa.setAlignment(Element.ALIGN_CENTER);
        empresa.setSpacingAfter(24);
        documento.add(empresa);

        PdfPTable tabla = new PdfPTable(new float[]{2, 3});
        tabla.setWidthPercentage(100);
        fila(tabla, "Evento", resumen.evento());
        fila(tabla, "Fecha del evento", resumen.fechaEvento().format(FECHA));
        fila(tabla, "Cantidad de entradas", String.valueOf(resumen.cantidadEntradas()));
        fila(tabla, "Precio unitario", pesos(resumen.precioUnitario()));
        fila(tabla, "Precio total", pesos(resumen.precioTotal()));
        fila(tabla, "Fecha de compra", resumen.fechaCompra().format(FECHA_HORA));
        documento.add(tabla);

        documento.close();
        return salida.toByteArray();
    }

    public String nombreArchivo(Long numeroResumen) {
        return "resumen-compra-" + numeroResumen + ".pdf";
    }

    private static void fila(PdfPTable tabla, String etiqueta, String valor) {
        PdfPCell celdaEtiqueta = new PdfPCell(new Phrase(etiqueta, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11)));
        celdaEtiqueta.setBackgroundColor(GRIS);
        celdaEtiqueta.setPadding(8);
        PdfPCell celdaValor = new PdfPCell(new Phrase(valor, FontFactory.getFont(FontFactory.HELVETICA, 11)));
        celdaValor.setPadding(8);
        tabla.addCell(celdaEtiqueta);
        tabla.addCell(celdaValor);
    }

    private static String pesos(BigDecimal monto) {
        NumberFormat formato = NumberFormat.getCurrencyInstance(CHILE);
        formato.setMaximumFractionDigits(monto.stripTrailingZeros().scale() > 0 ? 2 : 0);
        return formato.format(monto);
    }
}
