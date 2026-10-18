package edu.sistemasd.ejercicio4;

import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.*;

@Service
public class BridgeService {
    private final ExecutorService executor = Executors.newCachedThreadPool();
    private final Object stateLock = new Object();
    private volatile State state = State.idle();

    public Map<String, Object> start(int northTotal, int southTotal, int crossingMillis) {
        synchronized (stateLock) {
            if (state.running) return Map.of("started", false, "message", "Ya existe una simulación en ejecución");
            state = new State(true, "NINGUNA", 0, 0, 0, 0, northTotal, southTotal, crossingMillis,
                    new CopyOnWriteArrayList<Event>());
            Bridge bridge = new Bridge();
            for (int i = 1; i <= northTotal; i++) submit(bridge, "NORTE", i);
            for (int i = 1; i <= southTotal; i++) submit(bridge, "SUR", i);
            return Map.of("started", true, "north", northTotal, "south", southTotal);
        }
    }

    private void submit(Bridge bridge, String direction, int id) {
        executor.submit(() -> {
            boolean entered = false;
            try {
                bridge.enter(direction, id);
                entered = true;
                Thread.sleep(state.crossingMillis);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                addEvent("Vehículo " + id + " (" + direction + ") interrumpido");
            } finally {
                if (entered) bridge.leave(direction, id);
            }
        });
    }

    private final class Bridge {
        private final ReentrantLock mutex = new ReentrantLock(true);
        private final Condition changed = mutex.newCondition();

        void enter(String direction, int id) throws InterruptedException {
            mutex.lockInterruptibly();
            try {
                while (!state.direction.equals("NINGUNA") && !state.direction.equals(direction)) changed.await();
                set(s -> s.withDirection(direction).withOnBridge(s.onBridge + 1)
                        .withCount(direction, s.count(direction) + 1)
                        .add("Vehículo " + id + " entró hacia " + direction));
            } finally { mutex.unlock(); }
        }

        void leave(String direction, int id) {
            mutex.lock();
            try {
                set(s -> s.withOnBridge(s.onBridge - 1).withCount(direction, s.count(direction) - 1)
                        .withCompleted(s.completed + 1)
                        .withDirection(s.onBridge - 1 == 0 ? "NINGUNA" : direction)
                        .add("Vehículo " + id + " salió hacia " + direction));
                if (state.onBridge == 0) changed.signalAll();
                if (state.isComplete() && state.running) {
                    set(s -> s.withRunning(false).add("Cruce finalizado sin direcciones simultáneas"));
                }
            } finally { mutex.unlock(); }
        }
    }

    public Map<String, Object> status() {
        State s = state;
        return Map.of("running", s.running, "direction", s.direction, "onBridge", s.onBridge,
                "northOnBridge", s.north, "southOnBridge", s.south,
                "northTotal", s.totalNorth, "southTotal", s.totalSouth,
                "crossingMillis", s.crossingMillis, "completed", s.completed,
                "total", s.totalNorth + s.totalSouth);
    }
    public List<Event> events() { return List.copyOf(state.events); }
    private void addEvent(String m) { synchronized (stateLock) { state = state.add(m); } }
    private void set(java.util.function.UnaryOperator<State> f) { synchronized (stateLock) { state = f.apply(state); } }

    record Event(long timestamp, String message) {}
    private record State(boolean running, String direction, int onBridge, int north, int south,
                         int completed, int totalNorth, int totalSouth, int crossingMillis,
                         List<Event> events) {
        static State idle() {
            return new State(false, "NINGUNA", 0, 0, 0, 0, 0, 0, 250,
                    new CopyOnWriteArrayList<>());
        }
        int count(String d) { return d.equals("NORTE") ? north : south; }
        boolean isComplete() { return completed == totalNorth + totalSouth && onBridge == 0; }
        State add(String m) { events.add(new Event(System.currentTimeMillis(), m)); return this; }
        State withRunning(boolean v) { return new State(v, direction, onBridge, north, south, completed, totalNorth, totalSouth, crossingMillis, events); }
        State withDirection(String v) { return new State(running, v, onBridge, north, south, completed, totalNorth, totalSouth, crossingMillis, events); }
        State withOnBridge(int v) { return new State(running, direction, v, north, south, completed, totalNorth, totalSouth, crossingMillis, events); }
        State withCompleted(int v) { return new State(running, direction, onBridge, north, south, v, totalNorth, totalSouth, crossingMillis, events); }
        State withCount(String d, int v) { return d.equals("NORTE") ? new State(running, direction, onBridge, v, south, completed, totalNorth, totalSouth, crossingMillis, events) : new State(running, direction, onBridge, north, v, completed, totalNorth, totalSouth, crossingMillis, events); }
    }
}
