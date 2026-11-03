package com.bikestore.broker;

import com.bikestore.config.Config;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.RedeliveryPolicy;

import javax.jms.Connection;
import javax.jms.JMSException;

/**
 * Crea conexiones JMS al broker con la politica de reintentos configurada.
 */
public final class Conexiones {

    private Conexiones() {
    }

    public static Connection nueva(String clientId) throws JMSException {
        ActiveMQConnectionFactory factory = new ActiveMQConnectionFactory(Config.BROKER_URL);

        // Politica de reintentos: 3 reintentos con 1 s de espera, luego DLQ
        RedeliveryPolicy politica = new RedeliveryPolicy();
        politica.setMaximumRedeliveries(Config.MAX_REINTENTOS);
        politica.setInitialRedeliveryDelay(Config.ESPERA_REINTENTO_MS);
        politica.setRedeliveryDelay(Config.ESPERA_REINTENTO_MS);
        politica.setUseExponentialBackOff(false);
        factory.setRedeliveryPolicy(politica);

        // Mientras un pedido espera su reintento, el consumidor puede seguir con otros
        factory.setNonBlockingRedelivery(true);

        Connection con = factory.createConnection();
        con.setClientID(clientId);
        return con;
    }
}
