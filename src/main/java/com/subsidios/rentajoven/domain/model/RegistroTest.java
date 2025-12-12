package com.subsidios.rentajoven.domain.model;

import com.subsidios.rentajoven.domain.enums.CategoriaTest;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Registro de prueba del dataset maestro.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegistroTest {
    private String cedula;
    private Integer edad;
    private String departamento;
    private String municipio;
    private String sisbenNivel;
    private Double sisbenPuntaje;
    private Boolean tituloSnies;
    private String programaTitulo;
    private Boolean matriculaVigente;
    private String estadoMatricula;
    private String institucionMatricula;
    private String programaMatricula;
    private Boolean esFraudeReal;
    private CategoriaTest categoria;
    private String matriculaId; // Se buscará en MEN
}