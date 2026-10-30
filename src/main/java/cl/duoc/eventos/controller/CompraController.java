package cl.duoc.eventos.controller;

import cl.duoc.eventos.dto.CompraRequest;
import cl.duoc.eventos.dto.ResumenCompra;
import cl.duoc.eventos.service.CompraService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

@RestController
@RequestMapping("/api/compras")
public class CompraController {

    private final CompraService compraService;

    public CompraController(CompraService compraService) {
        this.compraService = compraService;
    }

    @PostMapping
    public ResponseEntity<ResumenCompra> comprar(@Valid @RequestBody CompraRequest request) {
        ResumenCompra resumen = compraService.comprar(request);
        URI ubicacion = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{numero}").buildAndExpand(resumen.numeroResumen()).toUri();
        return ResponseEntity.created(ubicacion).body(resumen);
    }

    @GetMapping("/{numeroResumen}")
    public ResumenCompra obtenerResumen(@PathVariable Long numeroResumen) {
        return compraService.obtenerResumen(numeroResumen);
    }
}
