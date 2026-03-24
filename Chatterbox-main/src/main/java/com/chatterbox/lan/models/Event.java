package com.chatterbox.lan.models;

import java.io.Serial;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class Event implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;
    public enum Type {
        SEND_MESSAGE,
        GET_MESSAGES,
        LOGIN,
        LOGOUT,
        LOGIN_SUCCESS,
        LOGIN_FAILED,
        GET_CONVERSATIONS,
        CREATE_CONVERSATION,
        CONVERSATIONS_UPDATED,
        NEW_CONVERSATION,
        MESSAGES_RESPONSE,
        USERS_UPDATED,
        NEW_MESSAGE,
        CALL_REQUEST,
        CALL_ACCEPTED,
        CALL_REJECTED,
        CALL_ENDED,
        CALL_AUDIO
    }

    private Type type;
    private String username;
    private String conversationId;
    private String text;
    private final Map<String, Object> data;

    public Event(String type) {
        this.type = Type.valueOf(type);
        this.data = new HashMap<>();
    }

    public String getType() {
        return type.name();
    }

    public void setType(String type) {
        this.type = Type.valueOf(type);
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getConversationId() {
        return conversationId;
    }

    public void setConversationId(String conversationId) {
        this.conversationId = conversationId;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }
    public void setData(String key, Object value) {
        this.data.put(key, value);
    }

    public Object getData(String key) {
        return this.data.get(key);
    }
}
