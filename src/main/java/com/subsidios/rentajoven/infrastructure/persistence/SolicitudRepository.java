package com.subsidios.rentajoven.infrastructure.persistence;

import com.subsidios.rentajoven.domain.model.Solicitud;
import com.subsidios.rentajoven.domain.enums.EstadoSolicitud;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repositorio para gestionar solicitudes de subsidio.
 */
@Repository
public interface SolicitudRepository extends JpaRepository<Solicitud, Long> {
    
    // Buscar solicitudes por estado
    List<Solicitud> findByEstado(EstadoSolicitud estado);
    
    // Buscar por cédula
    List<Solicitud> findByCedula(String cedula);
    
    // Solicitudes aprobadas
    List<Solicitud> findByAprobadaTrue();
    
    // Solicitudes rechazadas
    List<Solicitud> findByAprobadaFalse();
    
    // Solicitudes en un rango de fechas
    List<Solicitud> findByFechaSolicitudBetween(LocalDateTime inicio, LocalDateTime fin);
    
    // Contar solicitudes aprobadas
    @Query("SELECT COUNT(s) FROM Solicitud s WHERE s.aprobada = true")
    Long contarAprobadas();
    
    // Contar solicitudes rechazadas
    @Query("SELECT COUNT(s) FROM Solicitud s WHERE s.aprobada = false")
    Long contarRechazadas();
    
    // Calcular tiempo promedio de procesamiento
    @Query("SELECT AVG(s.tiempoProcesamientoMs) FROM Solicitud s WHERE s.tiempoProcesamientoMs IS NOT NULL")
    Double calcularTiempoPromedioMs();
}