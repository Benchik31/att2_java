package ru.coursework.javasems.client;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MenuClientApp extends Application {

    private static final String API_URL = "http://localhost:8080/api/items";

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    private final ObservableList<MenuItemDto> items = FXCollections.observableArrayList();

    private TableView<MenuItemDto> tableView;
    private TextField dishField;
    private TextField categoryField;
    private TextField priceField;
    private Label statusLabel;

    @Override
    public void start(Stage stage) {
        tableView = createTable();

        dishField = new TextField();
        categoryField = new TextField();
        priceField = new TextField();

        GridPane form = createForm();
        HBox actions = createActions();

        statusLabel = new Label("Ready.");
        statusLabel.setStyle("-fx-text-fill: #6a5643;");

        VBox leftPanel = new VBox(14, form, actions, statusLabel);
        leftPanel.setPadding(new Insets(16));
        leftPanel.setPrefWidth(320);

        BorderPane root = new BorderPane();
        root.setLeft(leftPanel);
        root.setCenter(tableView);
        BorderPane.setMargin(tableView, new Insets(16));

        Scene scene = new Scene(root, 920, 560);
        stage.setTitle("Menu Studio Client");
        stage.setScene(scene);
        stage.show();

        loadItemsAsync();
    }

    @Override
    public void stop() {
        executor.shutdownNow();
    }

    private TableView<MenuItemDto> createTable() {
        TableView<MenuItemDto> table = new TableView<>(items);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        TableColumn<MenuItemDto, Long> idCol = new TableColumn<>("ID");
        idCol.setCellValueFactory(new PropertyValueFactory<>("id"));
        idCol.setMaxWidth(1f * Integer.MAX_VALUE * 10);

        TableColumn<MenuItemDto, String> dishCol = new TableColumn<>("Dish");
        dishCol.setCellValueFactory(new PropertyValueFactory<>("dishName"));
        dishCol.setMaxWidth(1f * Integer.MAX_VALUE * 30);

        TableColumn<MenuItemDto, String> categoryCol = new TableColumn<>("Category");
        categoryCol.setCellValueFactory(new PropertyValueFactory<>("category"));
        categoryCol.setMaxWidth(1f * Integer.MAX_VALUE * 30);

        TableColumn<MenuItemDto, BigDecimal> priceCol = new TableColumn<>("Price");
        priceCol.setCellValueFactory(new PropertyValueFactory<>("price"));
        priceCol.setMaxWidth(1f * Integer.MAX_VALUE * 20);

        table.getColumns().addAll(idCol, dishCol, categoryCol, priceCol);

        table.getSelectionModel().selectedItemProperty().addListener((obs, oldValue, newValue) -> {
            if (newValue != null) {
                dishField.setText(newValue.getDishName());
                categoryField.setText(newValue.getCategory());
                priceField.setText(newValue.getPrice() != null ? newValue.getPrice().toString() : "");
            }
        });

        return table;
    }

    private GridPane createForm() {
        GridPane form = new GridPane();
        form.setHgap(8);
        form.setVgap(10);
        form.setPadding(new Insets(0, 0, 8, 0));

        Label header = new Label("Menu item");
        header.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");

        Label dishLabel = new Label("Dish name");
        Label categoryLabel = new Label("Category");
        Label priceLabel = new Label("Price");

        dishField.setPromptText("Truffle risotto");
        categoryField.setPromptText("Main");
        priceField.setPromptText("12.50");

        form.add(header, 0, 0, 2, 1);
        form.add(dishLabel, 0, 1);
        form.add(dishField, 1, 1);
        form.add(categoryLabel, 0, 2);
        form.add(categoryField, 1, 2);
        form.add(priceLabel, 0, 3);
        form.add(priceField, 1, 3);

        return form;
    }

    private HBox createActions() {
        Button addButton = new Button("Add");
        Button updateButton = new Button("Update");
        Button deleteButton = new Button("Delete");
        Button refreshButton = new Button("Refresh");

        addButton.setOnAction(event -> addItem());
        updateButton.setOnAction(event -> updateItem());
        deleteButton.setOnAction(event -> deleteItem());
        refreshButton.setOnAction(event -> loadItemsAsync());

        HBox actions = new HBox(10, addButton, updateButton, deleteButton, refreshButton);
        actions.setAlignment(Pos.CENTER_LEFT);
        return actions;
    }

    private void addItem() {
        MenuItemDto payload = buildPayload();
        if (payload == null) {
            return;
        }
        runAsync(() -> {
            HttpRequest request = HttpRequest.newBuilder(URI.create(API_URL))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(payload)))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 201) {
                throw new IllegalStateException("Add failed: HTTP " + response.statusCode());
            }
            loadItemsInternal();
            clearFields();
            setStatus("Item added.", false);
        });
    }

    private void updateItem() {
        MenuItemDto selected = tableView.getSelectionModel().getSelectedItem();
        if (selected == null || selected.getId() == null) {
            setStatus("Select a row to update.", true);
            return;
        }
        MenuItemDto payload = buildPayload();
        if (payload == null) {
            return;
        }
        runAsync(() -> {
            HttpRequest request = HttpRequest.newBuilder(URI.create(API_URL + "/" + selected.getId()))
                    .header("Content-Type", "application/json")
                    .PUT(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(payload)))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                throw new IllegalStateException("Update failed: HTTP " + response.statusCode());
            }
            loadItemsInternal();
            setStatus("Item updated.", false);
        });
    }

    private void deleteItem() {
        MenuItemDto selected = tableView.getSelectionModel().getSelectedItem();
        if (selected == null || selected.getId() == null) {
            setStatus("Select a row to delete.", true);
            return;
        }
        runAsync(() -> {
            HttpRequest request = HttpRequest.newBuilder(URI.create(API_URL + "/" + selected.getId()))
                    .DELETE()
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 204) {
                throw new IllegalStateException("Delete failed: HTTP " + response.statusCode());
            }
            loadItemsInternal();
            clearFields();
            setStatus("Item deleted.", false);
        });
    }

    private void loadItemsAsync() {
        runAsync(this::loadItemsInternal);
    }

    private void loadItemsInternal() throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create(API_URL))
                .GET()
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new IllegalStateException("Load failed: HTTP " + response.statusCode());
        }
        List<MenuItemDto> responseItems = objectMapper.readValue(response.body(), new TypeReference<List<MenuItemDto>>() {});
        Platform.runLater(() -> {
            items.setAll(responseItems);
            setStatus("Loaded " + responseItems.size() + " items.", false);
        });
    }

    private MenuItemDto buildPayload() {
        String dish = dishField.getText() != null ? dishField.getText().trim() : "";
        String category = categoryField.getText() != null ? categoryField.getText().trim() : "";
        String priceText = priceField.getText() != null ? priceField.getText().trim() : "";

        if (dish.isEmpty() || category.isEmpty() || priceText.isEmpty()) {
            setStatus("Fill in dish, category, and price.", true);
            return null;
        }

        BigDecimal price;
        try {
            price = new BigDecimal(priceText);
        } catch (NumberFormatException ex) {
            setStatus("Price must be a number.", true);
            return null;
        }

        if (price.compareTo(BigDecimal.ZERO) <= 0) {
            setStatus("Price must be greater than 0.", true);
            return null;
        }

        MenuItemDto payload = new MenuItemDto();
        payload.setDishName(dish);
        payload.setCategory(category);
        payload.setPrice(price);
        return payload;
    }

    private void runAsync(ThrowingRunnable action) {
        executor.submit(() -> {
            try {
                action.run();
            } catch (Exception ex) {
                setStatus(ex.getMessage() != null ? ex.getMessage() : "Request failed.", true);
            }
        });
    }

    private void clearFields() {
        Platform.runLater(() -> {
            dishField.clear();
            categoryField.clear();
            priceField.clear();
            tableView.getSelectionModel().clearSelection();
        });
    }

    private void setStatus(String message, boolean error) {
        Platform.runLater(() -> {
            statusLabel.setText(message);
            statusLabel.setStyle(error
                    ? "-fx-text-fill: #b05f22; -fx-font-weight: bold;"
                    : "-fx-text-fill: #3e6f4a; -fx-font-weight: bold;");
        });
    }

    @FunctionalInterface
    private interface ThrowingRunnable {
        void run() throws Exception;
    }

    public static void main(String[] args) {
        launch(args);
    }
}
