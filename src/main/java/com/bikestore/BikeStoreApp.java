package com.bikestore;

import com.bikestore.broker.BrokerServer;
import com.bikestore.config.Config;
import com.bikestore.producer.OrderProducer;
import com.bikestore.util.Log;
import com.bikestore.workers.DlqWorker;
import com.bikestore.workers.EmailWorker;
import com.bikestore.workers.PaymentWorker;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

/**
 * Arranca todo en un solo proceso: broker + OrderProducer (API) + PaymentWorker
 * + EmailWorker + DlqWorker. Por defecto manda unos pedidos de prueba al API.
 *
 * Uso: mvn compile exec:java                 (con pedidos de demo)
 *      mvn compile exec:java -Dexec.args="--sin-demo"   (solo levanta el sistema)
 */
public class BikeStoreApp {

    public static void main(String[] args) throws Exception {
        boolean demo = args.length == 0 || !"--sin-demo".equals(args[0]);

        System.out.println("==============================================");
        System.out.println("        BikeStore Async v1.0 - Mateo          ");
        System.out.println("==============================================");

        BrokerServer broker = new BrokerServer();
        broker.start();

        PaymentWorker payment = new PaymentWorker();
        EmailWorker email = new EmailWorker();
        DlqWorker dlq = new DlqWorker();
        OrderProducer api = new OrderProducer();
        payment.start();
        email.start();
        dlq.start();
        api.start();

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                api.stop();
                payment.stop();
                email.stop();
                dlq.stop();
                broker.stop();
            } catch (Exception ignored) {
            }
        }, "shutdown"));

        if (demo) {
            enviarPedidosDemo();
        }

        Log.info("App", "Sistema corriendo. Abre http://localhost:" + Config.HTTP_PORT
                + " o manda POST a /ordenar. Ctrl+C para salir.");
        Thread.currentThread().join();
    }

    /** Simula al front-end llamando al API "Ordenar" varias veces. */
    private static void enviarPedidosDemo() throws Exception {
        String[][] clientes = {
                {"Ana Torres", "ana@mail.com", "Bicicleta MTB Rin 29", "450.00"},
                {"Luis Perez", "luis@mail.com", "Casco Pro", "65.50"},
                {"Carla Ruiz", "carla@mail.com", "Bicicleta de ruta", "899.99"},
                {"Diego Mora", "diego@mail.com", "Kit de luces LED", "25.00"},
                {"Sofia Vega", "sofia@mail.com", "Bicicleta urbana", "380.00"},
        };

        HttpClient client = HttpClient.newHttpClient();
        String url = "http://localhost:" + Config.HTTP_PORT + "/ordenar";

        for (String[] c : clientes) {
            enviar(client, url, c[0], c[1], c[2], c[3], false);
        }
        // Un pedido que siempre falla para que se vea la Dead-Letter Queue
        enviar(client, url, "Pedro Prueba", "pedro@mail.com", "Tarjeta sin fondos", "999.00", true);
    }

    private static void enviar(HttpClient client, String url, String cliente, String mail,
                               String producto, String precio, boolean forzarFallo) throws Exception {
        String json = "{"
                + "\"cliente\":\"" + cliente + "\","
                + "\"email\":\"" + mail + "\","
                + "\"forzarFallo\":" + forzarFallo + ","
                + "\"items\":[{\"producto\":\"" + producto + "\",\"cantidad\":1,\"precio\":" + precio + "}]"
                + "}";
        HttpRequest req = HttpRequest.newBuilder(URI.create(url))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();
        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());
        Log.info("Front-end", "Respuesta del API: HTTP " + resp.statusCode() + " " + resp.body());
    }
}
