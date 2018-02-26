package wgWizard.helper;

import javafx.scene.control.Alert;
import javafx.scene.image.ImageView;

import java.util.Optional;

/**
 * Simple helper class for showing JavaFX Alert dialogs
 */
public class PopUp {

    /**
     * Show Alert dialog, optionally with an image
     * @param alertType Alert.AlertType
     * @param title The title as String
     * @param header The header as String
     * @param context The main body text as String
     * @param imageView optionally add an ImageView
     */
    public static void showAlert(Alert.AlertType alertType, String title, String header, String context, ImageView... imageView) {
        Alert alert = new Alert(alertType);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(context);
        if (imageView.length == 1) {
            alert.setGraphic(imageView[0]);
        }
        alert.showAndWait();
    }
}
