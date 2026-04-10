package com.chatterbox.lan.controllers;

import com.chatterbox.lan.models.Message;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.CustomMenuItem;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.MenuItem;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.text.TextFlow;
import javafx.stage.FileChooser;
import org.kordamp.ikonli.javafx.FontIcon;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BooleanSupplier;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Predicate;

public class MessageCell extends ListCell<Message> {
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("h:mm a");
    private final BooleanSupplier showSenderNames;
    private final Consumer<Message> onUnsend;
    private final Consumer<Message> onReply;
    private final Consumer<Message> onNavigateToReply;
    private final Predicate<Message> isHighlighted;
    private final BiConsumer<Message, String> onReact;
    private static final Map<String, String> INLINE_EMOJI_ICONS = new LinkedHashMap<>();
    private static final ReactionOption[] REACTION_OPTIONS = {
            new ReactionOption("fas-heart", "Heart"),
            new ReactionOption("fas-thumbs-up", "Like"),
            new ReactionOption("far-laugh-beam", "Laugh"),
            new ReactionOption("far-surprise", "Wow"),
            new ReactionOption("far-sad-tear", "Sad"),
            new ReactionOption("far-angry", "Angry"),
            new ReactionOption("fas-fire", "Fire"),
            new ReactionOption("fas-star", "Star")
    };

    static {
        INLINE_EMOJI_ICONS.put(":smile:", "far-smile");
        INLINE_EMOJI_ICONS.put(":laugh:", "far-laugh");
        INLINE_EMOJI_ICONS.put(":love:", "far-grin-hearts");
        INLINE_EMOJI_ICONS.put(":flirt:", "far-kiss-wink-heart");
        INLINE_EMOJI_ICONS.put(":wow:", "far-surprise");
        INLINE_EMOJI_ICONS.put(":sad:", "far-sad-tear");
        INLINE_EMOJI_ICONS.put(":angry:", "far-angry");
        INLINE_EMOJI_ICONS.put(":thumbsup:", "fas-thumbs-up");
        INLINE_EMOJI_ICONS.put(":heart:", "fas-heart");
        INLINE_EMOJI_ICONS.put(":fire:", "fas-fire");
        INLINE_EMOJI_ICONS.put(":star:", "fas-star");
        INLINE_EMOJI_ICONS.put(":spark:", "fas-magic");
    }

    public MessageCell(
            BooleanSupplier showSenderNames,
            Consumer<Message> onUnsend,
            Consumer<Message> onReply,
            Consumer<Message> onNavigateToReply,
            Predicate<Message> isHighlighted,
            BiConsumer<Message, String> onReact
    ) {
        this.showSenderNames = showSenderNames;
        this.onUnsend = onUnsend;
        this.onReply = onReply;
        this.onNavigateToReply = onNavigateToReply;
        this.isHighlighted = isHighlighted;
        this.onReact = onReact;
    }

