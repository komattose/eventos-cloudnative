package cl.duoc.eventos.dto;

import cl.duoc.eventos.model.AccionArchivo;
import cl.duoc.eventos.model.HistorialArchivo;

import java.time.LocalDateTime;

public record HistorialArchivoResponse(
        Long id,
        Long ticketId,
        Long eventoId,
        String usuario,
        AccionArchivo accion,
        String almacenamiento,
        String ruta,
        Long tamanoBytes,
        LocalDateTime fecha) {

    public static HistorialArchivoResponse from(HistorialArchivo h) {
        return new HistorialArchivoResponse(h.getId(), h.getTicketId(), h.getEventoId(), h.getUsuario(),
                h.getAccion(), h.getAlmacenamiento(), h.getRuta(), h.getTamanoBytes(), h.getFecha());
    }
}
