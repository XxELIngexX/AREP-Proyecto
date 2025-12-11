package com.subsidios.rentajoven.domain.enums;

/**
 * Niveles de clasificación del SISBÉN.
 * Solo los niveles A, B y C son elegibles para Renta Joven.
 */
public enum NivelSISBEN {
    A("Pobreza extrema", true),
    B("Pobreza moderada", true),
    C("Vulnerable", true),
    D("No pobre no vulnerable", false);

    private final String descripcion;
    private final boolean elegible;

    NivelSISBEN(String descripcion, boolean elegible) {
        this.descripcion = descripcion;
        this.elegible = elegible;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public boolean isElegible() {
        return elegible;
    }
}