package com.spamascotas.spa_mascotas_api.service.mascotas;

import com.spamascotas.spa_mascotas_api.dto.request.MascotaRequest;
import com.spamascotas.spa_mascotas_api.dto.request.RestriccionRequest;
import com.spamascotas.spa_mascotas_api.dto.response.MascotaResponse;
import com.spamascotas.spa_mascotas_api.dto.response.RestriccionResponse;
import com.spamascotas.spa_mascotas_api.dto.response.TemperamentoResponse;
import com.spamascotas.spa_mascotas_api.model.CatTemperamento;
import com.spamascotas.spa_mascotas_api.model.Cliente;
import com.spamascotas.spa_mascotas_api.model.Mascota;
import com.spamascotas.spa_mascotas_api.model.RestriccionMascota;
import com.spamascotas.spa_mascotas_api.model.Usuario;
import com.spamascotas.spa_mascotas_api.repository.CatTemperamentoRepository;
import com.spamascotas.spa_mascotas_api.repository.ClienteRepository;
import com.spamascotas.spa_mascotas_api.repository.MascotaRepository;
import com.spamascotas.spa_mascotas_api.repository.RestriccionMascotaRepository;
import com.spamascotas.spa_mascotas_api.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MascotaService {

    private final MascotaRepository mascotaRepo;
    private final CatTemperamentoRepository temperamentoRepo;
    private final ClienteRepository clienteRepo;
    private final UsuarioRepository usuarioRepo;
    private final RestriccionMascotaRepository restriccionRepo;

    @Value("${app.uploads.dir:uploads}")
    private String uploadsDir;

    // ── Temperamentos ────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<TemperamentoResponse> listarTemperamentos() {
        return temperamentoRepo.findAllByOrderByNombreAsc().stream()
                .map(t -> TemperamentoResponse.builder()
                        .id(t.getId()).nombre(t.getNombre()).colorAlerta(t.getColorAlerta())
                        .build())
                .toList();
    }

    // ── CLIENTE: mis mascotas ────────────────────────────────────

    @Transactional(readOnly = true)
    public List<MascotaResponse> misMascotas(String correo) {
        Cliente cliente = getCliente(correo);
        return mascotaRepo.findByClienteOrderByNombreAsc(cliente).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public MascotaResponse registrar(MascotaRequest req, String correo) {
        Cliente cliente = getCliente(correo);

        Mascota m = Mascota.builder()
                .nombre(req.getNombre())
                .especie(req.getEspecie())
                .raza(req.getRaza())
                .tamano(req.getTamano())
                .fechaNacimiento(parseDate(req.getFechaNacimiento()))
                .alergias(req.getAlergias())
                .temperamento(resolveTemperamento(req.getTemperamentoId()))
                .cliente(cliente)
                .build();

        Mascota saved = mascotaRepo.save(m);
        saveRestricciones(saved, req.getRestricciones());
        return toResponse(saved);
    }

    @Transactional
    public MascotaResponse actualizar(Long id, MascotaRequest req, String correo) {
        Mascota m = findOwnMascota(id, correo);

        m.setNombre(req.getNombre());
        m.setEspecie(req.getEspecie());
        m.setRaza(req.getRaza());
        m.setTamano(req.getTamano());
        m.setFechaNacimiento(parseDate(req.getFechaNacimiento()));
        m.setAlergias(req.getAlergias());
        m.setTemperamento(resolveTemperamento(req.getTemperamentoId()));

        Mascota saved = mascotaRepo.save(m);
        restriccionRepo.deleteByMascota(saved);
        saveRestricciones(saved, req.getRestricciones());
        return toResponse(saved);
    }

    @Transactional
    public void eliminar(Long id, String correo) {
        Mascota m = findOwnMascota(id, correo);
        restriccionRepo.deleteByMascota(m);
        mascotaRepo.delete(m);
    }

    @Transactional
    public String subirCarnet(Long id, MultipartFile file, String correo) {
        Mascota m = findOwnMascota(id, correo);
        String url = saveFile(file, "carnets");
        m.setUrlCarnet(url);
        mascotaRepo.save(m);
        return url;
    }

    @Transactional
    public String subirFoto(Long id, MultipartFile file, String correo) {
        Mascota m = findOwnMascota(id, correo);
        String url = saveFile(file, "fotos");
        m.setUrlFotoMascota(url);
        mascotaRepo.save(m);
        return url;
    }

    // ── STAFF: mascotas de un cliente ────────────────────────────

    @Transactional(readOnly = true)
    public List<MascotaResponse> mascotasByCliente(Long clienteId) {
        Cliente cliente = clienteRepo.findById(clienteId)
                .orElseThrow(() -> new RuntimeException("Cliente no encontrado"));
        return mascotaRepo.findByClienteOrderByNombreAsc(cliente).stream()
                .map(this::toResponse)
                .toList();
    }

    // ── Helpers ──────────────────────────────────────────────────

    private Cliente getCliente(String correo) {
        Usuario usuario = usuarioRepo.findByCorreo(correo)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
        return clienteRepo.findByUsuario(usuario)
                .orElseThrow(() -> new RuntimeException("Perfil de cliente no encontrado"));
    }

    private Mascota findOwnMascota(Long id, String correo) {
        Cliente cliente = getCliente(correo);
        Mascota m = mascotaRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Mascota no encontrada"));
        if (!m.getCliente().getId().equals(cliente.getId())) {
            throw new RuntimeException("Sin permisos sobre esta mascota");
        }
        return m;
    }

    private void saveRestricciones(Mascota mascota, List<RestriccionRequest> lista) {
        if (lista == null || lista.isEmpty()) return;
        for (RestriccionRequest r : lista) {
            if (r.getDescripcion() != null && !r.getDescripcion().isBlank()) {
                restriccionRepo.save(RestriccionMascota.builder()
                        .mascota(mascota)
                        .tipo(r.getTipo())
                        .descripcion(r.getDescripcion())
                        .gravedad(r.getGravedad())
                        .build());
            }
        }
    }

    private String saveFile(MultipartFile file, String subdir) {
        String original = file.getOriginalFilename();
        String baseName = (original != null) ? Paths.get(original).getFileName().toString() : "file";
        String filename = UUID.randomUUID() + "_" + baseName;
        Path dir = Paths.get(uploadsDir, subdir).toAbsolutePath();
        try {
            Files.createDirectories(dir);
            Files.copy(file.getInputStream(), dir.resolve(filename));
        } catch (IOException e) {
            throw new RuntimeException("Error al guardar el archivo: " + e.getMessage());
        }
        return "/api/files/" + subdir + "/" + filename;
    }

    private CatTemperamento resolveTemperamento(Long tempId) {
        if (tempId == null) return null;
        return temperamentoRepo.findById(tempId).orElse(null);
    }

    private LocalDate parseDate(String s) {
        if (s == null || s.isBlank()) return null;
        return LocalDate.parse(s);
    }

    private MascotaResponse toResponse(Mascota m) {
        List<RestriccionResponse> restricciones = restriccionRepo.findByMascota(m).stream()
                .map(r -> RestriccionResponse.builder()
                        .id(r.getId()).tipo(r.getTipo())
                        .descripcion(r.getDescripcion()).gravedad(r.getGravedad())
                        .build())
                .toList();

        return MascotaResponse.builder()
                .id(m.getId())
                .nombre(m.getNombre())
                .especie(m.getEspecie())
                .raza(m.getRaza())
                .tamano(m.getTamano())
                .fechaNacimiento(m.getFechaNacimiento())
                .alergias(m.getAlergias())
                .temperamentoId(m.getTemperamento() != null ? m.getTemperamento().getId() : null)
                .temperamentoNombre(m.getTemperamento() != null ? m.getTemperamento().getNombre() : null)
                .temperamentoColor(m.getTemperamento() != null ? m.getTemperamento().getColorAlerta() : null)
                .urlFotoMascota(m.getUrlFotoMascota())
                .urlCarnet(m.getUrlCarnet())
                .restricciones(restricciones)
                .build();
    }
}
