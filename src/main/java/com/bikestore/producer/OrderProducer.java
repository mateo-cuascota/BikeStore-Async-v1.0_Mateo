package com.bikestore.producer;

import com.bikestore.broker.Conexiones;
import com.bikestore.config.Config;
import com.bikestore.model.Pedido;
import com.bikestore.util.EventStore;
import com.bikestore.util.Json;
import com.bikestore.util.Log;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import javax.jms.Connection;
import javax.jms.DeliveryMode;
import javax.jms.MessageProducer;
import javax.jms.Session;
import javax.jms.TextMessage;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Executors;

/**
 * API "Ordenar". Recibe el pedido por HTTP (POST /ordenar), lo convierte a JSON
 * y lo publica en la cola bikestore.orders. Responde 202 Accepted sin esperar
 * a que se cobre: el procesamiento es asincrono.
 */
public class OrderProducer {

    private static final String COMP = "OrderProducer";

    private Connection conexion;
    private Session session;
    private MessageProducer producer;
    private HttpServer http;

    public void start() throws Exception {
        conexion = Conexiones.nueva("order-producer");
        session = conexion.createSession(false, Session.AUTO_ACKNOWLEDGE);
        producer = session.createProducer(session.createQueue(Config.QUEUE_ORDERS));
        producer.setDeliveryMode(DeliveryMode.PERSISTENT); // necesario para que la DLQ funcione
        conexion.start();

        http = HttpServer.create(new InetSocketAddress(Config.HTTP_PORT), 0);
        http.createContext("/ordenar", this::manejarOrdenar);
        http.createContext("/eventos", this::manejarEventos);
        http.createContext("/", this::manejarFrontEnd);
        http.setExecutor(Executors.newFixedThreadPool(4, r -> {
            Thread t = new Thread(r);
            t.setName("http-" + t.getId());
            return t;
        }));
        http.start();
        Log.info(COMP, "API lista en http://localhost:" + Config.HTTP_PORT + "/ordenar (POST)");
    }

    /** Publica el pedido en el broker. Se sincroniza porque una Session JMS no es thread-safe. */
    public synchronized String publicar(Pedido pedido) throws Exception {
        if (pedido.getPedidoId() == null || pedido.getPedidoId().isEmpty()) {
            pedido.setPedidoId("PED-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        }
        pedido.setTimestamp(Instant.now().toString());
        pedido.setPaymentStatus("PENDING");
        pedido.calcularTotal();

        String json = Json.toJson(pedido);
        TextMessage msg = session.createTextMessage(json);
        msg.setStringProperty("pedidoId", pedido.getPedidoId());
        producer.send(msg);

        Map<String, Object> extra = new HashMap<>();
        extra.put("cliente", pedido.getCliente());
        extra.put("email", pedido.getEmail());
        extra.put("total", pedido.getTotal());
        StringBuilder productos = new StringBuilder();
        for (Pedido.Item i : pedido.getItems()) {
            if (productos.length() > 0) productos.append(", ");
            productos.append(i.getCantidad()).append(" x ").append(i.getProducto());
        }
        extra.put("producto", productos.toString());

        Log.evento(COMP, pedido.getPedidoId(), "PUBLICADO", "Pedido publicado en " + Config.QUEUE_ORDERS
                + " (total=$" + pedido.getTotal() + ")", extra);
        return pedido.getPedidoId();
    }

    private void manejarOrdenar(HttpExchange ex) throws IOException {
        if (!"POST".equalsIgnoreCase(ex.getRequestMethod())) {
            responder(ex, 405, "{\"error\":\"Usa POST\"}", "application/json");
            return;
        }
        try (InputStream in = ex.getRequestBody()) {
            String body = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            Pedido pedido = Json.fromJson(body, Pedido.class);
            Log.info(COMP, "Peticion HTTP recibida de cliente=" + pedido.getCliente());
            String id = publicar(pedido);
            responder(ex, 202, "{\"pedidoId\":\"" + id + "\",\"estado\":\"RECIBIDO\"}", "application/json");
        } catch (Exception e) {
            Log.info(COMP, "Error al recibir pedido: " + e.getMessage());
            responder(ex, 400, "{\"error\":\"" + e.getMessage().replace("\"", "'") + "\"}", "application/json");
        }
    }

    /** GET /eventos?desde=N -> eventos nuevos para el monitor en vivo de la pantalla. */
    private void manejarEventos(HttpExchange ex) throws IOException {
        long desde = 0;
        String q = ex.getRequestURI().getQuery();
        if (q != null && q.startsWith("desde=")) {
            try {
                desde = Long.parseLong(q.substring(6));
            } catch (NumberFormatException ignored) {
            }
        }
        Map<String, Object> resp = new HashMap<>();
        resp.put("maxReintentos", Config.MAX_REINTENTOS);
        resp.put("eventos", EventStore.desde(desde));
        responder(ex, 200, Json.toJson(resp), "application/json");
    }

    /** Front-end minimo para probar el boton "Confirmar pedido" desde el navegador. */
    private void manejarFrontEnd(HttpExchange ex) throws IOException {
        try (InputStream in = OrderProducer.class.getResourceAsStream("/public/index.html")) {
            String html = in == null ? "<h1>BikeStore</h1>" : new String(in.readAllBytes(), StandardCharsets.UTF_8);
            responder(ex, 200, html, "text/html; charset=utf-8");
        }
    }

    private void responder(HttpExchange ex, int codigo, String cuerpo, String tipo) throws IOException {
        byte[] bytes = cuerpo.getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().set("Content-Type", tipo);
        ex.sendResponseHeaders(codigo, bytes.length);
        try (OutputStream os = ex.getResponseBody()) {
            os.write(bytes);
        }
    }

    public void stop() throws Exception {
        if (http != null) http.stop(0);
        if (conexion != null) conexion.close();
    }

    public static void main(String[] args) throws Exception {
        new OrderProducer().start();
        Thread.currentThread().join();
    }
}
