package com.chatterbox.lan.database;

import com.chatterbox.lan.models.Conversation;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import org.bson.types.ObjectId;

import java.util.*;
import org.bson.Document;

public class ConversationRepo {

    private final MongoCollection<Document> conversationCollection;

    public ConversationRepo() {
        MongoDatabase database = db.getDatabase();
        this.conversationCollection = database.getCollection("conversations");
    }

    public String createConversation(String name, List<String> members) {
        Document doc = new Document()
                .append("name", name)
                .append("members", members)
                .append("createdAt", new Date());
        conversationCollection.insertOne(doc);
        return doc.getObjectId("_id").toHexString();
    }

    public Conversation findDirectConversation(String firstUsername, String secondUsername) {
        if (firstUsername == null || secondUsername == null
                || firstUsername.isBlank() || secondUsername.isBlank()) {
            return null;
        }

        for (Document doc : conversationCollection.find()) {
            @SuppressWarnings("unchecked")
            List<String> members = (List<String>) doc.get("members");
            if (members == null || members.size() != 2) {
                continue;
            }

            boolean hasFirst = members.stream().anyMatch(member -> member != null && member.equalsIgnoreCase(firstUsername));
            boolean hasSecond = members.stream().anyMatch(member -> member != null && member.equalsIgnoreCase(secondUsername));
            if (!hasFirst || !hasSecond) {
                continue;
            }

            String name = doc.getString("name");
            return new Conversation(doc.getObjectId("_id").toHexString(), name, members);
        }
        return null;
    }

    // Get conversation by ID
    public Conversation getConversation(String id) {
        Document doc = conversationCollection.find(Filters.eq("_id", new ObjectId(id))).first();
        if (doc == null) return null;

        String name = doc.getString("name");
        @SuppressWarnings("unchecked")
        List<String> members = (List<String>) doc.get("members");

        return new Conversation(id, name, members);
    }


    public List<Conversation> getUserConversations(String username) {
        List<Conversation> result = new ArrayList<>();
        for (Document doc : conversationCollection.find(Filters.in("members", username))) {
            String id = doc.getObjectId("_id").toHexString();
            String name = doc.getString("name");
            @SuppressWarnings("unchecked")
            List<String> members = (List<String>) doc.get("members");
            result.add(new Conversation(id, name, members));
        }
        return result;
    }


    public void addMember(String conversationId, String username) {
        conversationCollection.updateOne(
                Filters.eq("_id", new ObjectId(conversationId)),
                new Document("$addToSet", new Document("members", username))
        );
    }


    public void removeMember(String conversationId, String username) {
        conversationCollection.updateOne(
                Filters.eq("_id", new ObjectId(conversationId)),
                new Document("$pull", new Document("members", username))
        );
    }
}
