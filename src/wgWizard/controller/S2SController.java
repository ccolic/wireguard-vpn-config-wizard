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
import javafx.stage.DirectoryChooser;
import javafx.util.Pair;
import net.lingala.zip4j.exception.ZipException;
import wgWizard.config.Configuration;
import wgWizard.config.IPv4Netmask;
import wgWizard.config.Keypair;
import wgWizard.helper.Log;
import wgWizard.helper.PopUp;
import wgWizard.helper.Zipper;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class S2SController implements Initializable {

    private static final String IPV4_ADDR_PATTERN =
            "^([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\." +
                    "([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\." +
                    "([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\." +
                    "([01]?\\d\\d?|2[0-4]\\d|25[0-5])$";

    @FXML
    private MenuItem menu_close, menu_about;

    @FXML
    private TextField s1_publicIP, s1_publicPort, s1_tunnelIP, s1_tunnelMask, s1_localNetwork, s1_localNetworkMask, s1_sec, s1_tunnelInterfaceName;

    @FXML
    private TextField s2_publicIP, s2_publicPort, s2_tunnelIP, s2_tunnelMask, s2_localNetwork, s2_localNetworkMask, s2_sec, s2_tunnelInterfaceName;

    @FXML
    private Button btn_save, btn_back, btn_add_s1, btn_add_s2;

    @FXML
    private CheckBox s1_defaultGateway, s2_defaultGateway, s1_psk, s2_psk;

    @FXML
    private AnchorPane main_anchorPane;

    @FXML
    private MenuItem menu_guide;

    @FXML
    private GridPane grid_localNetworks_s1, grid_localNetworks_s2;

    private Logger logger;

    private List<TextField> ipFields;
    private List<TextField> netmaskFields;
    private List<TextField> listenPortFields;
    private List<TextField> intNameFields;
    private List<TextField> keepaliveFields;
    private List<Pair<TextField, TextField>> s1_localNetworks = new ArrayList<>();
    private List<Pair<TextField, TextField>> s2_localNetworks = new ArrayList<>();

    private int s1_localNetworkCounter = 1;
    private int s2_localNetworkCounter = 1;
    private Configuration site1Config;
    private Configuration site2Config;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        logger = Log.getInstance();
        logger.info("Initializing S2SController");

        // add event handler to buttons
        this.btn_save.setOnAction(this::handleSave);
        this.btn_add_s1.setOnAction(this::handleAdd);
        this.btn_add_s2.setOnAction(this::handleAdd);
        this.menu_about.setOnAction(MainController::handleAbout);
        this.menu_close.setOnAction(MainController::handleClose);
        this.s1_psk.setOnAction(this::handlePsk);
        this.s2_psk.setOnAction(this::handlePsk);
        this.btn_back.setOnAction(event -> MainController.handleBack(event, getClass(), main_anchorPane));
        this.menu_guide.setOnAction(this::handleS2SGuide);

        // populate field-lists for easier access
        this.ipFields = Arrays.asList(s1_publicIP, s2_publicIP, s1_tunnelIP, s2_tunnelIP);
        this.netmaskFields = Arrays.asList(s1_tunnelMask, s2_tunnelMask);
        this.listenPortFields = Arrays.asList(s1_publicPort, s2_publicPort);
        this.intNameFields = Arrays.asList(s1_tunnelInterfaceName, s2_tunnelInterfaceName);
        this.keepaliveFields = Arrays.asList(s1_sec, s2_sec);
        this.s1_localNetworks.add(new Pair<>(s1_localNetwork, s1_localNetworkMask));
        this.s2_localNetworks.add(new Pair<>(s2_localNetwork, s2_localNetworkMask));
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

            // create config files and the installer
            site1Config = createSite1Config();
            site2Config = createSite2Config();

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
                        writeConfiguration(site1Config, site2Config, selectedDirectory + "");
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
     * Handle clicking the plus button for adding more local networks
     * Add a new row in the Grid
     */
    public void handleAdd(Event event) {
        if (event.getSource() == btn_add_s1) {
            logger.info("Add local networks button was clicked for site 1");
            // move the plus button one row down
            GridPane.setRowIndex(btn_add_s1, s1_localNetworkCounter + 1);

            // create two new input fields for additional ip/masks
            TextField ip = new TextField();
            TextField mask = new TextField();

            // add them to the list, to keep track
            s1_localNetworks.add(new Pair<>(ip, mask));

            grid_localNetworks_s1.add(ip, 1, s1_localNetworkCounter);
            grid_localNetworks_s1.add(mask, 2, s1_localNetworkCounter);

            s1_localNetworkCounter += 1;
        } else {
            logger.info("Add local networks button was clicked for site 2");
            // move the plus button one row down
            GridPane.setRowIndex(btn_add_s2, s2_localNetworkCounter + 1);

            // create two new input fields for additional ip/masks
            TextField ip = new TextField();
            TextField mask = new TextField();

            // add them to the list, to keep track
            s2_localNetworks.add(new Pair<>(ip, mask));

            grid_localNetworks_s2.add(ip, 1, s2_localNetworkCounter);
            grid_localNetworks_s2.add(mask, 2, s2_localNetworkCounter);

            s2_localNetworkCounter += 1;
        }
    }

    /**
     * Handle clicking the "Guide" button. Show a dialog with instructions
     */
    public void handleS2SGuide(Event clicked) {
        Image fxImage = new Image(S2SController.class.getResource("/wgWizard/res/pictures/configExampleS2S.png").toString());

        ImageView imageView = new ImageView(fxImage);
        PopUp.showAlert(Alert.AlertType.INFORMATION, "Configuration helper dialog", "Please enter your configuration details as the example on the right side illustrates",
                "Public IP:Port\t\t\t\tThis is your public IP and listenPort, via which the other site will connect\n" +
                        "Tunnel IP/Mask\t\t\t\tThis is the ip address which you bind to your adapter within the tunnel\n" +
                        "Local Network/mask\t\t\tThese are your local networks that you want to share with the other site via the tunnel\n" +
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
        this.s1_psk.setSelected(checkBox.isSelected());
        this.s2_psk.setSelected(checkBox.isSelected());
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
        for (Pair<TextField, TextField> pair : s1_localNetworks) {
            error = validateIPv4(pair.getKey().getText());
            markInputField(pair.getKey(), !error);
            if (!error) {
                logger.warning("One of the s1_localNetwork IP fields is not valid!");
                valid = false;
            }

            error = validateNetmask(pair.getValue().getText());
            markInputField(pair.getValue(), !error);
            if (!error) {
                logger.warning("One of the s1_localNetwork Netmask fields is not valid!");
                valid = false;
            }
        }
        for (Pair<TextField, TextField> pair : s2_localNetworks) {
            error = validateIPv4(pair.getKey().getText());
            markInputField(pair.getKey(), !error);
            if (!error) {
                logger.warning("One of the s2_localNetwork IP fields is not valid!");
                valid = false;
            }

            error = validateNetmask(pair.getValue().getText());
            markInputField(pair.getValue(), !error);
            if (!error) {
                logger.warning("One of the s2_localNetwork Netmask fields is not valid!");
                valid = false;
            }
        }

        return valid;
    }

    /**
     * Validate an IP
     *
     * @param ip The IP as string x.x.x.x, without any kind of netmask
     * @return true if valid, false if invalid
     */
    public static boolean validateIPv4(String ip) {
        Pattern pattern = Pattern.compile(IPV4_ADDR_PATTERN);
        Matcher matcher = pattern.matcher(ip);
        if (matcher.matches()) {
            return true;
        } else {
            Log.getInstance().warning(String.format("IP '%s' is not valid!", ip));
            return false;
        }
    }

    /**
     * Validate the listen port.
     * Valid range: 1 - 65535
     *
     * @param listenPort the Port entered by the user
     * @return true if valid, false if invalid
     */
    public static boolean validateListenPort(String listenPort) {
        try {
            int port = Integer.parseInt(listenPort);
            if (port >= 1 && port <= 65535) {
                return true;
            } else {
                Log.getInstance().warning(String.format("listenPort '%s' is not valid!", listenPort));
                return false;
            }
        } catch (NumberFormatException nfe) {
            Log.getInstance().warning(String.format("listenPort '%s' is not a valid Integer valid!", listenPort));
            return false;
        }
    }

    /**
     * Validate the wireguard interface name
     * Valid range: wg0 - wg255
     *
     * @param interfaceName the interface name entered by the user, eg: wg21
     * @return true if valid, false if invalid
     */
    public static boolean validateInterfaceName(String interfaceName) {
        int wgNumber;
        if (interfaceName.startsWith("wg")) {
            try {
                wgNumber = Integer.parseInt(interfaceName.substring(2));
            } catch (NumberFormatException e) {
                return false;
            }
            if (wgNumber >= 0 && wgNumber <= 255) {
                return true;
            } else {
                Log.getInstance().warning(String.format("InterfaceName '%s' starts with 'wg' but the number is not valid.", interfaceName));
                return false;
            }
        }
        Log.getInstance().warning(String.format("InterfaceName '%s' is not valid!", interfaceName));
        return false;
    }

    /**
     * Validate the keepalive value
     * Valid range: 1 - 65535
     *
     * @param keepalive the keepalive value in seconds
     * @return true if valid, false if invalid
     */
    public static boolean validateKeepaliveSec(String keepalive) {
        try {
            int sec = Integer.parseInt(keepalive);
            if (sec >= 1 && sec <= 65535) {
                return true;
            } else {
                Log.getInstance().warning(String.format("listenPort '%s' is not valid!", keepalive));
                return false;
            }
        } catch (NumberFormatException nfe) {
            Log.getInstance().warning(String.format("listenPort '%s' is not a valid Integer valid!", keepalive));
            return false;
        }
    }

    /**
     * Validate the IPv4 subnet mask
     *
     * @param netmask the netmask in full format "255.255.255.0" or as prefix "24"
     * @return true if valid, false if invalid
     */
    public static boolean validateNetmask(String netmask) {
        return IPv4Netmask.validateIPv4Netmask(netmask) || IPv4Netmask.validateIPv4NetmaskPrefix(netmask);
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
     *
     * @return an object of SiteConfiguration for Site1
     */
    private Configuration createSite1Config() {
        logger.info("Creating config for Site 1");
        Configuration site1Config = new Configuration();
        site1Config.setName("site1");
        site1Config.setKeypair(new Keypair());
        site1Config.setEndpoint(this.s1_publicIP.getText());
        site1Config.setListenPort(Integer.parseInt(this.s1_publicPort.getText()));
        site1Config.setIp(this.s1_tunnelIP.getText());
        site1Config.setNetmask(this.s1_tunnelMask.getText());
        if (s1_psk.isSelected()) {
            site1Config.setPsk(Keypair.genpsk());
        }
        if (s1_defaultGateway.isSelected()) {
            site1Config.setDefaultGateway(true);
        }
        if (!this.s1_tunnelInterfaceName.getText().isEmpty()) {
            site1Config.setIntName(this.s1_tunnelInterfaceName.getText());
        }
        if (!this.s1_sec.getText().isEmpty()) {
            site1Config.setKeepAliveSeconds(Integer.parseInt(this.s1_sec.getText()));
        }
        for (Pair<TextField, TextField> pair : s1_localNetworks) {
            site1Config.addLocalNetwork(new Pair<>(pair.getKey().getText(), new IPv4Netmask(pair.getValue().getText())));
        }

        return site1Config;
    }

    /**
     * Creates a SiteConfiguration object for site2 with all the data that the user has entered
     *
     * @return an object of SiteConfiguration for Site2
     */
    private Configuration createSite2Config() {
        logger.info("Creating config for Site 2");
        Configuration site2Config = new Configuration();
        site2Config.setName("site2");
        site2Config.setKeypair(new Keypair());
        site2Config.setEndpoint(this.s2_publicIP.getText());
        site2Config.setListenPort(Integer.parseInt(this.s2_publicPort.getText()));
        site2Config.setIp(this.s2_tunnelIP.getText());
        site2Config.setNetmask(this.s2_tunnelMask.getText());
        if (s2_psk.isSelected()) {
            site2Config.setPsk(site1Config.getPsk());
        }
        if (s2_defaultGateway.isSelected()) {
            site2Config.setDefaultGateway(true);
        }
        if (!this.s2_tunnelInterfaceName.getText().isEmpty()) {
            site2Config.setIntName(this.s2_tunnelInterfaceName.getText());
        }
        if (!this.s2_sec.getText().isEmpty()) {
            site2Config.setKeepAliveSeconds(Integer.parseInt(this.s2_sec.getText()));
        }
        for (Pair<TextField, TextField> pair : s2_localNetworks) {
            site2Config.addLocalNetwork(new Pair<>(pair.getKey().getText(), new IPv4Netmask(pair.getValue().getText())));
        }

        return site2Config;
    }

    /**
     * Open a dialog and prompt the user for a ZIP password
     * @return the password or an empty string when canceling
     */
    public static String promptPassword() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Protect your files with a password");
        dialog.setHeaderText("Please enter a password");
        dialog.setContentText("The files will be packed in a ZIP and will be protected by a password.\n" +
                "Please enter a password: ");

        Optional<String> result = dialog.showAndWait();
        return result.orElse("");
    }

    /**
     * Open a DirectoryChooser dialog and prompt the user for the destination directory
     * @return the selected directory, or null if canceled
     */
    public static Path chooseDirectory(AnchorPane main) {
        DirectoryChooser directoryChooser = new DirectoryChooser();
        File selectedDirectory = directoryChooser.showDialog(main.getScene().getWindow());
        Path dir;
        if (selectedDirectory == null) {
            Log.getInstance().warning("No directory was selected! Returning homedir as default");
            return null;
        } else {
            Log.getInstance().info("Selected directory is: " + selectedDirectory);
            try {
                Log.getInstance().info("Trying to create directory 'wireguard-configuration' in " + selectedDirectory);
                dir = Files.createDirectory(Paths.get(selectedDirectory + "/wireguard-configuration"));
            } catch (FileAlreadyExistsException e) {
                Log.getInstance().info("Directory 'wireguard-configuration' already exists in " + selectedDirectory);
                dir = new File(selectedDirectory + "/wireguard-configuration").toPath();
            } catch (IOException e) {
                Log.getInstance().warning("IO error. Showing error dialog");
                PopUp.showAlert(Alert.AlertType.ERROR, "Access Rights!", "Access to this folder is denied!", "Please choose another folder than " + selectedDirectory);
                return null;
            }
            return dir;
        }
    }

    /**
     * Generate the wireguard configuration files and the setup scripts
     * Opens a DirectoryChooser dialog which lets the user select the destination directory
     *
     * @param site1Config configuration of Site1
     * @param site2Config configuration of Site2
     */
    private void writeConfiguration(Configuration site1Config, Configuration site2Config, String selectedDirectory) throws IOException {
        site1Config.generateConfigFile(site2Config, selectedDirectory + "/site1.conf");
        site2Config.generateConfigFile(site1Config, selectedDirectory + "/site2.conf");
        site1Config.generateSetupFile(site2Config, selectedDirectory + "/setup_site1.sh");
        site2Config.generateSetupFile(site1Config, selectedDirectory + "/setup_site2.sh");
        Configuration.copyInstaller(selectedDirectory + "/install_wireguard.sh");
        Configuration.copyReadme(selectedDirectory + "/README.txt", "README_s2s.txt");
    }

    /**
     * pack the generated setup and configuration files in a password protected ZIP
     * delete the original files
     *
     * @param filePath path where the setup files have been saved
     */
    public static void zipConfiguration(String filePath, String password) throws ZipException {
        Zipper.pack(filePath, password);
    }
}
