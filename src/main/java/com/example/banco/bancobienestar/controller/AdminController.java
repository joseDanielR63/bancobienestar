package com.example.banco.bancobienestar.controller;

import com.example.banco.bancobienestar.Repository.CuentaRepository;
import com.example.banco.bancobienestar.Repository.SolicitudCreditoRepository;
import com.example.banco.bancobienestar.Repository.UsuarioRepository;
import com.example.banco.bancobienestar.Repository.MovimientoCuentaRepository;
import com.example.banco.bancobienestar.model.CuentaEntity;
import com.example.banco.bancobienestar.model.SolicitudCreditoEntity;
import com.example.banco.bancobienestar.model.UsuarioEntity;
import com.example.banco.bancobienestar.model.MovimientoEntity;
import com.example.banco.bancobienestar.service.BancaService;
import com.example.banco.bancobienestar.service.PDFService;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/admin")
public class AdminController {

    private final BancaService bancaService;
    private final UsuarioRepository usuarioRepository;
    private final SolicitudCreditoRepository solicitudCreditoRepository;
    private final CuentaRepository cuentaRepository;
    private final PasswordEncoder passwordEncoder;
    private final MovimientoCuentaRepository movimientoRepository;
    private final PDFService pdfService;

    public AdminController(BancaService bancaService,
                           UsuarioRepository usuarioRepository,
                           SolicitudCreditoRepository solicitudCreditoRepository,
                           CuentaRepository cuentaRepository,
                           PasswordEncoder passwordEncoder,
                           MovimientoCuentaRepository movimientoRepository,
                           PDFService pdfService) {
        this.bancaService = bancaService;
        this.usuarioRepository = usuarioRepository;
        this.solicitudCreditoRepository = solicitudCreditoRepository;
        this.cuentaRepository = cuentaRepository;
        this.passwordEncoder = passwordEncoder;
        this.movimientoRepository = movimientoRepository;
        this.pdfService = pdfService;
    }

    // Redirige a la sección de clientes por defecto
    @GetMapping
    public String redirigirClientes() {
        return "redirect:/admin/clientes";
    }

    // ============================================================
    // SECCIÓN CLIENTES
    // ============================================================
    @GetMapping("/clientes")
    public String listarClientes(Model model) {
        List<UsuarioEntity> clientes = usuarioRepository.findAll().stream()
                .filter(u -> "CLIENTE".equals(u.getRol()))
                .collect(Collectors.toList());
        model.addAttribute("clientes", clientes);
        model.addAttribute("seccion", "clientes");
        
        // Contadores para el menú y tarjetas
        model.addAttribute("totalClientes", clientes.size());
        model.addAttribute("solicitudesPendientes", 
            solicitudCreditoRepository.findByEstadoOrderByFechaDesc("PENDIENTE").size());
        model.addAttribute("solicitudesAprobadas", 
            solicitudCreditoRepository.findByEstadoOrderByFechaDesc("APROBADO").size());
        model.addAttribute("solicitudesRechazadas", 
            solicitudCreditoRepository.findByEstadoOrderByFechaDesc("RECHAZADO").size());
        
        return "admin";
    }

    @PostMapping("/clientes/eliminar/{id}")
    public String eliminarCliente(@PathVariable Long id, RedirectAttributes redirect) {
        try {
            UsuarioEntity usuario = usuarioRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Cliente no encontrado"));
            List<CuentaEntity> cuentas = usuario.getCuentas();
            if (cuentas != null) {
                cuentaRepository.deleteAll(cuentas);
            }
            usuarioRepository.delete(usuario);
            redirect.addFlashAttribute("exito", "Cliente eliminado correctamente.");
        } catch (Exception e) {
            redirect.addFlashAttribute("error", "Error al eliminar cliente: " + e.getMessage());
        }
        return "redirect:/admin/clientes";
    }

    @PostMapping("/clientes/actualizar")
    public String actualizarCliente(@RequestParam Long id,
                                    @RequestParam String nombre,
                                    @RequestParam(required = false) String password,
                                    RedirectAttributes redirect) {
        try {
            UsuarioEntity usuario = usuarioRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Cliente no encontrado"));
            usuario.setNombre(nombre);
            if (password != null && !password.trim().isEmpty()) {
                usuario.setPassword(passwordEncoder.encode(password));
            }
            usuarioRepository.save(usuario);
            redirect.addFlashAttribute("exito", "Cliente actualizado correctamente.");
        } catch (Exception e) {
            redirect.addFlashAttribute("error", "Error al actualizar: " + e.getMessage());
        }
        return "redirect:/admin/clientes";
    }

    // ============================================================
    // SECCIÓN CRÉDITOS
    // ============================================================
    @GetMapping("/creditos")
    public String listarCreditos(@RequestParam(required = false) String estado,
                                 Model model) {
        List<SolicitudCreditoEntity> solicitudes;
        if (estado != null && !estado.isEmpty()) {
            solicitudes = solicitudCreditoRepository.findByEstadoOrderByFechaDesc(estado.toUpperCase());
        } else {
            solicitudes = solicitudCreditoRepository.findAllByOrderByFechaDesc();
        }
        model.addAttribute("solicitudes", solicitudes);
        model.addAttribute("estadoFiltro", estado);
        model.addAttribute("seccion", "creditos");
        
        // Contadores para gráficas
        model.addAttribute("solicitudesPendientes", 
            solicitudCreditoRepository.findByEstadoOrderByFechaDesc("PENDIENTE").size());
        model.addAttribute("solicitudesAprobadas", 
            solicitudCreditoRepository.findByEstadoOrderByFechaDesc("APROBADO").size());
        model.addAttribute("solicitudesRechazadas", 
            solicitudCreditoRepository.findByEstadoOrderByFechaDesc("RECHAZADO").size());
        model.addAttribute("totalClientes", 
            usuarioRepository.findAll().stream().filter(u -> "CLIENTE".equals(u.getRol())).count());
        
        return "admin";
    }

