package org.phoebus.app.shift;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * Standalone JavaFX entry point for the shift viewer.
 *
 * <p>Run via Maven:
 * <pre>
 *   mvn javafx:run -Dshift.url=http://localhost:8080/Shift/resources
 * </pre>
 *
 * <p>This is structured to be extracted as a Phoebus app module later. The
 * controller ({@link org.phoebus.app.shift.ui.ShiftTableController}) and dialogs
 * will become an {@code AppInstance} inside a Phoebus {@code DockItem}.
 */
public class ShiftApp extends Application {

    @Override
    public void start(Stage stage) throws Exception {
        FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/org/phoebus/app/shift/ui/ShiftTable.fxml"));
        Scene scene = new Scene(loader.load());
        stage.setTitle("Shift Viewer");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
