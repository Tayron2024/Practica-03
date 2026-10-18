package edu.sistemasd.ejercicio5;

import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.*;

@Service
public class PrintingService {
    private final Object guard = new Object();
    private final ExecutorService executor = Executors.newFixedThreadPool(3);
    private Simulation current = new Simulation(false);

    public Map<String, Object> start() {
        synchronized (guard) {
            if (current.running) return Map.of("started", false, "message", "Ya existe una simulación");
            current = new Simulation(true);
            Simulation run = current;
            executor.submit(() -> process(run, "DISEÑO", run.design, run.review));
            executor.submit(() -> process(run, "REVISIÓN", run.review, run.print));
            executor.submit(() -> process(run, "IMPRESIÓN", run.print, run.design));
            return status();
        }
    }

    private void process(Simulation run, String stage, Semaphore own, Semaphore next) {
        try {
            for (int document = 1; document <= 3; document++) {
                own.acquire();
                add(run, "Inicia " + stage + " del documento " + document);
                synchronized (guard) { run.stage = stage; run.document = document; }
                Thread.sleep(250);
                add(run, "Finaliza " + stage + " del documento " + document);
                if ("IMPRESIÓN".equals(stage)) {
                    synchronized (guard) {
                        run.finished++;
                        run.documentStates.put("doc" + document, "IMPRESO");
                        if (run.finished == 3) {
                            run.running = false;
                            run.stage = "COMPLETADO";
                            add(run, "Los 3 documentos fueron impresos correctamente");
                        }
                    }
                }
                next.release();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            synchronized (guard) { run.running = false; run.stage = "INTERRUMPIDO"; }
        }
    }

    private void add(Simulation run, String message) {
        synchronized (guard) { run.events.add(new Event(System.currentTimeMillis(), message)); }
    }

    public Map<String, Object> status() {
        synchronized (guard) {
            return Map.of("running", current.running, "stage", current.stage, "document", current.document,
                    "finishedDocuments", current.finished, "totalDocuments", 3);
        }
    }

    public List<Event> events() { synchronized (guard) { return List.copyOf(current.events); } }
    public record Event(long timestamp, String message) {}

    private static final class Simulation {
        final Semaphore design = new Semaphore(1);
        final Semaphore review = new Semaphore(0);
        final Semaphore print = new Semaphore(0);
        final List<Event> events = new ArrayList<>();
        final Map<String, String> documentStates = new LinkedHashMap<>();
        boolean running;
        String stage = "ESPERANDO";
        int document, finished;
        Simulation(boolean running) {
            this.running = running;
            for (int i = 1; i <= 3; i++) documentStates.put("doc" + i, "PENDIENTE");
        }
    }
}
