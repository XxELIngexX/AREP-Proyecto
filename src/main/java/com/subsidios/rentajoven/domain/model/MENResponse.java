package com.subsidios.rentajoven.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Respuesta completa de la consulta al MEN.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MENResponse {
    private String matriculaId;
    private String cedula;
    private String institucion;
    private String programa;
    private String estado; // VIGENTE, INACTIVA
    private Integer intensidadHoraria;
    private boolean encontrada;
}