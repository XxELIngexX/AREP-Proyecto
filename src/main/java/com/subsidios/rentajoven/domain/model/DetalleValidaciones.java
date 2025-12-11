package com.subsidios.rentajoven.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Detalle de cada validación individual realizada.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DetalleValidaciones {
    private ValidacionIndividual sisben;
    private ValidacionIndividual tituloProfesional;
    private ValidacionIndividual matricula;
}