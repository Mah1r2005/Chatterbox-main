package com.chatterbox.lan.models;

import java.io.Serializable;
import java.util.List;

public class Conversation implements Serializable {
        private static final long serialVersionUID = 1L;
    private String conversationId;
    private String name;
    private List<String> members;

    public Conversation(String conversationId, String name, List<String> members) {
        this.conversationId = conversationId;
        this.name = name;
        this.members = members;
    }

    public String getConversationId() {
        return conversationId;
    }

    public void setConversationId(String conversationId) {
        this.conversationId = conversationId;
    }

    public List<String> getMembers() {
        return members;
    }

    public void setMembers(List<String> members) {
        this.members = members;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
    @Override
    public String toString() {
        return name;
    }
}

