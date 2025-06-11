package com.myobservation.listener.connection;

import com.myobservation.listener.ack.HL7AckGenerator;
import com.myobservation.storage.service.parser.HL7ParserService; // <-- Importa el servicio de parser
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.net.ServerSocket;
import java.net.Socket;
import java.time.LocalDateTime;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// Importa las constantes de tu clase ProtocolConstants
import static com.myobservation.listener.utils.ProtocolConstants.*;

/**
 * Es la clase principal y orquestadora
 * Su responsabilidad es la gestión del ciclo de vida del servidor MLLP (iniciar, parar) y
 * la aperta del ServerSocket y aceptación de nuevas conexiones
 */
@Component
public class MLLPServer implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(MLLPServer.class);

    private final ExecutorService executorService = Executors.newFixedThreadPool(10); // Maneja hasta 10 conexiones

    private final HL7AckGenerator hl7AckGenerator;
    private final HL7ParserService hl7ParserService; // <-- Inyecta el servicio de parser aquí también

    // Llave del puerto en properties
    @Value("${mllp.server.port}")
    private int mllpPort;

    // Constructor actualizado para inyectar HL7ParserService
    public MLLPServer(HL7AckGenerator hl7AckGenerator, HL7ParserService hl7ParserService) {
        this.hl7AckGenerator = hl7AckGenerator;
        this.hl7ParserService = hl7ParserService; // Asigna el servicio
    }

    @Override
    public void run(String... args) throws Exception {
        executorService.submit(this::startServer); // Enviar la tarea al pool
    }

    private void startServer() {
        try (ServerSocket serverSocket = new ServerSocket(mllpPort)) { // Usa la variable inyectada
            logger.info("⚡ [SERVER START] Servidor MLLP iniciado en puerto: {} - {}", mllpPort, LocalDateTime.now());

            while (true) {
                try {
                    logger.info("🔄 [LISTENING] Esperando conexiones...");
                    Socket socket = serverSocket.accept();
                    logger.info("🔗 [CONNECTION] Nueva conexión desde: {}:{}", socket.getInetAddress(), socket.getPort());

                    // Crea un nuevo MLLPConnectionHandler y lo ejecuta en el pool de hilos
                    // Pasa las dependencias (ackGenerator, parserService) al handler
                    executorService.submit(new MLLPConnectionHandler(socket, hl7AckGenerator, hl7ParserService));

                } catch (Exception e) {
                    logger.error("❌ [SERVER ERROR] Error en el servidor: {}", e.getMessage(), e);
                    // No es necesario e.printStackTrace(); si usas logger.error con el Throwable
                }
            }
        } catch (Exception e) {
            logger.error("❌ [SERVER FATAL] No se pudo iniciar el servidor: {}", e.getMessage(), e);
            // No es necesario e.printStackTrace();
        }
    }

    // El método handleClientConnection() se ha eliminado de aquí,
    // ya que su lógica ahora está en MLLPConnectionHandler.
}