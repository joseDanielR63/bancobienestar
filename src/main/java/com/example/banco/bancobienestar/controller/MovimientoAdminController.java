package com.example.banco.bancobienestar.controller;

import com.example.banco.bancobienestar.Repository.UsuarioRepository;
import com.example.banco.bancobienestar.model.MovimientoEntity;
import com.example.banco.bancobienestar.model.UsuarioEntity;
import com.example.banco.bancobienestar.service.BancaService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/admin/movimientos")  // ← ESTO SIGUE IGUAL
public class MovimientoAdminController {
    private final BancaService bancaService;
    private final UsuarioRepository usuarioRepository;
    
    public MovimientoAdminController(BancaService bancaService, UsuarioRepository usuarioRepository) {
        this.bancaService = bancaService;
        this.usuarioRepository = usuarioRepository;
    }

    @GetMapping
    public String listarMovimientos(@RequestParam(required = false) String cliente,
                                    Model model) {
        List<MovimientoEntity> movimientos;

        if (cliente != null && !cliente.isEmpty()) {
            UsuarioEntity usuario = usuarioRepository.findByUsername(cliente).orElse(null);
            if (usuario != null) {
                List<String> clabes = usuario.getCuentas().stream()
                        .map(c -> c.getClabe())
                        .collect(Collectors.toList());
                movimientos = bancaService.obtenerMovimientosPorClabes(clabes);
            } else {
                movimientos = List.of();
            }
        } else {
            movimientos = bancaService.todosMovimientos();
        }

        List<UsuarioEntity> clientes = usuarioRepository.findAll().stream()
                .filter(u -> "CLIENTE".equals(u.getRol()))
                .collect(Collectors.toList());

        model.addAttribute("movimientos", movimientos);
        model.addAttribute("clientes", clientes);
        model.addAttribute("clienteFiltro", cliente);

        return "adminMovimientos";  // ← VISTA DIFERENTE
    }

    @PostMapping("/actualizar")
    public String actualizarEstado(@RequestParam Long id,
                                   @RequestParam String nuevoEstado,
                                   RedirectAttributes redirect) {
        try {
            MovimientoEntity movimiento = bancaService.obtenerMovimientoId(id);
            String estadoAnterior = movimiento != null ? movimiento.getEstadoMovimiento() : "desconocido";
            bancaService.actualizarMovimiento(id, nuevoEstado);
            String mensaje = "Movimiento #" + id + " actualizado: " + estadoAnterior + " → " + nuevoEstado;
            if ("rechazado".equalsIgnoreCase(nuevoEstado) && "autorizado".equalsIgnoreCase(estadoAnterior)) {
                mensaje += " 🔄 Saldos revertidos (origen recupera, destino devuelve).";
            }
            redirect.addFlashAttribute("exito", mensaje);
        } catch (Exception e) {
            redirect.addFlashAttribute("error", "❌ Error al actualizar movimiento: " + e.getMessage());
        }
        return "redirect:/admin/movimientos";
    }

    @PostMapping("/eliminar/{id}")
    public String eliminarMovimiento(@PathVariable Long id,
                                     RedirectAttributes redirect) {
        try {
            bancaService.eliminarMovimiento(id);
            redirect.addFlashAttribute("exito", " Movimiento #" + id + " eliminado correctamente.");
        } catch (Exception e) {
            redirect.addFlashAttribute("error", " Error al eliminar movimiento: " + e.getMessage());
        }
        return "redirect:/admin/movimientos";
    }
}