package com.subsidios.rentajoven.api.controller;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.subsidios.rentajoven.application.service.SolicitudService;
import com.subsidios.rentajoven.domain.model.DecisionResult;

/**
 * Controlador para generar datos de prueba masivos.
 * Útil para obtener métricas estadísticamente significativas.
 */
@RestController
@RequestMapping("/api/test")
public class TestDataController {
    
    @Autowired
    private SolicitudService solicitudService;
    
    /**
     * Genera N solicitudes aleatorias usando datos de los CSVs.
     * 
     * POST /api/test/generar-solicitudes?cantidad=100
     * 
     * @param cantidad Número de solicitudes a generar
     * @return Resumen del proceso
     */
    @PostMapping("/generar-solicitudes")
    public ResponseEntity<Map<String, Object>> generarSolicitudes(
            @RequestParam(defaultValue = "100") int cantidad) {
        
        long inicio = System.currentTimeMillis();
        
        try {
            // Leer cédulas y matrículas de los CSVs
            List<RegistroCompleto> registros = leerRegistrosCompletos();
            
            if (registros.isEmpty()) {
                return ResponseEntity.badRequest().body(
                    Map.of("error", "No se pudieron cargar datos de los CSVs")
                );
            }
            
            Random random = new Random();
            int aprobadas = 0;
            int rechazadas = 0;
            List<Long> tiempos = new ArrayList<>();
            Map<String, Integer> razonesRechazo = new HashMap<>();
            
            System.out.println("\n🔄 Generando " + cantidad + " solicitudes...\n");
            
            for (int i = 0; i < cantidad; i++) {
                // Seleccionar registro aleatorio
                RegistroCompleto reg = registros.get(random.nextInt(registros.size()));
                
                try {
                    // Procesar solicitud
                    DecisionResult resultado = solicitudService.procesarSolicitud(
                        reg.cedula, 
                        reg.matriculaId,
                        reg.edad
                    );
                    
                    if (resultado.getAprobada()) {
                        aprobadas++;
                    } else {
                        rechazadas++;
                        
                        // Contar razones de rechazo
                        if (resultado.getValidaciones() != null) {
                            if (!resultado.getValidaciones().getSisben().getExitosa()) {
                                razonesRechazo.merge("SISBEN", 1, Integer::sum);
                            }
                            if (!resultado.getValidaciones().getTituloProfesional().getExitosa()) {
                                razonesRechazo.merge("TITULO_PROFESIONAL", 1, Integer::sum);
                            }
                            if (!resultado.getValidaciones().getMatricula().getExitosa()) {
                                razonesRechazo.merge("MATRICULA", 1, Integer::sum);
                            }
                        }
                    }
                    
                    tiempos.add(resultado.getTiempoTotalMs());
                    
                    // Mostrar progreso cada 50 solicitudes
                    if ((i + 1) % 50 == 0) {
                        System.out.println(String.format("✓ Procesadas: %d/%d (%.1f%%)", 
                            i + 1, cantidad, ((i + 1) * 100.0 / cantidad)));
                    }
                    
                } catch (Exception e) {
                    System.err.println("⚠️  Error procesando solicitud " + (i + 1) + ": " + e.getMessage());
                }
            }
            
            long fin = System.currentTimeMillis();
            long tiempoTotal = fin - inicio;
            
            // Calcular estadísticas
            double tiempoPromedio = tiempos.stream()
                    .mapToLong(Long::longValue)
                    .average()
                    .orElse(0.0);
            
            long tiempoMin = tiempos.stream()
                    .mapToLong(Long::longValue)
                    .min()
                    .orElse(0L);
            
            long tiempoMax = tiempos.stream()
                    .mapToLong(Long::longValue)
                    .max()
                    .orElse(0L);
            
            System.out.println("\n✅ Proceso completado!\n");
            
            Map<String, Object> resultado = new HashMap<>();
            resultado.put("solicitudesGeneradas", cantidad);
            resultado.put("aprobadas", aprobadas);
            resultado.put("rechazadas", rechazadas);
            resultado.put("tasaAprobacion", cantidad > 0 ? (double) aprobadas / cantidad : 0.0);
            resultado.put("tasaRechazo", cantidad > 0 ? (double) rechazadas / cantidad : 0.0);
            resultado.put("tiempoPromedioMs", tiempoPromedio);
            resultado.put("tiempoMinimoMs", tiempoMin);
            resultado.put("tiempoMaximoMs", tiempoMax);
            resultado.put("tiempoTotalProcesoMs", tiempoTotal);
            resultado.put("tiempoTotalProcesoSegundos", tiempoTotal / 1000.0);
            resultado.put("razonesRechazo", razonesRechazo);
            
            return ResponseEntity.ok(resultado);
            
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(
                Map.of("error", e.getMessage())
            );
        }
    }
    
