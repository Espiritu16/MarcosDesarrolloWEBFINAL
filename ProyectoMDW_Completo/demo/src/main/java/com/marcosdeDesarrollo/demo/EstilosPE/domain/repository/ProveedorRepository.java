package com.marcosdeDesarrollo.demo.EstilosPE.domain.repository;

import com.marcosdeDesarrollo.demo.EstilosPE.persistence.entity.Proveedores;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProveedorRepository extends JpaRepository<Proveedores, Integer> {
    Optional<Proveedores> findByNombreProveedorIgnoreCase(String nombreProveedor);

    boolean existsByNombreProveedorIgnoreCase(String nombreProveedor);
}
