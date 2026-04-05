package com.chatterbox.lan.network;

import com.chatterbox.lan.database.*;
import com.chatterbox.lan.utils.env;

import java.net.*;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;

public class ServerMain {
    public ConcurrentHashMap<String, SocketWrapper> connectedClients;
    private EventHandler eventHandler;

    public ServerMain() {
        connectedClients = new ConcurrentHashMap<>();
        // Connect to database
        db.connect();
        initEventHandler();
        // Seed database
//        System.out.println("[SERVER] Initializing database...");
//        seedDatabaseIfEmpty();


        try (ServerSocket serverSocket = new ServerSocket(env.getPORT())) {
            System.out.println("[SERVER] Running on port " + env.getPORT());
            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("[SERVER] New connection: " + clientSocket.getInetAddress().getHostAddress());
                SocketWrapper socketWrapper = new SocketWrapper(clientSocket);
                ServerThread serverThread = new ServerThread(socketWrapper, connectedClients, eventHandler);
                Thread thread = new Thread(serverThread, "server-client-" + socketWrapper.getRemoteAddress());
                thread.start();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    private void initEventHandler() {
        MessageRepo messageRepo = new MessageRepo();
        UserRepo userRepo = new UserRepo();
        ConversationRepo conversationRepo = new ConversationRepo();

        eventHandler = new EventHandler(connectedClients, messageRepo, userRepo, conversationRepo);
    }


    private void seedDatabaseIfEmpty() {
        try {
            UserRepo userRepo = new UserRepo();
            int userCount = userRepo.getAllUsers().size();

            System.out.println("[SERVER] Checking database... Found " + userCount + " users");

            if (userCount == 0) {
                System.out.println("[SERVER] Database is empty, seeding test data...");
                DatabaseSeeder.seedDatabase();

                // Verify seeding was successful
                int newUserCount = userRepo.getAllUsers().size();
                System.out.println("[SERVER] Seeding complete. Now have " + newUserCount + " users");

                if (newUserCount == 0) {
                    System.err.println("[SERVER] WARNING: Seeding failed - no users were created!");
                }
            } else {
                System.out.println("[SERVER] Database already has data, skipping seed");
            }
        } catch (Exception e) {
            System.err.println("[SERVER] Error during seeding: " + e.getMessage());
            e.printStackTrace();
        }
    }


    public static void main(String[] args) {
        new ServerMain();
    }
}
