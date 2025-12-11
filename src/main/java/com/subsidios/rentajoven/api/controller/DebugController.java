package com.subsidios.rentajoven.api.controller;

import com.subsidios.rentajoven.infrastructure.external.MENSimulator;
import com.subsidios.rentajoven.infrastructure.external.SISBENSimulator;
import com.subsidios.rentajoven.infrastructure.external.SNIESSimulator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Controlador de debugging - Solo para desarrollo.
 */
@RestController
@RequestMapping("/api/debug")
public class DebugController {

    @Autowired
    private SISBENSimulator sisbenSimulator;

    @Autowired
    private SNIESSimulator sniesSimulator;

    @Autowired
    private MENSimulator menSimulator;

    /**
     * Ver tamaño de las bases de datos cargadas.
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("sisben_registros", sisbenSimulator.getTamanioBaseDatos());
        stats.put("snies_registros", sniesSimulator.getTamanioBaseDatos());
        stats.put("men_registros", menSimulator.getTamanioBaseDatos());

        return ResponseEntity.ok(stats);
    }

    /**
     * Ver primeras N matrículas del CSV.
     */
    @GetMapping("/matriculas-sample")
    public ResponseEntity<List<String>> getMatriculasSample(
            @RequestParam(defaultValue = "20") int limit) {

        List<String> matriculas = new ArrayList<>();

        try {
            ClassPathResource resource = new ClassPathResource("data/men_simulator.csv");
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8));

            String line = reader.readLine(); // Skip header
            int count = 0;

            while ((line = reader.readLine()) != null && count < limit) {
                String[] campos = line.split(",");
                if (campos.length > 0) {
                    matriculas.add(campos[0].trim());
                    count++;
                }
            }

            reader.close();

        } catch (Exception e) {
            System.err.println("Error leyendo matrículas: " + e.getMessage());
        }

        return ResponseEntity.ok(matriculas);
    }

    /**
     * Ver primeras N cédulas del CSV SISBEN.
     */
    @GetMapping("/cedulas-sample")
    public ResponseEntity<Map<String, Object>> getCedulasSample(
            @RequestParam(defaultValue = "20") int limit) {

        List<Map<String, String>> cedulas = new ArrayList<>();

        try {
            ClassPathResource resource = new ClassPathResource("data/sisben_simulator.csv");
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8));

            String line = reader.readLine(); // Skip header
            int count = 0;

            while ((line = reader.readLine()) != null && count < limit) {
                String[] campos = line.split(",");
                if (campos.length >= 3) {
                    Map<String, String> item = new HashMap<>();
                    item.put("cedula", campos[1].trim());
                    item.put("nivel", campos[2].trim());
                    cedulas.add(item);
                    count++;
                }
            }

            reader.close();

        } catch (Exception e) {
            System.err.println("Error leyendo cédulas: " + e.getMessage());
        }

        Map<String, Object> response = new HashMap<>();
        response.put("total", cedulas.size());
        response.put("datos", cedulas);

        return ResponseEntity.ok(response);
    }

    /**
     * Buscar matrícula por ID exacto.
     */
    @GetMapping("/buscar-matricula")
    public ResponseEntity<Map<String, Object>> buscarMatricula(
            @RequestParam String matriculaId) {

        Map<String, Object> response = new HashMap<>();

        try {
            var resultado = menSimulator.consultar(matriculaId);

            response.put("matriculaId", matriculaId);
            response.put("encontrada", resultado.isEncontrada());

            if (resultado.isEncontrada()) {
                response.put("cedula", resultado.getCedula());
                response.put("institucion", resultado.getInstitucion());
                response.put("programa", resultado.getPrograma());
                response.put("estado", resultado.getEstado());
                response.put("intensidadHoraria", resultado.getIntensidadHoraria());
            }

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.put("error", e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * Buscar cédula en SISBEN.
     */
    @GetMapping("/buscar-cedula-sisben")
    public ResponseEntity<Map<String, Object>> buscarCedulaSisben(
            @RequestParam String cedula) {

        Map<String, Object> response = new HashMap<>();

        try {
            var resultado = sisbenSimulator.consultar(cedula);

            response.put("cedula", cedula);
            response.put("encontrada", resultado.isEncontrado());

            if (resultado.isEncontrado()) {
                response.put("nivel", resultado.getNivel().name());
                response.put("puntaje", resultado.getPuntaje());
                response.put("departamento", resultado.getDepartamento());
                response.put("municipio", resultado.getMunicipio());
            }

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.put("error", e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * Buscar registros del dataset_maestro que tengan TODO:
     * - Cédula en SISBEN
     * - Matrícula en MEN
     * - Sin título en SNIES
     * - Edad 14-28
     */
    @GetMapping("/buscar-registros-completos")
    public ResponseEntity<List<Map<String, Object>>> buscarRegistrosCompletos(
            @RequestParam(defaultValue = "10") int limit) {

        List<Map<String, Object>> registros = new ArrayList<>();

        try {
            // Leer primeras N matrículas del MEN
            ClassPathResource menResource = new ClassPathResource("data/men_simulator.csv");
            BufferedReader menReader = new BufferedReader(
                    new InputStreamReader(menResource.getInputStream(), StandardCharsets.UTF_8));

            String line = menReader.readLine(); // Skip header
            int count = 0;

            while ((line = menReader.readLine()) != null && count < limit) {
                String[] campos = line.split(",");

                if (campos.length >= 7) {
                    String matriculaId = campos[0].trim();
                    String cedula = campos[1].trim();
                    String estado = campos[4].trim();

                    // Solo matrículas VIGENTES
                    if (!"VIGENTE".equalsIgnoreCase(estado)) {
                        continue;
                    }

                    // Verificar en SISBEN
                    var sisben = sisbenSimulator.consultar(cedula);
                    if (!sisben.isEncontrado()) {
                        continue;
                    }

                    // Solo niveles elegibles (A, B, C)
                    if (!sisben.getNivel().isElegible()) {
                        continue;
                    }

                    // Verificar en SNIES (debe NO tener título)
                    var snies = sniesSimulator.consultar(cedula);
                    if (snies.isTieneTitulo()) {
                        continue;
                    }

                    // Verificar matrícula en MEN
                    var men = menSimulator.consultar(matriculaId);
                    if (!men.isEncontrada()) {
                        continue;
                    }

                    // ✅ REGISTRO COMPLETO Y ELEGIBLE
                    Map<String, Object> registro = new HashMap<>();
                    registro.put("cedula", cedula);
                    registro.put("matriculaId", matriculaId);
                    registro.put("sisben_nivel", sisben.getNivel().name());
                    registro.put("sisben_puntaje", sisben.getPuntaje());
                    registro.put("tiene_titulo", false);
                    registro.put("matricula_estado", men.getEstado());
                    registro.put("intensidad_horaria", men.getIntensidadHoraria());
                    registro.put("institucion", men.getInstitucion());
                    registro.put("programa", men.getPrograma());
                    registro.put("deberia_aprobar", true);

                    registros.add(registro);
                    count++;
                }
            }

            menReader.close();

            return ResponseEntity.ok(registros);

        } catch (Exception e) {
            System.err.println("Error buscando registros: " + e.getMessage());
            return ResponseEntity.status(500).body(new ArrayList<>());
        }
    }
}