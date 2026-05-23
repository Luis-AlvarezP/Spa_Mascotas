package com.spamascotas.spa_mascotas_api.repository;

import com.spamascotas.spa_mascotas_api.model.MovimientoInventario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface MovimientoInventarioRepository extends JpaRepository<MovimientoInventario, Long> {

    @Query("SELECT m FROM MovimientoInventario m WHERE m.cita.id = :citaId AND m.tipoMovimiento = 'ENTREGA_GROOMER' ORDER BY m.fecha DESC")
    List<MovimientoInventario> findInsumosByCita(@Param("citaId") Long citaId);

    @Query("SELECT m FROM MovimientoInventario m WHERE m.tipoMovimiento = 'ENTREGA_GROOMER' ORDER BY m.fecha DESC")
    List<MovimientoInventario> findAllEntregasGroomer();

    @Query("SELECT m FROM MovimientoInventario m WHERE m.empleado.id = :empleadoId " +
           "AND m.tipoMovimiento = 'ENTREGA_GROOMER' " +
           "AND m.estado IN ('USADO', 'DESPERDICIADO', 'DEVUELTO') " +
           "AND m.fecha >= :inicio AND m.fecha < :fin " +
           "ORDER BY m.fecha")
    List<MovimientoInventario> findInsumosUsadosByGroomerEntre(@Param("empleadoId") Long empleadoId,
                                                               @Param("inicio") LocalDateTime inicio,
                                                               @Param("fin") LocalDateTime fin);
}

