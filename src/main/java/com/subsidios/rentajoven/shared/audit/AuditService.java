package com.subsidios.rentajoven.shared.audit;

import com.subsidios.rentajoven.domain.enums.TipoValidacion;
import com.subsidios.rentajoven.infrastructure.persistence.AuditLogRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Servicio de auditoría para registrar cada paso del proceso.
 * Garantiza trazabilidad completa e inmutable.
 */
@Service
public class AuditService {
    
    @Autowired
    private AuditLogRepository auditLogRepository;
    
    /**
     * Registra un evento de auditoría.
     * 
     * @param solicitudId ID de la solicitud
     * @param tipo Tipo de validación
     * @param exitosa Si fue exitosa o no
     * @param mensaje Mensaje descriptivo
     * @param detalles Detalles adicionales (JSON, texto, etc.)
     * @param tiempoMs Tiempo de ejecución en milisegundos
     */
    public void registrar(Long solicitudId, 
                         TipoValidacion tipo, 
                         boolean exitosa, 
                         String mensaje,
                         String detalles,
                         long tiempoMs) {
        
        AuditLog log = AuditLog.builder()
                .solicitudId(solicitudId)
                .tipoValidacion(tipo)
                .exitosa(exitosa)
                .mensaje(mensaje)
                .detalles(detalles)
                .tiempoEjecucionMs(tiempoMs)
                .timestamp(LocalDateTime.now())
                .build();
        
        auditLogRepository.save(log);
    }
    
    /**
     * Obtiene todos los logs de una solicitud específica.
     * 
     * @param solicitudId ID de la solicitud
     * @return Lista de logs ordenados cronológicamente
     */
    public List<AuditLog> obtenerLogsSolicitud(Long solicitudId) {
        return auditLogRepository.findBySolicitudIdOrderByTimestampAsc(solicitudId);
    }
    
    /**
     * Obtiene todos los logs de un tipo de validación.
     * 
     * @param tipo Tipo de validación
     * @return Lista de logs
     */
    public List<AuditLog> obtenerLogsPorTipo(TipoValidacion tipo) {
        return auditLogRepository.findByTipoValidacion(tipo);
    }
}