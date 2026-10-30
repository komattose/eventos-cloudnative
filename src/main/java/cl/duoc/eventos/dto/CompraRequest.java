package cl.duoc.eventos.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record CompraRequest(
        @NotNull(message = "Debe indicar el evento")
        Long eventoId,

        @NotNull(message = "Debe indicar la cantidad de entradas")
        @Min(value = 1, message = "Debe comprar al menos 1 entrada")
        @Max(value = 20, message = "No se pueden comprar más de 20 entradas por compra")
        Integer cantidadEntradas) {
}
