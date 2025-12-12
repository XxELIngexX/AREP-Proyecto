package com.subsidios.rentajoven.api.controller;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.subsidios.rentajoven.shared.metrics.MetricsCollector;
import com.subsidios.rentajoven.shared.metrics.MetricsSummary;

/**
 * Controlador para generar reportes académicos.
 */
@RestController
@RequestMapping("/api/reporte")
public class ReporteController {
    
    @Autowired
    private MetricsCollector metricsCollector;
    
    /**
     * Genera un reporte completo formateado para el artículo académico.
     * 
     * GET /api/reporte/academico
     */
    @GetMapping("/academico")
    public ResponseEntity<Map<String, Object>> generarReporteAcademico() {
        
        MetricsSummary metrics = metricsCollector.generarResumen();
        
        Map<String, Object> reporte = new HashMap<>();
        
        // Metadatos
        reporte.put("fechaGeneracion", LocalDateTime.now().format(
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
        ));
        reporte.put("sistema", "Renta Joven - Motor de Decisión Automatizado");
        reporte.put("version", "1.0.0");
        
        // Métricas de volumen
        Map<String, Object> volumen = new HashMap<>();
        volumen.put("totalSolicitudes", metrics.getTotalSolicitudes());
        volumen.put("aprobadas", metrics.getSolicitudesAprobadas());
        volumen.put("rechazadas", metrics.getSolicitudesRechazadas());
        volumen.put("tasaAprobacionPorcentaje", String.format("%.2f%%", metrics.getTasaAprobacion() * 100));
        volumen.put("tasaRechazoPorcentaje", String.format("%.2f%%", metrics.getTasaRechazo() * 100));
        reporte.put("volumen", volumen);
        
        // Métricas de rendimiento
        Map<String, Object> rendimiento = new HashMap<>();
        rendimiento.put("tiempoPromedioSegundos", String.format("%.3f", metrics.getTiempoPromedioSegundos()));
        rendimiento.put("tiempoMinimoMs", metrics.getTiempoMinimoMs());
        rendimiento.put("tiempoMaximoMs", metrics.getTiempoMaximoMs());
        rendimiento.put("tiempoSISBENMs", String.format("%.2f", metrics.getTiempoPromedioSISBEN()));
        rendimiento.put("tiempoSNIESMs", String.format("%.2f", metrics.getTiempoPromedioSNIES()));
        rendimiento.put("tiempoMENMs", String.format("%.2f", metrics.getTiempoPromedioMEN()));
        reporte.put("rendimiento", rendimiento);
        
        // Comparativa con sistema tradicional
        Map<String, Object> comparativa = new HashMap<>();
        comparativa.put("sistemaTradicionalDias", metrics.getTiempoSistemaTradicionalDias());
        comparativa.put("sistemaAutomatizadoSegundos", String.format("%.3f", metrics.getTiempoPromedioSegundos()));
        comparativa.put("mejoraEnTiempoPorcentaje", String.format("%.4f%%", metrics.getMejoraPorcentual()));
        comparativa.put("ahorroDiasPorSolicitud", String.format("%.2f", metrics.getAhorroDiasPorSolicitud()));
        reporte.put("comparativaConSistemaTradicional", comparativa);
        
        // Razones de rechazo
        Map<String, Object> rechazos = new HashMap<>();
        rechazos.put("porSISBEN", metrics.getRechazosPorSISBEN());
        rechazos.put("porTituloProfesional", metrics.getRechazosPorTitulo());
        rechazos.put("porMatricula", metrics.getRechazosPorMatricula());
        rechazos.put("distribucionPorcentual", metrics.getDistribucionRechazos());
        reporte.put("razonesDeRechazo", rechazos);
        
        return ResponseEntity.ok(reporte);
    }
}