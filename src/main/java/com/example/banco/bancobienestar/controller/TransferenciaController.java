package com.example.banco.bancobienestar.controller;
import com.example.banco.bancobienestar.model.CuentaEntity;
import com.example.banco.bancobienestar.model.UsuarioEntity;
import com.example.banco.bancobienestar.service.BancaService;
import com.example.banco.bancobienestar.Repository.UsuarioRepository;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
public class TransferenciaController {

    private final BancaService bancaService;
    private final UsuarioRepository usuarioRepository;

    public TransferenciaController(BancaService bancaService, UsuarioRepository usuarioRepository) {
        this.bancaService = bancaService;
        this.usuarioRepository = usuarioRepository;
    }

    // Mostrar formulario de transferencia
    @GetMapping("/transferencias")
    public String mostrarTransferencias(Model model, Authentication authentication) {
        String username = authentication.getName();
        UsuarioEntity usuario = usuarioRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        List<CuentaEntity> cuentas = usuario.getCuentas();
        model.addAttribute("cuentas", cuentas);

        return "transferencias";
    }

    // Procesar transferencia
    @PostMapping("/procesar-transferencia")
    public String procesarTransferencia(@RequestParam String clabeDestino,
                                        @RequestParam Double monto,
                                        @RequestParam(required = false) String descripcion,
                                        Authentication authentication,
                                        RedirectAttributes redirectAttributes) {
        try {
            String username = authentication.getName();
            bancaService.transferirDesdeUsuario(username, clabeDestino, monto, descripcion);
            redirectAttributes.addFlashAttribute("exito", "Transferencia realizada con éxito");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error al transferir: " + e.getMessage());
        }
        return "redirect:/transferencias";
    }
}