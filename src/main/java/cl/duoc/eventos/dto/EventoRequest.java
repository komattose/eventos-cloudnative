package cl.duoc.eventos.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record EventoRequest(
        @NotBlank(message = "El nombre del evento es obligatorio")
        @Size(max = 150, message = "El nombre no puede superar los 150 caracteres")
        String nombre,

        @NotNull(message = "La fecha del evento es obligatoria")
        @FutureOrPresent(message = "La fecha del evento no puede estar en el pasado")
        LocalDate fecha,

        @NotNull(message = "El precio de la entrada es obligatorio")
        @DecimalMin(value = "0.0", inclusive = false, message = "El precio debe ser mayor que 0")
        BigDecimal precioEntrada) {
}
