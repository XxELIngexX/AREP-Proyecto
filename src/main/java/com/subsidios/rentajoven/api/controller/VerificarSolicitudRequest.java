package com.subsidios.rentajoven.api.controller;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request para verificar elegibilidad de subsidio.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class VerificarSolicitudRequest {
    
    @NotBlank(message = "La cédula es obligatoria")
    @Pattern(regexp = "\\d{7,10}", message = "La cédula debe tener entre 7 y 10 dígitos")
    private String cedula;
    
    @NotBlank(message = "El ID de matrícula es obligatorio")
    private String matriculaId;
    
    @NotNull(message = "La edad es obligatoria")
    @Min(value = 10, message = "La edad debe ser mayor a 10 años")
    @Max(value = 35, message = "La edad debe ser menor a 35 años")
    private Integer edad;
}