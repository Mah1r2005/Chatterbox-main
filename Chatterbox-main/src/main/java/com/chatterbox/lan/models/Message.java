package com.chatterbox.lan.models;

import java.io.Serializable;
import java.time.LocalDateTime;

public class Message implements Serializable {
    private  static final long serialVersionUID = 1L;
    private static final String TYPE_TEXT = "TEXT";
    private static final String TYPE_FILE = "FILE";
    private User sender;
    private User receiver;
    private String text;
    private String id;
    private LocalDateTime timestamp;
    private String conversationId;
    private String messageType;
    private String fileName;
    private byte[] fileData;


    public Message(User user, String text,String conversationId ) {
        this.sender = user;
        this.text = text;
        this.conversationId = conversationId;
        this.timestamp = LocalDateTime.now();
        this.messageType = TYPE_TEXT;
    }
    public Message(User sender, User receiver, String text,String conversationId ,LocalDateTime timestamp) {
        this.receiver = receiver;
        this.sender = sender;
        this.text = text;
        this.timestamp = timestamp;
        this.conversationId = conversationId;
        this.messageType = TYPE_TEXT;
    }
    public Message(String id, User sender, User reciever, String text,String conversationId ,LocalDateTime timestamp) {
        this.id = id;
        this.receiver = reciever;
        this.sender = sender;
        this.text = text;
        this.timestamp = timestamp;
        this.conversationId = conversationId;
        this.messageType = TYPE_TEXT;
    }

    public Message(User sender, String fileName, byte[] fileData, String conversationId) {
        this.sender = sender;
        this.fileName = fileName;
        this.fileData = fileData;
        this.conversationId = conversationId;
        this.timestamp = LocalDateTime.now();
        this.messageType = TYPE_FILE;
        this.text = fileName;
    }

    public Message(String id, User sender, User receiver, String text, String conversationId, LocalDateTime timestamp,
                   String messageType, String fileName, byte[] fileData) {
        this.id = id;
        this.receiver = receiver;
        this.sender = sender;
        this.text = text;
        this.conversationId = conversationId;
        this.timestamp = timestamp;
        this.messageType = messageType == null || messageType.isBlank() ? TYPE_TEXT : messageType;
        this.fileName = fileName;
        this.fileData = fileData;
    }

    public String getConversationId() {
        return conversationId;
    }

    public void setConversationId(String conversationId) {
        this.conversationId = conversationId;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }


    public User getReceiver() {
        return receiver;
    }

    public void setReceiver(User receiver) {
        this.receiver = receiver;
    }

    public User getSender() {
        return sender;
    }

    public void setSender(User sender) {
        this.sender = sender;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public String getMessageType() {
        return messageType;
    }

    public void setMessageType(String messageType) {
        this.messageType = messageType;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public byte[] getFileData() {
        return fileData;
    }

    public void setFileData(byte[] fileData) {
        this.fileData = fileData;
    }

    public boolean isFileMessage() {
        return TYPE_FILE.equals(messageType);
    }
}
