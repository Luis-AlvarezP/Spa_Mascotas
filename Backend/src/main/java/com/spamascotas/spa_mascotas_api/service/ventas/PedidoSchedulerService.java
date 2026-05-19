package com.spamascotas.spa_mascotas_api.service.ventas;

import com.spamascotas.spa_mascotas_api.model.Pedido;
import com.spamascotas.spa_mascotas_api.model.Producto;
import com.spamascotas.spa_mascotas_api.model.VentaItem;
import com.spamascotas.spa_mascotas_api.repository.PedidoRepository;
import com.spamascotas.spa_mascotas_api.repository.ProductoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class PedidoSchedulerService {

    private final PedidoRepository pedidoRepository;
    private final ProductoRepository productoRepository;

    @Scheduled(fixedRate = 3600000) 
    @Transactional
    public void cancelarPedidosVencidos() {
        LocalDateTime limite = LocalDateTime.now().minusHours(24);
        List<Pedido> vencidos = pedidoRepository
                .findByEstadoAndVenta_FechaVentaBefore("EN_ESPERA", limite);

        for (Pedido p : vencidos) {
            p.setEstado("CANCELADO");
            for (VentaItem item : p.getVenta().getItems()) {
                Producto prod = item.getProducto();
                prod.setStockActual(prod.getStockActual() + item.getCantidad());
                productoRepository.save(prod);
            }
            pedidoRepository.save(p);
            log.info("Pedido #{} cancelado automáticamente por vencimiento de 24h", p.getId());
        }
    }
}
