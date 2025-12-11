package com.subsidios.rentajoven.domain.enums;

/**
 * Motivos específicos de rechazo de una solicitud.
 */
public enum MotivoRechazo {
    EDAD_FUERA_RANGO("Edad fuera del rango permitido (14-28 años)"),
    NIVEL_SISBEN_NO_ELEGIBLE("Nivel SISBEN no elegible (solo A, B, C)"),
    SISBEN_NO_ENCONTRADO("No se encontró registro en SISBEN"),
    TIENE_TITULO_PROFESIONAL("Ya cuenta con título profesional"),
    MATRICULA_NO_ENCONTRADA("No se encontró la matrícula en el sistema MEN"),
    MATRICULA_NO_VIGENTE("La matrícula no está vigente"),
    INTENSIDAD_HORARIA_INSUFICIENTE("Intensidad horaria menor a 20 horas semanales"),
    INSTITUCION_NO_RECONOCIDA("Institución educativa no reconocida por el MEN"),
    COHERENCIA_CEDULA_EDAD("Inconsistencia entre cédula y edad declarada (posible fraude)"),
    ERROR_SISTEMA("Error en el sistema durante la validación");

    private final String descripcion;

    MotivoRechazo(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getDescripcion() {
        return descripcion;
    }
}