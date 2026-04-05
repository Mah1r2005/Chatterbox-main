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
    public void start(Stage stage) throws IOException {
        // Load login page instead of chat page
        Parent root = FXMLLoader.load(getClass().getResource("/com/chatterbox/lan/Login-view.fxml"));
        Scene scene = new Scene(root, 800, 600);
        stage.setTitle("Chatterbox - Login");
        stage.setScene(scene);
        stage.show();
    }
}

// Instructions to run the application:
//1. first run the ServerMain class to start the server
//2. then run the MainApp class to start the client application
