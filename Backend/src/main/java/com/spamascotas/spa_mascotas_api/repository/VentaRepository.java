package com.spamascotas.spa_mascotas_api.repository;

import com.spamascotas.spa_mascotas_api.model.Venta;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface VentaRepository extends JpaRepository<Venta, Long> {
    List<Venta> findByClienteIdOrderByFechaVentaDesc(Long clienteId);
    List<Venta> findAllByOrderByFechaVentaDesc();
    long countByClienteIdAndEstado(Long clienteId, String estado);
}
