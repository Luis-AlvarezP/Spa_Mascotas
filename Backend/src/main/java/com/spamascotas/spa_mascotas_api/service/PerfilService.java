package com.spamascotas.spa_mascotas_api.service;

import com.spamascotas.spa_mascotas_api.dto.request.PerfilUpdateRequest;
import com.spamascotas.spa_mascotas_api.dto.response.MessageResponse;
import com.spamascotas.spa_mascotas_api.dto.response.PerfilResponse;
import com.spamascotas.spa_mascotas_api.dto.response.Setup2faResponse;
import com.spamascotas.spa_mascotas_api.model.Cliente;
import com.spamascotas.spa_mascotas_api.model.Empleado;
import com.spamascotas.spa_mascotas_api.model.Usuario;
import com.spamascotas.spa_mascotas_api.model.enums.RolEnum;
import com.spamascotas.spa_mascotas_api.model.enums.TipoAccion;
import com.spamascotas.spa_mascotas_api.repository.ClienteRepository;
import com.spamascotas.spa_mascotas_api.repository.EmpleadoRepository;
import com.spamascotas.spa_mascotas_api.repository.UsuarioRepository;
import com.spamascotas.spa_mascotas_api.security.TotpService;
import com.spamascotas.spa_mascotas_api.service.admin.AuditService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PerfilService {

    private final UsuarioRepository usuarioRepository;
    private final ClienteRepository clienteRepository;
    private final EmpleadoRepository empleadoRepository;
    private final TotpService totpService;
    private final PasswordEncoder passwordEncoder;
    private final AuditService auditService;

    public PerfilResponse obtenerPerfil(String correo) {
        Usuario usuario = usuarioRepository.findByCorreo(correo)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        String rolNombre = usuario.getRoles().stream()
                .findFirst().map(r -> r.getNombre().name()).orElse("CLIENTE");

        String nombre = null;
        String ci = null;
        String telefono = null;

        String direccion = null;
        if (RolEnum.CLIENTE.name().equals(rolNombre)) {
            Cliente cliente = clienteRepository.findByUsuario(usuario).orElse(null);
            if (cliente != null) {
                nombre   = cliente.getNombre();
                ci       = cliente.getCi();
                telefono = cliente.getTelefono();
                direccion = cliente.getDireccion();
            }
        } else if (!RolEnum.ADMIN.name().equals(rolNombre)) {
            Empleado emp = empleadoRepository.findByUsuario(usuario).orElse(null);
            if (emp != null) {
                nombre   = emp.getNombre();
                ci       = emp.getCi();
                telefono = emp.getTelefono();
            }
        }

        return PerfilResponse.builder()
                .correo(correo)
                .nombre(nombre)
                .nombreUsuario(usuario.getNombreUsuario())
                .rol(rolNombre)
                .ci(ci)
                .telefono(telefono)
                .direccion(direccion)
                .totpHabilitado(usuario.getTotpHabilitado())
                .tieneGoogle(usuario.getGoogleId() != null)
                .build();
    }

    @Transactional
    public PerfilResponse actualizarPerfil(String correo, PerfilUpdateRequest request) {
        Usuario usuario = usuarioRepository.findByCorreo(correo)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        String rolNombre = usuario.getRoles().stream()
                .findFirst().map(r -> r.getNombre().name()).orElse("CLIENTE");

        StringBuilder cambios = new StringBuilder();

        if (RolEnum.CLIENTE.name().equals(rolNombre)) {
            Cliente cliente = clienteRepository.findByUsuario(usuario)
                    .orElse(Cliente.builder().usuario(usuario).nombre("").build());
            if (request.getNombre()   != null) { cliente.setNombre(request.getNombre());   cambios.append("Nombre: ").append(request.getNombre()).append(" | "); }
            if (request.getCi()       != null) { cliente.setCi(request.getCi());           cambios.append("CI: ").append(request.getCi()).append(" | "); }
            if (request.getTelefono() != null) { cliente.setTelefono(request.getTelefono()); cambios.append("Tel: ").append(request.getTelefono()).append(" | "); }
            if (request.getDireccion()!= null) { cliente.setDireccion(request.getDireccion()); cambios.append("Dir: ").append(request.getDireccion()).append(" | "); }
            clienteRepository.save(cliente);
        } else if (!RolEnum.ADMIN.name().equals(rolNombre)) {
            Empleado emp = empleadoRepository.findByUsuario(usuario).orElse(null);
            if (emp != null) {
                if (request.getNombre()   != null) { emp.setNombre(request.getNombre());     cambios.append("Nombre: ").append(request.getNombre()).append(" | "); }
                if (request.getCi()       != null) { emp.setCi(request.getCi());             cambios.append("CI: ").append(request.getCi()).append(" | "); }
                if (request.getTelefono() != null) { emp.setTelefono(request.getTelefono()); cambios.append("Tel: ").append(request.getTelefono()).append(" | "); }
                empleadoRepository.save(emp);
            }
        }

        if (request.getNombreUsuario() != null) {
            String nu = request.getNombreUsuario().isBlank() ? null : request.getNombreUsuario().trim();
            usuario.setNombreUsuario(nu);
            usuarioRepository.save(usuario);
            cambios.append("Usuario: ").append(nu != null ? nu : "(borrado)").append(" | ");
        }

        if (!cambios.isEmpty() && !RolEnum.CLIENTE.name().equals(rolNombre)) {
            String detalles = cambios.toString().replaceAll(" \\| $", "");
            auditService.registrar(TipoAccion.MODIFICAR_USUARIO, correo, rolNombre, true, detalles);
        }

        return obtenerPerfil(correo);
    }

    // ── Cambio de contraseña ──────────────────────────────────

    @Transactional
    public MessageResponse cambiarPassword(String correo, String passwordActual, String nuevaPassword) {
        Usuario u = usuarioRepository.findByCorreo(correo)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
        if (u.getPasswordHash() == null || !passwordEncoder.matches(passwordActual, u.getPasswordHash())) {
            throw new RuntimeException("La contraseña actual es incorrecta");
        }
        u.setPasswordHash(passwordEncoder.encode(nuevaPassword));
        usuarioRepository.save(u);

        String rolNombre = u.getRoles().stream().findFirst().map(r -> r.getNombre().name()).orElse("CLIENTE");
        auditService.registrar(TipoAccion.CAMBIO_PASSWORD, correo, rolNombre, true,
                "Contraseña actualizada desde perfil");

        return new MessageResponse("Contraseña actualizada correctamente");
    }

    

    @Transactional
    public Setup2faResponse init2faLoggedIn(String correo) {
        Usuario u = usuarioRepository.findByCorreo(correo)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        if (u.getTotpHabilitado()) {
            throw new RuntimeException("El 2FA ya está habilitado");
        }

        String secret = u.getTotpSecret() != null ? u.getTotpSecret() : totpService.generarSecret();
        u.setTotpSecret(secret);
        usuarioRepository.save(u);

        return new Setup2faResponse(totpService.urlQr(correo, secret), secret);
    }

    @Transactional
    public MessageResponse enable2faLoggedIn(String correo, int codigo) {
        Usuario u = usuarioRepository.findByCorreo(correo)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        if (u.getTotpSecret() == null) {
            throw new RuntimeException("Inicia el proceso de configuración primero");
        }
        if (!totpService.verificar(u.getTotpSecret(), codigo)) {
            throw new RuntimeException("Código de verificación inválido");
        }

        u.setTotpHabilitado(true);
        usuarioRepository.save(u);

        return new MessageResponse("2FA activado correctamente");
    }

    @Transactional
    public MessageResponse disable2fa(String correo, String password) {
        Usuario u = usuarioRepository.findByCorreo(correo)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        String rolNombre = u.getRoles().stream()
                .findFirst().map(r -> r.getNombre().name()).orElse("CLIENTE");

        if (!RolEnum.CLIENTE.name().equals(rolNombre)) {
            throw new RuntimeException("Solo los clientes pueden desactivar el 2FA");
        }
        if (u.getPasswordHash() == null || !passwordEncoder.matches(password, u.getPasswordHash())) {
            throw new RuntimeException("Contraseña incorrecta");
        }

        u.setTotpHabilitado(false);
        u.setTotpSecret(null);
        usuarioRepository.save(u);

        return new MessageResponse("2FA desactivado correctamente");
    }
}
