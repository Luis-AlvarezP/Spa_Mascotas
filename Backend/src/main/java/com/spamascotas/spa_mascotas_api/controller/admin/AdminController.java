package com.spamascotas.spa_mascotas_api.controller.admin;

import com.spamascotas.spa_mascotas_api.dto.request.CreateStaffRequest;
import com.spamascotas.spa_mascotas_api.dto.request.UpdateStaffRequest;
import com.spamascotas.spa_mascotas_api.dto.response.StaffResponse;
import com.spamascotas.spa_mascotas_api.service.admin.AdminService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final AdminService adminService;

    @PostMapping("/staff")
    @ResponseStatus(HttpStatus.CREATED)
    public StaffResponse crearStaff(
            @Valid @RequestBody CreateStaffRequest request,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        return adminService.crearStaff(request, userDetails.getUsername());
    }

    @GetMapping("/staff")
    public List<StaffResponse> listarStaff() {
        return adminService.listarStaff();
    }

    @PutMapping("/staff/{id}")
    public ResponseEntity<StaffResponse> actualizar(@PathVariable Long id,
            @Valid @RequestBody UpdateStaffRequest request,
            @AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.ok(adminService.actualizarStaff(id, request, user.getUsername()));
    }

    @PatchMapping("/staff/{id}/toggle")
    public ResponseEntity<StaffResponse> toggleEstado(@PathVariable Long id,
            @AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.ok(adminService.toggleEstadoStaff(id, user.getUsername()));
    }
}
