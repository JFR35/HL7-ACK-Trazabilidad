package com.myobservation.storage.controller;


import com.myobservation.storage.model.dto.HL7MessageDTO;
import com.myobservation.storage.model.entity.HL7Message;
import com.myobservation.storage.repository.HL7MessageRepository;
import com.myobservation.storage.service.parser.HL7ParserService;
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

    @Transactional
    @PostMapping
    public HL7MessageDTO saveMessage(@RequestBody String hl7Raw) {
        return parserService.parseHL7Message(hl7Raw);
    }

    @GetMapping
    public List<HL7Message> getAllMessages() {
        return repository.findAll();
    }
}
