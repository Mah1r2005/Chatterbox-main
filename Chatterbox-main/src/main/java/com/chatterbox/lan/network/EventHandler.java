package com.chatterbox.lan.network;

import com.chatterbox.lan.database.*;
import com.chatterbox.lan.models.*;
import com.chatterbox.lan.utils.Loginout;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class EventHandler {

    private final ConcurrentHashMap<String, SocketWrapper> connectedClients;
    private final MessageRepo messageRepo;
    private final UserRepo userRepo;
    private final ConversationRepo conversationRepo;

    public EventHandler(
            ConcurrentHashMap<String, SocketWrapper> connectedClients,
            MessageRepo messageRepo,
            UserRepo userRepo,
            ConversationRepo conversationRepo
    ) {
        this.connectedClients = connectedClients;
        this.messageRepo = messageRepo;
        this.userRepo = userRepo;
        this.conversationRepo = conversationRepo;
    }

    public void handleEvent(String username, Event event) {
        try {

            switch (event.getType()) {
                case "LOGIN" -> handleLogin(username, (String) (event.getData("password")));
                case "SEND_MESSAGE" -> handleSendMessage(username, event.getConversationId(), event.getText());
                case "GET_MESSAGES" -> handleGetMessages(username, event.getConversationId());
                case "GET_CONVERSATIONS" -> handleGetConversations(username);
                case "CREATE_CONVERSATION" ->
                        handleCreateConversation(username, (String) event.getData("name"), (List<String>) event.getData("members"));
                case "CALL_REQUEST", "CALL_ACCEPTED", "CALL_REJECTED", "CALL_ENDED", "CALL_AUDIO" ->
                        forwardCallEvent(username, event);

                default -> System.err.println("[UNKNOWN Event] " + event.getType());
            }
        } catch (Exception e) {
            System.err.println("[HANDLER] Error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void handleLogin(String username, String password) throws IOException {
        System.out.println("[LOGIN] " + username);
        Event response;
        User user = userRepo.getUserByUsername(username);

        if (user == null) {
//            response = new Event("LOGIN_FAILED");
//            response.setData("message", "User not found");
            User x = new User(username);
            x.setPassword(Loginout.Hasher(password));
            userRepo.saveUser(x);
            response = new Event("LOGIN_SUCCESS");
            response.setUsername(username);

        } else if (!Loginout.ValidatePass(password, user.getPassword())) {
            response = new Event("LOGIN_FAILED");
            response.setData("message", "Invalid password");
        } else {
            response = new Event("LOGIN_SUCCESS");
            response.setUsername(username);
        }

        SocketWrapper client = connectedClients.get(username);
        if (client != null) {
            client.write(response);
        }
    }

    private void handleSendMessage(String username, String conversationId, String text) {
        User sender = userRepo.getUserByUsername(username);
        if (sender == null) {
            sender = new User(username, "/avatars/default.png");
        }
        Message message = new Message(sender, text, conversationId);

        messageRepo.saveMessage(message);
        System.out.println("[MSG] [" + conversationId + "] " + username + ": " + text);

        broadcastMessage(message);
    }

    private void handleGetMessages(String username, String conversationId) throws IOException {
        List<Message> messages = messageRepo.getMessages(conversationId);

        Event resp = new Event("MESSAGES_RESPONSE");
        resp.setConversationId(conversationId);
        resp.setData("messages", messages);

        SocketWrapper client = connectedClients.get(username);
        if (client != null) {
            client.write(resp);
        }
    }

    private void handleGetConversations(String username) throws IOException {
        // Get all conversations of the user
        List<Conversation> conversations = conversationRepo.getUserConversations(username);

        Event event = new Event("CONVERSATIONS_UPDATED");
        event.setData("conversations", conversations);

        SocketWrapper client = connectedClients.get(username);
        if (client != null) {
            client.write(event);
        }
    }

    private void handleCreateConversation(String username, String name, List<String> members) {
        String conversationId = conversationRepo.createConversation(name, members);
        System.out.println("[CREATE_CONVERSATION] Created: " + name + " with members " + members);

        Event event = new Event("NEW_CONVERSATION");
        Conversation newConversation = conversationRepo.getConversation(conversationId);

        event.setData("conversation", newConversation);
        broadcast(event, conversationId);

    }

    private void broadcastMessage(Message message) {
        String conversationId = message.getConversationId();

        Event broadcastEvent = new Event("NEW_MESSAGE");
        broadcastEvent.setConversationId(conversationId);
        broadcastEvent.setUsername(message.getSender().getUsername());
        broadcastEvent.setText(message.getText());

        broadcast(broadcastEvent, conversationId);
    }

    private void broadcast(Event event, String conversationId) {
        Conversation conversation = conversationRepo.getConversation(conversationId);
        if (conversation == null) {
            System.err.println("[BROADCAST] Conversation not found: " + conversationId);
            return;
        }

        List<String> members = conversation.getMembers();
        System.out.println("[BROADCAST] Sending to conversation members: " + members);

        for (String memberUsername : members) {
            SocketWrapper clientSocket = connectedClients.get(memberUsername);
            if (clientSocket != null) {
                try {
                    clientSocket.write(event);
                    System.out.println("[BROADCAST] Sent to " + memberUsername);
                } catch (IOException e) {
                    System.err.println("[BROADCAST] Failed to send to " + memberUsername + ": " + e.getMessage());
                }
            }
        }
    }

    private void forwardCallEvent(String username, Event event) throws IOException {
        String targetUser = (String) event.getData("targetUser");
        if (targetUser == null || targetUser.isBlank()) {
            return;
        }

        event.setUsername(username);
        SocketWrapper clientSocket = connectedClients.get(targetUser);
        if (clientSocket != null) {
            clientSocket.write(event);
        } else if ("CALL_REQUEST".equals(event.getType())) {
            Event rejected = new Event("CALL_REJECTED");
            rejected.setUsername(targetUser);
            SocketWrapper callerSocket = connectedClients.get(username);
            if (callerSocket != null) {
                callerSocket.write(rejected);
            }
        }
    }
}
