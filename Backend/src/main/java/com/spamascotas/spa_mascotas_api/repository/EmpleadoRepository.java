package com.spamascotas.spa_mascotas_api.repository;

import com.spamascotas.spa_mascotas_api.model.Empleado;
import com.spamascotas.spa_mascotas_api.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EmpleadoRepository extends JpaRepository<Empleado, Long> {
    Optional<Empleado> findByUsuario(Usuario usuario);
    Optional<Empleado> findByUsuarioCorreo(String correo);
    List<Empleado> findByPuesto(String puesto);
}
