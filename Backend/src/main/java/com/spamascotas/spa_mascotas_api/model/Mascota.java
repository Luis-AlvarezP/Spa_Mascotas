package com.spamascotas.spa_mascotas_api.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "mascotas")
public class Mascota {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "mascotas_gen")
    @SequenceGenerator(name = "mascotas_gen", sequenceName = "mascotas_id_seq", allocationSize = 1)
    private Long id;

    @Column(nullable = false)
    private String nombre;

    private String especie;

    private String raza;

    @Column(name = "tamaño")
    private String tamano;

    @Column(name = "fecha_nacimiento")
    private LocalDate fechaNacimiento;

    private String alergias;

    @Column(name = "url_foto_mascota")
    private String urlFotoMascota;

    @Column(name = "url_carnet")
    private String urlCarnet;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "temperamento_esperado_id")
    private CatTemperamento temperamento;

    @Column(nullable = false)
    @Builder.Default
    private Boolean activa = true;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cliente_id")
    private Cliente cliente;
}
