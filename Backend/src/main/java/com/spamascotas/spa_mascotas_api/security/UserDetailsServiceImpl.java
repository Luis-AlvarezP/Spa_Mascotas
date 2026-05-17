package com.spamascotas.spa_mascotas_api.security;

import com.spamascotas.spa_mascotas_api.model.Usuario;
import com.spamascotas.spa_mascotas_api.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UsuarioRepository usuarioRepository;

    @Override
    public UserDetails loadUserByUsername(String correo) throws UsernameNotFoundException {
        Usuario usuario = usuarioRepository.findByCorreo(correo)
                .or(() -> usuarioRepository.findByNombreUsuario(correo))
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado: " + correo));

        List<GrantedAuthority> authorities = usuario.getRoles().stream()
                .map(rol -> new SimpleGrantedAuthority("ROLE_" + rol.getNombre().name()))
                .collect(Collectors.toList());

        return new User(
                usuario.getCorreo(),
                usuario.getPasswordHash() != null ? usuario.getPasswordHash() : "",
                authorities
        );
    }
}
