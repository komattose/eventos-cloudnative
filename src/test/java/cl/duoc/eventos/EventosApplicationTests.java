package cl.duoc.eventos;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("local")
class EventosApplicationTests {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper mapper;

    private long crearEvento(String nombre, LocalDate fecha, String precio) throws Exception {
        String body = """
                {"nombre": "%s", "fecha": "%s", "precioEntrada": %s}
                """.formatted(nombre, fecha, precio);
        String respuesta = mvc.perform(post("/api/eventos").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return mapper.readTree(respuesta).get("id").asLong();
    }

    @Test
    void creaEventoYLoListaComoDisponible() throws Exception {
        crearEvento("Concierto de prueba", LocalDate.now().plusDays(10), "25000");

        mvc.perform(get("/api/eventos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.nombre == 'Concierto de prueba')].precioEntrada").value(25000.0));
    }

    @Test
    void compraEntradasYDevuelveResumen() throws Exception {
        long eventoId = crearEvento("Festival", LocalDate.now().plusDays(30), "15990.50");

        String respuesta = mvc.perform(post("/api/compras").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"eventoId\": " + eventoId + ", \"cantidadEntradas\": 3}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.evento").value("Festival"))
                .andExpect(jsonPath("$.cantidadEntradas").value(3))
                .andExpect(jsonPath("$.precioUnitario").value(15990.50))
                .andExpect(jsonPath("$.precioTotal").value(47971.50))
                .andReturn().getResponse().getContentAsString();

        JsonNode resumen = mapper.readTree(respuesta);
        mvc.perform(get("/api/compras/" + resumen.get("numeroResumen").asLong()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.precioTotal").value(47971.50));
    }

    @Test
    void rechazaDatosInvalidos() throws Exception {
        mvc.perform(post("/api/eventos").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\": \"\", \"fecha\": \"2020-01-01\", \"precioEntrada\": -5}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.nombre").exists())
                .andExpect(jsonPath("$.errores.fecha").exists())
                .andExpect(jsonPath("$.errores.precioEntrada").exists());

        mvc.perform(post("/api/compras").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"eventoId\": 999999, \"cantidadEntradas\": 2}"))
                .andExpect(status().isNotFound());
    }
}
