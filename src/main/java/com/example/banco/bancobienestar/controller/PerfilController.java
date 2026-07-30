package com.example.banco.bancobienestar.controller;

import com.example.banco.bancobienestar.Repository.UsuarioRepository;
import com.example.banco.bancobienestar.model.UsuarioEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class PerfilController {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    public PerfilController(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping("/perfil")
    public String mostrarPerfil(Model model, Authentication authentication) {
        String username = authentication.getName();
        UsuarioEntity usuario = usuarioRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        model.addAttribute("usuario", usuario);
        return "perfil";
    }

    @PostMapping("/perfil/actualizar")
    public String actualizarPerfil(@RequestParam String nombre,
                                   @RequestParam(required = false) String nuevaPassword,
                                   Authentication authentication,
                                   RedirectAttributes redirect) {
        try {
            String username = authentication.getName();
            UsuarioEntity usuario = usuarioRepository.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

            // Actualizar nombre
            usuario.setNombre(nombre);

            // Actualizar contraseña solo si se envió y no está vacía
            if (nuevaPassword != null && !nuevaPassword.trim().isEmpty()) {
                usuario.setPassword(passwordEncoder.encode(nuevaPassword));
            }

            usuarioRepository.save(usuario);
            redirect.addFlashAttribute("exito", "Perfil actualizado correctamente.");

        } catch (Exception e) {
            redirect.addFlashAttribute("error", "Error al actualizar perfil: " + e.getMessage());
        }
        return "redirect:/perfil";
    }
}