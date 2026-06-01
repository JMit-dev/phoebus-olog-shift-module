package org.phoebus.app.shift.ui;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import org.phoebus.shift.client.ShiftClient;
import org.phoebus.shift.client.ShiftClientException;
import org.phoebus.shift.client.model.Shift;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Executors;

public class ShiftTableController {

    @FXML private TableView<Shift> shiftTable;
    @FXML private TableColumn<Shift, Integer> colId;
    @FXML private TableColumn<Shift, String>  colType;
    @FXML private TableColumn<Shift, String>  colStatus;
    @FXML private TableColumn<Shift, String>  colOwner;
    @FXML private TableColumn<Shift, Date>    colStart;
    @FXML private Button startShiftBtn;
    @FXML private Button endShiftBtn;
    @FXML private Label  statusLabel;

    private ShiftClient shiftClient;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm");

    @FXML
    public void initialize() {
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colOwner.setCellValueFactory(new PropertyValueFactory<>("owner"));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));

        colType.setCellValueFactory(data -> {
            Shift s = data.getValue();
            String name = (s.getType() != null) ? s.getType().getName() : "";
            return new javafx.beans.property.SimpleStringProperty(name);
        });

        colStart.setCellValueFactory(new PropertyValueFactory<>("startDate"));
        colStart.setCellFactory(col -> new javafx.scene.control.TableCell<>() {
            @Override
            protected void updateItem(Date date, boolean empty) {
                super.updateItem(date, empty);
                setText((!empty && date != null) ? dateFormat.format(date) : null);
            }
        });

        shiftTable.getSelectionModel().selectedItemProperty().addListener(
                (obs, old, selected) -> endShiftBtn.setDisable(
                        selected == null || !"Active".equalsIgnoreCase(selected.getStatus())));

        shiftClient = ShiftClient.builder()
                .baseUrl(System.getProperty("shift.url", "http://localhost:8080/Shift/resources"))
                .username(System.getProperty("shift.username", ""))
                .password(System.getProperty("shift.password", ""))
                .build();
    }

    @FXML
    private void handleRefresh() {
        setStatus("Loading…");
        Executors.newSingleThreadExecutor().submit(() -> {
            try {
                List<Shift> shifts = shiftClient.listShifts();
                Platform.runLater(() -> {
                    shiftTable.setItems(FXCollections.observableArrayList(shifts));
                    setStatus("Loaded " + shifts.size() + " shift(s)");
                });
            } catch (ShiftClientException e) {
                Platform.runLater(() -> setStatus("Error: " + e.getMessage()));
            }
        });
    }

    @FXML
    private void handleStartShift() {
        new StartShiftDialog(shiftClient).showAndWait().ifPresent(result -> {
            setStatus("Shift started: " + result);
            handleRefresh();
        });
    }

    @FXML
    private void handleEndShift() {
        Shift selected = shiftTable.getSelectionModel().getSelectedItem();
        if (selected == null) return;

        new EndShiftDialog(selected).showAndWait().ifPresent(confirmed -> {
            if (confirmed) {
                setStatus("Shift ended: id=" + selected.getId());
                handleRefresh();
            }
        });
    }

    private void setStatus(String message) {
        statusLabel.setText(message);
    }
}