    @PostMapping("/creditos/eliminar/{id}")
    public String eliminarCredito(@PathVariable Long id, RedirectAttributes redirect) {
        try {
            solicitudCreditoRepository.deleteById(id);
            redirect.addFlashAttribute("exito", "Crédito eliminado correctamente.");
        } catch (Exception e) {
            redirect.addFlashAttribute("error", "Error al eliminar crédito: " + e.getMessage());
        }
        return "redirect:/admin/creditos";
    }

    // ============================================================
    // SECCIÓN MOVIMIENTOS - ELIMINADA (ahora está en MovimientoAdminController)
    // ============================================================
    // El método @GetMapping("/movimientos") ya no está aquí
    // Ahora usa MovimientoAdminController para los movimientos

    // ============================================================
    // CREAR CLIENTE Y CAMBIAR ESTADO DE CRÉDITO
    // ============================================================
    @PostMapping("/crear-cliente")
    public String crearCliente(@RequestParam String username,
                               @RequestParam String password,
                               @RequestParam Double saldoInicial,
                               RedirectAttributes redirect) {
        if (username == null || username.trim().isEmpty()) {
            redirect.addFlashAttribute("error", "El nombre de usuario es obligatorio.");
            return "redirect:/admin/clientes";
        }
        if (password == null || password.trim().isEmpty()) {
            redirect.addFlashAttribute("error", "La contraseña es obligatoria.");
            return "redirect:/admin/clientes";
        }
        if (saldoInicial == null || saldoInicial < 0) {
            redirect.addFlashAttribute("error", "El saldo inicial no puede ser negativo.");
            return "redirect:/admin/clientes";
        }
        try {
            bancaService.crearClienteConCuenta(username, password, saldoInicial);
            redirect.addFlashAttribute("exito", "Cliente creado exitosamente.");
        } catch (Exception e) {
            redirect.addFlashAttribute("error", "Error al crear cliente: " + e.getMessage());
        }
        return "redirect:/admin/clientes";
    }

    @PostMapping("/cambiar-estado")
    public String cambiarEstadoCredito(@RequestParam Long solicitudId,
                                       @RequestParam String nuevoEstado,
                                       RedirectAttributes redirect) {
        try {
            bancaService.cambiarEstadoCredito(solicitudId, nuevoEstado);
            String mensaje = "Solicitud #" + solicitudId + " " +
                             (nuevoEstado.equals("APROBADO") ? "✅ APROBADA" : "❌ RECHAZADA") +
                             " correctamente.";
            redirect.addFlashAttribute("exito", mensaje);
        } catch (Exception e) {
            redirect.addFlashAttribute("error", "Error al cambiar estado: " + e.getMessage());
        }
        return "redirect:/admin/creditos";
    }

    // ============================================================
    // PDFs
    // ============================================================
    
    @GetMapping("/pdf/estado-cuenta/{clienteId}")
    public ResponseEntity<InputStreamResource> generarEstadoCuentaPDF(@PathVariable Long clienteId) {
        UsuarioEntity cliente = usuarioRepository.findById(clienteId)
                .orElseThrow(() -> new RuntimeException("Cliente no encontrado"));
        
        List<CuentaEntity> cuentas = cliente.getCuentas();
        String clabe = (cuentas != null && !cuentas.isEmpty()) ? cuentas.get(0).getClabe() : null;
        
        List<MovimientoEntity> movimientos = clabe != null ? 
            movimientoRepository.findByClabe(clabe) : 
            movimientoRepository.findAllByOrderByFechaDesc();
        
        ByteArrayInputStream pdfStream = pdfService.generarEstadoCuenta(cliente, movimientos);
        
        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-Disposition", "attachment; filename=estado_cuenta_" + cliente.getUsername() + ".pdf");
        
        return ResponseEntity.ok()
                .headers(headers)
                .contentType(MediaType.APPLICATION_PDF)
                .body(new InputStreamResource(pdfStream));
    }

    @GetMapping("/pdf/contrato/{solicitudId}")
    public ResponseEntity<InputStreamResource> generarContratoPDF(@PathVariable Long solicitudId) {
        SolicitudCreditoEntity solicitud = solicitudCreditoRepository.findById(solicitudId)
                .orElseThrow(() -> new RuntimeException("Solicitud no encontrada"));
        
        ByteArrayInputStream pdfStream = pdfService.generarContratoCredito(solicitud);
        
        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-Disposition", "attachment; filename=contrato_" + solicitud.getId() + ".pdf");
        
        return ResponseEntity.ok()
                .headers(headers)
                .contentType(MediaType.APPLICATION_PDF)
                .body(new InputStreamResource(pdfStream));
    }
}