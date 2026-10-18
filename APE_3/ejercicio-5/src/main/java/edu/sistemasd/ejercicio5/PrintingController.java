package edu.sistemasd.ejercicio5;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/printing")
@CrossOrigin(origins = "*")
public class PrintingController {
    private final PrintingService service;
    public PrintingController(PrintingService service) { this.service = service; }
    @PostMapping("/start") public ResponseEntity<Map<String, Object>> start() { return ResponseEntity.ok(service.start()); }
    @GetMapping("/status") public Map<String, Object> status() { return service.status(); }
    @GetMapping("/events") public List<PrintingService.Event> events() { return service.events(); }
}
