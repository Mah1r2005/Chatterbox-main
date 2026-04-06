package com.chatterbox.lan.network;

import com.chatterbox.lan.database.ConversationRepo;
import com.chatterbox.lan.database.MessageRepo;
import com.chatterbox.lan.database.UserRepo;
import com.chatterbox.lan.models.*;
import java.io.*;
import java.net.Socket;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;

public class ServerThread implements Runnable {
    private SocketWrapper clientSocket;
    private String username;
    private EventHandler eventHandler;
    public ConcurrentHashMap<String, SocketWrapper> connectedClients;

    public ServerThread(SocketWrapper clientSocket, ConcurrentHashMap<String, SocketWrapper> connectedClients, EventHandler eventHandler) {
        this.clientSocket = clientSocket;
        this.connectedClients = connectedClients;
        this.eventHandler = eventHandler;
    }


    @Override
    public void run() {
        try {
            Event loginReq = (Event) clientSocket.read();

            if ("LOGIN".equals(loginReq.getType()) || "REGISTER".equals(loginReq.getType())) {
                this.username = loginReq.getUsername();
                connectedClients.put(username, clientSocket);
                eventHandler.markUserActive(username);
                eventHandler.handleEvent(username, loginReq);
                eventHandler.broadcastUsersUpdated();
                System.out.println("[CONNECTED] " + username);
            }

            // Process requests
            Object req;
            while ((req = clientSocket.read()) != null) {
                if (req instanceof Event) {
                    Event event = (Event) req;
                    eventHandler.markUserActive(username);
                    System.out.println("[EVENT] From " + username + ": " + event.getType());
                    eventHandler.handleEvent(username, event);
                }
            }


        } catch (EOFException e) {
            // Client disconnected normally
            System.out.println("[DISCONNECTED] " + username);
        } catch (Exception e) {
            System.err.println("[ERROR] " + username + ": " + e.getMessage());
            e.printStackTrace();
        } finally {
            if (username != null) {
                eventHandler.markUserActive(username);
                connectedClients.remove(username);
                eventHandler.broadcastUsersUpdated();
            }
            try {
                clientSocket.closeConnection();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


}
