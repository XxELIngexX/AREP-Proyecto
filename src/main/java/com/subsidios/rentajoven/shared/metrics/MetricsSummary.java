package com.subsidios.rentajoven.shared.metrics;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Resumen de métricas del sistema.
 * Esta información es clave para el análisis del artículo académico.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MetricsSummary {
    
    // ===================================================================
    // MÉTRICAS GENERALES
    // ===================================================================
    
    /**
     * Total de solicitudes procesadas
     */
    private Long totalSolicitudes;
    
    /**
     * Solicitudes aprobadas
     */
    private Long solicitudesAprobadas;
    
    /**
     * Solicitudes rechazadas
     */
    private Long solicitudesRechazadas;
    
    /**
     * Tasa de aprobación (0.0 a 1.0)
     */
    private Double tasaAprobacion;
    
    /**
     * Tasa de rechazo (0.0 a 1.0)
     */
    private Double tasaRechazo;
    
    // ===================================================================
    // MÉTRICAS DE RENDIMIENTO
    // ===================================================================
    
    /**
     * Tiempo promedio de procesamiento en milisegundos
     */
    private Double tiempoPromedioMs;
    
    /**
     * Tiempo mínimo observado en milisegundos
     */
    private Long tiempoMinimoMs;
    
    /**
     * Tiempo máximo observado en milisegundos
     */
    private Long tiempoMaximoMs;
    
    /**
     * Tiempo promedio en segundos (para legibilidad)
     */
    private Double tiempoPromedioSegundos;
    
    // ===================================================================
    // MÉTRICAS POR VALIDACIÓN
    // ===================================================================
    
    /**
     * Tiempo promedio consulta SISBEN (ms)
     */
    private Double tiempoPromedioSISBEN;
    
    /**
     * Tiempo promedio consulta SNIES (ms)
     */
    private Double tiempoPromedioSNIES;
    
    /**
     * Tiempo promedio consulta MEN (ms)
     */
    private Double tiempoPromedioMEN;
    
    // ===================================================================
    // RAZONES DE RECHAZO
    // ===================================================================
    
    /**
     * Rechazos por nivel SISBEN
     */
    private Long rechazosPorSISBEN;
    
    /**
     * Rechazos por título profesional
     */
    private Long rechazosPorTitulo;
    
    /**
     * Rechazos por matrícula
     */
    private Long rechazosPorMatricula;
    
    /**
     * Distribución porcentual de razones de rechazo
     */
    private Map<String, Double> distribucionRechazos;
    
    // ===================================================================
    // COMPARATIVA CON SISTEMA TRADICIONAL
    // ===================================================================
    
    /**
     * Tiempo sistema tradicional en días (45 días según artículo)
     */
    private Integer tiempoSistemaTradicionalDias;
    
    /**
     * Mejora en tiempo (porcentaje)
     */
    private Double mejoraPorcentual;
    
    /**
     * Ahorro estimado en días por solicitud
     */
    private Double ahorroDiasPorSolicitud;
}