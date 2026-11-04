package cl.duoc.eventos.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

/**
 * Ticket personalizado de una reserva. Su PDF se genera en el EFS y se sube a S3
 * en la ruta evento{eventoId}/{usuario}/ticket{id}.pdf
 */
@Entity
@Table(name = "TICKETS")
public class Ticket {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "COMPRA_ID", nullable = false)
    private Compra compra;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "EVENTO_ID", nullable = false)
    private Evento evento;

    @Column(nullable = false, length = 50)
    private String usuario;

    @Column(name = "NOMBRE_ASISTENTE", nullable = false, length = 100)
    private String nombreAsistente;

    @Column(nullable = false, length = 30)
    private String zona;

    /** Código único que se imprime en el ticket para validarlo en la entrada. */
    @Column(nullable = false, unique = true, length = 36)
    private String codigo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoTicket estado;

    @Column(name = "RUTA_EFS", length = 300)
    private String rutaEfs;

    @Column(name = "CLAVE_S3", length = 300)
    private String claveS3;

    @Column(name = "FECHA_GENERACION", nullable = false)
    private LocalDateTime fechaGeneracion;

    @Column(name = "FECHA_ACTUALIZACION", nullable = false)
    private LocalDateTime fechaActualizacion;

    protected Ticket() {
    }

    public Ticket(Compra compra, String usuario, String nombreAsistente, String zona) {
        this.compra = compra;
        this.evento = compra.getEvento();
        this.usuario = usuario;
        this.nombreAsistente = nombreAsistente;
        this.zona = zona;
        this.codigo = UUID.randomUUID().toString();
        this.estado = EstadoTicket.GENERADO;
        this.fechaGeneracion = ahora();
        this.fechaActualizacion = fechaGeneracion;
    }

    public void generadoEnEfs(String rutaEfs) {
        this.rutaEfs = rutaEfs;
        tocar();
    }

    public void subidoAS3(String claveS3) {
        this.claveS3 = claveS3;
        this.estado = EstadoTicket.SUBIDO;
        tocar();
    }

    public void modificar(String nombreAsistente, String zona) {
        if (nombreAsistente != null) {
            this.nombreAsistente = nombreAsistente;
        }
        if (zona != null) {
            this.zona = zona;
        }
        tocar();
    }

    public void eliminar() {
        this.estado = EstadoTicket.ELIMINADO;
        tocar();
    }

    public boolean estaEliminado() {
        return estado == EstadoTicket.ELIMINADO;
    }

    private void tocar() {
        this.fechaActualizacion = ahora();
    }

    private static LocalDateTime ahora() {
        return LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);
    }

    public Long getId() {
        return id;
    }

    public Compra getCompra() {
        return compra;
    }

    public Evento getEvento() {
        return evento;
    }

    public String getUsuario() {
        return usuario;
    }

    public String getNombreAsistente() {
        return nombreAsistente;
    }

    public String getZona() {
        return zona;
    }

    public String getCodigo() {
        return codigo;
    }

    public EstadoTicket getEstado() {
        return estado;
    }

    public String getRutaEfs() {
        return rutaEfs;
    }

    public String getClaveS3() {
        return claveS3;
    }

    public LocalDateTime getFechaGeneracion() {
        return fechaGeneracion;
    }

    public LocalDateTime getFechaActualizacion() {
        return fechaActualizacion;
    }
}
