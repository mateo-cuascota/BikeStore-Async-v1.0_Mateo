package com.bikestore.broker;

import com.bikestore.config.Config;
import com.bikestore.util.Log;
import org.apache.activemq.broker.BrokerService;
import org.apache.activemq.broker.region.policy.IndividualDeadLetterStrategy;
import org.apache.activemq.broker.region.policy.PolicyEntry;
import org.apache.activemq.broker.region.policy.PolicyMap;

import java.util.Collections;

/**
 * Levanta un broker ActiveMQ embebido en tcp://localhost:61616.
 * Configura una DLQ individual: los mensajes muertos de "bikestore.orders"
 * terminan en "DLQ.bikestore.orders".
 */
public class BrokerServer {

    private BrokerService broker;

    public void start() throws Exception {
        broker = new BrokerService();
        broker.setBrokerName("bikestore-broker");
        broker.setPersistent(false);   // en memoria, suficiente para el ejercicio
        broker.setUseJmx(false);
        broker.addConnector(Config.BROKER_URL);

        // DLQ individual por cola: DLQ.<nombreCola>
        IndividualDeadLetterStrategy dlq = new IndividualDeadLetterStrategy();
        dlq.setQueuePrefix("DLQ.");
        dlq.setUseQueueForQueueMessages(true);

        PolicyEntry politica = new PolicyEntry();
        politica.setQueue(">");   // aplica a todas las colas
        politica.setDeadLetterStrategy(dlq);

        PolicyMap mapa = new PolicyMap();
        mapa.setPolicyEntries(Collections.singletonList(politica));
        broker.setDestinationPolicy(mapa);

        broker.start();
        broker.waitUntilStarted();
        Log.info("BROKER", "ActiveMQ iniciado en " + Config.BROKER_URL);
    }

    public void stop() throws Exception {
        if (broker != null) {
            broker.stop();
            broker.waitUntilStopped();
            Log.info("BROKER", "ActiveMQ detenido");
        }
    }

    /** Permite correr el broker solo, en su propia terminal. */
    public static void main(String[] args) throws Exception {
        BrokerServer server = new BrokerServer();
        server.start();
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                server.stop();
            } catch (Exception ignored) {
            }
        }));
        Thread.currentThread().join();
    }
}
