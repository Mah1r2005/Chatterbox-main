package com.chatterbox.lan.database;

import com.chatterbox.lan.models.Message;
import com.chatterbox.lan.models.User;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Date;

public class DatabaseSeeder {

    public static void seedDatabase() {
        System.out.println("[SEEDER] Starting database initialization...");

        UserRepo userRepo = new UserRepo();
        ConversationRepo conversationRepo = new ConversationRepo();
        MessageRepo messageRepo = new MessageRepo();

        try {
            System.out.println("[SEEDER] Creating test users...");
            User jon = ensureUser(userRepo, "jon", "/avatars/avatar1.png");
            User alice = ensureUser(userRepo, "alice", "/avatars/avatar2.png");
            User bob = ensureUser(userRepo, "bob", "/avatars/avatar3.png");
            System.out.println("[SEEDER] Users ready: jon, alice, bob");

            // [CHANGED] Use existing repo API: createConversation(String, List<String>)
            System.out.println("[SEEDER] Creating conversations...");
            String conv1Id = conversationRepo.createConversation(
                    "jon-alice",
                    Arrays.asList("jon", "alice")
            );
            String conv2Id = conversationRepo.createConversation(
                    "jon-bob",
                    Arrays.asList("jon", "bob")
            );
            System.out.println("[SEEDER] Conversations created");

            // [CHANGED] Use imported Message class directly
            System.out.println("[SEEDER] Creating test messages...");

            // Messages in conversation 1 (jon & alice)
            messageRepo.saveMessage(new Message(jon, "Hey Alice!", conv1Id));
            messageRepo.saveMessage(new Message(alice, "Hi Jon! How are you?", conv1Id));
            messageRepo.saveMessage(new Message(jon, "I'm doing great! How about you?", conv1Id));
            messageRepo.saveMessage(new Message(alice, "Doing well too! Want to grab coffee?", conv1Id));

            // Messages in conversation 2 (jon & bob)
            messageRepo.saveMessage(new Message(jon, "Hey Bob!", conv2Id));
            messageRepo.saveMessage(new Message(bob, "Hey Jon! What's up?", conv2Id));
            messageRepo.saveMessage(new Message(jon, "Just finished the project", conv2Id));

            System.out.println("[SEEDER] Database seeding completed successfully!");

        } catch (Exception e) {
            System.err.println("[SEEDER] Error seeding database: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // [CHANGED] Helper to avoid duplicate user inserts on rerun
    private static User ensureUser(UserRepo userRepo, String username, String avatarPath) {
        User existing = userRepo.getUserByUsername(username);
        if (existing != null) {
            return existing;
        }
        User created = new User(username, avatarPath);
        userRepo.saveUser(created);
        return created;
    }
}