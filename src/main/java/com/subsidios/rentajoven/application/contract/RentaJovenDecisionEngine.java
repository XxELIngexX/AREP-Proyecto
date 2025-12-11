package com.subsidios.rentajoven.application.contract;

import com.subsidios.rentajoven.domain.enums.MotivoRechazo;
import com.subsidios.rentajoven.domain.enums.NivelSISBEN;
import com.subsidios.rentajoven.domain.enums.TipoValidacion;
import com.subsidios.rentajoven.domain.model.*;
import com.subsidios.rentajoven.infrastructure.external.MENSimulator;
import com.subsidios.rentajoven.infrastructure.external.SISBENSimulator;
import com.subsidios.rentajoven.infrastructure.external.SNIESSimulator;
import com.subsidios.rentajoven.shared.audit.AuditService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Motor de Decisión Automatizado para el programa Renta Joven - VERSIÓN 2.0
 * 
 * REGLAS DE ELEGIBILIDAD COMPLETAS:
 * 1. Edad entre 14 y 28 años
 * 2. Coherencia entre cédula y edad (detección de fraude)
 * 3. Estar en SISBEN nivel A, B o C
 * 4. NO tener título profesional registrado en SNIES
 * 5. Tener matrícula VIGENTE en el MEN
 * 6. Intensidad horaria >= 20 horas semanales
 * 7. Institución reconocida por el MEN (no fraudulenta)
 * 
 * @author Cesar Amaya Gomez
 * @version 2.0
 */
@Service
public class RentaJovenDecisionEngine {
    
    @Autowired
    private SISBENSimulator sisbenSimulator;
    
    @Autowired
    private SNIESSimulator sniesSimulator;
    
    @Autowired
    private MENSimulator menSimulator;
    
    @Autowired
    private AuditService auditService;
    
    // Lista de instituciones fraudulentas conocidas
    private static final List<String> INSTITUCIONES_FRAUDULENTAS = Arrays.asList(
        "Instituto Digital Global",
        "Universidad Virtual del Caribe Online",
        "Centro Educativo Los Pinos",
        "Academia Superior de Gestión"
    );
    
    // Rangos de cédula por cohorte de nacimiento (según a.py)
    private static final int CEDULA_MIN_2006_2010 = 1120000000;
    private static final int CEDULA_MAX_2010 = 1150000000;
    private static final int CEDULA_MIN_2001_2005 = 1100000000;
    private static final int CEDULA_MAX_2005 = 1119999999;
    private static final int CEDULA_MIN_1996_2000 = 1080000000;
    private static final int CEDULA_MAX_2000 = 1099999999;
    
