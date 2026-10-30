package cl.duoc.eventos.service;

import cl.duoc.eventos.dto.EventoRequest;
import cl.duoc.eventos.dto.EventoResponse;
import cl.duoc.eventos.exception.RecursoNoEncontradoException;
import cl.duoc.eventos.model.Evento;
import cl.duoc.eventos.repository.EventoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
public class EventoService {

    private final EventoRepository eventoRepository;

    public EventoService(EventoRepository eventoRepository) {
        this.eventoRepository = eventoRepository;
    }

    /**
     * Eventos disponibles: los que aún no ocurren, ordenados por fecha.
     */
    @Transactional(readOnly = true)
    public List<EventoResponse> listarDisponibles() {
        return eventoRepository.findByFechaGreaterThanEqualOrderByFechaAsc(LocalDate.now()).stream()
                .map(EventoResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public EventoResponse obtener(Long id) {
        return EventoResponse.from(buscar(id));
    }

    @Transactional
    public EventoResponse crear(EventoRequest request) {
        Evento evento = new Evento(request.nombre().trim(), request.fecha(), request.precioEntrada());
        return EventoResponse.from(eventoRepository.save(evento));
    }

    Evento buscar(Long id) {
        return eventoRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe un evento con id " + id));
    }
}
