package com.team4.controller;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.team4.client.ApiClient;
import com.team4.model.Item;
import com.team4.util.StageUtils;
import com.team4.util.UserSession;
import javafx.animation.PauseTransition;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.scene.Scene;
import javafx.stage.Popup;
import javafx.util.Duration;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URL;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.Set;

public class MainController implements Initializable {

    @FXML private BorderPane mainRoot;
    @FXML private VBox navContainer;
    @FXML private StackPane contentArea;
    @FXML private Label pageTitle, pageSubtitle;
    @FXML private Label userNameLabel, userRoleBadge;
    @FXML private Label userAvatarText;
    @FXML private Label userAvatarRoleText;
    @FXML private Label balanceValueLabel;
    @FXML private StackPane userAvatarBg;
    @FXML private Button notiBtn;
    @FXML private Label notiBadge;
    @FXML private Button depositBtn;

    private static final NumberFormat MONEY_FORMAT = NumberFormat.getNumberInstance(Locale.US);

    private String userRole = "bidder";
    private String currentPage = "";
    private final ObservableList<AppNotification> notifications = FXCollections.observableArrayList();
    private final Set<String> runtimeNotificationKeys = new HashSet<>();
    private Popup notificationPopup;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        updateNotificationBadge();
    }

    public void setUserRole(String role) {
        this.userRole = role;
        setupSidebar();
        updateUserInfo();
        updateBalanceVisibility();
        updateDepositVisibility();
        refreshUserDisplayName();
        refreshUserBalance();

        if (role != null && role.startsWith("admin")) {
            loadPage("admin_dashboard", "Dashboard", "Monitor and manage the entire system");
        } else if ("seller".equals(role)) {
            loadPage("seller_products", "Products", "Manage auction products");
        } else {
            loadPage("bidder_auctions", "Auctions", "Explore and join auctions");
        }
        refreshNotifications();
    }

    private void setupSidebar() {
        navContainer.getChildren().clear();

        if (userRole != null && userRole.startsWith("admin")) {
            addNavItem("Dashboard", "admin_dashboard", "Monitor and manage the system");
            addNavItem("Users", "admin_users", "Lock/unlock accounts");
            addNavItem("Auctions", "admin_auctions", "Approve and manage auctions");
            addNavItem("Profile", "profile", "Update your information");
        } else if ("seller".equals(userRole)) {
            addNavItem("Products", "seller_products", "Manage your products");
            addNavItem("My Auctions", "bidder_auctions", "Sessions you created");
            addNavItem("Profile", "profile", "Update your information");
        } else {
            addNavItem("Auction Room", "bidder_auctions", "Explore active auction sessions");
            addNavItem("Owned Items", "bidder_owned_items", "Collection");
            addNavItem("Profile", "profile", "Update your information");
        }

        if (!navContainer.getChildren().isEmpty() && navContainer.getChildren().get(0) instanceof Button) {
            selectNavButton((Button) navContainer.getChildren().get(0));
        }
    }

    private void addNavItem(String label, String pageId, String subtitle) {
        Button btn = new Button(label);
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.getStyleClass().add("nav-btn");
        btn.setUserData(pageId);
        btn.setOnAction(e -> {
            selectNavButton(btn);
            loadPage(pageId, label, subtitle);
        });
        navContainer.getChildren().add(btn);
    }

    private void selectNavButton(Button selected) {
        for (Node n : navContainer.getChildren()) {
            if (n instanceof Button) {
                Button b = (Button) n;
                b.getStyleClass().removeAll("nav-btn-active");
                if (!b.getStyleClass().contains("nav-btn")) {
                    b.getStyleClass().add("nav-btn");
                }
            }
        }
        selected.getStyleClass().removeAll("nav-btn");
        selected.getStyleClass().add("nav-btn-active");
    }

    private void updateUserInfo() {
        String name;
        String roleText;

        if (userRole != null && userRole.startsWith("admin")) {
            name = "Admin";
            roleText = "admin_super".equals(userRole) ? "SUPER ADMIN" : "ADMIN";
        } else if ("seller".equals(userRole)) {
            name = "Seller";
            roleText = "SELLER";
        } else {
            name = "Buyer";
            roleText = "BIDDER";
        }

        UserSession session = UserSession.getInstance();
        if (session != null) {
            name = firstPresent(session.getFullName(), session.getUsername(), name);
        }

        userNameLabel.setText(name);
        userRoleBadge.setText(roleText);
        updateAvatarInitial(name);
        updateAvatarRoleCode();
    }

    private void refreshUserDisplayName() {
        UserSession session = UserSession.getInstance();
        if (session == null || session.getUserId() == null || session.getUserId().isBlank()) {
            return;
        }

        javafx.concurrent.Task<JsonObject> task = new javafx.concurrent.Task<>() {
            @Override
            protected JsonObject call() throws Exception {
                return new ApiClient().getUserProfile(session.getUserId());
            }
        };

        task.setOnSucceeded(e -> {
            JsonObject profile = task.getValue();
            if (profile == null) {
                return;
            }
            if (profile.has("fullName") && !profile.get("fullName").isJsonNull()) {
                String fullName = profile.get("fullName").getAsString();
                session.setFullName(fullName);
                userNameLabel.setText(firstPresent(fullName, session.getUsername(), "User"));
                updateAvatarInitial(fullName);
                updateAvatarRoleCode();
            }
        });

        Thread thread = new Thread(task);
        thread.setDaemon(true);
        thread.start();
    }

    public Object loadPage(String pageId, String title, String subtitle) {
        this.currentPage = pageId;
        pageTitle.setText(title);
        pageSubtitle.setText(subtitle);

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/team4/view/" + pageId + ".fxml"));
            Parent page = loader.load();

            // Inject MainController reference into supported controllers
            Object controller = loader.getController();
            if (controller instanceof BidderAuctionsController) {
                ((BidderAuctionsController) controller).setMainController(this);
            } else if (controller instanceof BiddingRoomController) {
                ((BiddingRoomController) controller).setMainController(this);
            } else if (controller instanceof BidderOwnedItemsController) {
                ((BidderOwnedItemsController) controller).setMainController(this);
            } else if (controller instanceof AdminDashboardController) {
                ((AdminDashboardController) controller).setMainController(this);
            } else if (controller instanceof ProfileController) {
                ((ProfileController) controller).setMainController(this);
            }

            contentArea.getChildren().clear();
            contentArea.getChildren().add(page);
            if (!isAdminRole()) {
                refreshUserBalance();
            }
            return controller;
        } catch (Exception ex) {
            ex.printStackTrace();
            Label placeholder = new Label("Page " + title + " (under development)");
            placeholder.getStyleClass().add("muted-text");
            contentArea.getChildren().clear();
            contentArea.getChildren().add(placeholder);
            return null;
        }
    }

    public Object navigateByPageId(String pageId) {
        for (Node n : navContainer.getChildren()) {
            if (n instanceof Button) {
                Button btn = (Button) n;
                if (pageId.equals(btn.getUserData())) {
                    selectNavButton(btn);
                    String title = btn.getText();
                    String subtitle = "";
                    if ("admin_dashboard".equals(pageId)) subtitle = "Monitor and manage the system";
                    else if ("admin_users".equals(pageId)) subtitle = "Lock/unlock accounts";
                    else if ("admin_auctions".equals(pageId)) subtitle = "Approve and manage auctions";
                    else if ("profile".equals(pageId)) subtitle = "Update your information";
                    
                    return loadPage(pageId, title, subtitle);
                }
            }
        }
        return loadPage(pageId, pageId, "");
    }

    public void refreshUserBalance() {
        UserSession session = UserSession.getInstance();
        if (balanceValueLabel == null || session == null || session.getUserId() == null) {
            return;
        }
        if (isAdminRole()) {
            updateBalanceVisibility();
            return;
        }

        balanceValueLabel.setText("Balance: " + formatMoney(session.getBalance()));

        javafx.concurrent.Task<com.google.gson.JsonObject> task = new javafx.concurrent.Task<>() {
            @Override
            protected com.google.gson.JsonObject call() throws Exception {
                return new ApiClient().getUserProfile(session.getUserId());
            }
        };
        task.setOnSucceeded(e -> {
            JsonObject profile = task.getValue();
            if (profile != null && profile.has("balance") && !profile.get("balance").isJsonNull()) {
                BigDecimal balance = profile.get("balance").getAsBigDecimal();
                session.setBalance(balance);
                balanceValueLabel.setText("Balance: " + formatMoney(balance));
            }
        });
        task.setOnFailed(e -> balanceValueLabel.setText("Balance: --"));

        Thread thread = new Thread(task);
        thread.setDaemon(true);
        thread.start();
    }

    public void refreshUserIdentityFromSession() {
        updateUserInfo();
        refreshUserBalance();
    }

    private void updateBalanceVisibility() {
        if (balanceValueLabel == null) {
            return;
        }
        boolean showBalance = !isAdminRole();
        balanceValueLabel.setManaged(showBalance);
        balanceValueLabel.setVisible(showBalance);
    }

    private boolean isAdminRole() {
        return userRole != null && userRole.startsWith("admin");
    }

    private boolean isBidderRole() {
        return "bidder".equalsIgnoreCase(userRole);
    }

    private void updateDepositVisibility() {
        if (depositBtn == null) {
            return;
        }
        boolean showDeposit = isBidderRole();
        depositBtn.setManaged(showDeposit);
        depositBtn.setVisible(showDeposit);
    }

    private String formatMoney(BigDecimal value) {
        if (value == null) {
            return "0 VND";
        }
        return MONEY_FORMAT.format(value) + " VND";
    }

    private String firstPresent(String first, String second, String fallback) {
        if (first != null && !first.isBlank()) {
            return first;
        }
        if (second != null && !second.isBlank()) {
            return second;
        }
        return fallback;
    }

    private void updateAvatarInitial(String name) {
        if (userAvatarText == null) {
            return;
        }
        userAvatarText.setText(initialsFromName(name, "US", 2));
    }

    private void updateAvatarRoleCode() {
        if (userAvatarRoleText == null) {
            return;
        }
        if (userRole != null && userRole.startsWith("admin")) {
            userAvatarRoleText.setText("ADM");
        } else if ("seller".equals(userRole)) {
            userAvatarRoleText.setText("SEL");
        } else if ("bidder".equals(userRole)) {
            userAvatarRoleText.setText("BID");
        } else {
            userAvatarRoleText.setText("USR");
        }
    }

    private String initialsFromName(String name, String fallback, int maxLength) {
        if (name == null || name.isBlank()) {
            return fallback;
        }
        String[] parts = name.trim().split("\\s+");
        StringBuilder initials = new StringBuilder();
        for (String part : parts) {
            if (!part.isBlank()) {
                initials.append(part.substring(0, 1).toUpperCase(Locale.ROOT));
            }
            if (initials.length() == maxLength) {
                break;
            }
        }
        return initials.length() == 0 ? fallback : initials.toString();
    }

    /** Navigate to an already-loaded page (used by child controllers). */
    public void navigateTo(Parent page, String title, String subtitle) {
        pageTitle.setText(title);
        pageSubtitle.setText(subtitle);
        contentArea.getChildren().clear();
        contentArea.getChildren().add(page);
    }

    @FXML private void onLogout() {
        try {
            com.team4.client.Client.getInstance().disconnect();
            com.team4.util.UserSession.clearSession();

            Parent root = FXMLLoader.load(getClass().getResource("/com/team4/view/login.fxml"));
            Stage stage = (Stage) mainRoot.getScene().getWindow();
            Scene scene = new Scene(root);
            scene.getStylesheets().add(getClass().getResource("/com/team4/view/style.css").toExternalForm());
            stage.setScene(scene);
            stage.setTitle("AuctionSpace - Login");
            StageUtils.maximize(stage);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    @FXML private void onNotificationClick() {
        if (notificationPopup != null && notificationPopup.isShowing()) {
            notificationPopup.hide();
            return;
        }
        refreshNotifications();
        showNotificationCenter();
    }

    public void pushNotification(String title, String message, String type) {
        String key = "runtime:" + type + ":" + title + ":" + message;
        if (runtimeNotificationKeys.add(key)) {
            notifications.add(0, new AppNotification(key, title, message, type, "", false));
            updateNotificationBadge();
        }
        showNotification(title + ": " + message, false);
    }

    public void pushNavigationNotification(String title, String message, String type, String targetPage) {
        String key = "runtime:" + type + ":" + title + ":" + message + ":" + targetPage;
        if (runtimeNotificationKeys.add(key)) {
            notifications.add(0, new AppNotification(key, title, message, type, targetPage, false));
            updateNotificationBadge();
        }
    }

    private void refreshNotifications() {
        UserSession session = UserSession.getInstance();
        if (session == null || session.getUserId() == null || session.getUserId().isBlank()) {
            notifications.clear();
            updateNotificationBadge();
            return;
        }

        javafx.concurrent.Task<List<AppNotification>> task = new javafx.concurrent.Task<>() {
            @Override
            protected List<AppNotification> call() throws Exception {
                ApiClient apiClient = new ApiClient();
                if (isAdminRole()) {
                    return buildAdminNotifications(apiClient, session.getUserId());
                }
                if ("seller".equalsIgnoreCase(userRole)) {
                    return buildSellerNotifications(apiClient, session.getUserId());
                }
                return buildBidderNotifications(apiClient, session.getUserId());
            }
        };

        task.setOnSucceeded(e -> {
            notifications.removeIf(AppNotification::generated);
            notifications.addAll(0, task.getValue());
            updateNotificationBadge();
            if (notificationPopup != null && notificationPopup.isShowing()) {
                notificationPopup.getContent().setAll(createNotificationPanel());
            }
        });

        Thread thread = new Thread(task);
        thread.setDaemon(true);
        thread.start();
    }

    private List<AppNotification> buildAdminNotifications(ApiClient apiClient, String userId) throws Exception {
        List<AppNotification> result = new ArrayList<>();

        JsonObject stats = apiClient.getDashboardStats(userId);
        long pending = longValue(stats, "pendingAuctions", 0);
        if (pending > 0) {
            result.add(new AppNotification(
                    "admin:pending:" + pending,
                    "Auctions pending review",
                    pending + " auction" + (pending == 1 ? " is" : "s are") + " waiting for approval.",
                    "review",
                    "admin_auctions",
                    true));
        }

        JsonArray auctions = apiClient.getAuctions("all", userId);
        int reportedCount = 0;
        int violatedCount = 0;
        for (JsonElement element : auctions) {
            if (!element.isJsonObject()) {
                continue;
            }
            JsonObject auction = element.getAsJsonObject();
            int reports = (int) longValue(auction, "reportCount", 0);
            String item = stringValue(auction, "itemName", "Auction item");
            String auctionId = stringValue(auction, "id", item);
            if (reports > 0) {
                result.add(new AppNotification(
                        "admin:reported:" + auctionId,
                        "Reported auction",
                        item + " has " + reports + " report" + (reports == 1 ? "." : "s."),
                        "warning",
                        "admin_auctions",
                        true));
                reportedCount++;
            }
            if (hasStatus(stringValue(auction, "status", ""), "CANCELLED", "REJECTED")) {
                result.add(new AppNotification(
                        "admin:violated:" + auctionId,
                        "Auction requires attention",
                        item + " is marked rejected or cancelled.",
                        "warning",
                        "admin_auctions",
                        true));
                violatedCount++;
            }
            if (reportedCount + violatedCount == 4) {
                break;
            }
        }

        return result;
    }

    private List<AppNotification> buildSellerNotifications(ApiClient apiClient, String sellerId) throws Exception {
        List<AppNotification> result = new ArrayList<>();
        List<Item> items = apiClient.getSellerItems(sellerId);

        for (Item item : items) {
            String status = item.getStatus();
            String name = item.getName() == null || item.getName().isBlank() ? "Your product" : item.getName();
            if (hasStatus(status, "RUNNING", "ACTIVE", "APPROVED", "ONGOING")) {
                result.add(new AppNotification(
                        "seller:running:" + item.getId(),
                        "Auction started",
                        name + " has been approved and is now live.",
                        "success",
                        "bidder_auctions",
                        true));
            } else if (hasStatus(status, "CANCELLED", "REJECTED")) {
                result.add(new AppNotification(
                        "seller:rejected:" + item.getId(),
                        "Product rejected",
                        name + " was not approved for auction.",
                        "warning",
                        "seller_products",
                        true));
            } else if (hasStatus(status, "FINISHED", "ENDED", "COMPLETED", "PAID", "SOLD")) {
                result.add(new AppNotification(
                        "seller:ended:" + item.getId(),
                        "Auction ended",
                        name + " has finished.",
                        "info",
                        "bidder_auctions",
                        true));
            }

            if (result.size() == 6) {
                break;
            }
        }

        return result;
    }

    private List<AppNotification> buildBidderNotifications(ApiClient apiClient, String bidderId) throws Exception {
        List<AppNotification> result = new ArrayList<>();
        JsonArray ownedItems = apiClient.getOwnedItems(bidderId);
        for (JsonElement element : ownedItems) {
            if (!element.isJsonObject()) {
                continue;
            }
            JsonObject item = element.getAsJsonObject();
            String name = stringValue(item, "name", "Auction item");
            String price = item.has("wonPrice") && !item.get("wonPrice").isJsonNull()
                    ? formatMoney(item.get("wonPrice").getAsBigDecimal())
                    : "final price";
            result.add(new AppNotification(
                    "bidder:won:" + stringValue(item, "auctionId", stringValue(item, "id", name)),
                    "Auction won",
                    "You won " + name + " at " + price + ".",
                    "success",
                    "bidder_owned_items",
                    true));
            if (result.size() == 3) {
                break;
            }
        }

        JsonArray bidHistory = apiClient.getBidHistoryByBidder(bidderId);
        Set<String> seenAuctions = new HashSet<>();
        for (int i = bidHistory.size() - 1; i >= 0 && seenAuctions.size() < 5; i--) {
            JsonElement element = bidHistory.get(i);
            if (!element.isJsonObject()) {
                continue;
            }
            String auctionId = stringValue(element.getAsJsonObject(), "auctionId", "");
            if (auctionId.isBlank() || !seenAuctions.add(auctionId)) {
                continue;
            }

            JsonObject auction = apiClient.getAuctionDetail(auctionId);
            String status = stringValue(auction, "status", "");
            String itemName = stringValue(auction, "itemName", "Auction item");
            String leaderId = stringValue(auction, "currentHighestBidderId", "");
            String price = auction.has("currentPrice") && !auction.get("currentPrice").isJsonNull()
                    ? formatMoney(auction.get("currentPrice").getAsBigDecimal())
                    : "current price";

            if (hasStatus(status, "RUNNING", "ACTIVE", "APPROVED", "ONGOING")) {
                if (bidderId.equals(leaderId)) {
                    result.add(new AppNotification(
                            "bidder:leading:" + auctionId,
                            "You are leading",
                            "Your bid is currently leading " + itemName + " at " + price + ".",
                            "success",
                            "bidder_auctions",
                            true));
                } else {
                    result.add(new AppNotification(
                            "bidder:outbid:" + auctionId,
                            "You have been outbid",
                            "Another bidder is now leading " + itemName + " at " + price + ".",
                            "warning",
                            "bidder_auctions",
                            true));
                }
            } else if (hasStatus(status, "FINISHED", "ENDED", "COMPLETED", "PAID", "SOLD", "CANCELLED")) {
                if (bidderId.equals(leaderId)) {
                    result.add(new AppNotification(
                            "bidder:closed-won:" + auctionId,
                            "Auction won",
                            "You won " + itemName + " at " + price + ".",
                            "success",
                            "bidder_owned_items",
                            true));
                } else {
                    result.add(new AppNotification(
                            "bidder:lost:" + auctionId,
                            "Auction ended",
                            "You did not win " + itemName + ".",
                            "info",
                            "bidder_auctions",
                            true));
                }
            }
        }

        return result;
    }

    private void showNotificationCenter() {
        notificationPopup = new Popup();
        notificationPopup.setAutoHide(true);
        notificationPopup.getContent().add(createNotificationPanel());
        notificationPopup.show(notiBtn, notiBtn.localToScreen(0, notiBtn.getHeight() + 8).getX() - 260,
                notiBtn.localToScreen(0, notiBtn.getHeight() + 8).getY());
    }

    private VBox createNotificationPanel() {
        VBox panel = new VBox(12);
        panel.getStyleClass().add("notification-panel");

        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);
        Label title = new Label("Notifications");
        title.getStyleClass().add("notification-panel-title");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        Button refresh = new Button("Refresh");
        refresh.getStyleClass().add("notification-refresh-btn");
        refresh.setOnAction(e -> refreshNotifications());
        header.getChildren().addAll(title, spacer, refresh);

        VBox list = new VBox(8);
        list.getStyleClass().add("notification-list");
        if (notifications.isEmpty()) {
            VBox empty = new VBox(4);
            empty.setAlignment(Pos.CENTER);
            empty.setPadding(new Insets(24, 12, 24, 12));
            Label emptyTitle = new Label("No notifications");
            emptyTitle.getStyleClass().add("notification-empty-title");
            Label emptyText = new Label("Relevant auction activity will appear here.");
            emptyText.getStyleClass().add("notification-empty-text");
            empty.getChildren().addAll(emptyTitle, emptyText);
            list.getChildren().add(empty);
        } else {
            for (AppNotification notification : notifications) {
                list.getChildren().add(createNotificationRow(notification));
            }
        }

        ScrollPane scroller = new ScrollPane(list);
        scroller.setFitToWidth(true);
        scroller.setPrefHeight(320);
        scroller.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroller.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scroller.getStyleClass().add("notification-scroll");

        panel.getChildren().addAll(header, scroller);
        return panel;
    }

    private HBox createNotificationRow(AppNotification notification) {
        HBox row = new HBox(10);
        row.setAlignment(Pos.CENTER_LEFT);
        row.getStyleClass().add("notification-row");
        if (notification.targetPage() != null && !notification.targetPage().isBlank()) {
            row.setOnMouseClicked(event -> {
                if (notificationPopup != null) {
                    notificationPopup.hide();
                }
                navigateByPageId(notification.targetPage());
            });
        }

        Label dot = new Label(notification.typeSymbol());
        dot.getStyleClass().addAll("notification-type", notification.typeStyle());

        VBox text = new VBox(3);
        text.setMinWidth(0);
        HBox.setHgrow(text, Priority.ALWAYS);
        Label title = new Label(notification.title());
        title.getStyleClass().add("notification-title");
        title.setMaxWidth(Double.MAX_VALUE);
        title.setTextOverrun(OverrunStyle.ELLIPSIS);
        Label message = new Label(notification.message());
        message.getStyleClass().add("notification-message");
        message.setWrapText(true);
        message.setMaxWidth(310);
        text.getChildren().addAll(title, message);

        row.getChildren().addAll(dot, text);
        return row;
    }

    private void updateNotificationBadge() {
        if (notiBadge == null) {
            return;
        }
        int count = notifications.size();
        notiBadge.setText(count > 99 ? "99+" : String.valueOf(count));
        notiBadge.setVisible(count > 0);
        notiBadge.setManaged(count > 0);
    }

    @FXML private void onDeposit() {
        UserSession session = UserSession.getInstance();
        if (!isBidderRole() || session == null || session.getUserId() == null) {
            showNotification("Only bidders can add funds.", true);
            return;
        }

        showDepositDialog().ifPresent(amount -> {
            depositBtn.setDisable(true);
            javafx.concurrent.Task<BigDecimal> task = new javafx.concurrent.Task<>() {
                @Override
                protected BigDecimal call() throws Exception {
                    String response = new ApiClient().depositWallet(session.getUserId(), amount);
                    JsonObject root = JsonParser.parseString(response).getAsJsonObject();
                    if (root.has("data") && root.get("data").isJsonObject()) {
                        JsonObject data = root.getAsJsonObject("data");
                        if (data.has("balance") && !data.get("balance").isJsonNull()) {
                            return data.get("balance").getAsBigDecimal();
                        }
                    }
                    return amount.add(session.getBalance() != null ? session.getBalance() : BigDecimal.ZERO);
                }
            };

            task.setOnSucceeded(e -> {
                BigDecimal newBalance = task.getValue();
                session.setBalance(newBalance);
                balanceValueLabel.setText("Balance: " + formatMoney(newBalance));
                depositBtn.setDisable(false);
                pushNotification(
                        "Deposit completed",
                        formatMoney(amount) + " has been added to your balance.",
                        "success");
            });

            task.setOnFailed(e -> {
                depositBtn.setDisable(false);
                showNotification(cleanMessage(task.getException()), true);
            });

            Thread thread = new Thread(task);
            thread.setDaemon(true);
            thread.start();
        });
    }

    private java.util.Optional<BigDecimal> showDepositDialog() {
        Dialog<BigDecimal> dialog = new Dialog<>();
        dialog.setTitle("Deposit Funds");
        dialog.setHeaderText(null);

        DialogPane pane = dialog.getDialogPane();
        pane.getStylesheets().add(getClass().getResource("/com/team4/view/style.css").toExternalForm());
        pane.getStyleClass().add("deposit-dialog-pane");

        ButtonType depositType = new ButtonType("Deposit", ButtonBar.ButtonData.OK_DONE);
        pane.getButtonTypes().addAll(depositType, ButtonType.CANCEL);

        Label title = new Label("Deposit Funds");
        title.getStyleClass().add("dialog-title");
        Label help = new Label("Enter the amount you want to add to your bidder balance.");
        help.setWrapText(true);
        help.getStyleClass().add("dialog-help");

        TextField amountField = new TextField();
        amountField.setPromptText("Amount (VND)");
        amountField.getStyleClass().add("input-field");

        Label error = new Label();
        error.getStyleClass().add("error-message");
        error.setManaged(false);
        error.setVisible(false);

        VBox content = new VBox(12, title, help, amountField, error);
        content.getStyleClass().add("deposit-dialog-content");
        pane.setContent(content);

        Button depositButton = (Button) pane.lookupButton(depositType);
        depositButton.getStyleClass().add("deposit-action-btn");
        Button cancelButton = (Button) pane.lookupButton(ButtonType.CANCEL);
        cancelButton.getStyleClass().add("dialog-secondary-btn");
        depositButton.addEventFilter(ActionEvent.ACTION, event -> {
            try {
                parseDepositAmount(amountField.getText());
            } catch (IllegalArgumentException ex) {
                error.setText(ex.getMessage());
                error.setManaged(true);
                error.setVisible(true);
                event.consume();
            }
        });

        dialog.setResultConverter(button -> button == depositType
                ? parseDepositAmount(amountField.getText())
                : null);
        return dialog.showAndWait();
    }

    private BigDecimal parseDepositAmount(String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            throw new IllegalArgumentException("Amount is required.");
        }
        try {
            BigDecimal amount = new BigDecimal(raw.trim().replace(",", ""));
            if (amount.compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("Amount must be greater than 0.");
            }
            if (amount.remainder(BigDecimal.valueOf(1000)).compareTo(BigDecimal.ZERO) != 0) {
                throw new IllegalArgumentException("Amount must be a multiple of 1,000 VND.");
            }
            return amount.setScale(2, RoundingMode.HALF_UP);
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("Enter a valid amount.");
        }
    }

    private long longValue(JsonObject obj, String key, long fallback) {
        return obj != null && obj.has(key) && !obj.get(key).isJsonNull()
                ? obj.get(key).getAsLong()
                : fallback;
    }

    private String stringValue(JsonObject obj, String key, String fallback) {
        return obj != null && obj.has(key) && !obj.get(key).isJsonNull()
                ? obj.get(key).getAsString()
                : fallback;
    }

    private boolean hasStatus(String value, String... candidates) {
        if (value == null || value.isBlank()) {
            return false;
        }
        String normalized = value.trim().replace(' ', '_').toUpperCase(Locale.ROOT);
        for (String candidate : candidates) {
            if (candidate.equals(normalized)) {
                return true;
            }
        }
        return false;
    }

    private void showNotification(String message, boolean error) {
        if (contentArea == null) {
            return;
        }
        Label toast = new Label(message);
        toast.getStyleClass().add(error ? "toast-notification-error" : "toast-notification");
        toast.setMaxWidth(360);
        toast.setWrapText(true);
        StackPane.setAlignment(toast, Pos.TOP_RIGHT);
        contentArea.getChildren().add(toast);

        PauseTransition delay = new PauseTransition(Duration.seconds(2.8));
        delay.setOnFinished(event -> contentArea.getChildren().remove(toast));
        delay.play();
    }

    private String cleanMessage(Throwable throwable) {
        String raw = throwable == null ? "" : throwable.getMessage();
        if (raw == null || raw.trim().isEmpty()) {
            return "Please try again.";
        }
        try {
            JsonObject parsed = JsonParser.parseString(raw).getAsJsonObject();
            if (parsed.has("message") && !parsed.get("message").isJsonNull()) {
                return parsed.get("message").getAsString();
            }
        } catch (Exception ignored) {
        }
        return raw.trim();
    }

    private record AppNotification(
            String key,
            String title,
            String message,
            String type,
            String targetPage,
            boolean generated
    ) {
        String typeSymbol() {
            return switch (type == null ? "" : type) {
                case "success" -> "OK";
                case "warning" -> "!";
                case "review" -> "R";
                case "bid" -> "B";
                default -> "i";
            };
        }

        String typeStyle() {
            return switch (type == null ? "" : type) {
                case "success" -> "notification-type-success";
                case "warning" -> "notification-type-warning";
                case "review" -> "notification-type-review";
                case "bid" -> "notification-type-bid";
                default -> "notification-type-info";
            };
        }
    }
}
