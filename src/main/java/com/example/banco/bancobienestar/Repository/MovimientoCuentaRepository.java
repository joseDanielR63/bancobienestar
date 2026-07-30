package com.example.banco.bancobienestar.Repository;

import com.example.banco.bancobienestar.model.MovimientoEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface MovimientoCuentaRepository extends JpaRepository<MovimientoEntity, Long> {

    // 1. Busca movimientos por cuentaOrigen O cuentaDestino (sin límite)
    List<MovimientoEntity> findByCuentaOrigenOrCuentaDestinoOrderByFechaDesc(
            String cuentaOrigen,
            String cuentaDestino
    );

    // 2. Busca los 10 movimientos más recientes por cuentaOrigen O cuentaDestino
    @Query("SELECT m FROM MovimientoEntity m " +
           "WHERE m.cuentaOrigen = :origen OR m.cuentaDestino = :destino " +
           "ORDER BY m.fecha DESC")
    List<MovimientoEntity> findTop10ByCuenta(
            @Param("origen") String origen,
            @Param("destino") String destino
    );

    // 3. Busca gastos (monto < 0) de una lista de CLABES (origen o destino)
    @Query("SELECT m FROM MovimientoEntity m " +
           "WHERE (m.cuentaOrigen IN :clabes OR m.cuentaDestino IN :clabes) " +
           "AND m.monto < 0")
    List<MovimientoEntity> findGastosByClabes(@Param("clabes") List<String> clabes);

    // 4. Busca movimientos por CLABES
    @Query("SELECT m FROM MovimientoEntity m WHERE m.cuentaOrigen IN :clabes OR m.cuentaDestino IN :clabes ORDER BY m.fecha DESC")
    List<MovimientoEntity> findByCuentaOrigenInOrCuentaDestinoInOrderByFechaDesc(@Param("clabes") List<String> clabes);

    // 5. Todos los movimientos ordenados por fecha (para el admin)
    @Query("SELECT m FROM MovimientoEntity m ORDER BY m.fecha DESC")
    List<MovimientoEntity> findAllByOrderByFechaDesc();
    
    // 6. Buscar movimientos por CLABE de cuenta (para el admin)
    @Query("SELECT m FROM MovimientoEntity m WHERE m.cuentaOrigen = :clabe OR m.cuentaDestino = :clabe ORDER BY m.fecha DESC")
    List<MovimientoEntity> findByClabe(@Param("clabe") String clabe);
}