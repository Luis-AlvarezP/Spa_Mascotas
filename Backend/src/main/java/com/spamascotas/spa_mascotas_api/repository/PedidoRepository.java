package com.spamascotas.spa_mascotas_api.repository;

import com.spamascotas.spa_mascotas_api.model.Pedido;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface PedidoRepository extends JpaRepository<Pedido, Long> {
    Optional<Pedido> findByVentaId(Long ventaId);
    List<Pedido> findAllByOrderByIdDesc();
    List<Pedido> findByVentaClienteIdOrderByIdDesc(Long clienteId);
    List<Pedido> findByEstadoAndVenta_FechaVentaBefore(String estado, LocalDateTime fechaLimite);
}
