package com.subsidios.rentajoven.shared.metrics;

import com.subsidios.rentajoven.domain.enums.TipoValidacion;
import com.subsidios.rentajoven.domain.model.Solicitud;
import com.subsidios.rentajoven.infrastructure.persistence.AuditLogRepository;
import com.subsidios.rentajoven.infrastructure.persistence.SolicitudRepository;
import com.subsidios.rentajoven.shared.audit.AuditLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Servicio para recolectar y calcular métricas del sistema.
 * Genera estadísticas para el análisis académico.
 */
@Service
public class MetricsCollector {
    
    @Autowired
    private SolicitudRepository solicitudRepository;
    
    @Autowired
    private AuditLogRepository auditLogRepository;
    
    // Tiempo del sistema tradicional según el artículo (45 días)
    private static final int TIEMPO_SISTEMA_TRADICIONAL_DIAS = 45;
    
    /**
     * Genera un resumen completo de métricas del sistema.
     * 
     * @return Resumen con todas las estadísticas
     */
    public MetricsSummary generarResumen() {
        
        // Obtener todas las solicitudes
        List<Solicitud> todasLasSolicitudes = solicitudRepository.findAll();
        
        if (todasLasSolicitudes.isEmpty()) {
            return generarResumenVacio();
        }
        
        // Calcular métricas generales
        long total = todasLasSolicitudes.size();
        long aprobadas = solicitudRepository.contarAprobadas();
        long rechazadas = solicitudRepository.contarRechazadas();
        
        double tasaAprobacion = total > 0 ? (double) aprobadas / total : 0.0;
        double tasaRechazo = total > 0 ? (double) rechazadas / total : 0.0;
        
        // Calcular métricas de rendimiento
        Double tiempoPromedioMs = solicitudRepository.calcularTiempoPromedioMs();
        if (tiempoPromedioMs == null) tiempoPromedioMs = 0.0;
        
        Long tiempoMinimo = calcularTiempoMinimo(todasLasSolicitudes);
        Long tiempoMaximo = calcularTiempoMaximo(todasLasSolicitudes);
        
        // Calcular tiempos por validación
        Double tiempoSISBEN = auditLogRepository.calcularTiempoPromedioMs(TipoValidacion.SISBEN);
        Double tiempoSNIES = auditLogRepository.calcularTiempoPromedioMs(TipoValidacion.TITULO_PROFESIONAL);
        Double tiempoMEN = auditLogRepository.calcularTiempoPromedioMs(TipoValidacion.MATRICULA);
        
        // Calcular razones de rechazo
        Map<String, Long> rechazos = calcularRazonesRechazo();
        Map<String, Double> distribucion = calcularDistribucionRechazos(rechazos, rechazadas);
        
        // Calcular comparativa con sistema tradicional
        double tiempoPromedioSegundos = tiempoPromedioMs / 1000.0;
        double tiempoTradSegundos = TIEMPO_SISTEMA_TRADICIONAL_DIAS * 24 * 60 * 60;
        double mejoraPorcentual = ((tiempoTradSegundos - tiempoPromedioSegundos) / tiempoTradSegundos) * 100;
        double ahorroDias = TIEMPO_SISTEMA_TRADICIONAL_DIAS - (tiempoPromedioSegundos / (24 * 60 * 60));
        
        return MetricsSummary.builder()
                // Métricas generales
                .totalSolicitudes(total)
                .solicitudesAprobadas(aprobadas)
                .solicitudesRechazadas(rechazadas)
                .tasaAprobacion(tasaAprobacion)
                .tasaRechazo(tasaRechazo)
                // Métricas de rendimiento
                .tiempoPromedioMs(tiempoPromedioMs)
                .tiempoMinimoMs(tiempoMinimo)
                .tiempoMaximoMs(tiempoMaximo)
                .tiempoPromedioSegundos(tiempoPromedioSegundos)
                // Métricas por validación
                .tiempoPromedioSISBEN(tiempoSISBEN != null ? tiempoSISBEN : 0.0)
                .tiempoPromedioSNIES(tiempoSNIES != null ? tiempoSNIES : 0.0)
                .tiempoPromedioMEN(tiempoMEN != null ? tiempoMEN : 0.0)
                // Razones de rechazo
                .rechazosPorSISBEN(rechazos.getOrDefault("SISBEN", 0L))
                .rechazosPorTitulo(rechazos.getOrDefault("TITULO", 0L))
                .rechazosPorMatricula(rechazos.getOrDefault("MATRICULA", 0L))
                .distribucionRechazos(distribucion)
                // Comparativa
                .tiempoSistemaTradicionalDias(TIEMPO_SISTEMA_TRADICIONAL_DIAS)
                .mejoraPorcentual(mejoraPorcentual)
                .ahorroDiasPorSolicitud(ahorroDias)
                .build();
    }
    
