package com.chatterbox.lan.network;

import com.chatterbox.lan.database.*;
import com.chatterbox.lan.utils.env;

import java.net.*;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;

public class ServerMain {
    public ConcurrentHashMap<String, SocketWrapper> connectedClients;
    public ConcurrentHashMap<String, Long> lastActiveTimestamps;
    private EventHandler eventHandler;

    public ServerMain() {
        connectedClients = new ConcurrentHashMap<>();
        lastActiveTimestamps = new ConcurrentHashMap<>();
        // Connect to database
        db.connect();
        initEventHandler();

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

        eventHandler = new EventHandler(connectedClients, lastActiveTimestamps, messageRepo, userRepo, conversationRepo);
    }


    public static void main(String[] args) {
        new ServerMain();
    }
}
