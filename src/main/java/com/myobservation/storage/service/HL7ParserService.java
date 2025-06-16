package com.myobservation.storage.service;

import ca.uhn.hl7v2.HL7Exception;
import ca.uhn.hl7v2.HapiContext;
import ca.uhn.hl7v2.model.Message;
import ca.uhn.hl7v2.model.v25.segment.MSH;
import ca.uhn.hl7v2.parser.Parser;

import ca.uhn.hl7v2.model.v25.message.ORU_R01; // Para ORU^R01
import ca.uhn.hl7v2.model.v25.message.ADT_A01; // Para ADT^A01


import com.myobservation.storage.model.dto.HL7MessageDTO;
import com.myobservation.storage.model.entity.HL7Message;
import com.myobservation.storage.repository.HL7MessageRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Service("HL7ParserService")
public class HL7ParserService {

    private static final Logger logger = LoggerFactory.getLogger(HL7ParserService.class);

    private final HL7MessageRepository repository;
    private final HapiContext hapiContext;

    public HL7ParserService(HL7MessageRepository repository, HapiContext hapiContext) {
        this.repository = repository;
        this.hapiContext = hapiContext;
    }

    @Transactional
    public HL7MessageDTO parseHL7Message(String hl7Raw) {
        logger.debug("[PARSER] Texto plano (Raw del mensaje) input del mensaje para parsear: \n{}", hl7Raw);

        String cleanedMessage = hl7Raw.trim();
        logger.info("[PARSER] Processando mensaje (Limpiando posibles espacios en blanco):\n{}", cleanedMessage);

        HL7MessageDTO dto = new HL7MessageDTO();
        dto.setMessage(cleanedMessage);

        String sender = "UNKNOWN";
        String receiver = "UNKNOWN";
        String messageType = "UNKNOWN";
        String ackStatus = "AA";
        String ackErrorDetail = "";

        Message hl7ParsedMessage = null; // Objeto del mensaje HL7 parseado por HAPI
        MSH msh = null;

        try {
            Parser parser = hapiContext.getGenericParser();
            hl7ParsedMessage = parser.parse(cleanedMessage);
            logger.debug("[PARSER] Mensaje HL7 parseado por HAPI: {}", hl7ParsedMessage.printStructure());

            // Se castea a la clase MSH específica de la versión la version 25
            msh = (MSH) hl7ParsedMessage.get("MSH");

            sender = msh.getSendingApplication().getNamespaceID().getValue();
            receiver = msh.getReceivingApplication().getNamespaceID().getValue();

            String messageCode = msh.getMessageType().getMessageCode().getValue();
            String triggerEvent = msh.getMessageType().getTriggerEvent().getValue();
            messageType = messageCode + "^" + triggerEvent;

            String messageControlId = msh.getMessageControlID().getValue();

            logger.debug("[PARSER] Extracted (via HAPI): Sender='{}', Receiver='{}', MessageType='{}', ControlID='{}'",
                    sender, receiver, messageType, messageControlId);

            // Validaciones HL7 del mensaje recibido
            if (messageCode == null || messageCode.isEmpty() || "UNKNOWN".equals(messageCode)) {
                ackStatus = "AE";
                ackErrorDetail = "MSH-9 (Tipo de mensaje - MessageCode) está vacío o es desconocido.";
            } else if (triggerEvent == null || triggerEvent.isEmpty()) {
                ackStatus = "AE";
                ackErrorDetail = "MSH-9 (Tipo de mensaje - TriggerEvent) está vacío o es desconocido.";
            } else if (messageControlId == null || messageControlId.isEmpty()) {
                ackStatus = "AE";
                ackErrorDetail = "MSH-10 (ID de Control de Mensaje) está vacío.";
            }

            // Ahora puede castear hl7ParsedMessage a ADT_A01 si se necesita acceder
            // a segmentos específicos de esos mensajes, que no están en el MSH
            if ("ORU^R01".equals(messageType)) {
                ORU_R01 oruR01Message = (ORU_R01) hl7ParsedMessage; // <-- Casteo a tipo de mensaje específico
                logger.debug("[PARSER] Procesando mensaje de tipo ORU^R01");
            } else if ("ADT^A01".equals(messageType)) {
                ADT_A01 adtA01Message = (ADT_A01) hl7ParsedMessage; // Casteo a tipo de mensaje específico
                logger.debug("[PARSER] Procesando mensaje de tipo ADT^A01");
            }
            // Puedes añadir más else if para otros tipos de mensajes
            // else {
            //     ackStatus = "AR"; // O manejar como un tipo no soportado
            //     ackErrorDetail = "Tipo de mensaje HL7 no soportado: " + messageType;
            // }

        } catch (HL7Exception e) {
            ackStatus = "AR"; // Application Reject - Fallo en el parsing HL7v2
            ackErrorDetail = "Mensaje HL7 mal formado o no válido para el parser: " + e.getMessage();
            logger.error("[PARSER ERROR] HL7Exception durante parseo: {}", e.getMessage(), e);
        } catch (ClassCastException e) {
            // Este catch es para cuando se castea a ADT_A01 y no lo es.
            ackStatus = "AE";
            ackErrorDetail = "Error de casteo: El mensaje HL7 no es del tipo o segmento esperado para un acceso específico. " + e.getMessage();
            logger.error("[PARSER ERROR] ClassCastException durante procesamiento: {}", e.getMessage(), e);
        } catch (Exception e) {
            ackStatus = "AE"; // Application Error
            ackErrorDetail = "Error interno del parser: " + e.getMessage();
            logger.error("[PARSER ERROR] Excepción inesperada durante parseo: {}", e.getMessage(), e);
        }

        // Asignar los valores finales al DTO
        dto.setSender(sender);
        dto.setReceiver(receiver);
        dto.setMessageType(messageType);
        dto.setAckStatus(ackStatus);
        dto.setAckError(ackErrorDetail);
        logger.debug("[PARSER] Final DTO status antes del guardado: AckStatus='{}', AckErrorDetail='{}'", ackStatus, ackErrorDetail);

        // Crear y guardar la entidad HL7 en bbdd para auditoria interna
        HL7Message hl7MessageEntity = new HL7Message();
        hl7MessageEntity.setMessageRaw(cleanedMessage);
        hl7MessageEntity.setSender(sender);
        hl7MessageEntity.setReceiver(receiver);
        hl7MessageEntity.setMessageType(messageType);
        hl7MessageEntity.setAckStatus(ackStatus);
        hl7MessageEntity.setAckError(ackErrorDetail);

        HL7Message savedMessage = repository.save(hl7MessageEntity);
        logger.info("[DB SAVE SUCCESS] Mensaje Guardado con ID: {} Status: {}", savedMessage.getId(), ackStatus);

        return dto;
    }
}