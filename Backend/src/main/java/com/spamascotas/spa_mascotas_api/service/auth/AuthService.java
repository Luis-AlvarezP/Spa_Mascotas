package com.spamascotas.spa_mascotas_api.service.auth;

import com.spamascotas.spa_mascotas_api.dto.request.LoginRequest;
import com.spamascotas.spa_mascotas_api.dto.request.RegisterRequest;
import com.spamascotas.spa_mascotas_api.dto.response.AuthResponse;
import com.spamascotas.spa_mascotas_api.dto.response.MessageResponse;
import com.spamascotas.spa_mascotas_api.dto.response.Setup2faResponse;
import com.spamascotas.spa_mascotas_api.model.Cliente;
import com.spamascotas.spa_mascotas_api.model.Rol;
import com.spamascotas.spa_mascotas_api.model.Usuario;
import com.spamascotas.spa_mascotas_api.model.enums.EstadoUsuario;
import com.spamascotas.spa_mascotas_api.model.enums.RolEnum;
import com.spamascotas.spa_mascotas_api.model.enums.TipoAccion;
import com.spamascotas.spa_mascotas_api.repository.ClienteRepository;
import com.spamascotas.spa_mascotas_api.repository.RolRepository;
import com.spamascotas.spa_mascotas_api.repository.UsuarioRepository;
import com.spamascotas.spa_mascotas_api.security.JwtUtil;
import com.spamascotas.spa_mascotas_api.security.TotpService;
import com.spamascotas.spa_mascotas_api.service.admin.AuditService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private static final int MAX_INTENTOS  = 5;
    private static final int MIN_BLOQUEO   = 15;
    private static final int MIN_TOKEN_ACT = 15;

    private final UsuarioRepository usuarioRepository;
    private final RolRepository     rolRepository;
    private final ClienteRepository clienteRepository;
    private final PasswordEncoder   passwordEncoder;
    private final JwtUtil           jwtUtil;
    private final EmailService      emailService;
    private final TotpService       totpService;
    private final AuditService      auditService;

    // ── Registro de cliente ──────────────────────────────────

    @Transactional
    public MessageResponse registrar(RegisterRequest request) {
        if (usuarioRepository.existsByCorreo(request.getCorreo())) {
            throw new RuntimeException("Ya existe una cuenta con ese correo");
        }

        if (request.getNombreUsuario() != null && !request.getNombreUsuario().isBlank()
                && usuarioRepository.existsByNombreUsuario(request.getNombreUsuario())) {
            throw new RuntimeException("Ese nombre de usuario ya está en uso");
        }

        Rol rolCliente = rolRepository.findByNombre(RolEnum.CLIENTE)
                .orElseThrow(() -> new RuntimeException("Rol CLIENTE no encontrado. Ejecuta el schema SQL."));

        String token = UUID.randomUUID().toString();

        Usuario usuario = new Usuario();
        usuario.setCorreo(request.getCorreo());
        usuario.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        usuario.setEstado(EstadoUsuario.PENDIENTE);
        usuario.setTokenActivacion(token);
        usuario.setTokenActivacionExp(LocalDateTime.now().plusMinutes(MIN_TOKEN_ACT));
        if (request.getNombreUsuario() != null && !request.getNombreUsuario().isBlank()) {
            usuario.setNombreUsuario(request.getNombreUsuario());
        }
        usuario.getRoles().add(rolCliente);
        usuarioRepository.save(usuario);

        if (request.getNombre() != null && !request.getNombre().isBlank()) {
            Cliente cliente = Cliente.builder()
                    .usuario(usuario)
                    .nombre(request.getNombre())
                    .ci(request.getCi())
                    .telefono(request.getTelefono())
                    .build();
            clienteRepository.save(cliente);
        }

        emailService.enviarActivacion(request.getCorreo(), token);
        auditService.registrar(TipoAccion.REGISTRO, request.getCorreo(), "CLIENTE", true,
                "Nombre: " + (request.getNombre() != null ? request.getNombre() : "-")
                + " | CI: " + (request.getCi() != null ? request.getCi() : "-"));

        return new MessageResponse("Registro exitoso. Revisa tu correo para activar la cuenta.");
    }

    // ── Activación ───────────────────────────────────────────

    @Transactional
    public MessageResponse activarCuenta(String token) {
        Usuario usuario = usuarioRepository.findByTokenActivacion(token)
                .orElseThrow(() -> new RuntimeException("Token de activación inválido o ya utilizado"));

        if (LocalDateTime.now().isAfter(usuario.getTokenActivacionExp())) {
            throw new RuntimeException("El link de activación ha expirado. Solicita uno nuevo.");
        }

        usuario.setEstado(EstadoUsuario.ACTIVO);
        usuario.setTokenActivacion(null);
        usuario.setTokenActivacionExp(null);
        usuarioRepository.save(usuario);
        auditService.registrar(TipoAccion.ACTIVACION_CUENTA, usuario.getCorreo(), "CLIENTE", true, null);

        return new MessageResponse("¡Cuenta activada exitosamente! Ya puedes iniciar sesión.");
    }

    // ── Login ────────────────────────────────────────────────

    // noRollbackFor: los RuntimeException son errores esperados (credenciales malas, 2FA inválido, etc.)
    // Sin esto, Spring hace rollback al lanzar la excepción y el contador de intentos_fallidos
    // nunca se persiste, rompiendo el mecanismo de bloqueo.
    @Transactional(noRollbackFor = RuntimeException.class)
    public AuthResponse login(LoginRequest request) {
        Usuario usuario = usuarioRepository.findByCorreo(request.getIdentificador())
                .or(() -> usuarioRepository.findByNombreUsuario(request.getIdentificador()))
                .orElseThrow(() -> new RuntimeException("Credenciales inválidas"));

        if (usuario.estaBloqueado()) {
            throw new RuntimeException("Cuenta bloqueada temporalmente. Intenta en " + MIN_BLOQUEO + " minutos.");
        }

        // Google-only accounts have no password hash
        if (usuario.getPasswordHash() == null) {
            throw new RuntimeException("Esta cuenta usa inicio de sesión con Google. Usa el botón 'Continuar con Google'.");
        }

        if (!passwordEncoder.matches(request.getPassword(), usuario.getPasswordHash())) {
            registrarIntentoFallido(usuario);
            auditService.registrar(TipoAccion.LOGIN_FALLIDO, usuario.getCorreo(), null, false, "Contraseña incorrecta");
            throw new RuntimeException("Credenciales inválidas");
        }

        if (usuario.getEstado() == EstadoUsuario.PENDIENTE) {
            throw new RuntimeException("Cuenta no activada. Revisa tu correo.");
        }
        if (usuario.getEstado() == EstadoUsuario.INACTIVO) {
            throw new RuntimeException("Cuenta inactiva. Contacta al administrador.");
        }

        String rolNombre = usuario.getRoles().stream()
                .findFirst()
                .map(r -> r.getNombre().name())
                .orElse("CLIENTE");

        // Staff and admin require 2FA — if not set up, force setup first
        if (!RolEnum.CLIENTE.name().equals(rolNombre) && !usuario.getTotpHabilitado()) {
            return AuthResponse.builder()
                    .requiere2faSetup(true)
                    .correo(usuario.getCorreo())
                    .rol(rolNombre)
                    .totpHabilitado(false)
                    .build();
        }

        // Any role with 2FA enabled requires TOTP code
        if (usuario.getTotpHabilitado()) {
            if (request.getCodigoTotp() == null) {
                return AuthResponse.builder()
                        .requiere2fa(true)
                        .correo(usuario.getCorreo())
                        .build();
            }
            if (!totpService.verificar(usuario.getTotpSecret(), request.getCodigoTotp())) {
                auditService.registrar(TipoAccion.LOGIN_FALLIDO, usuario.getCorreo(), rolNombre, false, "Código 2FA inválido");
                throw new RuntimeException("Código 2FA inválido");
            }
        }

        resetearIntentos(usuario);

        String accessToken  = jwtUtil.generateAccessToken(usuario.getCorreo(), rolNombre);
        String refreshToken = jwtUtil.generateRefreshToken(usuario.getCorreo());
        auditService.registrar(TipoAccion.LOGIN_EXITOSO, usuario.getCorreo(), rolNombre, true, null);
        log.info("Login exitoso: {} ({})", usuario.getCorreo(), rolNombre);

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .correo(usuario.getCorreo())
                .nombreUsuario(usuario.getNombreUsuario())
                .rol(rolNombre)
                .requiere2fa(false)
                .totpHabilitado(usuario.getTotpHabilitado())
                .build();
    }

    // ── Refresh token ────────────────────────────────────────

    public AuthResponse refreshToken(String refreshToken) {
        if (!jwtUtil.isValid(refreshToken)) {
            throw new RuntimeException("Refresh token inválido o expirado");
        }
        String correo = jwtUtil.extractCorreo(refreshToken);
        Usuario usuario = usuarioRepository.findByCorreo(correo)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        String rolNombre = usuario.getRoles().stream()
                .findFirst()
                .map(r -> r.getNombre().name())
                .orElse("CLIENTE");

        return AuthResponse.builder()
                .accessToken(jwtUtil.generateAccessToken(correo, rolNombre))
                .refreshToken(jwtUtil.generateRefreshToken(correo))
                .correo(correo)
                .nombreUsuario(usuario.getNombreUsuario())
                .rol(rolNombre)
                .requiere2fa(false)
                .totpHabilitado(usuario.getTotpHabilitado())
                .build();
    }

    // ── 2FA — setup inicial (pre-login, con contraseña) ─────

    @Transactional
    public Setup2faResponse init2faSetup(String correo, String password) {
        Usuario u = usuarioRepository.findByCorreo(correo)
                .orElseThrow(() -> new RuntimeException("Credenciales inválidas"));

        if (u.getPasswordHash() == null || !passwordEncoder.matches(password, u.getPasswordHash())) {
            throw new RuntimeException("Credenciales inválidas");
        }
        if (u.getTotpHabilitado()) {
            throw new RuntimeException("El 2FA ya está habilitado en esta cuenta");
        }

        String secret = u.getTotpSecret() != null ? u.getTotpSecret() : totpService.generarSecret();
        u.setTotpSecret(secret);
        usuarioRepository.save(u);

        return new Setup2faResponse(totpService.urlQr(correo, secret), secret);
    }

    @Transactional
    public AuthResponse enable2fa(String correo, String password, int codigo) {
        Usuario u = usuarioRepository.findByCorreo(correo)
                .orElseThrow(() -> new RuntimeException("Credenciales inválidas"));

        if (u.getPasswordHash() == null || !passwordEncoder.matches(password, u.getPasswordHash())) {
            throw new RuntimeException("Credenciales inválidas");
        }
        if (u.getTotpSecret() == null) {
            throw new RuntimeException("Inicia el proceso de configuración primero (/2fa/init)");
        }
        if (!totpService.verificar(u.getTotpSecret(), codigo)) {
            throw new RuntimeException("Código de verificación inválido. Revisa tu app de autenticación.");
        }

        u.setTotpHabilitado(true);
        u.setIntentosFallidos(0);
        u.setUltimaActividad(LocalDateTime.now());
        usuarioRepository.save(u);

        String rolNombre = u.getRoles().stream().findFirst()
                .map(r -> r.getNombre().name()).orElse("CLIENTE");

        auditService.registrar(TipoAccion.ACCESO_MODULO, correo, rolNombre, true, "2FA habilitado");
        log.info("2FA habilitado para: {}", correo);

        return AuthResponse.builder()
                .accessToken(jwtUtil.generateAccessToken(correo, rolNombre))
                .refreshToken(jwtUtil.generateRefreshToken(correo))
                .correo(correo)
                .nombreUsuario(u.getNombreUsuario())
                .rol(rolNombre)
                .totpHabilitado(true)
                .build();
    }

    // ── Reset de contraseña ──────────────────────────────────

    @Transactional
    public MessageResponse solicitarReset(String identificador) {
        Usuario usuario = usuarioRepository.findByCorreo(identificador)
                .or(() -> usuarioRepository.findByNombreUsuario(identificador))
                .orElseThrow(() -> new RuntimeException("No existe una cuenta con ese correo o usuario"));

        // Si es cuenta Google-only sin password, no permite reset
        if (usuario.getPasswordHash() == null) {
            throw new RuntimeException("Esta cuenta usa Google. Inicia sesión con Google.");
        }

        String token = UUID.randomUUID().toString();
        usuario.setResetToken(token);
        usuario.setResetTokenExp(LocalDateTime.now().plusMinutes(15));
        usuarioRepository.save(usuario);

        emailService.enviarRestablecimiento(usuario.getCorreo(), token);
        auditService.registrar(TipoAccion.RESETEO_PASSWORD, usuario.getCorreo(), null, true,
                "Solicitud de restablecimiento de contraseña");

        return new MessageResponse("Se envió un enlace de restablecimiento a tu correo.");
    }

    @Transactional
    public MessageResponse resetearPassword(String token, String nuevaPassword) {
        Usuario usuario = usuarioRepository.findByResetToken(token)
                .orElseThrow(() -> new RuntimeException("Token inválido o ya utilizado"));

        if (LocalDateTime.now().isAfter(usuario.getResetTokenExp())) {
            throw new RuntimeException("El enlace ha expirado. Solicita uno nuevo.");
        }

        usuario.setPasswordHash(passwordEncoder.encode(nuevaPassword));
        usuario.setResetToken(null);
        usuario.setResetTokenExp(null);
        usuario.setIntentosFallidos(0);
        usuario.setBloqueadoHasta(null);
        usuarioRepository.save(usuario);

        auditService.registrar(TipoAccion.RESETEO_PASSWORD, usuario.getCorreo(), null, true,
                "Contraseña restablecida exitosamente");
        return new MessageResponse("Contraseña restablecida exitosamente. Ya puedes iniciar sesión.");
    }

    // ── Intentos fallidos / bloqueo ──────────────────────────

    private void registrarIntentoFallido(Usuario usuario) {
        int intentos = usuario.getIntentosFallidos() + 1;
        usuario.setIntentosFallidos(intentos);
        if (intentos >= MAX_INTENTOS) {
            usuario.setBloqueadoHasta(LocalDateTime.now().plusMinutes(MIN_BLOQUEO));
            auditService.registrar(TipoAccion.CUENTA_BLOQUEADA, usuario.getCorreo(), null, false,
                    "Bloqueada " + MIN_BLOQUEO + " min tras " + intentos + " intentos");
            log.warn("Cuenta bloqueada {} min: {}", MIN_BLOQUEO, usuario.getCorreo());
        }
        usuarioRepository.save(usuario);
    }

    private void resetearIntentos(Usuario usuario) {
        usuario.setIntentosFallidos(0);
        usuario.setBloqueadoHasta(null);
        usuario.setUltimaActividad(LocalDateTime.now());
        usuarioRepository.save(usuario);
    }
}
