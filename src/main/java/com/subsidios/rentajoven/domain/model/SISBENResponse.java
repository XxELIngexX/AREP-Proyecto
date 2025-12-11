package com.subsidios.rentajoven.domain.model;

import com.subsidios.rentajoven.domain.enums.NivelSISBEN;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Respuesta completa de la consulta al SISBEN.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SISBENResponse {
    private NivelSISBEN nivel;
    private Double puntaje;
    private String departamento;
    private String municipio;
    private boolean encontrado;
}