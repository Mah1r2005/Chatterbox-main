package com.chatterbox.lan.controllers;


import com.chatterbox.lan.models.*;
import com.chatterbox.lan.network.*;
import com.chatterbox.lan.database.UserRepo;
import com.chatterbox.lan.utils.Loginout;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.css.PseudoClass;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.input.MouseEvent;
import javafx.scene.control.*;
import javafx.scene.input.MouseButton;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import javax.sound.sampled.LineUnavailableException;
import java.awt.AWTException;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


public class ChatPageController {
    private static final PseudoClass SELECTED_PSEUDO_CLASS = PseudoClass.getPseudoClass("selected");
    private static final Image DEFAULT_AVATAR = new Image(
            ChatPageController.class.getResourceAsStream("/Image/default.jpg")
    );

    private Client client;
    private User currentUser;
    private String currentConversationId;
    private Conversation currentConversation;
    private final AudioCallManager audioCallManager = new AudioCallManager();
    private final VideoCallManager videoCallManager = new VideoCallManager();
    private final UserRepo userRepo = new UserRepo();
    private final Map<String, Image> avatarCache = new ConcurrentHashMap<>();

    private final ObservableList<String> friends = FXCollections.observableArrayList();
    private final Set<String> selectedGroupMembers = new HashSet<>();



    @FXML
    private ListView<Message> messageList;

    @FXML
    private ListView<Conversation> conversationList;

    @FXML
    private Label inboxLabel;

    @FXML
    private HBox chatHeader;

    @FXML
    private ImageView headerAvatar;

    @FXML
    private TextField inputMessage;

    @FXML
    private Button sendButton;

    @FXML
    private HBox chatComposer;

    @FXML
    private Label emptyStateLabel;

    @FXML
    private HBox callNotificationBanner;

    @FXML
    private Label callNotificationLabel;

    @FXML private VBox createConversationOverlay;
    @FXML private TextField conversationNameField;
    @FXML private ListView<String> availableUsersList;


    @FXML private VBox addFriendOverlay;
    @FXML private TextField friendUserNameField;
    @FXML private VBox sidebarDrawer;
    @FXML private VBox sidebarContent;
    @FXML private VBox ownIdSidebarSections;
    @FXML private Label sidebarTitleLabel;
    @FXML private Button addFriendButton;
    @FXML private Button createGroupButton;
    @FXML private Button myIdButton;
    @FXML private Button drawerToggleButton;
    @FXML private StackPane rootContainer;
    @FXML private StackPane sidebarPane;
    @FXML private VBox chatContent;
    @FXML private VBox ownIdPage;
    @FXML private ImageView ownIdAvatar;
    @FXML private Label ownIdUsernameLabel;
    @FXML private Label ownIdFirstNameLabel;
    @FXML private Label ownIdLastNameLabel;
    @FXML private Label ownIdEmailLabel;
    @FXML private Label ownIdPhoneLabel;
    @FXML private Label ownIdLocationLabel;
    @FXML private Button personalDetailsButton;
    @FXML private Button editDetailsButton;
    @FXML private Button changePasswordButton;
    @FXML private VBox personalDetailsView;
    @FXML private VBox editDetailsView;
    @FXML private VBox changePasswordView;
    @FXML private TextField editAvatarField;
    @FXML private TextField editUsernameField;
    @FXML private TextField editFirstNameField;
    @FXML private TextField editLastNameField;
    @FXML private TextField editEmailField;
    @FXML private TextField editPhoneField;
    @FXML private TextField editLocationField;
    @FXML private Label editDetailsMessageLabel;
    @FXML private PasswordField currentPasswordField;
    @FXML private PasswordField newPasswordField;
    @FXML private PasswordField confirmPasswordField;
    @FXML private Label changePasswordMessageLabel;

