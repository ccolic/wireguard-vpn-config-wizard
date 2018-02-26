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
import javafx.scene.layout.GridPane;
import javafx.util.Pair;
import net.lingala.zip4j.exception.ZipException;
import wgWizard.config.ClientToSite.SiteConfiguration;
import wgWizard.config.Configuration;
import wgWizard.config.IPv4Netmask;
import wgWizard.config.Keypair;
import wgWizard.helper.Log;
import wgWizard.helper.PopUp;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.ResourceBundle;
import java.util.logging.Logger;

import static wgWizard.controller.S2SController.*;

public class C2SController implements Initializable {

    @FXML
    private MenuItem menu_close, menu_about;

    @FXML
    private TextField c_tunnelIP, c_tunnelMask, c_sec, c_tunnelInterfaceName;

    @FXML
    private TextField s_publicIP, s_publicPort, s_tunnelIP, s_tunnelMask, s_sec, s_tunnelInterfaceName, s_localNetwork, s_localNetworkMask;

    @FXML
    private Button btn_save, btn_back, btn_add, btn_add_client;

    @FXML
    private CheckBox c_defaultGateway, c_psk, s_psk;

    @FXML
    private AnchorPane main_anchorPane;

    @FXML
    private MenuItem menu_guide;

    @FXML
    private GridPane grid_clients, grid_localNetworks;

    private Logger logger;
    private List<TextField> ipFields;
    private List<TextField> netmaskFields;
    private List<TextField> listenPortFields;
    private List<TextField> intNameFields;
    private List<TextField> keepaliveFields;
    private List<Pair<TextField, TextField>> localNetworks = new ArrayList<>();
    private List<Pair<TextField, TextField>> clients = new ArrayList<>();
    private List<Configuration> clientConfigs = new ArrayList<>();
    private int clientCounter = 1;
    private int localNetworkCounter = 1;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        logger = Log.getInstance();
        logger.info("Initializing C2SController");

        // add event handler to buttons
        this.btn_save.setOnAction(this::handleSave);
        this.btn_add.setOnAction(this::handleAdd);
        this.btn_add_client.setOnAction(this::addClient);
        this.menu_about.setOnAction(MainController::handleAbout);
        this.menu_close.setOnAction(MainController::handleClose);
        this.c_psk.setOnAction(this::handlePsk);
        this.s_psk.setOnAction(this::handlePsk);
        this.btn_back.setOnAction(event -> MainController.handleBack(event, getClass(), main_anchorPane));
        this.menu_guide.setOnAction(this::handleC2SGuide);

