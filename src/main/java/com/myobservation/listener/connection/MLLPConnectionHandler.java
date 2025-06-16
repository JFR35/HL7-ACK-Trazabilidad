package com.myobservation.listener.connection;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

import com.myobservation.listener.ack.HL7AckGenerator;
import com.myobservation.storage.model.dto.HL7MessageDTO; // <-- Si quieres usar el DTO de retorno

import static com.myobservation.listener.utils.ProtocolConstants.*;

import com.myobservation.storage.service.HL7ParserService;
import org.slf4j.Logger; // Importa Logger
import org.slf4j.LoggerFactory; // Importa LoggerFactory

// @Component // Ojo: Si es un nuevo hilo por cada conexión, no puede ser un @Component directo.
// Mejor inyectar sus dependencias en MLLPServer y pasarlas al constructor
// de MLLPConnectionHandler cuando se crea cada instancia.
// Mantendremos MLLPConnectionHandler como una clase normal instanciada por MLLPServer
// pero sus dependencias serán inyectadas por Spring.

public class MLLPConnectionHandler implements Runnable {

    private static final Logger logger = LoggerFactory.getLogger(MLLPConnectionHandler.class);

    private final Socket socket;
    private final HL7AckGenerator ackGenerator;
    private final HL7ParserService parserService; // <-- Inyectar HL7ParserService

    // Constructor actualizado para recibir parserService
    public MLLPConnectionHandler(Socket socket, HL7AckGenerator ackGenerator, HL7ParserService parserService) {
        this.socket = socket;
        this.ackGenerator = ackGenerator;
        this.parserService = parserService; // Asignar el servicio
    }

    @Override
    public void run() {
        String hl7Message = null;
        String ackStatus = "AE"; // Default a Error
        String ackErrorDetail = "Error desconocido";

        try (InputStream inputStream = socket.getInputStream();
             OutputStream outputStream = socket.getOutputStream()) {

            logger.info("[READING] Procesando conexión desde: {}", socket.getInetAddress());

            // Procesar mensaje HL7
            hl7Message = readHL7Message(inputStream); // Ahora readHL7Message lanza IOException
            if (hl7Message == null || hl7Message.trim().isEmpty()) {
                ackStatus = "AR"; // Mensaje rechazado (vacío o mal formado MLLP)
                ackErrorDetail = "Mensaje HL7 vacío o mal formado en MLLP";
                logger.warn("[WARNING] Mensaje HL7 recibido está vacío o es nulo después del framing.");
                sendAck(outputStream, ackGenerator.buildAckMessage(hl7Message != null ? hl7Message : "", ackStatus, ackErrorDetail));
                return;
            }

            logger.info("[MESSAGE RECEIVED] Mensaje HL7:\n{}", hl7Message);

            // --- ¡Aquí está la integración con la persistencia! ---
            try {
                HL7MessageDTO processedDto = parserService.parseHL7Message(hl7Message);
                ackStatus = processedDto.getAckStatus(); // Obtener el estado real del ACK del servicio de parser
                ackErrorDetail = processedDto.getAckError(); // Obtener el detalle del error si lo hay
                logger.info("[DB PERSISTENCE] Mensaje HL7 procesado y persistido. Status: {}", ackStatus);
            } catch (Exception e) {
                logger.error("[PERSISTENCE ERROR] Error al parsear o persistir el mensaje HL7: {}", e.getMessage(), e);
                ackStatus = "AE"; // Indicar error en ACK por fallo de procesamiento/persistencia
                ackErrorDetail = "Error interno de procesamiento: " + e.getMessage();
                // Limpiar el detalle de error para el ACK (evitar caracteres especiales)
                ackErrorDetail = ackErrorDetail.length() > 200 ? ackErrorDetail.substring(0, 200) + "..." : ackErrorDetail;
                ackErrorDetail = ackErrorDetail.replace('\r', ' ').replace('\n', ' '); // Limpiar saltos de línea
            }
            // --- Fin de la integración ---

            // Enviar ACK con el estado y detalle obtenidos del procesamiento
            String ackMessage = ackGenerator.buildAckMessage(hl7Message, ackStatus, ackErrorDetail);
            sendAck(outputStream, ackMessage);

        } catch (IOException e) { // Atrapar IOException aquí para problemas de socket/stream
            logger.error("[CONNECTION ERROR] Error de E/S en la conexión: {}", e.getMessage());
        } catch (Exception e) { // Capturar cualquier otra excepción no esperada
            logger.error("[UNEXPECTED ERROR] Error inesperado en MLLPConnectionHandler: {}", e.getMessage(), e);
            // Intentar enviar un ACK de error genérico si es posible
            try {
                if (socket != null && !socket.isClosed()) {
                    OutputStream os = socket.getOutputStream();
                    String genericErrorAck = ackGenerator.buildAckMessage(hl7Message != null ? hl7Message : "", "AE", "Error inesperado del servidor.");
                    sendAck(os, genericErrorAck);
                }
            } catch (IOException ioE) {
                logger.error("Error al enviar ACK de error genérico: {}", ioE.getMessage());
            }
        } finally {
            closeSocket();
        }
    }

    private String readHL7Message(InputStream inputStream) throws IOException {
        ByteArrayOutputStream messageBytes = new ByteArrayOutputStream();
        boolean startBlockFound = false;
        int bytesRead;

        while ((bytesRead = inputStream.read()) != -1) {
            char currentChar = (char) bytesRead;

            if (!startBlockFound && currentChar == START_BLOCK) {
                startBlockFound = true;
                continue;
            }

            if (startBlockFound) {
                if (currentChar == END_BLOCK) {
                    // Consume any trailing Carriage Return after END_BLOCK (ETXCR)
                    if (inputStream.available() > 0 && (char)inputStream.read() == CARRIAGE_RETURN) {
                        // CR consumed
                    }
                    return messageBytes.toString(StandardCharsets.UTF_8.name());
                }
                messageBytes.write(bytesRead);
            }
        }
        logger.warn("[WARNING] Mensaje recibido sin terminador MLLP (0x1C) o bloque de inicio (0x0B).");
        return null; // O lanzar una excepción específica
    }


    private void sendAck(OutputStream outputStream, String ackMessage) throws IOException {
        outputStream.write(START_BLOCK);
        outputStream.write(ackMessage.getBytes(StandardCharsets.UTF_8));
        outputStream.write(END_BLOCK);
        outputStream.write(CARRIAGE_RETURN);
        outputStream.flush();
        logger.info("[ACK SENT] ACK enviado correctamente");
    }

    private void closeSocket() {
        try {
            if (socket != null && !socket.isClosed()) {
                socket.close();
                logger.info("[CONNECTION CLOSED] Conexión cerrada");
            }
        } catch (IOException e) {
            logger.error("[WARNING] Error al cerrar la conexión: {}", e.getMessage());
        }
    }
}