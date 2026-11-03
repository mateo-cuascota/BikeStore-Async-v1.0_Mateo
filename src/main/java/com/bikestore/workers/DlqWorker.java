package com.bikestore.workers;

import com.bikestore.broker.Conexiones;
import com.bikestore.config.Config;
import com.bikestore.model.Pedido;
import com.bikestore.util.Json;
import com.bikestore.util.Log;

import javax.jms.Connection;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.Session;
import javax.jms.TextMessage;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.Instant;

/**
 * Lee la Dead-Letter Queue y guarda cada pedido fallido en dlq-pedidos.log
 * para analizarlo despues (offline).
 */
public class DlqWorker {

    private static final String COMP = "DlqWorker";
    private static final String ARCHIVO = "dlq-pedidos.log";
    private Connection conexion;

    public void start() throws JMSException {
        conexion = Conexiones.nueva("dlq-worker");
        Session session = conexion.createSession(false, Session.AUTO_ACKNOWLEDGE);
        MessageConsumer consumer = session.createConsumer(session.createQueue(Config.QUEUE_DLQ));
        consumer.setMessageListener(this::procesar);
        conexion.start();
        Log.info(COMP, "Escuchando " + Config.QUEUE_DLQ + " (pedidos se guardan en " + ARCHIVO + ")");
    }

    private void procesar(Message message) {
        try {
            String json = ((TextMessage) message).getText();
            Pedido pedido = Json.fromJson(json, Pedido.class);
            pedido.setPaymentStatus("FAILED");

            try (PrintWriter out = new PrintWriter(new FileWriter(ARCHIVO, true))) {
                out.println(Instant.now() + " " + Json.toJson(pedido));
            }
            Log.evento(COMP, pedido.getPedidoId(), "DLQ", "Pedido en DLQ (paymentStatus=FAILED). Guardado en "
                    + ARCHIVO + " para analisis offline");
        } catch (JMSException | IOException e) {
            Log.info(COMP, "Error leyendo DLQ: " + e.getMessage());
        }
    }

    public void stop() throws JMSException {
        if (conexion != null) conexion.close();
    }

    public static void main(String[] args) throws Exception {
        new DlqWorker().start();
        Thread.currentThread().join();
    }
}
