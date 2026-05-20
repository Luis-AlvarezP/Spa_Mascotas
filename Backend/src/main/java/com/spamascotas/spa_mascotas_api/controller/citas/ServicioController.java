package com.spamascotas.spa_mascotas_api.controller.citas;

import com.spamascotas.spa_mascotas_api.dto.response.ServicioResponse;
import com.spamascotas.spa_mascotas_api.service.citas.CitaService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/servicios")
@RequiredArgsConstructor
public class ServicioController {

    private final CitaService service;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public List<ServicioResponse> listar() {
        return service.listarServicios();
    }
}