    @Override
    protected void updateItem(Message message, boolean empty) {
        super.updateItem(message, empty);

        if (empty || message == null) {
            setGraphic(null);
            setText(null);
            return;
        }

        boolean isMine = message.getSender() != null && message.getSender().isMe();

        HBox root = new HBox(8);
        root.setMaxWidth(Region.USE_PREF_SIZE);

        VBox messageBox = new VBox(4);
        messageBox.getStyleClass().add("message-box");
        if (isHighlighted.test(message)) {
            messageBox.getStyleClass().add("message-highlighted");
        }

        if (showSenderNames.getAsBoolean() && message.getSender() != null) {
            Label usernameLabel = new Label(message.getSender().getUsername());
            usernameLabel.getStyleClass().add("username-label");
            messageBox.getChildren().add(usernameLabel);
        }

        if (message.hasReply()) {
            VBox replyBox = new VBox(2);
            replyBox.getStyleClass().add("message-reply-box");
            if (message.getReplyToMessageId() != null && !message.getReplyToMessageId().isBlank()) {
                replyBox.setOnMouseClicked(event -> onNavigateToReply.accept(message));
            }

            String senderName = message.getReplyToSenderName() == null || message.getReplyToSenderName().isBlank()
                    ? "Original message"
                    : message.getReplyToSenderName();
            Label replySender = new Label(senderName);
            replySender.getStyleClass().add("message-reply-sender");

            String previewText = message.getReplyToText() == null || message.getReplyToText().isBlank()
                    ? "Tap to view replied message"
                    : message.getReplyToText();
            Label replyText = new Label(previewText);
            replyText.setWrapText(true);
            replyText.setMaxWidth(260);
            replyText.getStyleClass().add("message-reply-text");

            replyBox.getChildren().addAll(replySender, replyText);
            messageBox.getChildren().add(replyBox);
        }

        if (message.isCallEvent()) {
            HBox callRow = new HBox(6);
            callRow.setAlignment(Pos.CENTER_LEFT);
            callRow.getStyleClass().add("message-call-row");
            VBox callTextBox = new VBox(1);
            Label callLabel = new Label(message.getText() == null ? "Audio Call" : message.getText());
            callLabel.getStyleClass().add("message-call-text");
            if (message.getCallAccentColor() != null) {
                callLabel.setStyle("-fx-text-fill: " + message.getCallAccentColor() + ";");
            }
            callTextBox.getChildren().add(callLabel);
            if (message.getCallDetails() != null && !message.getCallDetails().isBlank()) {
                Label callDetailsLabel = new Label(formatCallDetails(message.getCallDetails()));
                callDetailsLabel.getStyleClass().add("message-call-details");
                callTextBox.getChildren().add(callDetailsLabel);
            }
            callRow.getChildren().add(callTextBox);
            messageBox.getChildren().add(callRow);
            messageBox.getStyleClass().add(isMine ? "message-me" : "message-other");
        } else if (message.isDeleted()) {
            HBox deletedRow = new HBox(6);
            deletedRow.setAlignment(Pos.CENTER_LEFT);
            FontIcon deletedIcon = new FontIcon("fas-trash-alt");
            deletedIcon.getStyleClass().add("message-deleted-icon");
            Label deleted = new Label(isMine ? "You deleted this message" : "This message was deleted");
            deleted.getStyleClass().add("message-text");
            deletedRow.getChildren().addAll(deletedIcon, deleted);
            messageBox.getChildren().add(deletedRow);
            messageBox.getStyleClass().add(isMine ? "message-me" : "message-other");
        } else if (message.isFileMessage()) {
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
            TextFlow textFlow = buildMessageTextFlow(message.getText());
            messageBox.getChildren().add(textFlow);
            messageBox.getStyleClass().add(isMine ? "message-me" : "message-other");
        }

        if (message.hasReactions()) {
            HBox reactionsPane = new HBox(4);
            reactionsPane.getStyleClass().add("message-reactions");
            reactionsPane.setAlignment(Pos.CENTER_LEFT);
            reactionsPane.setMaxWidth(Region.USE_PREF_SIZE);
            reactionsPane.setFillHeight(false);
            message.getReactions().values().stream()
                    .distinct()
                    .forEach(iconLiteral -> {
                        long count = message.getReactions().values().stream().filter(iconLiteral::equals).count();
                        HBox reactionChip = new HBox(4);
                        reactionChip.getStyleClass().add("message-reaction-chip");
                        reactionChip.setAlignment(Pos.CENTER);
                        reactionChip.setMaxWidth(Region.USE_PREF_SIZE);
                        FontIcon icon = new FontIcon(iconLiteral);
                        icon.getStyleClass().add("message-reaction-icon");
                        Label countLabel = new Label(String.valueOf(count));
                        countLabel.getStyleClass().add("message-reaction-count");
                        reactionChip.getChildren().addAll(icon, countLabel);
                        Tooltip.install(reactionChip, createReactionTooltip(message, iconLiteral));
                        reactionChip.setOnMouseClicked(event -> onReact.accept(message, iconLiteral));
                        reactionsPane.getChildren().add(reactionChip);
                    });
            messageBox.getChildren().add(reactionsPane);
        }

        Label timeLabel = new Label(formatTimestamp(message));
        timeLabel.getStyleClass().add("message-time");
        HBox timeRow = new HBox();
        timeRow.setAlignment(Pos.CENTER_RIGHT);
        timeRow.getChildren().add(timeLabel);
        messageBox.getChildren().add(timeRow);

        HBox actions = new HBox(6);
        actions.getStyleClass().add("message-actions");
        actions.setAlignment(Pos.CENTER);
        actions.setOpacity(0);
        actions.setManaged(false);
        actions.setVisible(false);
        ContextMenu reactionMenu = createReactionMenu(message, actions, root);

        if (!message.isDeleted() && !message.isCallEvent()) {
            if (isMine) {
                Button unsend = createActionButton("fas-trash", "message-trash-btn");
                unsend.setOnAction(e -> onUnsend.accept(message));
                actions.getChildren().add(unsend);
            }

            Button react = createActionButton("fas-smile", "message-react-btn");
            react.setOnAction(e -> {
                if (reactionMenu.isShowing()) {
                    reactionMenu.hide();
                } else {
                    reactionMenu.show(react, javafx.geometry.Side.BOTTOM, 0, 4);
                }
            });
            actions.getChildren().add(react);

            Button reply = createActionButton("fas-reply", "message-reply-btn");
            reply.setOnAction(e -> onReply.accept(message));
            actions.getChildren().add(reply);
        }

        root.setOnMouseEntered(e -> {
            if (!actions.getChildren().isEmpty()) {
                actions.setManaged(true);
                actions.setVisible(true);
                actions.setOpacity(1);
            }
        });
        root.setOnMouseExited(e -> {
            if (reactionMenu.isShowing()) {
                return;
            }
            actions.setOpacity(0);
            actions.setManaged(false);
            actions.setVisible(false);
        });

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        root.getChildren().clear();
        if (isMine) {
            messageBox.getStyleClass().add("message-me");
            root.getChildren().addAll(spacer, actions, messageBox);
            root.setAlignment(Pos.CENTER_RIGHT);
            setAlignment(Pos.CENTER_RIGHT);
        } else {
            messageBox.getStyleClass().add("message-other");
            root.getChildren().addAll(messageBox, actions, spacer);
            root.setAlignment(Pos.CENTER_LEFT);
            setAlignment(Pos.CENTER_LEFT);
        }

        setGraphic(root);
        setText(null);
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

    private Button createActionButton(String iconLiteral, String styleClass) {
        Button button = new Button();
        button.getStyleClass().addAll("message-action-btn", styleClass);
        button.setGraphic(new FontIcon(iconLiteral));
        button.setFocusTraversable(false);
        return button;
    }

    private ContextMenu createReactionMenu(Message message, HBox actions, HBox root) {
        ContextMenu menu = new ContextMenu();
        HBox reactionRow = new HBox(6);
        reactionRow.getStyleClass().add("emoji-picker-row");
        for (ReactionOption reaction : REACTION_OPTIONS) {
            Button button = new Button();
            button.getStyleClass().add("emoji-picker-button");
            FontIcon icon = new FontIcon(reaction.iconLiteral());
            icon.getStyleClass().add("emoji-picker-icon");
            button.setGraphic(icon);
            button.setFocusTraversable(false);
            button.setOnAction(event -> {
                onReact.accept(message, reaction.iconLiteral());
                menu.hide();
            });
            reactionRow.getChildren().add(button);
        }
        CustomMenuItem rowItem = new CustomMenuItem(reactionRow, false);
        rowItem.getStyleClass().add("emoji-picker-item");
        menu.getItems().add(rowItem);
        menu.setOnHidden(event -> {
            if (!root.isHover()) {
                actions.setOpacity(0);
                actions.setManaged(false);
                actions.setVisible(false);
            }
        });
        return menu;
    }

    private Tooltip createReactionTooltip(Message message, String iconLiteral) {
        List<String> usernames = new ArrayList<>();
        for (Map.Entry<String, String> entry : message.getReactions().entrySet()) {
            if (iconLiteral.equals(entry.getValue())) {
                usernames.add(entry.getKey());
            }
        }

        String tooltipText;
        if (usernames.isEmpty()) {
            tooltipText = "No one reacted";
        } else if (usernames.size() == 1) {
            tooltipText = usernames.get(0) + " reacted";
        } else {
            tooltipText = String.join(", ", usernames) + " reacted";
        }
        Tooltip tooltip = new Tooltip(tooltipText);
        tooltip.getStyleClass().add("reaction-tooltip");
        FontIcon reactionIcon = new FontIcon(iconLiteral);
        reactionIcon.getStyleClass().add("reaction-tooltip-icon");
        Label tooltipLabel = new Label(tooltipText);
        tooltipLabel.getStyleClass().add("reaction-tooltip-label");
        tooltipLabel.setGraphic(reactionIcon);
        tooltipLabel.setContentDisplay(javafx.scene.control.ContentDisplay.RIGHT);
        tooltipLabel.setGraphicTextGap(2);
        tooltip.setText(null);
        tooltip.setGraphic(tooltipLabel);
        return tooltip;
    }

    private TextFlow buildMessageTextFlow(String rawText) {
        TextFlow textFlow = new TextFlow();
        textFlow.getStyleClass().add("message-text-flow");
        textFlow.setMaxWidth(300);
        String text = rawText == null ? "" : rawText;
        int index = 0;
        while (index < text.length()) {
            int nextEmojiStart = -1;
            String matchedToken = null;
            for (String token : INLINE_EMOJI_ICONS.keySet()) {
                int candidate = text.indexOf(token, index);
                if (candidate != -1 && (nextEmojiStart == -1 || candidate < nextEmojiStart)) {
                    nextEmojiStart = candidate;
                    matchedToken = token;
                }
            }
            if (nextEmojiStart == -1 || matchedToken == null) {
                textFlow.getChildren().add(createTextNode(text.substring(index)));
                break;
            }
            if (nextEmojiStart > index) {
                textFlow.getChildren().add(createTextNode(text.substring(index, nextEmojiStart)));
            }
            FontIcon emojiIcon = new FontIcon(INLINE_EMOJI_ICONS.get(matchedToken));
            emojiIcon.getStyleClass().add("message-inline-icon");
            textFlow.getChildren().add(emojiIcon);
            index = nextEmojiStart + matchedToken.length();
        }
        return textFlow;
    }

    private javafx.scene.text.Text createTextNode(String value) {
        javafx.scene.text.Text textNode = new javafx.scene.text.Text(value);
        textNode.getStyleClass().add("message-text");
        return textNode;
    }

    private String formatTimestamp(Message message) {
        if (message == null || message.getTimestamp() == null) {
            return "";
        }
        return TIME_FORMATTER.format(message.getTimestamp());
    }

    private String formatCallDetails(String callDetails) {
        if (callDetails == null) {
            return "";
        }
        return callDetails.replaceFirst("(?i)^Time\\s+spent\\s+", "").trim();
    }

    private record ReactionOption(String iconLiteral, String label) {}
}
