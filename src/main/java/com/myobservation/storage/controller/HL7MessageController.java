package com.myobservation.storage.controller;

import com.myobservation.storage.model.dto.HL7MessageDTO;
import com.myobservation.storage.model.entity.HL7Message;
import com.myobservation.storage.repository.HL7MessageRepository;
import com.myobservation.storage.service.HL7ParserService;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/hl7messages")
public class HL7MessageController {

    private final HL7MessageRepository repository;
    private final HL7ParserService parserService;

    public HL7MessageController(HL7MessageRepository repository, HL7ParserService parserService) {
        this.repository = repository;
        this.parserService = parserService;
    }

    // Mensaje de prueba
    @PostMapping("/test")
    public HL7Message testSave() {
        HL7Message testMessage = new HL7Message();
        testMessage.setMessageRaw("Test message");
        testMessage.setSender("TEST");
        testMessage.setReceiver("TEST");
        testMessage.setMessageType("TEST");
        testMessage.setAckStatus("AA");

        return repository.save(testMessage);
    }

    // Obtener fallos en la transmisión de mensajes
    @GetMapping("/failed")
    public List<HL7Message> getFailedMessages() {
        return repository.findByAckStatusIn(List.of("AE", "AR"));
    }

    // Endpoint para persistencia en BBDD usa concepto transaccional para garantizar la atomicidad de la transacción
    @Transactional
    @PostMapping
    public HL7MessageDTO saveMessage(@RequestBody String hl7Raw) {
        return parserService.parseHL7Message(hl7Raw);
    }

    // Extraer los mensajes
    @GetMapping
    public List<HL7Message> getAllMessages() {
        return repository.findAll();
    }

    // Definir más endpoints para extraer por ID y Delete(softDelete)
}