    /**
     * Evalúa la elegibilidad de un beneficiario con TODAS las validaciones.
     */
    public DecisionResult evaluar(Beneficiario beneficiario, Long solicitudId) {
        
        long tiempoInicio = System.currentTimeMillis();
        List<MotivoRechazo> motivosRechazo = new ArrayList<>();
        
        // ===================================================================
        // VALIDACIÓN 1: EDAD (14-28 años)
        // ===================================================================
        ValidacionIndividual validacionEdad = validarEdad(beneficiario.getEdad(), solicitudId);
        if (!validacionEdad.getExitosa()) {
            motivosRechazo.add(validacionEdad.getMotivoRechazo());
        }
        
        // ===================================================================
        // VALIDACIÓN 2: COHERENCIA CÉDULA-EDAD (anti-fraude)
        // ===================================================================
        ValidacionIndividual validacionCoherencia = validarCoherenciaCedulaEdad(
            beneficiario.getCedula(), 
            beneficiario.getEdad(), 
            solicitudId
        );
        if (!validacionCoherencia.getExitosa()) {
            motivosRechazo.add(validacionCoherencia.getMotivoRechazo());
        }
        
        // ===================================================================
        // VALIDACIÓN 3: NIVEL SISBEN (A, B, C)
        // ===================================================================
        ValidacionIndividual validacionSISBEN = validarNivelSISBEN(
            beneficiario.getCedula(), 
            solicitudId
        );
        if (!validacionSISBEN.getExitosa()) {
            motivosRechazo.add(validacionSISBEN.getMotivoRechazo());
        }
        
        // ===================================================================
        // VALIDACIÓN 4: AUSENCIA DE TÍTULO PROFESIONAL
        // ===================================================================
        ValidacionIndividual validacionTitulo = validarAusenciaTituloProfesional(
            beneficiario.getCedula(), 
            solicitudId
        );
        if (!validacionTitulo.getExitosa()) {
            motivosRechazo.add(validacionTitulo.getMotivoRechazo());
        }
        
        // ===================================================================
        // VALIDACIÓN 5: MATRÍCULA VIGENTE
        // ===================================================================
        ValidacionIndividual validacionMatricula = validarMatriculaCompleta(
            beneficiario.getMatriculaId(), 
            solicitudId
        );
        if (!validacionMatricula.getExitosa()) {
            motivosRechazo.add(validacionMatricula.getMotivoRechazo());
        }
        
        // ===================================================================
        // DECISIÓN FINAL: TODAS las validaciones deben ser exitosas
        // ===================================================================
        boolean aprobada = validacionEdad.getExitosa()
                        && validacionCoherencia.getExitosa()
                        && validacionSISBEN.getExitosa() 
                        && validacionTitulo.getExitosa() 
                        && validacionMatricula.getExitosa();
        
        long tiempoFin = System.currentTimeMillis();
        long tiempoTotal = tiempoFin - tiempoInicio;
        
        // Construir razones detalladas
        List<String> razones = construirRazones(
            validacionEdad,
            validacionCoherencia,
            validacionSISBEN, 
            validacionTitulo, 
            validacionMatricula, 
            aprobada
        );
        
        // Registrar decisión final en auditoría
        auditService.registrar(
            solicitudId,
            TipoValidacion.DECISION_FINAL,
            aprobada,
            aprobada ? "SOLICITUD APROBADA" : "SOLICITUD RECHAZADA",
            aprobada ? String.join("; ", razones) : "Motivos: " + motivosRechazo.toString(),
            tiempoTotal
        );
        
        // ===================================================================
        // CONSTRUIR RESULTADO COMPLETO
        // ===================================================================
        return DecisionResult.builder()
                .solicitudId(solicitudId)
                .aprobada(aprobada)
                .mensaje(aprobada 
                    ? "✅ Solicitud aprobada - Cumple todos los requisitos del programa Renta Joven" 
                    : "❌ Solicitud rechazada - No cumple con " + motivosRechazo.size() + " requisito(s)")
                .razones(razones)
                .tiempoTotalMs(tiempoTotal)
                .validaciones(DetalleValidaciones.builder()
                    .sisben(validacionSISBEN)
                    .tituloProfesional(validacionTitulo)
                    .matricula(validacionMatricula)
                    .build())
                .build();
    }
    
    // ===================================================================
    // VALIDACIONES INDIVIDUALES
    // ===================================================================
    
    /**
     * VALIDACIÓN 1: Edad debe estar entre 14 y 28 años.
     */
    private ValidacionIndividual validarEdad(Integer edad, Long solicitudId) {
        long inicio = System.currentTimeMillis();
        
        boolean exitosa = edad != null && edad >= 14 && edad <= 28;
        
        String mensaje = exitosa
            ? String.format("✓ Edad %d años - Dentro del rango permitido (14-28)", edad)
            : String.format("✗ Edad %d años - Fuera del rango permitido (14-28)", edad != null ? edad : 0);
        
        String detalle = String.format(
            "Validación de edad. Edad declarada: %d. Rango permitido: 14-28 años. Resultado: %s",
            edad != null ? edad : 0,
            exitosa ? "VÁLIDO" : "INVÁLIDO"
        );
        
        long fin = System.currentTimeMillis();
        
        auditService.registrar(
            solicitudId,
            TipoValidacion.SISBEN, // Temporalmente
            exitosa,
            mensaje,
            detalle,
            fin - inicio
        );
        
        return ValidacionIndividual.builder()
                .exitosa(exitosa)
                .mensaje(mensaje)
                .tiempoMs(fin - inicio)
                .detalle(detalle)
                .motivoRechazo(exitosa ? null : MotivoRechazo.EDAD_FUERA_RANGO)
                .build();
    }
    
