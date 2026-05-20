package com.spamascotas.spa_mascotas_api.repository;

import com.spamascotas.spa_mascotas_api.model.BloqueoAgenda;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface BloqueoAgendaRepository extends JpaRepository<BloqueoAgenda, Long> {
    List<BloqueoAgenda> findAllByOrderByFechaInicioDesc();

    @Query("SELECT b FROM BloqueoAgenda b WHERE b.fechaInicio < :hasta AND b.fechaFin > :desde")
    List<BloqueoAgenda> findActivasEnRango(
            @Param("desde") LocalDateTime desde,
            @Param("hasta") LocalDateTime hasta);
}
