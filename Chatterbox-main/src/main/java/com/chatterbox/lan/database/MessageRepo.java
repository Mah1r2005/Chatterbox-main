package com.chatterbox.lan.database;

import com.chatterbox.lan.models.*;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Sorts;
import org.bson.BsonBinary;
import org.bson.Document;
import org.bson.types.Binary;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;


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
                .append("timestamp", timestamp);
        messageCollection.insertOne(doc);
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
            Date timestamp = doc.getDate("timestamp");

            LocalDateTime localDateTime = timestamp.toInstant()
                    .atZone(ZoneId.systemDefault())
                    .toLocalDateTime();

            User sender = userRepo.getUserByUsername(senderUsername);
            if (sender == null) {
                sender = new User(senderUsername, "/avatars/default.png");
            }

            Message message = new Message(id, sender, null, text, conversationId, localDateTime, messageType, fileName, fileData);
            messages.add(message);
        }

        return messages;
    }
}
