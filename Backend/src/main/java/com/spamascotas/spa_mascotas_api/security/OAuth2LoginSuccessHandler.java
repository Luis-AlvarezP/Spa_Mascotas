package com.spamascotas.spa_mascotas_api.security;

import com.spamascotas.spa_mascotas_api.model.Cliente;
import com.spamascotas.spa_mascotas_api.model.Rol;
import com.spamascotas.spa_mascotas_api.model.Usuario;
import com.spamascotas.spa_mascotas_api.model.enums.EstadoUsuario;
import com.spamascotas.spa_mascotas_api.model.enums.RolEnum;
import com.spamascotas.spa_mascotas_api.repository.ClienteRepository;
import com.spamascotas.spa_mascotas_api.repository.RolRepository;
import com.spamascotas.spa_mascotas_api.repository.UsuarioRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2LoginSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;
    private final ClienteRepository clienteRepository;
    private final JwtUtil jwtUtil;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    @Override
    @Transactional
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();

        String correo   = oAuth2User.getAttribute("email");
        String nombre   = oAuth2User.getAttribute("name");
        String googleId = oAuth2User.getAttribute("sub");

        if (correo == null) {
            response.sendRedirect(frontendUrl + "/auth/login?error=no_email");
            return;
        }

        boolean esNuevo = false;
        Usuario usuario = usuarioRepository.findByCorreo(correo).orElse(null);

        if (usuario == null) {
            Rol rolCliente = rolRepository.findByNombre(RolEnum.CLIENTE)
                    .orElseThrow(() -> new IllegalStateException("Rol CLIENTE no encontrado"));

            usuario = new Usuario();
            usuario.setCorreo(correo);
            usuario.setGoogleId(googleId);
            usuario.setEstado(EstadoUsuario.ACTIVO);
            usuario.getRoles().add(rolCliente);
            usuarioRepository.save(usuario);

            String displayName = nombre != null ? nombre : correo.split("@")[0];
            Cliente cliente = Cliente.builder()
                    .usuario(usuario)
                    .nombre(displayName)
                    .build();
            clienteRepository.save(cliente);
            esNuevo = true;
            log.info("Nuevo usuario OAuth2 registrado: {}", correo);
        } else if (usuario.getGoogleId() == null) {
            usuario.setGoogleId(googleId);
            usuarioRepository.save(usuario);
        }

        if (usuario.getEstado() == EstadoUsuario.INACTIVO) {
            response.sendRedirect(frontendUrl + "/auth/login?error=cuenta_inactiva");
            return;
        }

        String rolNombre = usuario.getRoles().stream()
                .findFirst()
                .map(r -> r.getNombre().name())
                .orElse("CLIENTE");

        String token = jwtUtil.generateAccessToken(correo, rolNombre);

        String nuParam = usuario.getNombreUsuario() != null
                ? "&nombreUsuario=" + URLEncoder.encode(usuario.getNombreUsuario(), StandardCharsets.UTF_8)
                : "";

        String redirectUrl = frontendUrl + "/auth/oauth-callback"
                + "?token=" + URLEncoder.encode(token, StandardCharsets.UTF_8)
                + "&correo=" + URLEncoder.encode(correo, StandardCharsets.UTF_8)
                + "&rol=" + URLEncoder.encode(rolNombre, StandardCharsets.UTF_8)
                + "&setup=" + esNuevo
                + nuParam;

        response.sendRedirect(redirectUrl);
    }
}
