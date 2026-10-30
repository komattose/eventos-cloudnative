package cl.duoc.eventos.controller;

import cl.duoc.eventos.dto.ArchivoS3Response;
import cl.duoc.eventos.service.ResumenS3Service;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.util.List;

/**
 * Archivo del resumen de compra: generación en PDF y gestión en el bucket S3.
 */
@RestController
@RequestMapping("/api/compras/{numeroResumen}/resumen")
public class ResumenArchivoController {

    private final ResumenS3Service resumenS3Service;

    public ResumenArchivoController(ResumenS3Service resumenS3Service) {
        this.resumenS3Service = resumenS3Service;
    }

    /** Genera el resumen como archivo PDF para guardarlo en el computador. */
    @GetMapping("/pdf")
    public ResponseEntity<byte[]> generarPdf(@PathVariable Long numeroResumen) {
        return pdf(numeroResumen, resumenS3Service.generarPdf(numeroResumen));
    }

    /** Sube el resumen al bucket, en la carpeta {numeroResumen}/. */
    @PostMapping("/s3")
    public ResponseEntity<ArchivoS3Response> subir(@PathVariable Long numeroResumen) {
        ArchivoS3Response archivo = resumenS3Service.subir(numeroResumen);
        return ResponseEntity.created(ServletUriComponentsBuilder.fromCurrentRequest().build().toUri()).body(archivo);
    }

    /** Modifica el archivo en S3: con un PDF corregido (campo "archivo") o regenerándolo si no se envía. */
    @PutMapping(value = "/s3", consumes = {MediaType.MULTIPART_FORM_DATA_VALUE, MediaType.ALL_VALUE})
    public ArchivoS3Response modificar(@PathVariable Long numeroResumen,
                                       @RequestPart(name = "archivo", required = false) MultipartFile archivo) {
        return resumenS3Service.actualizar(numeroResumen, archivo);
    }

    /** Descarga el archivo guardado en S3. */
    @GetMapping("/s3")
    public ResponseEntity<byte[]> descargar(@PathVariable Long numeroResumen) {
        return pdf(numeroResumen, resumenS3Service.descargar(numeroResumen));
    }

    /** Borra el archivo de S3 (valida antes que exista). */
    @DeleteMapping("/s3")
    public ResponseEntity<Void> eliminar(@PathVariable Long numeroResumen) {
        resumenS3Service.eliminar(numeroResumen);
        return ResponseEntity.noContent().build();
    }

    /** Datos del archivo en S3 (bucket, carpeta, clave, tamaño y fecha). */
    @GetMapping("/s3/info")
    public ArchivoS3Response info(@PathVariable Long numeroResumen) {
        return resumenS3Service.info(numeroResumen);
    }

    /** Objetos dentro de la carpeta del resumen en el bucket. */
    @GetMapping("/s3/carpeta")
    public List<String> carpeta(@PathVariable Long numeroResumen) {
        return resumenS3Service.listarCarpeta(numeroResumen);
    }

    private ResponseEntity<byte[]> pdf(Long numeroResumen, byte[] contenido) {
        HttpHeaders cabeceras = new HttpHeaders();
        cabeceras.setContentType(MediaType.APPLICATION_PDF);
        cabeceras.setContentDisposition(ContentDisposition.attachment()
                .filename(resumenS3Service.nombreArchivo(numeroResumen)).build());
        return ResponseEntity.ok().headers(cabeceras).body(contenido);
    }
}
