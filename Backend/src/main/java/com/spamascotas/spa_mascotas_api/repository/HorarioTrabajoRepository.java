package com.spamascotas.spa_mascotas_api.repository;

import com.spamascotas.spa_mascotas_api.model.Empleado;
import com.spamascotas.spa_mascotas_api.model.HorarioTrabajo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface HorarioTrabajoRepository extends JpaRepository<HorarioTrabajo, Long> {
    List<HorarioTrabajo> findByEmpleadoOrderByDiaSemana(Empleado empleado);
}
