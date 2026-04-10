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

    public boolean isConnected() {
        return socketWrapper != null;
    }

    public void login(String username, String password) {
        this.username = username;
        Event req = new Event("LOGIN");
        req.setUsername(username);
        req.setData("password", password);
        sendRequest(req);
    }

    public void register(User user) {
        this.username = user.getUsername();
        Event req = new Event("REGISTER");
        req.setUsername(user.getUsername());
        req.setData("password", user.getPassword());
        req.setData("avatarPath", user.getAvatarPath());
        req.setData("firstName", user.getFirstName());
        req.setData("lastName", user.getLastName());
        req.setData("email", user.getEmail());
        req.setData("phoneNumber", user.getPhoneNumber());
        req.setData("location", user.getLocation());
        sendRequest(req);
    }


    public void sendMessage(String conversationId, String text) {
        sendMessage(conversationId, text, null);
    }

    public void sendMessage(String conversationId, String text, Message replyTo) {
        Event req = new Event("SEND_MESSAGE");
        req.setConversationId(conversationId);
        req.setText(text);
        if (replyTo != null) {
            req.setData("replyToMessageId", replyTo.getId());
            req.setData("replyToSenderName", replyTo.getSender() == null ? null : replyTo.getSender().getUsername());
            req.setData("replyToText", buildReplyPreview(replyTo));
        }
        sendRequest(req);
    }

    public void sendFileMessage(String conversationId, String fileName, byte[] fileData) {
        Event req = new Event("SEND_MESSAGE");
        req.setConversationId(conversationId);
        req.setText(fileName);
        req.setData("messageType", "FILE");
        req.setData("fileName", fileName);
        req.setData("fileData", fileData);
        sendRequest(req);
    }

    public void sendCallLogMessage(
            String conversationId,
            String text,
            String callIconLiteral,
            String callAccentColor,
            String callDetails
    ) {
        Event req = new Event("SEND_MESSAGE");
        req.setConversationId(conversationId);
        req.setText(text);
        req.setData("messageType", "CALL_EVENT");
        req.setData("callIconLiteral", callIconLiteral);
        req.setData("callAccentColor", callAccentColor);
        req.setData("callDetails", callDetails);
        sendRequest(req);
    }

    public void unsendMessage(String conversationId, String messageId) {
        Event req = new Event("UNSEND_MESSAGE");
        req.setConversationId(conversationId);
        req.setData("messageId", messageId);
        sendRequest(req);
    }

    public void addReaction(String conversationId, String messageId, String emoji) {
        Event req = new Event("ADD_REACTION");
        req.setConversationId(conversationId);
        req.setData("messageId", messageId);
        req.setData("emoji", emoji);
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

    public void getUsers() {
        Event req = new Event("GET_USERS");
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
        sendCallControlEvent("CALL_REQUEST", targetUser, "AUDIO");
    }

    public void requestVideoCall(String targetUser) {
        sendCallControlEvent("CALL_REQUEST", targetUser, "VIDEO");
    }

    public void acceptCall(String targetUser) {
        sendCallControlEvent("CALL_ACCEPTED", targetUser, "AUDIO");
    }

    public void acceptCall(String targetUser, String callMode) {
        sendCallControlEvent("CALL_ACCEPTED", targetUser, callMode);
    }

    public void rejectCall(String targetUser) {
        sendCallControlEvent("CALL_REJECTED", targetUser, "AUDIO");
    }

    public void rejectCall(String targetUser, String callMode) {
        sendCallControlEvent("CALL_REJECTED", targetUser, callMode);
    }

    public void endCall(String targetUser) {
        sendCallControlEvent("CALL_ENDED", targetUser, "AUDIO");
    }

    public void endCall(String targetUser, String callMode) {
        sendCallControlEvent("CALL_ENDED", targetUser, callMode);
    }

    private void sendCallControlEvent(String type, String targetUser, String callMode) {
        Event event = new Event("CALL_REQUEST");
        event.setType(type);
        event.setUsername(username);
        event.setData("targetUser", targetUser);
        event.setData("callMode", callMode);
        sendRequest(event);
    }

    public void sendCallAudio(String targetUser, byte[] audioChunk) {
        Event event = new Event("CALL_AUDIO");
        event.setUsername(username);
        event.setData("targetUser", targetUser);
        event.setData("audioChunk", audioChunk);
        sendRequest(event);
    }

    public void sendCallVideoFrame(String targetUser, byte[] imageBytes) {
        Event event = new Event("CALL_VIDEO_FRAME");
        event.setUsername(username);
        event.setData("targetUser", targetUser);
        event.setData("imageBytes", imageBytes);
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

    private String buildReplyPreview(Message message) {
        if (message.isDeleted()) {
            return "Message unsent";
        }
        if (message.isFileMessage()) {
            return message.getFileName() == null || message.getFileName().isBlank()
                    ? "File"
                    : "File: " + message.getFileName();
        }
        return message.getText();
    }
}
