package com.spamascotas.spa_mascotas_api.repository;

import com.spamascotas.spa_mascotas_api.model.Cliente;
import com.spamascotas.spa_mascotas_api.model.ClientePreferencia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface ClientePreferenciaRepository extends JpaRepository<ClientePreferencia, Long> {
    List<ClientePreferencia> findByCliente(Cliente cliente);
    Optional<ClientePreferencia> findByClienteAndNombre(Cliente cliente, String nombre);
}
