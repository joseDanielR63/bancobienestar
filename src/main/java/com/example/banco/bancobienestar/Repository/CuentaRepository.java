package com.example.banco.bancobienestar.Repository;

import com.example.banco.bancobienestar.model.CuentaEntity;
import com.example.banco.bancobienestar.model.UsuarioEntity;
import org.springframework.data.jpa.repository.JpaRepository;
// @Repository no es necesaria, Spring Data JPA la detecta automáticamente

import java.util.List;
import java.util.Optional;

public interface CuentaRepository extends JpaRepository<CuentaEntity, Long> {

    // Busca una cuenta por su CLABE (número de cuenta único)
    Optional<CuentaEntity> findByClabe(String clabe);

    // Busca todas las cuentas asociadas a un usuario
    List<CuentaEntity> findByUsuario(UsuarioEntity usuario);

    // Verifica si ya existe una cuenta con esa CLABE
    boolean existsByClabe(String clabe);

    // Busca cuentas de un usuario ordenadas por saldo descendente (mayor a menor)
    List<CuentaEntity> findByUsuarioOrderBySaldoDesc(UsuarioEntity usuario);
}
