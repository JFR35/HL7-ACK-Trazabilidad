package com.myobservation.listener.ack;

import java.time.LocalDateTime;

public class HL7AckGenerator {


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
