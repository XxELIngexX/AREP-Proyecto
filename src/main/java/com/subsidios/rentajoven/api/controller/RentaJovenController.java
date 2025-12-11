package com.subsidios.rentajoven.api.controller;

import com.subsidios.rentajoven.application.service.SolicitudService;
import com.subsidios.rentajoven.domain.model.DecisionResult;
import com.subsidios.rentajoven.domain.model.Solicitud;
import com.subsidios.rentajoven.shared.audit.AuditLog;
import com.subsidios.rentajoven.shared.metrics.MetricsCollector;
import com.subsidios.rentajoven.shared.metrics.MetricsSummary;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

/**
 * Controlador REST para el sistema de verificación de subsidios Renta Joven.
 * 
 * Endpoints disponibles:
 * - POST /api/renta-joven/verificar - Procesa nueva solicitud
 * - GET /api/renta-joven/solicitud/{id} - Consulta solicitud
 * - GET /api/renta-joven/solicitud/{id}/audit - Obtiene trazabilidad
 * - GET /api/renta-joven/metrics/summary - Métricas del sistema
 * 
 * @author Cesar Amaya Gomez
 * @version 2.0
 */
@RestController
@RequestMapping("/api/renta-joven")
@CrossOrigin(origins = "*")
public class RentaJovenController {
    
    @Autowired
    private SolicitudService solicitudService;
    
    @Autowired
    private MetricsCollector metricsCollector;
    
    /**
     * Endpoint principal: Verificar elegibilidad de un ciudadano.
     * 
     * POST /api/renta-joven/verificar
     * 
     * Body:
     * {
     *   "cedula": "1000000001",
     *   "matriculaId": "MAT-00000001",
     *   "edad": 20
     * }
     * 
     * @param request Datos del solicitante (incluye edad)
     * @return Resultado de la verificación con todas las validaciones
     */
    @PostMapping("/verificar")
    public ResponseEntity<DecisionResult> verificarElegibilidad(
            @Valid @RequestBody VerificarSolicitudRequest request) {
        
        try {
            DecisionResult resultado = solicitudService.procesarSolicitud(
                request.getCedula(), 
                request.getMatriculaId(),
                request.getEdad()  // ⭐ NUEVO: Edad incluida
            );
            
            return ResponseEntity.ok(resultado);
            
        } catch (Exception e) {
            // En caso de error, retornar 500
            DecisionResult errorResult = DecisionResult.builder()
                    .aprobada(false)
                    .mensaje("Error al procesar la solicitud: " + e.getMessage())
                    .build();
            
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(errorResult);
        }
    }
    
    /**
     * Obtener información de una solicitud específica.
     * 
     * GET /api/renta-joven/solicitud/{id}
     * 
     * @param id ID de la solicitud
     * @return Datos de la solicitud
     */
    @GetMapping("/solicitud/{id}")
    public ResponseEntity<Solicitud> obtenerSolicitud(@PathVariable Long id) {
        
        Optional<Solicitud> solicitud = solicitudService.obtenerSolicitud(id);
        
        return solicitud
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
    
    /**
     * Obtener el audit trail completo de una solicitud.
     * Muestra paso a paso todo lo que ocurrió durante la verificación.
     * 
     * GET /api/renta-joven/solicitud/{id}/audit
     * 
     * Incluye:
     * - Validación de edad
     * - Validación de coherencia cédula-edad
     * - Consulta SISBEN
     * - Consulta SNIES
     * - Consulta MEN (matrícula, intensidad, institución)
     * - Decisión final
     * 
     * @param id ID de la solicitud
     * @return Lista de logs de auditoría ordenados cronológicamente
     */
    @GetMapping("/solicitud/{id}/audit")
    public ResponseEntity<List<AuditLog>> obtenerAuditSolicitud(@PathVariable Long id) {
        
        // Verificar que la solicitud existe
        Optional<Solicitud> solicitud = solicitudService.obtenerSolicitud(id);
        
        if (solicitud.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        
        List<AuditLog> logs = solicitudService.obtenerAuditSolicitud(id);
        
        return ResponseEntity.ok(logs);
    }
    
    /**
     * Obtener todas las solicitudes de un ciudadano.
     * 
     * GET /api/renta-joven/solicitudes?cedula=1000000001
     * 
     * @param cedula Cédula del ciudadano
     * @return Lista de solicitudes del ciudadano
     */
    @GetMapping("/solicitudes")
    public ResponseEntity<List<Solicitud>> obtenerSolicitudesPorCedula(
            @RequestParam String cedula) {
        
        List<Solicitud> solicitudes = solicitudService.obtenerSolicitudesPorCedula(cedula);
        
        return ResponseEntity.ok(solicitudes);
    }
    
    /**
     * Obtener resumen de métricas del sistema.
     * Esta información es clave para el artículo académico.
     * 
     * GET /api/renta-joven/metrics/summary
     * 
     * Incluye:
     * - Total de solicitudes procesadas
     * - Tasa de aprobación/rechazo
     * - Tiempos promedio por validación
     * - Razones de rechazo más frecuentes
     * - Comparativa con sistema tradicional (45 días)
     * 
     * @return Métricas agregadas del sistema
     */
    @GetMapping("/metrics/summary")
    public ResponseEntity<MetricsSummary> obtenerMetricas() {
        
        MetricsSummary metrics = metricsCollector.generarResumen();
        
        return ResponseEntity.ok(metrics);
    }
    
    /**
     * Obtener todas las solicitudes aprobadas.
     * 
     * GET /api/renta-joven/solicitudes/aprobadas
     * 
     * @return Lista de solicitudes aprobadas
     */
    @GetMapping("/solicitudes/aprobadas")
    public ResponseEntity<List<Solicitud>> obtenerSolicitudesAprobadas() {
        
        List<Solicitud> solicitudes = solicitudService.obtenerSolicitudesAprobadas();
        
        return ResponseEntity.ok(solicitudes);
    }
    
    /**
     * Obtener todas las solicitudes rechazadas.
     * 
     * GET /api/renta-joven/solicitudes/rechazadas
     * 
     * @return Lista de solicitudes rechazadas
     */
    @GetMapping("/solicitudes/rechazadas")
    public ResponseEntity<List<Solicitud>> obtenerSolicitudesRechazadas() {
        
        List<Solicitud> solicitudes = solicitudService.obtenerSolicitudesRechazadas();
        
        return ResponseEntity.ok(solicitudes);
    }
    
    /**
     * Endpoint de health check para verificar que la API está funcionando.
     * 
     * GET /api/renta-joven/health
     * 
     * @return Status del sistema con información de carga de datos
     */
    @GetMapping("/health")
    public ResponseEntity<HealthStatus> healthCheck() {
        HealthStatus status = HealthStatus.builder()
                .status("OK")
                .mensaje("Sistema Renta Joven operativo ✅")
                .version("2.0")
                .validacionesActivas(7)
                .build();
        
        return ResponseEntity.ok(status);
    }
    
    /**
     * DTO para respuesta de health check.
     */
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class HealthStatus {
        private String status;
        private String mensaje;
        private String version;
        private Integer validacionesActivas;
    }
}