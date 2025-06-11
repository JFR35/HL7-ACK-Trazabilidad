package com.myobservation.listener.connection;


import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import com.myobservation.listener.ack.HL7AckGenerator;
import static com.myobservation.listener.utils.ProtocolConstants.*;

public class MLLPConnectionHandler implements Runnable {
    private final Socket socket;
    private final HL7AckGenerator ackGenerator;

    public MLLPConnectionHandler(Socket socket, HL7AckGenerator ackGenerator) {
        this.socket = socket;
        this.ackGenerator = ackGenerator;
    }

    @Override
    public void run() {
        try (InputStream inputStream = socket.getInputStream();
             OutputStream outputStream = socket.getOutputStream()) {

            System.out.println("📥 [READING] Procesando conexión desde: " + socket.getInetAddress());

            // Procesar mensaje HL7
            String hl7Message = readHL7Message(inputStream);
            if (hl7Message == null) return;

            System.out.println("📨 [MESSAGE RECEIVED] Mensaje HL7:\n" + hl7Message);

            // Enviar ACK
            String ackMessage = ackGenerator.buildAckMessage(hl7Message);
            sendAck(outputStream, ackMessage);

        } catch (Exception e) {
            System.err.println("❌ [CONNECTION ERROR] " + e.getMessage());
            e.printStackTrace();
        } finally {
            closeSocket();
        }
    }

    private String readHL7Message(InputStream inputStream) throws IOException {
        StringBuilder messageBuffer = new StringBuilder();
        boolean startBlockFound = false;
        int bytesRead;

        while ((bytesRead = inputStream.read()) != -1) {
            char currentChar = (char) bytesRead;

            if (currentChar == START_BLOCK) {
                startBlockFound = true;
                continue;
            }
            if (startBlockFound && currentChar == END_BLOCK) {
                return messageBuffer.toString();
            }
            messageBuffer.append(currentChar);
        }
        System.err.println("⚠️ [WARNING] No se encontró el delimitador de inicio.");
        return null;
    }

    private void sendAck(OutputStream outputStream, String ackMessage) throws Exception {
        outputStream.write(START_BLOCK);
        outputStream.write(ackMessage.getBytes(StandardCharsets.UTF_8));
        outputStream.write(END_BLOCK);
        outputStream.write(CARRIAGE_RETURN);
        outputStream.flush();
        System.out.println("✅ [ACK SENT] ACK enviado correctamente");
    }

    private void closeSocket() {
        try {
            socket.close();
            System.out.println("🔌 [CONNECTION CLOSED] Conexión cerrada");
        } catch (Exception e) {
            System.err.println("⚠️ [WARNING] Error al cerrar la conexión: " + e.getMessage());
        }
    }
}
