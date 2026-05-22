package com.spamascotas.spa_mascotas_api.repository;

import com.spamascotas.spa_mascotas_api.model.FichaDetalle;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FichaDetalleRepository extends JpaRepository<FichaDetalle, Long> {
    List<FichaDetalle> findByFichaId(Long fichaId);
    void deleteByFichaId(Long fichaId);
}
