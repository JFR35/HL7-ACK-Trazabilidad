package com.myobservation.listener.connection;

import com.myobservation.listener.ack.HL7AckGenerator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static com.myobservation.listener.utils.ProtocolConstants.*;


/**
 * Es la clase principal y orquestadora
 * Su responsabilidad es la gestión del ciclo de vida del servidor MLLP (iniciar, parar) y
 * la aperta del ServerSocket y aceptación de nuevas conexiones
 */
@Component
public class MLLPServer implements CommandLineRunner {

    private final ExecutorService executorService = Executors.newFixedThreadPool(10); // Maneja hasta 10 conexiones

    private final HL7AckGenerator hl7AckGenerator;

    // Llave del puerto en properties
    @Value("${mllp.server.port}")
    private int mllPort;

    public MLLPServer(HL7AckGenerator hl7AckGenerator) {
        this.hl7AckGenerator = hl7AckGenerator;
    }

    /**
     * Mejora la gestión de hilos y permite manejar mútiples clientes sin saturar la CPU
     * @param args
     * @throws Exception
     */
    @Override
    public void run(String... args) throws Exception {
        executorService.submit(this::startServer); // Enviar la tarea al pool
    }

    private void startServer() {
        try (ServerSocket serverSocket = new ServerSocket(mllPort)) {
            System.out.println("[SERVER START] Servidor MLLP iniciado en puerto: " + mllPort + " - " + LocalDateTime.now());

            while (true) {
                try {
                    System.out.println("[LISTENING] Esperando conexiones...");
                    Socket socket = serverSocket.accept();
                    System.out.println("[CONNECTION] Nueva conexión desde: " + socket.getInetAddress() + ":" + socket.getPort());

                    handleClientConnection(socket);
                } catch (Exception e) {
                    System.err.println("[SERVER ERROR] Error en el servidor: " + e.getMessage());
                    e.printStackTrace();
                }
            }
        } catch (Exception e) {
            System.err.println("[SERVER FATAL] No se pudo iniciar el servidor: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void handleClientConnection(Socket socket) {
        try (InputStream inputStream = socket.getInputStream();
             OutputStream outputStream = socket.getOutputStream()) {

            // Leer mensaje MLLP
            ByteArrayOutputStream messageBuffer = new ByteArrayOutputStream();
            boolean startBlockFound = false;
            int bytesRead;

            System.out.println("[READING] Leyendo mensaje HL7...");

            while ((bytesRead = inputStream.read()) != -1) {
                char currentChar = (char) bytesRead;

                if (currentChar == START_BLOCK) {
                    startBlockFound = true;
                    continue; // Saltamos el carácter de inicio
                }

                if (startBlockFound) {
                    if (currentChar == END_BLOCK) {
                        break; // Fin del mensaje
                    }
                    messageBuffer.write(bytesRead);
                }
            }

            if (!startBlockFound) {
                System.err.println("[WARNING] Mensaje recibido sin carácter de inicio MLLP (0x0B)");
                return;
            }

            String hl7Message = messageBuffer.toString(StandardCharsets.UTF_8.name());
            System.out.println("[MESSAGE RECEIVED] Mensaje HL7 recibido:\n" + hl7Message);

            // Procesar y responder ACK
            String ackMessage = hl7AckGenerator.buildAckMessage(hl7Message);
            System.out.println("[SENDING ACK] Preparando ACK:\n" + ackMessage);

            // Enviar ACK con encapsulamiento MLLP
            outputStream.write(START_BLOCK);
            outputStream.write(ackMessage.getBytes(StandardCharsets.UTF_8));
            outputStream.write(END_BLOCK);
            outputStream.write(CARRIAGE_RETURN);
            outputStream.flush();

            System.out.println("[ACK SENT] ACK enviado correctamente");

        } catch (Exception e) {
            System.err.println("[CONNECTION ERROR] Error en la conexión: " + e.getMessage());
            e.printStackTrace();
        } finally {
            try {
                socket.close();
                System.out.println("[CONNECTION CLOSED] Conexión cerrada");
            } catch (IOException e) {
                System.err.println("[WARNING] Error al cerrar la conexión: " + e.getMessage());
            }
        }
    }
}