package cl.duoc.eventos;

import cl.duoc.eventos.repository.S3Repository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import software.amazon.awssdk.core.exception.SdkClientException;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Pruebas de la semana 3: tickets en EFS (carpeta temporal) y S3 (mock).
 */
@SpringBootTest(properties = {"aws.s3.bucket=bucket-pruebas", "efs.path=target/efs-pruebas"})
@AutoConfigureMockMvc
@ActiveProfiles("local")
class TicketTests {

    private static final String BUCKET = "bucket-pruebas";
    private static final Path EFS = Path.of("target/efs-pruebas");
    private static final byte[] PDF = "%PDF-1.4 ticket".getBytes(StandardCharsets.US_ASCII);

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper mapper;

    @MockBean
    private S3Repository s3Repository;

    private long eventoId;

    @BeforeEach
    void crearEvento() throws Exception {
        String evento = mvc.perform(post("/api/eventos").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\": \"Festival\", \"fecha\": \"" + LocalDate.now().plusDays(20)
                                + "\", \"precioEntrada\": 20000}"))
                .andReturn().getResponse().getContentAsString();
        eventoId = mapper.readTree(evento).get("id").asLong();
    }

    private JsonNode generar(String usuario, int cantidad) throws Exception {
        String respuesta = mvc.perform(post("/api/tickets").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"eventoId\": %d, \"usuario\": \"%s\", \"cantidad\": %d, \"nombreAsistente\": \"Martin\"}"
                                .formatted(eventoId, usuario, cantidad)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return mapper.readTree(respuesta);
    }

    private String clave(String usuario, long ticketId) {
        return "evento%d/%s/ticket%03d.pdf".formatted(eventoId, usuario, ticketId);
    }

    @Test
    void generaLosTicketsEnElEfsYLosSubeAS3PorEventoYUsuario() throws Exception {
        JsonNode tickets = generar("usuarioX", 2);

        assertThat(tickets).hasSize(2);
        for (JsonNode ticket : tickets) {
            String clave = clave("usuarioX", ticket.get("id").asLong());
            assertThat(ticket.get("estado").asText()).isEqualTo("SUBIDO");
            assertThat(ticket.get("claveS3").asText()).isEqualTo(clave);
            assertThat(Files.readAllBytes(EFS.resolve(clave))).startsWith("%PDF-".getBytes());
            verify(s3Repository).guardar(eq(BUCKET), eq(clave), any(byte[].class), eq("application/pdf"));
        }
    }

    @Test
    void siS3FallaLosTicketsQuedanEnElEfsYSePuedenSubirDespues() throws Exception {
        doThrow(SdkClientException.create("sin conexión")).when(s3Repository).guardar(any(), any(), any(), any());
        JsonNode ticket = generar("usuarioSinRed", 1).get(0);
        assertThat(ticket.get("estado").asText()).isEqualTo("GENERADO");

        reset(s3Repository);
        mvc.perform(post("/api/tickets/s3").param("eventoId", String.valueOf(eventoId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].estado").value("SUBIDO"));
        verify(s3Repository).guardar(eq(BUCKET), eq(clave("usuarioSinRed", ticket.get("id").asLong())),
                any(byte[].class), eq("application/pdf"));
    }

    @Test
    void descargaUnTicketYLosDeUnUsuarioEnZip() throws Exception {
        JsonNode tickets = generar("usuarioZip", 2);
        when(s3Repository.obtener(eq(BUCKET), anyString())).thenReturn(PDF);
        long id = tickets.get(0).get("id").asLong();

        byte[] pdf = mvc.perform(get("/api/tickets/{id}/descargar", id))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "application/pdf"))
                .andReturn().getResponse().getContentAsByteArray();
        assertThat(pdf).isEqualTo(PDF);

        byte[] zip = mvc.perform(get("/api/tickets/descargar").param("usuario", "usuarioZip"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "application/zip"))
                .andReturn().getResponse().getContentAsByteArray();
        List<String> entradas = new ArrayList<>();
        try (ZipInputStream lector = new ZipInputStream(new ByteArrayInputStream(zip))) {
            for (ZipEntry entrada; (entrada = lector.getNextEntry()) != null; ) {
                entradas.add(entrada.getName());
            }
        }
        assertThat(entradas).containsExactly(clave("usuarioZip", id), clave("usuarioZip", tickets.get(1).get("id").asLong()));

        mvc.perform(get("/api/tickets/descargar")).andExpect(status().isBadRequest());
    }

    @Test
    void modificaElTicketYActualizaElArchivoEnS3() throws Exception {
        long id = generar("usuarioMod", 1).get(0).get("id").asLong();

        mvc.perform(put("/api/tickets/{id}", id).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombreAsistente\": \"Ana Pérez\", \"zona\": \"vip\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombreAsistente").value("Ana Pérez"))
                .andExpect(jsonPath("$.zona").value("VIP"))
                .andExpect(jsonPath("$.estado").value("SUBIDO"));

        verify(s3Repository, times(2)).guardar(eq(BUCKET), eq(clave("usuarioMod", id)), any(byte[].class), eq("application/pdf"));
        mvc.perform(put("/api/tickets/{id}", id).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void eliminaUnTicketEspecificoDeS3YDelEfs() throws Exception {
        long id = generar("usuarioDel", 1).get(0).get("id").asLong();
        String clave = clave("usuarioDel", id);

        mvc.perform(delete("/api/tickets/{id}", id)).andExpect(status().isNoContent());

        verify(s3Repository).eliminar(BUCKET, clave);
        assertThat(EFS.resolve(clave)).doesNotExist();
        mvc.perform(get("/api/tickets/{id}", id)).andExpect(jsonPath("$.estado").value("ELIMINADO"));
        mvc.perform(delete("/api/tickets/{id}", id)).andExpect(status().isConflict());
        mvc.perform(delete("/api/tickets/999999")).andExpect(status().isNotFound());
    }

    @Test
    void reportaEstadisticasYConsultaElHistorial() throws Exception {
        JsonNode tickets = generar("usuarioEst", 3);
        generar("otroUsuario", 1);
        mvc.perform(delete("/api/tickets/{id}", tickets.get(0).get("id").asLong()));

        mvc.perform(get("/api/estadisticas/eventos/{id}", eventoId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reservas").value(2))
                .andExpect(jsonPath("$.entradasVendidas").value(4))
                .andExpect(jsonPath("$.recaudacion").value(80000.0))
                .andExpect(jsonPath("$.ticketsGenerados").value(4))
                .andExpect(jsonPath("$.ticketsVigentes").value(3))
                .andExpect(jsonPath("$.ticketsEnS3").value(3))
                .andExpect(jsonPath("$.ticketsEliminados").value(1))
                .andExpect(jsonPath("$.usuarios").value(2));

        mvc.perform(get("/api/tickets/historial").param("eventoId", String.valueOf(eventoId))
                        .param("accion", "GENERADO_EFS"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(4));
        mvc.perform(get("/api/tickets/historial").param("usuario", "usuarioEst").param("accion", "ELIMINADO"))
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[*].almacenamiento").value(org.hamcrest.Matchers.containsInAnyOrder("S3", "EFS")));
        mvc.perform(get("/api/tickets/historial").param("eventoId", String.valueOf(eventoId))
                        .param("desde", LocalDate.now().plusDays(1).toString()))
                .andExpect(jsonPath("$.length()").value(0));
        mvc.perform(get("/api/tickets/historial").param("accion", "INVENTADA"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void validaLosDatosDeLaReserva() throws Exception {
        mvc.perform(post("/api/tickets").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"eventoId\": %d, \"usuario\": \"con espacio\", \"cantidad\": 0}".formatted(eventoId)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.usuario").exists())
                .andExpect(jsonPath("$.errores.cantidad").exists());
        mvc.perform(post("/api/tickets").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"eventoId\": 999999, \"usuario\": \"usuario1\", \"cantidad\": 1}"))
                .andExpect(status().isNotFound());
    }
}
