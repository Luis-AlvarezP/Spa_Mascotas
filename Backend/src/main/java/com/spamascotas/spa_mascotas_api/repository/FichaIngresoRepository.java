package com.spamascotas.spa_mascotas_api.repository;

import com.spamascotas.spa_mascotas_api.model.FichaIngreso;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface FichaIngresoRepository extends JpaRepository<FichaIngreso, Long> {
    Optional<FichaIngreso> findByCitaId(Long citaId);
}
