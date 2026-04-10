package com.chatterbox.lan.controllers;


import com.chatterbox.lan.models.*;
import com.chatterbox.lan.network.*;
import com.chatterbox.lan.database.UserRepo;
import com.chatterbox.lan.utils.Loginout;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.util.Callback;
import javafx.css.PseudoClass;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
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
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.kordamp.ikonli.javafx.FontIcon;

import javax.sound.sampled.LineUnavailableException;
import java.awt.AWTException;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Optional;
import java.util.Set;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.IntStream;


public class ChatPageController {
    private static final PseudoClass SELECTED_PSEUDO_CLASS = PseudoClass.getPseudoClass("selected");
    private static final Image DEFAULT_AVATAR = new Image(
            ChatPageController.class.getResourceAsStream("/image/default.jpg")
    );

    private Client client;
    private User currentUser;
    private User viewedProfileUser;
    private Conversation viewedGroupConversation;
    private String currentConversationId;
    private Conversation currentConversation;
    private final AudioCallManager audioCallManager = new AudioCallManager();
    private final VideoCallManager videoCallManager = new VideoCallManager();
    private final UserRepo userRepo = new UserRepo();
    private final Map<String, Image> avatarCache = new ConcurrentHashMap<>();
    private final Set<String> onlineUsernames = ConcurrentHashMap.newKeySet();
    private final Map<String, Long> lastActiveTimestamps = new ConcurrentHashMap<>();
    private final ContextMenu composerEmojiMenu = new ContextMenu();
    private Message replyTarget;
    private String highlightedMessageId;
    private String activeCallPeer;
    private String activeCallMode;
    private boolean activeCallInitiatedByMe;
    private long activeCallStartedAt;
    private static final EmojiOption[] EMOJI_OPTIONS = {
            new EmojiOption("far-smile", ":smile:", "Smile"),
            new EmojiOption("far-laugh", ":laugh:", "Laugh"),
            new EmojiOption("far-grin-hearts", ":love:", "Love"),
            new EmojiOption("far-kiss-wink-heart", ":flirt:", "Flirt"),
            new EmojiOption("far-surprise", ":wow:", "Wow"),
            new EmojiOption("far-sad-tear", ":sad:", "Sad"),
            new EmojiOption("far-angry", ":angry:", "Angry"),
            new EmojiOption("fas-thumbs-up", ":thumbsup:", "Thumbs Up"),
            new EmojiOption("fas-heart", ":heart:", "Heart"),
            new EmojiOption("fas-fire", ":fire:", "Fire"),
            new EmojiOption("fas-star", ":star:", "Star"),
            new EmojiOption("fas-magic", ":spark:", "Spark")
    };

    private final ObservableList<String> friends = FXCollections.observableArrayList();
    private final Set<String> selectedGroupMembers = new HashSet<>();
    private final ObservableList<Conversation> allConversations = FXCollections.observableArrayList();
    private final FilteredList<Conversation> filteredConversations = new FilteredList<>(allConversations, conversation -> true);



    @FXML
    private ListView<Message> messageList;

    @FXML
    private ListView<Conversation> conversationList;
    @FXML
    private TextField chatSearchField;
    @FXML
    private HBox chatSearchBar;
    @FXML
    private VBox conversationLoadingOverlay;

    @FXML
    private Label inboxLabel;

    @FXML
    private HBox chatHeader;
    @FXML
    private Label chatHeaderStatusLabel;

    @FXML
    private ImageView headerAvatar;
    @FXML
    private Pane headerActiveStatusDot;

    @FXML
    private TextField inputMessage;

    @FXML
    private Button sendButton;
    @FXML
    private Button emojiPickerButton;

    @FXML
    private VBox chatComposer;
    @FXML
    private HBox replyPreviewBar;
    @FXML
    private Label replyPreviewSenderLabel;
    @FXML
    private Label replyPreviewTextLabel;

    @FXML
    private Label emptyStateLabel;

    @FXML
    private HBox callNotificationBanner;

    @FXML
    private Label callNotificationLabel;

    @FXML private VBox createConversationOverlay;
    @FXML private TextField conversationNameField;
    @FXML private ListView<String> availableUsersList;
    @FXML private VBox messageLoadingOverlay;


