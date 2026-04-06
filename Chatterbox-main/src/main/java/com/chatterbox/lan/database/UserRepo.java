package com.chatterbox.lan.database;

import com.chatterbox.lan.models.User;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;

import java.util.ArrayList;
import java.util.List;

public class UserRepo {
    private final MongoCollection<Document> userCollection;

    public UserRepo() {
        MongoDatabase database = db.getDatabase();
        this.userCollection = database.getCollection("users");
    }

    // Get all users
    public List<User> getAllUsers() {
        List<User> users = new ArrayList<>();

        for (Document doc : userCollection.find()) {
            String id = doc.getObjectId("_id").toString();
            String username = doc.getString("username");
            String avatarPath = doc.getString("avatarPath");
            users.add(new User(id, username, avatarPath));
        }

        return users;
    }

    // Save or update a user
    public String saveUser(User user) {
        Document existingUser = userCollection.find(new Document("username", user.getUsername())).first();

        if (existingUser != null) {
            //Update existing user
            userCollection.updateOne(
                    new Document("username", user.getUsername()),
                    new Document("$set", new Document("avatarPath", user.getAvatarPath())
                            .append("password", user.getPassword())
                            .append("firstName", user.getFirstName())
                            .append("lastName", user.getLastName())
                            .append("email", user.getEmail())
                            .append("phoneNumber", user.getPhoneNumber())
                            .append("location", user.getLocation()))
            );
            return existingUser.getObjectId("_id").toString();
        } else {
            // Insert new user
            Document doc = new Document("username", user.getUsername())
                    .append("avatarPath", user.getAvatarPath())
                    .append("password", user.getPassword())
                    .append("firstName", user.getFirstName())
                    .append("lastName", user.getLastName())
                    .append("email", user.getEmail())
                    .append("phoneNumber", user.getPhoneNumber())
                    .append("location", user.getLocation());
            userCollection.insertOne(doc);
            return doc.getObjectId("_id").toString();
        }
    }

    // Get user by username
    public User getUserByUsername(String username) {
        Document doc = userCollection.find(new Document("username", username)).first();

        if (doc != null) {
            String id = doc.getObjectId("_id").toString();
            String avatarPath = doc.getString("avatarPath");
            String password = doc.getString("password");
            User x = new User(id, username, avatarPath);
            x.setPassword(password);
            x.setFirstName(doc.getString("firstName"));
            x.setLastName(doc.getString("lastName"));
            x.setEmail(doc.getString("email"));
            x.setPhoneNumber(doc.getString("phoneNumber"));
            x.setLocation(doc.getString("location"));
            return x;
        }

        return null;
    }

    public boolean usernameExists(String username) {
        return userCollection.find(new Document("username", username)).first() != null;
    }


    // Delete user by username
    public void deleteUser(String username) {
        userCollection.deleteOne(new Document("username", username));
    }
}
