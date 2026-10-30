package cl.duoc.eventos.controller;

import cl.duoc.eventos.dto.EventoRequest;
import cl.duoc.eventos.dto.EventoResponse;
import cl.duoc.eventos.service.EventoService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/eventos")
public class EventoController {

    private final EventoService eventoService;

    public EventoController(EventoService eventoService) {
        this.eventoService = eventoService;
    }

    @GetMapping
    public List<EventoResponse> listarDisponibles() {
        return eventoService.listarDisponibles();
    }

    @GetMapping("/{id}")
    public EventoResponse obtener(@PathVariable Long id) {
        return eventoService.obtener(id);
    }

    @PostMapping
    public ResponseEntity<EventoResponse> crear(@Valid @RequestBody EventoRequest request) {
        EventoResponse creado = eventoService.crear(request);
        URI ubicacion = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(creado.id()).toUri();
        return ResponseEntity.created(ubicacion).body(creado);
    }
}
