package com.example.banco.bancobienestar.Repository;


import com.example.banco.bancobienestar.model.UsuarioEntity;
import org.springframework.data.jpa.repository.JpaRepository;
// @Repository no es necesaria, Spring Data JPA la detecta automáticamente
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<UsuarioEntity, Long> {
    
@Query("SELECT u FROM UsuarioEntity u LEFT JOIN FETCH u.cuentas WHERE u.username = :username")
Optional<UsuarioEntity> findByUsernameWithCuentas(@Param("username") String username);


    Optional<UsuarioEntity> findByUsername(String username);
}