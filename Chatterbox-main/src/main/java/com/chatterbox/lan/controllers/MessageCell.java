package com.chatterbox.lan.controllers;

import com.chatterbox.lan.models.Message;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;

public class MessageCell extends ListCell<Message> {

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

            Label usernameLabel = new Label(message.getSender().getUsername());
            usernameLabel.getStyleClass().add("username-label");

            Text messageText = new Text(message.getText());
            messageText.getStyleClass().add("message-text");

            messageBox.getChildren().addAll(usernameLabel, messageText);


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
}