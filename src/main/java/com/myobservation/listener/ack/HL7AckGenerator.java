package com.myobservation.listener.ack;

import org.springframework.stereotype.Component; // O @Service, si lo vas a inyectar

import java.time.LocalDateTime;

import static com.myobservation.listener.utils.ProtocolConstants.CARRIAGE_RETURN;
import static com.myobservation.listener.utils.ProtocolConstants.TIMESTAMP_FORMAT;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory; // Importa el Logger

@Component // O @Service
public class HL7AckGenerator {

    private static final Logger logger = LoggerFactory.getLogger(HL7AckGenerator.class);

    // Este es el método que necesitas. Asegúrate de que solo este exista o que
    // el otro (sin parámetros) no sea llamado por tu MLLPConnectionHandler.
    public String buildAckMessage(String hl7Message, String ackStatus, String ackErrorDetail) {
        try {
            String[] segments = hl7Message.split("\\r");
            // Asegúrate de que el mensaje no esté vacío o sea muy corto antes de intentar parsear MSH
            if (segments.length == 0 || !segments[0].startsWith("MSH")) {
                logger.warn("[WARNING] Mensaje HL7 recibido sin segmento MSH inicial. Generando ACK de error.");
                // Fallback a un ACK de error si el mensaje es completamente irreconocible
                return "MSH|^~\\&|ACK_SERVER|||" + LocalDateTime.now().format(TIMESTAMP_FORMAT) +
                        "||ACK^A01||P|2.5" + CARRIAGE_RETURN +
                        "MSA|AR||Mensaje recibido no es un HL7 MSH valido" + CARRIAGE_RETURN;
            }

            String mshSegment = segments[0];
            String[] mshFields = mshSegment.split("\\|");
            // Algunas validaciones básicas del MSH para evitar IndexOutOfBoundsException
            if (mshFields.length < 10) { // MSH-9 (Tipo de mensaje) e MSH-10 (ID de Control de Mensaje) son cruciales para el ACK
                logger.warn("[WARNING] MSH segmento incompleto para ACK: {}. Campos insuficientes.", mshSegment);
                // Fallback a un ACK de error si el MSH es muy incompleto
                return "MSH|^~\\&|ACK_SERVER|||" + LocalDateTime.now().format(TIMESTAMP_FORMAT) +
                        "||ACK^A01||P|2.5" + CARRIAGE_RETURN +
                        "MSA|AE||MSH incompleto para generar ACK" + CARRIAGE_RETURN;
            }

            String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMAT);

            StringBuilder ackBuilder = new StringBuilder();
            ackBuilder.append("MSH|^~\\&|")
                    .append(mshFields.length > 4 ? mshFields[4] : "UNKNOWN_APP").append("|")  // Sending App (original Receiving App)
                    .append(mshFields.length > 5 ? mshFields[5] : "UNKNOWN_FAC").append("|")  // Sending Facility (original Receiving Facility)
                    .append(mshFields.length > 2 ? mshFields[2] : "ACK_SERVER").append("|")  // Receiving App (original Sending App)
                    .append(mshFields.length > 3 ? mshFields[3] : "ACK_FAC").append("|")  // Receiving Facility (original Sending Facility)
                    .append(timestamp).append("|")      // Fecha/Hora del ACK
                    .append("|ACK^").append(mshFields.length > 8 ? mshFields[8] : "A01").append("|") // Tipo de mensaje ACK para el tipo de mensaje original (ej. ACK^A01 para ADT^A01)
                    .append(mshFields.length > 9 ? mshFields[9] : "UNKNOWN_ID").append("|")  // ID del mensaje original
                    .append("P|2.5").append(CARRIAGE_RETURN); // Tipo de procesamiento y versión HL7

            // Segmento MSA
            ackBuilder.append("MSA|")
                    .append(ackStatus).append("|")     // AA = Aceptación, AE = Error, AR = Rechazo
                    .append(mshFields[9]);             // ID del mensaje original

            if (ackErrorDetail != null && !ackErrorDetail.isEmpty()) {
                ackBuilder.append("|").append(ackErrorDetail); // Detalle del error
            }
            ackBuilder.append(CARRIAGE_RETURN);

            return ackBuilder.toString();

        } catch (Exception e) {
            logger.error("❌ [ACK GENERATOR FATAL ERROR] Error crítico al construir ACK: {}", e.getMessage(), e);
            // ACK genérico de error si falló la construcción del ACK mismo
            return "MSH|^~\\&|ACK_SERVER|||" + LocalDateTime.now().format(TIMESTAMP_FORMAT) +
                    "||ACK^A01||P|2.5" + CARRIAGE_RETURN +
                    "MSA|AE||Error interno al generar ACK: " + (e.getMessage() != null ? e.getMessage().replace('\r', ' ').replace('\n', ' ') : "Desconocido") + CARRIAGE_RETURN;
        }
    }
}