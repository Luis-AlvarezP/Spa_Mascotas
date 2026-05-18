package com.spamascotas.spa_mascotas_api.service.admin;

import com.spamascotas.spa_mascotas_api.dto.response.ClienteAdminResponse;
import com.spamascotas.spa_mascotas_api.model.Cliente;
import com.spamascotas.spa_mascotas_api.model.enums.EstadoUsuario;
import com.spamascotas.spa_mascotas_api.model.enums.TipoAccion;
import com.spamascotas.spa_mascotas_api.repository.ClientePreferenciaRepository;
import com.spamascotas.spa_mascotas_api.repository.ClienteRepository;
import com.spamascotas.spa_mascotas_api.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminClienteService {

    private final ClienteRepository clienteRepository;
    private final UsuarioRepository usuarioRepository;
    private final ClientePreferenciaRepository preferenciaRepository;
    private final AuditService auditService;

    @Transactional(readOnly = true)
    public List<ClienteAdminResponse> listarClientes() {
        return clienteRepository.findAll().stream()
            .map(this::toResponse)
            .collect(Collectors.toList());
    }

    @Transactional
    public ClienteAdminResponse toggleEstadoCliente(Long clienteId, String adminCorreo) {
        Cliente cliente = clienteRepository.findById(clienteId)
            .orElseThrow(() -> new RuntimeException("Cliente no encontrado"));

        var usuario = cliente.getUsuario();
        EstadoUsuario nuevo = usuario.getEstado() == EstadoUsuario.ACTIVO
            ? EstadoUsuario.INACTIVO : EstadoUsuario.ACTIVO;
        usuario.setEstado(nuevo);
        usuarioRepository.save(usuario);

        TipoAccion accionCliente = nuevo == EstadoUsuario.INACTIVO
            ? TipoAccion.DESACTIVAR_USUARIO : TipoAccion.ACTIVAR_USUARIO;
        auditService.registrar(accionCliente, adminCorreo, "ADMIN", true,
            "Nombre: " + (cliente.getNombre() != null ? cliente.getNombre() : "-")
            + " | Correo: " + usuario.getCorreo()
            + " | Estado: " + nuevo);

        return toResponse(cliente);
    }

    private ClienteAdminResponse toResponse(Cliente c) {
        var u = c.getUsuario();
        Map<String, String> preferencias = preferenciaRepository.findByCliente(c).stream()
            .collect(Collectors.toMap(p -> p.getNombre(), p -> p.getValor() != null ? p.getValor() : ""));
        return ClienteAdminResponse.builder()
            .id(c.getId())
            .usuarioId(u.getId())
            .correo(u.getCorreo())
            .nombre(c.getNombre())
            .ci(c.getCi())
            .telefono(c.getTelefono())
            .direccion(c.getDireccion())
            .estado(u.getEstado().name())
            .totpHabilitado(u.getTotpHabilitado())
            .mascotas(List.of())
            .preferencias(preferencias)
            .build();
    }
}
