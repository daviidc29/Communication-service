package co.edu.escuelaing.uplearn.chat.controller;

import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
class RootController {
  @GetMapping("/")
  public Map<String,String> root() {
    return Map.of("service","chat-service","status","OK");
  }
}
