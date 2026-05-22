package com.spamascotas.spa_mascotas_api.repository;

import com.spamascotas.spa_mascotas_api.model.CalificacionCita;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface CalificacionCitaRepository extends JpaRepository<CalificacionCita, Long> {
    Optional<CalificacionCita> findByCitaId(Long citaId);
}
