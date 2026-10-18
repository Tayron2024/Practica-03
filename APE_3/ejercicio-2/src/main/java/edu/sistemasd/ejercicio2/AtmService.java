package edu.sistemasd.ejercicio2;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;

@Service
public class AtmService {
    private static final int INITIAL_BALANCE = 1000;
    private final ExecutorService executor = Executors.newFixedThreadPool(3);
    private final ReentrantLock balanceMutex = new ReentrantLock(true);
    private final Object stateLock = new Object();
    private volatile State state = State.idle();

    public Map<String, Object> start() {
        synchronized (stateLock) {
            if (state.running()) return Map.of("started", false, "message", "Ya existe una simulación en ejecución");
            state = new State(true, INITIAL_BALANCE, new CopyOnWriteArrayList<>());
            state.add("Saldo inicial: $" + INITIAL_BALANCE);
            CountDownLatch startGate = new CountDownLatch(1);
            executor.submit(() -> runWithdrawal(startGate, 1, 300));
            executor.submit(() -> runWithdrawal(startGate, 2, 250));
            executor.submit(() -> runDeposit(startGate, 3, 200));
            startGate.countDown();
            return Map.of("started", true, "initialBalance", INITIAL_BALANCE);
        }
    }

    private void runWithdrawal(CountDownLatch gate, int processId, int amount) {
        await(gate);
        balanceMutex.lock();
        try {
            addEvent("Proceso " + processId + " intenta retirar $" + amount);
            pause();
            if (state.balance() >= amount) {
                int balance = state.balance() - amount;
                setBalance(balance);
                addEvent("Proceso " + processId + ": retiro realizado de $" + amount
                        + ". Saldo actual: $" + balance);
            } else {
                addEvent("Proceso " + processId + ": saldo insuficiente");
            }
        } finally {
            balanceMutex.unlock();
            completeIfReady();
        }
    }

    private void runDeposit(CountDownLatch gate, int processId, int amount) {
        await(gate);
        balanceMutex.lock();
        try {
            addEvent("Proceso " + processId + " deposita $" + amount);
            pause();
            int balance = state.balance() + amount;
            setBalance(balance);
            addEvent("Proceso " + processId + ": depósito realizado. Saldo actual: $" + balance);
        } finally {
            balanceMutex.unlock();
            completeIfReady();
        }
    }

    private void completeIfReady() {
        synchronized (stateLock) {
            if (state.events().stream().filter(event -> event.message().contains("realizado")
                    || event.message().contains("insuficiente")).count() >= 3) {
                state = state.withRunning(false);
                state.add("Saldo final: $" + state.balance());
            }
        }
    }

    private void await(CountDownLatch gate) {
        try {
            gate.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            addEvent("Proceso interrumpido antes de iniciar");
        }
    }

    private void pause() {
        try {
            Thread.sleep(700);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void setBalance(int balance) {
        synchronized (stateLock) {
            state = state.withBalance(balance);
        }
    }

    private void addEvent(String message) {
        synchronized (stateLock) {
            state.add(message);
        }
    }

    public Map<String, Object> status() {
        State current = state;
        return Map.of("running", current.running(), "initialBalance", INITIAL_BALANCE,
                "balance", current.balance());
    }

    public List<Event> events() {
        return List.copyOf(state.events());
    }

    record Event(long timestamp, String message) {}

    private record State(boolean running, int balance, List<Event> events) {
        static State idle() {
            return new State(false, INITIAL_BALANCE, new CopyOnWriteArrayList<>());
        }

        State withBalance(int value) {
            return new State(running, value, events);
        }

        State withRunning(boolean value) {
            return new State(value, balance, events);
        }

        void add(String message) {
            events.add(new Event(System.currentTimeMillis(), message));
        }
    }
}
