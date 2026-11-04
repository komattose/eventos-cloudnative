package cl.duoc.eventos.dto;

import cl.duoc.eventos.model.EstadoTicket;
import cl.duoc.eventos.model.Ticket;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record TicketResponse(
        Long id,
        String codigo,
        Long numeroReserva,
        Long eventoId,
        String evento,
        LocalDate fechaEvento,
        String usuario,
        String nombreAsistente,
        String zona,
        EstadoTicket estado,
        String rutaEfs,
        String claveS3,
        LocalDateTime fechaGeneracion,
        LocalDateTime fechaActualizacion) {

    public static TicketResponse from(Ticket ticket) {
        return new TicketResponse(
                ticket.getId(),
                ticket.getCodigo(),
                ticket.getCompra().getId(),
                ticket.getEvento().getId(),
                ticket.getEvento().getNombre(),
                ticket.getEvento().getFecha(),
                ticket.getUsuario(),
                ticket.getNombreAsistente(),
                ticket.getZona(),
                ticket.getEstado(),
                ticket.getRutaEfs(),
                ticket.getClaveS3(),
                ticket.getFechaGeneracion(),
                ticket.getFechaActualizacion());
    }
}
