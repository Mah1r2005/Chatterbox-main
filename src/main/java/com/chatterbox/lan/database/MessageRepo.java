package com.chatterbox.lan.database;

import com.chatterbox.lan.models.*;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.model.Sorts;
import com.mongodb.client.result.UpdateResult;
import org.bson.BsonBinary;
import org.bson.Document;
import org.bson.types.*;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Date;
import java.util.List;
import java.util.Map;


public class MessageRepo {
    private final MongoCollection<Document> messageCollection;
    private final UserRepo userRepo;

    public MessageRepo() {
        MongoDatabase database = db.getDatabase();
        this.messageCollection = database.getCollection("messages");
        this.userRepo = new UserRepo();
    }

    public void saveMessage(Message message) {
        Date timestamp = Date.from(message.getTimestamp()
                .atZone(ZoneId.systemDefault())
                .toInstant());

        Document doc = new Document("conversationId", message.getConversationId())
                .append("sender", message.getSender().getUsername())
                .append("text", message.getText())
                .append("messageType", message.getMessageType())
                .append("fileName", message.getFileName())
                .append("fileData", message.getFileData() == null ? null : new BsonBinary(message.getFileData()))
                .append("replyToMessageId", message.getReplyToMessageId())
                .append("replyToSenderName", message.getReplyToSenderName())
                .append("replyToText", message.getReplyToText())
                .append("callIconLiteral", message.getCallIconLiteral())
                .append("callAccentColor", message.getCallAccentColor())
                .append("callDetails", message.getCallDetails())
                .append("reactions", new Document(message.getReactions()))
                .append("timestamp", timestamp)
                .append("isDeleted", false);

        messageCollection.insertOne(doc);
        Object id = doc.get("_id");
        if (id != null) {
            message.setId(id.toString());
        }
    }

    public boolean deleteMessage(String conversationId, String messageId) {
        UpdateResult result = messageCollection.updateOne(
                Filters.and(
                        Filters.eq("_id", new ObjectId(messageId)),
                        Filters.eq("conversationId", conversationId)
                ),
                Updates.combine(
                        Updates.set("isDeleted", true),
                        Updates.set("text", ""),
                        Updates.set("fileName", null),
                        Updates.set("fileData", null)
                )
        );
        return result.getModifiedCount() > 0;
    }

    public List<Message> getMessages(String conversationId) {
        List<Message> messages = new ArrayList<>();

        for (Document doc : messageCollection.find(Filters.eq("conversationId", conversationId))
                .sort(Sorts.ascending("timestamp"))) {

            String senderUsername = doc.getString("sender");
            String id = doc.getObjectId("_id").toHexString();
            String text = doc.getString("text");
            String messageType = doc.getString("messageType");
            String fileName = doc.getString("fileName");
            Binary fileBinary = doc.get("fileData", Binary.class);
            byte[] fileData = fileBinary == null ? null : fileBinary.getData();
            String replyToMessageId = doc.getString("replyToMessageId");
            String replyToSenderName = doc.getString("replyToSenderName");
            String replyToText = doc.getString("replyToText");
            String callIconLiteral = doc.getString("callIconLiteral");
            String callAccentColor = doc.getString("callAccentColor");
            String callDetails = doc.getString("callDetails");
            Document reactionsDoc = doc.get("reactions", Document.class);
            Date timestamp = doc.getDate("timestamp");

            LocalDateTime localDateTime = timestamp.toInstant()
                    .atZone(ZoneId.systemDefault())
                    .toLocalDateTime();

            User sender = userRepo.getUserByUsername(senderUsername);
            if (sender == null) {
                sender = new User(senderUsername, "/image/default.jpg");
            }

            Message message = new Message(id, sender, null, text, conversationId, localDateTime, messageType, fileName, fileData);
            message.setReplyToMessageId(replyToMessageId);
            message.setReplyToSenderName(replyToSenderName);
            message.setReplyToText(replyToText);
            message.setCallIconLiteral(callIconLiteral);
            message.setCallAccentColor(callAccentColor);
            message.setCallDetails(callDetails);
            message.setReactions(documentToReactionMap(reactionsDoc));
            Boolean deleted = doc.getBoolean("isDeleted", false);
            message.setDeleted(Boolean.TRUE.equals(deleted));
            messages.add(message);
        }

        return messages;
    }
    public Message getMessageById(String messageId) {
        Document doc = messageCollection.find(Filters.eq("_id", new ObjectId(messageId))).first();
        if (doc == null) return null;

        String senderUsername = doc.getString("sender");
        String conversationId = doc.getString("conversationId");
        String text = doc.getString("text");
        String messageType = doc.getString("messageType");
        String fileName = doc.getString("fileName");
        Binary fileBinary = doc.get("fileData", Binary.class);
        byte[] fileData = fileBinary == null ? null : fileBinary.getData();
        String replyToMessageId = doc.getString("replyToMessageId");
        String replyToSenderName = doc.getString("replyToSenderName");
        String replyToText = doc.getString("replyToText");
        String callIconLiteral = doc.getString("callIconLiteral");
        String callAccentColor = doc.getString("callAccentColor");
        String callDetails = doc.getString("callDetails");
        Document reactionsDoc = doc.get("reactions", Document.class);
        Date timestamp = doc.getDate("timestamp");

        LocalDateTime localDateTime = timestamp.toInstant()
                .atZone(ZoneId.systemDefault())
                .toLocalDateTime();

        User sender = userRepo.getUserByUsername(senderUsername);
        if (sender == null) {
            sender = new User(senderUsername, "/image/default.jpg");
        }

        Message message = new Message(messageId, sender, null, text, conversationId, localDateTime, messageType, fileName, fileData);
        message.setReplyToMessageId(replyToMessageId);
        message.setReplyToSenderName(replyToSenderName);
        message.setReplyToText(replyToText);
        message.setCallIconLiteral(callIconLiteral);
        message.setCallAccentColor(callAccentColor);
        message.setCallDetails(callDetails);
        message.setReactions(documentToReactionMap(reactionsDoc));
        return message;
    }

    public Map<String, String> updateReaction(String conversationId, String messageId, String username, String emoji) {
        if (conversationId == null || messageId == null || username == null || emoji == null) {
            return null;
        }

        Document doc = messageCollection.find(
                Filters.and(
                        Filters.eq("_id", new ObjectId(messageId)),
                        Filters.eq("conversationId", conversationId)
                )
        ).first();
        if (doc == null) {
            return null;
        }

        Map<String, String> reactions = documentToReactionMap(doc.get("reactions", Document.class));
        if (emoji.isBlank()) {
            reactions.remove(username);
        } else {
            reactions.put(username, emoji);
        }

        messageCollection.updateOne(
                Filters.eq("_id", new ObjectId(messageId)),
                Updates.set("reactions", new Document(reactions))
        );
        return reactions;
    }

    private Map<String, String> documentToReactionMap(Document reactionsDoc) {
        Map<String, String> reactions = new LinkedHashMap<>();
        if (reactionsDoc == null) {
            return reactions;
        }
        for (Map.Entry<String, Object> entry : reactionsDoc.entrySet()) {
            if (entry.getValue() instanceof String emoji && !emoji.isBlank()) {
                reactions.put(entry.getKey(), emoji);
            }
        }
        return reactions;
    }
}
