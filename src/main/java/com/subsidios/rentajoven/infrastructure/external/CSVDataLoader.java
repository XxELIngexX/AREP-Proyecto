package com.subsidios.rentajoven.infrastructure.external;

import com.subsidios.rentajoven.domain.enums.NivelSISBEN;
import com.subsidios.rentajoven.domain.model.MENResponse;
import com.subsidios.rentajoven.domain.model.SISBENResponse;
import com.subsidios.rentajoven.domain.model.SNIESResponse;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * Cargador de datos CSV al iniciar la aplicación.
 */
@Component
public class CSVDataLoader implements CommandLineRunner {
    
    @Autowired
    private SISBENSimulator sisbenSimulator;
    
    @Autowired
    private SNIESSimulator sniesSimulator;
    
    @Autowired
    private MENSimulator menSimulator;
    
    @Override
    public void run(String... args) throws Exception {
        System.out.println("\n" + "=".repeat(70));
        System.out.println("🔄 INICIANDO CARGA DE DATOS DESDE CSVs");
        System.out.println("=".repeat(70) + "\n");
        
        long inicio = System.currentTimeMillis();
        
        cargarSISBEN();
        cargarSNIES();
        cargarMEN();
        
        long fin = System.currentTimeMillis();
        
        System.out.println("\n" + "=".repeat(70));
        System.out.println("✅ CARGA COMPLETADA EN " + (fin - inicio) + "ms");
        System.out.println("=".repeat(70) + "\n");
    }
    
