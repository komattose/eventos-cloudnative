package cl.duoc.eventos.service;

import cl.duoc.eventos.dto.GenerarTicketsRequest;
import cl.duoc.eventos.dto.HistorialArchivoResponse;
import cl.duoc.eventos.dto.ModificarTicketRequest;
import cl.duoc.eventos.dto.TicketResponse;
import cl.duoc.eventos.exception.RecursoNoEncontradoException;
import cl.duoc.eventos.model.AccionArchivo;
import cl.duoc.eventos.model.Compra;
import cl.duoc.eventos.model.EstadoTicket;
import cl.duoc.eventos.model.HistorialArchivo;
import cl.duoc.eventos.model.Ticket;
import cl.duoc.eventos.repository.EfsRepository;
import cl.duoc.eventos.repository.HistorialArchivoRepository;
import cl.duoc.eventos.repository.S3Repository;
import cl.duoc.eventos.repository.TicketRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Tickets personalizados: se generan en el EFS (almacenamiento temporal) y se suben a S3
 * en evento{eventoId}/{usuario}/ticket{id}.pdf. Cada operación sobre los archivos queda en el historial.
 */
@Service
public class TicketService {

    private static final Logger log = LoggerFactory.getLogger(TicketService.class);
    private static final String TIPO_PDF = "application/pdf";
    private static final String EFS = "EFS";
    private static final String S3 = "S3";

    private final TicketRepository ticketRepository;
    private final HistorialArchivoRepository historialRepository;
    private final CompraService compraService;
    private final TicketPdfService ticketPdfService;
    private final EfsRepository efsRepository;
    private final S3Repository s3Repository;
    private final String bucket;
    private final boolean subidaAutomatica;

    public TicketService(TicketRepository ticketRepository, HistorialArchivoRepository historialRepository,
                         CompraService compraService, TicketPdfService ticketPdfService,
                         EfsRepository efsRepository, S3Repository s3Repository,
                         @Value("${aws.s3.bucket:}") String bucket,
                         @Value("${tickets.subida-automatica:true}") boolean subidaAutomatica) {
        this.ticketRepository = ticketRepository;
        this.historialRepository = historialRepository;
        this.compraService = compraService;
        this.ticketPdfService = ticketPdfService;
        this.efsRepository = efsRepository;
        this.s3Repository = s3Repository;
        this.bucket = bucket;
        this.subidaAutomatica = subidaAutomatica;
    }

