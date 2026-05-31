package com.team4.controller;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.util.StringConverter;
import javafx.scene.control.Label;
import javafx.scene.control.Button;
import javafx.scene.control.Alert;
import javafx.concurrent.Task;
import javafx.animation.Timeline;
import javafx.animation.KeyFrame;
import javafx.util.Duration;

import com.team4.client.ApiClient;
import com.google.gson.JsonObject;
import com.google.gson.JsonElement;
import com.google.gson.JsonArray;

import com.team4.util.UserSession;

import java.net.URL;
import java.util.ResourceBundle;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.function.Consumer;

public class AdminDashboardController implements Initializable {

    @FXML private Label totalUsersLabel, totalAuctionsLabel, activeAuctionsLabel, pendingAuctionsLabel;
    @FXML private Label pendingReviewCount, liveAuctionCount;
    @FXML private BarChart<String, Number> regChart;
    @FXML private Button refreshButton;

    private Timeline autoRefresh;
    private MainController mainController;

    public void setMainController(MainController mainController) {
        this.mainController = mainController;
    }

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        regChart.setAnimated(true);
        
        // Cấu hình trục Y chỉ hiển thị các mốc số nguyên (bước nhảy = 1, tránh nhảy chẵn hoặc hiển thị số thập phân)
        if (regChart.getYAxis() instanceof NumberAxis) {
            NumberAxis yAxis = (NumberAxis) regChart.getYAxis();
            yAxis.setForceZeroInRange(true);
            yAxis.setTickUnit(1.0);
            yAxis.setMinorTickVisible(false);
            yAxis.setTickLabelFormatter(new StringConverter<Number>() {
                @Override
                public String toString(Number object) {
                    if (object.doubleValue() == object.intValue()) {
                        return String.valueOf(object.intValue());
                    }
                    return "";
                }

                @Override
                public Number fromString(String string) {
                    return Double.valueOf(string);
                }
            });
        }

        loadData();
        
        autoRefresh = new Timeline(new KeyFrame(Duration.seconds(30), e -> loadData()));
        autoRefresh.setCycleCount(Timeline.INDEFINITE);
        autoRefresh.play();

