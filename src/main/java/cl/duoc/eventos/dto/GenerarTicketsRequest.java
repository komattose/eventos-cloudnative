package cl.duoc.eventos.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Reserva de entradas para un usuario: genera un ticket por cada entrada.
 * nombreAsistente y zona son opcionales (por defecto el usuario y "GENERAL").
 */
public record GenerarTicketsRequest(
        @NotNull(message = "Debe indicar el evento")
        Long eventoId,

        @NotBlank(message = "Debe indicar el usuario")
        @Pattern(regexp = "^[A-Za-z0-9._@-]{3,50}$",
                message = "El usuario debe tener entre 3 y 50 caracteres (letras, números, punto, guion, guion bajo o @)")
        String usuario,

        @NotNull(message = "Debe indicar la cantidad de tickets")
        @Min(value = 1, message = "Debe reservar al menos 1 ticket")
        @Max(value = 20, message = "No se pueden reservar más de 20 tickets por reserva")
        Integer cantidad,

        @Size(max = 100, message = "El nombre del asistente no puede superar los 100 caracteres")
        String nombreAsistente,

        @Size(max = 30, message = "La zona no puede superar los 30 caracteres")
        String zona) {
}
