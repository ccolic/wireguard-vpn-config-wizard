/*
 * This software is GPL2 licensed, find further license information
 * in the LICENSE file located in the root directory
 *
 * Created on : 22-12-17
 * Authors    : Christian Colic, Marc Werenfels
 *
 */

package wgWizard;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;


public class Main extends Application {
    public static final int WINDOW_WIDTH = 1200;
    public static final int WINDOW_HEIGHT = 600;

    public static void main(String[] args) {
        // set logging format
        System.setProperty("java.util.logging.SimpleFormatter.format",
                "%1$tY-%1$tm-%1$td %1$tH:%1$tM:%1$tS %4$-6s %2$s %5$s%6$s%n");
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        // Create FXMLLoader based on the GameView.fxml file
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/wgWizard/res/view/Main.fxml"));
        setUserAgentStylesheet(STYLESHEET_MODENA);

        // Create scene with specific width and height
        Scene scene = new Scene(loader.load(), WINDOW_WIDTH, WINDOW_HEIGHT);

        // Set the stylesheet for the scene
        scene.getStylesheets().add(getClass().getResource("/wgWizard/res/view/style.css").toExternalForm());

        // Set stage attributes and settings
        primaryStage.setTitle("Wireguard VPN - Config Wizard");
        primaryStage.setScene(scene);
        primaryStage.setResizable(true);

        primaryStage.show();
    }
}
