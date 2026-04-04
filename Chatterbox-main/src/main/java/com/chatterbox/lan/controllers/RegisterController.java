package com.chatterbox.lan.controllers;

import com.chatterbox.lan.models.Event;
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
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;

public class RegisterController {
    private static final String DEFAULT_AVATAR_PATH = "/Image/default.jpg";

    private Client client;
    private String selectedAvatarPath = DEFAULT_AVATAR_PATH;

    @FXML
    private TextField avatarField;

    @FXML
    private TextField usernameField;

    @FXML
    private TextField firstNameField;

    @FXML
    private TextField lastNameField;

    @FXML
    private TextField emailField;

    @FXML
    private TextField phoneField;

    @FXML
    private TextField locationField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private Label errorLabel;

    @FXML
    public void initialize() {
        avatarField.setText(selectedAvatarPath);
        errorLabel.setVisible(false);
    }

    @FXML
    public void onBrowseAvatar() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select Profile Photo");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg", "*.gif")
        );

        Stage stage = (Stage) usernameField.getScene().getWindow();
        File selectedFile = fileChooser.showOpenDialog(stage);
        if (selectedFile != null) {
            selectedAvatarPath = selectedFile.toURI().toString();
            avatarField.setText(selectedAvatarPath);
        }
    }

    @FXML
    public void onRegister() {
        String username = usernameField.getText().trim();
        String firstName = firstNameField.getText().trim();
        String lastName = lastNameField.getText().trim();
        String email = emailField.getText().trim();
        String phone = phoneField.getText().trim();
        String location = locationField.getText().trim();
        String password = passwordField.getText().trim();

        if (username.isEmpty() || firstName.isEmpty() || lastName.isEmpty()
                || email.isEmpty() || phone.isEmpty() || location.isEmpty() || password.isEmpty()) {
            showError("Please fill in all fields");
            return;
        }

        User user = new User(username, selectedAvatarPath);
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setEmail(email);
        user.setPhoneNumber(phone);
        user.setLocation(location);
        user.setPassword(password);

        try {
            client = new Client();
            if (!client.isConnected()) {
                showError("Could not connect to the server. Start the server and try again.");
                return;
            }
            client.setListener(event -> Platform.runLater(() -> {
                switch (event.getType()) {
                    case "REGISTER_SUCCESS" -> handleRegisterSuccess(event);
                    case "REGISTER_FAILED" -> handleRegisterFailed(event);
                    default -> {
                    }
                }
            }));
            client.register(user);
        } catch (Exception e) {
            showError("Registration failed: " + e.getMessage());
        }
    }

    @FXML
    public void onBackToLogin() {
        navigateToLogin();
    }

    private void handleRegisterSuccess(Event event) {
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
        } catch (IOException e) {
            showError("Failed to load chat page: " + e.getMessage());
        }
    }

    private void handleRegisterFailed(Event event) {
        Object message = event.getData("message");
        showError(message != null ? message.toString() : "Registration failed");

        if (client != null) {
            client.disconnect();
            client = null;
        }
    }

    private void navigateToLogin() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/com/chatterbox/lan/Login-view.fxml")
            );
            Parent root = loader.load();

            Stage stage = (Stage) usernameField.getScene().getWindow();
            Scene scene = new Scene(root, 800, 600);
            stage.setScene(scene);
            stage.setTitle("Chatterbox - Login");
        } catch (IOException e) {
            showError("Failed to load login page: " + e.getMessage());
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

    private void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
    }
}
