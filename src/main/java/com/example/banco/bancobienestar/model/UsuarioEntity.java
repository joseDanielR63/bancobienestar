package com.example.banco.bancobienestar.model;

import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Data;


@Entity
@Data
@Table(name = "usuarios")

public class UsuarioEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nombre;

    @Column(nullable = false, unique = true)
    private String username;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private String rol;

// Relación de un usuario con muchas cuentas
@OneToMany(
    mappedBy = "usuario",
    cascade = CascadeType.ALL,
    fetch = FetchType.LAZY
)
private List<CuentaEntity> cuentas;

@Override
public String toString() {
    return "UsuarioEntity{id=" + id + ", nombre='" + nombre + "', username='" + username + "'}";
}
}