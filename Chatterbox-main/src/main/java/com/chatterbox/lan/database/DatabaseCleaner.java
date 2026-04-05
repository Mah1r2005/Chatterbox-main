package com.chatterbox.lan.database;

public class DatabaseCleaner {
    public static void main(String[] args) {
        System.out.println("[CLEANER] Connecting to database...");
        db.connect();

        System.out.println("[CLEANER] Clearing all collections...");
        db.getDatabase().getCollection("users").deleteMany(new org.bson.Document());
        db.getDatabase().getCollection("conversations").deleteMany(new org.bson.Document());
        db.getDatabase().getCollection("messages").deleteMany(new org.bson.Document());

        System.out.println("[CLEANER] Database cleared!");
        System.out.println("[CLEANER] Now restart ServerMain to seed fresh data");

        db.close();
    }
}