package com.spamascotas.spa_mascotas_api.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "pedido")
public class Pedido {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "pedido_gen")
    @SequenceGenerator(name = "pedido_gen", sequenceName = "pedido_id_seq", allocationSize = 1)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "venta_id")
    private Venta venta;

    @Column(name = "tipo_entrega", nullable = false)
    @Builder.Default
    private String tipoEntrega = "RECOGER";

    @Column(name = "direccion_entrega")
    private String direccionEntrega;

    @Column(nullable = false)
    @Builder.Default
    private String estado = "EN_ESPERA";

    @Column(name = "fecha_entrega_pedido")
    private LocalDateTime fechaEntregaPedido;
}
