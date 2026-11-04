package cl.duoc.eventos.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/**
 * Compra de entradas. Su id es el número del resumen de compra.
 */
@Entity
@Table(name = "COMPRAS")
public class Compra {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "EVENTO_ID", nullable = false)
    private Evento evento;

    @Column(name = "CANTIDAD_ENTRADAS", nullable = false)
    private Integer cantidadEntradas;

    @Column(name = "PRECIO_UNITARIO", nullable = false, precision = 12, scale = 2)
    private BigDecimal precioUnitario;

    @Column(name = "PRECIO_TOTAL", nullable = false, precision = 14, scale = 2)
    private BigDecimal precioTotal;

    @Column(name = "FECHA_COMPRA", nullable = false)
    private LocalDateTime fechaCompra;

    /** Usuario que hizo la reserva (semana 3). Las compras de las semanas anteriores no lo tienen. */
    @Column(length = 50)
    private String usuario;

    protected Compra() {
    }

    public Compra(Evento evento, int cantidadEntradas) {
        this(evento, cantidadEntradas, null);
    }

    public Compra(Evento evento, int cantidadEntradas, String usuario) {
        this.evento = evento;
        this.usuario = usuario;
        this.cantidadEntradas = cantidadEntradas;
        // El precio unitario se congela al momento de la compra
        this.precioUnitario = evento.getPrecioEntrada();
        this.precioTotal = precioUnitario.multiply(BigDecimal.valueOf(cantidadEntradas));
        this.fechaCompra = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);
    }

    public Long getId() {
        return id;
    }

    public Evento getEvento() {
        return evento;
    }

    public Integer getCantidadEntradas() {
        return cantidadEntradas;
    }

    public BigDecimal getPrecioUnitario() {
        return precioUnitario;
    }

    public BigDecimal getPrecioTotal() {
        return precioTotal;
    }

    public LocalDateTime getFechaCompra() {
        return fechaCompra;
    }

    public String getUsuario() {
        return usuario;
    }
}
