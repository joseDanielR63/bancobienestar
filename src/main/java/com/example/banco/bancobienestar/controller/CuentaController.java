package com.example.banco.bancobienestar.controller;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class CuentaController {

    @GetMapping("/cuentas")
    public String listarCuentas(Model modelo) {

        // Datos de ejemplo del cliente
        String cliente = "Ana Lucia Perez";
        String clabe = "646180123456789012";
        Double saldoTotal = 12500.75;

        // Lista de cuentas (ejemplo) con los campos que usa la vista: id, clabe, tipo, saldo
        List<Map<String, Object>> cuentas = List.of(
            Map.of("id", 1L, "clabe", "646180123456789012", "tipo", "Ahorro", "saldo", 8500.00),
            Map.of("id", 2L, "clabe", "646180987654321098", "tipo", "Corriente", "saldo", 4000.75),
            Map.of("id", 3L, "clabe", "646180112233445566", "tipo", "Inversión", "saldo", 0.00)
        );

        modelo.addAttribute("nombreCliente", cliente);
        modelo.addAttribute("cuentaClabe", clabe);
        modelo.addAttribute("saldoTotal", saldoTotal);
        modelo.addAttribute("cuentas", cuentas);

        return "cuentas";
    }
}