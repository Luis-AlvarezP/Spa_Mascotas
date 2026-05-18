package com.spamascotas.spa_mascotas_api.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "ventas")
public class Venta {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "ventas_gen")
    @SequenceGenerator(name = "ventas_gen", sequenceName = "ventas_id_seq", allocationSize = 1)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cliente_id")
    private Cliente cliente;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vendedor_id")
    private Empleado vendedor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "metodo_pago_id")
    private MetodoPago metodoPago;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cupon_id")
    private Cupon cupon;

    @Column(name = "fecha_venta", nullable = false)
    @Builder.Default
    private LocalDateTime fechaVenta = LocalDateTime.now();

    @Column(nullable = false)
    @Builder.Default
    private BigDecimal subtotal = BigDecimal.ZERO;

    @Builder.Default
    private BigDecimal descuento = BigDecimal.ZERO;

    @Column(name = "descuento_frecuente")
    @Builder.Default
    private BigDecimal descuentoFrecuente = BigDecimal.ZERO;

    @Column(name = "descuento_manual")
    @Builder.Default
    private BigDecimal descuentoManual = BigDecimal.ZERO;

    @Column(name = "total_final", nullable = false)
    private BigDecimal totalFinal;

    @Column(nullable = false)
    @Builder.Default
    private String estado = "CONFIRMADA";

    private String notas;

    @OneToMany(mappedBy = "venta", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<VentaItem> items = new ArrayList<>();

    @OneToOne(mappedBy = "venta", fetch = FetchType.LAZY)
    private Pedido pedido;
}
