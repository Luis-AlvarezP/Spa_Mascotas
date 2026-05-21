package com.spamascotas.spa_mascotas_api.service.citas;

import com.spamascotas.spa_mascotas_api.model.Cita;
import com.spamascotas.spa_mascotas_api.repository.CitaRepository;
import com.spamascotas.spa_mascotas_api.service.auth.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class CitaReminderSchedulerService {

    private final CitaRepository citaRepo;
    private final EmailService   emailService;

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @Scheduled(fixedRate = 1_800_000)
    @Transactional
    public void enviarRecordatorios() {
        LocalDateTime ahora = LocalDateTime.now();

        List<Cita> pendientes24 = citaRepo.findPendientes24h(
                ahora.plusHours(23), ahora.plusHours(25));
        for (Cita c : pendientes24) {
            try {
                emailService.enviarRecordatorio(
                        c.getCliente().getUsuario().getCorreo(),
                        c.getCliente().getNombre(),
                        c.getServicio().getNombre(),
                        c.getFechaHoraInicio().format(FMT), 24);
                c.setRecordatorio24hEnviado(true);
                citaRepo.save(c);
                log.info("Recordatorio 24h enviado — cita #{}", c.getId());
            } catch (Exception e) {
                log.warn("Error recordatorio 24h cita #{}: {}", c.getId(), e.getMessage());
            }
        }

        List<Cita> pendientes2 = citaRepo.findPendientes2h(
                ahora.plusMinutes(110), ahora.plusMinutes(130));
        for (Cita c : pendientes2) {
            try {
                emailService.enviarRecordatorio(
                        c.getCliente().getUsuario().getCorreo(),
                        c.getCliente().getNombre(),
                        c.getServicio().getNombre(),
                        c.getFechaHoraInicio().format(FMT), 2);
                c.setRecordatorio2hEnviado(true);
                citaRepo.save(c);
                log.info("Recordatorio 2h enviado — cita #{}", c.getId());
            } catch (Exception e) {
                log.warn("Error recordatorio 2h cita #{}: {}", c.getId(), e.getMessage());
            }
        }
    }
}
