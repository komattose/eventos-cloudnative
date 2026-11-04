package cl.duoc.eventos.controller;

import cl.duoc.eventos.dto.GenerarTicketsRequest;
import cl.duoc.eventos.dto.HistorialArchivoResponse;
import cl.duoc.eventos.dto.ModificarTicketRequest;
import cl.duoc.eventos.dto.TicketResponse;
import cl.duoc.eventos.model.AccionArchivo;
import cl.duoc.eventos.model.EstadoTicket;
import cl.duoc.eventos.service.TicketService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

/**
 * Tickets personalizados (semana 3): generación en EFS, subida a S3, descarga, modificación,
 * eliminación e historial de archivos.
 */
@RestController
@RequestMapping("/api/tickets")
public class TicketController {

    private final TicketService ticketService;

    public TicketController(TicketService ticketService) {
        this.ticketService = ticketService;
    }

    /** Reserva entradas y genera un ticket por entrada (EFS y subida automática a S3). */
    @PostMapping
    public ResponseEntity<List<TicketResponse>> generar(@Valid @RequestBody GenerarTicketsRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ticketService.generar(request));
    }

    /** Lista tickets filtrando por evento, usuario y/o estado. */
    @GetMapping
    public List<TicketResponse> listar(@RequestParam(required = false) Long eventoId,
                                       @RequestParam(required = false) String usuario,
                                       @RequestParam(required = false) EstadoTicket estado) {
        return ticketService.listar(eventoId, usuario, estado);
    }

    @GetMapping("/{id}")
    public TicketResponse obtener(@PathVariable Long id) {
        return ticketService.obtener(id);
    }

    /** Sube (o vuelve a subir) un ticket a S3 desde el EFS. */
    @PostMapping("/{id}/s3")
    public TicketResponse subir(@PathVariable Long id) {
        return ticketService.subir(id);
    }

    /** Sube a S3 los tickets que están solo en el EFS, por evento y/o usuario. */
    @PostMapping("/s3")
    public List<TicketResponse> subirPendientes(@RequestParam(required = false) Long eventoId,
                                                @RequestParam(required = false) String usuario) {
        return ticketService.subirPendientes(eventoId, usuario);
    }

    /** Descarga desde S3 el PDF de un ticket. */
    @GetMapping("/{id}/descargar")
    public ResponseEntity<byte[]> descargar(@PathVariable Long id) {
        return archivo(ticketService.descargar(id), MediaType.APPLICATION_PDF, "ticket%03d.pdf".formatted(id));
    }

    /** Descarga desde S3 en un ZIP los tickets de un evento y/o de un usuario. */
    @GetMapping("/descargar")
    public ResponseEntity<byte[]> descargarZip(@RequestParam(required = false) Long eventoId,
                                               @RequestParam(required = false) String usuario) {
        String nombre = "tickets" + (eventoId == null ? "" : "-evento" + eventoId)
                + (usuario == null || usuario.isBlank() ? "" : "-" + usuario.trim()) + ".zip";
        return archivo(ticketService.descargarZip(eventoId, usuario), MediaType.parseMediaType("application/zip"), nombre);
    }

    /** Modifica los detalles del ticket y actualiza sus archivos en EFS y S3. */
    @PutMapping("/{id}")
    public TicketResponse modificar(@PathVariable Long id, @Valid @RequestBody ModificarTicketRequest request) {
        return ticketService.modificar(id, request);
    }

    /** Elimina un ticket específico: borra su archivo de S3 y del EFS. */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        ticketService.eliminar(id);
        return ResponseEntity.noContent().build();
    }

    /** Historial de archivos generados con filtros opcionales (fechas AAAA-MM-DD). */
    @GetMapping("/historial")
    public List<HistorialArchivoResponse> historial(
            @RequestParam(required = false) Long eventoId,
            @RequestParam(required = false) String usuario,
            @RequestParam(required = false) Long ticketId,
            @RequestParam(required = false) AccionArchivo accion,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        return ticketService.historial(eventoId, usuario, ticketId, accion, desde, hasta);
    }

    /** Objetos que hay en el bucket S3 para un evento y/o usuario. */
    @GetMapping("/archivos-s3")
    public List<String> archivosEnS3(@RequestParam(required = false) Long eventoId,
                                     @RequestParam(required = false) String usuario) {
        return ticketService.archivosEnS3(eventoId, usuario);
    }

    private static ResponseEntity<byte[]> archivo(byte[] contenido, MediaType tipo, String nombre) {
        HttpHeaders cabeceras = new HttpHeaders();
        cabeceras.setContentType(tipo);
        cabeceras.setContentDisposition(ContentDisposition.attachment().filename(nombre).build());
        return ResponseEntity.ok().headers(cabeceras).body(contenido);
    }
}
