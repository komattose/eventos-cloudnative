package cl.duoc.eventos.repository;

import cl.duoc.eventos.model.EstadoTicket;
import cl.duoc.eventos.model.Ticket;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface TicketRepository extends JpaRepository<Ticket, Long> {

    /**
     * Tickets filtrados por evento, usuario y/o estado. Los filtros nulos no se aplican.
     */
    @Query("""
            select t from Ticket t
            where (:eventoId is null or t.evento.id = :eventoId)
              and (:usuario is null or t.usuario = :usuario)
              and (:estado is null or t.estado = :estado)
            order by t.id
            """)
    List<Ticket> buscar(@Param("eventoId") Long eventoId, @Param("usuario") String usuario,
                        @Param("estado") EstadoTicket estado);

    List<Ticket> findByEventoId(Long eventoId);
}
