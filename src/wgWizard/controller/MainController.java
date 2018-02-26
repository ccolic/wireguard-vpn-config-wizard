/*
 * This software is GPL2 licensed, find further license information
 * in the LICENSE file located in the root directory
 *
 * Created on : 22-12-17
 * Authors    : Christian Colic, Marc Werenfels
 *
 */

package wgWizard.controller;

import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.MenuItem;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;
import wgWizard.Main;
import wgWizard.helper.Log;
import wgWizard.helper.PopUp;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;
import java.util.logging.Logger;

import static javafx.application.Application.STYLESHEET_MODENA;
import static javafx.application.Application.setUserAgentStylesheet;

public class MainController implements Initializable {

    @FXML
    private Button main_btn_s2s, main_btn_c2s, main_btn_c2c;

    @FXML
    private MenuItem menu_close, menu_about;

    @FXML
    private AnchorPane main_anchorPane;

    @FXML
    private MenuItem menu_guide;

    private Logger logger;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        logger = Log.getInstance();
        logger.info("Initializing MainController");
        this.menu_about.setOnAction(MainController::handleAbout);
        this.menu_close.setOnAction(MainController::handleClose);
        this.menu_guide.setOnAction(this::handleMainMenuGuide);
        this.main_btn_s2s.setOnAction(this::handleButton);
        this.main_btn_c2s.setOnAction(this::handleButton);
        this.main_btn_c2c.setOnAction(this::handleButton);
    }

    /**
     * Handle clicking any of the three main buttons on the screen
     * @param clicked the click-event
     */
    public void handleButton(Event clicked) {
        String choice = ((Button) clicked.getSource()).getId();
        // load the appropriate view
        switch (choice) {
            case "main_btn_s2s":
                loadNewScene("S2S.fxml", getClass(), main_anchorPane);
                break;
            case "main_btn_c2s":
                loadNewScene("C2S.fxml", getClass(), main_anchorPane);
                break;
            case "main_btn_c2c":
                loadNewScene("C2C.fxml", getClass(), main_anchorPane);
                break;
        }
    }

    /**
     * Handle clicks of back button. Change back to main menu
     */
    public static void handleBack(Event event, Class thisClass, AnchorPane anchorPane) {
        loadNewScene("Main.fxml", thisClass, anchorPane);
        Log.getInstance().info("Back button was clicked. Loading MainView");
    }

    /**
     * Handle clicking of the "Guide" button. Show an info dialog
     */
    public void handleMainMenuGuide(Event clicked){
        PopUp.showAlert(Alert.AlertType.INFORMATION, "User Guide", "Wireguard VPN configuration wizard - User Guide",
                "Please choose what kind of VPN configuration you want to setup.");
    }

    /**
     * Handle clicking of close button. Exit the program
     */
    public static void handleClose(Event event) {
        Log.getInstance().info("User pressed Exit. Exiting program with code 0");
        System.exit(0);
    }

    /**
     * Handle clicking of the about button. Show dialog with information
     */
    public static void handleAbout(Event event) {
        Log.getInstance().info("User pressed About. Showing About Dialog");
        String content =
                "Authors: Christian Colic, Marc Werenfels\n" +
                "License: GNU General Public License, Version 2\n" +
                "Copyright: 2017 (C) SK-R&D LTD LIAB. CO\n" +
                "Version: 1.0";
        PopUp.showAlert(Alert.AlertType.INFORMATION, "About", "About", content);
    }

    /**
     * Handle switching scenes. load new view.
     */
    public static void loadNewScene(String view, Class thisClass, AnchorPane anchorPane) {
        FXMLLoader loader = new FXMLLoader(thisClass.getResource("/wgWizard/res/view/" + view));
        setUserAgentStylesheet(STYLESHEET_MODENA);

        try {
            // Create scene with specific width and height
            Scene scene = new Scene(loader.load(), Main.WINDOW_WIDTH, Main.WINDOW_HEIGHT);

            // Set the stylesheet for the scene
            scene.getStylesheets().add(thisClass.getResource("/wgWizard/res/view/style.css").toExternalForm());

            Stage stage = (Stage) anchorPane.getScene().getWindow();
            stage.setScene(scene);
            stage.setResizable(true);
            stage.show();

        } catch (IOException e) {
            PopUp.showAlert(Alert.AlertType.ERROR, "Error", "Error loading the view file",
                    String.format("Something went wrong. Could not load '%s'. Please try again.", view));
        }
    }
}
