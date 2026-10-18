package edu.sistemasd.ejercicio1;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/parking")
@CrossOrigin(origins = "*")
public class ParkingController {
    private final ParkingService service;

    public ParkingController(ParkingService service) { this.service = service; }

    @PostMapping("/start")
    public ResponseEntity<Map<String, Object>> start(@RequestBody(required = false) StartRequest request) {
        int capacity = request == null || request.capacity() == null ? 4 : request.capacity();
        if (capacity != 1 && capacity != 4 && capacity != 10) {
            return ResponseEntity.badRequest().body(Map.of("error", "La capacidad debe ser 1, 4 o 10"));
        }
        return ResponseEntity.ok(service.start(capacity));
    }

    @GetMapping("/status")
    public Map<String, Object> status() { return service.status(); }

    @GetMapping("/events")
    public List<Event> events() { return service.events(); }

    public record StartRequest(Integer capacity) {}
    public record Event(long timestamp, String message) {}
}
