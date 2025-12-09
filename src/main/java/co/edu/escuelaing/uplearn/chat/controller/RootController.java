package co.edu.escuelaing.uplearn.chat.controller;

import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controlador raíz para verificar el estado del servicio.
 */
@RestController
class RootController {
    
    /**
     * Punto de entrada raíz que devuelve el estado del servicio.
     * 
     * @return Mapa con el estado del servicio
     */
    @GetMapping("/")
    public Map<String, String> root() {
        return Map.of("service", "chat-service", "status", "OK");
    }
}
