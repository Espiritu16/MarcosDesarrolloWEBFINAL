package com.marcosdeDesarrollo.demo.EstilosPE.domain.repository;

import com.marcosdeDesarrollo.demo.EstilosPE.persistence.entity.EstadoOrdenCompra;
import com.marcosdeDesarrollo.demo.EstilosPE.persistence.entity.Ordenes_Compra;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OrdenCompraRepository extends JpaRepository<Ordenes_Compra, Integer> {
    List<Ordenes_Compra> findByEstado(EstadoOrdenCompra estado);
}