    /**
     * VALIDACIÓN 2: Coherencia entre cédula y edad (anti-fraude).
     */
    private ValidacionIndividual validarCoherenciaCedulaEdad(String cedula, Integer edad, Long solicitudId) {
        long inicio = System.currentTimeMillis();
        
        try {
            long cedulaNum = Long.parseLong(cedula);
            int añoActual = 2025;
            int edadEstimada = -1;
            
            // Estimar edad según rango de cédula
            if (cedulaNum >= CEDULA_MIN_2006_2010 && cedulaNum <= CEDULA_MAX_2010) {
                edadEstimada = añoActual - 2008; // ~17 años (promedio 2006-2010)
            } else if (cedulaNum >= CEDULA_MIN_2001_2005 && cedulaNum <= CEDULA_MAX_2005) {
                edadEstimada = añoActual - 2003; // ~22 años
            } else if (cedulaNum >= CEDULA_MIN_1996_2000 && cedulaNum <= CEDULA_MAX_2000) {
                edadEstimada = añoActual - 1998; // ~27 años
            }
            
            // Permitir margen de error de ±4 años
            boolean coherente = edadEstimada == -1 || Math.abs(edadEstimada - edad) <= 4;
            
            String mensaje = coherente
                ? String.format("✓ Cédula y edad son coherentes (edad: %d, estimada: %d)", edad, edadEstimada)
                : String.format("✗ ALERTA FRAUDE: Cédula no coherente con edad (edad: %d, estimada: %d, diferencia: %d)", 
                    edad, edadEstimada, Math.abs(edadEstimada - edad));
            
            String detalle = String.format(
                "Cédula: %s, Edad declarada: %d, Edad estimada por cédula: %d, Diferencia: %d años",
                cedula, edad, edadEstimada, edadEstimada != -1 ? Math.abs(edadEstimada - edad) : 0
            );
            
            long fin = System.currentTimeMillis();
            
            auditService.registrar(
                solicitudId,
                TipoValidacion.SISBEN,
                coherente,
                mensaje,
                detalle,
                fin - inicio
            );
            
            return ValidacionIndividual.builder()
                    .exitosa(coherente)
                    .mensaje(mensaje)
                    .tiempoMs(fin - inicio)
                    .detalle(detalle)
                    .motivoRechazo(coherente ? null : MotivoRechazo.COHERENCIA_CEDULA_EDAD)
                    .build();
                    
        } catch (Exception e) {
            long fin = System.currentTimeMillis();
            return ValidacionIndividual.builder()
                    .exitosa(false)
                    .mensaje("✗ Error validando coherencia cédula-edad")
                    .tiempoMs(fin - inicio)
                    .detalle(e.getMessage())
                    .motivoRechazo(MotivoRechazo.ERROR_SISTEMA)
                    .build();
        }
    }
    
    /**
     * VALIDACIÓN 3: Nivel SISBEN debe ser A, B o C.
     */
    private ValidacionIndividual validarNivelSISBEN(String cedula, Long solicitudId) {
        long inicio = System.currentTimeMillis();
        
        try {
            SISBENResponse response = sisbenSimulator.consultar(cedula);
            
            long fin = System.currentTimeMillis();
            long tiempo = fin - inicio;
            
            if (!response.isEncontrado()) {
                auditService.registrar(
                    solicitudId,
                    TipoValidacion.SISBEN,
                    false,
                    "No encontrado en SISBEN",
                    "Cédula " + cedula + " no encontrada en base SISBEN",
                    tiempo
                );
                
                return ValidacionIndividual.builder()
                        .exitosa(false)
                        .mensaje("✗ No se encontró registro en SISBEN")
                        .tiempoMs(tiempo)
                        .detalle("Cédula no encontrada en base de datos SISBEN")
                        .motivoRechazo(MotivoRechazo.SISBEN_NO_ENCONTRADO)
                        .build();
            }
            
            boolean elegible = response.getNivel().isElegible();
            
            String mensaje = elegible 
                ? String.format("✓ Nivel %s (%s) - Elegible", 
                    response.getNivel().name(), response.getNivel().getDescripcion())
                : String.format("✗ Nivel %s (%s) - No elegible", 
                    response.getNivel().name(), response.getNivel().getDescripcion());
            
            String detalle = String.format(
                "SISBEN consultado. Cédula: %s, Nivel: %s, Puntaje: %.2f, Ubicación: %s - %s",
                cedula, response.getNivel().name(), response.getPuntaje(),
                response.getDepartamento(), response.getMunicipio()
            );
            
            auditService.registrar(
                solicitudId,
                TipoValidacion.SISBEN,
                elegible,
                mensaje,
                detalle,
                tiempo
            );
            
            return ValidacionIndividual.builder()
                    .exitosa(elegible)
                    .mensaje(mensaje)
                    .tiempoMs(tiempo)
                    .detalle(detalle)
                    .motivoRechazo(elegible ? null : MotivoRechazo.NIVEL_SISBEN_NO_ELEGIBLE)
                    .build();
                    
        } catch (Exception e) {
            long fin = System.currentTimeMillis();
            return crearValidacionError(solicitudId, TipoValidacion.SISBEN, e, fin - inicio);
        }
    }
    
