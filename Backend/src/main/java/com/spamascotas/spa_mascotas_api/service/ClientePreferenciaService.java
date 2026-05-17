package com.spamascotas.spa_mascotas_api.service;

import com.spamascotas.spa_mascotas_api.dto.request.PreferenciaRequest;
import com.spamascotas.spa_mascotas_api.dto.response.PreferenciaResponse;
import com.spamascotas.spa_mascotas_api.model.Cliente;
import com.spamascotas.spa_mascotas_api.model.ClientePreferencia;
import com.spamascotas.spa_mascotas_api.repository.ClientePreferenciaRepository;
import com.spamascotas.spa_mascotas_api.repository.ClienteRepository;
import com.spamascotas.spa_mascotas_api.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ClientePreferenciaService {

    private final ClientePreferenciaRepository repo;
    private final ClienteRepository clienteRepo;
    private final UsuarioRepository usuarioRepo;

    public List<PreferenciaResponse> listar(String correo) {
        Cliente cliente = getCliente(correo);
        return repo.findByCliente(cliente).stream()
            .map(p -> new PreferenciaResponse(p.getId(), p.getNombre(), p.getValor()))
            .collect(Collectors.toList());
    }

    @Transactional
    public PreferenciaResponse guardar(String correo, PreferenciaRequest request) {
        Cliente cliente = getCliente(correo);
        ClientePreferencia pref = repo.findByClienteAndNombre(cliente, request.getNombre())
            .orElse(ClientePreferencia.builder().cliente(cliente).nombre(request.getNombre()).build());
        pref.setValor(request.getValor());
        pref = repo.save(pref);
        return new PreferenciaResponse(pref.getId(), pref.getNombre(), pref.getValor());
    }

    @Transactional
    public void eliminar(String correo, Long id) {
        Cliente cliente = getCliente(correo);
        ClientePreferencia pref = repo.findById(id)
            .orElseThrow(() -> new RuntimeException("Preferencia no encontrada"));
        if (!pref.getCliente().getId().equals(cliente.getId())) {
            throw new RuntimeException("No tienes permiso para eliminar esta preferencia");
        }
        repo.delete(pref);
    }

    private Cliente getCliente(String correo) {
        var usuario = usuarioRepo.findByCorreo(correo)
            .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
        return clienteRepo.findByUsuario(usuario)
            .orElseThrow(() -> new RuntimeException("Perfil de cliente no encontrado"));
    }
}
