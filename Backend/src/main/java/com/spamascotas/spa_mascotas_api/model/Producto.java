package com.spamascotas.spa_mascotas_api.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "productos")
public class Producto {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "productos_gen")
    @SequenceGenerator(name = "productos_gen", sequenceName = "productos_id_seq", allocationSize = 1)
    private Long id;

    @Column(nullable = false)
    private String nombre;

    private String descripcion;

    @Column(unique = true)
    private String sku;

    @Column(name = "stock_actual", nullable = false)
    @Builder.Default
    private Integer stockActual = 0;

    @Column(name = "stock_minimo", nullable = false)
    @Builder.Default
    private Integer stockMinimo = 0;

    @Column(name = "precio_venta")
    private BigDecimal precioVenta;

    @Column(name = "fecha_vencimiento")
    private LocalDate fechaVencimiento;

    private String lote;

    @Column(name = "url_imagen")
    private String urlImagen;

    @Builder.Default
    @Column(nullable = false)
    private Boolean activo = true;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "categoria_id")
    private Categoria categoria;
}