    /**
     * VALIDACIÓN 4: NO debe tener título profesional.
     */
    private ValidacionIndividual validarAusenciaTituloProfesional(String cedula, Long solicitudId) {
        long inicio = System.currentTimeMillis();
        
        try {
            SNIESResponse response = sniesSimulator.consultar(cedula);
            
            long fin = System.currentTimeMillis();
            long tiempo = fin - inicio;
            
            boolean exitosa = !response.isTieneTitulo();
            
            String mensaje = exitosa
                ? "✓ No tiene título profesional registrado - Elegible"
                : String.format("✗ Tiene título profesional: %s (%s) - No elegible",
                    response.getPrograma(), response.getTipoTitulo());
            
            String detalle = response.isTieneTitulo()
                ? String.format("SNIES: Título registrado. Programa: %s, Institución: %s, Tipo: %s",
                    response.getPrograma(), response.getInstitucion(), response.getTipoTitulo())
                : "SNIES: No se encontró título profesional registrado";
            
            auditService.registrar(
                solicitudId,
                TipoValidacion.TITULO_PROFESIONAL,
                exitosa,
                mensaje,
                detalle,
                tiempo
            );
            
            return ValidacionIndividual.builder()
                    .exitosa(exitosa)
                    .mensaje(mensaje)
                    .tiempoMs(tiempo)
                    .detalle(detalle)
                    .motivoRechazo(exitosa ? null : MotivoRechazo.TIENE_TITULO_PROFESIONAL)
                    .build();
                    
        } catch (Exception e) {
            long fin = System.currentTimeMillis();
            return crearValidacionError(solicitudId, TipoValidacion.TITULO_PROFESIONAL, e, fin - inicio);
        }
    }
    
