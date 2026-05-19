package com.spamascotas.spa_mascotas_api.service.inventario;

import com.spamascotas.spa_mascotas_api.model.Producto;
import com.spamascotas.spa_mascotas_api.repository.ProductoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductoVencimientoSchedulerService {

    private final ProductoRepository productoRepository;

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void onStartup() {
        desactivarVencidos();
    }

    @Scheduled(cron = "0 0 0 * * *")
    @Transactional
    public void desactivarVencidos() {
        List<Producto> vencidos = productoRepository
                .findByFechaVencimientoLessThanEqualAndActivoTrue(LocalDate.now());
        for (Producto p : vencidos) {
            p.setActivo(false);
            productoRepository.save(p);
            log.info("Producto '{}' (id={}) desactivado por vencimiento", p.getNombre(), p.getId());
        }
        if (!vencidos.isEmpty()) {
            log.info("{} producto(s) vencido(s) desactivado(s) automáticamente", vencidos.size());
        }
    }
}
