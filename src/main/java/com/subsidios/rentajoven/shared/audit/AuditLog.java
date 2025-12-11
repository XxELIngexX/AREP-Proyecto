package com.subsidios.rentajoven.shared.audit;

import com.subsidios.rentajoven.domain.enums.TipoValidacion;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Registro de auditoría para trazabilidad completa.
 * Cada paso del proceso genera un log inmutable.
 */
@Entity
@Table(name = "audit_logs")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditLog {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private Long solicitudId;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoValidacion tipoValidacion;
    
    @Column(nullable = false)
    private Boolean exitosa;
    
    @Column(columnDefinition = "TEXT")
    private String mensaje;
    
    @Column(columnDefinition = "TEXT")
    private String detalles;
    
    @Column(nullable = false)
    private Long tiempoEjecucionMs;
    
    @Column(nullable = false)
    private LocalDateTime timestamp;
}