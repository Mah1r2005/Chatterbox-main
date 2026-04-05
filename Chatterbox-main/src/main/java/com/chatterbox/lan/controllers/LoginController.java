package com.chatterbox.lan.controllers;

import com.chatterbox.lan.models.User;
import com.chatterbox.lan.network.Client;
import com.chatterbox.lan.models.Event;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.IOException;

public class LoginController {
    private static final Duration LOGIN_TIMEOUT = Duration.seconds(20);
    boolean loginPending;
    Client client;
    private PauseTransition loginTimeout;

    @FXML
    private TextField usernameField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private Label errorLabel;

    @FXML
    private Button loginButton;

    @FXML
    private ProgressIndicator loginSpinner;

    @FXML
    public void initialize() {
        errorLabel.setVisible(false);
        setLoginPending(false);
    }

    @FXML
    public void onLogin() {
        if (loginPending) {
            return;
        }

        String username = usernameField.getText().trim();
        String password = passwordField.getText().trim();
        errorLabel.setVisible(false);

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
            if (client != null) {
                client.disconnect();
                client = null;
            }
            client = new Client();
            if (!client.isConnected()) {
                errorLabel.setText("Could not connect to the server. Start the server and try again.");
                errorLabel.setVisible(true);
                setLoginPending(false);
                return;
            }

            client.setListener(event -> Platform.runLater(() -> {
                switch (event.getType()) {
                    case "LOGIN_SUCCESS" -> handleLoginSuccess(event);
                    case "LOGIN_FAILED" -> handleLoginFailed(event);
                    default -> {
                        // Ignore non-login events here
                    }
                }
            }));

            setLoginPending(true);
            startLoginTimeout();
            client.login(username, password);
        } catch (Exception e) {
            setLoginPending(false);
            errorLabel.setText("Connection failed: " + e.getMessage());
            errorLabel.setVisible(true);
            e.printStackTrace();
        }
    }

    @FXML
    public void onCreateAccount() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/com/chatterbox/lan/Register-view.fxml")
            );
            Parent root = loader.load();

            Stage stage = (Stage) usernameField.getScene().getWindow();
            Scene scene = new Scene(root, 800, 600);
            stage.setScene(scene);
            stage.setTitle("Chatterbox - Create Account");
        } catch (IOException e) {
            errorLabel.setText("Failed to load registration page: " + e.getMessage());
            errorLabel.setVisible(true);
            e.printStackTrace();
        }
    }

    private void handleLoginSuccess(Event event) {
        try {
            User currentUser = buildUserFromEvent(event);
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

            stopLoginTimeout();
            setLoginPending(false);

        } catch (IOException e) {
            stopLoginTimeout();
            setLoginPending(false);
            errorLabel.setText("Failed to load chat page: " + e.getMessage());
            errorLabel.setVisible(true);
            e.printStackTrace();
        }
    }

    private void handleLoginFailed(Event event) {
        stopLoginTimeout();
        setLoginPending(false);

        Object message = event.getData("message");
        errorLabel.setText(message != null ? message.toString() : "Login failed");
        errorLabel.setVisible(true);

        if (client != null) {
            client.disconnect();
            client = null;
        }
    }

    private User buildUserFromEvent(Event event) {
        String username = event.getUsername();
        String avatarPath = (String) event.getData("avatarPath");
        User user = new User(username, avatarPath);
        user.setFirstName((String) event.getData("firstName"));
        user.setLastName((String) event.getData("lastName"));
        user.setEmail((String) event.getData("email"));
        user.setPhoneNumber((String) event.getData("phoneNumber"));
        user.setLocation((String) event.getData("location"));
        return user;
    }

    private void startLoginTimeout() {
        stopLoginTimeout();
        loginTimeout = new PauseTransition(LOGIN_TIMEOUT);
        loginTimeout.setOnFinished(event -> {
            if (!loginPending) {
                return;
            }
            if (client != null) {
                client.disconnect();
                client = null;
            }
            setLoginPending(false);
            errorLabel.setText("Login timed out. Please try again.");
            errorLabel.setVisible(true);
        });
        loginTimeout.play();
    }

    private void stopLoginTimeout() {
        if (loginTimeout != null) {
            loginTimeout.stop();
            loginTimeout = null;
        }
    }

    private void setLoginPending(boolean pending) {
        loginPending = pending;
        if (loginButton != null) {
            loginButton.setDisable(pending);
            loginButton.setText(pending ? "Logging in..." : "Login");
            loginButton.setGraphic(pending ? loginSpinner : null);
        }
        if (loginSpinner != null) {
            loginSpinner.setVisible(pending);
        }
    }
}
