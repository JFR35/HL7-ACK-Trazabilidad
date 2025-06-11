package com.myobservation.storage.service.parser;

import com.myobservation.storage.model.dto.HL7MessageDTO;
import com.myobservation.storage.model.entity.HL7Message;
import com.myobservation.storage.repository.HL7MessageRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger; // Importa Logger
import org.slf4j.LoggerFactory; // Importa LoggerFactory

@Service
public class HL7ParserService {

    private static final Logger logger = LoggerFactory.getLogger(HL7ParserService.class);

    private final HL7MessageRepository repository;

    public HL7ParserService(HL7MessageRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public HL7MessageDTO parseHL7Message(String hl7Raw) {
        logger.debug("[PARSER] Raw input for parsing: \n{}", hl7Raw);

        // Limpieza inicial del mensaje (eliminar espacios en blanco al principio/final)
        String cleanedMessage = hl7Raw.trim();
        logger.info("[PARSER] Processing message (cleaned):\n{}", cleanedMessage);

        HL7MessageDTO dto = new HL7MessageDTO();
        dto.setMessage(cleanedMessage);

        // Inicializar con valores por defecto
        String sender = "UNKNOWN";
        String receiver = "UNKNOWN";
        String messageType = "UNKNOWN";
        String ackStatus = "AA"; // Predeterminado: Aceptación
        String ackErrorDetail = "";

        try {
            // Verificar si el mensaje HL7 comienza con MSH
            if (cleanedMessage.startsWith("MSH")) {
                // Split de segmentos robusto: maneja \r\n, \n o \r como delimitadores de segmento
                String[] segments = cleanedMessage.split("\\r?\\n|\\r");
                logger.debug("[PARSER] Number of segments found: {}", segments.length);

                if (segments.length == 0) {
                    ackStatus = "AR"; // Rechazo (mensaje vacío después de limpiar/dividir)
                    ackErrorDetail = "Mensaje HL7 vacío o sin segmentos después del procesamiento.";
                } else {
                    String mshSegment = segments[0];
                    logger.debug("[PARSER] MSH Segment: {}", mshSegment);

                    // Split de campos MSH: usar -1 para mantener cadenas vacías al final
                    // MSH.1 (delimitador) y MSH.2 (caracteres de codificación) son mshFields[1] y mshFields[2]
                    // MSH.3 es mshFields[3]
                    // ...
                    // MSH.9 es mshFields[9] (0-indexed)
                    String[] mshFields = mshSegment.split("\\|", -1);
                    logger.debug("[PARSER] MSH Fields length: {}", mshFields.length);
                    for (int i = 0; i < mshFields.length; i++) {
                        logger.debug("[PARSER] MSH Field {}: '{}'", i, mshFields[i]);
                    }

                    // Extraer campos básicos (usando índices 0-basados correctos del split)
                    // MSH.3 (Sending Application) -> mshFields[2]
                    sender = mshFields.length > 2 ? mshFields[2].trim() : "UNKNOWN";
                    // MSH.5 (Receiving Application) -> mshFields[4]
                    receiver = mshFields.length > 4 ? mshFields[4].trim() : "UNKNOWN";
                    // MSH.9 (Message Type) -> mshFields[8]
                    messageType = mshFields.length > 8 ? mshFields[8].trim() : "UNKNOWN"; // <--- ESTE ES EL CAMPO MSH.9

                    logger.debug("[PARSER] Extracted: Sender='{}', Receiver='{}', MessageType='{}'", sender, receiver, messageType);
                    logger.debug("[PARSER] Is messageType empty? {}", messageType.isEmpty());

                    // Validaciones HL7 del mensaje recibido para determinar el ACK
                    if (messageType.isEmpty() || "UNKNOWN".equals(messageType)) {
                        ackStatus = "AE"; // Application Error
                        ackErrorDetail = "MSH-9 (Tipo de mensaje) está vacío o es desconocido.";
                    } else if (segments.length < 2) { // Un mensaje HL7 válido tiene al menos 2 segmentos (MSH + un segmento de datos/evento)
                        ackStatus = "AR"; // Application Reject
                        ackErrorDetail = "Estructura HL7 inválida: mensaje demasiado corto (menos de 2 segmentos).";
                    }
                    // Puedes añadir más validaciones aquí, ej:
                    // else if (!mshFields[9].equals("MSH0001")) { // MSH.10 Message Control ID
                    //     ackStatus = "AE";
                    //     ackErrorDetail = "MSH-10 (ID de Control de Mensaje) no válido.";
                    // }

                }
            } else {
                ackStatus = "AR"; // Application Reject
                ackErrorDetail = "Mensaje no comienza con MSH.";
            }
        } catch (Exception e) {
            ackStatus = "AE"; // Application Error (para errores inesperados durante el parsing)
            ackErrorDetail = "Error interno del parser: " + e.getMessage();
            logger.error("[PARSER ERROR] Exception during message parsing: {}", e.getMessage(), e);
        }

        // Asignar los valores finales al DTO
        dto.setSender(sender);
        dto.setReceiver(receiver);
        dto.setMessageType(messageType);
        dto.setAckStatus(ackStatus);
        dto.setAckError(ackErrorDetail);
        logger.debug("[PARSER] Final DTO status before saving: AckStatus='{}', AckErrorDetail='{}'", ackStatus, ackErrorDetail);

        // Crear y guardar la entidad HL7Message en la base de datos
        HL7Message hl7MessageEntity = new HL7Message();
        hl7MessageEntity.setMessageRaw(cleanedMessage);
        hl7MessageEntity.setSender(sender);
        hl7MessageEntity.setReceiver(receiver);
        hl7MessageEntity.setMessageType(messageType);
        hl7MessageEntity.setAckStatus(ackStatus);
        hl7MessageEntity.setAckError(ackErrorDetail);

        HL7Message savedMessage = repository.save(hl7MessageEntity);
        logger.info("[DB SAVE SUCCESS] Message saved with ID: {} Status: {}", savedMessage.getId(), ackStatus);

        return dto;
    }
}