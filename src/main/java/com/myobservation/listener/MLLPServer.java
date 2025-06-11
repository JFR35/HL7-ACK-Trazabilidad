package com.myobservation.listener;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Es la clase principal y orquestadora
 * Su responsabilidad es la gestión del ciclo de vida del servidor MLLP (iniciar, parar)
 * la aperta del ServerSocket y aceptación de nuevas conexiones
 */
@Component
public class MLLPServer implements CommandLineRunner {

    private static final char START_BLOCK = 0x0B;
    private static final char END_BLOCK = 0x1C;
    private static final char CARRIAGE_RETURN = 0x0D;
    private static final int PORT = 6661;
    private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    @Override
    public void run(String... args) throws Exception {
        new Thread(() -> startServer()).start();
    }

    private void startServer() {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("⚡ [SERVER START] Servidor MLLP iniciado en puerto: " + PORT + " - " + LocalDateTime.now());

            while (true) {
                try {
                    System.out.println("🔄 [LISTENING] Esperando conexiones...");
                    Socket socket = serverSocket.accept();
                    System.out.println("🔗 [CONNECTION] Nueva conexión desde: " + socket.getInetAddress() + ":" + socket.getPort());

                    handleClientConnection(socket);
                } catch (Exception e) {
                    System.err.println("❌ [SERVER ERROR] Error en el servidor: " + e.getMessage());
                    e.printStackTrace();
                }
            }
        } catch (Exception e) {
            System.err.println("❌ [SERVER FATAL] No se pudo iniciar el servidor: " + e.getMessage());
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

            System.out.println("📥 [READING] Leyendo mensaje HL7...");

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
                System.err.println("⚠️ [WARNING] Mensaje recibido sin carácter de inicio MLLP (0x0B)");
                return;
            }

            String hl7Message = messageBuffer.toString(StandardCharsets.UTF_8.name());
            System.out.println("📨 [MESSAGE RECEIVED] Mensaje HL7 recibido:\n" + hl7Message);

            // Procesar y responder ACK
            String ackMessage = buildAckMessage(hl7Message);
            System.out.println("📤 [SENDING ACK] Preparando ACK:\n" + ackMessage);

            // Enviar ACK con encapsulamiento MLLP
            outputStream.write(START_BLOCK);
            outputStream.write(ackMessage.getBytes(StandardCharsets.UTF_8));
            outputStream.write(END_BLOCK);
            outputStream.write(CARRIAGE_RETURN);
            outputStream.flush();

            System.out.println("✅ [ACK SENT] ACK enviado correctamente");

        } catch (Exception e) {
            System.err.println("❌ [CONNECTION ERROR] Error en la conexión: " + e.getMessage());
            e.printStackTrace();
        } finally {
            try {
                socket.close();
                System.out.println("🔌 [CONNECTION CLOSED] Conexión cerrada");
            } catch (IOException e) {
                System.err.println("⚠️ [WARNING] Error al cerrar la conexión: " + e.getMessage());
            }
        }
    }

    private String buildAckMessage(String hl7Message) {
        try {
            String[] segments = hl7Message.split("\\r");
            String mshSegment = segments[0]; // Asumimos que MSH es el primer segmento

            String[] mshFields = mshSegment.split("\\|");
            if (mshFields.length < 12) {
                throw new IllegalArgumentException("MSH segmento no válido");
            }

            // Construir MSH para el ACK
            String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMAT);
            StringBuilder ackBuilder = new StringBuilder();

            // Segmento MSH del ACK
            ackBuilder.append("MSH|^~\\&|")
                    .append(mshFields[4]).append("|")  // Receiving Application
                    .append(mshFields[5]).append("|")  // Receiving Facility
                    .append(mshFields[2]).append("|")  // Sending Application
                    .append(mshFields[3]).append("|")  // Sending Facility
                    .append(timestamp).append("|")      // Fecha/Hora del ACK
                    .append("|ACK^A01|")               // Tipo de mensaje
                    .append(mshFields[9]).append("|")  // ID del mensaje original
                    .append("P|2.5").append(CARRIAGE_RETURN);

            // Segmento MSA
            ackBuilder.append("MSA|AA|")                 // AA = Aceptación, AE = Error, AR = Rechazo
                    .append(mshFields[9])              // ID del mensaje original
                    .append(CARRIAGE_RETURN);

            return ackBuilder.toString();

        } catch (Exception e) {
            System.err.println("⚠️ [WARNING] Error al construir ACK: " + e.getMessage());

            // ACK genérico en caso de error
            return "MSH|^~\\&|ACK_SERVER|||" + LocalDateTime.now().format(TIMESTAMP_FORMAT) +
                    "||ACK^A01||P|2.5" + CARRIAGE_RETURN +
                    "MSA|AE||Error procesando mensaje" + CARRIAGE_RETURN;
        }
    }
}