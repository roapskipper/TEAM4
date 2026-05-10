
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
                // ===== LATO=====
                "Lato Regular.ttf",
                "Lato Bold.ttf",
                "Lato Italic.ttf",
                "Lato Light.ttf",
                "Lato Medium.ttf",
                "Lato Semibold.ttf",
                "Lato Black.ttf",
                "Lato Heavy.ttf",
                "Lato Hairline.ttf",
                "Lato Thin.ttf",

                // ===== PLAYFAIR DISPLAY=====
                "PlayfairDisplay-Regular.ttf",
                "PlayfairDisplay-Bold.ttf",
                "PlayfairDisplay-BoldItalic.ttf",
                "PlayfairDisplay-Italic.ttf",
                "PlayfairDisplay-Medium.ttf",
                "PlayfairDisplay-MediumItalic.ttf",
                "PlayfairDisplay-SemiBold.ttf",
                "PlayfairDisplay-SemiBoldItalic.ttf",
                "PlayfairDisplay-ExtraBold.ttf",
                "PlayfairDisplay-ExtraBoldItalic.ttf",
                "PlayfairDisplay-Black.ttf",
                "PlayfairDisplay-BlackItalic.ttf"
        };

        for (String file : fontFiles) {
            var stream = getClass().getResourceAsStream("/fonts/" + file);
            if (stream != null) {
                Font.loadFont(stream, 14);
                System.out.println("[Main] Loaded font: " + file);
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

