package com.team4;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Main extends Application {
    @Override
    public void start(Stage stage) throws Exception {
        FXMLLoader loader = new FXMLLoader();
        loader.setLocation(getClass().getResource("/com/team4/view/login.fxml"));
        Scene scene = new Scene(loader.load(), 1100, 700);
        stage.setTitle("AuctionSpace - Đăng nhập");
        stage.setScene(scene);
        stage.setMinWidth(900);
        stage.setMinHeight(650);
        stage.show();
    }
    public static void main(String[] args) {
        launch();
    }
}
