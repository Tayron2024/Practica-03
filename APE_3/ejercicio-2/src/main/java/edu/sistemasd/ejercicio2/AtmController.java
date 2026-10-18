package edu.sistemasd.ejercicio2;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/atm")
@CrossOrigin(origins = "*")
public class AtmController {
    private final AtmService service;

    public AtmController(AtmService service) {
        this.service = service;
    }

    @PostMapping("/start")
    public ResponseEntity<Map<String, Object>> start() {
        return ResponseEntity.ok(service.start());
    }

    @GetMapping("/status")
    public Map<String, Object> status() {
        return service.status();
    }

    @GetMapping("/events")
    public List<AtmService.Event> events() {
        return service.events();
    }
}
