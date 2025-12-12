package com.subsidios.rentajoven.infrastructure.external;

import com.subsidios.rentajoven.domain.enums.CategoriaTest;
import com.subsidios.rentajoven.domain.model.RegistroTest;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Carga y clasifica registros del dataset maestro para pruebas.
 */
@Component
public class DatasetTestLoader {
    
    /**
     * Carga registros del dataset maestro y los clasifica.
     */
    public List<RegistroTest> cargarRegistros(int limit) throws Exception {
        System.out.println("\n📂 Cargando dataset maestro para pruebas...");
        
        List<RegistroTest> registros = new ArrayList<>();
        
        ClassPathResource resource = new ClassPathResource("data/dataset_maestro.csv");
        BufferedReader reader = new BufferedReader(
            new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8)
        );
        
        // Leer y remover BOM si existe
        reader.mark(1);
        int firstChar = reader.read();
        if (firstChar != 0xFEFF) {
            reader.reset();
        }
        
        CSVParser parser = CSVFormat.DEFAULT
                .withFirstRecordAsHeader()
                .withTrim()
                .withIgnoreEmptyLines()
                .parse(reader);
        
        int count = 0;
        int cargados = 0;
        
        for (CSVRecord record : parser) {
            if (count >= limit) break;
            count++;
            
            try {
                String cedula = record.get("cedula").trim();
                String edadStr = record.get("edad").trim();
                String sisbenNivel = record.get("sisben_nivel").trim();
                String sisbenPuntajeStr = record.get("sisben_puntaje").trim();
                String tituloSniesStr = record.get("titulo_snies").trim();
                String matriculaVigenteStr = record.get("matricula_vigente").trim();
                String estadoMatricula = record.get("estado_matricula").trim();
                String esFraudeStr = record.get("es_fraude_real").trim();
                
                // Parsear datos
                Integer edad = Integer.parseInt(edadStr);
                Double sisbenPuntaje = Double.parseDouble(sisbenPuntajeStr);
                Boolean tituloSnies = "True".equalsIgnoreCase(tituloSniesStr);
                Boolean matriculaVigente = "True".equalsIgnoreCase(matriculaVigenteStr);
                Boolean esFraude = "True".equalsIgnoreCase(esFraudeStr);
                
                // Buscar matrícula en MEN
                String matriculaId = buscarMatriculaPorCedula(cedula);
                
                // Solo procesar si tiene matrícula en MEN
                if (matriculaId != null) {
                    RegistroTest registro = RegistroTest.builder()
                            .cedula(cedula)
                            .edad(edad)
                            .departamento(record.get("departamento").trim())
                            .municipio(record.get("municipio").trim())
                            .sisbenNivel(sisbenNivel)
                            .sisbenPuntaje(sisbenPuntaje)
                            .tituloSnies(tituloSnies)
                            .programaTitulo(record.get("programa_titulo").trim())
                            .matriculaVigente(matriculaVigente)
                            .estadoMatricula(estadoMatricula)
                            .institucionMatricula(record.get("institucion_matricula").trim())
                            .programaMatricula(record.get("programa_matricula").trim())
                            .esFraudeReal(esFraude)
                            .matriculaId(matriculaId)
                            .build();
                    
                    // Clasificar registro
                    registro.setCategoria(clasificarRegistro(registro));
                    
                    registros.add(registro);
                    cargados++;
                }
                
            } catch (Exception e) {
                // Ignorar registros con errores
            }
        }
        
        parser.close();
        reader.close();
        
        System.out.println("   ✅ " + cargados + " registros cargados y clasificados\n");
        
        // Mostrar distribución
        mostrarDistribucion(registros);
        
        return registros;
    }
    
    /**
     * Clasifica un registro en una categoría de prueba.
     */
    private CategoriaTest clasificarRegistro(RegistroTest reg) {
        // Si es fraude real
        if (reg.getEsFraudeReal()) {
            return CategoriaTest.FRAUDE_REAL;
        }
        
        // Si edad fuera de rango (14-28)
        if (reg.getEdad() < 14 || reg.getEdad() > 28) {
            return CategoriaTest.RECHAZO_EDAD;
        }
        
        // Si nivel SISBEN D
        if ("D".equals(reg.getSisbenNivel())) {
            return CategoriaTest.RECHAZO_SISBEN;
        }
        
        // Si tiene título profesional
        if (reg.getTituloSnies()) {
            return CategoriaTest.RECHAZO_TITULO;
        }
        
        // Si matrícula no vigente
        if (!reg.getMatriculaVigente() || !"VIGENTE".equalsIgnoreCase(reg.getEstadoMatricula())) {
            return CategoriaTest.RECHAZO_MATRICULA;
        }
        
        // Si pasa todos los filtros, debería ser aprobado
        return CategoriaTest.LEGITIMO_DEBERIA_APROBAR;
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
     * Muestra distribución de categorías.
     */
    private void mostrarDistribucion(List<RegistroTest> registros) {
        Map<CategoriaTest, Integer> distribucion = new HashMap<>();
        
        for (RegistroTest reg : registros) {
            distribucion.merge(reg.getCategoria(), 1, Integer::sum);
        }
        
        System.out.println("   📊 Distribución de categorías:");
        for (CategoriaTest cat : CategoriaTest.values()) {
            int count = distribucion.getOrDefault(cat, 0);
            if (count > 0) {
                System.out.println(String.format("      ├─ %s: %d", cat.name(), count));
            }
        }
        System.out.println();
    }
}