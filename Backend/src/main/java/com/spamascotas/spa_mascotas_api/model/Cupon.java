package com.spamascotas.spa_mascotas_api.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "cupones")
public class Cupon {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "cupones_gen")
    @SequenceGenerator(name = "cupones_gen", sequenceName = "cupones_id_seq", allocationSize = 1)
    private Long id;

    @Column(unique = true, nullable = false)
    private String codigo;

    private String descripcion;

    @Column(name = "descuento_porcentaje")
    @Builder.Default
    private BigDecimal descuentoPorcentaje = BigDecimal.ZERO;

    @Column(name = "descuento_fijo")
    @Builder.Default
    private BigDecimal descuentoFijo = BigDecimal.ZERO;

    @Column(name = "usos_max")
    private Integer usosMax;

    @Column(name = "usos_actuales", nullable = false)
    @Builder.Default
    private Integer usosActuales = 0;

    @Column(name = "fecha_vencimiento")
    private LocalDate fechaVencimiento;

    @Builder.Default
    @Column(nullable = false)
    private Boolean activo = true;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "cupon_productos",
        joinColumns = @JoinColumn(name = "cupon_id"),
        inverseJoinColumns = @JoinColumn(name = "producto_id")
    )
    @Builder.Default
    private Set<Producto> productos = new HashSet<>();
}
