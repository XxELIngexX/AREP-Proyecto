package com.subsidios.rentajoven.domain.enums;

/**
 * Estados posibles de una solicitud de subsidio.
 */
public enum EstadoSolicitud {
    PENDIENTE("Solicitud pendiente de evaluación"),
    APROBADA("Solicitud aprobada automáticamente"),
    RECHAZADA("Solicitud rechazada por incumplimiento de criterios"),
    EN_REVISION_MANUAL("Requiere revisión manual por caso especial"),
    ERROR("Error durante el procesamiento");

    private final String descripcion;

    EstadoSolicitud(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getDescripcion() {
        return descripcion;
    }
}