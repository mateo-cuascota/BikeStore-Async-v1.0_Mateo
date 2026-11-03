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

/**
 * Consumidor 2: envia el e-mail de confirmacion (simulado en consola).
 * Solo lo hace si el pedido llega con paymentStatus=PAID.
 */
public class EmailWorker {

    private static final String COMP = "EmailWorker";
    private Connection conexion;

    public void start() throws JMSException {
        conexion = Conexiones.nueva("email-worker");
        Session session = conexion.createSession(false, Session.AUTO_ACKNOWLEDGE);
        MessageConsumer consumer = session.createConsumer(session.createQueue(Config.QUEUE_PAYMENTS));
        consumer.setMessageListener(this::procesar);
        conexion.start();
        Log.info(COMP, "Escuchando " + Config.QUEUE_PAYMENTS);
    }

    private void procesar(Message message) {
        try {
            Pedido pedido = Json.fromJson(((TextMessage) message).getText(), Pedido.class);
            String id = pedido.getPedidoId();

            if (!"PAID".equals(pedido.getPaymentStatus())) {
                Log.evento(COMP, id, "EMAIL_OMITIDO", "No se envia e-mail: paymentStatus=" + pedido.getPaymentStatus());
                return;
            }

            Thread.sleep(150); // simula el envio por SMTP
            Log.evento(COMP, id, "EMAIL", "E-mail de confirmacion enviado a " + pedido.getEmail()
                    + " (\"Tu pedido de BikeStore por $" + pedido.getTotal() + " fue confirmado\")");
        } catch (Exception e) {
            Log.info(COMP, "Error enviando e-mail: " + e.getMessage());
        }
    }

    public void stop() throws JMSException {
        if (conexion != null) conexion.close();
    }

    public static void main(String[] args) throws Exception {
        new EmailWorker().start();
        Thread.currentThread().join();
    }
}
