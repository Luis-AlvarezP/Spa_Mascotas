package com.spamascotas.spa_mascotas_api.repository;

import com.spamascotas.spa_mascotas_api.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
    Optional<Usuario> findByCorreo(String correo);
    boolean existsByCorreo(String correo);
    Optional<Usuario> findByTokenActivacion(String token);
    Optional<Usuario> findByNombreUsuario(String nombreUsuario);
    Optional<Usuario> findByResetToken(String resetToken);
    boolean existsByNombreUsuario(String nombreUsuario);
}
