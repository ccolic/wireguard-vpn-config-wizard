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
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import net.lingala.zip4j.exception.ZipException;
import wgWizard.config.Configuration;
import wgWizard.config.Keypair;
import wgWizard.helper.Log;
import wgWizard.helper.PopUp;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.ResourceBundle;
import java.util.logging.Logger;

import static wgWizard.controller.S2SController.*;

public class C2CController implements Initializable {

    @FXML
    private MenuItem menu_close, menu_about;

    @FXML
    private TextField c1_publicIP, c1_publicPort, c1_tunnelIP, c1_tunnelMask, c1_sec, c1_tunnelInterfaceName;

    @FXML
    private TextField c2_publicIP, c2_publicPort, c2_tunnelIP, c2_tunnelMask, c2_sec, c2_tunnelInterfaceName;

    @FXML
    private Button btn_save, btn_back;

    @FXML
    private CheckBox c2_defaultGateway, c1_defaultGateway, c1_psk, c2_psk;

    @FXML
    private MenuItem menu_guide;

    @FXML
    private AnchorPane main_anchorPane;

    private Logger logger;

    private List<TextField> ipFields;
    private List<TextField> netmaskFields;
    private List<TextField> listenPortFields;
    private List<TextField> intNameFields;
    private List<TextField> keepaliveFields;
    private Configuration client1Config;
    private Configuration client2Config;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        logger = Log.getInstance();
        logger.info("Initializing C2CController");

        // add event handler to buttons
        this.btn_save.setOnAction(this::handleSave);
        this.menu_about.setOnAction(MainController::handleAbout);
        this.menu_close.setOnAction(MainController::handleClose);
        this.c1_psk.setOnAction(this::handlePsk);
        this.c2_psk.setOnAction(this::handlePsk);
        this.btn_back.setOnAction(event -> MainController.handleBack(event, getClass(), main_anchorPane));
        this.menu_guide.setOnAction(this::handleC2CGuide);

