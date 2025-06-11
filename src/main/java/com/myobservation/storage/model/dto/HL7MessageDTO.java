package com.myobservation.storage.model.dto;

public class HL7MessageDTO {

    private String message;
    private String sender;
    private String receiver;
    private String messageType;
    private String ackStatus;
    private String ackError;

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
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
