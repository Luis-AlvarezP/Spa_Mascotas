package com.spamascotas.spa_mascotas_api.repository;

import com.spamascotas.spa_mascotas_api.model.CatTemperamento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CatTemperamentoRepository extends JpaRepository<CatTemperamento, Long> {
    Optional<CatTemperamento> findByNombre(String nombre);
    List<CatTemperamento> findAllByOrderByNombreAsc();
}