        // populate field-lists for easier access
        this.ipFields = Arrays.asList(c1_publicIP, c2_publicIP, c1_tunnelIP, c2_tunnelIP);
        this.netmaskFields = Arrays.asList(c1_tunnelMask, c2_tunnelMask);
        this.listenPortFields = Arrays.asList(c1_publicPort, c2_publicPort);
        this.intNameFields = Arrays.asList(c1_tunnelInterfaceName, c2_tunnelInterfaceName);
        this.keepaliveFields = Arrays.asList(c1_sec, c2_sec);
    }

    /**
     * Handle clicking of save button
     * Validate the input fields, open directory chooser, prompt user for password
     * Write and ZIP the configuration files
     */
    public void handleSave(Event event) {
        logger.info("Save was clicked. Validating inputs...");
        if (validateConfig()) {
            logger.info("Input is valid");

            logger.info("Creating config for Client 1");
            client1Config = new Configuration();
            client1Config.setName("client1");
            client1Config.setKeypair(new Keypair());
            client1Config.setEndpoint(this.c1_publicIP.getText());
            client1Config.setListenPort(Integer.parseInt(this.c1_publicPort.getText()));
            client1Config.setIp(this.c1_tunnelIP.getText());
            client1Config.setNetmask(this.c1_tunnelMask.getText());
            if (c1_defaultGateway.isSelected()) {
                client1Config.setDefaultGateway(true);
            }
            if (c1_psk.isSelected()) {
                client1Config.setPsk(Keypair.genpsk());
            }
            if (!this.c1_tunnelInterfaceName.getText().isEmpty()) {
                client1Config.setIntName(this.c1_tunnelInterfaceName.getText());
            }
            if (!this.c1_sec.getText().isEmpty()) {
                client1Config.setKeepAliveSeconds(Integer.parseInt(this.c1_sec.getText()));
            }

            logger.info("Creating config for Client 2");
            client2Config = new Configuration();
            client2Config.setName("client2");
            client2Config.setKeypair(new Keypair());
            client2Config.setEndpoint(this.c2_publicIP.getText());
            client2Config.setListenPort(Integer.parseInt(this.c2_publicPort.getText()));
            client2Config.setIp(this.c2_tunnelIP.getText());
            client2Config.setNetmask(this.c2_tunnelMask.getText());
            if (c2_defaultGateway.isSelected()) {
                client2Config.setDefaultGateway(true);
            }
            if (c2_psk.isSelected()) {
                client2Config.setPsk(client1Config.getPsk());
            }
            if (!this.c2_tunnelInterfaceName.getText().isEmpty()) {
                client2Config.setIntName(this.c1_tunnelInterfaceName.getText());
            }
            if (!this.c2_sec.getText().isEmpty()) {
                client2Config.setKeepAliveSeconds(Integer.parseInt(this.c2_sec.getText()));
            }

            logger.info("Opening directory chooser");
            // let user choose a directory
            Path selectedDirectory = chooseDirectory(main_anchorPane);
            if (selectedDirectory == null) {
                logger.warning("No directory selected. No Configuration will be written");
            } else {
                logger.info("Directory selected: '" + selectedDirectory + "'");
                try {
                    logger.info("Prompting the user for a ZIP password");
                    String password = promptPassword();
                    if (password.equals("")) {
                        // no password entered or dialog canceled
                        logger.warning("No password entered or Dialog canceled. Showing error dialog");
                        PopUp.showAlert(Alert.AlertType.ERROR, "Empty Password!", "Empty or no password entered!", "Please enter a password for the resulting ZIP file");
                    } else {
                        logger.info("Password entered. Writing and zipping and encrypting the files");
                        client1Config.generateConfigFile(client2Config, selectedDirectory + "/client1.conf");
                        client2Config.generateConfigFile(client1Config, selectedDirectory + "/client2.conf");
                        client1Config.generateSetupFile(client2Config, selectedDirectory + "/setup_client1.sh");
                        client2Config.generateSetupFile(client1Config,  selectedDirectory + "/setup_client2.sh");
                        Configuration.copyInstaller(selectedDirectory + "/install_wireguard.sh");
                        Configuration.copyReadme(selectedDirectory + "/README.txt", "README_c2c.txt");
                        zipConfiguration(selectedDirectory.toString(), password);
                        Configuration.deleteFiles(selectedDirectory.toString());
                    }
                } catch (ZipException | IOException e) {
                    logger.warning("IO or ZIP error. Showing error dialog");
                    PopUp.showAlert(Alert.AlertType.ERROR, "Access Rights!", "Access to this folder is denied!", "Please choose another folder than " + selectedDirectory);
                }
            }
        } else {
            logger.warning("Input is not valid. Showing error dialog");
            PopUp.showAlert(Alert.AlertType.INFORMATION, "Input Error!", null, "At least one of the necessary content is wrong or missing");
        }
    }

    /**
     * Handle clicking the "Guide" button. Show a dialog with instructions
     */
    public void handleC2CGuide(Event clicked) {
        Image fxImage = new Image(C2SController.class.getResource("/wgWizard/res/pictures/configExampleC2C.png").toString());

        ImageView imageView = new ImageView(fxImage);
        PopUp.showAlert(Alert.AlertType.INFORMATION, "Configuration helper dialog", "Please enter your configuration details as the example on the right side illustrates",
                "Public IP:Port\t\t\t\tThis is your public IP and listenPort, via which the other site will connect\n" +
                        "Tunnel IP/Mask\t\t\t\tThis is the ip address which you bind to your adapter within the tunnel\n\n" +
                        "Tunnel as default gateway?\tIf enabled, the default route for your system will point through the VPN tunnel\n" +
                        "Keepalive in sec\t\t\tSpecify the keepalive interval in seconds: between 1 and 65535\n" +
                        "Tunnel interface name\t\tSpecify the interface Name: between wg0 and wg255\n\n" +
                        "Press Back to choose another configuration scenario\n" +
                        "Press Save to go ahead and create your configuration", imageView);
    }

    /**
     * The preshared key has to be in both or neither configurations.
     * Bind the two check-boxes together. Clicking one, will also trigger the other one
     */
    private void handlePsk(Event event) {
        CheckBox checkBox = (CheckBox) event.getSource();
        this.c1_psk.setSelected(checkBox.isSelected());
        this.c2_psk.setSelected(checkBox.isSelected());
    }

    /**
     * Check all input fields and validate their values
     * Wrong fields will be marked red
     *
     * @return true if there are no errors, false if there is wrong input data
     */
    private boolean validateConfig() {
        boolean valid = true;
        boolean error;

        for (TextField field : ipFields) {
            error = validateIPv4(field.getText());
            markInputField(field, !error);
            if (!error) {
                logger.warning("One of the ip fields is not valid!");
                valid = false;
            }
        }
        for (TextField field : netmaskFields) {
            error = validateNetmask(field.getText());
            markInputField(field, !error);
            if (!error) {
                logger.warning("One of the netmask fields is not valid!");
                valid = false;
            }
        }
        for (TextField field : listenPortFields) {
            error = validateListenPort(field.getText());
            markInputField(field, !error);
            if (!error) {
                logger.warning("One of the listenPort fields is not valid!");
                valid = false;
            }
        }
        for (TextField field : intNameFields) {
            if (!field.getText().isEmpty()) {
                error = validateInterfaceName(field.getText());
                markInputField(field, !error);
                if (!error) {
                    logger.warning("One of the interfaceName fields is not valid!");
                    valid = false;
                }
            }
        }
        for (TextField field : keepaliveFields) {
            if (!field.getText().isEmpty()) {
                error = validateKeepaliveSec(field.getText());
                markInputField(field, !error);
                if (!error) {
                    logger.warning("One of the keepaliveInterval fields is not valid!");
                    valid = false;
                }
            }
        }

        return valid;
    }

    /**
     * Add or remove the style class "error" to any given TextField
     * This will give it a red border and show the user that there is a wrong input
     *
     * @param input TextField
     */
    private void markInputField(TextField input, boolean error) {
        if (error) {
            input.getStyleClass().add("error");
        } else {
            input.getStyleClass().removeAll("error");
        }
    }
}
