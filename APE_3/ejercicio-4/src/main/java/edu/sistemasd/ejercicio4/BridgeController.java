package edu.sistemasd.ejercicio4;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/bridge")
@CrossOrigin(origins = "*")
public class BridgeController {
    private final BridgeService service;
    public BridgeController(BridgeService service) { this.service = service; }

    @PostMapping("/start")
    public ResponseEntity<Map<String, Object>> start(@RequestBody(required = false) StartRequest request) {
        int north = request == null || request.north() == null ? 5 : request.north();
        int south = request == null || request.south() == null ? 5 : request.south();
        int crossingMillis = request == null || request.crossingMillis() == null ? 250 : request.crossingMillis();
        if (north < 1 || south < 1 || north > 30 || south > 30 || crossingMillis < 50 || crossingMillis > 5000)
            return ResponseEntity.badRequest().body(Map.of("error", "Vehículos deben estar entre 1 y 30 y el tiempo de cruce entre 50 y 5000 ms"));
        return ResponseEntity.ok(service.start(north, south, crossingMillis));
    }

    @GetMapping("/status")
    public Map<String, Object> status() { return service.status(); }

    @GetMapping("/events")
    public java.util.List<BridgeService.Event> events() { return service.events(); }

    public record StartRequest(Integer north, Integer south, Integer crossingMillis) {}
}
