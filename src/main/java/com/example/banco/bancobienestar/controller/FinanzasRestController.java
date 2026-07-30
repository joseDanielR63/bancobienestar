package com.example.banco.bancobienestar.controller;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.banco.bancobienestar.Repository.MovimientoCuentaRepository;
import com.example.banco.bancobienestar.Repository.UsuarioRepository;
import com.example.banco.bancobienestar.model.GastosDTO;
import com.example.banco.bancobienestar.model.MovimientoEntity;
import com.example.banco.bancobienestar.model.UsuarioEntity;

@RestController
@RequestMapping("/api/v1/finanzas")
public class FinanzasRestController {

    private final MovimientoCuentaRepository movimientoRepository;
    private final UsuarioRepository usuarioRepository;

    // Mapa de colores por categoría
    private static final Map<String, String> COLOR_MAP = new HashMap<>();
    static {
        COLOR_MAP.put("Alimentación", "#FF6384");
        COLOR_MAP.put("Vivienda", "#4BC0C0");
        COLOR_MAP.put("Transporte", "#FFCE56");
        COLOR_MAP.put("Otros", "#36A2EB");
        COLOR_MAP.put("Servicios", "#4BC0C0");
        COLOR_MAP.put("Ocio", "#FF7675");
        COLOR_MAP.put("Comida", "#00C6FD");
        COLOR_MAP.put("Renta", "#00A8FF");
        COLOR_MAP.put("Nomina", "#00C6FD");
        COLOR_MAP.put("Tecnología", "#6C5CE7");
        COLOR_MAP.put("Entretenimiento", "#FD79A8");
        COLOR_MAP.put("CREDITO", "#FF6B6B");
        COLOR_MAP.put("TRANSFERENCIA", "#FF9F43");
        COLOR_MAP.put("DEPOSITO", "#2ED573");
    }

    private static final List<String> PALETA_COLORES = Arrays.asList(
        "#FF6384", "#00d6fd", "#ffc107", "#dc3545",
        "#4cd137", "#ff7675", "#e056fd", "#00a8ff", "#e056fd"
    );

    public FinanzasRestController(MovimientoCuentaRepository movimientoRepository,
                                  UsuarioRepository usuarioRepository) {
        this.movimientoRepository = movimientoRepository;
        this.usuarioRepository = usuarioRepository;
    }

    @GetMapping("/gastos-mes")
    public List<GastosDTO> obtenerGastos(Authentication authentication) {
        // 1. Obtener usuario autenticado
        String username = authentication.getName();
        UsuarioEntity usuario = usuarioRepository.findByUsernameWithCuentas(username)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        // 2. Obtener CLABES de TODAS sus cuentas
        List<String> clabes = usuario.getCuentas().stream()
                .map(c -> c.getClabe())
                .collect(Collectors.toList());

        if (clabes.isEmpty()) {
            return List.of();
        }

        // 3. Recorrer todas las cuentas y obtener movimientos
        List<MovimientoEntity> todosLosMovimientos = new ArrayList<>();
        for (String clabe : clabes) {
            List<MovimientoEntity> movs = movimientoRepository
                    .findByCuentaOrigenOrCuentaDestinoOrderByFechaDesc(clabe, clabe);
            if (movs != null && !movs.isEmpty()) {
                todosLosMovimientos.addAll(movs);
            }
        }

        if (todosLosMovimientos.isEmpty()) {
            return Collections.emptyList();
        }

        // 4. Tomamos TODOS los movimientos (sin filtrar por signo)
        List<MovimientoEntity> movimientosFiltrados = todosLosMovimientos.stream()
                .filter(m -> m.getMonto() != null)
                .collect(Collectors.toList());

        if (movimientosFiltrados.isEmpty()) {
            return Collections.emptyList();
        }

        // 5. Agrupar por tipo NORMALIZADO (primera letra mayúscula, resto minúscula)
        Map<String, Double> agrupado = movimientosFiltrados.stream()
                .filter(m -> m.getTipo() != null && m.getMonto() != null)
                .collect(Collectors.groupingBy(
                        m -> normalizarCategoria(m.getTipo()),
                        Collectors.collectingAndThen(
                                Collectors.toList(),
                                list -> list.stream()
                                        .mapToDouble(m -> Math.abs(m.getMonto()))
                                        .sum()
                        )
                ));

        // 6. Convertir a lista de GastosDTO
        // MODIFICADO: Se mantiene el nombre de la categoría tal como está en 'entry.getKey()'
        // Si no hay color en el mapa, se usa uno de la paleta.
        int[] idx = {0};
        return agrupado.entrySet().stream()
                .map(entry -> {
                    String categoria = entry.getKey(); // Nombre del tipo (normalizado)
                    Double monto = entry.getValue();
                    // Buscar color en el mapa, si no existe usar paleta
                    String color = COLOR_MAP.getOrDefault(categoria, PALETA_COLORES.get(idx[0] % PALETA_COLORES.size()));
                    idx[0]++;
                    return new GastosDTO(categoria, monto, color);
                })
                .collect(Collectors.toList());
    }

    // Normaliza el texto (primera letra mayúscula, resto minúscula)
    private String normalizarCategoria(String texto) {
        if (texto == null || texto.isEmpty()) {
            return "Otros";
        }
        return texto.substring(0, 1).toUpperCase() + texto.substring(1).toLowerCase();
    }
}