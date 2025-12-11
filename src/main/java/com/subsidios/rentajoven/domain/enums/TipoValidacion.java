package com.subsidios.rentajoven.domain.enums;

/**
 * Tipos de validación que se realizan en el proceso.
 */
public enum TipoValidacion {
    SISBEN("Verificación nivel SISBÉN"),
    TITULO_PROFESIONAL("Verificación ausencia título profesional"),
    MATRICULA("Verificación matrícula vigente"),
    DECISION_FINAL("Decisión final del motor");

    private final String descripcion;

    TipoValidacion(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getDescripcion() {
        return descripcion;
    }
}