    /**
     * Registra la reserva, genera un ticket por entrada en el EFS y, si está activada la subida
     * automática, los sube a S3. Si S3 falla los tickets quedan en estado GENERADO para subirlos después.
     */
    @Transactional
    public List<TicketResponse> generar(GenerarTicketsRequest request) {
        Compra reserva = compraService.registrar(request.eventoId(), request.cantidad(), request.usuario());
        String nombreAsistente = StringUtils.hasText(request.nombreAsistente())
                ? request.nombreAsistente().trim() : request.usuario();
        String zona = StringUtils.hasText(request.zona()) ? request.zona().trim().toUpperCase() : "GENERAL";

        List<Ticket> tickets = new ArrayList<>();
        for (int i = 0; i < request.cantidad(); i++) {
            Ticket ticket = ticketRepository.save(new Ticket(reserva, request.usuario(), nombreAsistente, zona));
            guardarEnEfs(ticket, AccionArchivo.GENERADO_EFS);
            tickets.add(ticket);
        }
        if (subidaAutomatica) {
            try {
                tickets.forEach(ticket -> subirAS3(ticket, AccionArchivo.SUBIDO_S3));
            } catch (RuntimeException e) {
                log.warn("No se pudieron subir automáticamente los tickets de la reserva {} a S3: {}",
                        reserva.getId(), e.getMessage());
            }
        }
        return tickets.stream().map(TicketResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public TicketResponse obtener(Long id) {
        return TicketResponse.from(buscar(id));
    }

    @Transactional(readOnly = true)
    public List<TicketResponse> listar(Long eventoId, String usuario, EstadoTicket estado) {
        return ticketRepository.buscar(eventoId, vacioANulo(usuario), estado).stream()
                .map(TicketResponse::from)
                .toList();
    }

    /**
     * Sube (o vuelve a subir) a S3 el archivo del ticket que está en el EFS.
     */
    @Transactional
    public TicketResponse subir(Long id) {
        Ticket ticket = vigente(id);
        subirAS3(ticket, AccionArchivo.SUBIDO_S3);
        return TicketResponse.from(ticket);
    }

    /**
     * Sube a S3 todos los tickets que aún están solo en el EFS, filtrando por evento y/o usuario.
     */
    @Transactional
    public List<TicketResponse> subirPendientes(Long eventoId, String usuario) {
        List<Ticket> pendientes = ticketRepository.buscar(eventoId, vacioANulo(usuario), EstadoTicket.GENERADO);
        pendientes.forEach(ticket -> subirAS3(ticket, AccionArchivo.SUBIDO_S3));
        return pendientes.stream().map(TicketResponse::from).toList();
    }

    /**
     * Descarga desde S3 el PDF de un ticket.
     */
    @Transactional
    public byte[] descargar(Long id) {
        Ticket ticket = vigente(id);
        if (ticket.getEstado() != EstadoTicket.SUBIDO) {
            throw new IllegalStateException("El ticket " + id + " todavía no está en S3. Súbelo con POST /api/tickets/" + id + "/s3");
        }
        byte[] contenido = s3Repository.obtener(bucket(), ticket.getClaveS3());
        registrar(ticket, AccionArchivo.DESCARGADO, S3, ticket.getClaveS3(), (long) contenido.length);
        return contenido;
    }

    /**
     * Descarga desde S3 los tickets de un evento y/o de un usuario en un archivo ZIP
     * que mantiene la estructura de carpetas del bucket.
     */
    @Transactional
    public byte[] descargarZip(Long eventoId, String usuario) {
        if (eventoId == null && !StringUtils.hasText(usuario)) {
            throw new IllegalArgumentException("Debe indicar el evento (eventoId) y/o el usuario");
        }
        List<Ticket> tickets = ticketRepository.buscar(eventoId, vacioANulo(usuario), EstadoTicket.SUBIDO);
        if (tickets.isEmpty()) {
            throw new RecursoNoEncontradoException("No hay tickets en S3 para " + descripcionFiltro(eventoId, usuario));
        }
        ByteArrayOutputStream salida = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(salida)) {
            for (Ticket ticket : tickets) {
                byte[] contenido = s3Repository.obtener(bucket(), ticket.getClaveS3());
                zip.putNextEntry(new ZipEntry(ticket.getClaveS3()));
                zip.write(contenido);
                zip.closeEntry();
                registrar(ticket, AccionArchivo.DESCARGADO, S3, ticket.getClaveS3(), (long) contenido.length);
            }
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo armar el ZIP de tickets", e);
        }
        return salida.toByteArray();
    }

    /**
     * Corrige los detalles del ticket, regenera su PDF en el EFS y actualiza el archivo en S3 si ya estaba subido.
     */
    @Transactional
    public TicketResponse modificar(Long id, ModificarTicketRequest request) {
        Ticket ticket = vigente(id);
        ticket.modificar(recortar(request.nombreAsistente()),
                request.zona() == null ? null : request.zona().trim().toUpperCase());
        guardarEnEfs(ticket, AccionArchivo.MODIFICADO);
        if (ticket.getEstado() == EstadoTicket.SUBIDO) {
            subirAS3(ticket, AccionArchivo.MODIFICADO);
        }
        return TicketResponse.from(ticket);
    }

    /**
     * Borra los archivos del ticket (S3 y EFS) y lo deja en estado ELIMINADO.
     * El registro se conserva para las estadísticas y el historial.
     */
    @Transactional
    public void eliminar(Long id) {
        Ticket ticket = vigente(id);
        if (ticket.getEstado() == EstadoTicket.SUBIDO) {
            s3Repository.eliminar(bucket(), ticket.getClaveS3());
            registrar(ticket, AccionArchivo.ELIMINADO, S3, ticket.getClaveS3(), null);
        }
        if (ticket.getRutaEfs() != null && efsRepository.eliminar(ticket.getRutaEfs())) {
            registrar(ticket, AccionArchivo.ELIMINADO, EFS, ticket.getRutaEfs(), null);
        }
        ticket.eliminar();
    }

    /**
     * Historial de archivos generados, con filtros opcionales. Las fechas incluyen el día completo.
     */
    @Transactional(readOnly = true)
    public List<HistorialArchivoResponse> historial(Long eventoId, String usuario, Long ticketId,
                                                    AccionArchivo accion, LocalDate desde, LocalDate hasta) {
        LocalDateTime inicio = desde == null ? null : desde.atStartOfDay();
        LocalDateTime fin = hasta == null ? null : hasta.plusDays(1).atStartOfDay();
        return historialRepository.buscar(eventoId, vacioANulo(usuario), ticketId, accion, inicio, fin).stream()
                .map(HistorialArchivoResponse::from)
                .toList();
    }

    /**
     * Objetos que existen realmente en el bucket para un evento y/o usuario.
     */
    public List<String> archivosEnS3(Long eventoId, String usuario) {
        String prefijo = eventoId == null ? "" : carpetaEvento(eventoId);
        String carpetaUsuario = StringUtils.hasText(usuario) ? "/" + usuario.trim() + "/" : null;
        return s3Repository.listar(bucket(), prefijo).stream()
                .map(objeto -> objeto.key())
                .filter(clave -> clave.startsWith("evento"))
                .filter(clave -> carpetaUsuario == null || clave.contains(carpetaUsuario))
                .toList();
    }

    private void guardarEnEfs(Ticket ticket, AccionArchivo accion) {
        String ruta = ruta(ticket);
        byte[] pdf = ticketPdfService.generar(ticket);
        efsRepository.guardar(ruta, pdf);
        ticket.generadoEnEfs(ruta);
        registrar(ticket, accion, EFS, efsRepository.rutaCompleta(ruta), (long) pdf.length);
    }

    private void subirAS3(Ticket ticket, AccionArchivo accion) {
        String clave = ruta(ticket);
        // Si el temporal ya no está en el EFS se vuelve a generar
        byte[] pdf = efsRepository.leer(clave).orElseGet(() -> {
            byte[] regenerado = ticketPdfService.generar(ticket);
            efsRepository.guardar(clave, regenerado);
            return regenerado;
        });
        s3Repository.guardar(bucket(), clave, pdf, TIPO_PDF);
        ticket.subidoAS3(clave);
        registrar(ticket, accion, S3, clave, (long) pdf.length);
    }

    private void registrar(Ticket ticket, AccionArchivo accion, String almacenamiento, String ruta, Long tamano) {
        historialRepository.save(new HistorialArchivo(ticket, accion, almacenamiento, ruta, tamano));
    }

    /** Misma ruta relativa en el EFS y en S3: evento{eventoId}/{usuario}/ticket{id}.pdf */
    private static String ruta(Ticket ticket) {
        return carpetaEvento(ticket.getEvento().getId()) + ticket.getUsuario() + "/"
                + "ticket%03d.pdf".formatted(ticket.getId());
    }

    private static String carpetaEvento(Long eventoId) {
        return "evento" + eventoId + "/";
    }

    private Ticket buscar(Long id) {
        return ticketRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe un ticket con id " + id));
    }

    private Ticket vigente(Long id) {
        Ticket ticket = buscar(id);
        if (ticket.estaEliminado()) {
            throw new IllegalStateException("El ticket " + id + " fue eliminado");
        }
        return ticket;
    }

    private String bucket() {
        if (!StringUtils.hasText(bucket)) {
            throw new IllegalStateException("No hay un bucket de S3 configurado (variable de entorno S3_BUCKET)");
        }
        return bucket;
    }

    private static String vacioANulo(String texto) {
        return StringUtils.hasText(texto) ? texto.trim() : null;
    }

    private static String recortar(String texto) {
        return texto == null ? null : texto.trim();
    }

    private static String descripcionFiltro(Long eventoId, String usuario) {
        List<String> partes = new ArrayList<>();
        if (eventoId != null) {
            partes.add("el evento " + eventoId);
        }
        if (StringUtils.hasText(usuario)) {
            partes.add("el usuario " + usuario.trim());
        }
        return String.join(" y ", partes);
    }
}
