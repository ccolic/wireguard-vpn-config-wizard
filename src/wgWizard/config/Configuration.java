/*
 * This software is GPL2 licensed, find further license information
 * in the LICENSE file located in the root directory
 *
 * Created on : 22-12-17
 * Authors    : Christian Colic, Marc Werenfels
 *
 */

package wgWizard.config;

import javafx.scene.control.Alert;
import javafx.util.Pair;
import wgWizard.helper.Log;
import wgWizard.helper.PopUp;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class Configuration {
    // default values
    private final int DEFAULT_KEEPALIVE = 30;
    private final String DEFAULT_INTNAME = "wg0";

    private String name;
    private Keypair keypair;
    private String presharedKey;
    private String endpoint;
    private int listenPort;
    private String ip;
    private IPv4Netmask netmask;
    private boolean keepAlive;
    private boolean defaultGateway;
    private int keepAliveSeconds;
    private String intName;
    private List<Pair<String, IPv4Netmask>> localNetworks;

    public Configuration() {
        setIntName(DEFAULT_INTNAME);
        this.keepAliveSeconds = DEFAULT_KEEPALIVE;
        localNetworks = new ArrayList<>();
    }

    public Configuration(String name, Keypair keypair, String endpoint, int listenPort, String ip, String netmask_prefix, String intName) {
        this();
        setName(name);
        setKeypair(keypair);
        setEndpoint(endpoint);
        setListenPort(listenPort);
        setIp(ip);
        setNetmask(netmask_prefix);
        setKeepAlive(false);
        setDefaultGateway(false);
        setKeepAliveSeconds(DEFAULT_KEEPALIVE);
        setIntName(intName);
    }

    public Configuration(String name, Keypair keypair, String endpoint, int listenPort, String ip, String netmask_prefix, int keepAlive, String intName) {
        this(name, keypair, endpoint, listenPort, ip, netmask_prefix, intName);
        setKeepAliveSeconds(keepAlive);
    }

    /**
     * Generate the wireguard config file
     *
     * @param othersite SiteConfiguration of the other site
     * @param filePath  the destination path
     */
    public void generateConfigFile(Configuration othersite, String filePath) throws IOException {
        Log.getInstance().info("Writing Configuration file to " + filePath);
        PrintWriter writer = new PrintWriter(filePath, "UTF-8");
        writer.print("[Interface]\n");
        // set this sites private key
        writer.print("PrivateKey = " + this.getKeypair().getPrivateKey() + "\n");
        // set this sites listen port
        writer.print("ListenPort = " + this.getListenPort() + "\n");
        writer.print("\n");

        writer.print("[Peer]\n");
        // set the other sites public key
        writer.print("PublicKey = " + othersite.getKeypair().getPublicKey() + "\n");
        if (this.presharedKey != null) {
            // set preshared key
            writer.print("PresharedKey = " + this.getPsk() + "\n");
        }
        // set the other sites endpoint ip and port
        writer.print("Endpoint = " + othersite.getEndpoint() + ":" + othersite.getListenPort() + "\n");

        // add allowed ips. tunnelIP of the other site is always allowed.
        // add all the local networks of the other site
        String allowedIps = othersite.getIp() + "/32";
        allowedIps += othersite.getLocalNetworksAsString();
        writer.print("AllowedIPs = " + allowedIps + "\n");

        // if enabled, set the keepalive interval
        if (this.isSetKeepAlive()) {
            writer.print("PersistentKeepalive = " + this.getKeepAliveSeconds() + "\n");
        }
        writer.close();
    }

    /**
     * Generate the Wireguard setup script
     * This will create the wireguard interface and add the wireguard config to it
     *
     * @param othersite SiteConfiguration of the other site
     * @param filePath  the destination path
     */
    public void generateSetupFile(Configuration othersite, String filePath) {
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

            if (defaultGateway) {
                // if enabled, add default route over the wireguard interface
                writer.print("sudo ip route add default via " + othersite.getIp() + "\n");
            } else {
                // else, add routes for each local network of the other site
                writer.print(addRoutes(othersite));
            }
            writer.close();
        } catch (IOException e) {
            Log.getInstance().warning("IO Error while writing setup file " + this.getName());
            PopUp.showAlert(Alert.AlertType.ERROR, "IO Error", "Error writing setup file",
                    "There was an error while creating the setup file. Please choose another destination directory.");
        }
    }

    /**
     * Copy the wireguard_installer.sh script to the output directory
     * @param path destination directory
     * @throws IOException if there are any permission problems
     */
    public static void copyInstaller(String path) throws IOException {
        Log.getInstance().info("Copying the wireguard install file to " + path);
        InputStream in = Configuration.class.getResourceAsStream("/files/install_wireguard.sh");
        byte[] bytes = in.readAllBytes();
        try (FileOutputStream fos = new FileOutputStream(path)) {
            fos.write(bytes);
            fos.close();
        }
    }

    /**
     * Copy the readme file to the output directory
     * @param path destination directory
     * @param name name of the README file
     * @throws IOException if there are any permission problems
     */
    public static void copyReadme(String path, String name) throws IOException {
        Log.getInstance().info("Copying the readme file to " + path);
        InputStream in = Configuration.class.getResourceAsStream("/files/" + name);
        byte[] bytes = in.readAllBytes();
        try (FileOutputStream fos = new FileOutputStream(path)) {
            fos.write(bytes);
            fos.close();
        }
    }

    /**
     * Delete the generated files, leaving only the ZIP archive, which is password protected
     * @param path Path to the 'wireguard-configuration' directory, which contains all the files and also the zip file
     */
    public static void deleteFiles(String path) {
        File dir = new File(path);
        File[] dirList = dir.listFiles();
        if (dirList != null) {
            Log.getInstance().info("Deleting files all .conf and .sh files in " + path);
            for (File file : dirList)
                if (getFileExtension(file).equals("conf") || getFileExtension(file).equals("sh"))
                    file.delete();
        }
    }

    private static String getFileExtension(File file) {
        String name = file.getName();
        try {
            return name.substring(name.lastIndexOf(".") + 1);
        } catch (Exception e) {
            return "";
        }
    }



    /**
     * Create a route for each local network of the other site
     * The gateway for the route, is the other site's tunnel ip
     *
     * @param othersite SiteConfiguration of the other site
     * @return a String of commands
     */
    public String addRoutes(Configuration othersite) {
        // if there are no local networks on the other site, leave empty
        if (othersite.getLocalNetworks().size() == 0) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (Pair<String, IPv4Netmask> pair : othersite.getLocalNetworks()) {
            sb.append("sudo ip route add ");
            // calculate the network address
            sb.append(IPv4Netmask.getNetworkAddress(pair.getKey(), pair.getValue()));
            sb.append("/");
            sb.append(pair.getValue().getPrefix());
            sb.append(" via ");
            sb.append(othersite.getIp());
            sb.append("\n");
        }

        return sb.toString();
    }

    /**
     * Return a list of pairs. Each pair contains the IP and the netmask
     * @return List of Pair<String ip, IPv4Netmask netmask>
     */
    public List<Pair<String, IPv4Netmask>> getLocalNetworks() {
        return this.localNetworks;
    }

    /**
     * Set local networks
     * @param localNetworks List of Pair<String ip, IPv4Netmask netmask>
     */
    public void setLocalNetworks(List<Pair<String, IPv4Netmask>> localNetworks) {
        this.localNetworks = localNetworks;
    }

    /**
     * Return the list of local networks as a comma-seperated list
     * ", 192.168.1.0/24,10.1.1.0/16"
     *
     * @return a comma separated list of the local networks and their netmask prefix
     */
    public String getLocalNetworksAsString() {
        StringBuilder stringBuilder = new StringBuilder();
        for (Pair<String, IPv4Netmask> pair : localNetworks) {
            stringBuilder.append(",");
            // calculate the network address
            stringBuilder.append(IPv4Netmask.getNetworkAddress(pair.getKey(), pair.getValue()));
            stringBuilder.append("/");
            stringBuilder.append(pair.getValue().getPrefix());
        }
        return stringBuilder.toString();
    }

    /**
     * Add a Pair<String ip, IPv4Netmask netmask> to the LocalNetwork list
     * @param pair Pair<String ip, IPv4Netmask netmask>
     */
    public void addLocalNetwork(Pair<String, IPv4Netmask> pair) {
        this.localNetworks.add(pair);
    }

    /**
     * Get the Keypair
     * @return Keypair
     */
    public Keypair getKeypair() {
        return this.keypair;
    }

    /**
     * Set the Keypar
     * @param keypair Keypair
     */
    public void setKeypair(Keypair keypair) {
        this.keypair = keypair;
    }

    /**
     * Get the Endpoint IP as String
     * @return IP as String
     */
    public String getEndpoint() {
        return this.endpoint;
    }

    /**
     * Set the endpoint IP
     * @param endpoint IP as string
     */
    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

    /**
     * Get the listen port as int
     * @return listenPort as int
     */
    public int getListenPort() {
        return this.listenPort;
    }

    /**
     * Set the listenport
     * @param listenPort port as int
     */
    public void setListenPort(int listenPort) {
        this.listenPort = listenPort;
    }

    /**
     * Get the IP as string
     * @return the IP as string
     */
    public String getIp() {
        return this.ip;
    }

    /**
     * Set the IP
     * @param ip IP as String
     */
    public void setIp(String ip) {
        this.ip = ip;
    }

    /**
     * Check if KeepAlive is set
     * @return true if set, else if not
     */
    public boolean isSetKeepAlive() {
        return this.keepAlive;
    }

    /**
     * Check if Default Gateway is set
     * @return true if set, else if not
     */
    public boolean isSetDefaultGateway() {
        return this.defaultGateway;
    }

    /**
     * Set the Keepalive status
     * @param keepAlive true to activate, else false
     */
    public void setKeepAlive(boolean keepAlive) {
        this.keepAlive = keepAlive;
    }

    /**
     * Get the interface name as Sring
     * @return the interface name as Sring
     */
    public String getIntName() {
        return this.intName;
    }

    /**
     * Set the interface name
     * @param intName as String
     */
    public void setIntName(String intName) {
        this.intName = intName;
    }

    /**
     * Get the keepalive interval in seconds
     * @return keepalive interval in seconds as int
     */
    public int getKeepAliveSeconds() {
        return this.keepAliveSeconds;
    }

    /**
     * Set the keepalive interval in seconds
     * @param keepAliveSeconds keepalive interval in seconds
     */
    public void setKeepAliveSeconds(int keepAliveSeconds) {
        this.setKeepAlive(true);
        this.keepAliveSeconds = keepAliveSeconds;
    }

    /**
     * Get the netmask as String, in full format. eg: 255.255.255.0
     * @return the netmask as String
     */
    public String getNetmask() {
        return this.netmask.getNetmask();
    }

    /**
     * Set the netmask
     * @param netmask Netmask as String in full format. eg: 255.255.255.0
     */
    public void setNetmask(String netmask) {
        this.netmask = new IPv4Netmask(netmask);
    }

    /**
     * Get the netmask as String in Prefix format. eg: /24
     * @return for example '24' as String
     */
    public String getNetmaskPrefix() {
        return this.netmask.getPrefix();
    }

    /**
     * Get the name as String
     * @return the name as String
     */
    public String getName() {
        return this.name;
    }

    /**
     * Set the name
     * @param name Name as String
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Set default gateway. If enabled, all traffic will be routed over the wireguard tunnel
     * @param defaultGateway true if active, else false
     */
    public void setDefaultGateway(boolean defaultGateway) {
        this.defaultGateway = defaultGateway;
    }

    /**
     * Set preshared key.
     * @param psk base64 encoded preshared key, as generated by "wg genpsk"
     */
    public void setPsk(String psk) {
        this.presharedKey = psk;
    }

    /**
     * Get the preshared key as String
     * @return the PSK as String
     */
    public String getPsk() {
        return this.presharedKey;
    }

}
