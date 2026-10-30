package cl.duoc.eventos.dto;

import java.time.Instant;

/**
 * Datos del archivo del resumen de compra guardado en S3.
 * La clave tiene la forma {numeroResumen}/resumen-compra-{numeroResumen}.pdf
 */
public record ArchivoS3Response(
        Long numeroResumen,
        String bucket,
        String carpeta,
        String clave,
        Long tamanoBytes,
        Instant ultimaModificacion) {
}
