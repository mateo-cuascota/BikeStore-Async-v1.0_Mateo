package com.bikestore.config;

/**
 * Constantes compartidas por todos los componentes.
 */
public final class Config {

    private Config() {
    }

    /** URL del broker. Se puede cambiar con -Dbroker.url=... */
    public static final String BROKER_URL = System.getProperty("broker.url", "tcp://localhost:61616");

    /** Cola donde OrderProducer publica los pedidos nuevos. */
    public static final String QUEUE_ORDERS = "bikestore.orders";

    /** Cola donde PaymentWorker publica el resultado del cobro (lo lee EmailWorker). */
    public static final String QUEUE_PAYMENTS = "bikestore.payments";

    /** Dead-Letter Queue: pedidos que fallaron el cobro despues de 3 reintentos. */
    public static final String QUEUE_DLQ = "DLQ." + QUEUE_ORDERS;

    /** Reintentos maximos antes de mandar el mensaje a la DLQ (1 intento + 3 reintentos). */
    public static final int MAX_REINTENTOS = 3;

    /** Espera entre reintentos (ms). */
    public static final long ESPERA_REINTENTO_MS = 1000;

    /** Probabilidad de que falle el cobro (0.5 = 50 %). */
    public static final double PROBABILIDAD_FALLO = 0.5;

    /** Puerto del API REST "Ordenar". */
    public static final int HTTP_PORT = Integer.getInteger("http.port", 8080);
}
