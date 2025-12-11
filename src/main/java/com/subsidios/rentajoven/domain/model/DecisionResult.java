package com.subsidios.rentajoven.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Resultado completo de la evaluación del motor de decisión.
 * Esta clase representa la respuesta que recibe el usuario después
 * de que el sistema evalúa su solicitud de subsidio.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DecisionResult {
    
    /**
     * ID de la solicitud procesada
     */
    private Long solicitudId;
    
    /**
     * Decisión final: true = APROBADA, false = RECHAZADA
     */
    private Boolean aprobada;
    
    /**
     * Mensaje principal del resultado
     */
    private String mensaje;
    
    /**
     * Lista de razones detalladas (por qué fue aprobada o rechazada)
     */
    private List<String> razones;
    
    /**
     * Tiempo total de procesamiento en milisegundos
     */
    private Long tiempoTotalMs;
    
    /**
     * Detalle de cada validación individual realizada
     */
    private DetalleValidaciones validaciones;
}