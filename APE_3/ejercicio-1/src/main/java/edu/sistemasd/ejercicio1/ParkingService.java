package edu.sistemasd.ejercicio1;

import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.*;

@Service
public class ParkingService {
    private static final int TOTAL = 10;
    private final Object monitor = new Object();
    private Simulation simulation;

    public Map<String, Object> start(int capacity) {
        synchronized (monitor) {
            if (simulation != null && simulation.running) return Map.of("started", false, "message", "Ya existe una simulación");
            simulation = new Simulation(capacity);
            for (int id = 1; id <= TOTAL; id++) {
                int vehicle = id;
                simulation.executor.submit(() -> park(vehicle));
            }
            simulation.gate.countDown();
            return status();
        }
    }

    private void park(int id) {
        boolean acquired = false;
        try {
            Simulation run;
            synchronized (monitor) { run = simulation; add(run, "Vehículo " + id + " llega y espera una plaza"); }
            run.gate.await();
            run.spaces.acquire();
            acquired = true;
            synchronized (monitor) {
                run.occupied++;
                run.maxConcurrent = Math.max(run.maxConcurrent, run.occupied);
                run.cars.put(id, "ESTACIONADO");
                add(run, "Vehículo " + id + " ingresa (" + run.occupied + "/" + run.capacity + ")");
            }
            Thread.sleep(300);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            synchronized (monitor) {
                if (simulation == null) return;
                if ("ESTACIONADO".equals(simulation.cars.get(id))) {
                    simulation.cars.put(id, "SALIO");
                    simulation.occupied--;
                    add(simulation, "Vehículo " + id + " sale y libera una plaza");
                } else if (!"SALIO".equals(simulation.cars.get(id))) {
                    simulation.cars.put(id, "CANCELADO");
                }
                if (acquired) simulation.spaces.release();
                simulation.completed++;
                if (simulation.completed == TOTAL) {
                    simulation.running = false;
                    add(simulation, "Simulación finalizada. Máximo simultáneo: " + simulation.maxConcurrent);
                    simulation.executor.shutdown();
                }
            }
        }
    }

    public Map<String, Object> status() {
        synchronized (monitor) {
            if (simulation == null) return Map.of("running", false, "capacity", 4, "totalVehicles", TOTAL,
                    "occupied", 0, "waiting", 0, "exited", 0, "maxConcurrent", 0);
            long waiting = simulation.cars.values().stream().filter("ESPERANDO"::equals).count();
            long exited = simulation.cars.values().stream().filter("SALIO"::equals).count();
            return Map.of("running", simulation.running, "capacity", simulation.capacity,
                    "totalVehicles", TOTAL, "occupied", simulation.occupied, "waiting", waiting,
                    "exited", exited, "maxConcurrent", simulation.maxConcurrent);
        }
    }

    public List<ParkingController.Event> events() {
        synchronized (monitor) { return simulation == null ? List.of() : List.copyOf(simulation.events); }
    }

    private void add(Simulation run, String message) {
        run.events.add(new ParkingController.Event(System.currentTimeMillis(), message));
    }

    private static final class Simulation {
        final int capacity;
        final Semaphore spaces;
        final CountDownLatch gate = new CountDownLatch(1);
        final ExecutorService executor = Executors.newFixedThreadPool(TOTAL);
        final Map<Integer, String> cars = new HashMap<>();
        final List<ParkingController.Event> events = new ArrayList<>();
        boolean running = true;
        int occupied, maxConcurrent, completed;
        Simulation(int capacity) {
            this.capacity = capacity;
            this.spaces = new Semaphore(capacity, true);
            for (int i = 1; i <= TOTAL; i++) cars.put(i, "ESPERANDO");
        }
    }
}
