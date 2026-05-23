package com.spamascotas.spa_mascotas_api.repository;

import com.spamascotas.spa_mascotas_api.model.VentaItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface VentaItemRepository extends JpaRepository<VentaItem, Long> {

    @Query("SELECT vi.producto.nombre, SUM(vi.cantidad), SUM(vi.cantidad * vi.precioUnitarioHistorico) " +
           "FROM VentaItem vi GROUP BY vi.producto.nombre " +
           "ORDER BY SUM(vi.cantidad * vi.precioUnitarioHistorico) DESC")
    List<Object[]> rankingProductosTodo();

    @Query("SELECT vi.producto.nombre, SUM(vi.cantidad), SUM(vi.cantidad * vi.precioUnitarioHistorico) " +
           "FROM VentaItem vi WHERE vi.venta.fechaVenta >= :desde AND vi.venta.fechaVenta < :hasta " +
           "GROUP BY vi.producto.nombre ORDER BY SUM(vi.cantidad * vi.precioUnitarioHistorico) DESC")
    List<Object[]> rankingProductosEntre(@Param("desde") LocalDateTime desde,
                                          @Param("hasta") LocalDateTime hasta);
}
