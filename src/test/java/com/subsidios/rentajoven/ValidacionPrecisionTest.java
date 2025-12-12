package com.subsidios.rentajoven;

import com.subsidios.rentajoven.application.service.SolicitudService;
import com.subsidios.rentajoven.domain.enums.CategoriaTest;
import com.subsidios.rentajoven.domain.model.DecisionResult;
import com.subsidios.rentajoven.domain.model.RegistroTest;
import com.subsidios.rentajoven.domain.model.ResultadoValidacion;
import com.subsidios.rentajoven.infrastructure.external.DatasetTestLoader;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.FileWriter;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pruebas de validación y precisión del sistema.
 * Genera métricas para el artículo académico.
 */
@SpringBootTest
public class ValidacionPrecisionTest {
    
    @Autowired
    private SolicitudService solicitudService;
    
    @Autowired
    private DatasetTestLoader datasetLoader;
    
    /**
     * Prueba principal: Valida 1000 registros del dataset maestro.
     */
    @Test
    public void testValidacionConDatasetMaestro() throws Exception {
        System.out.println("\n" + "=".repeat(70));
        System.out.println("🧪 VALIDACIÓN DE PRECISIÓN DEL SISTEMA");
        System.out.println("=".repeat(70) + "\n");
        
        // Cargar registros del dataset maestro
        List<RegistroTest> registros = datasetLoader.cargarRegistros(1000);
        
        assertFalse(registros.isEmpty(), "Debe haber registros en el dataset");
        
        // Ejecutar validaciones
        List<ResultadoValidacion> resultados = ejecutarValidaciones(registros);
        
        // Calcular métricas
        MetricasValidacion metricas = calcularMetricas(resultados);
        
        // Generar reporte
        generarReporteCompleto(metricas, resultados);
        
        // Assertions
        assertTrue(metricas.precision >= 0.50, "La precisión debe ser al menos 50%");
        
        System.out.println("\n" + "=".repeat(70));
        System.out.println("✅ VALIDACIÓN COMPLETADA");
        System.out.println(String.format("   Precisión global: %.2f%%", metricas.precision * 100));
        System.out.println(String.format("   Detección de fraudes: %.2f%%", metricas.tasaDeteccionFraudes * 100));
        System.out.println("=".repeat(70) + "\n");
    }
    
    /**
     * Ejecuta validaciones para todos los registros.
     */
    private List<ResultadoValidacion> ejecutarValidaciones(List<RegistroTest> registros) {
        List<ResultadoValidacion> resultados = new ArrayList<>();
        
        System.out.println("🔄 Ejecutando validaciones...\n");
        
        int count = 0;
        for (RegistroTest reg : registros) {
            count++;
            
            try {
                long inicio = System.currentTimeMillis();
                
                // Ejecutar solicitud
                DecisionResult decision = solicitudService.procesarSolicitud(
                    reg.getCedula(),
                    reg.getMatriculaId(),
                    reg.getEdad()
                );
                
                long fin = System.currentTimeMillis();
                
                // Determinar resultado esperado
                boolean esperado = reg.getCategoria() == CategoriaTest.LEGITIMO_DEBERIA_APROBAR;
                boolean obtenido = decision.getAprobada();
                boolean correcto = (esperado == obtenido);
                
                // Extraer razón de rechazo si fue rechazado
                String razonRechazo = null;
                if (!decision.getAprobada() && decision.getRazones() != null && !decision.getRazones().isEmpty()) {
                    razonRechazo = decision.getRazones().stream()
                        .filter(r -> r.startsWith("✗"))
                        .findFirst()
                        .orElse("Sin razón específica");
                }
                
                ResultadoValidacion resultado = ResultadoValidacion.builder()
                        .cedula(reg.getCedula())
                        .categoriaEsperada(reg.getCategoria())
                        .resultadoEsperado(esperado)
                        .resultadoObtenido(obtenido)
                        .correcto(correcto)
                        .razonRechazo(razonRechazo)
                        .tiempoMs(fin - inicio)
                        .detalle(decision.getMensaje())
                        .build();
                
                resultados.add(resultado);
                
                // Mostrar progreso
                if (count % 100 == 0) {
                    System.out.println(String.format("   ✓ %d/%d procesados (%.1f%%)", 
                        count, registros.size(), (count * 100.0 / registros.size())));
                }
                
            } catch (Exception e) {
                System.err.println("   ⚠️  Error procesando registro " + count + ": " + e.getMessage());
            }
        }
        
        System.out.println("\n✅ Validaciones completadas\n");
        
        return resultados;
    }
    
