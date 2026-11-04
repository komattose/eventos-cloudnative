package cl.duoc.eventos.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/**
 * Registro de cada operación sobre los archivos de los tickets (EFS y S3).
 * Guarda los ids y no relaciones para que el historial se mantenga aunque el ticket se elimine.
 */
@Entity
@Table(name = "HISTORIAL_ARCHIVOS")
public class HistorialArchivo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "TICKET_ID", nullable = false)
    private Long ticketId;

    @Column(name = "EVENTO_ID", nullable = false)
    private Long eventoId;

    @Column(nullable = false, length = 50)
    private String usuario;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AccionArchivo accion;

    /** EFS o S3 */
    @Column(nullable = false, length = 10)
    private String almacenamiento;

    @Column(nullable = false, length = 300)
    private String ruta;

    @Column(name = "TAMANO_BYTES")
    private Long tamanoBytes;

    @Column(nullable = false)
    private LocalDateTime fecha;

    protected HistorialArchivo() {
    }

    public HistorialArchivo(Ticket ticket, AccionArchivo accion, String almacenamiento, String ruta, Long tamanoBytes) {
        this.ticketId = ticket.getId();
        this.eventoId = ticket.getEvento().getId();
        this.usuario = ticket.getUsuario();
        this.accion = accion;
        this.almacenamiento = almacenamiento;
        this.ruta = ruta;
        this.tamanoBytes = tamanoBytes;
        this.fecha = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);
    }

    public Long getId() {
        return id;
    }

    public Long getTicketId() {
        return ticketId;
    }

    public Long getEventoId() {
        return eventoId;
    }

    public String getUsuario() {
        return usuario;
    }

    public AccionArchivo getAccion() {
        return accion;
    }

    public String getAlmacenamiento() {
        return almacenamiento;
    }

    public String getRuta() {
        return ruta;
    }

    public Long getTamanoBytes() {
        return tamanoBytes;
    }

    public LocalDateTime getFecha() {
        return fecha;
    }
}
