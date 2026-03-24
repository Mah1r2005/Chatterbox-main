package com.chatterbox.lan.network;

import com.chatterbox.lan.models.*;
import com.chatterbox.lan.utils.env;
import java.io.*;
import java.util.List;


public class Client {
    private SocketWrapper socketWrapper;
    private String username;

    private volatile Listener listener;



    public interface Listener {
        void onResponseReceived(Event event);
    }

    public Client() {
        try {
            int PORT = env.getPORT();
            this.socketWrapper = new SocketWrapper("localhost", PORT);
            // Start listening thread
            Thread readThread = new Thread(
                    new ClientThread(socketWrapper, this),
                    "client-read-thread"
            );
            readThread.setDaemon(true);
            readThread.start();
            System.out.println("[CLIENT] Connected to server on port " + PORT);
        } catch (IOException e) {
            System.err.println("[CLIENT] Failed to connect: " + e.getMessage());
            e.printStackTrace();
        }
    }
    public void login(String username, String password) {
        this.username = username;
        Event req = new Event("LOGIN");
        req.setUsername(username);
        req.setData("password", password);
        sendRequest(req);
    }


    public void sendMessage(String conversationId, String text) {
        Event req = new Event("SEND_MESSAGE");
        req.setConversationId(conversationId);
        req.setText(text);
        sendRequest(req);
    }

    public void getMessages(String conversationId) {
        Event req = new Event("GET_MESSAGES");
        req.setConversationId(conversationId);
        sendRequest(req);
    }

    public void getConversations() {
        Event req = new Event("GET_CONVERSATIONS");
        req.setUsername(username);
        sendRequest(req);
    }
    public void createConversation(String name, List<String> members) {
        Event event = new Event("CREATE_CONVERSATION");
        event.setUsername(username);
        event.setData("name", name);
        event.setData("members", members);
        sendRequest(event);
    }

    public void requestCall(String targetUser) {
        Event event = new Event("CALL_REQUEST");
        event.setUsername(username);
        event.setData("targetUser", targetUser);
        sendRequest(event);
    }

    public void acceptCall(String targetUser) {
        Event event = new Event("CALL_ACCEPTED");
        event.setUsername(username);
        event.setData("targetUser", targetUser);
        sendRequest(event);
    }

    public void rejectCall(String targetUser) {
        Event event = new Event("CALL_REJECTED");
        event.setUsername(username);
        event.setData("targetUser", targetUser);
        sendRequest(event);
    }

    public void endCall(String targetUser) {
        Event event = new Event("CALL_ENDED");
        event.setUsername(username);
        event.setData("targetUser", targetUser);
        sendRequest(event);
    }

    public void sendCallAudio(String targetUser, byte[] audioChunk) {
        Event event = new Event("CALL_AUDIO");
        event.setUsername(username);
        event.setData("targetUser", targetUser);
        event.setData("audioChunk", audioChunk);
        sendRequest(event);
    }

    private void sendRequest(Event event) {
        try {
            if (socketWrapper != null) {
                socketWrapper.write(event);
            }
        } catch (IOException e) {
            System.err.println("[CLIENT] Error sending request: " + e.getMessage());
        }
    }

    // Called by ReadThreadClient
    public void handleIncomingEvent(Event event) {
        Listener listener = this.listener;
        if (listener != null) {
            listener.onResponseReceived(event);
        }
    }



    public void setListener(Listener listener) {
        this.listener = listener;
    }

    public void disconnect() {
        try {
            if (socketWrapper != null) {
                socketWrapper.closeConnection();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String getUsername() {
        return username;
    }
}
