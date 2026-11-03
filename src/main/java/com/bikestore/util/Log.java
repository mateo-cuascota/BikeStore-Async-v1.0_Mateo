package com.bikestore.util;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

/**
 * Logger sencillo: cada linea lleva timestamp, thread, componente y pedidoId.
 * Ademas guarda el evento en EventStore para mostrarlo en la pantalla web.
 */
public final class Log {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

    private Log() {
    }

    public static synchronized void evento(String componente, String pedidoId, String tipo,
                                           String mensaje, Map<String, Object> extra) {
        String ts = LocalDateTime.now().format(FMT);
        String thread = Thread.currentThread().getName();
        System.out.printf("[%s] [thread=%s] [%-14s] pedidoId=%s | %s%n",
                ts, thread, componente, pedidoId == null ? "-" : pedidoId, mensaje);
        EventStore.agregar(ts, thread, componente, pedidoId, tipo, mensaje, extra);
    }

    public static void evento(String componente, String pedidoId, String tipo, String mensaje) {
        evento(componente, pedidoId, tipo, mensaje, null);
    }

    public static void evento(String componente, String pedidoId, String mensaje) {
        evento(componente, pedidoId, "INFO", mensaje, null);
    }

    public static void info(String componente, String mensaje) {
        evento(componente, null, "INFO", mensaje, null);
    }
}
