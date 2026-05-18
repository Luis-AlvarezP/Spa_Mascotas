package com.spamascotas.spa_mascotas_api.config;

import com.spamascotas.spa_mascotas_api.model.CatTemperamento;
import com.spamascotas.spa_mascotas_api.model.Cliente;
import com.spamascotas.spa_mascotas_api.model.Empleado;
import com.spamascotas.spa_mascotas_api.model.HorarioTrabajo;
import com.spamascotas.spa_mascotas_api.model.Rol;
import com.spamascotas.spa_mascotas_api.model.Usuario;
import com.spamascotas.spa_mascotas_api.model.enums.EstadoUsuario;
import com.spamascotas.spa_mascotas_api.model.enums.RolEnum;
import com.spamascotas.spa_mascotas_api.repository.CatTemperamentoRepository;
import com.spamascotas.spa_mascotas_api.repository.ClienteRepository;
import com.spamascotas.spa_mascotas_api.repository.EmpleadoRepository;
import com.spamascotas.spa_mascotas_api.repository.HorarioTrabajoRepository;
import com.spamascotas.spa_mascotas_api.repository.RolRepository;
import com.spamascotas.spa_mascotas_api.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;

@Component
@RequiredArgsConstructor
public class DataInitializer implements ApplicationRunner {

    private final RolRepository rolRepository;
    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final ClienteRepository clienteRepository;
    private final EmpleadoRepository empleadoRepository;
    private final HorarioTrabajoRepository horarioTrabajoRepository;
    private final CatTemperamentoRepository catTemperamentoRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        inicializarRoles();
        inicializarAdmin();
        inicializarCliente();
        inicializarGroomer();
        inicializarRecepcion();
        inicializarHorariosGroomer();
        inicializarTemperamentos();
    }

    private void inicializarRoles() {
        for (RolEnum rolEnum : RolEnum.values()) {
            if (rolRepository.findByNombre(rolEnum).isEmpty()) {
                rolRepository.save(new Rol(rolEnum));
            }
        }
    }

    private void inicializarAdmin() {
        if (usuarioRepository.existsByCorreo("admin@gmail.com")) {
            return;
        }

        Rol adminRol = rolRepository.findByNombre(RolEnum.ADMIN)
                .orElseThrow(() -> new IllegalStateException("Rol ADMIN no encontrado tras inicialización"));

        Usuario admin = new Usuario();
        admin.setCorreo("admin@gmail.com");
        admin.setPasswordHash(passwordEncoder.encode("123456"));
        admin.setEstado(EstadoUsuario.ACTIVO);
        admin.getRoles().add(adminRol);
        usuarioRepository.save(admin);
    }

    private void inicializarCliente() {
        String correo = "cliente@spamascotas.com";
        if (usuarioRepository.existsByCorreo(correo)) {
            return;
        }

        Rol rolCliente = rolRepository.findByNombre(RolEnum.CLIENTE)
                .orElseThrow(() -> new IllegalStateException("Rol CLIENTE no encontrado"));

        Usuario usuario = new Usuario();
        usuario.setCorreo(correo);
        usuario.setPasswordHash(passwordEncoder.encode("123456"));
        usuario.setEstado(EstadoUsuario.ACTIVO);
        usuario.getRoles().add(rolCliente);
        usuarioRepository.save(usuario);

        Cliente cliente = Cliente.builder()
                .usuario(usuario)
                .nombre("Ana García")
                .build();
        clienteRepository.save(cliente);
    }

    private void inicializarGroomer() {
        String correo = "groomer@spamascotas.com";
        if (usuarioRepository.existsByCorreo(correo)) {
            return;
        }

        Rol rolGroomer = rolRepository.findByNombre(RolEnum.GROOMER)
                .orElseThrow(() -> new IllegalStateException("Rol GROOMER no encontrado"));

        Usuario usuario = new Usuario();
        usuario.setCorreo(correo);
        usuario.setPasswordHash(passwordEncoder.encode("123456"));
        usuario.setEstado(EstadoUsuario.ACTIVO);
        usuario.getRoles().add(rolGroomer);
        usuarioRepository.save(usuario);

        Empleado empleado = Empleado.builder()
                .usuario(usuario)
                .nombre("Carlos Ruiz")
                .puesto(RolEnum.GROOMER.name())
                .build();
        empleadoRepository.save(empleado);
    }

    private void inicializarRecepcion() {
        String correo = "recepcion@spamascotas.com";
        if (usuarioRepository.existsByCorreo(correo)) {
            return;
        }

        Rol rolRecepcion = rolRepository.findByNombre(RolEnum.RECEPCION)
                .orElseThrow(() -> new IllegalStateException("Rol RECEPCION no encontrado"));

        Usuario usuario = new Usuario();
        usuario.setCorreo(correo);
        usuario.setPasswordHash(passwordEncoder.encode("123456"));
        usuario.setEstado(EstadoUsuario.ACTIVO);
        usuario.getRoles().add(rolRecepcion);
        usuarioRepository.save(usuario);

        Empleado empleado = Empleado.builder()
                .usuario(usuario)
                .nombre("Laura Méndez")
                .puesto(RolEnum.RECEPCION.name())
                .build();
        empleadoRepository.save(empleado);
    }

    private void inicializarTemperamentos() {
        String[][] datos = {
            {"Tranquilo", "#4ade80"},
            {"Nervioso",  "#fbbf24"},
            {"Agresivo",  "#f87171"},
            {"Inquieto",  "#a78bfa"}
        };
        for (String[] d : datos) {
            if (catTemperamentoRepository.findByNombre(d[0]).isEmpty()) {
                catTemperamentoRepository.save(
                    CatTemperamento.builder().nombre(d[0]).colorAlerta(d[1]).build()
                );
            }
        }
    }

    private void inicializarHorariosGroomer() {
        usuarioRepository.findByCorreo("groomer@spamascotas.com").ifPresent(usuario ->
            empleadoRepository.findByUsuario(usuario).ifPresent(empleado -> {
                if (!horarioTrabajoRepository.findByEmpleadoOrderByDiaSemana(empleado).isEmpty()) {
                    return;
                }
                String[] dias = {"LUNES", "MARTES", "MIERCOLES", "JUEVES", "VIERNES"};
                for (String dia : dias) {
                    horarioTrabajoRepository.save(HorarioTrabajo.builder()
                            .empleado(empleado)
                            .diaSemana(dia)
                            .horaInicio(LocalTime.of(9, 0))
                            .horaFin(LocalTime.of(18, 0))
                            .inicioAlmuerzo(LocalTime.of(13, 0))
                            .finAlmuerzo(LocalTime.of(14, 0))
                            .vigenteDesde(LocalDate.of(2025, 1, 1))
                            .capacidadMaxima(8)
                            .build());
                }
            })
        );
    }
}