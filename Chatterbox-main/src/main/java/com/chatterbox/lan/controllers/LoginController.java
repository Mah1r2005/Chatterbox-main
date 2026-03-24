package com.chatterbox.lan.controllers;

import com.chatterbox.lan.models.User;
import com.chatterbox.lan.network.Client;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.io.IOException;

public class LoginController {
    boolean loginPending;
    Client client;

    @FXML
    private TextField usernameField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private Label errorLabel;

    @FXML
    public void initialize() {
        errorLabel.setVisible(false);
    }

    @FXML
    public void onLogin() {
        String username = usernameField.getText().trim();
        String password = passwordField.getText().trim();

        if (username.isEmpty()) {
            errorLabel.setText("Please enter a username");
            errorLabel.setVisible(true);
            return;
        }

        if (password.isEmpty()) {
            errorLabel.setText("Please enter a password");
            errorLabel.setVisible(true);
            return;
        }

        try {
            client = new Client();

            client.setListener(event -> Platform.runLater(() -> {
                switch (event.getType()) {
                    case "LOGIN_SUCCESS" -> handleLoginSuccess(username);
                    case "LOGIN_FAILED" -> handleLoginFailed(event);
                    default -> {
                        // Ignore non-login events here
                    }
                }
            }));

            loginPending = true;
            client.login(username, password);
        } catch (Exception e) {
            errorLabel.setText("Connection failed: " + e.getMessage());
            errorLabel.setVisible(true);
            e.printStackTrace();
        }
    }
    private void handleLoginSuccess(String username) {
        try {
            User currentUser = new User(username, "/avatars/avatar1.png");
            currentUser.setMe(true);

            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/com/chatterbox/lan/ChatPage-view.fxml")
            );
            Parent root = loader.load();

            ChatPageController chatController = loader.getController();
            chatController.setCurrentUser(currentUser, client);

            Stage stage = (Stage) usernameField.getScene().getWindow();
            Scene scene = new Scene(root, 800, 600);
            stage.setScene(scene);
            stage.setTitle("Chatterbox");

            loginPending = false;

        } catch (IOException e) {
            loginPending = false;
            errorLabel.setText("Failed to load chat page: " + e.getMessage());
            errorLabel.setVisible(true);
            e.printStackTrace();
        }
    }

    private void handleLoginFailed(com.chatterbox.lan.models.Event event) {
        loginPending = false;

        Object message = event.getData("message");
        errorLabel.setText(message != null ? message.toString() : "Login failed");
        errorLabel.setVisible(true);

        if (client != null) {
            client.disconnect();
            client = null;
        }
    }
}