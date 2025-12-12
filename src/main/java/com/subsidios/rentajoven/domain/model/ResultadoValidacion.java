package com.subsidios.rentajoven.domain.model;

import com.subsidios.rentajoven.domain.enums.CategoriaTest;
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
public class ResultadoValidacion {
    private String cedula;
    private CategoriaTest categoriaEsperada;
    private Boolean resultadoEsperado; // true = debería aprobar
    private Boolean resultadoObtenido; // true = se aprobó
    private Boolean correcto; // true = coincide esperado con obtenido
    private String razonRechazo;
    private Long tiempoMs;
    private String detalle;
}