        // Dừng timeline khi view bị gỡ khỏi Scene (tránh trùng lặp tài nguyên và rò rỉ bộ nhớ)
        regChart.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene == null && autoRefresh != null) {
                autoRefresh.stop();
            }
        });
    }

    @FXML
    private void onRefresh() {
        loadData();
    }

    @FXML
    private void onViewPendingAuctions() {
        navigateTo("admin_auctions", controller -> {
            try {
                java.lang.reflect.Method method = controller.getClass().getDeclaredMethod("setFilter", String.class);
                method.setAccessible(true);
                method.invoke(controller, "pending");
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }



    @FXML
    private void onManageUsers() {
        navigateTo("admin_users", null);
    }

    private void navigateTo(String pageId, Consumer<Object> controllerAction) {
        if (mainController != null) {
            Object controller = mainController.navigateByPageId(pageId);
            if (controllerAction != null && controller != null) {
                controllerAction.accept(controller);
            }
        } else {
            try {
                javafx.scene.layout.StackPane contentArea = (javafx.scene.layout.StackPane) refreshButton.getScene().getRoot().lookup("#contentArea");
                if (contentArea != null) {
                    javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource("/com/team4/view/" + pageId + ".fxml"));
                    javafx.scene.Parent page = loader.load();
                    if (controllerAction != null) {
                        controllerAction.accept(loader.getController());
                    }
                    contentArea.getChildren().clear();
                    contentArea.getChildren().add(page);
                }
            } catch (Exception ex) {
                ex.printStackTrace();
                showError("Failed to navigate to " + pageId);
            }
        }
    }

    private void loadData() {
        setLoadingState();
        Task<JsonObject> task = new Task<>() {
            @Override
            protected JsonObject call() throws Exception {
                ApiClient apiClient = new ApiClient();
                return apiClient.getDashboardStats(currentUserId());
            }
        };

        task.setOnSucceeded(e -> {
            JsonObject data = task.getValue();
            updateStats(data);
            updateChart(data);
            updateReviewCenter(data);
            refreshButton.setDisable(false);
        });

        task.setOnFailed(e -> {
            refreshButton.setDisable(false);
            setDefaultState();
        });

        new Thread(task).start();
    }

    private void setLoadingState() {
        refreshButton.setDisable(true);
        totalUsersLabel.setText("...");
        totalAuctionsLabel.setText("...");
        activeAuctionsLabel.setText("...");
        pendingAuctionsLabel.setText("...");
        setText(pendingReviewCount, "...");
        setText(liveAuctionCount, "...");
    }

    private void setDefaultState() {
        totalUsersLabel.setText("0");
        totalAuctionsLabel.setText("0");
        activeAuctionsLabel.setText("0");
        pendingAuctionsLabel.setText("0");
        setText(pendingReviewCount, "0");
        setText(liveAuctionCount, "0");
    }

    private void updateStats(JsonObject data) {
        NumberFormat numberFormat = NumberFormat.getInstance(Locale.US);
        
        long totalUsers = data.has("totalUsers") ? data.get("totalUsers").getAsLong() : 0;
        long totalAuctions = data.has("totalAuctions") ? data.get("totalAuctions").getAsLong() : 0;
        long activeAuctions = data.has("activeAuctions") ? data.get("activeAuctions").getAsLong() : 0;
        long pendingAuctions = data.has("pendingAuctions") ? data.get("pendingAuctions").getAsLong() : 0;

        totalUsersLabel.setText(numberFormat.format(totalUsers));
        totalAuctionsLabel.setText(numberFormat.format(totalAuctions));
        activeAuctionsLabel.setText(numberFormat.format(activeAuctions));
        pendingAuctionsLabel.setText(numberFormat.format(pendingAuctions));
    }

    private void updateChart(JsonObject data) {
        // 1. Tải dữ liệu mới trước
        java.util.List<javafx.scene.chart.XYChart.Data<String, Number>> newPoints = new java.util.ArrayList<>();
        if (data.has("registrationChart") && data.get("registrationChart").isJsonArray()) {
            JsonArray chartData = data.getAsJsonArray("registrationChart");
            for (JsonElement el : chartData) {
                JsonObject point = el.getAsJsonObject();
                String month = point.has("month") ? point.get("month").getAsString() : "";
                int count = point.has("count") ? point.get("count").getAsInt() : 0;
                newPoints.add(new XYChart.Data<>(month, count));
            }
        } else {
            DateTimeFormatter monthFormatter = DateTimeFormatter.ofPattern("MMM", Locale.US);
            LocalDate month = LocalDate.now().minusMonths(5).withDayOfMonth(1);
            for (int i = 0; i < 6; i++) {
                newPoints.add(new XYChart.Data<>(month.plusMonths(i).format(monthFormatter), 0));
            }
        }

        // 2. Khóa cứng các nhãn trục X bằng cách thiết lập categories một cách tường minh (ngăn lỗi trùng lặp/sai thứ tự của JavaFX)
        if (regChart.getXAxis() instanceof CategoryAxis) {
            CategoryAxis xAxis = (CategoryAxis) regChart.getXAxis();
            java.util.List<String> months = new java.util.ArrayList<>();
            for (XYChart.Data<String, Number> pt : newPoints) {
                months.add(pt.getXValue());
            }
            xAxis.setCategories(javafx.collections.FXCollections.observableArrayList(months));
        }

        // 3. Cấu hình giới hạn trục Y thủ công dựa trên dữ liệu thực tế để tránh lỗi auto-ranging bị kẹt ở [0, 10] do hiệu ứng hoạt họa của JavaFX
        if (regChart.getYAxis() instanceof NumberAxis) {
            NumberAxis yAxis = (NumberAxis) regChart.getYAxis();
            yAxis.setAutoRanging(false);
            yAxis.setLowerBound(0);
            
            double maxVal = 0;
            for (XYChart.Data<String, Number> pt : newPoints) {
                double val = pt.getYValue().doubleValue();
                if (val > maxVal) {
                    maxVal = val;
                }
            }

            double tickUnit = 1.0;
            if (maxVal <= 10) {
                tickUnit = 1.0;
            } else if (maxVal <= 20) {
                tickUnit = 2.0;
            } else if (maxVal <= 50) {
                tickUnit = 5.0;
            } else if (maxVal <= 100) {
                tickUnit = 10.0;
            } else {
                tickUnit = Math.ceil(maxVal / 10.0);
            }

            double upperBound = Math.ceil(maxVal / tickUnit) * tickUnit;
            if (upperBound == 0) {
                upperBound = 10.0;
            } else if (upperBound == maxVal) {
                upperBound += tickUnit;
            }

            yAxis.setTickUnit(tickUnit);
            yAxis.setUpperBound(upperBound);
        }

        // 3. Tách biệt việc gán/cập nhật dữ liệu
        XYChart.Series<String, Number> series;
        if (regChart.getData().isEmpty()) {
            series = new XYChart.Series<>();
            series.setName("New Users");
            regChart.getData().add(series);
        } else {
            series = regChart.getData().get(0);
        }

        // Cập nhật giá trị mượt mà để giữ nguyên hiệu ứng hoạt họa và tránh lỗi lặp trục CategoryAxis
        if (series.getData().isEmpty()) {
            series.getData().addAll(newPoints);
        } else {
            // Cập nhật giá trị điểm cũ
            for (int i = 0; i < newPoints.size(); i++) {
                XYChart.Data<String, Number> newPt = newPoints.get(i);
                if (i < series.getData().size()) {
                    XYChart.Data<String, Number> existingPt = series.getData().get(i);
                    existingPt.setXValue(newPt.getXValue());
                    existingPt.setYValue(newPt.getYValue());
                } else {
                    series.getData().add(newPt);
                }
            }
            // Xóa phần thừa
            if (series.getData().size() > newPoints.size()) {
                series.getData().remove(newPoints.size(), series.getData().size());
            }
        }
    }

    @FXML
    private void onManageAuctions() {
        navigateTo("admin_auctions", null);
    }

    private void updateReviewCenter(JsonObject data) {
        NumberFormat numberFormat = NumberFormat.getInstance(Locale.US);
        long pending = data.has("pendingAuctions") ? data.get("pendingAuctions").getAsLong() : 0;
        long active = data.has("activeAuctions") ? data.get("activeAuctions").getAsLong() : 0;

        setText(pendingReviewCount, numberFormat.format(pending));
        setText(liveAuctionCount, numberFormat.format(active));
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR, message);
        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.show();
    }

    private String currentUserId() {
        UserSession session = UserSession.getInstance();
        return session != null && session.getUserId() != null ? session.getUserId() : "";
    }

    private void setText(Label label, String value) {
        if (label != null) {
            label.setText(value);
        }
    }
}
