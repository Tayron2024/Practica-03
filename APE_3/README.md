# APE 3 - Ejercicios 1, 2, 3, 4 y 5

Implementación de los cinco ejercicios de concurrencia con microservicios independientes.

## Ejecución

Requiere Java 25 (LTS) y Maven 3.9 o posterior.

En terminales separadas:

```bash
cd ejercicio-1 && mvn spring-boot:run
cd ejercicio-2 && mvn spring-boot:run
cd ejercicio-3 && mvn spring-boot:run
cd ejercicio-4 && mvn spring-boot:run
cd ejercicio-5 && mvn spring-boot:run
cd frontend && npm install && npm run dev
```

Abre la URL mostrada por Vite (normalmente `http://localhost:5173`). La misma plantilla permite seleccionar cualquiera de los cinco ejercicios.

- El ejercicio 1 usa un semáforo contador para controlar 10 vehículos en un estacionamiento de capacidad configurable. Escucha en `8081`.
- El ejercicio 2 usa un mutex justo para proteger el saldo durante dos retiros y un depósito concurrentes. Escucha en `8082`.
- El ejercicio 3 usa una cola `LinkedBlockingQueue` de capacidad 5, semáforos `espacios`/`elementos` y sincronización del estado.
- El ejercicio 4 usa `ReentrantLock(true)` y `Condition`; el bloqueo justo evita que una dirección monopolice el puente.
- El ejercicio 5 usa semáforos encadenados para coordinar diseño → revisión → impresión de tres documentos. Escucha en `8085`.
- En el ejercicio 3, productores, consumidores y piezas por productor se reciben en cada ejecución y se reflejan en el estado y los eventos.
- En el ejercicio 4, la cantidad de vehículos por dirección y el tiempo de cruce se reciben en cada ejecución; el estado distingue los totales configurados de los vehículos actualmente en el puente.
