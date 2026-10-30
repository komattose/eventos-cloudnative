package cl.duoc.eventos;

import cl.duoc.eventos.repository.S3Repository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Pruebas de la semana 2. S3 se reemplaza por un mock para no depender de AWS.
 */
@SpringBootTest(properties = "aws.s3.bucket=bucket-pruebas")
@AutoConfigureMockMvc
@ActiveProfiles("local")
class ResumenArchivoTests {

    private static final String BUCKET = "bucket-pruebas";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper mapper;

    @MockBean
    private S3Repository s3Repository;

    private long numeroResumen;
    private String clave;

    @BeforeEach
    void crearCompra() throws Exception {
        String evento = mvc.perform(post("/api/eventos").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\": \"Teatro\", \"fecha\": \"" + LocalDate.now().plusDays(5)
                                + "\", \"precioEntrada\": 12500}"))
                .andReturn().getResponse().getContentAsString();
        long eventoId = mapper.readTree(evento).get("id").asLong();
        String compra = mvc.perform(post("/api/compras").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"eventoId\": " + eventoId + ", \"cantidadEntradas\": 2}"))
                .andReturn().getResponse().getContentAsString();
        numeroResumen = mapper.readTree(compra).get("numeroResumen").asLong();
        clave = numeroResumen + "/resumen-compra-" + numeroResumen + ".pdf";
    }

    private static Optional<HeadObjectResponse> existe() {
        return Optional.of(HeadObjectResponse.builder().contentLength(1234L).lastModified(Instant.now()).build());
    }

    @Test
    void generaElResumenComoPdfDescargable() throws Exception {
        byte[] pdf = mvc.perform(get("/api/compras/{n}/resumen/pdf", numeroResumen))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "application/pdf"))
                .andExpect(header().string("Content-Disposition",
                        "attachment; filename=\"resumen-compra-" + numeroResumen + ".pdf\""))
                .andReturn().getResponse().getContentAsByteArray();

        assertThat(new String(pdf, 0, 5, StandardCharsets.US_ASCII)).isEqualTo("%PDF-");
    }

    @Test
    void subeElResumenEnUnaCarpetaConSuNumero() throws Exception {
        when(s3Repository.metadatos(BUCKET, clave)).thenReturn(Optional.empty(), existe());

        mvc.perform(post("/api/compras/{n}/resumen/s3", numeroResumen))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.carpeta").value(numeroResumen + "/"))
                .andExpect(jsonPath("$.clave").value(clave))
                .andExpect(jsonPath("$.bucket").value(BUCKET));

        verify(s3Repository).guardar(eq(BUCKET), eq(clave), any(byte[].class), eq("application/pdf"));
    }

    @Test
    void noSubeDosVecesElMismoResumen() throws Exception {
        when(s3Repository.metadatos(BUCKET, clave)).thenReturn(existe());

        mvc.perform(post("/api/compras/{n}/resumen/s3", numeroResumen))
                .andExpect(status().isConflict());
        verify(s3Repository, never()).guardar(any(), any(), any(), any());
    }

    @Test
    void modificaElArchivoConUnPdfCorregido() throws Exception {
        when(s3Repository.metadatos(BUCKET, clave)).thenReturn(existe());
        byte[] corregido = "%PDF-1.4 corregido".getBytes(StandardCharsets.US_ASCII);

        mvc.perform(multipart(HttpMethod.PUT, "/api/compras/{n}/resumen/s3", numeroResumen)
                        .file(new MockMultipartFile("archivo", "corregido.pdf", "application/pdf", corregido)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.clave").value(clave));

        verify(s3Repository).guardar(BUCKET, clave, corregido, "application/pdf");
    }

    @Test
    void modificaRegenerandoSiNoSeEnviaArchivo() throws Exception {
        when(s3Repository.metadatos(BUCKET, clave)).thenReturn(existe());

        mvc.perform(put("/api/compras/{n}/resumen/s3", numeroResumen))
                .andExpect(status().isOk());

        verify(s3Repository).guardar(eq(BUCKET), eq(clave), any(byte[].class), eq("application/pdf"));
    }

    @Test
    void rechazaArchivosQueNoSonPdf() throws Exception {
        when(s3Repository.metadatos(BUCKET, clave)).thenReturn(existe());

        mvc.perform(multipart(HttpMethod.PUT, "/api/compras/{n}/resumen/s3", numeroResumen)
                        .file(new MockMultipartFile("archivo", "nota.txt", "text/plain", "hola".getBytes())))
                .andExpect(status().isBadRequest());
        verify(s3Repository, never()).guardar(any(), any(), any(), any());
    }

    @Test
    void descargaElArchivoDesdeS3() throws Exception {
        byte[] contenido = "%PDF-1.4 guardado".getBytes(StandardCharsets.US_ASCII);
        when(s3Repository.metadatos(BUCKET, clave)).thenReturn(existe());
        when(s3Repository.obtener(BUCKET, clave)).thenReturn(contenido);

        byte[] descargado = mvc.perform(get("/api/compras/{n}/resumen/s3", numeroResumen))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "application/pdf"))
                .andReturn().getResponse().getContentAsByteArray();

        assertThat(descargado).isEqualTo(contenido);
    }

    @Test
    void borraElArchivoValidandoQueExista() throws Exception {
        when(s3Repository.metadatos(BUCKET, clave)).thenReturn(existe());
        mvc.perform(delete("/api/compras/{n}/resumen/s3", numeroResumen))
                .andExpect(status().isNoContent());
        verify(s3Repository).eliminar(BUCKET, clave);
    }

    @Test
    void responde404SiElArchivoNoEstaEnS3() throws Exception {
        when(s3Repository.metadatos(BUCKET, clave)).thenReturn(Optional.empty());

        mvc.perform(get("/api/compras/{n}/resumen/s3", numeroResumen)).andExpect(status().isNotFound());
        mvc.perform(delete("/api/compras/{n}/resumen/s3", numeroResumen)).andExpect(status().isNotFound());
        mvc.perform(put("/api/compras/{n}/resumen/s3", numeroResumen)).andExpect(status().isNotFound());
        verify(s3Repository, never()).eliminar(any(), any());
    }

    @Test
    void responde404SiLaCompraNoExiste() throws Exception {
        mvc.perform(post("/api/compras/999999/resumen/s3"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("No existe un resumen de compra con número 999999"));
    }
}
