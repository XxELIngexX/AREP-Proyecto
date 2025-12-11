package com.subsidios.rentajoven.infrastructure.external;

import com.subsidios.rentajoven.domain.model.MENResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * Simulador de la API del MEN.
 * Retorna información completa de la matrícula.
 */
@Service
public class MENSimulator {
    
    @Value("${simulador.men.latencia.min:280}")
    private int latenciaMin;
    
    @Value("${simulador.men.latencia.max:450}")
    private int latenciaMax;
    
    private final Random random = new Random();
    
    // Base de datos simulada
    private final Map<String, MENResponse> baseDatos = new HashMap<>();
    
    /**
     * Carga datos desde el CSV.
     */
    public void cargarDatos(Map<String, MENResponse> datos) {
        baseDatos.clear();
        baseDatos.putAll(datos);
        System.out.println("✅ MEN Simulator cargado con " + baseDatos.size() + " matrículas");
    }
    
    /**
     * Consulta información completa de la matrícula.
     */
    public MENResponse consultar(String matriculaId) {
        simularLatencia();
        
        MENResponse response = baseDatos.get(matriculaId);
        
        if (response == null) {
            return MENResponse.builder()
                    .encontrada(false)
                    .build();
        }
        
        return response;
    }
    
    /**
     * Método legacy (mantener compatibilidad).
     */
    public boolean tieneMatriculaVigente(String matriculaId) {
        MENResponse response = consultar(matriculaId);
        return response.isEncontrada() && "VIGENTE".equalsIgnoreCase(response.getEstado());
    }
    
    private void simularLatencia() {
        try {
            int latencia = latenciaMin + random.nextInt(latenciaMax - latenciaMin);
            Thread.sleep(latencia);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
    
    public int getTamanioBaseDatos() {
        return baseDatos.size();
    }
}