    @FXML
    public void initialize() {
        messageList.setCellFactory(param -> new MessageCell(this::shouldShowSenderNames));
        setConversationSelected(false);
        conversationList.setCellFactory(list -> new ListCell<>() {

            private final Label nameLabel = new Label();
            private final ImageView avatarView = createCircularAvatar(34);
            private final HBox container = new HBox(avatarView, nameLabel);

            {
                container.setSpacing(10);
                container.getStyleClass().add("conversation-list-item");
                nameLabel.getStyleClass().add("conversation-list-name");
            }

            @Override
            protected void updateItem(Conversation convo, boolean empty) {
                super.updateItem(convo, empty);

                if (empty || convo == null) {
                    setGraphic(null);
                    setText(null);
                    pseudoClassStateChanged(SELECTED_PSEUDO_CLASS, false);
                    container.pseudoClassStateChanged(SELECTED_PSEUDO_CLASS, false);
                } else {
                    nameLabel.setText(getConversationDisplayName(convo));
                    avatarView.setImage(loadConversationAvatar(convo));
                    setGraphic(container);
                    setText(null);
                    boolean selected = isSelected();
                    pseudoClassStateChanged(SELECTED_PSEUDO_CLASS, selected);
                    container.pseudoClassStateChanged(SELECTED_PSEUDO_CLASS, selected);
                }
            }

            @Override
            public void updateSelected(boolean selected) {
                super.updateSelected(selected);
                pseudoClassStateChanged(SELECTED_PSEUDO_CLASS, selected);
                container.pseudoClassStateChanged(SELECTED_PSEUDO_CLASS, selected);
            }
        });
        availableUsersList.setCellFactory(list -> new ListCell<>() {
            private final Button removeButton = new Button("x");
            private final ImageView avatarView = createCircularAvatar(28);
            private final Label nameLabel = new Label();
            private final HBox container = new HBox(removeButton, avatarView, nameLabel);

            {
                container.setSpacing(10);
                container.getStyleClass().add("available-user-item");
                removeButton.getStyleClass().add("available-user-remove");
                removeButton.setFocusTraversable(false);
                removeButton.setVisible(false);
                removeButton.setManaged(false);
                removeButton.setOnAction(event -> {
                    if (getItem() != null) {
                        selectedGroupMembers.remove(getItem());
                        availableUsersList.refresh();
                    }
                    event.consume();
                });
                nameLabel.getStyleClass().add("available-user-name");
                setOnMousePressed(event -> {
                    if (event.getButton() != MouseButton.PRIMARY || getItem() == null) {
                        return;
                    }
                    if (!selectedGroupMembers.contains(getItem())) {
                        selectedGroupMembers.add(getItem());
                        availableUsersList.refresh();
                    }
                    event.consume();
                });
            }

            @Override
            protected void updateItem(String username, boolean empty) {
                super.updateItem(username, empty);
                if (empty || username == null) {
                    setGraphic(null);
                    setText(null);
                    pseudoClassStateChanged(SELECTED_PSEUDO_CLASS, false);
                    container.pseudoClassStateChanged(SELECTED_PSEUDO_CLASS, false);
                    removeButton.setVisible(false);
                    removeButton.setManaged(false);
                } else {
                    nameLabel.setText(username);
                    avatarView.setImage(loadAvatarForUsername(username));
                    setGraphic(container);
                    setText(null);
                    boolean selected = selectedGroupMembers.contains(username);
                    pseudoClassStateChanged(SELECTED_PSEUDO_CLASS, selected);
                    container.pseudoClassStateChanged(SELECTED_PSEUDO_CLASS, selected);
                    removeButton.setVisible(selected);
                    removeButton.setManaged(selected);
                }
            }
        });
        availableUsersList.setItems(friends);
        // Hide overlays initially
        if (createConversationOverlay != null) createConversationOverlay.setVisible(false);
        if (addFriendOverlay != null) addFriendOverlay.setVisible(false);
        if (headerAvatar != null) {
            applyCircularClip(headerAvatar, 18);
            headerAvatar.setImage(DEFAULT_AVATAR);
        }
        if (ownIdAvatar != null) {
            applyCircularClip(ownIdAvatar, 46);
            ownIdAvatar.setImage(DEFAULT_AVATAR);
        }
        setDrawerVisible(false);
        showOwnIdSection("personal");
        if (sidebarPane != null && sidebarDrawer != null) {
            sidebarDrawer.prefHeightProperty().bind(sidebarPane.heightProperty());
        }
        if (rootContainer != null) {
            rootContainer.addEventFilter(MouseEvent.MOUSE_PRESSED, this::handleRootMousePressed);
        }

    }

    // Called by LoginController after successful login
    public void setCurrentUser(User user, Client clientInstance) {
        this.currentUser = user;
        this.client = clientInstance;
        if (myIdButton != null && currentUser != null) {
            myIdButton.setText(currentUser.getUsername());
        }

        client.setListener(event -> {
            if ("CALL_AUDIO".equals(event.getType())) {
                handleCallAudio(event);
                return;
            }

            Platform.runLater(() -> {
                switch (event.getType()) {
                    case "NEW_MESSAGE" -> handleIncomingMessage(event);
                    case "MESSAGES_RESPONSE" -> handleMessagesResponse(event);
                    case "CONVERSATIONS_UPDATED" -> handleConversationsUpdate(event);
                    case "NEW_CONVERSATION" -> handleNewConversation(event);
                    case "CREATE_CONVERSATION_FAILED" -> handleCreateConversationFailed(event);
                    case "CALL_REQUEST" -> handleIncomingCall(event);
                    case "CALL_ACCEPTED" -> handleCallAccepted(event);
                    case "CALL_REJECTED" -> handleCallRejected(event);
                    case "CALL_ENDED" -> handleCallEnded(event);
                    case "CALL_VIDEO_FRAME" -> handleCallVideoFrame(event);
                    default -> System.err.println("[CLIENT] Unknown event type: " + event.getType());
                }
            });
        });

        conversationList.getSelectionModel()
                .selectedItemProperty()
                .addListener((obs, oldVal, newVal) -> {
                    if (newVal != null) {
                        onConversationSelected(newVal);
                    }
                });

        // Fetch conversations immediately after login
        client.getConversations();
    }

    private void appendMessages(List<Message> messages) {
        messageList.getItems().addAll(messages);
        if (!messages.isEmpty()) {
            messageList.scrollTo(messages.size() - 1);
        }
    }

    private void onConversationSelected(Conversation conversation) {
        if (conversation == null) {
            currentConversationId = null;
            currentConversation = null;
            messageList.getItems().clear();
            setConversationSelected(false);
            return;
        }
        currentConversation = conversation;
        currentConversationId = conversation.getConversationId();
        inboxLabel.setText(getConversationDisplayName(conversation));
        if (headerAvatar != null) {
            headerAvatar.setImage(loadConversationAvatar(conversation));
        }
        setConversationSelected(true);
        client.getMessages(currentConversationId);
    }

