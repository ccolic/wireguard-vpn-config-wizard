/*
 * This software is GPL2 licensed, find further license information
 * in the LICENSE file located in the root directory
 *
 * Created on : 22-12-17
 * Authors    : Christian Colic, Marc Werenfels
 *
 */

package wgWizard.config.ClientToSite;

import javafx.scene.control.Alert;
import wgWizard.config.Configuration;
import wgWizard.config.Keypair;
import wgWizard.helper.Log;
import wgWizard.helper.PopUp;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

/**
 * Configuration for the "Site" in a Client-To-Site setup
 */
public class SiteConfiguration extends Configuration {

    public SiteConfiguration() {
        super();
    }

    public SiteConfiguration(String name, Keypair keypair, String endpoint, int listenPort, String ip, String netmask_prefix, String intName) {
        super(name, keypair, endpoint, listenPort, ip, netmask_prefix, intName);
    }

    public SiteConfiguration(String name, Keypair keypair, String endpoint, int listenPort, String ip, String netmask_prefix, int keepAlive, String intName) {
        this(name, keypair, endpoint, listenPort, ip, netmask_prefix, intName);
        setKeepAliveSeconds(keepAlive);
    }

    /**
     * Generate the wireguard config file
     *
     * @param filePath  the destination path
     */
    public void generateConfigFile(List<Configuration> clientConfigs, String filePath) throws IOException {
        Log.getInstance().info("Writing Configuration file to " + filePath);
        PrintWriter writer = new PrintWriter(filePath, "UTF-8");
        writer.print("[Interface]\n");
        // set this sites private key
        writer.print("PrivateKey = " + this.getKeypair().getPrivateKey() + "\n");
        // set this sites listen port
        writer.print("ListenPort = " + this.getListenPort() + "\n");
        writer.print("\n");

        // CLIENTS
        for (Configuration clientConfig : clientConfigs) {
            writer.print("[Peer]\n");
            // set the other sites public key
            writer.print("PublicKey = " + clientConfig.getKeypair().getPublicKey() + "\n");
            if (clientConfig.getPsk() != null) {
                // set preshared key
                writer.print("PresharedKey = " + clientConfig.getPsk() + "\n");
            }
            // add allowed ips. tunnelIP of the client is always allowed.
            writer.print("AllowedIPs = " + clientConfig.getIp() + "/32\n");

            // if enabled, set the keepalive interval
            if (this.isSetKeepAlive()) {
                writer.print("PersistentKeepalive = " + this.getKeepAliveSeconds() + "\n");
            }
            writer.print("\n\n");
        }
        writer.close();
    }

    /**
     * Generate the Wireguard setup script
     * This will create the wireguard interface and add the wireguard config to it
     *
     * @param filePath  the destination path
     */
    public void generateSetupFile(String filePath) {
        try {
            Log.getInstance().info("Writing Setup file to " + filePath);
            PrintWriter writer = new PrintWriter(filePath, "UTF-8");
            writer.print("#!/bin/bash\n");
            // add the wireguard interface, with the given interface name
            writer.print("sudo ip link add dev " + this.getIntName() + " type wireguard\n");
            // add the specified ip and netmask to the wireguard interface
            writer.print("sudo ip addr add " + this.getIp() + "/" + this.getNetmaskPrefix() + " dev " + this.getIntName() + "\n");
            // activate the interface
            writer.print("sudo ip link set dev " + this.getIntName() + " up\n");
            // apply the wireguard config
            writer.print("sudo wg setconf " + this.getIntName() + " " + this.getName() + ".conf\n");

            writer.close();
        } catch (IOException e) {
            Log.getInstance().warning("IO Error while writing setup file " + this.getName());
            PopUp.showAlert(Alert.AlertType.ERROR, "IO Error", "Error writing setup file",
                    "There was an error while creating the setup file. Please choose another destination directory.");
        }
    }
}
