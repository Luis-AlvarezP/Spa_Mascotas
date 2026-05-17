package com.spamascotas.spa_mascotas_api.repository;

import com.spamascotas.spa_mascotas_api.model.BloqueoAgenda;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BloqueoAgendaRepository extends JpaRepository<BloqueoAgenda, Long> {
    List<BloqueoAgenda> findAllByOrderByFechaInicioDesc();
}
