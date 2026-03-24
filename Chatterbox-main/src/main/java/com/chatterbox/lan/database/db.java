package com.chatterbox.lan.database;

import com.mongodb.*;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;

public class db {

    private static MongoClient mongoClient;
    private static MongoDatabase database;

    private static final String DATABASE_NAME = "chatterbox";
    private static final String CONNECTION_STRING = "mongodb+srv://chatter:LANTERNBox@chatterbox.gjfwlxy.mongodb.net/?appName=Chatterbox";
    public static void connect() {
        if (mongoClient == null) {
            try {

                ServerApi serverApi = ServerApi.builder()
                        .version(ServerApiVersion.V1)
                        .build();

                MongoClientSettings settings = MongoClientSettings.builder()
                        .applyConnectionString(new ConnectionString(CONNECTION_STRING))
                        .serverApi(serverApi)
                        .build();

                mongoClient = MongoClients.create(settings);
                database = mongoClient.getDatabase(DATABASE_NAME);
                System.out.println("Connected to MongoDB database: " + DATABASE_NAME);
            }catch (MongoException e){
                System.err.println("Failed to connect to MongoDB: " + e.getMessage());
                throw new RuntimeException("MongoDB connection failed", e);
            }

        }
    }

    public static MongoDatabase getDatabase() {
        if (database == null) {
            connect();
        }
        return database;
    }

    public static void close() {
        if (mongoClient != null) {
            mongoClient.close();
            System.out.println("MongoDB connection closed.");
        }
    }
    public static void main(String[] args){
        connect();
        System.out.println("Database connection test successful!");
        close();
    }
}