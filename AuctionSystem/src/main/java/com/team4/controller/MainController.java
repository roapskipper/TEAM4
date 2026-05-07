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

        if ("admin".equals(role)) loadPage("admin_dashboard", "Dashboard", "Monitor and manage the entire system");
        else if ("seller".equals(role)) loadPage("seller_products", "Products", "Manage auction products");
        else loadPage("bidder_auctions", "Auctions", "Explore and join auctions");
    }

    private void setupSidebar() {
        navContainer.getChildren().clear();

        if ("admin".equals(userRole)) {
            addNavItem("📊", "Dashboard", "admin_dashboard", "Monitor and manage the system");
            addNavItem("👥", "Users", "admin_users", "Lock/unlock accounts");
            addNavItem("🔨", "Auctions", "admin_auctions", "Approve and manage auctions");
            addNavItem("👤", "Profile", "profile", "Update your information");
        } else if ("seller".equals(userRole)) {
            addNavItem("📦", "Products", "seller_products", "Manage your products");
            addNavItem("📋", "My Auctions", "bidder_auctions", "Sessions you created");
            addNavItem("👤", "Profile", "profile", "Update your information");
        } else {
            addNavItem("🔨", "Auctions", "bidder_auctions", "Explore auction sessions");
            addNavItem("📋", "My Auctions", "bidding_room", "Sessions you joined");
            addNavItem("👤", "Profile", "profile", "Update your information");
        }
    }

    private void addNavItem(String icon, String label, String pageId, String subtitle) {
        Button btn = new Button(icon + "  " + label);
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
                b.getStyleClass().add("nav-btn");
            }
        }
        selected.getStyleClass().removeAll("nav-btn");
        selected.getStyleClass().add("nav-btn-active");
    }

    private void updateUserInfo() {
        String name = userRole.equals("admin") ? "Admin" :
                userRole.equals("seller") ? "Seller" : "Buyer";
        userNameLabel.setText(name);

        if ("admin".equals(userRole)) {
            userRoleBadge.setText("ADMIN");
            userRoleBadge.setStyle("-fx-text-fill: #ef4444; -fx-padding: 2 8; -fx-background-color: rgba(239,68,68,0.15); -fx-background-radius: 20;");
            userAvatarBg.setStyle("-fx-background-color: linear-gradient(to bottom right, #ef4444, #f97316); -fx-background-radius: 50%;");
        } else if ("seller".equals(userRole)) {
            userRoleBadge.setText("SELLER");
            userRoleBadge.setStyle("-fx-text-fill: #ec4899; -fx-padding: 2 8; -fx-background-color: rgba(236,72,153,0.15); -fx-background-radius: 20;");
            userAvatarBg.setStyle("-fx-background-color: linear-gradient(to bottom right, #ec4899, #f43f5e); -fx-background-radius: 50%;");
        } else {
            userRoleBadge.setText("BIDDER");
            userRoleBadge.setStyle("-fx-text-fill: #3b82f6; -fx-padding: 2 8; -fx-background-color: rgba(59,130,246,0.15); -fx-background-radius: 20;");
            userAvatarBg.setStyle("-fx-background-color: linear-gradient(to bottom right, #3b82f6, #06b6d4); -fx-background-radius: 50%;");
        }
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
            placeholder.setStyle("-fx-text-fill: #4b5563; -fx-font-size: 16;");
            contentArea.getChildren().clear();
            contentArea.getChildren().add(placeholder);
        }
    }

    @FXML private void onLogout() {
        try {
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