package com.myobservation.storage.model.entity;


import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
public class HL7Message {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)

    @Column(name = "id_internal")
    private long id;

    @Column(name = "message_raw", length = 10000)
    private String messageRaw;

    private String sender;

    private String receiver;

    private String messageType;

    private String ackStatus;

    private String ackError;

    private LocalDateTime createdAt = LocalDateTime.now();

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
