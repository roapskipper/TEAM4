package com.team4;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Main extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        // Load man hinh dang nhap
        FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/com/team4/view/login.fxml")
        );
        Parent root = loader.load();

        Scene scene = new Scene(root);
        scene.getStylesheets().add(
                getClass().getResource("/com/team4/view/style.css").toExternalForm()
        );

        primaryStage.setTitle("AuctionSpace - Dang nhap");
        primaryStage.setScene(scene);
        primaryStage.setMinWidth(1000);
        primaryStage.setMinHeight(700);
        primaryStage.centerOnScreen();
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
