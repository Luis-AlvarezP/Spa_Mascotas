package com.spamascotas.spa_mascotas_api.service.grooming;

import com.spamascotas.spa_mascotas_api.dto.request.FichaRequest;
import com.spamascotas.spa_mascotas_api.dto.request.InsumoGroomingRequest;
import com.spamascotas.spa_mascotas_api.dto.response.FichaResponse;
import com.spamascotas.spa_mascotas_api.dto.response.InsumoGroomingResponse;
import com.spamascotas.spa_mascotas_api.model.*;
import com.spamascotas.spa_mascotas_api.repository.*;
import com.spamascotas.spa_mascotas_api.service.auth.EmailService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FichaGroomingService {

    private final FichaIngresoRepository fichaRepo;
    private final FichaDetalleRepository detalleRepo;
    private final GaleriaMascotaRepository galeriaRepo;
    private final RecomendacionRepository recomendacionRepo;
    private final CitaRepository citaRepo;
    private final EmpleadoRepository empleadoRepo;
    private final ProductoRepository productoRepo;
    private final MovimientoInventarioRepository movimientoRepo;
    private final EmailService emailService;

    @Value("${app.uploads.dir:uploads}")
    private String uploadsDir;

    // ── Ficha ────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public FichaResponse getFichaPorCita(Long citaId) {
        return fichaRepo.findByCitaId(citaId)
                .map(this::toResponse)
                .orElse(null);
    }

    @Transactional
    public FichaResponse guardarFicha(FichaRequest req) {
        Cita cita = citaRepo.findById(req.getCitaId())
                .orElseThrow(() -> new RuntimeException("Cita no encontrada"));

        FichaIngreso ficha = fichaRepo.findByCitaId(req.getCitaId())
                .orElseGet(() -> FichaIngreso.builder().cita(cita).build());

        ficha.setEstadoIngreso(req.getEstadoIngreso());
        ficha.setObservacionesGenerales(req.getObservacionesGenerales());
        ficha.setChecklistUnas(Boolean.TRUE.equals(req.getChecklistUnas()));
        ficha.setChecklistOidos(Boolean.TRUE.equals(req.getChecklistOidos()));
        ficha.setChecklistGlandulas(Boolean.TRUE.equals(req.getChecklistGlandulas()));
        ficha.setChecklistCorte(Boolean.TRUE.equals(req.getChecklistCorte()));
        ficha.setChecklistBano(Boolean.TRUE.equals(req.getChecklistBano()));
        ficha.setChecklistPerfume(Boolean.TRUE.equals(req.getChecklistPerfume()));

        FichaIngreso saved = fichaRepo.save(ficha);

        detalleRepo.deleteByFichaId(saved.getId());
        if (req.getDetalles() != null) {
            for (String desc : req.getDetalles()) {
                if (desc != null && !desc.isBlank()) {
                    detalleRepo.save(FichaDetalle.builder().ficha(saved).descripcionEstado(desc).build());
                }
            }
        }

        if (req.getRecomendacion() != null || req.getProximaCitaSugerida() != null) {
            RecomendacionPostServicio rec = recomendacionRepo.findByCitaId(req.getCitaId())
                    .orElseGet(() -> RecomendacionPostServicio.builder().cita(cita).build());
            rec.setRecomendacion(req.getRecomendacion());
            if (req.getProximaCitaSugerida() != null && !req.getProximaCitaSugerida().isBlank()) {
                rec.setProximaCitaSugerida(LocalDate.parse(req.getProximaCitaSugerida()));
            }
            recomendacionRepo.save(rec);
        }

        return toResponse(saved);
    }

    @Transactional
    public FichaResponse cerrarFicha(Long fichaId) {
        FichaIngreso ficha = fichaRepo.findById(fichaId)
                .orElseThrow(() -> new RuntimeException("Ficha no encontrada"));

        int completados = contarChecklist(ficha);
        if (completados < 6) {
            throw new RuntimeException("Debes completar los 6 ítems del checklist para cerrar la ficha (completados: " + completados + ")");
        }

        List<MovimientoInventario> insumos = movimientoRepo.findInsumosByCita(ficha.getCita().getId());
        for (MovimientoInventario m : insumos) {
            if ("ENTREGADO".equals(m.getEstado())) {
                m.setEstado("USADO");
                movimientoRepo.save(m);
            }
        }

        ficha.setEstado("CERRADA");
        fichaRepo.save(ficha);

        Cita cita = ficha.getCita();
        if ("ACEPTADO".equals(cita.getEstado())) {
            cita.setEstado("PENDIENTE_PAGO");
            citaRepo.save(cita);
        }

        try {
            Cliente cliente = cita.getCliente();
            String correo   = cliente != null && cliente.getUsuario() != null ? cliente.getUsuario().getCorreo() : null;
            String nombreCliente = cliente != null ? cliente.getNombre() : "Cliente";
            String mascota  = cita.getMascota() != null ? cita.getMascota().getNombre() : "Mascota";
            String servicio = cita.getServicio() != null ? cita.getServicio().getNombre() : "Grooming";
            Empleado groomer = cita.getEmpleadoAsignado();
            String nombreGroomer = groomer != null ? groomer.getNombre() : "Groomer";
            if (correo != null) {
                emailService.enviarListaParaRecoger(correo, nombreCliente, mascota, servicio, nombreGroomer);
            }
        } catch (Exception ignored) {}

        return toResponse(ficha);
    }

    // ── Fotos ────────────────────────────────────────────────

    @Transactional
    public FichaResponse.FotoResponse subirFoto(Long citaId, MultipartFile file, String momento) {
        Cita cita = citaRepo.findById(citaId)
                .orElseThrow(() -> new RuntimeException("Cita no encontrada"));
        String url = saveFile(file, "galeria");
        GaleriaMascota foto = GaleriaMascota.builder()
                .cita(cita).urlImage(url).momento(momento).build();
        foto = galeriaRepo.save(foto);
        return FichaResponse.FotoResponse.builder()
                .id(foto.getId()).url(foto.getUrlImage()).momento(foto.getMomento()).build();
    }

    @Transactional
    public void eliminarFoto(Long fotoId) {
        galeriaRepo.deleteById(fotoId);
    }

    // ── Insumos ──────────────────────────────────────────────

    @Transactional
    public InsumoGroomingResponse solicitarInsumo(InsumoGroomingRequest req) {
        Cita cita = citaRepo.findById(req.getCitaId())
                .orElseThrow(() -> new RuntimeException("Cita no encontrada"));
        Empleado groomer = (req.getEmpleadoId() != null && req.getEmpleadoId() > 0)
                ? empleadoRepo.findById(req.getEmpleadoId()).orElse(null)
                : cita.getEmpleadoAsignado();
        Producto producto = productoRepo.findById(req.getProductoId())
                .orElseThrow(() -> new RuntimeException("Producto no encontrado"));

        if (producto.getStockActual() < req.getCantidad()) {
            throw new RuntimeException("Stock insuficiente. Disponible: " + producto.getStockActual());
        }

        producto.setStockActual(producto.getStockActual() - req.getCantidad());
        productoRepo.save(producto);

        MovimientoInventario mov = MovimientoInventario.builder()
                .cita(cita)
                .empleado(groomer)
                .producto(producto)
                .cantidad(req.getCantidad())
                .tipoMovimiento("ENTREGA_GROOMER")
                .estado("SOLICITADO")
                .notas(req.getNotas())
                .build();
        movimientoRepo.save(mov);

        return toInsumoResponse(mov);
    }

    @Transactional
    public InsumoGroomingResponse entregarInsumo(InsumoGroomingRequest req) {
        Cita cita = citaRepo.findById(req.getCitaId())
                .orElseThrow(() -> new RuntimeException("Cita no encontrada"));
        Empleado groomer = (req.getEmpleadoId() != null && req.getEmpleadoId() > 0)
                ? empleadoRepo.findById(req.getEmpleadoId()).orElse(null)
                : cita.getEmpleadoAsignado();
        Producto producto = productoRepo.findById(req.getProductoId())
                .orElseThrow(() -> new RuntimeException("Producto no encontrado"));

        if (producto.getStockActual() < req.getCantidad()) {
            throw new RuntimeException("Stock insuficiente. Disponible: " + producto.getStockActual());
        }

        MovimientoInventario mov = MovimientoInventario.builder()
                .cita(cita)
                .empleado(groomer)
                .producto(producto)
                .cantidad(req.getCantidad())
                .tipoMovimiento("ENTREGA_GROOMER")
                .estado("ENTREGADO")
                .notas(req.getNotas())
                .build();
        movimientoRepo.save(mov);

        return toInsumoResponse(mov);
    }

    @Transactional(readOnly = true)
    public List<InsumoGroomingResponse> listarInsumosPorCita(Long citaId) {
        return movimientoRepo.findInsumosByCita(citaId)
                .stream().map(this::toInsumoResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<InsumoGroomingResponse> listarTodasEntregas() {
        return movimientoRepo.findAllEntregasGroomer()
                .stream().map(this::toInsumoResponse).toList();
    }

    @Transactional
    public InsumoGroomingResponse actualizarEstadoInsumo(Long movId, String nuevoEstado) {
        MovimientoInventario mov = movimientoRepo.findById(movId)
                .orElseThrow(() -> new RuntimeException("Movimiento no encontrado"));

        if ("RECHAZADO".equals(nuevoEstado) && "SOLICITADO".equals(mov.getEstado())) {
            Producto p = mov.getProducto();
            p.setStockActual(p.getStockActual() + mov.getCantidad());
            productoRepo.save(p);
        }

        if ("DEVUELTO".equals(nuevoEstado) && "ENTREGADO".equals(mov.getEstado())) {
            Producto p = mov.getProducto();
            p.setStockActual(p.getStockActual() + mov.getCantidad());
            productoRepo.save(p);
        }

        mov.setEstado(nuevoEstado);
        return toInsumoResponse(movimientoRepo.save(mov));
    }

    // ── Helpers ──────────────────────────────────────────────

    private int contarChecklist(FichaIngreso f) {
        int n = 0;
        if (Boolean.TRUE.equals(f.getChecklistUnas()))      n++;
        if (Boolean.TRUE.equals(f.getChecklistOidos()))     n++;
        if (Boolean.TRUE.equals(f.getChecklistGlandulas())) n++;
        if (Boolean.TRUE.equals(f.getChecklistCorte()))     n++;
        if (Boolean.TRUE.equals(f.getChecklistBano()))      n++;
        if (Boolean.TRUE.equals(f.getChecklistPerfume()))   n++;
        return n;
    }

    private FichaResponse toResponse(FichaIngreso f) {
        List<String> detalles = detalleRepo.findByFichaId(f.getId())
                .stream().map(FichaDetalle::getDescripcionEstado).toList();
        List<FichaResponse.FotoResponse> fotos = galeriaRepo.findByCitaIdOrderByFechaCarga(f.getCita().getId())
                .stream().map(g -> FichaResponse.FotoResponse.builder()
                        .id(g.getId()).url(g.getUrlImage()).momento(g.getMomento()).build())
                .toList();
        RecomendacionPostServicio rec = recomendacionRepo.findByCitaId(f.getCita().getId()).orElse(null);
        Cita cita = f.getCita();

        return FichaResponse.builder()
                .id(f.getId())
                .citaId(cita.getId())
                .mascotaNombre(cita.getMascota() != null ? cita.getMascota().getNombre() : null)
                .clienteNombre(cita.getCliente() != null ? cita.getCliente().getNombre() : null)
                .servicioNombre(cita.getServicio() != null ? cita.getServicio().getNombre() : null)
                .fechaCita(cita.getFechaHoraInicio() != null
                        ? cita.getFechaHoraInicio().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")) : null)
                .estadoIngreso(f.getEstadoIngreso())
                .observacionesGenerales(f.getObservacionesGenerales())
                .detalles(detalles)
                .checklistUnas(f.getChecklistUnas())
                .checklistOidos(f.getChecklistOidos())
                .checklistGlandulas(f.getChecklistGlandulas())
                .checklistCorte(f.getChecklistCorte())
                .checklistBano(f.getChecklistBano())
                .checklistPerfume(f.getChecklistPerfume())
                .checklistCompletados(contarChecklist(f))
                .estado(f.getEstado())
                .creadoEn(f.getCreadoEn())
                .recomendacion(rec != null ? rec.getRecomendacion() : null)
                .proximaCitaSugerida(rec != null && rec.getProximaCitaSugerida() != null
                        ? rec.getProximaCitaSugerida().toString() : null)
                .fotos(fotos)
                .build();
    }

    private InsumoGroomingResponse toInsumoResponse(MovimientoInventario m) {
        return InsumoGroomingResponse.builder()
                .id(m.getId())
                .citaId(m.getCita() != null ? m.getCita().getId() : null)
                .mascotaNombre(m.getCita() != null && m.getCita().getMascota() != null
                        ? m.getCita().getMascota().getNombre() : null)
                .servicioNombre(m.getCita() != null && m.getCita().getServicio() != null
                        ? m.getCita().getServicio().getNombre() : null)
                .empleadoId(m.getEmpleado() != null ? m.getEmpleado().getId() : null)
                .empleadoNombre(m.getEmpleado() != null ? m.getEmpleado().getNombre() : null)
                .productoId(m.getProducto().getId())
                .productoNombre(m.getProducto().getNombre())
                .cantidad(m.getCantidad())
                .estado(m.getEstado())
                .notas(m.getNotas())
                .fecha(m.getFecha())
                .build();
    }

    private String saveFile(MultipartFile file, String subdir) {
        String original = file.getOriginalFilename();
        String baseName = (original != null) ? Paths.get(original).getFileName().toString() : "file";
        String filename = UUID.randomUUID() + "_" + baseName;
        var dir = Paths.get(uploadsDir, subdir).toAbsolutePath();
        try {
            Files.createDirectories(dir);
            Files.copy(file.getInputStream(), dir.resolve(filename));
        } catch (IOException e) {
            throw new RuntimeException("Error al guardar imagen: " + e.getMessage());
        }
        return "/api/files/" + subdir + "/" + filename;
    }
}
