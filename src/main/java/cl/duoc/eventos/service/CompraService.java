package cl.duoc.eventos.service;

import cl.duoc.eventos.dto.CompraRequest;
import cl.duoc.eventos.dto.ResumenCompra;
import cl.duoc.eventos.exception.RecursoNoEncontradoException;
import cl.duoc.eventos.model.Compra;
import cl.duoc.eventos.model.Evento;
import cl.duoc.eventos.repository.CompraRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
public class CompraService {

    private final CompraRepository compraRepository;
    private final EventoService eventoService;

    public CompraService(CompraRepository compraRepository, EventoService eventoService) {
        this.compraRepository = compraRepository;
        this.eventoService = eventoService;
    }

    @Transactional
    public ResumenCompra comprar(CompraRequest request) {
        Evento evento = eventoService.buscar(request.eventoId());
        if (evento.getFecha().isBefore(LocalDate.now())) {
            throw new IllegalStateException("El evento \"" + evento.getNombre() + "\" ya ocurrió y no tiene entradas a la venta");
        }
        Compra compra = compraRepository.save(new Compra(evento, request.cantidadEntradas()));
        return ResumenCompra.from(compra);
    }

    @Transactional(readOnly = true)
    public ResumenCompra obtenerResumen(Long numeroResumen) {
        return compraRepository.findById(numeroResumen)
                .map(ResumenCompra::from)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe un resumen de compra con número " + numeroResumen));
    }
}
