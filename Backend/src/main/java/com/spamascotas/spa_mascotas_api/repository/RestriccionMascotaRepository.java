package com.spamascotas.spa_mascotas_api.repository;

import com.spamascotas.spa_mascotas_api.model.Mascota;
import com.spamascotas.spa_mascotas_api.model.RestriccionMascota;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RestriccionMascotaRepository extends JpaRepository<RestriccionMascota, Long> {
    List<RestriccionMascota> findByMascota(Mascota mascota);
    void deleteByMascota(Mascota mascota);
}
