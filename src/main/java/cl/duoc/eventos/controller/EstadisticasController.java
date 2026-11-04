package cl.duoc.eventos.controller;

import cl.duoc.eventos.dto.EstadisticasEvento;
import cl.duoc.eventos.service.EstadisticasService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Reporte de estadísticas de ventas y reservas por evento.
 */
@RestController
@RequestMapping("/api/estadisticas")
public class EstadisticasController {

    private final EstadisticasService estadisticasService;

    public EstadisticasController(EstadisticasService estadisticasService) {
        this.estadisticasService = estadisticasService;
    }

    @GetMapping
    public List<EstadisticasEvento> todos() {
        return estadisticasService.todos();
    }

    @GetMapping("/eventos/{eventoId}")
    public EstadisticasEvento porEvento(@PathVariable Long eventoId) {
        return estadisticasService.porEvento(eventoId);
    }
}
