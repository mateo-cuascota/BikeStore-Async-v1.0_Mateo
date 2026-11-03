# BikeStore Async v1.0 — Mateo

Aplicación asíncrona de pedidos con un bróker de mensajería (Apache ActiveMQ embebido).

## Flujo

```
Front-end (index.html / demo)
      │  POST /ordenar
      ▼
OrderProducer ──JSON──► [bikestore.orders]
                               │
                               ▼
                        PaymentWorker  (50 % de fallo)
                         │          │
                 cobro OK│          │falla → rollback → reintento (máx. 3, cada 1 s)
                         ▼          │
             [bikestore.payments]   └─► tras 3 reintentos ─► [DLQ.bikestore.orders]
                         │                                          │
                         ▼                                          ▼
                    EmailWorker                                DlqWorker
            (solo si paymentStatus=PAID)              (guarda en dlq-pedidos.log)
```

## Requisitos
- Java 11 o superior (`java -version`)
- Internet la primera vez (se descargan Maven, ActiveMQ y Jackson)

No hace falta instalar Maven: el script `mvnw` lo descarga solo.
Tampoco hace falta RabbitMQ ni Docker: el bróker ActiveMQ arranca dentro de la misma app.

## Cómo correrlo

Desde la terminal de VS Code, en esta carpeta:

```bash
./mvnw compile exec:java          # Git Bash
.\mvnw.cmd compile exec:java      # PowerShell o CMD
```

(Si ya tienes Maven instalado, también sirve `mvn compile exec:java`. En los comandos de abajo
cambia `mvn` por `./mvnw` si no lo tienes.)

Esto levanta el bróker, los 3 workers y el API, y además manda 6 pedidos de prueba
(uno de ellos con `forzarFallo=true`, para que siempre se vea la DLQ).

Para levantar el sistema sin los pedidos de demo:

```bash
mvn compile exec:java -Dexec.args="--sin-demo"
```

### Probar desde el navegador
Abre http://localhost:8080, llena el formulario y presiona **Confirmar pedido**.

### Probar con curl / Postman
```bash
curl -X POST http://localhost:8080/ordenar -H "Content-Type: application/json" -d "{\"cliente\":\"Mateo\",\"email\":\"mateo@mail.com\",\"items\":[{\"producto\":\"Bicicleta MTB\",\"cantidad\":1,\"precio\":450}]}"
```

### Correr cada componente en su propia terminal (opcional)
Para mostrar que los consumidores son independientes:

```bash
mvn compile exec:java -Dexec.mainClass=com.bikestore.broker.BrokerServer
mvn exec:java -Dexec.mainClass=com.bikestore.workers.PaymentWorker
mvn exec:java -Dexec.mainClass=com.bikestore.workers.EmailWorker
mvn exec:java -Dexec.mainClass=com.bikestore.workers.DlqWorker
mvn exec:java -Dexec.mainClass=com.bikestore.producer.OrderProducer
```

## Ejemplo de log
Cada línea tiene `timestamp`, `thread`, componente y `pedidoId`:

```
[2026-11-03 09:40:12.345] [thread=http-21] [OrderProducer ] pedidoId=PED-3F2A1B9C | Pedido publicado en bikestore.orders (total=$450.0)
[2026-11-03 09:40:12.401] [thread=ActiveMQ Session Task-1] [PaymentWorker ] pedidoId=PED-3F2A1B9C | Cobrando $450.0 a la tarjeta (intento 1)
[2026-11-03 09:40:12.603] [thread=ActiveMQ Session Task-1] [PaymentWorker ] pedidoId=PED-3F2A1B9C | Cobro RECHAZADO por la pasarela. Se reintentara en 1000 ms
...
```

## Estructura
| Clase | Qué hace |
|---|---|
| `BikeStoreApp` | Arranca todo y manda los pedidos de demo |
| `broker/BrokerServer` | Bróker ActiveMQ embebido con la DLQ configurada |
| `broker/Conexiones` | Conexiones JMS con la política de 3 reintentos |
| `producer/OrderProducer` | API `POST /ordenar` → publica el JSON en la cola |
| `workers/PaymentWorker` | Simula el cobro (50 % de fallo), con reintentos por rollback |
| `workers/EmailWorker` | Envía el e-mail solo si `paymentStatus=PAID` |
| `workers/DlqWorker` | Lee la DLQ y guarda los pedidos fallidos en `dlq-pedidos.log` |
| `config/Config` | Nombres de colas, reintentos, probabilidad de fallo, puerto |
| `util/Log` | Log con timestamp + thread + pedidoId |

## Parámetros (en `Config.java`)
- `MAX_REINTENTOS = 3`
- `ESPERA_REINTENTO_MS = 1000`
- `PROBABILIDAD_FALLO = 0.5`
- `HTTP_PORT = 8080` (también se puede pasar con `-Dhttp.port=9090`)
