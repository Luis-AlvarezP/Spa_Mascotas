package com.spamascotas.spa_mascotas_api.controller.admin;

import com.spamascotas.spa_mascotas_api.dto.response.AdminCalificacionesResponse;
import com.spamascotas.spa_mascotas_api.dto.response.AdminVentasResponse;
import com.spamascotas.spa_mascotas_api.service.admin.AdminReportesService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/admin/reportes")
@RequiredArgsConstructor
public class AdminReportesController {

    private final AdminReportesService service;

    @GetMapping("/ventas")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public AdminVentasResponse getVentas(
            @RequestParam(required = false) String desde,
            @RequestParam(required = false) String hasta) {
        LocalDate d = desde != null && !desde.isBlank() ? LocalDate.parse(desde) : null;
        LocalDate h = hasta != null && !hasta.isBlank() ? LocalDate.parse(hasta) : null;
        return service.getVentas(d, h);
    }

    @GetMapping("/calificaciones")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public AdminCalificacionesResponse getCalificaciones(
            @RequestParam(required = false) Long empleadoId) {
        return service.getCalificaciones(empleadoId);
    }
}