    private void setConversationSelected(boolean selected) {
        if (chatHeader != null) {
            chatHeader.setVisible(selected);
            chatHeader.setManaged(selected);
        }
        if (messageList != null) {
            messageList.setVisible(selected);
            messageList.setManaged(selected);
        }
        if (chatComposer != null) {
            chatComposer.setVisible(selected);
            chatComposer.setManaged(selected);
        }
        if (emptyStateLabel != null) {
            emptyStateLabel.setVisible(!selected);
            emptyStateLabel.setManaged(!selected);
        }
    }

    private void setDrawerVisible(boolean visible) {
        if (sidebarDrawer != null) {
            if (visible) {
                sidebarDrawer.toFront();
            }
            sidebarDrawer.setVisible(visible);
            sidebarDrawer.setManaged(visible);
        }
    }

    private String getConversationDisplayName(Conversation conversation) {
        if (conversation == null) {
            return "";
        }
        String name = conversation.getName();
        if (name != null && !name.isBlank()) {
            return name;
        }
        if (conversation.getMembers() == null || conversation.getMembers().isEmpty() || currentUser == null) {
            return "Conversation";
        }
        for (String member : conversation.getMembers()) {
            if (!member.equals(currentUser.getUsername())) {
                return member;
            }
        }
        return conversation.getMembers().get(0);
    }

    private boolean shouldShowSenderNames() {
        return currentConversation != null
                && currentConversation.getMembers() != null
                && currentConversation.getMembers().size() > 2;
    }

    private ImageView createCircularAvatar(double size) {
        ImageView imageView = new ImageView(DEFAULT_AVATAR);
        imageView.setFitWidth(size);
        imageView.setFitHeight(size);
        imageView.setPreserveRatio(false);
        applyCircularClip(imageView, size / 2);
        return imageView;
    }

    private void applyCircularClip(ImageView imageView, double radius) {
        Circle clip = new Circle(radius, radius, radius);
        imageView.setClip(clip);
    }

    @FXML
    public void onStartAudioCall() {
        String targetUser = getDirectCallTarget();
        if (targetUser == null) {
            showInfo("Audio Call", "Audio calls currently support one-to-one conversations only.");
            return;
        }
        if (isAnyCallActive()) {
            showInfo("Audio Call", "A call is already active.");
            return;
        }
        client.requestCall(targetUser);
        showCallNotification("Calling " + targetUser + "...");
    }

    @FXML
    public void onStartVideoCall() {
        String targetUser = getDirectCallTarget();
        if (targetUser == null) {
            showInfo("Video Call", "Video calls currently support one-to-one conversations only.");
            return;
        }
        if (isAnyCallActive()) {
            showInfo("Video Call", "A call is already active.");
            return;
        }
        client.requestVideoCall(targetUser);
        showCallNotification("Video calling " + targetUser + "...");
    }

