package cl.duoc.eventos.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "EVENTOS")
public class Evento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String nombre;

    @Column(nullable = false)
    private LocalDate fecha;

    @Column(name = "PRECIO_ENTRADA", nullable = false, precision = 12, scale = 2)
    private BigDecimal precioEntrada;

    protected Evento() {
    }

    public Evento(String nombre, LocalDate fecha, BigDecimal precioEntrada) {
        this.nombre = nombre;
        this.fecha = fecha;
        this.precioEntrada = precioEntrada;
    }

    public Long getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }

    public LocalDate getFecha() {
        return fecha;
    }

    public BigDecimal getPrecioEntrada() {
        return precioEntrada;
    }
}
