package org.phoebus.app.shift.ui;

import javafx.geometry.Insets;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import org.phoebus.shift.client.model.Shift;

import java.text.SimpleDateFormat;

/**
 * Confirmation dialog for ending the selected shift.
 * Returns true if the user confirmed, false/empty if cancelled.
 */
public class EndShiftDialog extends Dialog<Boolean> {

    public EndShiftDialog(Shift shift) {
        setTitle("End Shift");
        setHeaderText("End this shift?");

        getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        String typeName = (shift.getType() != null) ? shift.getType().getName() : "—";
        String startStr = (shift.getStartDate() != null)
                ? new SimpleDateFormat("yyyy-MM-dd HH:mm").format(shift.getStartDate())
                : "—";

        VBox content = new VBox(8,
                new Label("ID:     " + shift.getId()),
                new Label("Type:   " + typeName),
                new Label("Owner:  " + shift.getOwner()),
                new Label("Start:  " + startStr));
        content.setPadding(new Insets(16));

        getDialogPane().setContent(content);

        setResultConverter(button -> button == ButtonType.OK);
    }
}
