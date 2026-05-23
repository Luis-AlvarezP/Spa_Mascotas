package com.spamascotas.spa_mascotas_api.repository;

import com.spamascotas.spa_mascotas_api.model.Cita;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface CitaRepository extends JpaRepository<Cita, Long> {

    List<Cita> findByClienteIdOrderByFechaHoraInicioDesc(Long clienteId);

    @Query("SELECT c FROM Cita c WHERE c.estado NOT IN ('CANCELADO','REALIZADO') " +
           "AND c.fechaHoraInicio < :fin AND c.fechaHoraFin > :inicio")
    List<Cita> findSolapadas(@Param("inicio") LocalDateTime inicio,
                             @Param("fin")   LocalDateTime fin);

    @Query("SELECT c FROM Cita c WHERE c.estado NOT IN ('CANCELADO','REALIZADO') " +
           "AND c.fechaHoraInicio >= :desde AND c.fechaHoraInicio < :hasta")
    List<Cita> findActivasEnDia(@Param("desde") LocalDateTime desde,
                                @Param("hasta") LocalDateTime hasta);

    @Query("SELECT c FROM Cita c WHERE c.estado NOT IN ('CANCELADO','REALIZADO') " +
           "AND c.fechaHoraInicio >= :desde AND c.fechaHoraInicio < :hasta " +
           "AND (c.empleadoPreferido IS NOT NULL AND c.empleadoPreferido.id = :empleadoId " +
           "  OR c.empleadoAsignado  IS NOT NULL AND c.empleadoAsignado.id  = :empleadoId)")
    List<Cita> findActivasEnDiaByEmpleado(@Param("desde")      LocalDateTime desde,
                                          @Param("hasta")      LocalDateTime hasta,
                                          @Param("empleadoId") Long empleadoId);

    @Query("SELECT c FROM Cita c WHERE c.estado NOT IN ('CANCELADO') " +
           "AND c.fechaHoraInicio >= :desde AND c.fechaHoraInicio < :hasta " +
           "AND (c.empleadoPreferido IS NOT NULL AND c.empleadoPreferido.id = :empleadoId " +
           "  OR c.empleadoAsignado  IS NOT NULL AND c.empleadoAsignado.id  = :empleadoId)")
    List<Cita> findAceptadasEnDiaByEmpleado(@Param("desde")      LocalDateTime desde,
                                             @Param("hasta")      LocalDateTime hasta,
                                             @Param("empleadoId") Long empleadoId);

    @Query("SELECT c FROM Cita c WHERE c.estado NOT IN ('CANCELADO') ORDER BY c.fechaHoraInicio DESC")
    List<Cita> findAllActivasOrderByFecha();

    @Query("SELECT c FROM Cita c WHERE c.estado IN ('ACEPTADO','PENDIENTE_PAGO') " +
           "AND (c.empleadoAsignado IS NOT NULL AND c.empleadoAsignado.id = :empleadoId " +
           "  OR c.empleadoPreferido IS NOT NULL AND c.empleadoPreferido.id = :empleadoId) " +
           "ORDER BY c.fechaHoraInicio")
    List<Cita> findServiciosPendientesByEmpleado(@Param("empleadoId") Long empleadoId);

    @Query("SELECT c FROM Cita c WHERE c.estado = 'ACEPTADO' " +
           "AND c.fechaHoraInicio >= :desde AND c.fechaHoraInicio < :hasta " +
           "AND c.recordatorio24hEnviado = false")
    List<Cita> findPendientes24h(@Param("desde") LocalDateTime desde,
                                 @Param("hasta") LocalDateTime hasta);

    @Query("SELECT c FROM Cita c WHERE c.estado = 'ACEPTADO' " +
           "AND c.fechaHoraInicio >= :desde AND c.fechaHoraInicio < :hasta " +
           "AND c.recordatorio2hEnviado = false")
    List<Cita> findPendientes2h(@Param("desde") LocalDateTime desde,
                                @Param("hasta") LocalDateTime hasta);

    @Query("SELECT c FROM Cita c WHERE c.cliente.id = :clienteId " +
           "AND c.estado IN ('EN_REVISION','ACEPTADO') AND c.id <> :excluirId " +
           "ORDER BY c.fechaHoraInicio ASC")
    List<Cita> findProximaActivaByCliente(@Param("clienteId") Long clienteId,
                                          @Param("excluirId") Long excluirId);

    @Query("SELECT c FROM Cita c WHERE c.estado = :estado " +
           "AND (c.empleadoAsignado IS NOT NULL AND c.empleadoAsignado.id = :empleadoId " +
           "  OR c.empleadoPreferido IS NOT NULL AND c.empleadoPreferido.id = :empleadoId) " +
           "ORDER BY c.fechaHoraInicio")
    List<Cita> findByEmpleadoIdAndEstado(@Param("empleadoId") Long empleadoId,
                                         @Param("estado") String estado);

    @Query("SELECT c FROM Cita c WHERE c.estado = 'ACEPTADO' ORDER BY c.fechaHoraInicio")
    List<Cita> findAllAceptadas();

    List<Cita> findByMascotaIdAndEstadoOrderByFechaHoraInicioDesc(Long mascotaId, String estado);

    @Query("SELECT c FROM Cita c WHERE c.estado = 'REALIZADO' AND c.empleadoAsignado.id = :empleadoId ORDER BY c.fechaHoraInicio DESC")
    List<Cita> findRealizadasByGroomer(@Param("empleadoId") Long empleadoId);

    @Query("SELECT c FROM Cita c WHERE c.estado = 'REALIZADO' " +
           "AND c.empleadoAsignado.id = :empleadoId " +
           "AND c.fechaHoraInicio >= :inicio AND c.fechaHoraInicio < :fin " +
           "ORDER BY c.fechaHoraInicio")
    List<Cita> findRealizadasByGroomerEntre(@Param("empleadoId") Long empleadoId,
                                             @Param("inicio") LocalDateTime inicio,
                                             @Param("fin") LocalDateTime fin);
}
