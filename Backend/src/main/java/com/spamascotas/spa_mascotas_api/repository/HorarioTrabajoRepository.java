package com.spamascotas.spa_mascotas_api.repository;

import com.spamascotas.spa_mascotas_api.model.Empleado;
import com.spamascotas.spa_mascotas_api.model.HorarioTrabajo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface HorarioTrabajoRepository extends JpaRepository<HorarioTrabajo, Long> {
    List<HorarioTrabajo> findByEmpleadoOrderByDiaSemana(Empleado empleado);

    @Query("SELECT h FROM HorarioTrabajo h WHERE h.empleado.id = :empleadoId " +
           "AND h.diaSemana = :diaSemana " +
           "AND h.vigenteDesde <= :fecha " +
           "AND (h.vigenteHasta IS NULL OR h.vigenteHasta >= :fecha)")
    List<HorarioTrabajo> findVigenteByEmpleadoAndDia(
            @Param("empleadoId") Long empleadoId,
            @Param("diaSemana") String diaSemana,
            @Param("fecha") LocalDate fecha);
}
