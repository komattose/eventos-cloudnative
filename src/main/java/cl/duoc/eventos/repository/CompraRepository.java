package cl.duoc.eventos.repository;

import cl.duoc.eventos.model.Compra;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CompraRepository extends JpaRepository<Compra, Long> {

    List<Compra> findByEventoId(Long eventoId);
}
