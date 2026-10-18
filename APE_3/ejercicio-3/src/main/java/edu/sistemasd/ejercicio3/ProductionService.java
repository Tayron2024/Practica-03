package edu.sistemasd.ejercicio3;

import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class ProductionService {
    private static final int CAPACITY = 5;
    private final ExecutorService executor = Executors.newCachedThreadPool();
    private final Object stateLock = new Object();
    private volatile RunState state = RunState.idle();

    public Map<String, Object> start(int producerCount, int consumerCount, int piecesPerWorker) {
        synchronized (stateLock) {
            if (state.running()) return Map.of("started", false, "message", "Ya existe una simulación en ejecución");
            int total = producerCount * piecesPerWorker;
            state = new RunState(true, producerCount, consumerCount, piecesPerWorker,
                    0, 0, 0, total, new CopyOnWriteArrayList<>(), Instant.now());
            LinkedBlockingQueue<Integer> warehouse = new LinkedBlockingQueue<>(CAPACITY);
            Semaphore spaces = new Semaphore(CAPACITY);
            Semaphore elements = new Semaphore(0);
            AtomicInteger nextPiece = new AtomicInteger(1);
            for (int i = 1; i <= producerCount; i++) {
                int id = i;
                executor.submit(() -> produce(id, piecesPerWorker, warehouse, spaces, elements, nextPiece));
            }
            for (int i = 1; i <= consumerCount; i++) {
                int id = i;
                int consumerPieces = piecesPerWorker * producerCount / consumerCount
                        + (i <= piecesPerWorker * producerCount % consumerCount ? 1 : 0);
                executor.submit(() -> consume(id, consumerPieces,
                        warehouse, spaces, elements));
            }
            return Map.of("started", true, "totalPieces", total, "capacity", CAPACITY);
        }
    }

    private void produce(int id, int count, BlockingQueue<Integer> warehouse, Semaphore spaces,
                         Semaphore elements, AtomicInteger nextPiece) {
        try {
            for (int i = 0; i < count; i++) {
                Thread.sleep(150);
                int piece = nextPiece.getAndIncrement();
                spaces.acquire();
                warehouse.put(piece);
                update(s -> s.withProduced(s.produced() + 1).withWarehouse(warehouse.size())
                        .add("PRODUCTOR " + id + " agregó pieza " + piece));
                elements.release();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            addEvent("Productor " + id + " interrumpido");
        } finally {
            finishIfComplete();
        }
    }

    private void consume(int id, int count, BlockingQueue<Integer> warehouse, Semaphore spaces, Semaphore elements) {
        try {
            for (int i = 0; i < count; i++) {
                elements.acquire();
                int piece = warehouse.take();
                spaces.release();
                update(s -> s.withConsumed(s.consumed() + 1).withWarehouse(warehouse.size())
                        .add("CONSUMIDOR " + id + " retiró pieza " + piece));
                Thread.sleep(250);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            addEvent("Consumidor " + id + " interrumpido");
        } finally {
            finishIfComplete();
        }
    }

    private void finishIfComplete() {
        synchronized (stateLock) {
            if (state.consumed() >= state.total()) state = state.withRunning(false)
                    .add("Simulación finalizada: todas las piezas fueron ensambladas");
        }
    }

    public Map<String, Object> status() {
        RunState s = state;
        return Map.of("running", s.running(), "producers", s.producers(), "consumers", s.consumers(),
                "piecesPerWorker", s.piecesPerWorker(), "produced", s.produced(),
                "consumed", s.consumed(), "warehouse", s.warehouse(), "capacity", CAPACITY,
                "total", s.total());
    }

    public List<Event> events() { return List.copyOf(state.events()); }
    private void addEvent(String message) { update(s -> s.add(message)); }
    private void update(java.util.function.UnaryOperator<RunState> change) {
        synchronized (stateLock) { state = change.apply(state); }
    }

    record Event(long timestamp, String message) {}
    private record RunState(boolean running, int producers, int consumers, int piecesPerWorker,
                            int produced, int consumed, int warehouse, int total,
                            List<Event> events, Instant startedAt) {
        static RunState idle() {
            return new RunState(false, 0, 0, 0, 0, 0, 0, 0,
                    new CopyOnWriteArrayList<>(), Instant.now());
        }
        RunState add(String message) {
            events.add(new Event(System.currentTimeMillis(), message));
            return this;
        }
        RunState withRunning(boolean value) { return new RunState(value, producers, consumers, piecesPerWorker, produced, consumed, warehouse, total, events, startedAt); }
        RunState withProduced(int value) { return new RunState(running, producers, consumers, piecesPerWorker, value, consumed, warehouse, total, events, startedAt); }
        RunState withConsumed(int value) { return new RunState(running, producers, consumers, piecesPerWorker, produced, value, warehouse, total, events, startedAt); }
        RunState withWarehouse(int value) { return new RunState(running, producers, consumers, piecesPerWorker, produced, consumed, value, total, events, startedAt); }
    }
}
