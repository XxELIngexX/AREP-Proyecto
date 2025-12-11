package com.subsidios.rentajoven.application.service;

import com.subsidios.rentajoven.application.contract.RentaJovenDecisionEngine;
import com.subsidios.rentajoven.domain.enums.EstadoSolicitud;
import com.subsidios.rentajoven.domain.model.Beneficiario;
import com.subsidios.rentajoven.domain.model.DecisionResult;
import com.subsidios.rentajoven.domain.model.Solicitud;
import com.subsidios.rentajoven.infrastructure.persistence.SolicitudRepository;
import com.subsidios.rentajoven.shared.audit.AuditLog;
import com.subsidios.rentajoven.shared.audit.AuditService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Servicio de aplicación para gestionar solicitudes de subsidio.
 * Orquesta el flujo completo: crear solicitud -> evaluar -> guardar resultado.
 */
@Service
public class SolicitudService {

    @Autowired
    private SolicitudRepository solicitudRepository;

    @Autowired
    private RentaJovenDecisionEngine decisionEngine;

    @Autowired
    private AuditService auditService;

    /**
     * Procesa una nueva solicitud de subsidio.
     * 
     * @param cedula      Cédula del solicitante
     * @param matriculaId ID de la matrícula
     * @return Resultado de la decisión
     */
    @Transactional
    public DecisionResult procesarSolicitud(String cedula, String matriculaId, Integer edad) {

        // 1. Crear solicitud inicial
        Solicitud solicitud = Solicitud.builder()
                .cedula(cedula)
                .matriculaId(matriculaId)
                .estado(EstadoSolicitud.PENDIENTE)
                .fechaSolicitud(LocalDateTime.now())
                .build();

        // Guardar para obtener ID
        solicitud = solicitudRepository.save(solicitud);

        // 2. Crear beneficiario con edad
        Beneficiario beneficiario = Beneficiario.builder()
                .cedula(cedula)
                .matriculaId(matriculaId)
                .edad(edad) // ⭐ NUEVO
                .build();

        // 3. Ejecutar motor de decisión
        DecisionResult resultado = decisionEngine.evaluar(beneficiario, solicitud.getId());

        // 4. Actualizar solicitud con resultado
        solicitud.setAprobada(resultado.getAprobada());
        solicitud.setEstado(resultado.getAprobada()
                ? EstadoSolicitud.APROBADA
                : EstadoSolicitud.RECHAZADA);
        solicitud.setRazonesRechazo(resultado.getAprobada()
                ? null
                : String.join("; ", resultado.getRazones()));
        solicitud.setFechaProcesamiento(LocalDateTime.now());
        solicitud.setTiempoProcesamientoMs(resultado.getTiempoTotalMs());

        // 5. Guardar resultado final
        solicitudRepository.save(solicitud);

        return resultado;
    }

    /**
     * Obtiene una solicitud por su ID.
     */
    public Optional<Solicitud> obtenerSolicitud(Long id) {
        return solicitudRepository.findById(id);
    }

    /**
     * Obtiene todas las solicitudes de un ciudadano.
     */
    public List<Solicitud> obtenerSolicitudesPorCedula(String cedula) {
        return solicitudRepository.findByCedula(cedula);
    }

    /**
     * Obtiene el audit trail completo de una solicitud.
     */
    public List<AuditLog> obtenerAuditSolicitud(Long solicitudId) {
        return auditService.obtenerLogsSolicitud(solicitudId);
    }

    /**
     * Obtiene todas las solicitudes aprobadas.
     */
    public List<Solicitud> obtenerSolicitudesAprobadas() {
        return solicitudRepository.findByAprobadaTrue();
    }

    /**
     * Obtiene todas las solicitudes rechazadas.
     */
    public List<Solicitud> obtenerSolicitudesRechazadas() {
        return solicitudRepository.findByAprobadaFalse();
    }
}