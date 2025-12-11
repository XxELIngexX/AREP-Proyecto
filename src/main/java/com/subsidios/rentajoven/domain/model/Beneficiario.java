package com.subsidios.rentajoven.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Información del solicitante del subsidio.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Beneficiario {
    private String cedula;
    private String nombres;
    private String apellidos;
    private Integer edad;
    private String matriculaId;
}