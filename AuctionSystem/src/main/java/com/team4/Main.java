package com.team4;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.text.Font;
import javafx.stage.Stage;

public class Main extends Application {

    @Override
    public void init() {
        String[] fontFiles = {
                "PlayfairDisplay-VariableFont_wght.ttf",
                "PlayfairDisplay-Italic-VariableFont_wght.ttf",
                "Lato-Black.ttf",
                "Lato-BlackItalic.ttf",
                "Lato-Bold.ttf",
                "Lato-BoldItalic.ttf",
                "Lato-Italic.ttf",
                "Lato-Light.ttf",
                "Lato-LightItalic.ttf",
                "Lato-Regular.ttf",
                "Lato-Thin.ttf",
                "Lato-ThinItalic.ttf"
        };
        for (String file : fontFiles) {
            var stream = getClass().getResourceAsStream("/fonts/" + file);
            if (stream != null) {
                Font.loadFont(stream, 14);
            } else {
                System.err.println("[Main] Font not found: " + file);
            }
        }
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
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