package cl.duoc.eventos.repository;

import cl.duoc.eventos.model.AccionArchivo;
import cl.duoc.eventos.model.HistorialArchivo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface HistorialArchivoRepository extends JpaRepository<HistorialArchivo, Long> {

    /**
     * Historial filtrado; los filtros nulos no se aplican. Lo más reciente primero.
     */
    @Query("""
            select h from HistorialArchivo h
            where (:eventoId is null or h.eventoId = :eventoId)
              and (:usuario is null or h.usuario = :usuario)
              and (:ticketId is null or h.ticketId = :ticketId)
              and (:accion is null or h.accion = :accion)
              and (:desde is null or h.fecha >= :desde)
              and (:hasta is null or h.fecha < :hasta)
            order by h.fecha desc, h.id desc
            """)
    List<HistorialArchivo> buscar(@Param("eventoId") Long eventoId, @Param("usuario") String usuario,
                                  @Param("ticketId") Long ticketId, @Param("accion") AccionArchivo accion,
                                  @Param("desde") LocalDateTime desde, @Param("hasta") LocalDateTime hasta);
}
