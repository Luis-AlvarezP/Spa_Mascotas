package com.spamascotas.spa_mascotas_api.repository;

import com.spamascotas.spa_mascotas_api.model.CalificacionCita;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CalificacionCitaRepository extends JpaRepository<CalificacionCita, Long> {
    Optional<CalificacionCita> findByCitaId(Long citaId);

    @Query("SELECT cc FROM CalificacionCita cc ORDER BY cc.fecha DESC")
    List<CalificacionCita> findAllOrderByFecha();

    @Query("SELECT cc FROM CalificacionCita cc WHERE cc.cita.empleadoAsignado.id = :empleadoId ORDER BY cc.fecha DESC")
    List<CalificacionCita> findByEmpleadoId(@Param("empleadoId") Long empleadoId);
}
