package com.bikestore.util;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Guarda en memoria los ultimos eventos del sistema para que la pantalla web
 * los pueda mostrar en vivo (GET /eventos?desde=N).
 */
public final class EventStore {

    private static final int MAXIMO = 2000;
    private static final LinkedList<Map<String, Object>> EVENTOS = new LinkedList<>();
    private static long seq = 0;

    private EventStore() {
    }

    public static synchronized void agregar(String timestamp, String thread, String componente,
                                            String pedidoId, String tipo, String mensaje,
                                            Map<String, Object> extra) {
        Map<String, Object> e = new LinkedHashMap<>();
        e.put("seq", ++seq);
        e.put("timestamp", timestamp);
        e.put("thread", thread);
        e.put("componente", componente);
        e.put("pedidoId", pedidoId);
        e.put("tipo", tipo);
        e.put("mensaje", mensaje);
        if (extra != null) {
            e.putAll(extra);
        }
        EVENTOS.addLast(e);
        if (EVENTOS.size() > MAXIMO) {
            EVENTOS.removeFirst();
        }
    }

    /** Devuelve los eventos con seq mayor a 'desde'. */
    public static synchronized List<Map<String, Object>> desde(long desde) {
        List<Map<String, Object>> lista = new ArrayList<>();
        for (Map<String, Object> e : EVENTOS) {
            if ((long) e.get("seq") > desde) {
                lista.add(e);
            }
        }
        return lista;
    }
}
