package com.myobservation.storage.model.entity;


import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

/**
 * Entidad de la base de datos y definición de sus campos DDL
 */
@Entity
@Table(name = "HL7_MESSAGE")
public class HL7Message {

    // Id lógico de la tabla tipo incremental
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)

    @Column(name = "id_internal")
    private long id;

    @NotBlank
    @Column(name = "message_raw", length = 10000)
    private String messageRaw;

    @Column(name = "his_sender", nullable = false, length = 100)
    private String sender; // Entidad que envía HIS

    private String receiver; // Entidad que recibe HIS

    @Column(name = "message_type", nullable = false, length = 7)
    @NotBlank
    private String messageType; // Según el tipo de evento ej; ADT^A01

    @NotBlank
    @Column(name = "ack_status", nullable = false, length = 2)
    private String ackStatus; // AA,AE,AR

    @Column(name = "ack_error", nullable = true)
    private String ackError;  // Descripción de error en caso de fallo

    private LocalDateTime createdAt = LocalDateTime.now(); // Auditoria de fecha/hora

    // GETTERS & SETTERS

    public long getId() {
        return id;
    }

    public String getMessage() {
        return messageRaw;
    }

    public void setMessage(String messageRaw) {
        this.messageRaw = messageRaw;
    }

    public String getSender() {
        return sender;
    }

    public void setSender(String sender) {
        this.sender = sender;
    }

    public String getReceiver() {
        return receiver;
    }

    public void setReceiver(String receiver) {
        this.receiver = receiver;
    }

    public String getMessageType() {
        return messageType;
    }

    public void setMessageType(String messageType) {
        this.messageType = messageType;
    }

    public String getMessageRaw() {
        return messageRaw;
    }

    public void setMessageRaw(String messageRaw) {
        this.messageRaw = messageRaw;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getAckStatus() {
        return ackStatus;
    }

    public void setAckStatus(String ackStatus) {
        this.ackStatus = ackStatus;
    }

    public String getAckError() {
        return ackError;
    }

    public void setAckError(String ackError) {
        this.ackError = ackError;
    }
}
