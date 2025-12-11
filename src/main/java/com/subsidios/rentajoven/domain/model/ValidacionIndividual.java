package com.subsidios.rentajoven.domain.model;

import com.subsidios.rentajoven.domain.enums.MotivoRechazo;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Resultado de una validación individual.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ValidacionIndividual {
    private Boolean exitosa;
    private String mensaje;
    private Long tiempoMs;
    private String detalle;
    private MotivoRechazo motivoRechazo; // Puede ser null si la validación fue exitosa
}