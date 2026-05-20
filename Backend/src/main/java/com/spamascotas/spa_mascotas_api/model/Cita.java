package com.spamascotas.spa_mascotas_api.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "citas")
public class Cita {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "citas_gen")
    @SequenceGenerator(name = "citas_gen", sequenceName = "citas_id_seq", allocationSize = 1)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cliente_id")
    private Cliente cliente;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mascota_id")
    private Mascota mascota;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "servicio_id")
    private Servicio servicio;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "empleado_id")
    private Empleado empleadoAsignado;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "empleado_preferido_id")
    private Empleado empleadoPreferido;

    @Column(name = "fecha_hora_inicio", nullable = false)
    private LocalDateTime fechaHoraInicio;

    @Column(name = "fecha_hora_fin", nullable = false)
    private LocalDateTime fechaHoraFin;

    @Builder.Default
    private String estado = "EN_REVISION";

    @Column(name = "duracion_minutos")
    private Integer duracionMinutos;

    @Column(name = "tamano_mascota")
    private String tamanoMascota;

    @Column(name = "temperamento_mascota")
    private String temperamentoMascota;

    @Column(name = "precio_final", precision = 10, scale = 2)
    private BigDecimal precioFinal;

    @Column(name = "recargo_porcentaje", precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal recargoPorcentaje = BigDecimal.ZERO;

    @Column(name = "motivo_cancelacion")
    private String motivoCancelacion;

    private String notas;

    @Column(name = "creado_en")
    @Builder.Default
    private LocalDateTime creadoEn = LocalDateTime.now();
}