    @FXML private VBox addFriendOverlay;
    @FXML private TextField friendUserNameField;
    @FXML private ListView<String> friendSuggestionsList;
    @FXML private ListView<String> onlineUsersList;
    @FXML private VBox onlineUsersSection;
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
    @FXML private Button recipientInfoCloseButton;
    @FXML private ImageView ownIdAvatar;
    @FXML private Pane ownIdActiveStatusDot;
    @FXML private Label ownIdUsernameLabel;
    @FXML private Label ownIdFirstNameLabel;
    @FXML private Label ownIdLastNameLabel;
    @FXML private Label ownIdEmailLabel;
    @FXML private Label ownIdPhoneLabel;
    @FXML private Label ownIdLocationLabel;
    @FXML private VBox ownIdGroupMembersBox;
    @FXML private HBox ownIdFirstNameRow;
    @FXML private HBox ownIdLastNameRow;
    @FXML private HBox ownIdEmailRow;
    @FXML private HBox ownIdPhoneRow;
    @FXML private HBox ownIdLocationRow;
    @FXML private Label ownIdFirstNameTitleLabel;
    @FXML private Label ownIdLastNameTitleLabel;
    @FXML private Label ownIdEmailTitleLabel;
    @FXML private Label ownIdPhoneTitleLabel;
    @FXML private Label ownIdLocationTitleLabel;
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
        configureComposerEmojiMenu();
        messageList.setCellFactory(param -> new MessageCell(
                this::shouldShowSenderNames,
                this::onUnsendMessage,
                this::onReplyToMessage,
                this::onNavigateToReply,
                this::isHighlightedMessage,
                this::onReactToMessage
        ));
        setConversationSelected(false);
        conversationList.setCellFactory(list -> new ListCell<>() {

            private final Label nameLabel = new Label();
            private final ImageView avatarView = createCircularAvatar(34);
            private final Pane statusDot = createActiveStatusDot(10, 2, 1);
            private final StackPane avatarContainer = new StackPane(avatarView, statusDot);
            private final HBox container = new HBox(avatarContainer, nameLabel);

            {
                container.setSpacing(10);
                container.getStyleClass().add("conversation-list-item");
                avatarContainer.getStyleClass().add("avatar-status-container");
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
                    setNodeVisible(statusDot, shouldShowConversationOnlineDot(convo));
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
        conversationList.setItems(filteredConversations);
        if (chatSearchField != null) {
            chatSearchField.textProperty().addListener((obs, oldValue, newValue) -> applyConversationFilter());
        }
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
        if (friendSuggestionsList != null) {
            friendSuggestionsList.setCellFactory(createUserPickerCellFactory(friendSuggestionsList));
        }
        availableUsersList.setItems(friends);
        if (friendSuggestionsList != null) {
            friendSuggestionsList.setItems(FXCollections.observableArrayList());
            friendSuggestionsList.getSelectionModel().selectedItemProperty().addListener((obs, oldValue, newValue) -> {
                if (newValue != null && friendUserNameField != null) {
                    friendUserNameField.setText(newValue);
                }
            });
        }
        if (onlineUsersList != null) {
            onlineUsersList.setItems(FXCollections.observableArrayList());
            onlineUsersList.getSelectionModel().selectedItemProperty().addListener((obs, oldValue, newValue) -> {
                if (newValue != null && friendUserNameField != null) {
                    friendUserNameField.setText(newValue);
                }
            });
        }
        // Hide overlays initially
        if (createConversationOverlay != null) createConversationOverlay.setVisible(false);
        if (addFriendOverlay != null) addFriendOverlay.setVisible(false);
        if (headerAvatar != null) {
            applyCircularClip(headerAvatar, 18);
            headerAvatar.setImage(DEFAULT_AVATAR);
        }
        configureHeaderStatusDot();
        if (ownIdAvatar != null) {
            applyCircularClip(ownIdAvatar, 46);
            ownIdAvatar.setImage(DEFAULT_AVATAR);
        }
        configureOwnIdStatusDot();
        refreshActiveStatusIndicators();
        setDrawerVisible(false);
        showOwnIdSection("personal");
        if (sidebarPane != null && sidebarDrawer != null) {
            sidebarDrawer.prefHeightProperty().bind(sidebarPane.heightProperty());
        }
        if (rootContainer != null) {
            rootContainer.addEventFilter(MouseEvent.MOUSE_PRESSED, this::handleRootMousePressed);
        }
        setConversationListLoading(false);
        setMessageLoading(false);

    }

    // Called by LoginController after successful login
    public void setCurrentUser(User user, Client clientInstance) {
        this.currentUser = user;
        this.client = clientInstance;
        if (myIdButton != null && currentUser != null) {
            myIdButton.setText(currentUser.getUsername());
        }
        refreshActiveStatusIndicators();

        client.setListener(event -> {
            if ("CALL_AUDIO".equals(event.getType())) {
                handleCallAudio(event);
                return;
            }

            Platform.runLater(() -> {
                switch (event.getType()) {
                    case "NEW_MESSAGE" -> handleIncomingMessage(event);
                    case "MESSAGES_RESPONSE" -> handleMessagesResponse(event);
                    case "MESSAGE_DELETED" -> handleMessageDeleted(event);
                    case "MESSAGE_REACTION_UPDATED" -> handleMessageReactionUpdated(event);
                    case "USERS_UPDATED" -> handleUsersUpdate(event);
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
        setConversationListLoading(true);
        client.getConversations();
        client.getUsers();
    }

    private void appendMessages(List<Message> messages) {
        messageList.getItems().addAll(messages);
        if (!messages.isEmpty()) {
            messageList.scrollTo(messageList.getItems().size() - 1);
        }
    }

    private void onConversationSelected(Conversation conversation) {
        if (isRecipientProfileVisible()) {
            showProfilePane(false, false);
        }
        if (conversation == null) {
            currentConversationId = null;
            currentConversation = null;
            clearReplyPreview();
            messageList.getItems().clear();
            setConversationSelected(false);
            setMessageLoading(false);
            refreshActiveStatusIndicators();
            return;
        }
        currentConversation = conversation;
        currentConversationId = conversation.getConversationId();
        clearReplyPreview();
        if (messageList != null) {
            messageList.getItems().clear();
        }
        inboxLabel.setText(getConversationDisplayName(conversation));
        if (headerAvatar != null) {
            headerAvatar.setImage(loadConversationAvatar(conversation));
        }
        setConversationSelected(true);
        refreshActiveStatusIndicators();
        updateChatHeaderStatus();
        setMessageLoading(true);
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
        return isGroupConversation(currentConversation);
    }

    private boolean isGroupConversation(Conversation conversation) {
        if (conversation == null) {
            return false;
        }
        String name = conversation.getName();
        if (name != null && !name.isBlank()) {
            return true;
        }
        return conversation.getMembers() != null && conversation.getMembers().size() > 2;
    }

    private ImageView createCircularAvatar(double size) {
        ImageView imageView = new ImageView(DEFAULT_AVATAR);
        imageView.setFitWidth(size);
        imageView.setFitHeight(size);
        imageView.setPreserveRatio(false);
        applyCircularClip(imageView, size / 2);
        return imageView;
    }

    private Pane createActiveStatusDot(double size, double borderWidth, double offset) {
        Pane statusDot = new Pane();
        statusDot.getStyleClass().add("active-status-dot");
        statusDot.setMouseTransparent(true);
        statusDot.setPrefSize(size, size);
        statusDot.setMinSize(size, size);
        statusDot.setMaxSize(size, size);
        statusDot.setStyle("-fx-border-width: " + borderWidth + ";");
        StackPane.setAlignment(statusDot, javafx.geometry.Pos.BOTTOM_RIGHT);
        StackPane.setMargin(statusDot, new javafx.geometry.Insets(0, offset, offset, 0));
        return statusDot;
    }

    private Callback<ListView<String>, ListCell<String>> createUserPickerCellFactory(ListView<String> listView) {
        return list -> new ListCell<>() {
            private final ImageView avatarView = createCircularAvatar(28);
            private final Label nameLabel = new Label();
            private final HBox container = new HBox(avatarView, nameLabel);

            {
                container.setSpacing(10);
                container.getStyleClass().add("available-user-item");
                nameLabel.getStyleClass().add("available-user-name");
                setOnMousePressed(event -> {
                    if (event.getButton() != MouseButton.PRIMARY || getItem() == null) {
                        return;
                    }
                    listView.getSelectionModel().select(getItem());
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
                } else {
                    nameLabel.setText(username);
                    avatarView.setImage(loadAvatarForUsername(username));
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
        };
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
        activeCallPeer = targetUser;
        activeCallMode = "AUDIO";
        activeCallInitiatedByMe = true;
        activeCallStartedAt = 0L;
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
        activeCallPeer = targetUser;
        activeCallMode = "VIDEO";
        activeCallInitiatedByMe = true;
        activeCallStartedAt = 0L;
        showCallNotification("Video calling " + targetUser + "...");
    }

    @FXML
    public void onShowRecipientInfo() {
        if (currentConversation == null) {
            showInfo("Recipient Info", "Select a conversation first.");
            return;
        }

        if (isGroupConversation(currentConversation)) {
            showGroupProfile(currentConversation);
            showOwnIdSection("personal");
            showProfilePane(true, false);
            return;
        }

        String targetUser = getDirectCallTarget();
        if (targetUser == null) {
            showInfo("Recipient Info", "No saved information was found for this conversation.");
            return;
        }

        User recipient = userRepo.getUserByUsername(targetUser);
        if (recipient == null) {
            showInfo("Recipient Info", "No saved information was found for " + targetUser + ".");
            return;
        }
        showProfile(recipient, false);
        showOwnIdSection("personal");
        showProfilePane(true, false);
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
                activeCallPeer = caller;
                activeCallMode = callMode;
                activeCallInitiatedByMe = false;
                activeCallStartedAt = System.currentTimeMillis();
                client.acceptCall(caller, callMode);
            } else {
                client.rejectCall(caller, callMode);
            }
        } else {
            clearActiveCallState();
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
            activeCallPeer = peer;
            activeCallMode = callMode;
            activeCallInitiatedByMe = true;
            activeCallStartedAt = System.currentTimeMillis();
            showCallNotification("On " + callMode.toLowerCase() + " call with " + peer);
        }
    }

    private void handleCallRejected(Event event) {
        String callMode = getCallMode(event);
        String peer = event.getUsername();
        if (activeCallInitiatedByMe) {
            sendCallLogMessage(peer, callMode, false, 0L);
        }
        clearActiveCallState();
        stopActiveCall(false, callMode);
        String name = peer == null || peer.isBlank() ? "The other user" : peer;
        showStyledInfo(getCallTitle(callMode), name + " is unavailable or rejected the call.", "red");
    }

    private void handleCallEnded(Event event) {
        String callMode = getCallMode(event);
        String peer = event.getUsername();
        long durationSeconds = activeCallStartedAt > 0L ? Math.max(1L, (System.currentTimeMillis() - activeCallStartedAt) / 1000L) : 0L;
        if (activeCallInitiatedByMe) {
            sendCallLogMessage(peer, callMode, true, durationSeconds);
        }
        clearActiveCallState();
        stopActiveCall(false, callMode);
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
            showCallNotification("On audio call with " + peerUsername);
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
            long durationSeconds = activeCallStartedAt > 0L ? Math.max(1L, (System.currentTimeMillis() - activeCallStartedAt) / 1000L) : 0L;
            if (activeCallInitiatedByMe) {
                sendCallLogMessage(peer, "AUDIO", true, durationSeconds);
            }
            clearActiveCallState();
        }
        if (notifyPeer && peer != null && !peer.isBlank()) {
            client.endCall(peer);
        }
        audioCallManager.stopCall();
        hideCallNotification();
    }

    private void stopVideoCall(boolean notifyPeer) {
        String peer = videoCallManager.getPeerUsername();
        if (notifyPeer && peer != null && !peer.isBlank()) {
            long durationSeconds = activeCallStartedAt > 0L ? Math.max(1L, (System.currentTimeMillis() - activeCallStartedAt) / 1000L) : 0L;
            if (activeCallInitiatedByMe) {
                sendCallLogMessage(peer, "VIDEO", true, durationSeconds);
            }
            clearActiveCallState();
        }
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

    private String getConversationIdForDirectUser(String username) {
        if (username == null || username.isBlank() || conversationList == null || currentUser == null) {
            return null;
        }
        String normalizedTarget = username.trim();
        String currentUsername = currentUser.getUsername();
        return conversationList.getItems().stream()
                .filter(conversation -> conversation.getMembers() != null
                        && conversation.getMembers().size() == 2
                        && conversation.getMembers().stream().anyMatch(member -> member != null && member.equalsIgnoreCase(normalizedTarget))
                        && conversation.getMembers().stream().anyMatch(member -> member != null && member.equalsIgnoreCase(currentUsername)))
                .map(Conversation::getConversationId)
                .findFirst()
                .orElse(null);
    }

    private String formatCallDuration(long totalSeconds) {
        long hours = totalSeconds / 3600;
        long minutes = totalSeconds / 60;
        long seconds = totalSeconds % 60;
        if (hours > 0) {
            long remainingMinutes = (totalSeconds % 3600) / 60;
            return String.format("%d:%02d:%02d", hours, remainingMinutes, seconds);
        }
        return String.format("%d:%02d", minutes, seconds);
    }

    private void sendCallLogMessage(String peerUsername, String callMode, boolean completed, long durationSeconds) {
        if (client == null) {
            return;
        }
        String conversationId = getConversationIdForDirectUser(peerUsername);
        if (conversationId == null) {
            return;
        }
        String callLabel = "VIDEO".equals(callMode) ? "Video Call" : "Audio Call";
        String text = completed ? callLabel : "Missed " + callLabel;
        String accent = completed ? "deepskyblue" : "#ef4444";
        String details = completed && durationSeconds > 0L ? formatCallDuration(durationSeconds) : null;
        client.sendCallLogMessage(conversationId, text, "VIDEO".equals(callMode) ? "fas-video" : "fas-phone", accent, details);
    }

    private void clearActiveCallState() {
        activeCallPeer = null;
        activeCallMode = null;
        activeCallInitiatedByMe = false;
        activeCallStartedAt = 0L;
    }



    // for loading conversations in the left pane when user logs in
    private void handleConversationsUpdate(Event event) {
        @SuppressWarnings("unchecked")
        List<Conversation> conversations = (List<Conversation>) event.getData("conversations");

        if (conversations == null) {
            System.err.println("No conversations in response");
            setConversationListLoading(false);
            return;
        }
        for (Conversation conv : conversations) {
            for (String member : conv.getMembers()) {
                if (!member.equals(currentUser.getUsername()) && !friends.contains(member)) {
                    friends.add(member);
                }
            }
        }
        allConversations.setAll(conversations);
        applyConversationFilter();
        setConversationListLoading(false);
        System.out.println("[CONTROLLER] Updated conversations: " + conversations.size() + " conversations");
    }

    private void handleNewConversation(Event event) {
        Conversation newConversation = (Conversation) event.getData("conversation");
        if (newConversation == null) {
            System.err.println("No conversation data in NEW_CONVERSATION event");
            return;
        }
        boolean exists = allConversations.stream()
                .anyMatch(conversation -> conversation.getConversationId().equals(newConversation.getConversationId()));
        if (!exists) {
            allConversations.add(newConversation);
            applyConversationFilter();
        }
        for (String member : newConversation.getMembers()) {
            if (!member.equals(currentUser.getUsername()) && !friends.contains(member)) {
                friends.add(member);
            }
        }
        Conversation createdConversation = allConversations.stream()
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

    private void handleUsersUpdate(Event event) {
        @SuppressWarnings("unchecked")
        List<String> suggestions = (List<String>) event.getData("suggestions");
        @SuppressWarnings("unchecked")
        List<String> onlineUsers = (List<String>) event.getData("onlineUsers");
        @SuppressWarnings("unchecked")
        Map<String, Long> lastActive = (Map<String, Long>) event.getData("lastActiveTimestamps");

        LinkedHashMap<String, String> combinedSuggestions = new LinkedHashMap<>();
        if (onlineUsers != null) {
            for (String username : onlineUsers) {
                combinedSuggestions.put(username, username);
            }
        }
        if (suggestions != null) {
            for (String username : suggestions) {
                combinedSuggestions.putIfAbsent(username, username);
            }
        }

        if (friendSuggestionsList != null) {
            friendSuggestionsList.getItems().setAll(combinedSuggestions.values());
        }
        onlineUsernames.clear();
        if (onlineUsersList != null) {
            onlineUsersList.getItems().setAll(onlineUsers == null ? List.of() : onlineUsers);
        }
        if (onlineUsers != null) {
            onlineUsernames.addAll(onlineUsers);
        }
        lastActiveTimestamps.clear();
        if (lastActive != null) {
            lastActiveTimestamps.putAll(lastActive);
        }
        if (onlineUsersSection != null) {
            boolean hasOnlineUsers = onlineUsers != null && !onlineUsers.isEmpty();
            onlineUsersSection.setVisible(hasOnlineUsers);
            onlineUsersSection.setManaged(hasOnlineUsers);
        }
        refreshActiveStatusIndicators();
        updateChatHeaderStatus();
        if (conversationList != null) {
            conversationList.refresh();
        }
    }


    // for loading new incoming message in real-time
    private void handleIncomingMessage(Event event) {
        if (currentConversationId == null || !currentConversationId.equals(event.getConversationId())) return;

        String messageId = (String) event.getData("messageId");
        User sender = new User(event.getUsername(), "/image/default.jpg");
        sender.setMe(sender.getUsername().equals(currentUser.getUsername()));
        String messageType = event.getData("messageType") instanceof String type ? type : "TEXT";
        Message incoming;
        if ("FILE".equals(messageType)) {
            incoming = new Message(
                    sender,
                    (String) event.getData("fileName"),
                    (byte[]) event.getData("fileData"),
                    event.getConversationId()
            );
        } else {
            incoming = new Message(sender, event.getText(), event.getConversationId());
            if ("CALL_EVENT".equals(messageType)) {
                incoming.markAsCallEvent();
            }
        }
        incoming.setReplyToMessageId((String) event.getData("replyToMessageId"));
        incoming.setReplyToSenderName((String) event.getData("replyToSenderName"));
        incoming.setReplyToText((String) event.getData("replyToText"));
        incoming.setCallIconLiteral((String) event.getData("callIconLiteral"));
        incoming.setCallAccentColor((String) event.getData("callAccentColor"));
        incoming.setCallDetails((String) event.getData("callDetails"));
        appendMessages(List.of(incoming));
        incoming.setId(messageId);
    }

    // for loading all message of a conversation when selected
    private void handleMessagesResponse(Event event) {
        if (event.getConversationId() == null || !event.getConversationId().equals(currentConversationId)) {
            return;
        }

        List<Message> messages = (List<Message>) event.getData("messages");
        if (messages == null) {
            System.err.println("No messages in response");
            setMessageLoading(false);
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
        setMessageLoading(false);
    }

    private void handleMessageDeleted(Event event) {
        String conversationId = event.getConversationId();
        String messageId = (String) event.getData("messageId");

        if (conversationId == null || messageId == null || !conversationId.equals(currentConversationId)) {
            return;
        }

        boolean changed = false;
        for (Message m : messageList.getItems()) {
            if (m != null && messageId.equals(m.getId())) {
                m.setDeleted(true);
                if (m.getText() == null || m.getText().isBlank()) {
                    m.setText("Message unsent");
                }
                changed = true;
                break;
            }
        }

        if (changed) {
            messageList.refresh();
        }
    }

    private void handleMessageReactionUpdated(Event event) {
        if (event.getConversationId() == null || !event.getConversationId().equals(currentConversationId)) {
            return;
        }
        String messageId = (String) event.getData("messageId");
        @SuppressWarnings("unchecked")
        Map<String, String> reactions = (Map<String, String>) event.getData("reactions");
        if (messageId == null || reactions == null) {
            return;
        }
        for (Message message : messageList.getItems()) {
            if (message != null && messageId.equals(message.getId())) {
                message.setReactions(reactions);
                messageList.refresh();
                break;
            }
        }
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
        client.sendMessage(currentConversationId, text, replyTarget);
        inputMessage.clear();
        clearReplyPreview();
    }

    @FXML
    public void onOpenComposerEmojiPicker() {
        if (emojiPickerButton == null || inputMessage == null) {
            return;
        }
        if (composerEmojiMenu.isShowing()) {
            composerEmojiMenu.hide();
            return;
        }
        composerEmojiMenu.show(inputMessage, javafx.geometry.Side.TOP, 0, 4);
    }

    @FXML
    public void onUnsendMessage(Message message) {
        String messageId = message.getId();
        if (message == null || currentConversationId == null || message.getSender() == null || currentUser == null) {
            return;
        }
        if (!currentUser.getUsername().equals(message.getSender().getUsername())) {
            return;
        }
        if (messageId == null || messageId.isBlank()) {
            System.err.println("Cannot unsend: message ID is missing");
            return;
        }
        client.unsendMessage(currentConversationId, messageId);
    }

    public void onReactToMessage(Message message, String emoji) {
        if (message == null || emoji == null || emoji.isBlank() || currentConversationId == null || client == null) {
            return;
        }
        String requestedReaction = emoji;
        if (currentUser != null) {
            Map<String, String> reactions = new LinkedHashMap<>(message.getReactions());
            String currentReaction = reactions.get(currentUser.getUsername());
            if (emoji.equals(currentReaction)) {
                reactions.remove(currentUser.getUsername());
                requestedReaction = "";
            } else {
                reactions.put(currentUser.getUsername(), emoji);
            }
            message.setReactions(reactions);
            if (messageList != null) {
                messageList.refresh();
            }
        }
        if (message.getId() == null || message.getId().isBlank()) {
            return;
        }
        client.addReaction(currentConversationId, message.getId(), requestedReaction);
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

    public void onReplyToMessage(Message message) {
        if (message == null || message.isDeleted() || inputMessage == null) {
            return;
        }
        replyTarget = message;
        if (replyPreviewSenderLabel != null) {
            String senderName = message.getSender() == null || message.getSender().getUsername() == null
                    ? "Unknown"
                    : message.getSender().getUsername();
            replyPreviewSenderLabel.setText("Replying to " + senderName);
        }
        if (replyPreviewTextLabel != null) {
            replyPreviewTextLabel.setText(buildReplyPreview(message));
        }
        if (replyPreviewBar != null) {
            replyPreviewBar.setVisible(true);
            replyPreviewBar.setManaged(true);
        }
        inputMessage.requestFocus();
    }

    @FXML
    public void onCancelReply() {
        clearReplyPreview();
    }

    public void onNavigateToReply(Message message) {
        if (message == null || !message.hasReply() || messageList == null) {
            return;
        }
        int targetIndex = IntStream.range(0, messageList.getItems().size())
                .filter(index -> {
                    Message item = messageList.getItems().get(index);
                    return item != null && message.getReplyToMessageId().equals(item.getId());
                })
                .findFirst()
                .orElse(-1);
        if (targetIndex < 0) {
            return;
        }
        highlightedMessageId = message.getReplyToMessageId();
        messageList.scrollTo(targetIndex);
        messageList.refresh();
        PauseTransition clearHighlightDelay = new PauseTransition(Duration.seconds(2));
        clearHighlightDelay.setOnFinished(event -> {
            highlightedMessageId = null;
            messageList.refresh();
        });
        clearHighlightDelay.play();
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

    private void clearReplyPreview() {
        replyTarget = null;
        if (replyPreviewBar != null) {
            replyPreviewBar.setVisible(false);
            replyPreviewBar.setManaged(false);
        }
        if (replyPreviewSenderLabel != null) {
            replyPreviewSenderLabel.setText("");
        }
        if (replyPreviewTextLabel != null) {
            replyPreviewTextLabel.setText("");
        }
    }

    private String buildReplyPreview(Message message) {
        String preview;
        if (message.isDeleted()) {
            preview = "Message unsent";
        } else if (message.isFileMessage()) {
            preview = message.getFileName() == null || message.getFileName().isBlank()
                    ? "File"
                    : "File: " + message.getFileName();
        } else {
            preview = message.getText() == null ? "" : message.getText();
        }
        if (preview.length() > 80) {
            return preview.substring(0, 77) + "...";
        }
        return preview;
    }

    public boolean isHighlightedMessage(Message message) {
        return message != null
                && highlightedMessageId != null
                && highlightedMessageId.equals(message.getId());
    }

    private void appendEmojiToInput(String emoji) {
        if (inputMessage == null || emoji == null) {
            return;
        }
        String currentText = inputMessage.getText() == null ? "" : inputMessage.getText();
        String spacer = currentText.isBlank() || currentText.endsWith(" ") ? "" : " ";
        String updatedText = currentText + spacer + emoji;
        int caretPosition = updatedText.length();
        inputMessage.setText(updatedText);
        Platform.runLater(() -> {
            inputMessage.requestFocus();
            inputMessage.deselect();
            inputMessage.positionCaret(caretPosition);
        });
    }

    private void configureComposerEmojiMenu() {
        composerEmojiMenu.getItems().clear();
        HBox emojiRow = new HBox(6);
        emojiRow.getStyleClass().add("emoji-picker-row");
        for (EmojiOption emoji : EMOJI_OPTIONS) {
            Button button = new Button();
            button.getStyleClass().add("emoji-picker-button");
            FontIcon icon = new FontIcon(emoji.iconLiteral());
            icon.getStyleClass().add("emoji-picker-icon");
            button.setGraphic(icon);
            button.setFocusTraversable(false);
            button.setOnAction(event -> {
                appendEmojiToInput(emoji.textValue());
                composerEmojiMenu.hide();
            });
            emojiRow.getChildren().add(button);
        }
        CustomMenuItem rowItem = new CustomMenuItem(emojiRow, false);
        rowItem.getStyleClass().add("emoji-picker-item");
        composerEmojiMenu.getItems().add(rowItem);
    }

    private record EmojiOption(String iconLiteral, String textValue, String label) {}

    @FXML
    public void onOpenSettings() {
        setDrawerVisible(false);
        showInfo("Setting", "Settings are not implemented yet.");
    }

    @FXML
    public void onShowOwnId() {
        setDrawerVisible(false);
        showProfile(currentUser, true);
        populateEditDetailsForm();
        showOwnIdSection("personal");
        showProfilePane(true, true);
    }

    @FXML
    public void onShowChats() {
        setDrawerVisible(false);
        showProfilePane(false, false);
    }

    @FXML
    public void onCloseOwnId() {
        showProfilePane(false, false);
    }

    @FXML
    public void onCloseRecipientInfo() {
        viewedGroupConversation = null;
        showProfilePane(false, false);
    }

    @FXML
    public void onShowPersonalDetails() {
        showOwnIdSection("personal");
    }

    @FXML
    public void onShowEditDetails() {
        if (!isViewingOwnProfile()) {
            return;
        }
        populateEditDetailsForm();
        showOwnIdSection("edit");
    }

    @FXML
    public void onShowChangePassword() {
        if (!isViewingOwnProfile()) {
            return;
        }
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
        if (friendSuggestionsList != null) {
            friendSuggestionsList.getItems().clear();
        }
        if (onlineUsersList != null) {
            onlineUsersList.getItems().clear();
        }
        if (client != null) {
            client.getUsers();
        }
    }

    @FXML
    public void onCloseAddFriend() {
        if (addFriendOverlay != null) {
            addFriendOverlay.setVisible(false);
            addFriendOverlay.setManaged(false);
        }
        friendUserNameField.clear();
        if (friendSuggestionsList != null) {
            friendSuggestionsList.getSelectionModel().clearSelection();
        }
        if (onlineUsersList != null) {
            onlineUsersList.getSelectionModel().clearSelection();
        }
    }
    @FXML
    public void onSubmitAddFriend() {
        String friendUsername = friendUserNameField.getText().trim();

        if (friendUsername.isEmpty()) {
            showStyledInfo("Add Friend", "Enter a username.", "red");
            return;
        }
        if (currentUser != null && friendUsername.equalsIgnoreCase(currentUser.getUsername())) {
            showStyledInfo("Add Friend", "You cannot add yourself.", "red");
            return;
        }
        if (!userRepo.usernameExists(friendUsername)) {
            showStyledInfo("Add Friend", "Username does not exist: " + friendUsername, "red");
            return;
        }
        if (getConversationIdForDirectUser(friendUsername) != null) {
            showStyledInfo("Add Friend", "This person is already in your chat list.", "red");
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
        if (ownIdUsernameLabel == null) {
            return;
        }
        if (viewedGroupConversation != null) {
            populateGroupInfoCard(viewedGroupConversation);
            return;
        }
        User profileUser = viewedProfileUser != null ? viewedProfileUser : currentUser;
        if (profileUser == null) {
            return;
        }

        configureProfileInfoRows();
        String username = safeValue(profileUser.getUsername());
        ownIdUsernameLabel.setText(username);
        ownIdFirstNameLabel.setText(profileValue(profileUser.getFirstName()));
        ownIdLastNameLabel.setText(profileValue(profileUser.getLastName()));
        ownIdEmailLabel.setText(profileValue(profileUser.getEmail()));
        ownIdPhoneLabel.setText(profileValue(profileUser.getPhoneNumber()));
        ownIdLocationLabel.setText(profileValue(profileUser.getLocation()));
        setNodeVisible(ownIdGroupMembersBox, false);
        ownIdAvatar.setImage(loadAvatarImage(profileUser.getAvatarPath()));
        refreshActiveStatusIndicators();
    }

    private void showProfile(User profileUser, boolean ownProfile) {
        viewedProfileUser = profileUser;
        viewedGroupConversation = null;
        populateOwnIdCard();
        updateOwnIdNavigationForProfile(ownProfile);
    }

    private void showGroupProfile(Conversation conversation) {
        viewedProfileUser = null;
        viewedGroupConversation = conversation;
        populateOwnIdCard();
        updateOwnIdNavigationForProfile(false);
    }

    private void refreshActiveStatusIndicators() {
        setNodeVisible(headerActiveStatusDot, shouldShowConversationOnlineDot(currentConversation));
        setNodeVisible(ownIdActiveStatusDot, shouldShowProfileOnlineDot());
        if (headerActiveStatusDot != null) {
            headerActiveStatusDot.toFront();
        }
        if (ownIdActiveStatusDot != null) {
            ownIdActiveStatusDot.toFront();
        }
    }

    private void updateChatHeaderStatus() {
        if (chatHeaderStatusLabel == null) {
            return;
        }
        String target = getConversationOnlineTarget(currentConversation);
        if (target == null) {
            chatHeaderStatusLabel.setText("");
            chatHeaderStatusLabel.setVisible(false);
            chatHeaderStatusLabel.setManaged(false);
            return;
        }
        String statusText = buildLastActiveText(target);
        chatHeaderStatusLabel.setText(statusText);
        chatHeaderStatusLabel.setVisible(true);
        chatHeaderStatusLabel.setManaged(true);
    }

    private String buildLastActiveText(String username) {
        if (shouldShowUserOnlineDot(username)) {
            return "Active now";
        }
        Long lastActiveMillis = lastActiveTimestamps.get(username);
        if (lastActiveMillis == null) {
            return "Active recently";
        }
        java.time.Duration elapsed = java.time.Duration.between(Instant.ofEpochMilli(lastActiveMillis), Instant.now());
        long minutes = Math.max(1, elapsed.toMinutes());
        if (minutes < 60) {
            return "Active " + minutes + (minutes == 1 ? " minute ago" : " minutes ago");
        }
        long hours = Math.max(1, elapsed.toHours());
        if (hours < 24) {
            return "Active " + hours + (hours == 1 ? " hour ago" : " hours ago");
        }
        long days = Math.max(1, elapsed.toDays());
        return "Active " + days + (days == 1 ? " day ago" : " days ago");
    }

    private boolean shouldShowConversationOnlineDot(Conversation conversation) {
        String target = getConversationOnlineTarget(conversation);
        return shouldShowUserOnlineDot(target);
    }

    private boolean shouldShowProfileOnlineDot() {
        if (viewedGroupConversation != null) {
            return false;
        }
        User profileUser = viewedProfileUser != null ? viewedProfileUser : currentUser;
        return profileUser != null && shouldShowUserOnlineDot(profileUser.getUsername());
    }

    private boolean shouldShowUserOnlineDot(String username) {
        if (username == null || username.isBlank()) {
            return false;
        }
        if (currentUser != null && username.equals(currentUser.getUsername())) {
            return true;
        }
        return onlineUsernames.contains(username);
    }

    private String getConversationOnlineTarget(Conversation conversation) {
        if (conversation == null || currentUser == null || conversation.getMembers() == null || conversation.getMembers().size() != 2) {
            return null;
        }
        for (String member : conversation.getMembers()) {
            if (!member.equals(currentUser.getUsername())) {
                return member;
            }
        }
        return null;
    }

    private void setNodeVisible(Node node, boolean visible) {
        if (node == null) {
            return;
        }
        node.setVisible(visible);
        node.setManaged(visible);
    }

    private void configureOwnIdStatusDot() {
        if (ownIdActiveStatusDot == null) {
            return;
        }
        ownIdActiveStatusDot.setPrefSize(18, 18);
        ownIdActiveStatusDot.setMinSize(18, 18);
        ownIdActiveStatusDot.setMaxSize(18, 18);
        ownIdActiveStatusDot.setStyle(
                "-fx-border-width: 1.5;" +
                        "-fx-background-radius: 999;" +
                        "-fx-border-radius: 999;"
        );
    }

    private void configureHeaderStatusDot() {
        if (headerActiveStatusDot == null) {
            return;
        }
        headerActiveStatusDot.setPrefSize(10, 10);
        headerActiveStatusDot.setMinSize(10, 10);
        headerActiveStatusDot.setMaxSize(10, 10);
        headerActiveStatusDot.setStyle(
                "-fx-border-width: 2;" +
                        "-fx-background-radius: 999;" +
                        "-fx-border-radius: 999;"
        );
    }

    private void setConversationListLoading(boolean loading) {
        setNodeVisible(conversationLoadingOverlay, loading);
    }

    private void setMessageLoading(boolean loading) {
        setNodeVisible(messageLoadingOverlay, loading);
    }

    private boolean isViewingOwnProfile() {
        return viewedGroupConversation == null
                && viewedProfileUser != null
                && currentUser != null
                && safeEquals(viewedProfileUser.getUsername(), currentUser.getUsername());
    }

    private void updateOwnIdNavigationForProfile(boolean ownProfile) {
        boolean groupProfile = viewedGroupConversation != null;
        setSectionVisible(editDetailsButton, ownProfile);
        setSectionVisible(changePasswordButton, ownProfile);
        setNodeVisible(recipientInfoCloseButton, !ownProfile || groupProfile);
        if (recipientInfoCloseButton != null && (!ownProfile || groupProfile)) {
            recipientInfoCloseButton.toFront();
        }
        if (!ownProfile) {
            updateOwnIdNavButton(editDetailsButton, false);
            updateOwnIdNavButton(changePasswordButton, false);
        }
        refreshActiveStatusIndicators();
    }

    private boolean isRecipientProfileVisible() {
        return ownIdPage != null && ownIdPage.isVisible() && !isViewingOwnProfile();
    }

    private void populateGroupInfoCard(Conversation conversation) {
        configureGroupInfoRows();
        String groupName = getConversationDisplayName(conversation);
        ownIdUsernameLabel.setText(safeValue(groupName));
        ownIdAvatar.setImage(headerAvatar != null && headerAvatar.getImage() != null ? headerAvatar.getImage() : DEFAULT_AVATAR);
        ownIdLastNameLabel.setText("");
        ownIdFirstNameLabel.setText("");
        ownIdEmailLabel.setText("");
        ownIdPhoneLabel.setText("");
        ownIdLocationLabel.setText("");
        populateGroupMembersBox(conversation);
        refreshActiveStatusIndicators();
    }

    private void configureProfileInfoRows() {
        setOwnIdRow(ownIdFirstNameRow, ownIdFirstNameTitleLabel, "First Name:", true);
        setOwnIdRow(ownIdLastNameRow, ownIdLastNameTitleLabel, "Second Name:", true);
        setOwnIdRow(ownIdEmailRow, ownIdEmailTitleLabel, "Email:", true);
        setOwnIdRow(ownIdPhoneRow, ownIdPhoneTitleLabel, "Phone number:", true);
        setOwnIdRow(ownIdLocationRow, ownIdLocationTitleLabel, "Location:", true);
        setNodeVisible(ownIdGroupMembersBox, false);
        setNodeVisible(ownIdLastNameLabel, true);
    }

    private void configureGroupInfoRows() {
        setOwnIdRow(ownIdFirstNameRow, ownIdFirstNameTitleLabel, "Group Name:", false);
        setOwnIdRow(ownIdLastNameRow, ownIdLastNameTitleLabel, "Members:", true);
        setOwnIdRow(ownIdEmailRow, ownIdEmailTitleLabel, "Total Members:", false);
        setOwnIdRow(ownIdPhoneRow, ownIdPhoneTitleLabel, "Phone number:", false);
        setOwnIdRow(ownIdLocationRow, ownIdLocationTitleLabel, "Location:", false);
        setNodeVisible(ownIdGroupMembersBox, true);
        setNodeVisible(ownIdLastNameLabel, false);
    }

    private void setOwnIdRow(HBox row, Label titleLabel, String title, boolean visible) {
        setSectionVisible(row, visible);
        if (titleLabel != null) {
            titleLabel.setText(title);
        }
    }

    private void populateGroupMembersBox(Conversation conversation) {
        if (ownIdGroupMembersBox == null) {
            return;
        }
        ownIdGroupMembersBox.getChildren().clear();
        if (conversation == null || conversation.getMembers() == null || conversation.getMembers().isEmpty()) {
            Label emptyLabel = new Label("No members");
            emptyLabel.getStyleClass().add("own-id-group-member-empty");
            ownIdGroupMembersBox.getChildren().add(emptyLabel);
            return;
        }

        for (String member : conversation.getMembers()) {
            if (member == null || member.isBlank()) {
                continue;
            }
            ImageView avatarView = createCircularAvatar(28);
            avatarView.setImage(loadAvatarForUsername(member));

            Label nameLabel = new Label(currentUser != null && member.equals(currentUser.getUsername()) ? member + " (You)" : member);
            nameLabel.getStyleClass().add("own-id-group-member-name");

            HBox memberRow = new HBox(10, avatarView, nameLabel);
            memberRow.getStyleClass().add("own-id-group-member-row");
            memberRow.setAlignment(Pos.CENTER_LEFT);
            ownIdGroupMembersBox.getChildren().add(memberRow);
        }
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

        if (!isValidEmail(email)) {
            showEditDetailsMessage("Invalid Email", true);
            return;
        }

        if (!isDigitsOnly(phone)) {
            showEditDetailsMessage("Invalid Phone Number", true);
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

    private void showProfilePane(boolean visible, boolean showOwnIdSidebar) {
        if (!visible) {
            viewedProfileUser = null;
            viewedGroupConversation = null;
        }
        if (chatContent != null) {
            chatContent.setVisible(!visible);
            chatContent.setManaged(!visible);
        }
        if (ownIdPage != null) {
            ownIdPage.setVisible(visible);
            ownIdPage.setManaged(visible);
        }
        if (sidebarTitleLabel != null) {
            sidebarTitleLabel.setVisible(!visible || !showOwnIdSidebar);
            sidebarTitleLabel.setManaged(!visible || !showOwnIdSidebar);
        }
        if (chatSearchBar != null) {
            chatSearchBar.setVisible(!visible || !showOwnIdSidebar);
            chatSearchBar.setManaged(!visible || !showOwnIdSidebar);
        }
        if (conversationList != null) {
            conversationList.setVisible(!visible || !showOwnIdSidebar);
            conversationList.setManaged(!visible || !showOwnIdSidebar);
        }
        if (ownIdSidebarSections != null) {
            ownIdSidebarSections.setVisible(visible && showOwnIdSidebar);
            ownIdSidebarSections.setManaged(visible && showOwnIdSidebar);
        }
        if (addFriendButton != null) {
            addFriendButton.setVisible(!visible || !showOwnIdSidebar);
            addFriendButton.setManaged(!visible || !showOwnIdSidebar);
        }
        if (createGroupButton != null) {
            createGroupButton.setVisible(!visible || !showOwnIdSidebar);
            createGroupButton.setManaged(!visible || !showOwnIdSidebar);
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
        } else {
            updateOwnIdNavigationForProfile(false);
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

    private void applyConversationFilter() {
        String query = chatSearchField == null || chatSearchField.getText() == null
                ? ""
                : chatSearchField.getText().trim().toLowerCase();
        filteredConversations.setPredicate(conversation -> {
            if (query.isEmpty()) {
                return true;
            }
            String displayName = getConversationDisplayName(conversation);
            if (displayName != null && displayName.toLowerCase().contains(query)) {
                return true;
            }
            if (conversation != null && conversation.getMembers() != null) {
                return conversation.getMembers().stream()
                        .filter(member -> member != null)
                        .anyMatch(member -> member.toLowerCase().contains(query));
            }
            return false;
        });
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

    private boolean safeEquals(String left, String right) {
        if (left == null) {
            return right == null;
        }
        return left.equals(right);
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

    private boolean isValidEmail(String email) {
        return email != null && email.contains("@");
    }

    private boolean isDigitsOnly(String value) {
        return value != null && !value.isBlank() && value.chars().allMatch(Character::isDigit);
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
