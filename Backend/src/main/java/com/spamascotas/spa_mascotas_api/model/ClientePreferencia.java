package com.spamascotas.spa_mascotas_api.model;

import jakarta.persistence.*;
import lombok.*;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
@Entity
@Table(name = "cliente_preferencias",
       uniqueConstraints = @UniqueConstraint(columnNames = {"cliente_id", "nombre"}))
public class ClientePreferencia {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "cli_prefs_gen")
    @SequenceGenerator(name = "cli_prefs_gen", sequenceName = "cli_prefs_id_seq", allocationSize = 1)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "cliente_id", nullable = false)
    private Cliente cliente;

    @Column(nullable = false)
    private String nombre;

    private String valor;
}
