package com.example.banco.bancobienestar.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.springframework.context.annotation.Lazy;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.banco.bancobienestar.Repository.CuentaRepository;
import com.example.banco.bancobienestar.Repository.MovimientoCuentaRepository;
import com.example.banco.bancobienestar.Repository.SolicitudCreditoRepository;
import com.example.banco.bancobienestar.Repository.UsuarioRepository;
import com.example.banco.bancobienestar.model.CuentaEntity;
import com.example.banco.bancobienestar.model.MovimientoEntity;
import com.example.banco.bancobienestar.model.SolicitudCreditoEntity;
import com.example.banco.bancobienestar.model.UsuarioEntity;

@Service
public class BancaService {

    private final UsuarioRepository usuarioRepository;
    private final CuentaRepository cuentaRepository;
    private final MovimientoCuentaRepository movimientoRepository;
    private final SolicitudCreditoRepository solicitudCreditoRepository;
    private final PasswordEncoder passwordEncoder;

    public BancaService(UsuarioRepository usuarioRepository,
                        CuentaRepository cuentaRepository,
                        MovimientoCuentaRepository movimientoRepository,
                        SolicitudCreditoRepository solicitudCreditoRepository,
                        @Lazy PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.cuentaRepository = cuentaRepository;
        this.movimientoRepository = movimientoRepository;
        this.solicitudCreditoRepository = solicitudCreditoRepository;
        this.passwordEncoder = passwordEncoder;
    }

    // ============================================================
    // 1. TRANSFERENCIA ENTRE CLABES (ACID)
    // ============================================================
    @Transactional(rollbackFor = Exception.class)
    public void transferirMonto(String clabeOrigen, String clabeDestino, Double monto, String descripcion) {
        if (monto <= 0) {
            throw new IllegalArgumentException("El monto debe ser mayor a cero.");
        }
        if (clabeOrigen.equals(clabeDestino)) {
            throw new IllegalArgumentException("La cuenta de destino no puede ser la misma que la de origen.");
        }

        CuentaEntity origen = cuentaRepository.findByClabe(clabeOrigen)
                .orElseThrow(() -> new RuntimeException("La cuenta de origen no existe."));

        CuentaEntity destino = cuentaRepository.findByClabe(clabeDestino)
                .orElseThrow(() -> new RuntimeException("La cuenta de destino no existe."));

        if (origen.getSaldo() < monto) {
            throw new FondosinsuficientesException("No cuentas con saldo suficiente para esta operación.");
        }

        origen.setSaldo(origen.getSaldo() - monto);
        cuentaRepository.save(origen);

        destino.setSaldo(destino.getSaldo() + monto);
        cuentaRepository.save(destino);

        MovimientoEntity movimiento = new MovimientoEntity();
        movimiento.setCuentaOrigen(clabeOrigen);
        movimiento.setCuentaDestino(clabeDestino);
        movimiento.setMonto(monto);
        movimiento.setDescripcion(descripcion);
        movimiento.setFecha(LocalDateTime.now());
        movimiento.setTipo("TRANSFERENCIA");
        movimiento.setEstadoMovimiento("autorizado");
        movimientoRepository.save(movimiento);
    }

    // ============================================================
    // 2. TRANSFERENCIA DESDE USUARIO AUTENTICADO
    // ============================================================
    @Transactional(rollbackFor = Exception.class)
    public void transferirDesdeUsuario(String username, String clabeDestino, Double monto, String descripcion) {
        UsuarioEntity usuario = usuarioRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado."));

        if (usuario.getCuentas() == null || usuario.getCuentas().isEmpty()) {
            throw new RuntimeException("El usuario no tiene una cuenta bancaria asignada.");
        }

        String clabeOrigen = usuario.getCuentas().get(0).getClabe();
        transferirMonto(clabeOrigen, clabeDestino, monto, descripcion);
    }

    // ============================================================
    // 3. CREAR CLIENTE CON CUENTA (CLABE ÚNICA)
    // ============================================================
    @Transactional(rollbackFor = Exception.class)
    public UsuarioEntity crearClienteConCuenta(String username, String password, Double saldoInicial) {
        if (usuarioRepository.findByUsername(username).isPresent()) {
            throw new RuntimeException("El nombre de usuario ya está registrado.");
        }

        UsuarioEntity usuario = new UsuarioEntity();
        usuario.setUsername(username);
        usuario.setPassword(passwordEncoder.encode(password));
        usuario.setRol("CLIENTE");
        usuario.setNombre(username);
        UsuarioEntity usuarioGuardado = usuarioRepository.save(usuario);

        String clabe = generarClabeUnica();

        CuentaEntity cuenta = new CuentaEntity();
        cuenta.setClabe(clabe);
        cuenta.setSaldo(saldoInicial);
        cuenta.setUsuario(usuarioGuardado);
        cuentaRepository.save(cuenta);

        List<CuentaEntity> list = new ArrayList<>();
        list.add(cuenta);
        usuarioGuardado.setCuentas(list);

        return usuarioGuardado;
    }

