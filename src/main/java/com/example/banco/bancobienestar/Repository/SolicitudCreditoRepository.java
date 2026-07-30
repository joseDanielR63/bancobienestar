package com.example.banco.bancobienestar.Repository;

import com.example.banco.bancobienestar.model.SolicitudCreditoEntity;
import com.example.banco.bancobienestar.model.UsuarioEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository  // ✅ Opcional pero recomendado para claridad (Spring lo detecta automáticamente)
public interface SolicitudCreditoRepository extends JpaRepository<SolicitudCreditoEntity, Long> {

    // Busca solicitudes por usuario específico, ordenadas por fecha descendente (más reciente primero)
    List<SolicitudCreditoEntity> findByUsuarioOrderByFechaDesc(UsuarioEntity usuario);

    // Busca todas las solicitudes ordenadas por fecha descendente (más reciente primero)
    List<SolicitudCreditoEntity> findAllByOrderByFechaDesc();

    // Busca solicitudes por estado (ej. "PENDIENTE", "APROBADO", "RECHAZADO"), ordenadas por fecha descendente
    List<SolicitudCreditoEntity> findByEstadoOrderByFechaDesc(String estado);
}