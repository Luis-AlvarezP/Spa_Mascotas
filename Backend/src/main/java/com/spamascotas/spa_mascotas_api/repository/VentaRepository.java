package com.spamascotas.spa_mascotas_api.repository;

import com.spamascotas.spa_mascotas_api.model.Venta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface VentaRepository extends JpaRepository<Venta, Long> {
    List<Venta> findByClienteIdOrderByFechaVentaDesc(Long clienteId);
    List<Venta> findAllByOrderByFechaVentaDesc();
    long countByClienteIdAndEstado(Long clienteId, String estado);

    @Query("SELECT v FROM Venta v WHERE v.fechaVenta >= :inicio AND v.fechaVenta < :fin ORDER BY v.fechaVenta")
    List<Venta> findVentasEntre(@Param("inicio") LocalDateTime inicio, @Param("fin") LocalDateTime fin);
}
