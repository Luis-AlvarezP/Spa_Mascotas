package com.spamascotas.spa_mascotas_api.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "fichas_ingreso")
public class FichaIngreso {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "fichas_gen")
    @SequenceGenerator(name = "fichas_gen", sequenceName = "fichas_id_seq", allocationSize = 1)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cita_id")
    private Cita cita;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "temperamento_id")
    private CatTemperamento temperamento;

    @Column(name = "observaciones_generales", columnDefinition = "TEXT")
    private String observacionesGenerales;

    @Column(name = "estado_ingreso", columnDefinition = "TEXT")
    private String estadoIngreso;

    @Builder.Default
    @Column(name = "checklist_unas")
    private Boolean checklistUnas = false;

    @Builder.Default
    @Column(name = "checklist_oidos")
    private Boolean checklistOidos = false;

    @Builder.Default
    @Column(name = "checklist_glandulas")
    private Boolean checklistGlandulas = false;

    @Builder.Default
    @Column(name = "checklist_corte")
    private Boolean checklistCorte = false;

    @Builder.Default
    @Column(name = "checklist_bano")
    private Boolean checklistBano = false;

    @Builder.Default
    @Column(name = "checklist_perfume")
    private Boolean checklistPerfume = false;

    @Builder.Default
    @Column(name = "checklist_peinado")
    private Boolean checklistPeinado = false;

    @Builder.Default
    @Column(nullable = false)
    private String estado = "ABIERTA";

    @Builder.Default
    @Column(name = "creado_en", nullable = false)
    private LocalDateTime creadoEn = LocalDateTime.now();

    @Builder.Default
    @OneToMany(mappedBy = "ficha", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<FichaDetalle> detalles = new ArrayList<>();
}