    /**
     * Calcula el tiempo mínimo de procesamiento.
     */
    private Long calcularTiempoMinimo(List<Solicitud> solicitudes) {
        return solicitudes.stream()
                .filter(s -> s.getTiempoProcesamientoMs() != null)
                .mapToLong(Solicitud::getTiempoProcesamientoMs)
                .min()
                .orElse(0L);
    }
    
    /**
     * Calcula el tiempo máximo de procesamiento.
     */
    private Long calcularTiempoMaximo(List<Solicitud> solicitudes) {
        return solicitudes.stream()
                .filter(s -> s.getTiempoProcesamientoMs() != null)
                .mapToLong(Solicitud::getTiempoProcesamientoMs)
                .max()
                .orElse(0L);
    }
    
    /**
     * Calcula las razones de rechazo analizando los logs de auditoría.
     */
    private Map<String, Long> calcularRazonesRechazo() {
        Map<String, Long> rechazos = new HashMap<>();
        
        Long rechazosSISBEN = auditLogRepository.contarFalladasPorTipo(TipoValidacion.SISBEN);
        Long rechazosTitulo = auditLogRepository.contarFalladasPorTipo(TipoValidacion.TITULO_PROFESIONAL);
        Long rechazosMatricula = auditLogRepository.contarFalladasPorTipo(TipoValidacion.MATRICULA);
        
        rechazos.put("SISBEN", rechazosSISBEN != null ? rechazosSISBEN : 0L);
        rechazos.put("TITULO", rechazosTitulo != null ? rechazosTitulo : 0L);
        rechazos.put("MATRICULA", rechazosMatricula != null ? rechazosMatricula : 0L);
        
        return rechazos;
    }
    
    /**
     * Calcula la distribución porcentual de las razones de rechazo.
     */
    private Map<String, Double> calcularDistribucionRechazos(
            Map<String, Long> rechazos, long totalRechazos) {
        
        Map<String, Double> distribucion = new HashMap<>();
        
        if (totalRechazos == 0) {
            distribucion.put("SISBEN", 0.0);
            distribucion.put("TITULO", 0.0);
            distribucion.put("MATRICULA", 0.0);
            return distribucion;
        }
        
        for (Map.Entry<String, Long> entry : rechazos.entrySet()) {
            double porcentaje = (entry.getValue().doubleValue() / totalRechazos) * 100;
            distribucion.put(entry.getKey(), porcentaje);
        }
        
        return distribucion;
    }
    
    /**
     * Genera un resumen vacío cuando no hay datos.
     */
    private MetricsSummary generarResumenVacio() {
        return MetricsSummary.builder()
                .totalSolicitudes(0L)
                .solicitudesAprobadas(0L)
                .solicitudesRechazadas(0L)
                .tasaAprobacion(0.0)
                .tasaRechazo(0.0)
                .tiempoPromedioMs(0.0)
                .tiempoMinimoMs(0L)
                .tiempoMaximoMs(0L)
                .tiempoPromedioSegundos(0.0)
                .tiempoPromedioSISBEN(0.0)
                .tiempoPromedioSNIES(0.0)
                .tiempoPromedioMEN(0.0)
                .rechazosPorSISBEN(0L)
                .rechazosPorTitulo(0L)
                .rechazosPorMatricula(0L)
                .distribucionRechazos(new HashMap<>())
                .tiempoSistemaTradicionalDias(TIEMPO_SISTEMA_TRADICIONAL_DIAS)
                .mejoraPorcentual(0.0)
                .ahorroDiasPorSolicitud(0.0)
                .build();
    }
}