    private void handleIncomingCall(Event event) {
        String caller = event.getUsername();
        String callMode = getCallMode(event);
        if (caller == null || caller.isBlank()) {
            return;
        }
        if (isAnyCallActive()) {
            client.rejectCall(caller, callMode);
            return;
        }

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        String callLabel = "VIDEO".equals(callMode) ? "Video Call" : "Audio Call";
        alert.setTitle("Incoming " + callLabel);
        alert.setHeaderText(caller + " is calling");
        alert.setContentText("Accept " + callLabel.toLowerCase() + "?");
        alert.setGraphic(null);
        ButtonType accept = new ButtonType("Accept", ButtonBar.ButtonData.OK_DONE);
        ButtonType reject = new ButtonType("Reject", ButtonBar.ButtonData.CANCEL_CLOSE);
        alert.getButtonTypes().setAll(accept, reject);

        DialogPane dialogPane = alert.getDialogPane();
        dialogPane.setStyle("-fx-background-color: white;");

        Node header = dialogPane.lookup(".header-panel");
        if (header != null) {
            header.setStyle("-fx-background-color: white;");
        }

        Node graphicContainer = dialogPane.lookup(".graphic-container");
        if (graphicContainer != null) {
            graphicContainer.setVisible(false);
            graphicContainer.setManaged(false);
        }

        Node headerLabel = dialogPane.lookup(".header-panel .label");
        if (headerLabel != null) {
            headerLabel.setStyle("-fx-text-fill: deepskyblue; -fx-font-weight: bold;");
        }

        Node contentLabel = dialogPane.lookup(".content.label");
        if (contentLabel != null) {
            contentLabel.setStyle("-fx-text-fill: deepskyblue;");
        }

        Button acceptButton = (Button) dialogPane.lookupButton(accept);
        if (acceptButton != null) {
            acceptButton.setStyle("-fx-background-color: springgreen; -fx-text-fill: white; -fx-font-weight: bold;");
        }

        Button rejectButton = (Button) dialogPane.lookupButton(reject);
        if (rejectButton != null) {
            rejectButton.setStyle("-fx-background-color: red; -fx-text-fill: white; -fx-font-weight: bold;");
        }

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == accept) {
            boolean started = "VIDEO".equals(callMode) ? startVideoCall(caller) : startAudioCall(caller);
            if (started) {
                client.acceptCall(caller, callMode);
            } else {
                client.rejectCall(caller, callMode);
            }
        } else {
            client.rejectCall(caller, callMode);
        }
    }

    private void handleCallAccepted(Event event) {
        String peer = event.getUsername();
        String callMode = getCallMode(event);
        if (peer == null || peer.isBlank()) {
            return;
        }
        if (isAnyCallActive()) {
            return;
        }
        boolean started = "VIDEO".equals(callMode) ? startVideoCall(peer) : startAudioCall(peer);
        if (started) {
            showCallNotification("On " + callMode.toLowerCase() + " call with " + peer);
        }
    }

    private void handleCallRejected(Event event) {
        String callMode = getCallMode(event);
        stopActiveCall(false, callMode);
        String peer = event.getUsername();
        String name = peer == null || peer.isBlank() ? "The other user" : peer;
        showStyledInfo(getCallTitle(callMode), name + " is unavailable or rejected the call.", "red");
    }

    private void handleCallEnded(Event event) {
        String callMode = getCallMode(event);
        stopActiveCall(false, callMode);
        String peer = event.getUsername();
        String name = peer == null || peer.isBlank() ? "The other user" : peer;
        showInfo(getCallTitle(callMode), name + " ended the call.");
    }

    private void handleCallAudio(Event event) {
        Object audioChunk = event.getData("audioChunk");
        if (audioChunk instanceof byte[] bytes) {
            audioCallManager.receiveAudio(bytes);
        }
    }

    private void handleCallVideoFrame(Event event) {
        Object imageBytes = event.getData("imageBytes");
        if (imageBytes instanceof byte[] bytes) {
            videoCallManager.receiveFrame(bytes);
        }
    }

    private boolean startAudioCall(String peerUsername) {
        try {
            audioCallManager.startCall(peerUsername, audioChunk -> client.sendCallAudio(peerUsername, audioChunk));
            showCallNotification("On call with " + peerUsername);
            return true;
        } catch (LineUnavailableException e) {
            hideCallNotification();
            showStyledInfo("Audio Call", "Audio device is unavailable: " + e.getMessage(), "red");
            return false;
        }
    }

    private boolean startVideoCall(String peerUsername) {
        try {
            videoCallManager.startCall(
                    peerUsername,
                    imageBytes -> client.sendCallVideoFrame(peerUsername, imageBytes),
                    () -> Platform.runLater(this::onEndAudioCall)
            );
            showCallNotification("On video call with " + peerUsername);
            return true;
        } catch (AWTException e) {
            hideCallNotification();
            showStyledInfo("Video Call", "Video device is unavailable: " + e.getMessage(), "red");
            return false;
        }
    }

    private void stopAudioCall(boolean notifyPeer) {
        String peer = audioCallManager.getPeerUsername();
        if (notifyPeer && peer != null && !peer.isBlank()) {
            client.endCall(peer);
        }
        audioCallManager.stopCall();
        hideCallNotification();
    }

    private void stopVideoCall(boolean notifyPeer) {
        String peer = videoCallManager.getPeerUsername();
        if (notifyPeer && peer != null && !peer.isBlank()) {
            client.endCall(peer, "VIDEO");
        }
        videoCallManager.stopCall();
        hideCallNotification();
    }

    private void stopActiveCall(boolean notifyPeer, String callMode) {
        if ("VIDEO".equals(callMode)) {
            stopVideoCall(notifyPeer);
        } else {
            stopAudioCall(notifyPeer);
        }
    }

    @FXML
    public void onEndAudioCall() {
        if (videoCallManager.isActive()) {
            stopVideoCall(true);
            return;
        }
        if (!audioCallManager.isActive()) {
            hideCallNotification();
            return;
        }
        stopAudioCall(true);
    }

    private String getDirectCallTarget() {
        if (currentConversation == null || currentConversation.getMembers() == null || currentConversation.getMembers().size() != 2 || currentUser == null) {
            return null;
        }
        for (String member : currentConversation.getMembers()) {
            if (!member.equals(currentUser.getUsername())) {
                return member;
            }
        }
        return null;
    }

    private void showInfo(String title, String content) {
        showStyledInfo(title, content, "deepskyblue");
    }

    private void showStyledInfo(String title, String content, String accentColor) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.setGraphic(null);

        DialogPane dialogPane = alert.getDialogPane();
        dialogPane.setStyle("-fx-background-color: white;");

        Node graphicContainer = dialogPane.lookup(".graphic-container");
        if (graphicContainer != null) {
            graphicContainer.setVisible(false);
            graphicContainer.setManaged(false);
        }

        Node contentLabel = dialogPane.lookup(".content.label");
        if (contentLabel != null) {
            contentLabel.setStyle("-fx-text-fill: " + accentColor + "; -fx-font-weight: bold;");
        }

        Button okButton = (Button) dialogPane.lookupButton(ButtonType.OK);
        if (okButton != null) {
            okButton.setStyle("-fx-background-color: " + accentColor + "; -fx-text-fill: white; -fx-font-weight: bold;");
        }

        alert.show();
    }

    private void showCallNotification(String message) {
        if (callNotificationLabel != null) {
            callNotificationLabel.setText(message);
        }
        if (callNotificationBanner != null) {
            callNotificationBanner.setVisible(true);
            callNotificationBanner.setManaged(true);
        }
    }

    private void hideCallNotification() {
        if (callNotificationBanner != null) {
            callNotificationBanner.setVisible(false);
            callNotificationBanner.setManaged(false);
        }
    }



    // for loading conversations in the left pane when user logs in
    private void handleConversationsUpdate(Event event) {
        @SuppressWarnings("unchecked")
        List<Conversation> conversations = (List<Conversation>) event.getData("conversations");

        if (conversations == null) {
            System.err.println("No conversations in response");
            return;
        }
        for (Conversation conv : conversations) {
            for (String member : conv.getMembers()) {
                if (!member.equals(currentUser.getUsername()) && !friends.contains(member)) {
                    friends.add(member);
                }
            }
        }
        conversationList.getItems().clear();
        conversationList.getItems().addAll(conversations);
        System.out.println("[CONTROLLER] Updated conversations: " + conversations.size() + " conversations");
    }

    private void handleNewConversation(Event event) {
        Conversation newConversation = (Conversation) event.getData("conversation");
        if (newConversation == null) {
            System.err.println("No conversation data in NEW_CONVERSATION event");
            return;
        }
        boolean exists = conversationList.getItems().stream()
                .anyMatch(conversation -> conversation.getConversationId().equals(newConversation.getConversationId()));
        if (!exists) {
            conversationList.getItems().add(newConversation);
        }
        for (String member : newConversation.getMembers()) {
            if (!member.equals(currentUser.getUsername()) && !friends.contains(member)) {
                friends.add(member);
            }
        }
        Conversation createdConversation = conversationList.getItems().stream()
                .filter(conversation -> conversation.getConversationId().equals(newConversation.getConversationId()))
                .findFirst()
                .orElse(null);
        if (createdConversation != null) {
            conversationList.getSelectionModel().select(createdConversation);
        }
    }

    private void handleCreateConversationFailed(Event event) {
        Object message = event.getData("message");
        showStyledInfo(
                "Create conversation",
                message == null ? "One or more usernames do not exist." : message.toString(),
                "red"
        );
    }


    // for loading new incoming message in real-time
    private void handleIncomingMessage(Event event) {
        if (currentConversationId == null || !currentConversationId.equals(event.getConversationId())) return;

        User sender = new User(event.getUsername(), "/avatars/default.png");
        sender.setMe(sender.getUsername().equals(currentUser.getUsername()));
        String messageType = event.getData("messageType") instanceof String type ? type : "TEXT";
        Message incoming = "FILE".equals(messageType)
                ? new Message(
                        sender,
                        (String) event.getData("fileName"),
                        (byte[]) event.getData("fileData"),
                        event.getConversationId()
                )
                : new Message(sender, event.getText(), event.getConversationId());
        appendMessages(List.of(incoming));
    }

    // for loading all message of a conversation when selected
    private void handleMessagesResponse(Event event) {

        List<Message> messages = (List<Message>) event.getData("messages");
        if (messages == null) {
            System.err.println("No messages in response");
            return;
        }

        // Set isMe flag for each message's sender
        for (Message message : messages) {
            if (message.getSender() != null) {
                boolean isCurrentUser = message.getSender().getUsername().equals(currentUser.getUsername());
                message.getSender().setMe(isCurrentUser);
            }
        }


        messageList.getItems().clear();
        appendMessages(messages);
    }


    @FXML
    public void onSendMessage() {
        if (inputMessage == null || inputMessage.getText().trim().isEmpty()) {
            return;
        }
        if (currentConversationId == null) {
            System.err.println("No conversation selected!");
            return;
        }

        String text = inputMessage.getText().trim();
//
//        // IMMEDIATELY add message to UI with current user as sender
//        User sender = new User(currentUser.getUsername(), currentUser.getAvatarPath());
//        sender.setMe(true);
//        Message outgoingMessage = new Message(sender, text, currentConversationId);
//        messageList.getItems().add(outgoingMessage);

        // Then send to server
        client.sendMessage(currentConversationId, text);
        inputMessage.clear();
    }

    @FXML
    public void onSendFile() {
        if (currentConversationId == null) {
            showStyledInfo("Send File", "Select a conversation first.", "red");
            return;
        }

        FileChooser chooser = new FileChooser();
        chooser.setTitle("Choose File");
        Stage stage = conversationList == null || conversationList.getScene() == null
                ? null
                : (Stage) conversationList.getScene().getWindow();
        File file = chooser.showOpenDialog(stage);
        if (file == null) {
            return;
        }

        try {
            byte[] fileBytes = Files.readAllBytes(file.toPath());
            if (fileBytes.length == 0) {
                showStyledInfo("Send File", "Selected file is empty.", "red");
                return;
            }
            client.sendFileMessage(currentConversationId, file.getName(), fileBytes);
        } catch (IOException e) {
            showStyledInfo("Send File", "Failed to read file: " + e.getMessage(), "red");
        }
    }

    @FXML
    public void onToggleDrawer() {
        if (sidebarDrawer != null) {
            setDrawerVisible(!sidebarDrawer.isVisible());
        }
    }

    @FXML
    public void onCloseDrawer() {
        setDrawerVisible(false);
    }

    private void handleRootMousePressed(MouseEvent event) {
        if (!isDrawerOpen()) {
            return;
        }
        Object target = event.getTarget();
        if (!(target instanceof Node node)) {
            setDrawerVisible(false);
            return;
        }
        if (isInside(node, sidebarDrawer) || isInside(node, drawerToggleButton)) {
            return;
        }
        setDrawerVisible(false);
    }

    @FXML
    public void onDrawerAreaPressed(MouseEvent event) {
        event.consume();
    }

    private boolean isDrawerOpen() {
        return sidebarDrawer != null && sidebarDrawer.isVisible();
    }

    private boolean isInside(Node node, Node container) {
        if (node == null || container == null) {
            return false;
        }
        Node current = node;
        while (current != null) {
            if (current == container) {
                return true;
            }
            current = current.getParent();
        }
        return false;
    }

    @FXML
    public void onOpenSettings() {
        setDrawerVisible(false);
        showInfo("Setting", "Settings are not implemented yet.");
    }

    @FXML
    public void onShowOwnId() {
        setDrawerVisible(false);
        populateOwnIdCard();
        populateEditDetailsForm();
        showOwnIdSection("personal");
        showOwnIdPage(true);
    }

    @FXML
    public void onShowChats() {
        setDrawerVisible(false);
        showOwnIdPage(false);
    }

    @FXML
    public void onCloseOwnId() {
        showOwnIdPage(false);
    }

    @FXML
    public void onShowPersonalDetails() {
        showOwnIdSection("personal");
    }

    @FXML
    public void onShowEditDetails() {
        populateEditDetailsForm();
        showOwnIdSection("edit");
    }

    @FXML
    public void onShowChangePassword() {
        clearChangePasswordForm();
        showOwnIdSection("password");
    }

    @FXML
    public void onLogout() {
        setDrawerVisible(false);
        try {
            stopAudioCall(true);
            if (client != null) {
                client.disconnect();
            }

            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/com/chatterbox/lan/Login-view.fxml")
            );
            Parent root = loader.load();
            Stage stage = (Stage) conversationList.getScene().getWindow();
            Scene scene = new Scene(root, 800, 600);
            stage.setScene(scene);
            stage.setTitle("Chatterbox - Login");
        } catch (IOException e) {
            showInfo("Logout", "Failed to return to login screen: " + e.getMessage());
        }
    }

    @FXML
    public void onOpenCreateConversation() {
        if (createConversationOverlay != null) {
            createConversationOverlay.setVisible(true);
            createConversationOverlay.setManaged(true);
        }
        selectedGroupMembers.clear();
        conversationNameField.clear();
        availableUsersList.refresh();
    }

    @FXML
    public void onCloseCreateConversation() {
        if (createConversationOverlay != null) {
            createConversationOverlay.setVisible(false);
            createConversationOverlay.setManaged(false);
        }
        selectedGroupMembers.clear();
        conversationNameField.clear();
        availableUsersList.refresh();
    }
    @FXML
    public void onSubmitCreateConversation() {
        String name = conversationNameField.getText().trim();
        List<String> selectedUsers = new ArrayList<>(selectedGroupMembers);

        if (name.isEmpty()) {
            showInfo("Create group", "Enter a group name.");
            return;
        }

        if (selectedUsers.isEmpty()) {
            showInfo("Create group", "Select at least one friend.");
            return;
        }

        Set<String> members = new LinkedHashSet<>();
        if (currentUser != null) {
            members.add(currentUser.getUsername());
        }
        members.addAll(selectedUsers);

        if (members.size() < 2) {
            showInfo("Create group", "Select at least one valid friend.");
            return;
        }

        client.createConversation(name, new ArrayList<>(members));

        onCloseCreateConversation();
    }
    @FXML
    public void onOpenAddFriend() {
        if (addFriendOverlay != null) {
            addFriendOverlay.setVisible(true);
            addFriendOverlay.setManaged(true);
        }
        friendUserNameField.clear();
    }

    @FXML
    public void onCloseAddFriend() {
        if (addFriendOverlay != null) {
            addFriendOverlay.setVisible(false);
            addFriendOverlay.setManaged(false);
        }
        friendUserNameField.clear();
    }
    @FXML
    public void onSubmitAddFriend() {
        String friendUsername = friendUserNameField.getText().trim();

        if (friendUsername.isEmpty()) {
            showStyledInfo("Add Friend", "Enter a username.", "red");
            return;
        }
        if (!userRepo.usernameExists(friendUsername)) {
            showStyledInfo("Add Friend", "Username does not exist: " + friendUsername, "red");
            return;
        }

        List<String> members = new ArrayList<>();
        members.add(friendUsername);
        if (currentUser != null) members.add(currentUser.getUsername());
        client.createConversation(null, members);
        System.out.println("[ADD_FRIEND] Added friend: " + friendUsername);
        onCloseAddFriend();
    }


    public void cleanup() {
        stopVideoCall(true);
        stopAudioCall(true);
        client.disconnect();
    }

    private boolean isAnyCallActive() {
        return audioCallManager.isActive() || videoCallManager.isActive();
    }

    private String getCallMode(Event event) {
        Object callMode = event.getData("callMode");
        return callMode instanceof String mode && !mode.isBlank() ? mode : "AUDIO";
    }

    private String getCallTitle(String callMode) {
        return "VIDEO".equals(callMode) ? "Video Call" : "Audio Call";
    }

    private void populateOwnIdCard() {
        if (ownIdUsernameLabel == null || currentUser == null) {
            return;
        }

        String username = safeValue(currentUser.getUsername());
        ownIdUsernameLabel.setText(username);
        ownIdFirstNameLabel.setText(profileValue(currentUser.getFirstName()));
        ownIdLastNameLabel.setText(profileValue(currentUser.getLastName()));
        ownIdEmailLabel.setText(profileValue(currentUser.getEmail()));
        ownIdPhoneLabel.setText(profileValue(currentUser.getPhoneNumber()));
        ownIdLocationLabel.setText(profileValue(currentUser.getLocation()));
        ownIdAvatar.setImage(loadAvatarImage(currentUser.getAvatarPath()));
    }

    private void populateEditDetailsForm() {
        if (currentUser == null) {
            return;
        }
        if (editAvatarField != null) {
            editAvatarField.setText(blankIfNull(currentUser.getAvatarPath()));
        }
        if (editUsernameField != null) {
            editUsernameField.setText(blankIfNull(currentUser.getUsername()));
        }
        if (editFirstNameField != null) {
            editFirstNameField.setText(blankIfNull(currentUser.getFirstName()));
        }
        if (editLastNameField != null) {
            editLastNameField.setText(blankIfNull(currentUser.getLastName()));
        }
        if (editEmailField != null) {
            editEmailField.setText(blankIfNull(currentUser.getEmail()));
        }
        if (editPhoneField != null) {
            editPhoneField.setText(blankIfNull(currentUser.getPhoneNumber()));
        }
        if (editLocationField != null) {
            editLocationField.setText(blankIfNull(currentUser.getLocation()));
        }
        if (editDetailsMessageLabel != null) {
            editDetailsMessageLabel.setVisible(false);
            editDetailsMessageLabel.setManaged(false);
        }
    }

    @FXML
    public void onBrowseEditAvatar() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select Profile Photo");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg", "*.gif")
        );
        Stage stage = conversationList == null || conversationList.getScene() == null
                ? null
                : (Stage) conversationList.getScene().getWindow();
        File selectedFile = fileChooser.showOpenDialog(stage);
        if (selectedFile != null && editAvatarField != null) {
            editAvatarField.setText(selectedFile.toURI().toString());
        }
    }

    @FXML
    public void onSaveEditDetails() {
        if (currentUser == null) {
            showEditDetailsMessage("No user is loaded.", true);
            return;
        }

        String firstName = valueOrNull(editFirstNameField);
        String lastName = valueOrNull(editLastNameField);
        String email = valueOrNull(editEmailField);
        String phone = valueOrNull(editPhoneField);
        String location = valueOrNull(editLocationField);

        if (firstName == null || lastName == null || email == null || phone == null || location == null) {
            showEditDetailsMessage("Please fill in all fields.", true);
            return;
        }

        String avatarPath = valueOrNull(editAvatarField);
        currentUser.setAvatarPath(avatarPath == null ? currentUser.getAvatarPath() : avatarPath);
        currentUser.setFirstName(firstName);
        currentUser.setLastName(lastName);
        currentUser.setEmail(email);
        currentUser.setPhoneNumber(phone);
        currentUser.setLocation(location);

        try {
            userRepo.saveUser(currentUser);
            avatarCache.remove(currentUser.getUsername());
            populateOwnIdCard();
            populateEditDetailsForm();
            showEditDetailsMessage("Details updated.", false);
        } catch (Exception e) {
            showEditDetailsMessage("Failed to save details: " + e.getMessage(), true);
        }
    }

    @FXML
    public void onUpdatePassword() {
        if (currentUser == null) {
            showChangePasswordMessage("No user is loaded.", true);
            return;
        }

        String currentPassword = passwordValue(currentPasswordField);
        String newPassword = passwordValue(newPasswordField);
        String confirmPassword = passwordValue(confirmPasswordField);

        if (currentPassword == null || newPassword == null || confirmPassword == null) {
            showChangePasswordMessage("Please fill in all password fields.", true);
            return;
        }
        if (!newPassword.equals(confirmPassword)) {
            showChangePasswordMessage("New passwords do not match.", true);
            return;
        }

        try {
            User storedUser = userRepo.getUserByUsername(currentUser.getUsername());
            if (storedUser == null || storedUser.getPassword() == null
                    || !Loginout.ValidatePass(currentPassword, storedUser.getPassword())) {
                showChangePasswordMessage("Current password is incorrect.", true);
                return;
            }

            storedUser.setAvatarPath(currentUser.getAvatarPath());
            storedUser.setFirstName(currentUser.getFirstName());
            storedUser.setLastName(currentUser.getLastName());
            storedUser.setEmail(currentUser.getEmail());
            storedUser.setPhoneNumber(currentUser.getPhoneNumber());
            storedUser.setLocation(currentUser.getLocation());
            storedUser.setPassword(Loginout.Hasher(newPassword));
            userRepo.saveUser(storedUser);
            clearChangePasswordForm();
            showChangePasswordMessage("Password updated.", false);
        } catch (Exception e) {
            showChangePasswordMessage("Failed to update password: " + e.getMessage(), true);
        }
    }

    private void showEditDetailsMessage(String message, boolean error) {
        if (editDetailsMessageLabel == null) {
            return;
        }
        editDetailsMessageLabel.setText(message);
        editDetailsMessageLabel.setTextFill(error ? javafx.scene.paint.Paint.valueOf("red") : javafx.scene.paint.Paint.valueOf("deepskyblue"));
        editDetailsMessageLabel.setVisible(true);
        editDetailsMessageLabel.setManaged(true);
    }

    private void showChangePasswordMessage(String message, boolean error) {
        if (changePasswordMessageLabel == null) {
            return;
        }
        changePasswordMessageLabel.setText(message);
        changePasswordMessageLabel.setTextFill(error ? javafx.scene.paint.Paint.valueOf("red") : javafx.scene.paint.Paint.valueOf("deepskyblue"));
        changePasswordMessageLabel.setVisible(true);
        changePasswordMessageLabel.setManaged(true);
    }


    private Image loadConversationAvatar(Conversation conversation) {
        String username = getConversationAvatarUsername(conversation);
        return loadAvatarForUsername(username);
    }

    private String getConversationAvatarUsername(Conversation conversation) {
        if (conversation == null || conversation.getMembers() == null || currentUser == null) {
            return null;
        }
        for (String member : conversation.getMembers()) {
            if (!member.equals(currentUser.getUsername())) {
                return member;
            }
        }
        return conversation.getMembers().isEmpty() ? null : conversation.getMembers().get(0);
    }

    private Image loadAvatarForUsername(String username) {
        if (username == null || username.isBlank()) {
            return DEFAULT_AVATAR;
        }
        if (currentUser != null && username.equals(currentUser.getUsername())) {
            return loadAvatarImage(currentUser.getAvatarPath());
        }
        return avatarCache.computeIfAbsent(username, key -> {
            User user = userRepo.getUserByUsername(key);
            return user == null ? DEFAULT_AVATAR : loadAvatarImage(user.getAvatarPath());
        });
    }

    private void showOwnIdPage(boolean visible) {
        if (chatContent != null) {
            chatContent.setVisible(!visible);
            chatContent.setManaged(!visible);
        }
        if (ownIdPage != null) {
            ownIdPage.setVisible(visible);
            ownIdPage.setManaged(visible);
        }
        if (sidebarTitleLabel != null) {
            sidebarTitleLabel.setVisible(!visible);
            sidebarTitleLabel.setManaged(!visible);
        }
        if (conversationList != null) {
            conversationList.setVisible(!visible);
            conversationList.setManaged(!visible);
        }
        if (ownIdSidebarSections != null) {
            ownIdSidebarSections.setVisible(visible);
            ownIdSidebarSections.setManaged(visible);
        }
        if (addFriendButton != null) {
            addFriendButton.setVisible(!visible);
            addFriendButton.setManaged(!visible);
        }
        if (createGroupButton != null) {
            createGroupButton.setVisible(!visible);
            createGroupButton.setManaged(!visible);
        }
        if (sidebarContent != null) {
            sidebarContent.requestLayout();
        }
        if (sidebarPane != null) {
            sidebarPane.setVisible(true);
            sidebarPane.setManaged(true);
        }
        if (visible) {
            setDrawerVisible(false);
        }
    }

    private void showOwnIdSection(String section) {
        boolean personal = "personal".equals(section);
        boolean edit = "edit".equals(section);
        boolean password = "password".equals(section);

        setSectionVisible(personalDetailsView, personal);
        setSectionVisible(editDetailsView, edit);
        setSectionVisible(changePasswordView, password);

        updateOwnIdNavButton(personalDetailsButton, personal);
        updateOwnIdNavButton(editDetailsButton, edit);
        updateOwnIdNavButton(changePasswordButton, password);
    }

    private void setSectionVisible(Node node, boolean visible) {
        if (node == null) {
            return;
        }
        node.setVisible(visible);
        node.setManaged(visible);
    }

    private void updateOwnIdNavButton(Button button, boolean active) {
        if (button == null) {
            return;
        }
        button.pseudoClassStateChanged(SELECTED_PSEUDO_CLASS, active);
    }

    private String[] splitName(String username) {
        if (username == null || username.isBlank()) {
            return new String[]{"Not provided", "Not provided"};
        }

        String normalized = username.trim().replace('.', ' ').replace('_', ' ').replace('-', ' ');
        String[] parts = Arrays.stream(normalized.split("\\s+"))
                .filter(part -> !part.isBlank())
                .toArray(String[]::new);

        if (parts.length == 0) {
            return new String[]{"Not provided", "Not provided"};
        }
        if (parts.length == 1) {
            return new String[]{capitalize(parts[0]), "Not provided"};
        }
        return new String[]{capitalize(parts[0]), capitalize(parts[parts.length - 1])};
    }

    private String capitalize(String value) {
        if (value == null || value.isBlank()) {
            return "Not provided";
        }
        String lower = value.toLowerCase();
        return Character.toUpperCase(lower.charAt(0)) + lower.substring(1);
    }

    private String safeValue(String value) {
        return value == null || value.isBlank() ? "Unavailable" : value;
    }

    private String blankIfNull(String value) {
        return value == null ? "" : value;
    }

    private String valueOrNull(TextField field) {
        if (field == null) {
            return null;
        }
        String value = field.getText();
        return value == null || value.isBlank() ? null : value.trim();
    }

    private String passwordValue(PasswordField field) {
        if (field == null) {
            return null;
        }
        String value = field.getText();
        return value == null || value.isBlank() ? null : value;
    }

    private void clearChangePasswordForm() {
        if (currentPasswordField != null) {
            currentPasswordField.clear();
        }
        if (newPasswordField != null) {
            newPasswordField.clear();
        }
        if (confirmPasswordField != null) {
            confirmPasswordField.clear();
        }
        if (changePasswordMessageLabel != null) {
            changePasswordMessageLabel.setVisible(false);
            changePasswordMessageLabel.setManaged(false);
        }
    }

    private String profileValue(String value) {
        return value == null || value.isBlank() ? "Not provided" : value;
    }

    private Image loadAvatarImage(String avatarPath) {
        if (avatarPath == null || avatarPath.isBlank()) {
            return DEFAULT_AVATAR;
        }
        try {
            if (avatarPath.startsWith("file:") || avatarPath.startsWith("http:") || avatarPath.startsWith("https:")) {
                return new Image(avatarPath, true);
            }
            if (avatarPath.startsWith("/")) {
                if (getClass().getResourceAsStream(avatarPath) != null) {
                    return new Image(getClass().getResourceAsStream(avatarPath));
                }
            }
        } catch (Exception ignored) {
        }
        return DEFAULT_AVATAR;
    }
}
