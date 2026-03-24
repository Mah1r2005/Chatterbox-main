package com.chatterbox.lan.models;

import java.io.Serializable;
import java.time.LocalDateTime;

public class Message implements Serializable {
    private  static final long serialVersionUID = 1L;
    private User sender;
    private User receiver;
    private String text;
    private String id;
    private LocalDateTime timestamp;
    private String conversationId;


    public Message(User user, String text,String conversationId ) {
        this.sender = user;
        this.text = text;
        this.conversationId = conversationId;
        this.timestamp = LocalDateTime.now();
    }
    public Message(User sender, User receiver, String text,String conversationId ,LocalDateTime timestamp) {
        this.receiver = receiver;
        this.sender = sender;
        this.text = text;
        this.timestamp = timestamp;
        this.conversationId = conversationId;
    }
    public Message(String id, User sender, User reciever, String text,String conversationId ,LocalDateTime timestamp) {
        this.id = id;
        this.receiver = reciever;
        this.sender = sender;
        this.text = text;
        this.timestamp = timestamp;
        this.conversationId = conversationId;
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
}
