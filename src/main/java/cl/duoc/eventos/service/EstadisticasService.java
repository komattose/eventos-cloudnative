package cl.duoc.eventos.service;

import cl.duoc.eventos.dto.EstadisticasEvento;
import cl.duoc.eventos.model.Compra;
import cl.duoc.eventos.model.EstadoTicket;
import cl.duoc.eventos.model.Evento;
import cl.duoc.eventos.model.Ticket;
import cl.duoc.eventos.repository.CompraRepository;
import cl.duoc.eventos.repository.EventoRepository;
import cl.duoc.eventos.repository.TicketRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;

/**
 * Estadísticas de ventas y reservas por evento.
 */
@Service
public class EstadisticasService {

    private final EventoService eventoService;
    private final EventoRepository eventoRepository;
    private final CompraRepository compraRepository;
    private final TicketRepository ticketRepository;

    public EstadisticasService(EventoService eventoService, EventoRepository eventoRepository,
                               CompraRepository compraRepository, TicketRepository ticketRepository) {
        this.eventoService = eventoService;
        this.eventoRepository = eventoRepository;
        this.compraRepository = compraRepository;
        this.ticketRepository = ticketRepository;
    }

    @Transactional(readOnly = true)
    public EstadisticasEvento porEvento(Long eventoId) {
        return calcular(eventoService.buscar(eventoId));
    }

    /**
     * Estadísticas de todos los eventos, ordenadas por recaudación (mayor primero).
     */
    @Transactional(readOnly = true)
    public List<EstadisticasEvento> todos() {
        return eventoRepository.findAll().stream()
                .map(this::calcular)
                .sorted(Comparator.comparing(EstadisticasEvento::recaudacion).reversed()
                        .thenComparing(EstadisticasEvento::eventoId))
                .toList();
    }

    private EstadisticasEvento calcular(Evento evento) {
        List<Compra> reservas = compraRepository.findByEventoId(evento.getId());
        List<Ticket> tickets = ticketRepository.findByEventoId(evento.getId());

        long entradas = reservas.stream().mapToLong(Compra::getCantidadEntradas).sum();
        BigDecimal recaudacion = reservas.stream().map(Compra::getPrecioTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
        long eliminados = contar(tickets, EstadoTicket.ELIMINADO);
        long usuarios = tickets.stream().map(Ticket::getUsuario).distinct().count();

        return new EstadisticasEvento(evento.getId(), evento.getNombre(), evento.getFecha(),
                reservas.size(), entradas, recaudacion,
                tickets.size(), tickets.size() - eliminados, contar(tickets, EstadoTicket.SUBIDO), eliminados,
                usuarios);
    }

    private static long contar(List<Ticket> tickets, EstadoTicket estado) {
        return tickets.stream().filter(t -> t.getEstado() == estado).count();
    }
}
