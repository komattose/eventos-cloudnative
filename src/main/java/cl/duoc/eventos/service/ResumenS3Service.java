package cl.duoc.eventos.service;

import cl.duoc.eventos.dto.ArchivoS3Response;
import cl.duoc.eventos.dto.ResumenCompra;
import cl.duoc.eventos.exception.ArchivoInvalidoException;
import cl.duoc.eventos.exception.RecursoNoEncontradoException;
import cl.duoc.eventos.repository.S3Repository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 * Gestiona el archivo del resumen de compra en S3.
 * Cada resumen se guarda en una carpeta con su número: {numeroResumen}/resumen-compra-{numeroResumen}.pdf
 */
@Service
public class ResumenS3Service {

    static final String TIPO_PDF = "application/pdf";
    private static final byte[] FIRMA_PDF = "%PDF-".getBytes();

    private final S3Repository s3Repository;
    private final CompraService compraService;
    private final ResumenPdfService resumenPdfService;
    private final String bucket;

    public ResumenS3Service(S3Repository s3Repository, CompraService compraService,
                            ResumenPdfService resumenPdfService, @Value("${aws.s3.bucket:}") String bucket) {
        this.s3Repository = s3Repository;
        this.compraService = compraService;
        this.resumenPdfService = resumenPdfService;
        this.bucket = bucket;
    }

    /**
     * Genera el PDF del resumen de compra (sin subirlo a S3).
     */
    public byte[] generarPdf(Long numeroResumen) {
        return resumenPdfService.generar(compraService.obtenerResumen(numeroResumen));
    }

    public String nombreArchivo(Long numeroResumen) {
        return resumenPdfService.nombreArchivo(numeroResumen);
    }

    /**
     * Genera el resumen y lo sube a su carpeta en el bucket. Si ya existe, se debe usar actualizar.
     */
    public ArchivoS3Response subir(Long numeroResumen) {
        ResumenCompra resumen = compraService.obtenerResumen(numeroResumen);
        String clave = clave(numeroResumen);
        if (s3Repository.metadatos(bucket(), clave).isPresent()) {
            throw new IllegalStateException("El resumen " + numeroResumen
                    + " ya está en S3. Usa PUT para modificarlo o DELETE para borrarlo.");
        }
        s3Repository.guardar(bucket(), clave, resumenPdfService.generar(resumen), TIPO_PDF);
        return info(numeroResumen);
    }

    /**
     * Reemplaza el archivo existente en S3. Si se envía un PDF corregido se sube ese;
     * si no, se vuelve a generar con los datos actuales de la compra.
     */
    public ArchivoS3Response actualizar(Long numeroResumen, MultipartFile archivo) {
        String clave = claveExistente(numeroResumen);
        byte[] contenido = archivo == null || archivo.isEmpty()
                ? generarPdf(numeroResumen)
                : leerPdf(archivo);
        s3Repository.guardar(bucket(), clave, contenido, TIPO_PDF);
        return info(numeroResumen);
    }

    public byte[] descargar(Long numeroResumen) {
        return s3Repository.obtener(bucket(), claveExistente(numeroResumen));
    }

    public void eliminar(Long numeroResumen) {
        s3Repository.eliminar(bucket(), claveExistente(numeroResumen));
    }

    public ArchivoS3Response info(Long numeroResumen) {
        String clave = clave(numeroResumen);
        HeadObjectResponse metadatos = s3Repository.metadatos(bucket(), clave)
                .orElseThrow(() -> noEstaEnS3(numeroResumen));
        return new ArchivoS3Response(numeroResumen, bucket(), carpeta(numeroResumen), clave,
                metadatos.contentLength(), metadatos.lastModified());
    }

    /**
     * Archivos que hay dentro de la carpeta del resumen en el bucket.
     */
    public List<String> listarCarpeta(Long numeroResumen) {
        compraService.obtenerResumen(numeroResumen);
        return s3Repository.listar(bucket(), carpeta(numeroResumen)).stream()
                .map(objeto -> objeto.key())
                .toList();
    }

    private String claveExistente(Long numeroResumen) {
        compraService.obtenerResumen(numeroResumen);
        String clave = clave(numeroResumen);
        if (s3Repository.metadatos(bucket(), clave).isEmpty()) {
            throw noEstaEnS3(numeroResumen);
        }
        return clave;
    }

    private static RecursoNoEncontradoException noEstaEnS3(Long numeroResumen) {
        return new RecursoNoEncontradoException("El resumen " + numeroResumen + " no está guardado en S3");
    }

    private String carpeta(Long numeroResumen) {
        return numeroResumen + "/";
    }

    private String clave(Long numeroResumen) {
        return carpeta(numeroResumen) + resumenPdfService.nombreArchivo(numeroResumen);
    }

    private String bucket() {
        if (!StringUtils.hasText(bucket)) {
            throw new IllegalStateException("No hay un bucket de S3 configurado (variable de entorno S3_BUCKET)");
        }
        return bucket;
    }

    private static byte[] leerPdf(MultipartFile archivo) {
        try {
            byte[] contenido = archivo.getBytes();
            boolean esPdf = contenido.length > FIRMA_PDF.length
                    && Arrays.equals(Arrays.copyOf(contenido, FIRMA_PDF.length), FIRMA_PDF);
            if (!esPdf) {
                throw new ArchivoInvalidoException("El archivo enviado no es un PDF válido");
            }
            return contenido;
        } catch (IOException e) {
            throw new ArchivoInvalidoException("No se pudo leer el archivo enviado");
        }
    }
}