    /**
     * Calcula métricas de precisión.
     */
    private MetricasValidacion calcularMetricas(List<ResultadoValidacion> resultados) {
        MetricasValidacion metricas = new MetricasValidacion();
        
        metricas.totalProcesados = resultados.size();
        metricas.correctos = (int) resultados.stream().filter(ResultadoValidacion::getCorrecto).count();
        metricas.incorrectos = metricas.totalProcesados - metricas.correctos;
        
        // Verdaderos Positivos: Casos legítimos aprobados
        metricas.verdaderosPositivos = (int) resultados.stream()
            .filter(r -> r.getCategoriaEsperada() == CategoriaTest.LEGITIMO_DEBERIA_APROBAR)
            .filter(r -> r.getResultadoObtenido())
            .count();
        
        // Verdaderos Negativos: Casos que deberían rechazarse y se rechazaron
        metricas.verdaderosNegativos = (int) resultados.stream()
            .filter(r -> r.getCategoriaEsperada() != CategoriaTest.LEGITIMO_DEBERIA_APROBAR)
            .filter(r -> !r.getResultadoObtenido())
            .count();
        
        // Falsos Positivos: Casos que deberían rechazarse pero se aprobaron
        metricas.falsosPositivos = (int) resultados.stream()
            .filter(r -> r.getCategoriaEsperada() != CategoriaTest.LEGITIMO_DEBERIA_APROBAR)
            .filter(r -> r.getResultadoObtenido())
            .count();
        
        // Falsos Negativos: Casos legítimos que se rechazaron
        metricas.falsosNegativos = (int) resultados.stream()
            .filter(r -> r.getCategoriaEsperada() == CategoriaTest.LEGITIMO_DEBERIA_APROBAR)
            .filter(r -> !r.getResultadoObtenido())
            .count();
        
        // Fraudes
        metricas.totalFraudes = (int) resultados.stream()
            .filter(r -> r.getCategoriaEsperada() == CategoriaTest.FRAUDE_REAL)
            .count();
        
        metricas.fraudesDetectados = (int) resultados.stream()
            .filter(r -> r.getCategoriaEsperada() == CategoriaTest.FRAUDE_REAL)
            .filter(r -> !r.getResultadoObtenido())
            .count();
        
        // Calcular tasas
        metricas.precision = (double) metricas.correctos / metricas.totalProcesados;
        metricas.tasaDeteccionFraudes = metricas.totalFraudes > 0 
            ? (double) metricas.fraudesDetectados / metricas.totalFraudes 
            : 0.0;
        
        // Tiempos
        metricas.tiempoPromedioMs = resultados.stream()
            .mapToLong(ResultadoValidacion::getTiempoMs)
            .average()
            .orElse(0.0);
        
        metricas.tiempoMinimoMs = resultados.stream()
            .mapToLong(ResultadoValidacion::getTiempoMs)
            .min()
            .orElse(0L);
        
        metricas.tiempoMaximoMs = resultados.stream()
            .mapToLong(ResultadoValidacion::getTiempoMs)
            .max()
            .orElse(0L);
        
        return metricas;
    }
    