    /**
     * VALIDACIÓN 5: Matrícula VIGENTE + Intensidad >= 20h + Institución reconocida.
     */
    private ValidacionIndividual validarMatriculaCompleta(String matriculaId, Long solicitudId) {
        long inicio = System.currentTimeMillis();
        
        try {
            MENResponse response = menSimulator.consultar(matriculaId);
            
            long fin = System.currentTimeMillis();
            long tiempo = fin - inicio;
            
            // 5.1 - Verificar que existe
            if (!response.isEncontrada()) {
                auditService.registrar(
                    solicitudId,
                    TipoValidacion.MATRICULA,
                    false,
                    "Matrícula no encontrada",
                    "ID: " + matriculaId,
                    tiempo
                );
                
                return ValidacionIndividual.builder()
                        .exitosa(false)
                        .mensaje("✗ Matrícula no encontrada en el sistema MEN")
                        .tiempoMs(tiempo)
                        .detalle("ID matrícula: " + matriculaId)
                        .motivoRechazo(MotivoRechazo.MATRICULA_NO_ENCONTRADA)
                        .build();
            }
            
            // 5.2 - Verificar que está VIGENTE
            boolean vigente = "VIGENTE".equalsIgnoreCase(response.getEstado());
            if (!vigente) {
                String mensaje = String.format("✗ Matrícula NO vigente (estado: %s)", response.getEstado());
                auditService.registrar(solicitudId, TipoValidacion.MATRICULA, false, mensaje, "", tiempo);
                
                return ValidacionIndividual.builder()
                        .exitosa(false)
                        .mensaje(mensaje)
                        .tiempoMs(tiempo)
                        .detalle("Estado actual: " + response.getEstado())
                        .motivoRechazo(MotivoRechazo.MATRICULA_NO_VIGENTE)
                        .build();
            }
            
            // 5.3 - Verificar intensidad horaria >= 20
            boolean intensidadSuficiente = response.getIntensidadHoraria() >= 20;
            if (!intensidadSuficiente) {
                String mensaje = String.format("✗ Intensidad horaria insuficiente: %dh (mínimo 20h)", 
                    response.getIntensidadHoraria());
                auditService.registrar(solicitudId, TipoValidacion.MATRICULA, false, mensaje, "", tiempo);
                
                return ValidacionIndividual.builder()
                        .exitosa(false)
                        .mensaje(mensaje)
                        .tiempoMs(tiempo)
                        .detalle("Intensidad actual: " + response.getIntensidadHoraria() + " horas/semana")
                        .motivoRechazo(MotivoRechazo.INTENSIDAD_HORARIA_INSUFICIENTE)
                        .build();
            }
            
            // 5.4 - Verificar institución reconocida (no fraudulenta)
            boolean institucionReconocida = !INSTITUCIONES_FRAUDULENTAS.contains(response.getInstitucion());
            if (!institucionReconocida) {
                String mensaje = String.format("✗ Institución NO reconocida: %s", response.getInstitucion());
                auditService.registrar(solicitudId, TipoValidacion.MATRICULA, false, mensaje, "", tiempo);
                
                return ValidacionIndividual.builder()
                        .exitosa(false)
                        .mensaje(mensaje)
                        .tiempoMs(tiempo)
                        .detalle("Institución en lista negra")
                        .motivoRechazo(MotivoRechazo.INSTITUCION_NO_RECONOCIDA)
                        .build();
            }
            
            // ✅ TODO BIEN
            String mensaje = String.format("✓ Matrícula VIGENTE - %s (%dh/semana) - Elegible",
                response.getInstitucion(), response.getIntensidadHoraria());
            
            String detalle = String.format(
                "MEN: Matrícula %s. Institución: %s, Programa: %s, Estado: %s, Intensidad: %dh",
                matriculaId, response.getInstitucion(), response.getPrograma(),
                response.getEstado(), response.getIntensidadHoraria()
            );
            
            auditService.registrar(solicitudId, TipoValidacion.MATRICULA, true, mensaje, detalle, tiempo);
            
            return ValidacionIndividual.builder()
                    .exitosa(true)
                    .mensaje(mensaje)
                    .tiempoMs(tiempo)
                    .detalle(detalle)
                    .motivoRechazo(null)
                    .build();
                    
        } catch (Exception e) {
            long fin = System.currentTimeMillis();
            return crearValidacionError(solicitudId, TipoValidacion.MATRICULA, e, fin - inicio);
        }
    }
    
    /**
     * Construye la lista de razones detalladas.
     */
    private List<String> construirRazones(
            ValidacionIndividual edad,
            ValidacionIndividual coherencia,
            ValidacionIndividual sisben,
            ValidacionIndividual titulo,
            ValidacionIndividual matricula,
            boolean aprobada) {
        
        List<String> razones = new ArrayList<>();
        
        razones.add(edad.getMensaje());
        razones.add(coherencia.getMensaje());
        razones.add(sisben.getMensaje());
        razones.add(titulo.getMensaje());
        razones.add(matricula.getMensaje());
        
        return razones;
    }
    
    /**
     * Crea una validación de error genérica.
     */
    private ValidacionIndividual crearValidacionError(
            Long solicitudId, 
            TipoValidacion tipo, 
            Exception e, 
            long tiempo) {
        
        String mensaje = "Error en validación: " + e.getMessage();
        
        auditService.registrar(solicitudId, tipo, false, mensaje, e.toString(), tiempo);
        
        return ValidacionIndividual.builder()
                .exitosa(false)
                .mensaje(mensaje)
                .tiempoMs(tiempo)
                .detalle(e.toString())
                .motivoRechazo(MotivoRechazo.ERROR_SISTEMA)
                .build();
    }
}