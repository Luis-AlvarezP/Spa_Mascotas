package com.spamascotas.spa_mascotas_api.repository;

import com.spamascotas.spa_mascotas_api.model.GaleriaMascota;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface GaleriaMascotaRepository extends JpaRepository<GaleriaMascota, Long> {
    List<GaleriaMascota> findByCitaIdOrderByFechaCarga(Long citaId);
}
