package com.bikestore.workers;

import com.bikestore.broker.Conexiones;
import com.bikestore.config.Config;
import com.bikestore.model.Pedido;
import com.bikestore.util.Json;
import com.bikestore.util.Log;

import javax.jms.Connection;
import javax.jms.DeliveryMode;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.jms.Session;
import javax.jms.TextMessage;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * Consumidor 1: simula el cobro con tarjeta.
 *
 * - Usa una sesion transaccional: si el cobro falla se hace rollback() y el broker
 *   vuelve a entregar el mensaje (reintento).
 * - Despues de 3 reintentos fallidos ActiveMQ mueve el mensaje a DLQ.bikestore.orders.
 * - Si el cobro sale bien publica el pedido con paymentStatus=PAID en bikestore.payments.
 */
public class PaymentWorker {

    private static final String COMP = "PaymentWorker";
    private final Random random = new Random();

    private Connection conexion;
    private Session session;
    private MessageProducer producerPagos;

    public void start() throws JMSException {
        conexion = Conexiones.nueva("payment-worker");
        session = conexion.createSession(true, Session.SESSION_TRANSACTED);
        MessageConsumer consumer = session.createConsumer(session.createQueue(Config.QUEUE_ORDERS));
        producerPagos = session.createProducer(session.createQueue(Config.QUEUE_PAYMENTS));
        producerPagos.setDeliveryMode(DeliveryMode.PERSISTENT);

        consumer.setMessageListener(this::procesar);
        conexion.start();
        Log.info(COMP, "Escuchando " + Config.QUEUE_ORDERS);
    }

    private void procesar(Message message) {
        String pedidoId = "?";
        try {
            Pedido pedido = Json.fromJson(((TextMessage) message).getText(), Pedido.class);
            pedidoId = pedido.getPedidoId();

            // JMSXDeliveryCount: 1 = primer intento, 2 = primer reintento, etc.
            int intento = message.getIntProperty("JMSXDeliveryCount");
            String etiqueta = intento == 1 ? "intento 1" : "reintento " + (intento - 1) + "/" + Config.MAX_REINTENTOS;
            Map<String, Object> extra = new HashMap<>();
            extra.put("intento", intento);
            Log.evento(COMP, pedidoId, "COBRANDO", "Cobrando $" + pedido.getTotal()
                    + " a la tarjeta (" + etiqueta + ")", extra);

            Thread.sleep(200); // simula la llamada a la pasarela de pago

            boolean fallo = pedido.isForzarFallo() || random.nextDouble() < Config.PROBABILIDAD_FALLO;

            if (fallo) {
                if (intento > Config.MAX_REINTENTOS) {
                    Log.evento(COMP, pedidoId, "AGOTADO", "Cobro RECHAZADO. Se agotaron los "
                            + Config.MAX_REINTENTOS + " reintentos -> se envia a " + Config.QUEUE_DLQ, extra);
                } else {
                    Log.evento(COMP, pedidoId, "RECHAZADO", "Cobro RECHAZADO por la pasarela. Se reintentara en "
                            + Config.ESPERA_REINTENTO_MS + " ms", extra);
                }
                session.rollback(); // el broker reentrega o, si ya no quedan reintentos, manda a la DLQ
                return;
            }

            pedido.setPaymentStatus("PAID");
            TextMessage pagado = session.createTextMessage(Json.toJson(pedido));
            pagado.setStringProperty("pedidoId", pedidoId);
            pagado.setStringProperty("paymentStatus", "PAID");
            producerPagos.send(pagado);
            session.commit(); // confirma el consumo y la publicacion juntos

            Log.evento(COMP, pedidoId, "PAGADO", "Cobro APROBADO (paymentStatus=PAID) -> publicado en "
                    + Config.QUEUE_PAYMENTS, extra);
        } catch (Exception e) {
            Log.evento(COMP, pedidoId, "Error inesperado: " + e.getMessage());
            try {
                session.rollback();
            } catch (JMSException ignored) {
            }
        }
    }

    public void stop() throws JMSException {
        if (conexion != null) conexion.close();
    }

    public static void main(String[] args) throws Exception {
        new PaymentWorker().start();
        Thread.currentThread().join();
    }
}