        // populate field-lists for easier access
        this.ipFields = Arrays.asList(c_tunnelIP, s_tunnelIP, s_publicIP);
        this.netmaskFields = Arrays.asList(s_tunnelMask, s_tunnelMask);
        this.listenPortFields = Arrays.asList(s_publicPort);
        this.intNameFields = Arrays.asList(c_tunnelInterfaceName, s_tunnelInterfaceName);
        this.keepaliveFields = Arrays.asList(c_sec, s_sec);
        this.localNetworks.add(new Pair<>(s_localNetwork, s_localNetworkMask));
        this.clients.add(new Pair<>(c_tunnelIP, c_tunnelMask));
    }

    /**
     * Handle clicking the plus button for adding more clients
     */
    private void addClient(Event event) {
        logger.info("Adding Client row");
        // move the plus button one row down
        GridPane.setRowIndex(btn_add_client, clientCounter + 2);

        // create two new input fields for additional ip/masks
        TextField ip = new TextField();
        TextField mask = new TextField();

        // add them to the list, to keep track
        clients.add(new Pair<>(ip, mask));

        grid_clients.add(ip, 1, clientCounter + 1);
        grid_clients.add(mask, 2, clientCounter + 1);

        clientCounter += 1;
    }

    /**
     * Handle clicking the save button
     * Validate the input, create the configurations and write and zip them
     */
    public void handleSave(Event event) {
        logger.info("Save was clicked. Validating inputs...");
        if (validateConfig()) {
            logger.info("Input is valid");
            // create config files and the installer
            SiteConfiguration siteConfig = createSiteConfig();

            for (int i = 1; i <= clients.size(); i++) {
                Configuration clientConfiguration = createClientConfig(clients.get(i-1), i);
                clientConfigs.add(clientConfiguration);
            }
            writeConfiguration(siteConfig);
        } else {
            logger.warning("Input is not valid. Showing error dialog");
            PopUp.showAlert(Alert.AlertType.INFORMATION, "Input Error!", null, "At least one of the necessary content is wrong or missing");
        }
    }

    /**
     * Handle clicking the plus button on server side for adding more local networks
     */
    public void handleAdd(Event event) {
        // move the plus button one row down
        GridPane.setRowIndex(btn_add, localNetworkCounter + 1);

        // create two new input fields for additional ip/masks
        TextField ip = new TextField();
        TextField mask = new TextField();

        // add them to the list, to keep track
        localNetworks.add(new Pair<>(ip, mask));

        grid_localNetworks.add(ip, 1, localNetworkCounter);
        grid_localNetworks.add(mask, 2, localNetworkCounter);

        localNetworkCounter += 1;
    }

    /**
     * Handle clicking the "Guide" button. Show a dialog with instructions
     */
    public void handleC2SGuide(Event clicked) {
        Image fxImage = new Image(C2SController.class.getResource("/wgWizard/res/pictures/configExampleC2S.png").toString());

        ImageView imageView = new ImageView(fxImage);
        PopUp.showAlert(Alert.AlertType.INFORMATION, "Configuration helper dialog", "Please enter your configuration details as the example on the right side illustrates",
                "Public IP:Port\t\t\tThis is your public IP and listenPort, via which the other site will connect\n" +
                        "Tunnel IP/Mask\t\tThis is the ip address which you bind to your adapter within the tunnel\n" +
                        "Local Network/mask\tThese are your local networks that you want to share with the other site via the tunnel\n" +
                        "\n" +
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
        this.c_psk.setSelected(checkBox.isSelected());
        this.s_psk.setSelected(checkBox.isSelected());
    }


    /**
     * Check all input fields and validate their values
     * Wrong fields will be marked red
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
        for (Pair<TextField, TextField> pair : localNetworks) {
            error = validateIPv4(pair.getKey().getText());
            markInputField(pair.getKey(), !error);
            if (!error) {
                logger.warning("One of the localNetwork IP fields is not valid!");
                valid = false;
            }

            error = validateNetmask(pair.getValue().getText());
            markInputField(pair.getValue(), !error);
            if (!error) {
                logger.warning("One of the localNetwork Netmask fields is not valid!");
                valid = false;
            }
        }
        for (Pair<TextField, TextField> pair : clients) {
            error = validateIPv4(pair.getKey().getText());
            markInputField(pair.getKey(), !error);
            if (!error) {
                logger.warning("One of the Clients IP fields is not valid!");
                valid = false;
            }

            error = validateNetmask(pair.getValue().getText());
            markInputField(pair.getValue(), !error);
            if (!error) {
                logger.warning("One of the Clients Netmask fields is not valid!");
                valid = false;
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

    /**
     * Creates a SiteConfiguration object for site1 with all the data that the user has entered
     * @return an object of SiteConfiguration for Site1
     */
    private SiteConfiguration createSiteConfig() {
        logger.info("Creating config for Site");
        SiteConfiguration siteConfig = new SiteConfiguration();
        siteConfig.setName("site");
        siteConfig.setKeypair(new Keypair());
        siteConfig.setEndpoint(this.s_publicIP.getText());
        siteConfig.setListenPort(Integer.parseInt(this.s_publicPort.getText()));
        siteConfig.setIp(this.s_tunnelIP.getText());
        siteConfig.setNetmask(this.s_tunnelMask.getText());
        if (!this.s_tunnelInterfaceName.getText().isEmpty()) {
            siteConfig.setIntName(this.s_tunnelInterfaceName.getText());
        }
        if (!this.s_sec.getText().isEmpty()) {
            siteConfig.setKeepAliveSeconds(Integer.parseInt(this.s_sec.getText()));
        }
        for (Pair<TextField, TextField> pair : localNetworks) {
            siteConfig.addLocalNetwork(new Pair<>(pair.getKey().getText(), new IPv4Netmask(pair.getValue().getText())));
        }

        return siteConfig;
    }

    /**
     * Creates a SiteConfiguration object for site2 with all the data that the user has entered
     *
     * @return an object of SiteConfiguration for Site2
     */
    private Configuration createClientConfig(Pair<TextField, TextField> ipmask, int id) {
        logger.info("Creating config for Client " + id);
        Configuration clientConfig = new Configuration();
        clientConfig.setName(String.format("client-%d", id));
        clientConfig.setKeypair(new Keypair());
        clientConfig.setIp(ipmask.getKey().getText());
        clientConfig.setNetmask(ipmask.getValue().getText());
        if (this.c_psk.isSelected()) {
            clientConfig.setPsk(Keypair.genpsk());
        }
        if (c_defaultGateway.isSelected()) {
            clientConfig.setDefaultGateway(true);
        }
        if (!this.c_tunnelInterfaceName.getText().isEmpty()) {
            clientConfig.setIntName(this.c_tunnelInterfaceName.getText());
        }
        if (!this.c_sec.getText().isEmpty()) {
            clientConfig.setKeepAliveSeconds(Integer.parseInt(this.c_sec.getText()));
        }
        return clientConfig;
    }


    /**
     * Generate the wireguard configuration files and the setup scripts
     * Opens a DirectoryChooser dialog which lets the user select the destination directory
     */
    private void writeConfiguration(SiteConfiguration siteConfig) {
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
                    siteConfig.generateConfigFile(clientConfigs, selectedDirectory + "/site.conf");
                    siteConfig.generateSetupFile(selectedDirectory + "/setup_site.conf");
                    for (int i = 1; i <= clientConfigs.size(); i++) {
                        Configuration clientConfig = clientConfigs.get(i-1);
                        clientConfig.generateConfigFile(siteConfig, selectedDirectory + String.format("/client-%d.conf", i));
                        clientConfig.generateSetupFile(siteConfig, selectedDirectory + String.format("/setup_client-%d.sh", i));
                    }
                    Configuration.copyInstaller(selectedDirectory + "/install_wireguard.sh");
                    Configuration.copyReadme(selectedDirectory + "/README.txt", "README_c2s.txt");
                    zipConfiguration(selectedDirectory.toString(), password);
                    Configuration.deleteFiles(selectedDirectory.toString());
                }
            } catch (ZipException | IOException e) {
                logger.warning("IO or ZIP error. Showing error dialog");
                PopUp.showAlert(Alert.AlertType.ERROR, "Access Rights!", "Access to this folder is denied!", "Please choose another folder than " + selectedDirectory);
            }
        }
    }

}
