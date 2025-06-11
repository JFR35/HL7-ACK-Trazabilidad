package com.myobservation.storage.service.parser;

import com.myobservation.storage.model.dto.HL7MessageDTO;
import com.myobservation.storage.model.entity.HL7Message;
import com.myobservation.storage.repository.HL7MessageRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

@Service
public class HL7ParserService {
    private final HL7MessageRepository repository;

    public HL7ParserService(HL7MessageRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public HL7MessageDTO parseHL7Message(String hl7Raw) {
        // Limpieza inicial del mensaje
        String cleanedMessage = hl7Raw.trim();
        System.out.println("[PARSER] Processing message: " + cleanedMessage);

        HL7MessageDTO dto = new HL7MessageDTO();
        dto.setMessage(cleanedMessage);

        // Inicializar con valores por defecto
        String sender = "UNKNOWN";
        String receiver = "UNKNOWN";
        String messageType = "UNKNOWN";
        String ackStatus = "AA";
        String ackErrorDetail = "";

        try {
            // Verificar si es mensaje HL7 completo (debe comenzar con MSH)
            if (cleanedMessage.startsWith("MSH")) {
                String[] segments = cleanedMessage.split("\n");
                String[] mshFields = segments[0].split("\\|");

                // Extraer campos básicos (con validación de longitud)
                sender = mshFields.length > 3 ? mshFields[3] : "UNKNOWN";
                receiver = mshFields.length > 5 ? mshFields[5] : "UNKNOWN";
                messageType = mshFields.length > 8 ? mshFields[8] : "UNKNOWN";

                // Validaciones adicionales
                if (messageType.isEmpty()) {
                    ackStatus = "AE";
                    ackErrorDetail = "Falta el tipo de evento en MSH-9";
                } else if (segments.length < 3) {
                    ackStatus = "AR";
                    ackErrorDetail = "Estructura inválida: faltan segmentos requeridos";
                }
            } else {
                // Mensaje parcial o no HL7 estándar
                ackStatus = "AR";
                ackErrorDetail = "Mensaje no comienza con MSH";

                // Intenta extraer información aunque no sea HL7 completo
                if (cleanedMessage.contains("PV1")) {
                    String[] pv1Fields = cleanedMessage.split("\\|");
                    if (pv1Fields.length > 3) {
                        String location = pv1Fields[3];
                        // Puedes asignar esta información a algún campo
                    }
                }
            }
        } catch (Exception e) {
            ackStatus = "AE";
            ackErrorDetail = "Error procesando mensaje: " + e.getMessage();
            System.err.println("[PARSER ERROR] " + e.getMessage());
        }

        // Asignar valores al DTO
        dto.setSender(sender);
        dto.setReceiver(receiver);
        dto.setMessageType(messageType);

        // Crear y guardar la entidad
        HL7Message hl7Message = new HL7Message();
        hl7Message.setMessageRaw(cleanedMessage);
        hl7Message.setSender(sender);
        hl7Message.setReceiver(receiver);
        hl7Message.setMessageType(messageType);
        hl7Message.setAckStatus(ackStatus);
        hl7Message.setAckError(ackErrorDetail);

        HL7Message savedMessage = repository.save(hl7Message);
        System.out.println("[DB SAVE SUCCESS] Mensaje guardado con ID: " + savedMessage.getId() +
                " Status: " + ackStatus);

        return dto;
    }
}