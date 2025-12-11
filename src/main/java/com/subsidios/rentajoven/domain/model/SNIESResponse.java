package com.subsidios.rentajoven.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Respuesta completa de la consulta al SNIES.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SNIESResponse {
    private boolean tieneTitulo;
    private String programa;
    private String institucion;
    private String tipoTitulo; // PROFESIONAL, TECNOLOGO
}