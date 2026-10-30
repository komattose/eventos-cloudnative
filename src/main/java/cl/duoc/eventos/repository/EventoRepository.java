package cl.duoc.eventos.repository;

import cl.duoc.eventos.model.Evento;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface EventoRepository extends JpaRepository<Evento, Long> {

    List<Evento> findByFechaGreaterThanEqualOrderByFechaAsc(LocalDate fecha);
}
