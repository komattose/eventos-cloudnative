package cl.duoc.eventos.dto;

import cl.duoc.eventos.model.Compra;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Resumen que se devuelve al comprar entradas. numeroResumen es el id de la compra.
 */
public record ResumenCompra(
        Long numeroResumen,
        Long eventoId,
        String evento,
        LocalDate fechaEvento,
        Integer cantidadEntradas,
        BigDecimal precioUnitario,
        BigDecimal precioTotal,
        LocalDateTime fechaCompra) {

    public static ResumenCompra from(Compra compra) {
        return new ResumenCompra(
                compra.getId(),
                compra.getEvento().getId(),
                compra.getEvento().getNombre(),
                compra.getEvento().getFecha(),
                compra.getCantidadEntradas(),
                compra.getPrecioUnitario(),
                compra.getPrecioTotal(),
                compra.getFechaCompra());
    }
}
