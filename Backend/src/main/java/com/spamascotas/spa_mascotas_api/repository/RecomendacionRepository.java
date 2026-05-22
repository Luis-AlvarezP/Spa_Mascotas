package com.spamascotas.spa_mascotas_api.repository;

import com.spamascotas.spa_mascotas_api.model.RecomendacionPostServicio;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RecomendacionRepository extends JpaRepository<RecomendacionPostServicio, Long> {
    Optional<RecomendacionPostServicio> findByCitaId(Long citaId);
}