    // ============================================================
    // 4. SOLICITUD DE CRÉDITO (APROBACIÓN CON CAMBIO DE ESTADO)
    // ============================================================
    
    @Transactional(rollbackFor = Exception.class)
    public SolicitudCreditoEntity guardarSolicitudCredito(String username, Double monto, String firmaBase64) {
        UsuarioEntity usuario = usuarioRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado."));

        SolicitudCreditoEntity solicitud = new SolicitudCreditoEntity();
        solicitud.setUsuario(usuario);
        solicitud.setMontoSolicitado(monto);
        solicitud.setFirmaBase64(firmaBase64);
        solicitud.setEstado("PENDIENTE");
        solicitud.setFecha(LocalDateTime.now());

        return solicitudCreditoRepository.save(solicitud);
    }

    @Transactional(rollbackFor = Exception.class)
    public SolicitudCreditoEntity cambiarEstadoCredito(Long solicitudId, String nuevoEstado) {
        SolicitudCreditoEntity solicitud = solicitudCreditoRepository.findById(solicitudId)
                .orElseThrow(() -> new RuntimeException("Solicitud no encontrada"));

        if ("APROBADO".equals(nuevoEstado) && !"APROBADO".equals(solicitud.getEstado())) {
            UsuarioEntity usuario = solicitud.getUsuario();
            if (usuario.getCuentas() != null && !usuario.getCuentas().isEmpty()) {
                CuentaEntity cuenta = usuario.getCuentas().get(0);
                cuenta.setSaldo(cuenta.getSaldo() + solicitud.getMontoSolicitado());
                cuentaRepository.save(cuenta);

                MovimientoEntity movimiento = new MovimientoEntity();
                movimiento.setCuentaOrigen("CRÉDITO-BANCO");
                movimiento.setCuentaDestino(cuenta.getClabe());
                movimiento.setMonto(solicitud.getMontoSolicitado());
                movimiento.setDescripcion("Abono de Crédito Aprobado");
                movimiento.setFecha(LocalDateTime.now());
                movimiento.setTipo("CREDITO");
                movimiento.setEstadoMovimiento("autorizado");
                movimientoRepository.save(movimiento);
            }
        }

        solicitud.setEstado(nuevoEstado);
        return solicitudCreditoRepository.save(solicitud);
    }

    public List<SolicitudCreditoEntity> obtenerSolicitudesPendientes() {
        return solicitudCreditoRepository.findByEstadoOrderByFechaDesc("PENDIENTE");
    }

    // ============================================================
    // 5. GENERAR CLABE ÚNICA (18 dígitos)
    // ============================================================
    private String generarClabeUnica() {
        Random random = new Random();
        String clabe;
        do {
            StringBuilder sb = new StringBuilder("012");
            for (int i = 0; i < 15; i++) {
                sb.append(random.nextInt(10));
            }
            clabe = sb.toString();
        } while (cuentaRepository.findByClabe(clabe).isPresent());
        return clabe;
    }

       // ============================================================
    // 6. MÉTODOS PARA MOVIMIENTOS (ADMIN) - CORREGIDO
    // ============================================================

    public List<MovimientoEntity> todosMovimientos() {
        return movimientoRepository.findAll();
    }

    public MovimientoEntity obtenerMovimientoId(Long id) {
        return movimientoRepository.findById(id).orElse(null);
    }

