package com.chatterbox.lan;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class MainApp extends Application {

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) {
        // Play the splash screen, and tell it to run loadLoginScreen() when finished
        SplashAnimation.play(stage, () -> loadLoginScreen(stage));
    }

    private void loadLoginScreen(Stage stage) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/com/chatterbox/lan/Login-view.fxml"));
            // We reuse the existing stage, just swap out the scene
            Scene scene = new Scene(root, 800, 600);
            stage.setTitle("Chatterbox - Login");
            stage.setScene(scene);
            stage.show();
        } catch (IOException e) {
            System.err.println("Failed to load Login-view.fxml");
            e.printStackTrace();
        }
    }
}
