package com.subsidios.rentajoven.domain.enums;

/**
 * Categorías de prueba para validación del sistema.
 */
public enum CategoriaTest {
    LEGITIMO_DEBERIA_APROBAR("Caso legítimo que debería ser aprobado"),
    FRAUDE_REAL("Caso marcado como fraude real"),
    RECHAZO_SISBEN("Debería rechazar por nivel SISBEN D"),
    RECHAZO_TITULO("Debería rechazar por tener título profesional"),
    RECHAZO_MATRICULA("Debería rechazar por matrícula no vigente"),
    RECHAZO_EDAD("Debería rechazar por edad fuera de rango");

    private final String descripcion;

    CategoriaTest(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getDescripcion() {
        return descripcion;
    }
}