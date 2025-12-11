package com.subsidios.rentajoven.infrastructure.external;

import com.subsidios.rentajoven.domain.enums.NivelSISBEN;
import com.subsidios.rentajoven.domain.model.SISBENResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * Simulador de la API del SISBÉN.
 * Retorna información completa del ciudadano.
 */
@Service
public class SISBENSimulator {
    
    @Value("${simulador.sisben.latencia.min:250}")
    private int latenciaMin;
    
    @Value("${simulador.sisben.latencia.max:400}")
    private int latenciaMax;
    
    private final Random random = new Random();
    
    // Base de datos simulada
    private final Map<String, SISBENResponse> baseDatos = new HashMap<>();
    
    /**
     * Carga datos desde el CSV.
     */
    public void cargarDatos(Map<String, SISBENResponse> datos) {
        baseDatos.clear();
        baseDatos.putAll(datos);
        System.out.println("✅ SISBEN Simulator cargado con " + baseDatos.size() + " registros");
    }
    
    /**
     * Consulta información completa del SISBÉN.
     */
    public SISBENResponse consultar(String cedula) {
        simularLatencia();
        
        SISBENResponse response = baseDatos.get(cedula);
        
        if (response == null) {
            // No encontrado
            return SISBENResponse.builder()
                    .encontrado(false)
                    .build();
        }
        
        return response;
    }
    
    /**
     * Método legacy (mantener compatibilidad).
     */
    public NivelSISBEN consultarNivel(String cedula) {
        SISBENResponse response = consultar(cedula);
        return response.isEncontrado() ? response.getNivel() : NivelSISBEN.D;
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