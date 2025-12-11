package com.subsidios.rentajoven.infrastructure.external;

import com.subsidios.rentajoven.domain.model.SNIESResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * Simulador de la API del SNIES.
 * Retorna información completa del título profesional.
 */
@Service
public class SNIESSimulator {
    
    @Value("${simulador.snies.latencia.min:300}")
    private int latenciaMin;
    
    @Value("${simulador.snies.latencia.max:500}")
    private int latenciaMax;
    
    private final Random random = new Random();
    
    // Base de datos simulada: cedula -> SNIESResponse
    private final Map<String, SNIESResponse> baseDatos = new HashMap<>();
    
    /**
     * Carga datos desde el CSV.
     */
    public void cargarDatos(Map<String, SNIESResponse> datos) {
        baseDatos.clear();
        baseDatos.putAll(datos);
        System.out.println("✅ SNIES Simulator cargado con " + baseDatos.size() + " títulos profesionales");
    }
    
    /**
     * Consulta información completa del título.
     */
    public SNIESResponse consultar(String cedula) {
        simularLatencia();
        
        SNIESResponse response = baseDatos.get(cedula);
        
        if (response == null) {
            return SNIESResponse.builder()
                    .tieneTitulo(false)
                    .build();
        }
        
        return response;
    }
    
    /**
     * Método legacy (mantener compatibilidad).
     */
    public boolean tieneTituloProfesional(String cedula) {
        SNIESResponse response = consultar(cedula);
        return response.isTieneTitulo();
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