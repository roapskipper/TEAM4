package com.team4.controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.scene.Scene;
import java.net.URL;
import java.util.ResourceBundle;

public class MainController implements Initializable {

    @FXML private BorderPane mainRoot;
    @FXML private VBox navContainer;
    @FXML private StackPane contentArea;
    @FXML private Label pageTitle, pageSubtitle;
    @FXML private Label userNameLabel, userRoleBadge;
    @FXML private StackPane userAvatarBg;
    @FXML private Button notiBtn;
    @FXML private Label notiBadge;

    private String userRole = "bidder";
    private String currentPage = "";

    @Override
    public void initialize(URL url, ResourceBundle rb) {
    }

    public void setUserRole(String role) {
        this.userRole = role;
        setupSidebar();
        updateUserInfo();

        if (role != null && role.startsWith("admin")) {
            loadPage("admin_dashboard", "Dashboard", "Monitor and manage the entire system");
        } else if ("seller".equals(role)) {
            loadPage("seller_products", "Products", "Manage auction products");
        } else {
            loadPage("bidder_auctions", "Auctions", "Explore and join auctions");
        }
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

        if (com.team4.util.UserSession.getInstance() != null && com.team4.util.UserSession.getInstance().getUsername() != null) {
            name = com.team4.util.UserSession.getInstance().getUsername();
        }

        userNameLabel.setText(name);
        userRoleBadge.setText(roleText);
    }

    private void loadPage(String pageId, String title, String subtitle) {
        this.currentPage = pageId;
        pageTitle.setText(title);
        pageSubtitle.setText(subtitle);

        try {
            Parent page = FXMLLoader.load(getClass().getResource("/com/team4/view/" + pageId + ".fxml"));
            contentArea.getChildren().clear();
            contentArea.getChildren().add(page);
        } catch (Exception ex) {
            Label placeholder = new Label("Page " + title + " (under development)");
            placeholder.getStyleClass().add("muted-text");
            contentArea.getChildren().clear();
            contentArea.getChildren().add(placeholder);
        }
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
            stage.setMaximized(false);
            stage.setWidth(1200);
            stage.setHeight(800);
            stage.centerOnScreen();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    @FXML private void onNotificationClick() {
        System.out.println("Notifications clicked");
    }
}
