package com.spamascotas.spa_mascotas_api.controller.admin;

import com.spamascotas.spa_mascotas_api.model.AuditLog;
import com.spamascotas.spa_mascotas_api.model.enums.TipoAccion;
import com.spamascotas.spa_mascotas_api.repository.AuditLogRepository;
import jakarta.persistence.criteria.Predicate;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/admin/audit")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('ROLE_ADMIN')")
public class AuditController {

    private final AuditLogRepository auditLogRepository;

    @GetMapping
    public Page<AuditLog> listar(
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "50") int size,
            @RequestParam(required = false) String rol,
            @RequestParam(required = false) List<String> acciones,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("timestamp").descending());
        LocalDateTime desdeTs = desde != null ? desde.atStartOfDay() : null;
        LocalDateTime hastaTs = hasta != null ? hasta.atTime(LocalTime.MAX) : null;

        return auditLogRepository.findAll(
                buildSpec(rol, parseAcciones(acciones), desdeTs, hastaTs),
                pageable);
    }

    @GetMapping("/export")
    public void exportExcel(
            @RequestParam(required = false) String rol,
            @RequestParam(required = false) List<String> acciones,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            HttpServletResponse response
    ) throws IOException {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename=\"audit_log.xlsx\"");

        LocalDateTime desdeTs = desde != null ? desde.atStartOfDay() : null;
        LocalDateTime hastaTs = hasta != null ? hasta.atTime(LocalTime.MAX) : null;

        List<AuditLog> logs = auditLogRepository.findAll(
                buildSpec(rol, parseAcciones(acciones), desdeTs, hastaTs),
                Sort.by("timestamp").descending());

        try (Workbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("Auditoría");

            String[] headers = {"ID", "Fecha/Hora", "Acción", "Correo", "Rol", "IP", "Exitoso", "Detalles", "Navegador"};
            Row headerRow = sheet.createRow(0);
            CellStyle headerStyle = wb.createCellStyle();
            Font font = wb.createFont();
            font.setBold(true);
            headerStyle.setFont(font);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            int rowNum = 1;
            for (AuditLog l : logs) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(l.getId() != null ? l.getId() : 0);
                row.createCell(1).setCellValue(l.getTimestamp() != null ? l.getTimestamp().toString() : "");
                row.createCell(2).setCellValue(l.getAccion() != null ? l.getAccion().name() : "");
                row.createCell(3).setCellValue(nvl(l.getCorreoUsuario()));
                row.createCell(4).setCellValue(nvl(l.getRol()));
                row.createCell(5).setCellValue(nvl(l.getIp()));
                row.createCell(6).setCellValue(l.isExitoso() ? "Sí" : "No");
                row.createCell(7).setCellValue(nvl(l.getDetalles()));
                row.createCell(8).setCellValue(nvl(l.getUserAgent()));
            }

            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            wb.write(response.getOutputStream());
        }
    }

    private Specification<AuditLog> buildSpec(String rol, List<TipoAccion> acciones,
                                               LocalDateTime desde, LocalDateTime hasta) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (rol != null && !rol.isBlank())
                predicates.add(cb.equal(cb.lower(root.get("rol")), rol.toLowerCase()));
            if (acciones != null && !acciones.isEmpty())
                predicates.add(root.get("accion").in(acciones));
            if (desde != null)
                predicates.add(cb.greaterThanOrEqualTo(root.get("timestamp"), desde));
            if (hasta != null)
                predicates.add(cb.lessThanOrEqualTo(root.get("timestamp"), hasta));
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private List<TipoAccion> parseAcciones(List<String> acciones) {
        if (acciones == null || acciones.isEmpty()) return List.of();
        return acciones.stream()
                .map(a -> { try { return TipoAccion.valueOf(a.toUpperCase()); } catch (IllegalArgumentException e) { return null; } })
                .filter(java.util.Objects::nonNull)
                .collect(java.util.stream.Collectors.toList());
    }

    private String nvl(String s) { return s != null ? s : ""; }
}
