
package com.example.banco.bancobienestar.config;

import com.example.banco.bancobienestar.Repository.UsuarioRepository;
import com.example.banco.bancobienestar.model.UsuarioEntity;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements CommandLineRunner {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) throws Exception {
        if (usuarioRepository.count() == 0) {
            System.out.println("✅ Agregando Datos de Prueba...");

            // 1. Crear ADMIN
            UsuarioEntity admin = new UsuarioEntity();
            admin.setUsername("admin1");
            admin.setNombre("Administrador del Sistema");
            admin.setPassword(passwordEncoder.encode("12345"));
            admin.setRol("ADMIN");
            usuarioRepository.save(admin);
            System.out.println("✅ Usuario ADMIN creado: admin1 / 12345");

            // 2. Crear CLIENTE
            UsuarioEntity cliente = new UsuarioEntity();
            cliente.setUsername("cliente1");
            cliente.setNombre("José Daniel R.");
            cliente.setPassword(passwordEncoder.encode("12345"));
            cliente.setRol("CLIENTE");
            usuarioRepository.save(cliente);
            System.out.println("✅ Usuario CLIENTE creado: cliente1 / 12345");
        }
    }
}