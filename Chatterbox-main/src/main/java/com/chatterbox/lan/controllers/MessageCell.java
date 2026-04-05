package com.chatterbox.lan.controllers;

import com.chatterbox.lan.models.Message;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.stage.FileChooser;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import java.util.function.BooleanSupplier;

public class MessageCell extends ListCell<Message> {
    private final BooleanSupplier showSenderNames;

    public MessageCell(BooleanSupplier showSenderNames) {
        this.showSenderNames = showSenderNames;
    }

    @Override
    protected void updateItem(Message message, boolean empty) {
        super.updateItem(message, empty);

        if (empty || message == null) {
            setGraphic(null);
            setText(null);
        } else {

            HBox hbox = new HBox();

            VBox messageBox = new VBox(5);


            messageBox.getStyleClass().add("message-box");

            if (showSenderNames.getAsBoolean()) {
                Label usernameLabel = new Label(message.getSender().getUsername());
                usernameLabel.getStyleClass().add("username-label");
                messageBox.getChildren().add(usernameLabel);
            }

            if (message.isFileMessage()) {
                Label fileLabel = new Label(message.getFileName() == null ? "File" : message.getFileName());
                fileLabel.getStyleClass().add("message-file-name");
                HBox.setHgrow(fileLabel, Priority.ALWAYS);
                fileLabel.setMaxWidth(Double.MAX_VALUE);

                Button downloadButton = new Button("Save File");
                downloadButton.getStyleClass().add("message-file-button");
                downloadButton.setOnAction(event -> saveFile(message));

                HBox fileRow = new HBox(8, fileLabel, downloadButton);
                fileRow.setAlignment(Pos.CENTER_LEFT);
                messageBox.getChildren().add(fileRow);
            } else {
                Text messageText = new Text(message.getText());
                messageText.getStyleClass().add("message-text");
                messageBox.getChildren().add(messageText);
            }


            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);

            if (message.getSender().isMe()) {

                messageBox.getStyleClass().add("message-me");
                hbox.getChildren().addAll(spacer, messageBox);
                hbox.setAlignment(Pos.CENTER_RIGHT);
            } else {

                messageBox.getStyleClass().add("message-other");
                hbox.getChildren().addAll(messageBox, spacer);
                hbox.setAlignment(Pos.CENTER_LEFT);
            }

            setGraphic(hbox);
            setText(null);
        }
    }

    private void saveFile(Message message) {
        if (message.getFileData() == null || message.getFileData().length == 0 || getScene() == null) {
            return;
        }

        FileChooser chooser = new FileChooser();
        chooser.setTitle("Save File");
        chooser.setInitialFileName(message.getFileName() == null ? "attachment" : message.getFileName());
        File file = chooser.showSaveDialog(getScene().getWindow());
        if (file == null) {
            return;
        }

        try {
            Files.write(file.toPath(), message.getFileData());
        } catch (IOException e) {
            throw new RuntimeException("Failed to save file", e);
        }
    }
}
