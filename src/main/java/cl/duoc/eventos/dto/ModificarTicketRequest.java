package cl.duoc.eventos.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Detalles del ticket que se pueden corregir. Los campos que no se envían no cambian.
 */
public record ModificarTicketRequest(
        @Size(min = 1, max = 100, message = "El nombre del asistente debe tener entre 1 y 100 caracteres")
        @Pattern(regexp = ".*\\S.*", message = "El nombre del asistente no puede estar en blanco")
        String nombreAsistente,

        @Size(min = 1, max = 30, message = "La zona debe tener entre 1 y 30 caracteres")
        @Pattern(regexp = ".*\\S.*", message = "La zona no puede estar en blanco")
        String zona) {

    @AssertTrue(message = "Debe enviar al menos un dato a modificar (nombreAsistente o zona)")
    public boolean isAlgunCambio() {
        return nombreAsistente != null || zona != null;
    }
}
