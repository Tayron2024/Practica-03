package edu.sistemasd.ejercicio3;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/production")
@CrossOrigin(origins = "*")
public class SimulationController {
    private final ProductionService service;

    public SimulationController(ProductionService service) {
        this.service = service;
    }

    @PostMapping("/start")
    public ResponseEntity<Map<String, Object>> start(@RequestBody(required = false) StartRequest request) {
        int producers = request == null || request.producers() == null ? 2 : request.producers();
        int consumers = request == null || request.consumers() == null ? 2 : request.consumers();
        int pieces = request == null || request.piecesPerWorker() == null ? 5 : request.piecesPerWorker();
        if (producers < 1 || producers > 10 || consumers < 1 || consumers > 10 || pieces < 1 || pieces > 50) {
            return ResponseEntity.badRequest().body(Map.of("error", "Valores fuera de rango"));
        }
        return ResponseEntity.ok(service.start(producers, consumers, pieces));
    }

    @GetMapping("/status")
    public Map<String, Object> status() {
        return service.status();
    }

    @GetMapping("/events")
    public List<ProductionService.Event> events() {
        return service.events();
    }

    public record StartRequest(@Min(1) @Max(10) Integer producers,
                               @Min(1) @Max(10) Integer consumers,
                               @Min(1) @Max(50) Integer piecesPerWorker) {}
}