    /**
     * Lee registros completos del dataset maestro.
     */
    private List<RegistroCompleto> leerRegistrosCompletos() {
        List<RegistroCompleto> registros = new ArrayList<>();
        
        try {
            ClassPathResource resource = new ClassPathResource("data/dataset_maestro.csv");
            BufferedReader reader = new BufferedReader(
                new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8)
            );
            
            // Leer header
            String headerLine = reader.readLine();
            if (headerLine.startsWith("\uFEFF")) {
                headerLine = headerLine.substring(1);
            }
            
            String[] headers = headerLine.split(",");
            Map<String, Integer> headerIndex = new HashMap<>();
            for (int i = 0; i < headers.length; i++) {
                headerIndex.put(headers[i].trim(), i);
            }
            
            String line;
            int count = 0;
            
            while ((line = reader.readLine()) != null && count < 5000) {
                String[] valores = line.split(",");
                
                try {
                    // Extraer solo registros con matrícula vigente
                    String matriculaVigente = valores[headerIndex.get("matricula_vigente")].trim();
                    
                    if ("True".equalsIgnoreCase(matriculaVigente)) {
                        String cedula = valores[headerIndex.get("cedula")].trim();
                        String edadStr = valores[headerIndex.get("edad")].trim();
                        
                        // Buscar matrícula en MEN por cédula
                        String matriculaId = buscarMatriculaPorCedula(cedula);
                        
                        if (matriculaId != null) {
                            RegistroCompleto reg = new RegistroCompleto();
                            reg.cedula = cedula;
                            reg.matriculaId = matriculaId;
                            reg.edad = Integer.parseInt(edadStr);
                            
                            registros.add(reg);
                            count++;
                        }
                    }
                } catch (Exception e) {
                    // Ignorar registros con errores
                }
            }
            
            reader.close();
            System.out.println("📋 Cargados " + registros.size() + " registros completos del dataset maestro");
            
        } catch (Exception e) {
            System.err.println("❌ Error leyendo dataset maestro: " + e.getMessage());
        }
        
        return registros;
    }
    
    /**
     * Busca matrícula por cédula en el CSV de MEN.
     */
    private String buscarMatriculaPorCedula(String cedula) {
        try {
            ClassPathResource resource = new ClassPathResource("data/men_simulator.csv");
            BufferedReader reader = new BufferedReader(
                new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8)
            );
            
            reader.readLine(); // Skip header
            String line;
            
            while ((line = reader.readLine()) != null) {
                String[] campos = line.split(",");
                if (campos.length >= 2 && campos[1].trim().equals(cedula)) {
                    reader.close();
                    return campos[0].trim(); // matricula_id
                }
            }
            
            reader.close();
        } catch (Exception e) {
            // Ignorar
        }
        
        return null;
    }
    
    /**
     * Clase interna para almacenar registros completos.
     */
    private static class RegistroCompleto {
        String cedula;
        String matriculaId;
        Integer edad;
    }
}