    /**
     * Obtiene movimientos donde la cuenta origen O destino esté en la lista de CLABES.
     * CORREGIDO: se pasa la misma lista para origen y destino.
     */
   public List<MovimientoEntity> obtenerMovimientosPorClabes(List<String> clabes) {
    return movimientoRepository.findByCuentaOrigenInOrCuentaDestinoInOrderByFechaDesc(clabes);
}
         /**
 * Cambia el estado de un movimiento entre: pendiente, autorizado, rechazado.
 * - autorizado → rechazado: REVIERTE EFECTO (origen recupera, destino devuelve)
 * - rechazado → autorizado: APLICA EFECTO (cargo/abono)
 * - Cualquier otro caso: SOLO cambia estado (sin afectar cuentas)
 */
@Transactional
public void actualizarMovimiento(Long id, String nuevoEstado) {
    MovimientoEntity movimiento = obtenerMovimientoId(id);
    if (movimiento == null) {
        throw new RuntimeException("Movimiento no encontrado");
    }

    String estadoActual = movimiento.getEstadoMovimiento();
    String tipo = movimiento.getTipo();
    Double monto = movimiento.getMonto();
    String cuentaOrigen = movimiento.getCuentaOrigen();
    String cuentaDestino = movimiento.getCuentaDestino();

    // Normalizar estados (ignorar mayúsculas/minúsculas)
    String actual = estadoActual.toLowerCase();
    String nuevo = nuevoEstado.toLowerCase();

    // ============================================================
    // 1. autorizado → rechazado: REVERTIR EFECTO
    // ============================================================
    if ("autorizado".equals(actual) && "rechazado".equals(nuevo)) {
        revertirEfectoMovimiento(tipo, monto, cuentaOrigen, cuentaDestino);
        movimiento.setEstadoMovimiento(nuevoEstado);
        movimientoRepository.save(movimiento);
        return;
    }

    // ============================================================
    // 2. rechazado → autorizado: APLICAR EFECTO
    // ============================================================
    if ("rechazado".equals(actual) && "autorizado".equals(nuevo)) {
        aplicarEfectoMovimiento(tipo, monto, cuentaOrigen, cuentaDestino);
        movimiento.setEstadoMovimiento(nuevoEstado);
        movimientoRepository.save(movimiento);
        return;
    }

    // ============================================================
    // 3. Cualquier otro caso (pendiente → autorizado/rechazado, etc.)
    //    SOLO cambia estado, sin afectar cuentas
    // ============================================================
    movimiento.setEstadoMovimiento(nuevoEstado);
    movimientoRepository.save(movimiento);
}

    /**
     * Aplica el efecto de un movimiento en las cuentas (cargo/abono).
     */
    private void aplicarEfectoMovimiento(String tipo, Double monto, String cuentaOrigen, String cuentaDestino) {
        if (monto == null) return;

        if ("TRANSFERENCIA".equalsIgnoreCase(tipo)) {
            CuentaEntity origen = cuentaRepository.findByClabe(cuentaOrigen)
                    .orElseThrow(() -> new RuntimeException("Cuenta origen no encontrada"));
            CuentaEntity destino = cuentaRepository.findByClabe(cuentaDestino)
                    .orElseThrow(() -> new RuntimeException("Cuenta destino no encontrada"));

            origen.setSaldo(origen.getSaldo() - monto);
            destino.setSaldo(destino.getSaldo() + monto);
            cuentaRepository.save(origen);
            cuentaRepository.save(destino);

        } else if ("CREDITO".equalsIgnoreCase(tipo)) {
            CuentaEntity destino = cuentaRepository.findByClabe(cuentaDestino)
                    .orElseThrow(() -> new RuntimeException("Cuenta destino no encontrada"));
            destino.setSaldo(destino.getSaldo() + monto);
            cuentaRepository.save(destino);
        }
    }

    /**
     * Revierte el efecto de un movimiento en las cuentas.
     * Origen: recupera el monto (+), Destino: devuelve el monto (-).
     */
    private void revertirEfectoMovimiento(String tipo, Double monto, String cuentaOrigen, String cuentaDestino) {
        if (monto == null) return;

        if ("TRANSFERENCIA".equalsIgnoreCase(tipo)) {
            CuentaEntity origen = cuentaRepository.findByClabe(cuentaOrigen)
                    .orElseThrow(() -> new RuntimeException("Cuenta origen no encontrada"));
            CuentaEntity destino = cuentaRepository.findByClabe(cuentaDestino)
                    .orElseThrow(() -> new RuntimeException("Cuenta destino no encontrada"));

            origen.setSaldo(origen.getSaldo() + monto);
            destino.setSaldo(destino.getSaldo() - monto);
            cuentaRepository.save(origen);
            cuentaRepository.save(destino);

        } else if ("CREDITO".equalsIgnoreCase(tipo)) {
            CuentaEntity destino = cuentaRepository.findByClabe(cuentaDestino)
                    .orElseThrow(() -> new RuntimeException("Cuenta destino no encontrada"));
            destino.setSaldo(destino.getSaldo() - monto);
            cuentaRepository.save(destino);
        }
    }

    @Transactional
    public void eliminarMovimiento(Long id) {
        movimientoRepository.deleteById(id);
    }
     }