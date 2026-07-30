package com.example.banco.bancobienestar.controller;

import java.util.List;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import com.example.banco.bancobienestar.Repository.MovimientoCuentaRepository;
import com.example.banco.bancobienestar.Repository.UsuarioRepository;
import com.example.banco.bancobienestar.model.CuentaEntity;
import com.example.banco.bancobienestar.model.MovimientoEntity;
import com.example.banco.bancobienestar.model.UsuarioEntity;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Controller
@RequiredArgsConstructor
@Slf4j
public class DashboardController {

    private final UsuarioRepository usuarioRepository;
    private final MovimientoCuentaRepository movimientoCuentaRepository;

    @GetMapping("/")
    public String index() {
        return "redirect:/dashboard";
    }

    @GetMapping("/dashboard")
    public String mostrarDashboard(Model model, Authentication authentication) {

        // 1. OBTENER EL USUARIO AUTENTICADO
        String username = authentication.getName();
        log.info("Usuario autenticado: {}", username);

        // Buscar el usuario con sus cuentas (JOIN FETCH)
        UsuarioEntity usuario = usuarioRepository.findByUsernameWithCuentas(username)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado: " + username));

        // 2. OBTENER DATOS DE LA CUENTA PRINCIPAL (si existe)
        String clabe = "Sin cuenta asignada";
        Double saldoTotal = 0.0;
        List<MovimientoEntity> ultimosMovimientos = List.of();

        List<CuentaEntity> cuentas = usuario.getCuentas();

        if (cuentas != null && !cuentas.isEmpty()) {
            CuentaEntity cuentaPrincipal = cuentas.get(0);
            clabe = cuentaPrincipal.getClabe();
            saldoTotal = cuentaPrincipal.getSaldo();

            // OBTENER ÚLTIMOS 10 MOVIMIENTOS (usando la CLABE como String)
            // El método en el repositorio acepta String (CLABE), no CuentaEntity
            ultimosMovimientos = movimientoCuentaRepository
                    .findTop10ByCuenta(cuentaPrincipal.getClabe(), cuentaPrincipal.getClabe());
        } else {
            log.warn("El usuario {} no tiene cuentas asignadas.", username);
        }

        // 3. PASAR DATOS AL MODELO PARA LA VISTA
        model.addAttribute("nombreCliente", usuario.getNombre());
        model.addAttribute("cuentaClabe", clabe);
        model.addAttribute("saldoTotal", saldoTotal);
        model.addAttribute("movimientos", ultimosMovimientos);

        return "dashboard";
    }
}