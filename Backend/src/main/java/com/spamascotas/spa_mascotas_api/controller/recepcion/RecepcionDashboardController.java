package com.spamascotas.spa_mascotas_api.controller.recepcion;

import com.spamascotas.spa_mascotas_api.dto.response.RecepcionDashboardResponse;
import com.spamascotas.spa_mascotas_api.service.recepcion.RecepcionDashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/recepcion")
@RequiredArgsConstructor
public class RecepcionDashboardController {

    private final RecepcionDashboardService service;

    @GetMapping("/dashboard")
    @PreAuthorize("hasAuthority('ROLE_RECEPCION')")
    public RecepcionDashboardResponse getDashboard() {
        return service.getDashboard();
    }
}
