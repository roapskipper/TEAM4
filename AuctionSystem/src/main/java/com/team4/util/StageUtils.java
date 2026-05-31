package com.team4.util;

import javafx.application.Platform;
import javafx.geometry.Rectangle2D;
import javafx.stage.Screen;
import javafx.stage.Stage;

import java.util.List;

public final class StageUtils {
    private StageUtils() {
    }

    public static void showMaximized(Stage stage) {
        if (stage == null) {
            return;
        }
        stage.show();
        maximize(stage);
    }

    public static void maximize(Stage stage) {
        if (stage == null) {
            return;
        }

        stage.setResizable(true);
        stage.setIconified(false);
        stage.setMaximized(false);

        Platform.runLater(() -> {
            Rectangle2D bounds = visualBoundsFor(stage);
            stage.setX(bounds.getMinX());
            stage.setY(bounds.getMinY());
            stage.setWidth(bounds.getWidth());
            stage.setHeight(bounds.getHeight());
            stage.setMaximized(true);
        });
    }

    private static Rectangle2D visualBoundsFor(Stage stage) {
        double width = Math.max(stage.getWidth(), 1);
        double height = Math.max(stage.getHeight(), 1);
        List<Screen> screens = Screen.getScreensForRectangle(stage.getX(), stage.getY(), width, height);
        Screen screen = screens.isEmpty() ? Screen.getPrimary() : screens.get(0);
        return screen.getVisualBounds();
    }
}
