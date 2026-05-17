package com.spamascotas.spa_mascotas_api.service.admin;

import com.spamascotas.spa_mascotas_api.dto.request.CreateStaffRequest;
import com.spamascotas.spa_mascotas_api.dto.request.UpdateStaffRequest;
import com.spamascotas.spa_mascotas_api.dto.response.StaffResponse;
import com.spamascotas.spa_mascotas_api.model.Empleado;
import com.spamascotas.spa_mascotas_api.model.Rol;
import com.spamascotas.spa_mascotas_api.model.Usuario;
import com.spamascotas.spa_mascotas_api.model.enums.EstadoUsuario;
import com.spamascotas.spa_mascotas_api.model.enums.RolEnum;
import com.spamascotas.spa_mascotas_api.model.enums.TipoAccion;
import com.spamascotas.spa_mascotas_api.repository.EmpleadoRepository;
import com.spamascotas.spa_mascotas_api.repository.RolRepository;
import com.spamascotas.spa_mascotas_api.repository.UsuarioRepository;
import com.spamascotas.spa_mascotas_api.service.auth.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminService {

    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;
    private final EmpleadoRepository empleadoRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final AuditService auditService;

    @Transactional
    public StaffResponse crearStaff(CreateStaffRequest request, String adminCorreo) {
        if (usuarioRepository.existsByCorreo(request.getCorreo())) {
            throw new RuntimeException("Ya existe un usuario con ese correo");
        }

        RolEnum rolEnum;
        try {
            rolEnum = RolEnum.valueOf(request.getRol().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Rol inválido. Solo se permite GROOMER o RECEPCION");
        }

        if (rolEnum == RolEnum.ADMIN || rolEnum == RolEnum.CLIENTE) {
            throw new RuntimeException("Solo se puede crear personal con rol GROOMER o RECEPCION");
        }

        Rol rol = rolRepository.findByNombre(rolEnum)
                .orElseThrow(() -> new RuntimeException("Rol " + rolEnum + " no encontrado en la base de datos"));

        String passwordGenerada = generarPassword();

        Usuario usuario = new Usuario();
        usuario.setCorreo(request.getCorreo());
        usuario.setPasswordHash(passwordEncoder.encode(passwordGenerada));
        usuario.setEstado(EstadoUsuario.ACTIVO);
        usuario.getRoles().add(rol);
        usuarioRepository.save(usuario);

        Empleado empleado = Empleado.builder()
                .usuario(usuario)
                .nombre(request.getNombre())
                .puesto(rolEnum.name())
                .ci(request.getCi())
                .telefono(request.getTelefono())
                .build();
        empleadoRepository.save(empleado);

        emailService.enviarBienvenidaStaff(request.getCorreo(), request.getNombre(), rolEnum.name(), passwordGenerada);
        auditService.registrar(TipoAccion.CREAR_STAFF, adminCorreo, "ADMIN", true,
                "Nombre: " + request.getNombre()
                + " | Correo: " + request.getCorreo()
                + " | CI: " + nvl(request.getCi())
                + " | Tel: " + nvl(request.getTelefono())
                + " | Rol: " + rolEnum.name());

        log.info("Staff creado: {} ({})", request.getCorreo(), rolEnum.name());
        return StaffResponse.builder()
                .id(usuario.getId()).correo(usuario.getCorreo())
                .nombre(empleado.getNombre()).ci(empleado.getCi()).telefono(empleado.getTelefono())
                .rol(rolEnum.name()).estado(EstadoUsuario.ACTIVO.name())
                .build();
    }

    @Transactional
    public StaffResponse actualizarStaff(Long id, UpdateStaffRequest request, String adminCorreo) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Personal no encontrado"));

        Empleado emp = empleadoRepository.findByUsuario(usuario)
                .orElseThrow(() -> new RuntimeException("Empleado no encontrado"));

        if (request.getNombre()   != null) emp.setNombre(request.getNombre());
        if (request.getCi()       != null) emp.setCi(request.getCi());
        if (request.getTelefono() != null) emp.setTelefono(request.getTelefono());
        empleadoRepository.save(emp);

        String rolNombre = usuario.getRoles().stream().findFirst()
                .map(r -> r.getNombre().name()).orElse("DESCONOCIDO");

        auditService.registrar(TipoAccion.MODIFICAR_USUARIO, adminCorreo, "ADMIN", true,
                "Correo: " + usuario.getCorreo()
                + " | Nombre: " + nvl(emp.getNombre())
                + " | CI: " + nvl(emp.getCi())
                + " | Tel: " + nvl(emp.getTelefono())
                + " | Rol: " + rolNombre);

        return StaffResponse.builder()
                .id(usuario.getId()).correo(usuario.getCorreo())
                .nombre(emp.getNombre()).ci(emp.getCi()).telefono(emp.getTelefono())
                .rol(rolNombre).estado(usuario.getEstado().name())
                .build();
    }

    @Transactional
    public StaffResponse toggleEstadoStaff(Long id, String adminCorreo) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Personal no encontrado"));

        EstadoUsuario nuevoEstado = usuario.getEstado() == EstadoUsuario.ACTIVO
                ? EstadoUsuario.INACTIVO : EstadoUsuario.ACTIVO;
        usuario.setEstado(nuevoEstado);
        usuarioRepository.save(usuario);

        Empleado empAudit = empleadoRepository.findByUsuario(usuario).orElse(null);
        String nombreAudit = empAudit != null ? nvl(empAudit.getNombre()) : "-";
        TipoAccion accionToggle = nuevoEstado == EstadoUsuario.INACTIVO
                ? TipoAccion.DESACTIVAR_USUARIO : TipoAccion.ACTIVAR_USUARIO;
        auditService.registrar(accionToggle, adminCorreo, "ADMIN", true,
                "Nombre: " + nombreAudit
                + " | Correo: " + usuario.getCorreo()
                + " | Rol: " + usuario.getRoles().stream().findFirst().map(r -> r.getNombre().name()).orElse("-")
                + " | Estado: " + nuevoEstado);

        String rolNombre = usuario.getRoles().stream().findFirst()
                .map(r -> r.getNombre().name()).orElse("DESCONOCIDO");
        return StaffResponse.builder()
                .id(usuario.getId()).correo(usuario.getCorreo())
                .nombre(empAudit != null ? empAudit.getNombre() : null)
                .ci(empAudit != null ? empAudit.getCi() : null)
                .telefono(empAudit != null ? empAudit.getTelefono() : null)
                .rol(rolNombre).estado(nuevoEstado.name())
                .build();
    }

    public List<StaffResponse> listarStaff() {
        return usuarioRepository.findAll().stream()
                .filter(u -> u.getRoles().stream().anyMatch(r ->
                        r.getNombre() == RolEnum.GROOMER || r.getNombre() == RolEnum.RECEPCION))
                .map(u -> {
                    String rolNombre = u.getRoles().stream().findFirst()
                            .map(r -> r.getNombre().name()).orElse("DESCONOCIDO");
                    Empleado empLista = empleadoRepository.findByUsuario(u).orElse(null);
                    return StaffResponse.builder()
                            .id(u.getId()).correo(u.getCorreo())
                            .nombre(empLista != null ? empLista.getNombre() : null)
                            .ci(empLista != null ? empLista.getCi() : null)
                            .telefono(empLista != null ? empLista.getTelefono() : null)
                            .rol(rolNombre).estado(u.getEstado().name())
                            .build();
                })
                .toList();
    }

    private String nvl(String s) { return s != null ? s : "-"; }

    private String generarPassword() {
        String chars = "ABCDEFGHJKMNPQRSTUVWXYZabcdefghjkmnpqrstuvwxyz23456789@#$!";
        SecureRandom random = new SecureRandom();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 12; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        return sb.toString();
    }
}
