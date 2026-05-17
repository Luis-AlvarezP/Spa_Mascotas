package com.spamascotas.spa_mascotas_api.service.admin;

import com.spamascotas.spa_mascotas_api.model.AuditLog;
import com.spamascotas.spa_mascotas_api.model.enums.TipoAccion;
import com.spamascotas.spa_mascotas_api.repository.AuditLogRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    public void registrar(TipoAccion accion, String correo, String rol, boolean exitoso, String detalles) {
        AuditLog entry = AuditLog.builder()
                .accion(accion)
                .correoUsuario(correo)
                .rol(rol)
                .ip(obtenerIp())
                .userAgent(obtenerUserAgent())
                .detalles(detalles)
                .exitoso(exitoso)
                .timestamp(LocalDateTime.now())
                .build();
        auditLogRepository.save(entry);
    }

    private String obtenerIp() {
        try {
            ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs != null) {
                HttpServletRequest req = attrs.getRequest();
                for (String header : new String[]{
                        "X-Forwarded-For", "X-Real-IP",
                        "Proxy-Client-IP", "WL-Proxy-Client-IP",
                        "HTTP_X_FORWARDED_FOR", "HTTP_CLIENT_IP"}) {
                    String val = req.getHeader(header);
                    if (val != null && !val.isBlank() && !"unknown".equalsIgnoreCase(val)) {
                        return val.split(",")[0].trim();
                    }
                }
                return req.getRemoteAddr();
            }
        } catch (Exception ignored) { }
        return "desconocida";
    }

    private String obtenerUserAgent() {
        try {
            ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs != null) {
                String ua = attrs.getRequest().getHeader("User-Agent");
                return ua != null ? ua : "desconocido";
            }
        } catch (Exception ignored) { }
        return "desconocido";
    }
}