    /**
     * Genera reporte completo para el artículo.
     */
    private void generarReporteCompleto(MetricasValidacion metricas, List<ResultadoValidacion> resultados) {
        try {
            FileWriter writer = new FileWriter("reporte_validacion_sistema.txt");
            
            writer.write("=".repeat(70) + "\n");
            writer.write("REPORTE DE VALIDACIÓN - SISTEMA RENTA JOVEN\n");
            writer.write("Fecha: " + new Date() + "\n");
            writer.write("=".repeat(70) + "\n\n");
            
            writer.write("1. RESUMEN GENERAL\n");
            writer.write(String.format("   - Total registros procesados: %d\n", metricas.totalProcesados));
            writer.write(String.format("   - Decisiones correctas: %d (%.2f%%)\n", 
                metricas.correctos, metricas.precision * 100));
            writer.write(String.format("   - Decisiones incorrectas: %d (%.2f%%)\n\n", 
                metricas.incorrectos, (1 - metricas.precision) * 100));
            
            writer.write("2. MÉTRICAS DE PRECISIÓN\n");
            writer.write(String.format("   - Precisión global: %.2f%%\n", metricas.precision * 100));
            writer.write(String.format("   - Verdaderos Positivos (TP): %d\n", metricas.verdaderosPositivos));
            writer.write(String.format("   - Verdaderos Negativos (TN): %d\n", metricas.verdaderosNegativos));
            writer.write(String.format("   - Falsos Positivos (FP): %d\n", metricas.falsosPositivos));
            writer.write(String.format("   - Falsos Negativos (FN): %d\n\n", metricas.falsosNegativos));
            
            writer.write("3. DETECCIÓN DE FRAUDES\n");
            writer.write(String.format("   - Total fraudes en dataset: %d\n", metricas.totalFraudes));
            writer.write(String.format("   - Fraudes detectados: %d (%.2f%%)\n", 
                metricas.fraudesDetectados, metricas.tasaDeteccionFraudes * 100));
            writer.write(String.format("   - Fraudes no detectados: %d (%.2f%%)\n\n", 
                metricas.totalFraudes - metricas.fraudesDetectados,
                (1 - metricas.tasaDeteccionFraudes) * 100));
            
            writer.write("4. RENDIMIENTO\n");
            writer.write(String.format("   - Tiempo promedio: %.2f ms\n", metricas.tiempoPromedioMs));
            writer.write(String.format("   - Tiempo mínimo: %d ms\n", metricas.tiempoMinimoMs));
            writer.write(String.format("   - Tiempo máximo: %d ms\n\n", metricas.tiempoMaximoMs));
            
            writer.write("5. ANÁLISIS DE ERRORES\n");
            
            // Falsos positivos
            List<ResultadoValidacion> fps = resultados.stream()
                .filter(r -> !r.getResultadoEsperado() && r.getResultadoObtenido())
                .toList();
            
            writer.write(String.format("   Falsos Positivos (%d casos):\n", fps.size()));
            if (!fps.isEmpty()) {
                Map<CategoriaTest, Long> fpsPorCategoria = fps.stream()
                    .collect(java.util.stream.Collectors.groupingBy(
                        ResultadoValidacion::getCategoriaEsperada,
                        java.util.stream.Collectors.counting()
                    ));
                
                fpsPorCategoria.forEach((cat, count) -> {
                    try {
                        writer.write(String.format("      - %s: %d\n", cat.name(), count));
                    } catch (Exception e) {}
                });
            }
            writer.write("\n");
            
            // Falsos negativos
            List<ResultadoValidacion> fns = resultados.stream()
                .filter(r -> r.getResultadoEsperado() && !r.getResultadoObtenido())
                .toList();
            
            writer.write(String.format("   Falsos Negativos (%d casos):\n", fns.size()));
            if (!fns.isEmpty() && fns.size() <= 10) {
                for (ResultadoValidacion fn : fns) {
                    writer.write(String.format("      - Cédula: %s | Razón: %s\n", 
                        fn.getCedula(), fn.getRazonRechazo()));
                }
            }
            
            writer.write("\n" + "=".repeat(70) + "\n");
            writer.close();
            
            System.out.println("📄 Reporte guardado en: reporte_validacion_sistema.txt\n");
            
        } catch (Exception e) {
            System.err.println("Error generando reporte: " + e.getMessage());
        }
    }
    
    /**
     * Clase interna para almacenar métricas.
     */
    private static class MetricasValidacion {
        int totalProcesados;
        int correctos;
        int incorrectos;
        int verdaderosPositivos;
        int verdaderosNegativos;
        int falsosPositivos;
        int falsosNegativos;
        int totalFraudes;
        int fraudesDetectados;
        double precision;
        double tasaDeteccionFraudes;
        double tiempoPromedioMs;
        long tiempoMinimoMs;
        long tiempoMaximoMs;
    }
}