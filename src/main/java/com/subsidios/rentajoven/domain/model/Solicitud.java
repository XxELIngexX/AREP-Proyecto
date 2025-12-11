package com.subsidios.rentajoven.domain.model;

import com.subsidios.rentajoven.domain.enums.EstadoSolicitud;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Entidad que representa una solicitud de subsidio Renta Joven.
 */
@Entity
@Table(name = "solicitudes")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Solicitud {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String cedula;
    
    @Column(nullable = false)
    private String matriculaId;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoSolicitud estado;
    
    @Column(columnDefinition = "TEXT")
    private String razonesRechazo;
    
    @Column(nullable = false)
    private LocalDateTime fechaSolicitud;
    
    @Column
    private LocalDateTime fechaProcesamiento;
    
    @Column
    private Long tiempoProcesamientoMs;
    
    @Column
    private Boolean aprobada;
}