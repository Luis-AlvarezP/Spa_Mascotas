package com.spamascotas.spa_mascotas_api.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "venta_items")
public class VentaItem {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "venta_items_gen")
    @SequenceGenerator(name = "venta_items_gen", sequenceName = "venta_items_id_seq", allocationSize = 1)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "venta_id")
    private Venta venta;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "producto_id")
    private Producto producto;

    @Column(nullable = false)
    private Integer cantidad;

    @Column(name = "precio_unitario_historico")
    private BigDecimal precioUnitarioHistorico;
}
