package cl.duoc.eventos.dto;

import cl.duoc.eventos.model.Evento;

import java.math.BigDecimal;
import java.time.LocalDate;

public record EventoResponse(Long id, String nombre, LocalDate fecha, BigDecimal precioEntrada) {

    public static EventoResponse from(Evento evento) {
        return new EventoResponse(evento.getId(), evento.getNombre(), evento.getFecha(), evento.getPrecioEntrada());
    }
}
