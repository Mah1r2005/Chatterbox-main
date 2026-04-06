package com.chatterbox.lan.models;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

public class Message implements Serializable {
    private  static final long serialVersionUID = 1L;
    private static final String TYPE_TEXT = "TEXT";
    private static final String TYPE_FILE = "FILE";
    private static final String TYPE_CALL_EVENT = "CALL_EVENT";
    private User sender;
    private User receiver;
    private String text;
    private String id;
    private LocalDateTime timestamp;
    private String conversationId;
    private String messageType;
    private String fileName;
    private byte[] fileData;
    private boolean isDeleted;
    private String replyToMessageId;
    private String replyToSenderName;
    private String replyToText;
    private Map<String, String> reactions = new LinkedHashMap<>();
    private String callIconLiteral;
    private String callAccentColor;
    private String callDetails;


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

    public String getReplyToMessageId() {
        return replyToMessageId;
    }

    public void setReplyToMessageId(String replyToMessageId) {
        this.replyToMessageId = replyToMessageId;
    }

    public String getReplyToSenderName() {
        return replyToSenderName;
    }

    public void setReplyToSenderName(String replyToSenderName) {
        this.replyToSenderName = replyToSenderName;
    }

    public String getReplyToText() {
        return replyToText;
    }

    public void setReplyToText(String replyToText) {
        this.replyToText = replyToText;
    }

    public void setDeleted(boolean deleted) {
        isDeleted = deleted;
    }
    public boolean isDeleted() {
        return isDeleted;
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

    public boolean hasReply() {
        return (replyToMessageId != null && !replyToMessageId.isBlank())
                || (replyToSenderName != null && !replyToSenderName.isBlank())
                || (replyToText != null && !replyToText.isBlank());
    }

    public Map<String, String> getReactions() {
        return reactions;
    }

    public void setReactions(Map<String, String> reactions) {
        this.reactions = reactions == null ? new LinkedHashMap<>() : new LinkedHashMap<>(reactions);
    }

    public boolean hasReactions() {
        return reactions != null && !reactions.isEmpty();
    }

    public String getCallIconLiteral() {
        return callIconLiteral;
    }

    public void setCallIconLiteral(String callIconLiteral) {
        this.callIconLiteral = callIconLiteral;
    }

    public String getCallAccentColor() {
        return callAccentColor;
    }

    public void setCallAccentColor(String callAccentColor) {
        this.callAccentColor = callAccentColor;
    }

    public String getCallDetails() {
        return callDetails;
    }

    public void setCallDetails(String callDetails) {
        this.callDetails = callDetails;
    }

    public boolean isCallEvent() {
        return TYPE_CALL_EVENT.equals(messageType);
    }

    public void markAsCallEvent() {
        this.messageType = TYPE_CALL_EVENT;
    }
}
