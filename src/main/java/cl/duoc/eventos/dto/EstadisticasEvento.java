package cl.duoc.eventos.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Estadísticas de ventas y reservas de un evento.
 *
 * @param reservas          compras/reservas registradas
 * @param entradasVendidas  suma de las entradas de todas las reservas
 * @param recaudacion       suma del precio total de las reservas
 * @param ticketsGenerados  tickets emitidos (incluye los eliminados)
 * @param ticketsVigentes   tickets no eliminados
 * @param ticketsEnS3       tickets vigentes que ya están en S3
 * @param ticketsEliminados tickets eliminados
 * @param usuarios          usuarios distintos con tickets
 */
public record EstadisticasEvento(
        Long eventoId,
        String evento,
        LocalDate fechaEvento,
        long reservas,
        long entradasVendidas,
        BigDecimal recaudacion,
        long ticketsGenerados,
        long ticketsVigentes,
        long ticketsEnS3,
        long ticketsEliminados,
        long usuarios) {
}
