package com.marcosdeDesarrollo.demo.EstilosPE.domain.repository;

import com.marcosdeDesarrollo.demo.EstilosPE.persistence.entity.Estado;
import com.marcosdeDesarrollo.demo.EstilosPE.persistence.entity.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.List;

public interface UsuarioRepository extends JpaRepository<Usuario, Integer> {

    Optional<Usuario> findByEmail(String email);

    Boolean existsByEmail(String email);

    long countByRol_Id(Integer idRol);

    List<Usuario> findByRol_Id(Integer idRol);

    long countByRol_IdAndEstado(Integer idRol, Estado estado);
    boolean existsByEmailAndIdNot(String email, Integer id);
}
