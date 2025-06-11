package com.myobservation.storage.service.parser;

import com.myobservation.storage.model.dto.HL7MessageDTO;
import com.myobservation.storage.model.entity.HL7Message;
import com.myobservation.storage.repository.HL7MessageRepository;
import org.springframework.stereotype.Service;

@Service
public class HL7ParserService {
    private final HL7MessageRepository repository;

    public HL7ParserService(HL7MessageRepository repository) {
        this.repository = repository;
    }

    public HL7MessageDTO parseHL7Message (String hl7Raw) {
        String[] segments = hl7Raw.split("\n");
        String[] mshFields = segments[0].split("\\|");

        HL7MessageDTO dto = new HL7MessageDTO();
        dto.setMessage(hl7Raw);                 // Mensaje completo
        dto.setSender(mshFields[3]);            // MSH-3
        dto.setReceiver(mshFields[5]);          // MSH-5
        dto.setMessageType(mshFields[8]);       // MSH-9

        HL7Message hl7Message = new HL7Message();
        hl7Message.setMessage(dto.getMessage());
        hl7Message.setSender(dto.getSender());
        hl7Message.setReceiver(dto.getReceiver());
        hl7Message.setMessageType(dto.getMessageType());

        repository.save(hl7Message);

        return dto;
    }
}
