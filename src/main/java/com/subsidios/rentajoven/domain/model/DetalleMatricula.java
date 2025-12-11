package com.subsidios.rentajoven.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Información detallada de una matrícula.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DetalleMatricula {
    private String matriculaId;
    private String estado;  // VIGENTE, INACTIVA
    private String institucion;
    private String programa;
    private Integer intensidadHoraria;  // Horas semanales
}