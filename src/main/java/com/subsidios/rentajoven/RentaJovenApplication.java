package com.subsidios.rentajoven;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Aplicación principal del sistema de verificación automatizada
 * de subsidios Renta Joven.
 * 
 * Este prototipo demuestra un motor de decisión automatizado
 * para la asignación de subsidios gubernamentales en Colombia.
 * 
 * @author Cesar Amaya Gomez
 * @version 1.0.0
 */
@SpringBootApplication
public class RentaJovenApplication {

    public static void main(String[] args) {
        SpringApplication.run(RentaJovenApplication.class, args);
        System.out.println("\n" +
                "========================================\n" +
                "  SISTEMA RENTA JOVEN INICIADO\n" +
                "========================================\n" +
                "  API: http://localhost:8080/api\n" +
                "  H2 Console: http://localhost:8080/h2-console\n" +
                "  JDBC URL: jdbc:h2:mem:rentajoven\n" +
                "========================================\n");
    }
}