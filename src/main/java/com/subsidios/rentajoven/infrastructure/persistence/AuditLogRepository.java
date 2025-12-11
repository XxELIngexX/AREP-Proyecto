package com.subsidios.rentajoven.infrastructure.persistence;

import com.subsidios.rentajoven.shared.audit.AuditLog;
import com.subsidios.rentajoven.domain.enums.TipoValidacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repositorio para logs de auditoría.
 */
@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
    
    // Obtener todos los logs de una solicitud (ordenados por timestamp)
    List<AuditLog> findBySolicitudIdOrderByTimestampAsc(Long solicitudId);
    
    // Obtener logs por tipo de validación
    List<AuditLog> findByTipoValidacion(TipoValidacion tipo);
    
    // Calcular tiempo promedio por tipo de validación
    @Query("SELECT AVG(a.tiempoEjecucionMs) FROM AuditLog a WHERE a.tipoValidacion = :tipo")
    Double calcularTiempoPromedioMs(TipoValidacion tipo);
    
    // Contar validaciones exitosas por tipo
    @Query("SELECT COUNT(a) FROM AuditLog a WHERE a.tipoValidacion = :tipo AND a.exitosa = true")
    Long contarExitosasPorTipo(TipoValidacion tipo);
    
    // Contar validaciones fallidas por tipo
    @Query("SELECT COUNT(a) FROM AuditLog a WHERE a.tipoValidacion = :tipo AND a.exitosa = false")
    Long contarFalladasPorTipo(TipoValidacion tipo);
}