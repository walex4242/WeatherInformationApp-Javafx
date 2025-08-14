package app.weather;

import app.weather.model.ForecastEntry;
import app.weather.model.WeatherData;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.SnapshotParameters;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.*;
import javafx.scene.text.Font;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WeatherApp extends Application {

    private final WeatherService weatherService = new WeatherService();
    private final HistoryManager historyManager = new HistoryManager();
    private final ComboBox<String> unitCombo = new ComboBox<>();
    private final TextField cityField = new TextField();
    private final Label statusLabel = new Label();
    private final Label tempLabel = new Label("--");
    private final Label feelsLabel = new Label("--");
    private final Label humidityLabel = new Label("--");
    private final Label windLabel = new Label("--");
    private final Label conditionLabel = new Label("--");
    private final ImageView iconView = new ImageView();
    private final ListView<String> historyList = new ListView<>();
    private final TableView<ForecastEntry> forecastTable = new TableView<>();
    private final BorderPane root = new BorderPane();

    private String currentCity = null;
    private WeatherData lastData = null;

    // Map OpenWeatherMap icon codes to local resource paths or fallback descriptions
    private static final Map<String, String> ICON_MAP = new HashMap<>();
    static {
        ICON_MAP.put("01d", "/app/weather/icons/sun.png"); // Clear sky (day)
        ICON_MAP.put("01n", "/app/weather/icons/moon.png"); // Clear sky (night)
        ICON_MAP.put("02d", "/app/weather/icons/partly_cloudy_day.png"); // Few clouds (day)
        ICON_MAP.put("02n", "/app/weather/icons/partly_cloudy_night.png"); // Few clouds (night)
        ICON_MAP.put("03d", "/app/weather/icons/cloud.png"); // Scattered clouds
        ICON_MAP.put("03n", "/app/weather/icons/cloud.png");
        ICON_MAP.put("04d", "/app/weather/icons/broken_clouds.png"); // Broken clouds
        ICON_MAP.put("04n", "/app/weather/icons/broken_clouds.png");
        ICON_MAP.put("09d", "/app/weather/icons/rain.png"); // Shower rain
        ICON_MAP.put("09n", "/app/weather/icons/rain.png");
        ICON_MAP.put("10d", "/app/weather/icons/rain.png"); // Rain (day)
        ICON_MAP.put("10n", "/app/weather/icons/rain.png"); // Rain (night)
        ICON_MAP.put("11d", "/app/weather/icons/thunderstorm.png"); // Thunderstorm
        ICON_MAP.put("11n", "/app/weather/icons/thunderstorm.png");
        ICON_MAP.put("13d", "/app/weather/icons/snow.png"); // Snow
        ICON_MAP.put("13n", "/app/weather/icons/snow.png");
        ICON_MAP.put("50d", "/app/weather/icons/mist.png"); // Mist
        ICON_MAP.put("50n", "/app/weather/icons/mist.png");
    }

    @Override
    public void start(Stage stage) {
        stage.setTitle("Weather Information App");

        // Top bar
        HBox topBar = new HBox(10);
        topBar.setPadding(new Insets(12));
        topBar.setAlignment(Pos.CENTER_LEFT);

        cityField.setPromptText("Enter city (e.g., London, Lagos, São Paulo)");
        Button searchBtn = new Button("Search");
        Button screenshotBtn = new Button("Save Screenshot");
        Button demoBtn = new Button("Demo Mode");

        unitCombo.setItems(FXCollections.observableArrayList("Metric (°C, km/h)", "Imperial (°F, mph)"));
        unitCombo.getSelectionModel().select(0);

        topBar.getChildren().addAll(new Label("City:"), cityField, searchBtn, unitCombo, screenshotBtn, demoBtn);

        // Center: current weather card
        VBox currentBox = new VBox(8);
        currentBox.setPadding(new Insets(16));
        currentBox.setAlignment(Pos.TOP_LEFT);
        currentBox.setMaxWidth(520);
        currentBox.setMinWidth(420);

        Label header = new Label("Current Weather");
        header.setFont(Font.font(20));

        HBox currRow = new HBox(16);
        currRow.setAlignment(Pos.CENTER_LEFT);

        iconView.setFitWidth(80);
        iconView.setFitHeight(80);
        VBox vals = new VBox(6);
        vals.getChildren().addAll(
                labeled("Temperature:", tempLabel),
                labeled("Feels Like:", feelsLabel),
                labeled("Humidity:", humidityLabel),
                labeled("Wind:", windLabel),
                labeled("Condition:", conditionLabel)
        );
        currRow.getChildren().addAll(iconView, vals);

        currentBox.getChildren().addAll(header, currRow);

        // Forecast table
        forecastTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        TableColumn<ForecastEntry, String> timeCol = new TableColumn<>("Time");
        timeCol.setCellValueFactory(cd -> {
            long epoch = cd.getValue().getEpoch();
            LocalDateTime dt = LocalDateTime.ofInstant(Instant.ofEpochSecond(epoch), ZoneId.systemDefault());
            return new SimpleStringProperty(dt.format(DateTimeFormatter.ofPattern("EEE, MMM d HH:mm")));
        });
        TableColumn<ForecastEntry, String> tempCol = new TableColumn<>("Temp");
        tempCol.setCellValueFactory(cd -> new SimpleStringProperty(String.format("%.1f°", cd.getValue().getTemp())));
        TableColumn<ForecastEntry, String> windCol = new TableColumn<>("Wind");
        windCol.setCellValueFactory(cd -> new SimpleStringProperty(String.format("%.1f", cd.getValue().getWind())));
        TableColumn<ForecastEntry, String> condCol = new TableColumn<>("Condition");
        condCol.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().getCondition()));

        forecastTable.getColumns().addAll(timeCol, tempCol, windCol, condCol);
        forecastTable.setPlaceholder(new Label("No forecast yet"));

        VBox centerBox = new VBox(16, currentBox, new Label("Short-term Forecast (next 24h)"), forecastTable);
        centerBox.setPadding(new Insets(16));

        // Right: history
        VBox rightBox = new VBox(10);
        rightBox.setPadding(new Insets(12));
        Label histLabel = new Label("Search History");
        historyList.setPrefWidth(300);
        historyList.setOnMouseClicked(e -> {
            String item = historyList.getSelectionModel().getSelectedItem();
            if (item != null) {
                String city = item.split(" @ ")[0];
                cityField.setText(city);
                fetchAndDisplay(city, false);
            }
        });
        Button clearHist = new Button("Clear History");
        clearHist.setOnAction(e -> {
            historyManager.clear();
            refreshHistory();
        });
        rightBox.getChildren().addAll(histLabel, historyList, clearHist);

        // Bottom status
        statusLabel.setPadding(new Insets(8));
        statusLabel.setText("Enter a city and press Search");

        root.setTop(topBar);
        root.setCenter(centerBox);
        root.setRight(rightBox);
        root.setBottom(statusLabel);

        // Actions
        searchBtn.setOnAction(e -> {
            String city = cityField.getText().trim();
            if (city.isEmpty()) {
                alert(Alert.AlertType.WARNING, "Validation Error", "Please enter a city name.");
                return;
            }
            fetchAndDisplay(city, true);
        });

        unitCombo.valueProperty().addListener((obs, oldV, newV) -> updateUnits());

        screenshotBtn.setOnAction(e -> saveScreenshot(stage));

        demoBtn.setOnAction(e -> loadDemo());

        // Initial UI background
        updateBackground(LocalDateTime.now().getHour());

        // Load history
        refreshHistory();

        Scene scene = new Scene(root, 1100, 700);
        stage.setScene(scene);
        stage.show();
    }

    private HBox labeled(String name, Label value) {
        Label label = new Label(name);
        label.setMinWidth(100);
        HBox row = new HBox(6, label, value);
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    private void fetchAndDisplay(String city, boolean addToHistory) {
        setStatus("Fetching weather for " + city + "...");
        currentCity = city;
        new Thread(() -> {
            try {
                boolean metric = unitCombo.getSelectionModel().getSelectedIndex() == 0;
                WeatherData data = weatherService.fetchWeather(city, metric);
                List<ForecastEntry> forecast = weatherService.fetchForecast(city, metric, 8); // next ~24h (3h * 8)
                Platform.runLater(() -> {
                    lastData = data;
                    applyWeather(data, forecast, metric);
                    if (addToHistory) {
                        historyManager.add(city);
                        refreshHistory();
                    }
                    setStatus("Updated " + city + " at " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm")));
                });
            } catch (Exception ex) {
                Platform.runLater(() -> {
                    alert(Alert.AlertType.ERROR, "API Error", ex.getMessage());
                    setStatus("Failed to fetch weather: " + ex.getMessage());
                });
            }
        }).start();
    }

    private void applyWeather(WeatherData data, List<ForecastEntry> forecast, boolean metric) {
        double t = data.getTemperature();
        double feels = data.getFeelsLike();
        double wind = data.getWind();
        String cond = data.getCondition();
        String icon = data.getIcon();

        tempLabel.setText(String.format("%.1f°", t));
        feelsLabel.setText(String.format("%.1f°", feels));
        humidityLabel.setText(data.getHumidity() + "%");
        windLabel.setText(String.format("%.1f %s", wind, metric ? "km/h" : "mph"));
        conditionLabel.setText(cond);

        System.out.println("Applying weather with icon: " + icon); // Debug output
        // Set weather icon with fallback
        if (icon != null && !icon.isEmpty()) {
            try {
                String url = "https://openweathermap.org/img/wn/" + icon + "@2x.png";
                System.out.println("Attempting to load URL: " + url); // Debug output
                Image image = new Image(url, true); // Background loading
                // Add a timeout to check if the image loads within a reasonable time
                new java.util.Timer().schedule(new java.util.TimerTask() {
                    @Override
                    public void run() {
                        if (!image.isError() && image.getProgress() < 1.0) { // If still loading
                            System.out.println("URL load timed out, falling back to local icon");
                            Platform.runLater(() -> setFallbackIcon(cond));
                        }
                    }
                }, 2000); // 2-second timeout
                // Check error property
                image.errorProperty().addListener((obs, oldVal, newVal) -> {
                    if (newVal) {
                        System.out.println("URL load failed, falling back to local icon"); // Debug output
                        Platform.runLater(() -> setFallbackIcon(cond));
                    } else if (image.isError()) {
                        System.out.println("Image error detected after load");
                        Platform.runLater(() -> setFallbackIcon(cond));
                    }
                });
                // Update ImageView when image is ready
                image.progressProperty().addListener((obs, oldVal, newVal) -> {
                    if (newVal.doubleValue() == 1.0 && !image.isError()) {
                        System.out.println("URL image loaded successfully, width: " + image.getWidth() + ", height: " + image.getHeight());
                        Platform.runLater(() -> {
                            iconView.setImage(image);
                            iconView.setPreserveRatio(false); // Ensure full fit
                            iconView.setSmooth(true); // Enable smooth scaling
                            iconView.setStyle("-fx-border-color: green; -fx-border-width: 2;"); // Visual debug border
                            System.out.println("ImageView updated, fitWidth: " + iconView.getFitWidth() + ", fitHeight: " + iconView.getFitHeight() +
                                    ", bounds: " + iconView.getBoundsInLocal());
                            // Force full UI refresh
                            root.requestLayout();
                            Scene scene = root.getScene();
                            if (scene != null) {
                                System.out.println("Requesting scene repaint");
                                scene.getWindow().sizeToScene(); // Resize to force redraw
                                // Aggressive refresh
                                scene.getRoot().setVisible(false);
                                scene.getRoot().setVisible(true); // Toggle visibility to force repaint
                            }
                            // Manual image check
                            if (iconView.getImage() == null) {
                                System.out.println("ImageView image is null after update");
                            } else {
                                System.out.println("ImageView image confirmed, width: " + iconView.getImage().getWidth() +
                                        ", height: " + iconView.getImage().getHeight());
                            }
                        });
                    }
                });
            } catch (Exception e) {
                System.out.println("Exception loading URL: " + e.getMessage()); // Debug output
                setFallbackIcon(cond);
            }
        } else {
            System.out.println("No icon provided, using fallback"); // Debug output
            setFallbackIcon(cond);
        }

        ObservableList<ForecastEntry> items = FXCollections.observableArrayList(forecast);
        forecastTable.setItems(items);

        updateBackground(LocalDateTime.now().getHour());
    }

    private void setFallbackIcon(String condition) {
        String iconPath = null;
        // Map condition to icon if specific icon code is unavailable
        if (condition != null) {
            String condLower = condition.toLowerCase();
            if (condLower.contains("clear")) {
                iconPath = ICON_MAP.get("01d"); // Use day clear icon as fallback
            } else if (condLower.contains("cloud")) {
                iconPath = ICON_MAP.get("03d");
            } else if (condLower.contains("rain") || condLower.contains("shower")) {
                iconPath = ICON_MAP.get("09d");
            } else if (condLower.contains("thunder")) {
                iconPath = ICON_MAP.get("11d");
            } else if (condLower.contains("snow")) {
                iconPath = ICON_MAP.get("13d");
            } else if (condLower.contains("mist") || condLower.contains("fog")) {
                iconPath = ICON_MAP.get("50d");
            }
        }

        if (iconPath != null) {
            try {
                System.out.println("Attempting to load local icon from: " + iconPath); // Debug output
                Image fallbackImage = new Image(getClass().getResourceAsStream(iconPath));
                if (fallbackImage.isError()) {
                    System.out.println("Local icon load failed for: " + iconPath); // Debug output
                    throw new Exception("Image load error");
                }
                iconView.setImage(fallbackImage);
            } catch (Exception e) {
                System.out.println("Exception loading local icon: " + e.getMessage()); // Debug output
                // Ultimate fallback: set placeholder text
                iconView.setImage(null);
                conditionLabel.setText(condition + " (No icon available)");
            }
        } else {
            System.out.println("No matching icon path for condition: " + condition); // Debug output
            iconView.setImage(null);
            conditionLabel.setText(condition + " (No icon available)");
        }
    }

    private void updateUnits() {
        if (lastData == null) return;
        boolean metric = unitCombo.getSelectionModel().getSelectedIndex() == 0;
        // Re-fetch in selected unit to keep wind conversion simple and forecast consistent
        if (currentCity != null) {
            fetchAndDisplay(currentCity, false);
        }
    }

    private void updateBackground(int hour) {
        // Morning/day/evening/night gradients
        Stop[] stops;
        if (hour >= 6 && hour < 12) { // morning
            stops = new Stop[]{ new Stop(0, Color.web("#89CFF0")), new Stop(1, Color.web("#E0FFFF")) };
        } else if (hour >= 12 && hour < 17) { // day
            stops = new Stop[]{ new Stop(0, Color.web("#87CEEB")), new Stop(1, Color.web("#FFFFFF")) };
        } else if (hour >= 17 && hour < 20) { // evening
            stops = new Stop[]{ new Stop(0, Color.web("#FF7E5F")), new Stop(1, Color.web("#FEB47B")) };
        } else { // night
            stops = new Stop[]{ new Stop(0, Color.web("#2C3E50")), new Stop(1, Color.web("#4CA1AF")) };
        }
        root.setBackground(new Background(new BackgroundFill(new LinearGradient(0,0,1,1,true, CycleMethod.NO_CYCLE, stops),
                CornerRadii.EMPTY, Insets.EMPTY)));
    }

    private void saveScreenshot(Stage stage) {
        try {
            FileChooser chooser = new FileChooser();
            chooser.setTitle("Save Screenshot");
            chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PNG Image", "*.png"));
            chooser.setInitialFileName("weather_screenshot.png");
            File file = chooser.showSaveDialog(stage);
            if (file == null) return;
            SnapshotParameters params = new SnapshotParameters();
            javafx.scene.image.WritableImage image = root.snapshot(params, null);
            javax.imageio.ImageIO.write(javafx.embed.swing.SwingFXUtils.fromFXImage(image, null), "png", file);
            alert(Alert.AlertType.INFORMATION, "Saved", "Screenshot saved to: " + file.getAbsolutePath());
        } catch (Exception ex) {
            alert(Alert.AlertType.ERROR, "Save Error", ex.getMessage());
        }
    }

    private void loadDemo() {
        WeatherData demo = WeatherData.demo();
        List<ForecastEntry> demoF = ForecastEntry.demoList();
        lastData = demo;
        applyWeather(demo, demoF, unitCombo.getSelectionModel().getSelectedIndex() == 0);
        setStatus("Demo data loaded — no API key required");
    }

    private void setStatus(String text) {
        statusLabel.setText(text);
    }

    private void alert(Alert.AlertType type, String title, String msg) {
        Alert a = new Alert(type);
        a.setTitle(title);
        a.setHeaderText(null);
        a.setContentText(msg);
        a.showAndWait();
    }

    private void refreshHistory() {
        List<String> items = historyManager.getAll();
        historyList.getItems().setAll(items);
    }

    public static void main(String[] args) {
        launch(args);
    }
}