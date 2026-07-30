package com.example.banco.bancobienestar.model;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class GastosDTO {
private String categoria;
private Double monto;
private String colorHex;


}