    /**
     * Carga datos del SISBEN desde CSV.
     */
    private void cargarSISBEN() {
        try {
            System.out.println("📊 Cargando SISBEN...");
            
            Map<String, SISBENResponse> datos = new HashMap<>();
            
            ClassPathResource resource = new ClassPathResource("data/sisben_simulator.csv");
            BufferedReader reader = new BufferedReader(
                new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8)
            );
            
            CSVParser parser = CSVFormat.DEFAULT
                    .withFirstRecordAsHeader()
                    .withTrim()
                    .withIgnoreEmptyLines()
                    .parse(reader);
            
            int count = 0;
            for (CSVRecord record : parser) {
                String cedula = record.get("cedula").trim();
                String nivelStr = record.get("sisben_nivel").trim();
                String puntajeStr = record.get("sisben_puntaje").trim();
                String departamento = record.get("departamento").trim();
                String municipio = record.get("municipio").trim();
                
                NivelSISBEN nivel = convertirNivelSISBEN(nivelStr);
                Double puntaje = Double.parseDouble(puntajeStr);
                
                SISBENResponse response = SISBENResponse.builder()
                        .nivel(nivel)
                        .puntaje(puntaje)
                        .departamento(departamento)
                        .municipio(municipio)
                        .encontrado(true)
                        .build();
                
                datos.put(cedula, response);
                count++;
            }
            
            sisbenSimulator.cargarDatos(datos);
            System.out.println("   ✅ " + count + " registros cargados\n");
            
            parser.close();
            reader.close();
            
        } catch (Exception e) {
            System.err.println("   ❌ Error cargando SISBEN: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Carga datos del SNIES desde CSV.
     */
    private void cargarSNIES() {
        try {
            System.out.println("🎓 Cargando SNIES...");
            
            Map<String, SNIESResponse> datos = new HashMap<>();
            
            ClassPathResource resource = new ClassPathResource("data/snies_simulator.csv");
            BufferedReader reader = new BufferedReader(
                new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8)
            );
            
            CSVParser parser = CSVFormat.DEFAULT
                    .withFirstRecordAsHeader()
                    .withTrim()
                    .withIgnoreEmptyLines()
                    .parse(reader);
            
            int count = 0;
            for (CSVRecord record : parser) {
                String cedula = record.get("cedula").trim();
                String programa = record.get("programa").trim();
                String institucion = record.get("institucion").trim();
                String tipoTitulo = record.get("tipo_titulo").trim();
                
                SNIESResponse response = SNIESResponse.builder()
                        .tieneTitulo(true)
                        .programa(programa)
                        .institucion(institucion)
                        .tipoTitulo(tipoTitulo)
                        .build();
                
                datos.put(cedula, response);
                count++;
            }
            
            sniesSimulator.cargarDatos(datos);
            System.out.println("   ✅ " + count + " registros cargados\n");
            
            parser.close();
            reader.close();
            
        } catch (Exception e) {
            System.err.println("   ❌ Error cargando SNIES: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
 * Carga datos del MEN desde CSV - CON MANEJO DE BOM.
 */
private void cargarMEN() {
    try {
        System.out.println("📚 Cargando MEN...");
        
        Map<String, MENResponse> datos = new HashMap<>();
        
        ClassPathResource resource = new ClassPathResource("data/men_simulator.csv");
        
        // ⭐ CAMBIO CLAVE: Usar InputStreamReader con BOM handling
        BufferedReader reader = new BufferedReader(
            new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8)
        );
        
        // Leer y descartar BOM si existe
        reader.mark(1);
        int firstChar = reader.read();
        if (firstChar != 0xFEFF) { // BOM character
            reader.reset();
        }
        
        CSVParser parser = CSVFormat.DEFAULT
                .builder()
                .setHeader()
                .setSkipHeaderRecord(true)
                .setTrim(true)
                .setIgnoreEmptyLines(true)
                .setIgnoreSurroundingSpaces(true)
                .build()
                .parse(reader);
        
        int count = 0;
        int errores = 0;
        System.out.println("   📋 Procesando registros...");
        
        for (CSVRecord record : parser) {
            try {
                // Obtener headers disponibles
                Map<String, Integer> headerMap = parser.getHeaderMap();
                
                // Buscar el header correcto (con o sin BOM)
                String matriculaHeader = null;
                for (String key : headerMap.keySet()) {
                    if (key.endsWith("matricula_id")) {
                        matriculaHeader = key;
                        break;
                    }
                }
                
                if (matriculaHeader == null) {
                    if (errores == 0) {
                        System.err.println("      ⚠️  No se encontró columna 'matricula_id'");
                        System.err.println("      📋 Headers disponibles: " + headerMap.keySet());
                    }
                    errores++;
                    continue;
                }
                
                String matriculaId = record.get(matriculaHeader).trim();
                String cedula = record.get("cedula").trim();
                String institucion = record.get("institucion").trim();
                String programa = record.get("programa").trim();
                String estado = record.get("estado").trim();
                String intensidadStr = record.get("intensidad_horaria").trim();
                
                Integer intensidad = Integer.parseInt(intensidadStr);
                
                MENResponse response = MENResponse.builder()
                        .matriculaId(matriculaId)
                        .cedula(cedula)
                        .institucion(institucion)
                        .programa(programa)
                        .estado(estado)
                        .intensidadHoraria(intensidad)
                        .encontrada(true)
                        .build();
                
                datos.put(matriculaId, response);
                count++;
                
                // Mostrar primeras 5 para debug
                if (count <= 5) {
                    System.out.println(String.format(
                        "      %d. ID: [%s] | Cédula: %s | Estado: %s | %dh",
                        count, matriculaId, cedula, estado, intensidad
                    ));
                }
                
            } catch (Exception e) {
                errores++;
                if (errores <= 3) {
                    System.err.println("      ⚠️  Error en registro " + (count + 1) + ": " + e.getMessage());
                }
            }
        }
        
        System.out.println("   💾 Cargando " + datos.size() + " matrículas en memoria...");
        menSimulator.cargarDatos(datos);
        System.out.println("   ✅ " + count + " registros procesados (" + errores + " errores)");
        System.out.println("   ✅ " + menSimulator.getTamanioBaseDatos() + " matrículas disponibles en simulador\n");
        
        // VERIFICACIÓN CRÍTICA
        if (menSimulator.getTamanioBaseDatos() == 0) {
            System.err.println("   ❌❌❌ PROBLEMA CRÍTICO: MENSimulator está vacío!");
        } else {
            // Verificar que MAT-00000001 existe
            var test = menSimulator.consultar("MAT-00000001");
            if (test.isEncontrada()) {
                System.out.println("   ✅ Verificación OK: MAT-00000001 encontrada");
            } else {
                System.err.println("   ⚠️  Verificación FALLA: MAT-00000001 NO encontrada");
            }
        }
        
        parser.close();
        reader.close();
        
    } catch (Exception e) {
        System.err.println("   ❌ Error cargando MEN: " + e.getMessage());
        e.printStackTrace();
    }
}
    
    private NivelSISBEN convertirNivelSISBEN(String nivel) {
        switch (nivel.toUpperCase().trim()) {
            case "A": return NivelSISBEN.A;
            case "B": return NivelSISBEN.B;
            case "C": return NivelSISBEN.C;
            case "D": return NivelSISBEN.D;
            default: return NivelSISBEN.D;
        }
    }
}